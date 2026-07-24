package com.moneyclarity.calc.engine

import kotlin.math.ln
import kotlin.math.pow

object Planning {

    /**
     * Borrowing capacity from income. Lenders cap total instalments as a share
     * of net income, so the headroom left after existing obligations is what
     * decides the sanction.
     */
    fun eligibility(
        monthlyNetIncome: Double,
        existingObligations: Double,
        foirPct: Double,
        annualRatePct: Double,
        months: Int
    ): Triple<Double, Double, Double> {
        val ceiling = monthlyNetIncome * foirPct / 100.0
        val headroom = (ceiling - existingObligations).coerceAtLeast(0.0)
        val r = annualRatePct / 12.0 / 100.0
        val amount = if (r <= 0.0) headroom * months
        else headroom * ((1 + r).pow(months.toDouble()) - 1) / (r * (1 + r).pow(months.toDouble()))
        return Triple(amount, headroom, ceiling)
    }

    /**
     * Take-home pay from cost to company. Provident fund is deducted from both
     * sides, gratuity is set aside by the employer, and tax is charged on the
     * balance.
     */
    fun takeHome(
        annualCtc: Double,
        basicSharePct: Double,
        newRegime: Boolean,
        otherDeductions: Double
    ): Quad {
        val basic = annualCtc * basicSharePct / 100.0
        val employerPf = basic * 0.12
        val employeePf = basic * 0.12
        val gratuityAccrual = basic * 15.0 / 26.0 / 12.0
        val grossSalary = annualCtc - employerPf - gratuityAccrual
        val tax = IncomeTax.compute(grossSalary, otherDeductions + employeePf, newRegime, true).total
        val takeHomeAnnual = grossSalary - employeePf - tax
        return Quad(takeHomeAnnual, takeHomeAnnual / 12.0, tax, employeePf + employerPf)
    }

    /**
     * Clearing a revolving card balance. Interest is charged monthly on the
     * outstanding, so a payment barely above the minimum stretches the debt for
     * years.
     */
    fun cardPayoff(
        balance: Double,
        monthlyRatePct: Double,
        monthlyPayment: Double
    ): Triple<Int, Double, Boolean> {
        val r = monthlyRatePct / 100.0
        if (monthlyPayment <= balance * r) return Triple(0, 0.0, false)
        var bal = balance
        var interest = 0.0
        var months = 0
        while (bal > 0.005 && months < 1200) {
            months++
            val i = bal * r
            interest += i
            bal = bal + i - monthlyPayment
            if (bal < 0) bal = 0.0
        }
        return Triple(months, interest, true)
    }

    /**
     * Renting against buying over a holding period. Buying carries the
     * instalment, the upfront costs and upkeep; renting frees the deposit to be
     * invested, and rent itself rises with inflation.
     */
    fun rentVsBuy(
        propertyPrice: Double,
        downPayment: Double,
        annualRatePct: Double,
        months: Int,
        monthlyRent: Double,
        rentRisePct: Double,
        appreciationPct: Double,
        investmentReturnPct: Double,
        upfrontCostPct: Double
    ): Quad {
        val loan = (propertyPrice - downPayment).coerceAtLeast(0.0)
        val instalment = Finance.emi(loan, annualRatePct, months)
        val upfront = propertyPrice * upfrontCostPct / 100.0
        val years = months / 12.0

        val totalPaid = instalment * months + downPayment + upfront
        val propertyValue = propertyPrice * (1 + appreciationPct / 100.0).pow(years)
        val buyNet = propertyValue - totalPaid

        var rentTotal = 0.0
        var rent = monthlyRent
        for (y in 1..(years.toInt().coerceAtLeast(1))) {
            rentTotal += rent * 12
            rent *= (1 + rentRisePct / 100.0)
        }
        // The deposit and the monthly difference are invested instead.
        val investedCorpus = (downPayment + upfront) * (1 + investmentReturnPct / 100.0).pow(years)
        val monthlyDifference = (instalment - monthlyRent).coerceAtLeast(0.0)
        val sipCorpus = Invest.sipValue(monthlyDifference, investmentReturnPct, months)
        val rentNet = investedCorpus + sipCorpus - rentTotal

        return Quad(buyNet, rentNet, instalment, propertyValue)
    }

    /**
     * Years until invested savings can cover living costs indefinitely, on the
     * common assumption that a portfolio can sustain a fixed withdrawal rate.
     */
    fun financialIndependence(
        currentSavings: Double,
        monthlyInvestment: Double,
        monthlyExpense: Double,
        returnPct: Double,
        withdrawalRatePct: Double
    ): Pair<Double, Int> {
        val target = monthlyExpense * 12 * 100.0 / withdrawalRatePct
        val i = returnPct / 12.0 / 100.0
        if (currentSavings >= target) return Pair(target, 0)
        if (monthlyInvestment <= 0 && i <= 0) return Pair(target, -1)
        var months = 0
        var balance = currentSavings
        while (balance < target && months < 1200) {
            months++
            balance = balance * (1 + i) + monthlyInvestment
        }
        return Pair(target, if (months >= 1200) -1 else months)
    }

    /** Small savings schemes that pay a fixed rate with a set compounding basis. */
    fun nsc(principal: Double, ratePct: Double, years: Double): Double =
        principal * (1 + ratePct / 100.0).pow(years)

    /** Doubling period at a given rate, which is how the savings certificate is framed. */
    fun yearsToDouble(ratePct: Double): Double =
        if (ratePct <= 0) 0.0 else ln(2.0) / ln(1 + ratePct / 100.0)

    /** Schemes that pay interest out rather than compounding it. */
    fun payoutScheme(principal: Double, ratePct: Double, payoutsPerYear: Int): Pair<Double, Double> {
        val perPayout = principal * ratePct / 100.0 / payoutsPerYear
        return Pair(perPayout, principal * ratePct / 100.0)
    }

    /** Monthly investment that rises by a fixed percentage each year. */
    fun stepUpSip(
        monthly: Double,
        annualReturnPct: Double,
        years: Int,
        stepUpPct: Double
    ): Pair<Double, Double> {
        val i = annualReturnPct / 12.0 / 100.0
        var balance = 0.0
        var contribution = monthly
        var invested = 0.0
        for (year in 1..years) {
            for (month in 1..12) {
                balance = (balance + contribution) * (1 + i)
                invested += contribution
            }
            contribution *= (1 + stepUpPct / 100.0)
        }
        return Pair(balance, invested)
    }
}
