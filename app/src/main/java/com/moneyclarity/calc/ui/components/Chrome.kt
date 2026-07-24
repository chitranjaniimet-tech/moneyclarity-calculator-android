package com.moneyclarity.calc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lets calculator screens tell the shared app chrome when a worked result is
 * on screen. The floating publisher chip can then leave the stage to the
 * figures without every screen needing to know how that chip is drawn.
 */
val LocalCalculationResultReporter = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
fun ReportCalculationResult(visible: Boolean) {
    val report = LocalCalculationResultReporter.current
    SideEffect { report(visible) }
    DisposableEffect(report) {
        onDispose { report(false) }
    }
}
