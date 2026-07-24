package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.data.Export
import com.moneyclarity.calc.ui.components.BrandPill
import com.moneyclarity.calc.ui.components.HairlineDivider
import com.moneyclarity.calc.ui.components.SectionCard
import com.moneyclarity.calc.ui.components.StatRow
import com.moneyclarity.calc.ui.components.ToggleRow
import com.moneyclarity.calc.ui.components.haptics

@Composable
fun SettingsScreen(
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    hapticsOn: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    onOpenSite: () -> Unit
) {
    val context = LocalContext.current
    val tap = haptics()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(title = "Appearance") {
            ToggleRow(
                options = listOf("System", "Light", "Dark"),
                selectedIndex = themeMode,
                onSelect = { mode -> if (mode != themeMode) onThemeChange(mode) }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Whichever you pick is remembered, including after the app is closed. " +
                    "On System it follows the phone's own light and dark setting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "Touch feedback") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Vibrate on taps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = hapticsOn,
                    onCheckedChange = { on ->
                        // Fire before the state flips so turning it off still gives
                        // the confirming tap, and turning it on demonstrates itself.
                        tap.select()
                        onHapticsChange(on)
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "A short tick when a value steps, a choice is made, or a figure is worked out. " +
                    "This uses the same feedback the keyboard does, which is why the app can " +
                    "offer it without asking for the vibration permission. If touch feedback is " +
                    "switched off for the whole phone, that setting wins over this one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "What this app does with your data") {
            Text(
                "Nothing leaves the phone. The app declares no internet permission at all, so it " +
                    "cannot send anything anywhere even if it wanted to. Saved figures sit in this " +
                    "app's own storage and disappear when you uninstall it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "Scope") {
            Text(
                "This is a calculator. It performs arithmetic on figures you type in. It does not " +
                    "offer credit, recommend a lender, or advise on whether to borrow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "About") {
            StatRow("Version", "1.0.0")
            HairlineDivider()
            StatRow("Built by", "MoneyClarity")
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { tap.commit(); Export.shareApp(context) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share the app") }
            Spacer(Modifier.height(14.dp))
            // The publisher, named as itself rather than folded into a generic
            // "Website" button, since this is a link to a different property
            // with its own brand, not another screen of this app.
            Text(
                "From the people behind",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            BrandPill(onClick = onOpenSite)
        }
    }
}
