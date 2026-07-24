package com.moneyclarity.calc.engine

/**
 * Income tax for the tax year 2026-27, assessment year 2027-28.
 *
 * The Union Budget 2026 left the slabs announced in Budget 2025 in force, so the
 * new regime bands, the sixty thousand rupee rebate and the seventy five
 * thousand rupee standard deduction all carry forward unchanged.
 *
 * A useful internal check on these figures: tax on taxable income of exactly
 * twelve lakh under the new regime comes to sixty thousand, which is precisely
 * the rebate. Income up to twelve lakh therefore falls to nil, which is the
 * stated intent of the provision.
 */
object IncomeTax {

    const val NEW_STANDARD_DEDUCTION = 75000.0
    const val OLD_STANDARD_DEDUCTION = 50000.0
    const val CESS = 0.04

    private val newSlabs = listOf(
        400000.0 to 0.0,
        800000.0 to 5.0,
        1200000.0 to 10.0,
        1600000.0 to 15.0,
        2000000.0 to 20.0,
        2400000.0 to 25.0,
        Double.MAX_VALUE to 30.0
    )

    private val oldSlabs = listOf(
        250000.0 to 0.0,
        500000.0 to 5.0,
        1000000.0 to 20.0,
        Double.MAX_VALUE to 30.0
    )

    private fun slabTax(taxable: Double, slabs: List<Pair<Double, Double>>): Double {
        var tax = 0.0
        var lower = 0.0
        for ((upper, rate) in slabs) {
            if (taxable <= lower) break
            val band = minOf(taxable, upper) - lower
            if (band > 0) tax += band * rate / 100.0
            lower = upper
        }
        return tax
    }

    private fun surchargeRate(totalIncome: Double, newRegime: Boolean): Double = when {
        totalIncome > 50000000 -> if (newRegime) 25.0 else 37.0
        totalIncome > 20000000 -> 25.0
        totalIncome > 10000000 -> 15.0
        totalIncome > 5000000 -> 10.0
        else -> 0.0
    }

    data class TaxResult(
        val taxableIncome: Double,
        val slabTax: Double,
        val rebate: Double,
        val surcharge: Double,
        val cess: Double,
        val total: Double,
        val effectiveRate: Double
    )

    fun compute(
        grossIncome: Double,
        deductions: Double,
        newRegime: Boolean,
        salaried: Boolean = true
    ): TaxResult {
        val standard = if (!salaried) 0.0
        else if (newRegime) NEW_STANDARD_DEDUCTION else OLD_STANDARD_DEDUCTION

        // Only the old regime recognises the chapter six A deductions.
        val allowed = if (newRegime) 0.0 else deductions
        val taxable = (grossIncome - standard - allowed).coerceAtLeast(0.0)

        val slabs = if (newRegime) newSlabs else oldSlabs
        val base = slabTax(taxable, slabs)

        val rebate = when {
            newRegime && taxable <= 1200000 -> minOf(base, 60000.0)
            !newRegime && taxable <= 500000 -> minOf(base, 12500.0)
            else -> 0.0
        }

        val afterRebate = (base - rebate).coerceAtLeast(0.0)
        val surcharge = afterRebate * surchargeRate(taxable, newRegime) / 100.0
        val cess = (afterRebate + surcharge) * CESS
        val total = afterRebate + surcharge + cess

        return TaxResult(
            taxableIncome = taxable,
            slabTax = base,
            rebate = rebate,
            surcharge = surcharge,
            cess = cess,
            total = total,
            effectiveRate = if (grossIncome > 0) total / grossIncome * 100.0 else 0.0
        )
    }
}

/**
 * Capital gains on the rates that took effect from July 2024: listed equity and
 * equity funds at twelve and a half percent long term above the yearly
 * exemption, twenty percent short term, and other assets at twelve and a half
 * percent long term without indexation.
 */
object CapitalGains {

    const val EQUITY_EXEMPTION = 125000.0

    data class GainResult(
        val gain: Double,
        val exempt: Double,
        val taxable: Double,
        val rate: Double,
        val tax: Double,
        val cess: Double,
        val total: Double,
        val label: String
    )

    fun compute(
        buyValue: Double,
        sellValue: Double,
        equity: Boolean,
        longTerm: Boolean
    ): GainResult {
        val gain = (sellValue - buyValue).coerceAtLeast(0.0)
        val exempt = if (equity && longTerm) minOf(gain, EQUITY_EXEMPTION) else 0.0
        val taxable = (gain - exempt).coerceAtLeast(0.0)
        val rate = when {
            equity && longTerm -> 12.5
            equity -> 20.0
            longTerm -> 12.5
            else -> 30.0
        }
        val tax = taxable * rate / 100.0
        val cess = tax * IncomeTax.CESS
        val label = when {
            equity && longTerm -> "Listed equity held over a year"
            equity -> "Listed equity held under a year"
            longTerm -> "Other assets held long term"
            else -> "Other assets held short term"
        }
        return GainResult(gain, exempt, taxable, rate, tax, cess, tax + cess, label)
    }
}
