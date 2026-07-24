package com.moneyclarity.calc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Paper,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDeep,
    secondary = Amber,
    onSecondary = Paper,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Ink,
    tertiary = Positive,
    background = Paper,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Alert,
    onError = Paper
)

private val DarkScheme = darkColorScheme(
    primary = NightTeal,
    onPrimary = NightBase,
    primaryContainer = NightTealContainer,
    onPrimaryContainer = NightTeal,
    secondary = NightAmber,
    onSecondary = NightBase,
    secondaryContainer = NightAmberContainer,
    onSecondaryContainer = NightAmber,
    tertiary = NightPositive,
    background = NightBase,
    onBackground = NightInk,
    surface = NightCard,
    onSurface = NightInk,
    surfaceVariant = NightBase,
    onSurfaceVariant = NightInkSoft,
    outline = NightHairline,
    outlineVariant = NightHairline,
    error = NightAlert,
    onError = NightBase
)


@Composable
fun MoneyClarityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor =
                (if (darkTheme) scheme.surface else scheme.primary).toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
