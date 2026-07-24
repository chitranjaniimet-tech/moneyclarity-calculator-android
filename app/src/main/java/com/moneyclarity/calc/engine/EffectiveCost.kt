package com.moneyclarity.calc.engine

import kotlin.math.pow

/**
 * Internal rate of return by bisection. Slower than Newton-Raphson but it cannot
 * diverge, which matters more than speed for a schedule of at most a few hundred
 * cashflows. Convention: index 0 is time zero, positive = money reaching the
 * borrower, negative = money leaving.
 */
object Irr {

    fun npv(monthlyRate: Double, cashflows: List<Double>): Double {
        var acc = 0.0
        for (t in cashflows.indices) {
            acc += cashflows[t] / (1.0 + monthlyRate).pow(t.toDouble())
        }
        return acc
    }

    /** Returns the monthly rate, or null when no sign change exists in range. */
    fun monthly(cashflows: List<Double>): Double? {
        if (cashflows.size < 2) return null
        var lo = -0.9999
        var hi = 3.0
        var fLo = npv(lo, cashflows)
        val fHi = npv(hi, cashflows)
        if (fLo.isNaN() || fHi.isNaN()) return null
        if (fLo * fHi > 0) return null
        repeat(240) {
            val mid = (lo + hi) / 2.0
            val fMid = npv(mid, cashflows)
            if (fLo * fMid <= 0) {
                hi = mid
            } else {
                lo = mid
                fLo = fMid
            }
        }
        return (lo + hi) / 2.0
    }

    /** Nominal annual rate, the basis Indian lenders quote on. */
    fun annualPct(cashflows: List<Double>): Double? = monthly(cashflows)?.let { it * 12.0 * 100.0 }
}

enum class RateType { REDUCING, FLAT }

data class CostInput(
    val amount: Double,
    val quotedRate: Double,
    val rateType: RateType,
    val months: Int,
    val processingFee: Double = 0.0,
    val gstOnFee: Boolean = true,
    val insurance: Double = 0.0,
    val otherCharges: Double = 0.0,
    val advanceEmis: Int = 0
)

data class CostResult(
    val instalment: Double,
    val quotedRate: Double,
    val effectiveRate: Double?,
    val totalCharges: Double,
    val netReceived: Double,
    val totalInterest: Double,
    val totalOutgo: Double,
    val gapPct: Double?
)

object EffectiveCost {

    const val GST_RATE = 0.18

    fun compute(input: CostInput): CostResult {
        val months = input.months.coerceAtLeast(1)
        val amount = input.amount.coerceAtLeast(0.0)

        val instalment = when (input.rateType) {
            RateType.REDUCING -> Finance.emi(amount, input.quotedRate, months)
            RateType.FLAT -> Finance.flatEmi(amount, input.quotedRate, months)
        }

        val fee = if (input.gstOnFee) input.processingFee * (1 + GST_RATE) else input.processingFee
        val charges = fee + input.insurance + input.otherCharges

        val advance = input.advanceEmis.coerceIn(0, months - 1)
        val netReceived = amount - charges - advance * instalment

        // Advance instalments are collected at disbursal, so the running schedule
        // is shorter by exactly that many months.
        val cashflows = mutableListOf(netReceived)
        repeat(months - advance) { cashflows.add(-instalment) }

        val effective = Irr.annualPct(cashflows)
        val totalOutgo = instalment * months + charges
        val totalInterest = instalment * months - amount

        return CostResult(
            instalment = instalment,
            quotedRate = input.quotedRate,
            effectiveRate = effective,
            totalCharges = charges,
            netReceived = netReceived,
            totalInterest = totalInterest,
            totalOutgo = totalOutgo,
            gapPct = effective?.let { it - input.quotedRate }
        )
    }
}

data class NoCostInput(
    val price: Double,
    val months: Int,
    val discountForgone: Double,
    val processingFee: Double,
    val gstOnFee: Boolean = true
)

data class NoCostResult(
    val instalment: Double,
    val cashPrice: Double,
    val realCost: Double,
    val effectiveRate: Double?
)

/**
 * "No cost EMI" is normally the cash discount you give up, plus a processing fee,
 * repackaged as zero interest. Comparing the instalment stream against the price
 * you could have paid outright is what surfaces the real rate.
 */
object NoCostEmi {

    fun compute(input: NoCostInput): NoCostResult {
        val months = input.months.coerceAtLeast(1)
        val instalment = input.price / months
        val fee = if (input.gstOnFee) input.processingFee * (1 + EffectiveCost.GST_RATE) else input.processingFee
        val cashPrice = input.price - input.discountForgone
        val netReceived = cashPrice - fee

        val cashflows = mutableListOf(netReceived)
        repeat(months) { cashflows.add(-instalment) }

        return NoCostResult(
            instalment = instalment,
            cashPrice = cashPrice,
            realCost = input.discountForgone + fee,
            effectiveRate = Irr.annualPct(cashflows)
        )
    }
}
