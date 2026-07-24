package com.moneyclarity.calc.engine

import kotlin.math.pow

/**
 * Savings, investment and payroll formulas.
 *
 * Compounding conventions follow Indian practice rather than the textbook
 * default: deposits compound quarterly, recurring deposits are converted to an
 * equivalent monthly rate off that quarterly basis, and monthly investment plans
 * are treated as beginning-of-period.
 */
object Invest {

    // ---- Monthly investment plan -------------------------------------------

    /** Future value of a fixed monthly investment, contributed at period start. */
    fun sipValue(monthly: Double, annualReturnPct: Double, months: Int): Double {
        if (monthly <= 0 || months <= 0) return 0.0
        val i = annualReturnPct / 12.0 / 100.0
        if (i <= 0.0) return monthly * months
        return monthly * (((1 + i).pow(months.toDouble()) - 1) / i) * (1 + i)
    }

    /** Monthly amount required to reach a target. */
    fun sipRequired(target: Double, annualReturnPct: Double, months: Int): Double {
        if (target <= 0 || months <= 0) return 0.0
        val i = annualReturnPct / 12.0 / 100.0
        if (i <= 0.0) return target / months
        return target / ((((1 + i).pow(months.toDouble()) - 1) / i) * (1 + i))
    }

    /** One-off investment grown at an annual rate. */
    fun lumpsum(principal: Double, annualReturnPct: Double, years: Double): Double =
        principal * (1 + annualReturnPct / 100.0).pow(years)

    /**
     * Systematic withdrawal. Returns how many months the corpus lasts and what
     * is left at the end of the requested period.
     */
    fun swp(corpus: Double, monthlyWithdrawal: Double, annualReturnPct: Double, months: Int): Pair<Int, Double> {
        val i = annualReturnPct / 12.0 / 100.0
        var balance = corpus
        var survived = 0
        for (m in 1..months) {
            balance = balance * (1 + i) - monthlyWithdrawal
            if (balance <= 0) return Pair(m, 0.0)
            survived = m
        }
        return Pair(survived, balance)
    }

    // ---- Deposits ----------------------------------------------------------

    /** Term deposit, compounded quarterly as banks do. */
    fun fixedDeposit(principal: Double, annualRatePct: Double, years: Double): Double =
        principal * (1 + annualRatePct / 400.0).pow(4 * years)

    /**
     * Recurring deposit. Each instalment earns interest for the time it remains
     * on deposit, at the monthly equivalent of the quarterly compounded rate.
     */
    fun recurringDeposit(monthly: Double, annualRatePct: Double, months: Int): Double {
        if (monthly <= 0 || months <= 0) return 0.0
        val quarterly = annualRatePct / 400.0
        val i = (1 + quarterly).pow(1.0 / 3.0) - 1
        if (i <= 0.0) return monthly * months
        return monthly * (((1 + i).pow(months.toDouble()) - 1) / i) * (1 + i)
    }

    /**
     * Annual deposit scheme compounded yearly, with the deposit made at the
     * start of each year. Used for the public provident fund and for the girl
     * child savings scheme.
     */
    fun annualDepositScheme(
        yearlyDeposit: Double,
        annualRatePct: Double,
        depositYears: Int,
        maturityYears: Int
    ): Double {
        var balance = 0.0
        val r = annualRatePct / 100.0
        for (year in 1..maturityYears) {
            if (year <= depositYears) balance += yearlyDeposit
            balance *= (1 + r)
        }
        return balance
    }

    // ---- Retirement and payroll -------------------------------------------

    /**
     * Provident fund accumulation. The employee contributes 12% of basic plus
     * dearness allowance. Of the employer's 12%, the pension share is 8.33%
     * but only on wages up to the statutory ceiling, and the balance goes to
     * the provident fund.
     */
    fun providentFund(
        monthlyBasic: Double,
        employeeSharePct: Double,
        annualRatePct: Double,
        years: Int,
        annualIncrementPct: Double,
        wageCeiling: Double = 15000.0
    ): Triple<Double, Double, Double> {
        var basic = monthlyBasic
        var balance = 0.0
        var employeeTotal = 0.0
        var employerTotal = 0.0
        val r = annualRatePct / 100.0 / 12.0
        for (year in 1..years) {
            for (month in 1..12) {
                val employee = basic * employeeSharePct / 100.0
                val pensionable = minOf(basic, wageCeiling)
                val pension = pensionable * 0.0833
                val employer = (basic * 0.12 - pension).coerceAtLeast(0.0)
                employeeTotal += employee
                employerTotal += employer
                balance = (balance + employee + employer) * (1 + r)
            }
            basic *= (1 + annualIncrementPct / 100.0)
        }
        return Triple(balance, employeeTotal, employerTotal)
    }

    /** Statutory gratuity: fifteen days of pay for each completed year. */
    fun gratuity(lastDrawnMonthly: Double, yearsServed: Double, cap: Double = 2000000.0): Double {
        if (yearsServed < 5) return 0.0
        val rounded = Math.round(yearsServed).toDouble()
        return (lastDrawnMonthly * 15.0 / 26.0 * rounded).coerceAtMost(cap)
    }

    /**
     * Exempt portion of a house rent allowance: the least of the allowance
     * received, rent above a tenth of basic pay, and half of basic pay in the
     * four metro cities or two fifths elsewhere.
     */
    fun hraExempt(
        basicAnnual: Double,
        hraReceived: Double,
        rentPaid: Double,
        metro: Boolean
    ): Double {
        val a = hraReceived
        val b = (rentPaid - 0.10 * basicAnnual).coerceAtLeast(0.0)
        val c = basicAnnual * (if (metro) 0.50 else 0.40)
        return minOf(a, b, c)
    }

    /**
     * National pension accumulation, with the statutory minimum share that must
     * buy an annuity at exit.
     */
    fun nationalPension(
        monthly: Double,
        annualReturnPct: Double,
        years: Int,
        annuitySharePct: Double,
        annuityRatePct: Double
    ): Quad {
        val corpus = sipValue(monthly, annualReturnPct, years * 12)
        val annuityPart = corpus * annuitySharePct / 100.0
        val lumpSum = corpus - annuityPart
        val pension = annuityPart * annuityRatePct / 100.0 / 12.0
        return Quad(corpus, lumpSum, annuityPart, pension)
    }

    /**
     * Corpus needed at retirement to fund an inflating expense for a set number
     * of years, discounted at the real rate of return.
     */
    fun retirementCorpus(
        monthlyExpenseNow: Double,
        yearsToRetire: Int,
        inflationPct: Double,
        yearsInRetirement: Int,
        postReturnPct: Double
    ): Pair<Double, Double> {
        val f = inflationPct / 100.0
        val expenseAtRetirement = monthlyExpenseNow * (1 + f).pow(yearsToRetire.toDouble())
        val real = ((1 + postReturnPct / 100.0) / (1 + f)) - 1
        val n = yearsInRetirement * 12
        val realMonthly = (1 + real).pow(1.0 / 12.0) - 1
        val corpus = if (realMonthly <= 0.0) expenseAtRetirement * n
        else expenseAtRetirement * (1 - (1 + realMonthly).pow(-n.toDouble())) / realMonthly * (1 + realMonthly)
        return Pair(expenseAtRetirement, corpus)
    }

    // ---- Plain interest and growth ----------------------------------------

    fun simpleInterest(principal: Double, ratePct: Double, years: Double): Double =
        principal * ratePct * years / 100.0

    fun compoundInterest(principal: Double, ratePct: Double, years: Double, perYear: Int): Double =
        principal * (1 + ratePct / 100.0 / perYear).pow(perYear * years) - principal

    fun cagr(start: Double, end: Double, years: Double): Double {
        if (start <= 0 || years <= 0) return 0.0
        return ((end / start).pow(1.0 / years) - 1) * 100.0
    }

    /** What a sum today will cost, and what it will be worth, after inflation. */
    fun inflation(amount: Double, ratePct: Double, years: Double): Pair<Double, Double> {
        val factor = (1 + ratePct / 100.0).pow(years)
        return Pair(amount * factor, amount / factor)
    }

    fun gstAdd(amount: Double, ratePct: Double): Pair<Double, Double> {
        val tax = amount * ratePct / 100.0
        return Pair(tax, amount + tax)
    }

    fun gstRemove(total: Double, ratePct: Double): Pair<Double, Double> {
        val base = total / (1 + ratePct / 100.0)
        return Pair(total - base, base)
    }
}

data class Quad(val a: Double, val b: Double, val c: Double, val d: Double)
