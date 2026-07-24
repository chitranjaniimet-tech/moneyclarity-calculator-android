package com.moneyclarity.calc.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.moneyclarity.calc.engine.trim

/**
 * One set of loan figures shared across screens, so moving from the instalment
 * screen to the schedule or the prepayment simulator carries the numbers over
 * instead of asking for them again.
 */
class CalcState {
    var amount by mutableStateOf("500000")
    var rate by mutableStateOf("11.5")
    var months by mutableIntStateOf(60)
    var schedulePaymentOverride by mutableStateOf<Double?>(null)

    val amountValue: Double get() = amount.toDoubleOrNull() ?: 0.0
    val rateValue: Double get() = rate.toDoubleOrNull() ?: 0.0

    /**
     * A solved rate arrives from the bisection as something like 11.499999996.
     * Storing it raw would put that string straight into a field the person is
     * about to read, so it is trimmed to the two decimals a rate is ever quoted
     * in before it lands.
     */
    fun set(
        amount: Double,
        rate: Double,
        months: Int,
        schedulePaymentOverride: Double? = null
    ) {
        this.amount = amount.toLong().toString()
        this.rate = trim(rate, 2)
        this.months = months.coerceIn(1, 600)
        this.schedulePaymentOverride = schedulePaymentOverride?.takeIf { it > 0.0 }
    }
}
