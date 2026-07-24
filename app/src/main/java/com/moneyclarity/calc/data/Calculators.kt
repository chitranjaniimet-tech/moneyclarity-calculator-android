package com.moneyclarity.calc.data

import com.moneyclarity.calc.engine.CapitalGains
import com.moneyclarity.calc.engine.IncomeTax
import com.moneyclarity.calc.engine.Invest
import com.moneyclarity.calc.engine.Planning
import com.moneyclarity.calc.engine.rupeesCompact
import com.moneyclarity.calc.engine.monthsToTenure
import com.moneyclarity.calc.engine.percent
import com.moneyclarity.calc.engine.rupees
import com.moneyclarity.calc.engine.trim

data class FieldSpec(
    val key: String,
    val label: String,
    val default: String,
    val prefix: String = "₹",
    val suffix: String? = null,
    val step: Double = 1000.0,
    val min: Double = 0.0,
    val max: Double = 100_000_000.0,
    val decimals: Int = 0
)

data class ResultLine(
    val label: String,
    val value: String,
    val emphasis: Boolean = false,
    val accent: Boolean = false
)

data class CalcOutput(
    val heroLabel: String,
    val heroValue: String,
    val heroCaption: String? = null,
    val lines: List<ResultLine> = emptyList(),
    /** Contributed versus earned, for the split bar. */
    val split: Pair<Double, Double>? = null,
    val note: String? = null
)

data class CalculatorSpec(
    val id: String,
    val title: String,
    val group: String,
    val blurb: String,
    val fields: List<FieldSpec>,
    val compute: (Map<String, Double>) -> CalcOutput
)

private fun rate(label: String = "Expected return", d: String = "12", max: Double = 40.0) =
    FieldSpec("rate", label, d, prefix = "", suffix = "% p.a.", step = 0.5, min = 0.0, max = max, decimals = 2)

private fun years(d: String = "10", max: Double = 60.0) =
    FieldSpec("years", "Period", d, prefix = "", suffix = "years", step = 1.0, min = 1.0, max = max)

object Calculators {

    val all: List<CalculatorSpec> = listOf(

        // ---------------- Investing ----------------

        CalculatorSpec(
            "sip", "Monthly investment plan", "Investing",
            "What a fixed monthly investment grows into.",
            listOf(
                FieldSpec("amount", "Monthly investment", "10000", step = 500.0, max = 1_000_000.0),
                rate(), years()
            )
        ) { v ->
            val months = (v["years"]!! * 12).toInt()
            val fv = Invest.sipValue(v["amount"]!!, v["rate"]!!, months)
            val invested = v["amount"]!! * months
            CalcOutput(
                "Value at the end", rupees(fv),
                "${months} contributions of ${rupees(v["amount"]!!)}",
                listOf(
                    ResultLine("Total invested", rupees(invested)),
                    ResultLine("Gain", rupees(fv - invested), accent = true),
                    ResultLine("Gain as share of investment",
                        if (invested > 0) "${Math.round((fv - invested) / invested * 100)}%" else "—")
                ),
                split = Pair(invested, fv - invested)
            )
        },

        CalculatorSpec(
            "sip_target", "Investment needed for a target", "Investing",
            "Work backwards from the sum you want to end up with.",
            listOf(
                FieldSpec("target", "Target amount", "10000000", step = 100000.0, max = 500_000_000.0),
                rate(), years("15")
            )
        ) { v ->
            val months = (v["years"]!! * 12).toInt()
            val need = Invest.sipRequired(v["target"]!!, v["rate"]!!, months)
            CalcOutput(
                "Invest each month", rupees(need),
                "for ${monthsToTenure(months)} to reach ${rupees(v["target"]!!)}",
                listOf(
                    ResultLine("Total you will put in", rupees(need * months)),
                    ResultLine("Growth doing the rest", rupees(v["target"]!! - need * months), accent = true)
                ),
                split = Pair(need * months, (v["target"]!! - need * months).coerceAtLeast(0.0))
            )
        },

        CalculatorSpec(
            "lumpsum", "One-off investment", "Investing",
            "A single sum left to grow.",
            listOf(
                FieldSpec("amount", "Amount invested", "500000", step = 10000.0),
                rate(), years()
            )
        ) { v ->
            val fv = Invest.lumpsum(v["amount"]!!, v["rate"]!!, v["years"]!!)
            CalcOutput(
                "Value at the end", rupees(fv),
                "${trim(v["years"]!!, 0)} years at ${percent(v["rate"]!!)}",
                listOf(
                    ResultLine("Amount invested", rupees(v["amount"]!!)),
                    ResultLine("Gain", rupees(fv - v["amount"]!!), accent = true)
                ),
                split = Pair(v["amount"]!!, fv - v["amount"]!!)
            )
        },

        CalculatorSpec(
            "swp", "Systematic withdrawal", "Investing",
            "How long a corpus lasts while you draw from it.",
            listOf(
                FieldSpec("corpus", "Corpus", "5000000", step = 100000.0),
                FieldSpec("withdraw", "Withdrawn each month", "30000", step = 1000.0, max = 1_000_000.0),
                rate("Return on the balance", "8"), years("30")
            )
        ) { v ->
            val months = (v["years"]!! * 12).toInt()
            val (lasted, left) = Invest.swp(v["corpus"]!!, v["withdraw"]!!, v["rate"]!!, months)
            val exhausted = left <= 0.0 && lasted < months
            CalcOutput(
                if (exhausted) "Runs out after" else "Left at the end",
                if (exhausted) monthsToTenure(lasted) else rupees(left),
                if (exhausted) "Drawing ${rupees(v["withdraw"]!!)} a month"
                else "After ${monthsToTenure(months)} of withdrawals",
                listOf(
                    ResultLine("Total withdrawn", rupees(v["withdraw"]!! * lasted)),
                    ResultLine("Months sustained", "$lasted", emphasis = true)
                ),
                note = if (exhausted)
                    "The corpus is exhausted before the period you entered." else null
            )
        },

        CalculatorSpec(
            "cagr", "Compound annual growth", "Investing",
            "The yearly rate that turns one value into another.",
            listOf(
                FieldSpec("start", "Starting value", "100000", step = 10000.0),
                FieldSpec("end", "Ending value", "250000", step = 10000.0),
                years("5")
            )
        ) { v ->
            val g = Invest.cagr(v["start"]!!, v["end"]!!, v["years"]!!)
            CalcOutput(
                "Compound annual growth", percent(g),
                "over ${trim(v["years"]!!, 0)} years",
                listOf(
                    ResultLine("Absolute gain", rupees(v["end"]!! - v["start"]!!)),
                    ResultLine("Total return",
                        if (v["start"]!! > 0) "${Math.round((v["end"]!! / v["start"]!! - 1) * 100)}%" else "—",
                        accent = true)
                )
            )
        },

        // ---------------- Deposits ----------------

        CalculatorSpec(
            "fd", "Term deposit", "Deposits",
            "A fixed deposit compounded quarterly, as banks do.",
            listOf(
                FieldSpec("amount", "Amount deposited", "100000", step = 10000.0),
                rate("Interest rate", "7", 20.0), years("5", 20.0)
            )
        ) { v ->
            val maturity = Invest.fixedDeposit(v["amount"]!!, v["rate"]!!, v["years"]!!)
            CalcOutput(
                "Maturity amount", rupees(maturity),
                "${trim(v["years"]!!, 0)} years at ${percent(v["rate"]!!)}, compounded quarterly",
                listOf(
                    ResultLine("Amount deposited", rupees(v["amount"]!!)),
                    ResultLine("Interest earned", rupees(maturity - v["amount"]!!), accent = true)
                ),
                split = Pair(v["amount"]!!, maturity - v["amount"]!!),
                note = "Interest on deposits is taxable. Banks deduct tax at source once it crosses the threshold for the year."
            )
        },

        CalculatorSpec(
            "rd", "Recurring deposit", "Deposits",
            "A fixed monthly deposit with a bank or post office.",
            listOf(
                FieldSpec("amount", "Deposited each month", "5000", step = 500.0, max = 1_000_000.0),
                rate("Interest rate", "7", 20.0), years("5", 20.0)
            )
        ) { v ->
            val months = (v["years"]!! * 12).toInt()
            val maturity = Invest.recurringDeposit(v["amount"]!!, v["rate"]!!, months)
            val paid = v["amount"]!! * months
            CalcOutput(
                "Maturity amount", rupees(maturity),
                "$months deposits of ${rupees(v["amount"]!!)}",
                listOf(
                    ResultLine("Total deposited", rupees(paid)),
                    ResultLine("Interest earned", rupees(maturity - paid), accent = true)
                ),
                split = Pair(paid, maturity - paid)
            )
        },

        CalculatorSpec(
            "ppf", "Public provident fund", "Deposits",
            "Fifteen yearly deposits, compounded annually.",
            listOf(
                FieldSpec("amount", "Deposited each year", "150000", step = 10000.0, max = 150000.0),
                rate("Interest rate", "7.1", 15.0),
                FieldSpec("years", "Period", "15", prefix = "", suffix = "years", step = 5.0, min = 15.0, max = 50.0)
            )
        ) { v ->
            val yrs = v["years"]!!.toInt()
            val maturity = Invest.annualDepositScheme(v["amount"]!!, v["rate"]!!, yrs, yrs)
            val paid = v["amount"]!! * yrs
            CalcOutput(
                "Maturity amount", rupees(maturity),
                "$yrs yearly deposits at ${percent(v["rate"]!!)}",
                listOf(
                    ResultLine("Total deposited", rupees(paid)),
                    ResultLine("Interest earned", rupees(maturity - paid), accent = true)
                ),
                split = Pair(paid, maturity - paid),
                note = "The yearly deposit ceiling is ₹1,50,000. Extensions run in blocks of five years."
            )
        },

        CalculatorSpec(
            "ssy", "Girl child savings scheme", "Deposits",
            "Deposits for fifteen years, maturing at twenty one.",
            listOf(
                FieldSpec("amount", "Deposited each year", "150000", step = 10000.0, max = 150000.0),
                rate("Interest rate", "8.2", 15.0)
            )
        ) { v ->
            val maturity = Invest.annualDepositScheme(v["amount"]!!, v["rate"]!!, 15, 21)
            val paid = v["amount"]!! * 15
            CalcOutput(
                "Maturity amount", rupees(maturity),
                "15 yearly deposits, maturing after 21 years",
                listOf(
                    ResultLine("Total deposited", rupees(paid)),
                    ResultLine("Interest earned", rupees(maturity - paid), accent = true)
                ),
                split = Pair(paid, maturity - paid)
            )
        },

        // ---------------- Work and retirement ----------------

        CalculatorSpec(
            "epf", "Provident fund at retirement", "Work",
            "Employee and employer contributions compounded to the end of service.",
            listOf(
                FieldSpec("basic", "Monthly basic and dearness allowance", "25000", step = 1000.0, max = 1_000_000.0),
                rate("Interest rate", "8.25", 15.0),
                years("25", 45.0),
                FieldSpec("increment", "Yearly increase in pay", "5", prefix = "", suffix = "%", step = 1.0, max = 25.0, decimals = 1)
            )
        ) { v ->
            val (corpus, emp, empr) = Invest.providentFund(
                v["basic"]!!, 12.0, v["rate"]!!, v["years"]!!.toInt(), v["increment"]!!
            )
            CalcOutput(
                "Balance at the end", rupees(corpus),
                "${trim(v["years"]!!, 0)} years of service",
                listOf(
                    ResultLine("Your contribution", rupees(emp)),
                    ResultLine("Employer contribution", rupees(empr)),
                    ResultLine("Interest credited", rupees(corpus - emp - empr), accent = true)
                ),
                split = Pair(emp + empr, corpus - emp - empr),
                note = "The employer's pension share is 8.33% of wages up to the statutory ceiling of ₹15,000, and the balance of their 12% goes to the fund."
            )
        },

        CalculatorSpec(
            "gratuity", "Gratuity", "Work",
            "Fifteen days of pay for every completed year of service.",
            listOf(
                FieldSpec("salary", "Last drawn monthly basic and dearness allowance", "50000", step = 1000.0, max = 1_000_000.0),
                FieldSpec("years", "Years of service", "10", prefix = "", suffix = "years", step = 1.0, min = 0.0, max = 45.0)
            )
        ) { v ->
            val g = Invest.gratuity(v["salary"]!!, v["years"]!!)
            CalcOutput(
                "Gratuity payable", rupees(g),
                if (v["years"]!! < 5) "Below the five year qualifying period"
                else "${trim(v["years"]!!, 0)} completed years",
                listOf(
                    ResultLine("Formula", "15 / 26 × pay × years"),
                    ResultLine("Statutory ceiling", rupees(2000000.0))
                ),
                note = if (v["years"]!! < 5)
                    "Five years of continuous service is normally required, except where service ends through death or disablement."
                else null
            )
        },

        CalculatorSpec(
            "hra", "House rent allowance exemption", "Work",
            "The exempt portion is the least of three amounts.",
            listOf(
                FieldSpec("basic", "Yearly basic and dearness allowance", "600000", step = 50000.0),
                FieldSpec("hra", "Allowance received in the year", "300000", step = 10000.0),
                FieldSpec("rent", "Rent paid in the year", "360000", step = 10000.0),
                FieldSpec("metro", "Living in Delhi, Mumbai, Kolkata or Chennai", "1", prefix = "", suffix = "1 yes, 0 no", step = 1.0, min = 0.0, max = 1.0)
            )
        ) { v ->
            val metro = v["metro"]!! >= 0.5
            val exempt = Invest.hraExempt(v["basic"]!!, v["hra"]!!, v["rent"]!!, metro)
            CalcOutput(
                "Exempt from tax", rupees(exempt),
                "Taxable portion ${rupees(v["hra"]!! - exempt)}",
                listOf(
                    ResultLine("Allowance received", rupees(v["hra"]!!)),
                    ResultLine("Rent above a tenth of basic", rupees((v["rent"]!! - 0.10 * v["basic"]!!).coerceAtLeast(0.0))),
                    ResultLine(
                        if (metro) "Half of basic pay" else "Two fifths of basic pay",
                        rupees(v["basic"]!! * (if (metro) 0.50 else 0.40))
                    ),
                    ResultLine("Least of the three", rupees(exempt), emphasis = true, accent = true)
                ),
                note = "This relief applies under the old regime. It is not available under the new regime."
            )
        },

        CalculatorSpec(
            "nps", "National pension accumulation", "Work",
            "Contributions to sixty, and the pension the annuity share buys.",
            listOf(
                FieldSpec("amount", "Contributed each month", "5000", step = 500.0, max = 500_000.0),
                rate("Expected return", "10"),
                years("25", 42.0),
                FieldSpec("annuity", "Share used to buy an annuity", "40", prefix = "", suffix = "%", step = 5.0, min = 40.0, max = 100.0),
                FieldSpec("arate", "Annuity rate", "6", prefix = "", suffix = "%", step = 0.5, max = 12.0, decimals = 2)
            )
        ) { v ->
            val q = Invest.nationalPension(
                v["amount"]!!, v["rate"]!!, v["years"]!!.toInt(), v["annuity"]!!, v["arate"]!!
            )
            val invested = v["amount"]!! * v["years"]!! * 12
            CalcOutput(
                "Corpus at sixty", rupees(q.a),
                "Monthly pension about ${rupees(q.d)}",
                listOf(
                    ResultLine("Total contributed", rupees(invested)),
                    ResultLine("Growth", rupees(q.a - invested), accent = true),
                    ResultLine("Taken as a lump sum", rupees(q.b)),
                    ResultLine("Used to buy an annuity", rupees(q.c))
                ),
                split = Pair(invested, q.a - invested),
                note = "At least 40% of the corpus must buy an annuity on exit at sixty."
            )
        },

        CalculatorSpec(
            "retirement", "Retirement corpus", "Work",
            "What you need saved by the time you stop working.",
            listOf(
                FieldSpec("expense", "Monthly spending today", "50000", step = 5000.0, max = 2_000_000.0),
                FieldSpec("towork", "Years until you retire", "20", prefix = "", suffix = "years", step = 1.0, min = 1.0, max = 45.0),
                FieldSpec("inflation", "Inflation", "6", prefix = "", suffix = "%", step = 0.5, max = 15.0, decimals = 2),
                FieldSpec("retired", "Years spent retired", "25", prefix = "", suffix = "years", step = 1.0, min = 1.0, max = 50.0),
                rate("Return after retiring", "8")
            )
        ) { v ->
            val (expenseThen, corpus) = Invest.retirementCorpus(
                v["expense"]!!, v["towork"]!!.toInt(), v["inflation"]!!,
                v["retired"]!!.toInt(), v["rate"]!!
            )
            CalcOutput(
                "Corpus needed", rupees(corpus),
                "by the time you stop working",
                listOf(
                    ResultLine("Monthly spending then", rupees(expenseThen), accent = true),
                    ResultLine("Spending today", rupees(v["expense"]!!)),
                    ResultLine("Inflation assumed", percent(v["inflation"]!!))
                ),
                note = "Assumes spending keeps pace with inflation through retirement and the balance stays invested."
            )
        },

        // ---------------- Everyday ----------------

        CalculatorSpec(
            "simple", "Simple interest", "Everyday",
            "Interest on the original sum only.",
            listOf(
                FieldSpec("amount", "Principal", "100000", step = 10000.0),
                rate("Rate", "10", 60.0), years("3")
            )
        ) { v ->
            val si = Invest.simpleInterest(v["amount"]!!, v["rate"]!!, v["years"]!!)
            CalcOutput(
                "Interest", rupees(si),
                "Total ${rupees(v["amount"]!! + si)}",
                listOf(
                    ResultLine("Principal", rupees(v["amount"]!!)),
                    ResultLine("Amount repayable", rupees(v["amount"]!! + si), emphasis = true)
                ),
                split = Pair(v["amount"]!!, si)
            )
        },

        CalculatorSpec(
            "compound", "Compound interest", "Everyday",
            "Interest on interest, at your chosen frequency.",
            listOf(
                FieldSpec("amount", "Principal", "100000", step = 10000.0),
                rate("Rate", "10", 60.0), years("3"),
                FieldSpec("freq", "Times compounded each year", "4", prefix = "", suffix = "per year", step = 1.0, min = 1.0, max = 12.0)
            )
        ) { v ->
            val ci = Invest.compoundInterest(v["amount"]!!, v["rate"]!!, v["years"]!!, v["freq"]!!.toInt())
            CalcOutput(
                "Interest", rupees(ci),
                "Total ${rupees(v["amount"]!! + ci)}",
                listOf(
                    ResultLine("Principal", rupees(v["amount"]!!)),
                    ResultLine("Amount at the end", rupees(v["amount"]!! + ci), emphasis = true),
                    ResultLine(
                        "Simple interest would have been",
                        rupees(Invest.simpleInterest(v["amount"]!!, v["rate"]!!, v["years"]!!))
                    )
                ),
                split = Pair(v["amount"]!!, ci)
            )
        },

        CalculatorSpec(
            "gst", "Goods and services tax", "Everyday",
            "Add tax to a price, or strip it back out.",
            listOf(
                FieldSpec("amount", "Amount", "10000", step = 500.0),
                FieldSpec("rate", "Rate", "18", prefix = "", suffix = "%", step = 1.0, max = 40.0, decimals = 2),
                FieldSpec("mode", "Amount already includes tax", "0", prefix = "", suffix = "1 yes, 0 no", step = 1.0, min = 0.0, max = 1.0)
            )
        ) { v ->
            val inclusive = v["mode"]!! >= 0.5
            val (tax, other) = if (inclusive) Invest.gstRemove(v["amount"]!!, v["rate"]!!)
            else Invest.gstAdd(v["amount"]!!, v["rate"]!!)
            CalcOutput(
                if (inclusive) "Price before tax" else "Price including tax",
                rupees(other),
                "Tax at ${percent(v["rate"]!!)}",
                listOf(
                    ResultLine("Tax", rupees(tax), accent = true),
                    ResultLine("Central share", rupees(tax / 2)),
                    ResultLine("State share", rupees(tax / 2))
                )
            )
        },

        CalculatorSpec(
            "inflation", "Inflation", "Everyday",
            "What today's money will cost, and what it will be worth.",
            listOf(
                FieldSpec("amount", "Amount today", "100000", step = 10000.0),
                FieldSpec("rate", "Inflation", "6", prefix = "", suffix = "%", step = 0.5, max = 20.0, decimals = 2),
                years()
            )
        ) { v ->
            val (future, worth) = Invest.inflation(v["amount"]!!, v["rate"]!!, v["years"]!!)
            CalcOutput(
                "The same basket will cost", rupees(future),
                "in ${trim(v["years"]!!, 0)} years at ${percent(v["rate"]!!)}",
                listOf(
                    ResultLine("Amount today", rupees(v["amount"]!!)),
                    ResultLine("What that money will then be worth", rupees(worth), accent = true),
                    ResultLine("Purchasing power lost",
                        "${Math.round((1 - worth / v["amount"]!!) * 100)}%")
                )
            )
        }
        ,

        // ---------------- Tax ----------------

        CalculatorSpec(
            "tax", "Income tax, new against old", "Tax",
            "Which regime leaves you with more, for tax year 2026-27.",
            listOf(
                FieldSpec("gross", "Gross yearly income", "1500000", step = 50000.0, max = 500_000_000.0),
                FieldSpec("deductions", "Deductions you would claim under the old regime", "150000", step = 25000.0, max = 5_000_000.0)
            )
        ) { v ->
            val newTax = IncomeTax.compute(v["gross"]!!, 0.0, true, true)
            val oldTax = IncomeTax.compute(v["gross"]!!, v["deductions"]!!, false, true)
            val newWins = newTax.total <= oldTax.total
            val gap = Math.abs(newTax.total - oldTax.total)
            CalcOutput(
                if (newWins) "New regime costs less" else "Old regime costs less",
                rupees(gap),
                "less tax, on the figures entered",
                listOf(
                    ResultLine("Tax under the new regime", rupees(newTax.total),
                        emphasis = newWins, accent = newWins),
                    ResultLine("Tax under the old regime", rupees(oldTax.total),
                        emphasis = !newWins, accent = !newWins),
                    ResultLine("Taxable income, new", rupees(newTax.taxableIncome)),
                    ResultLine("Taxable income, old", rupees(oldTax.taxableIncome)),
                    ResultLine("Effective rate, whichever you pick",
                        percent(if (newWins) newTax.effectiveRate else oldTax.effectiveRate))
                ),
                note = "Slabs for tax year 2026-27. The new regime carries a ₹75,000 standard deduction and a ₹60,000 rebate that clears tax up to ₹12,00,000 taxable. The old regime carries ₹50,000 and allows the deductions you enter above. Verify against the department before filing."
            )
        },

        CalculatorSpec(
            "capgains", "Capital gains", "Tax",
            "Tax on a sale of shares, funds or other assets.",
            listOf(
                FieldSpec("buy", "Purchase value", "500000", step = 25000.0),
                FieldSpec("sell", "Sale value", "800000", step = 25000.0),
                FieldSpec("equity", "Listed shares or equity funds", "1", prefix = "", suffix = "1 yes, 0 no", step = 1.0, min = 0.0, max = 1.0),
                FieldSpec("long", "Held beyond the long term threshold", "1", prefix = "", suffix = "1 yes, 0 no", step = 1.0, min = 0.0, max = 1.0)
            )
        ) { v ->
            val g = CapitalGains.compute(v["buy"]!!, v["sell"]!!, v["equity"]!! >= 0.5, v["long"]!! >= 0.5)
            CalcOutput(
                "Tax payable", rupees(g.total), g.label,
                listOf(
                    ResultLine("Gain", rupees(g.gain)),
                    ResultLine("Exempt", rupees(g.exempt)),
                    ResultLine("Taxed at", percent(g.rate), accent = true),
                    ResultLine("Cess", rupees(g.cess)),
                    ResultLine("Net in hand after tax", rupees(v["sell"]!! - g.total), emphasis = true)
                ),
                note = "Listed equity held beyond a year carries a yearly exemption of ₹1,25,000, then 12.5%. Held under a year it is 20%."
            )
        },

        // ---------------- Borrowing ----------------

        CalculatorSpec(
            "eligibility", "How much you can borrow", "Borrowing",
            "Working back from income and existing obligations.",
            listOf(
                FieldSpec("income", "Monthly income after tax", "100000", step = 5000.0, max = 5_000_000.0),
                FieldSpec("obligations", "Instalments you already pay", "15000", step = 2500.0, max = 2_000_000.0),
                FieldSpec("foir", "Share of income lenders allow", "55", prefix = "", suffix = "%", step = 5.0, min = 20.0, max = 80.0),
                rate("Interest rate", "9", 30.0),
                FieldSpec("years", "Tenure", "20", prefix = "", suffix = "years", step = 1.0, min = 1.0, max = 30.0)
            )
        ) { v ->
            val (amount, headroom, ceiling) = Planning.eligibility(
                v["income"]!!, v["obligations"]!!, v["foir"]!!, v["rate"]!!, (v["years"]!! * 12).toInt()
            )
            CalcOutput(
                "Indicative loan amount", rupees(amount),
                "on an instalment of ${rupees(headroom)}",
                listOf(
                    ResultLine("Ceiling on total instalments", rupees(ceiling)),
                    ResultLine("Already committed", rupees(v["obligations"]!!)),
                    ResultLine("Room for a new instalment", rupees(headroom), emphasis = true, accent = true)
                ),
                note = "The share of income a lender allows varies with income band, profile and product. This is an estimate, not a sanction."
            )
        },

        CalculatorSpec(
            "card", "Credit card payoff", "Borrowing",
            "How long a card balance takes to clear, and what it costs.",
            listOf(
                FieldSpec("balance", "Outstanding balance", "100000", step = 5000.0, max = 5_000_000.0),
                FieldSpec("rate", "Monthly rate charged", "3.5", prefix = "", suffix = "% per month", step = 0.1, min = 0.1, max = 6.0, decimals = 2),
                FieldSpec("payment", "Paid each month", "5000", step = 1000.0, max = 1_000_000.0)
            )
        ) { v ->
            val (months, interest, clears) = Planning.cardPayoff(v["balance"]!!, v["rate"]!!, v["payment"]!!)
            CalcOutput(
                if (clears) "Clear in" else "Never clears",
                if (clears) monthsToTenure(months) else "—",
                if (clears) "Paying ${rupees(v["payment"]!!)} a month" else "The payment is below the interest charged",
                if (clears) listOf(
                    ResultLine("Interest paid", rupees(interest), accent = true),
                    ResultLine("Total paid", rupees(v["balance"]!! + interest), emphasis = true),
                    ResultLine("Yearly equivalent rate", percent(v["rate"]!! * 12))
                ) else listOf(
                    ResultLine("Interest each month", rupees(v["balance"]!! * v["rate"]!! / 100.0), accent = true),
                    ResultLine("Your payment", rupees(v["payment"]!!))
                ),
                note = if (clears) null else "While the payment is smaller than the monthly interest the balance grows no matter how long you keep paying."
            )
        },

        CalculatorSpec(
            "rentbuy", "Renting against buying", "Borrowing",
            "Both paths over the same period, at the same assumptions.",
            listOf(
                FieldSpec("price", "Property price", "8000000", step = 500000.0, max = 500_000_000.0),
                FieldSpec("down", "Money down", "1600000", step = 100000.0, max = 100_000_000.0),
                rate("Loan rate", "8.5", 20.0),
                FieldSpec("years", "Period held", "20", prefix = "", suffix = "years", step = 1.0, min = 1.0, max = 30.0),
                FieldSpec("rent", "Rent each month", "25000", step = 2500.0, max = 1_000_000.0),
                FieldSpec("rentrise", "Rent rises each year by", "7", prefix = "", suffix = "%", step = 1.0, max = 20.0, decimals = 1),
                FieldSpec("appreciation", "Property appreciates by", "6", prefix = "", suffix = "%", step = 0.5, max = 20.0, decimals = 1),
                FieldSpec("invest", "Return if invested instead", "11", prefix = "", suffix = "%", step = 0.5, max = 25.0, decimals = 1),
                FieldSpec("costs", "Stamp duty, registration and costs", "8", prefix = "", suffix = "% of price", step = 0.5, max = 20.0, decimals = 1)
            )
        ) { v ->
            val months = (v["years"]!! * 12).toInt()
            val q = Planning.rentVsBuy(
                v["price"]!!, v["down"]!!, v["rate"]!!, months, v["rent"]!!,
                v["rentrise"]!!, v["appreciation"]!!, v["invest"]!!, v["costs"]!!
            )
            val buyAhead = q.a >= q.b
            CalcOutput(
                if (buyAhead) "Buying ends ahead by" else "Renting ends ahead by",
                rupees(Math.abs(q.a - q.b)),
                "after ${trim(v["years"]!!, 0)} years, on these assumptions",
                listOf(
                    ResultLine("Monthly instalment if you buy", rupees(q.c)),
                    ResultLine("Property worth then", rupees(q.d)),
                    ResultLine("Net position, buying", rupees(q.a), emphasis = buyAhead, accent = buyAhead),
                    ResultLine("Net position, renting and investing", rupees(q.b), emphasis = !buyAhead, accent = !buyAhead)
                ),
                note = "The answer swings hard on the appreciation and investment return you assume. Change either and the conclusion can flip, which is the honest finding here."
            )
        },

        // ---------------- More investing ----------------

        CalculatorSpec(
            "stepup", "Investment that rises each year", "Investing",
            "A monthly plan stepped up with your income.",
            listOf(
                FieldSpec("amount", "Monthly investment to begin", "10000", step = 500.0, max = 1_000_000.0),
                rate(), years(),
                FieldSpec("stepup", "Increase each year by", "10", prefix = "", suffix = "%", step = 1.0, max = 50.0, decimals = 1)
            )
        ) { v ->
            val (fv, invested) = Planning.stepUpSip(
                v["amount"]!!, v["rate"]!!, v["years"]!!.toInt(), v["stepup"]!!
            )
            val flat = Invest.sipValue(v["amount"]!!, v["rate"]!!, (v["years"]!! * 12).toInt())
            CalcOutput(
                "Value at the end", rupees(fv),
                "Stepping up ${percent(v["stepup"]!!)} a year",
                listOf(
                    ResultLine("Total invested", rupees(invested)),
                    ResultLine("Gain", rupees(fv - invested), accent = true),
                    ResultLine("Without stepping up", rupees(flat)),
                    ResultLine("Stepping up adds", rupees(fv - flat), emphasis = true)
                ),
                split = Pair(invested, fv - invested)
            )
        },

        CalculatorSpec(
            "fire", "Financial independence", "Investing",
            "When invested savings can cover your living costs.",
            listOf(
                FieldSpec("savings", "Invested already", "1000000", step = 100000.0),
                FieldSpec("monthly", "Investing each month", "50000", step = 5000.0, max = 2_000_000.0),
                FieldSpec("expense", "Monthly spending", "60000", step = 5000.0, max = 2_000_000.0),
                rate("Expected return", "12"),
                FieldSpec("withdraw", "Safe withdrawal rate", "4", prefix = "", suffix = "%", step = 0.25, min = 2.0, max = 8.0, decimals = 2)
            )
        ) { v ->
            val (target, months) = Planning.financialIndependence(
                v["savings"]!!, v["monthly"]!!, v["expense"]!!, v["rate"]!!, v["withdraw"]!!
            )
            CalcOutput(
                if (months < 0) "Not reached within a century" else if (months == 0) "Already there" else "Reached in",
                if (months <= 0) rupees(target) else monthsToTenure(months),
                "Target corpus ${rupees(target)}",
                listOf(
                    ResultLine("Corpus needed", rupees(target), emphasis = true),
                    ResultLine("Invested today", rupees(v["savings"]!!)),
                    ResultLine("Still to build", rupees((target - v["savings"]!!).coerceAtLeast(0.0)), accent = true)
                ),
                note = "The withdrawal rate is a rule of thumb drawn from long historical runs, not a guarantee."
            )
        },

        // ---------------- More deposits ----------------

        CalculatorSpec(
            "nsc", "Savings certificate", "Deposits",
            "A fixed deposit with the post office, compounded yearly.",
            listOf(
                FieldSpec("amount", "Amount deposited", "100000", step = 10000.0),
                rate("Interest rate", "7.7", 15.0),
                FieldSpec("years", "Period", "5", prefix = "", suffix = "years", step = 1.0, min = 1.0, max = 20.0)
            )
        ) { v ->
            val m = Planning.nsc(v["amount"]!!, v["rate"]!!, v["years"]!!)
            CalcOutput(
                "Maturity amount", rupees(m),
                "${trim(v["years"]!!, 0)} years at ${percent(v["rate"]!!)}",
                listOf(
                    ResultLine("Deposited", rupees(v["amount"]!!)),
                    ResultLine("Interest earned", rupees(m - v["amount"]!!), accent = true),
                    ResultLine("Money doubles in", "${trim(Planning.yearsToDouble(v["rate"]!!), 1)} years")
                ),
                split = Pair(v["amount"]!!, m - v["amount"]!!)
            )
        },

        CalculatorSpec(
            "payout", "Monthly income schemes", "Deposits",
            "Senior citizen and post office schemes that pay interest out.",
            listOf(
                FieldSpec("amount", "Amount deposited", "1500000", step = 100000.0, max = 50_000_000.0),
                rate("Interest rate", "8.2", 15.0),
                FieldSpec("freq", "Paid out", "12", prefix = "", suffix = "times a year", step = 1.0, min = 1.0, max = 12.0)
            )
        ) { v ->
            val (perPayout, yearly) = Planning.payoutScheme(v["amount"]!!, v["rate"]!!, v["freq"]!!.toInt())
            CalcOutput(
                "Each payout", rupees(perPayout),
                "${trim(v["freq"]!!, 0)} times a year",
                listOf(
                    ResultLine("Income each year", rupees(yearly), accent = true),
                    ResultLine("Capital returned at the end", rupees(v["amount"]!!)),
                    ResultLine("Income over five years", rupees(yearly * 5), emphasis = true)
                ),
                note = "Interest is paid out rather than compounded, so the capital does not grow. It is taxable in the year received."
            )
        },

        CalculatorSpec(
            "takehome", "Take-home pay", "Work",
            "What lands in the account, from the cost to company.",
            listOf(
                FieldSpec("ctc", "Yearly cost to company", "2000000", step = 100000.0, max = 200_000_000.0),
                FieldSpec("basic", "Basic as a share of the package", "50", prefix = "", suffix = "%", step = 5.0, min = 20.0, max = 70.0),
                FieldSpec("regime", "Under the new regime", "1", prefix = "", suffix = "1 yes, 0 no", step = 1.0, min = 0.0, max = 1.0),
                FieldSpec("deductions", "Deductions claimed, old regime only", "0", step = 25000.0, max = 5_000_000.0)
            )
        ) { v ->
            val q = Planning.takeHome(v["ctc"]!!, v["basic"]!!, v["regime"]!! >= 0.5, v["deductions"]!!)
            CalcOutput(
                "In hand each month", rupees(q.b),
                "from a package of ${rupeesCompact(v["ctc"]!!)}",
                listOf(
                    ResultLine("In hand each year", rupees(q.a), emphasis = true),
                    ResultLine("Income tax", rupees(q.c), accent = true),
                    ResultLine("Into the provident fund", rupees(q.d)),
                    ResultLine("Share of package reaching you",
                        "${Math.round(q.a / v["ctc"]!! * 100)}%")
                ),
                note = "Structures vary between employers. Allowances, reimbursements and variable pay are not modelled here."
            )
        }
    )


    val groups: List<String> = all.map { it.group }.distinct()

    fun byId(id: String): CalculatorSpec? = all.firstOrNull { it.id == id }
}
