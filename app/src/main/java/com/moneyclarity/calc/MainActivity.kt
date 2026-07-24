package com.moneyclarity.calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.content.Context
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.moneyclarity.calc.data.CalcState
import com.moneyclarity.calc.data.Calculators
import com.moneyclarity.calc.data.Links
import com.moneyclarity.calc.data.Store
import com.moneyclarity.calc.ui.components.LocalHaptics
import com.moneyclarity.calc.ui.components.rememberHaptics
import com.moneyclarity.calc.ui.components.BrandFooterBar
import com.moneyclarity.calc.ui.screens.*
import com.moneyclarity.calc.ui.theme.MoneyClarityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

const val SITE_URL = Links.SITE

private val titles = mapOf(
    "calculators" to "All calculators",
    "home" to "MoneyClarity Calc",
    "cost" to "Effective cost",
    "emi" to "Instalment",
    "prepay" to "Prepayment",
    "compare" to "Compare quotes",
    "schedule" to "Schedule",
    "saved" to "Saved",
    "settings" to "Settings"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    // Both preferences are read once at start-up and written the moment they
    // change, so the choice survives the app being closed.
    var themeMode by remember { mutableIntStateOf(Store.themeMode(context)) }
    var hapticsOn by remember { mutableStateOf(Store.hapticsEnabled(context)) }
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    MoneyClarityTheme(darkTheme = dark) {
      CompositionLocalProvider(LocalHaptics provides rememberHaptics(hapticsOn)) {
        val navController = rememberNavController()
        val state = remember { CalcState() }
        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route ?: "home"
        val argId = backStack?.arguments?.getString("id")
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        // The bar carries the brand colour so it reads as a distinct band rather
        // than blending into the body of the page.
        val header = if (dark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
        val onHeader = if (dark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                titles[route]
                                    ?: Calculators.byId(argId ?: "")?.title
                                    ?: "MoneyClarity Calc"
                            )
                        },
                        navigationIcon = {
                            if (route != "home") {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        actions = {
                            // The site link now lives only in the bottom bar,
                            // carrying the publisher's own mark rather than a
                            // generic globe. This icon opens the in-app
                            // Settings screen instead, and hides itself while
                            // already there.
                            if (route != "settings") {
                                IconButton(onClick = { navController.navigate("settings") }) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = header,
                            titleContentColor = onHeader,
                            navigationIconContentColor = onHeader,
                            actionIconContentColor = onHeader
                        )
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = if (dark) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
            ,
            bottomBar = {
                // The publisher's own mark, not the app's. Kept visually distinct
                // from the teal chrome above it so it reads as "brought to you
                // by moneyclaritytech.com" rather than as another app control.
                // It is deliberately removed while typing. A permanent footer
                // competing with the IME is what used to leave too little room
                // for the focused field on smaller phones.
                if (!keyboardVisible) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Column(Modifier.navigationBarsPadding()) {
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                            BrandFooterBar(onClick = { openSite(context) })
                        }
                    }
                }
            }
        ) { inner ->
            // Fill the complete resized window, consume the Scaffold insets once,
            // and then add only the current IME inset. Every scrollable screen
            // now receives a real, keyboard-safe viewport.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .consumeWindowInsets(inner)
                    .imePadding()
            ) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(onOpen = { navController.navigate(it) })
                    }
                    composable("cost") { EffectiveCostScreen(state) }
                    composable("emi") {
                        EmiScreen(state, onOpenSchedule = { navController.navigate("schedule") })
                    }
                    composable("prepay") { PrepaymentScreen(state) }
                    composable("compare") { CompareScreen() }
                    composable("schedule") { ScheduleScreen(state) }
                    composable("saved") {
                        SavedScreen(state, onLoaded = { navController.navigate("emi") })
                    }
                    composable("calculators") {
                        CalculatorsScreen(onOpen = { navController.navigate("calc/$it") })
                    }
                    composable("calc/{id}") { entry ->
                        CalculatorDetailScreen(entry.arguments?.getString("id") ?: "sip")
                    }
                    composable("settings") {
                        SettingsScreen(
                            themeMode = themeMode,
                            onThemeChange = { mode ->
                                themeMode = mode
                                Store.setThemeMode(context, mode)
                            },
                            hapticsOn = hapticsOn,
                            onHapticsChange = { on ->
                                hapticsOn = on
                                Store.setHaptics(context, on)
                            },
                            onOpenSite = { openSite(context) }
                        )
                    }
                }
            }
        }
      }
    }
}

/**
 * Opening a page hands the address to whichever browser the person already uses.
 * That is why the app can carry a link out without declaring the internet
 * permission: it never fetches anything itself.
 */
fun openSite(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, SITE_URL.toUri()))
    }
}
