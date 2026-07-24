package com.moneyclarity.calc.engine

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.abs

/**
 * Four figures describe a reducing-balance loan: amount, rate, tenure and
 * instalment. Fix any three and the fourth is determined. Most calculators only
 * walk that relationship in one direction; this one walks it in all four.
 *
 * Two of the inversions are closed-form. Tenure comes out of the instalment
 * formula by taking logs. Rate does not invert analytically at all, so it is
 * found by bisection on a function that is strictly increasing in the rate,
 * which makes the bracket reliable rather than a matter of luck.
 */
enum class SolveFor { INSTALMENT, TENURE, RATE, AMOUNT }

sealed interface Solved {
    /** [value] is rupees, months, or percent per annum depending on the target. */
    data class Ok(val value: Double) : Solved

    /** The three given figures do not describe a loan that can ever close. */
    data class Impossible(val reason: String) : Solved
}

object LoanSolve {

    /** Widest annual rate the search will consider. Beyond this the answer is noise. */
    private const val MAX_RATE = 100.0
    private const val MAX_MONTHS = 600.0

    fun principal(annualRatePct: Double, months: Int, instalment: Double): Solved {
        if (instalment <= 0.0) return Solved.Impossible("Enter an instalment above zero.")
        if (months <= 0) return Solved.Impossible("Enter a tenure of at least one month.")
        val r = annualRatePct / 1200.0
        if (r <= 0.0) return Solved.Ok(instalment * months)
        val f = (1.0 + r).pow(months.toDouble())
        return Solved.Ok(instalment * (f - 1.0) / (r * f))
    }

    /**
     * Tenure from the instalment. The instalment must clear the first month's
     * interest, otherwise the balance grows every month and the loan never ends.
     */
    fun months(principal: Double, annualRatePct: Double, instalment: Double): Solved {
        if (principal <= 0.0) return Solved.Impossible("Enter a loan amount above zero.")
        if (instalment <= 0.0) return Solved.Impossible("Enter an instalment above zero.")
        val r = annualRatePct / 1200.0
        if (r <= 0.0) return Solved.Ok(principal / instalment)

        val firstMonthInterest = principal * r
        if (instalment <= firstMonthInterest) {
            return Solved.Impossible(
                "At this rate the first month's interest alone is " +
                    "${rupees(firstMonthInterest)}. An instalment at or below that never " +
                    "reduces the balance, so the loan would never close."
            )
        }
        val n = ln(instalment / (instalment - principal * r)) / ln(1.0 + r)
        if (n > MAX_MONTHS) {
            return Solved.Impossible(
                "That works out to over 50 years. Raise the instalment to bring it into range."
            )
        }

        /*
         * The UI accepts and displays whole rupees. A perfectly ordinary EMI
         * can therefore be a few paise away from the exact formula. Feeding
         * that displayed amount back into the logarithm must not turn 240
         * months into 241 merely because the unrounded answer is 240.0018.
         *
         * Snap only when the entered whole-rupee payment is within half a
         * rupee of the EMI for the nearest whole month. This follows the same
         * rounding contract as the screen without masking a genuinely
         * fractional final instalment.
         */
        val nearestMonth = n.roundToInt().coerceAtLeast(1)
        val nearestPayment = Finance.emi(principal, annualRatePct, nearestMonth)
        if (abs(nearestPayment - instalment) <= 0.5) {
            return Solved.Ok(nearestMonth.toDouble())
        }
        return Solved.Ok(n)
    }

    /**
     * Rate from the instalment, by bisection. Finance.emi is strictly increasing
     * in the rate for a fixed amount and tenure, so a sign change between the
     * ends of the bracket guarantees exactly one root inside it.
     */
    fun rate(principal: Double, months: Int, instalment: Double): Solved {
        if (principal <= 0.0) return Solved.Impossible("Enter a loan amount above zero.")
        if (months <= 0) return Solved.Impossible("Enter a tenure of at least one month.")
        val interestFree = principal / months
        if (instalment < interestFree - 0.005) {
            return Solved.Impossible(
                "Even at zero interest this loan needs ${rupees(interestFree)} a month. " +
                    "No rate can produce an instalment below that."
            )
        }
        if (instalment <= interestFree + 1e-9) return Solved.Ok(0.0)

        val ceiling = Finance.emi(principal, MAX_RATE, months)
        if (instalment > ceiling) {
            return Solved.Impossible(
                "That instalment implies a rate above ${trim(MAX_RATE, 0)}% a year, " +
                    "which is outside what this calculator will report."
            )
        }

        var low = 0.0
        var high = MAX_RATE
        repeat(200) {
            val mid = (low + high) / 2.0
            if (Finance.emi(principal, mid, months) < instalment) low = mid else high = mid
        }
        return Solved.Ok((low + high) / 2.0)
    }

    /** The forward direction, wrapped so every target returns the same type. */
    fun instalment(principal: Double, annualRatePct: Double, months: Int): Solved {
        if (principal <= 0.0) return Solved.Impossible("Enter a loan amount above zero.")
        if (months <= 0) return Solved.Impossible("Enter a tenure of at least one month.")
        return Solved.Ok(Finance.emi(principal, annualRatePct, months))
    }
}
