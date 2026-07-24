package com.moneyclarity.calc.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Touch feedback, routed through the view rather than the vibrator service.
 *
 * This distinction is the whole reason haptics can exist in this app at all.
 * Driving the vibrator directly would require the VIBRATE permission, and the
 * manifest declaring no permissions whatsoever is a stated selling point.
 * View.performHapticFeedback needs none: it asks the window to produce the same
 * feedback the keyboard and system controls already produce.
 *
 * It also means the phone's own haptics setting is respected. If a person has
 * turned touch feedback off system-wide, nothing here overrides that, because
 * the FLAG_IGNORE_GLOBAL_SETTING flag is deliberately not passed.
 *
 * Three weights, used consistently:
 *   tick   - a value moved by one step
 *   select - a choice was made
 *   commit - something was worked out, saved or sent
 */
class Haptics(private val view: View?, private val enabled: Boolean) {

    private fun fire(constant: Int) {
        if (!enabled) return
        view?.performHapticFeedback(constant)
    }

    /** The lightest available. Used by the plus and minus steppers. */
    fun tick() = fire(HapticFeedbackConstants.CLOCK_TICK)

    /** Segmented controls, tiles, anything that changes what is on screen. */
    fun select() = fire(HapticFeedbackConstants.KEYBOARD_TAP)

    /** Reserved for the end of an action: saved, shared, or a result resolved. */
    fun commit() = fire(HapticFeedbackConstants.LONG_PRESS)

    companion object {
        val Off = Haptics(null, false)
    }
}

/**
 * Held at the root so the settings switch reaches every control at once,
 * without threading a flag through several layers of composables.
 */
val LocalHaptics = compositionLocalOf { Haptics.Off }

@Composable
fun haptics(): Haptics = LocalHaptics.current

/** Builds the instance the root provides. */
@Composable
fun rememberHaptics(enabled: Boolean): Haptics = Haptics(LocalView.current, enabled)
