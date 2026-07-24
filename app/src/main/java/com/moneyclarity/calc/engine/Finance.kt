package com.moneyclarity.calc.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

data class ScheduleRow(
    val month: Int,
    val opening: Double,
    val payment: Double,
    val interest: Double,
    val principal: Double,
    val closing: Double
)

data class FinancialYear(
    val label: String,
    val interest: Double,
    val principal: Double,
    val paid: Double,
    val closing: Double
)

object Finance {

    /** Standard reducing-balance instalment. */
    fun emi(principal: Double, annualRatePct: Double, months: Int): Double {
        if (principal <= 0.0 || months <= 0) return 0.0
        val r = annualRatePct / 12.0 / 100.0
        if (r <= 0.0) return principal / months
        val f = (1.0 + r).pow(months.toDouble())
        return principal * r * f / (f - 1.0)
    }

    /**
     * Flat rate: interest is charged on the full sanctioned amount for the whole
     * tenure, regardless of how much has already been repaid. This is how most
     * vehicle, consumer-durable and many gold facilities are quoted.
     */
    fun flatTotalInterest(principal: Double, flatAnnualPct: Double, months: Int): Double =
        principal * (flatAnnualPct / 100.0) * (months / 12.0)

    fun flatEmi(principal: Double, flatAnnualPct: Double, months: Int): Double {
        if (months <= 0) return 0.0
        return (principal + flatTotalInterest(principal, flatAnnualPct, months)) / months
    }

    /** Full month-by-month schedule on reducing balance. */
    fun schedule(principal: Double, annualRatePct: Double, months: Int): List<ScheduleRow> {
        val rows = mutableListOf<ScheduleRow>()
        if (principal <= 0.0 || months <= 0) return rows
        val r = annualRatePct / 12.0 / 100.0
        val e = emi(principal, annualRatePct, months)
        var balance = principal
        for (m in 1..months) {
            val interest = balance * r
            var principalPart = e - interest
            var payment = e
            if (m == months || principalPart > balance) {
                principalPart = balance
                payment = balance + interest
            }
            val closing = (balance - principalPart).coerceAtLeast(0.0)
            rows.add(ScheduleRow(m, balance, payment, interest, principalPart, closing))
            balance = closing
            if (balance <= 0.0) break
        }
        return rows
    }

    /**
     * Schedule driven by a fixed payment rather than by a fixed tenure.
     *
     * When the tenure is the unknown, the honest answer is rarely a whole
     * number of months. Recomputing the instalment to fit a rounded tenure
     * would quietly change the figure the person typed, so instead the payment
     * is held exactly as entered and the final instalment absorbs the
     * remainder, which is what a lender actually does.
     *
     * The caller is expected to have established that the payment exceeds the
     * first month's interest; if it does not, this returns an empty list rather
     * than looping forever.
     */
    fun scheduleAtPayment(
        principal: Double,
        annualRatePct: Double,
        payment: Double,
        cap: Int = 600
    ): List<ScheduleRow> {
        val rows = mutableListOf<ScheduleRow>()
        if (principal <= 0.0 || payment <= 0.0) return rows
        val r = annualRatePct / 12.0 / 100.0
        if (r > 0.0 && payment <= principal * r) return rows

        var balance = principal
        var m = 1
        while (balance > 0.0 && m <= cap) {
            val interest = balance * r
            var principalPart = payment - interest
            var paid = payment
            if (principalPart >= balance) {
                principalPart = balance
                paid = balance + interest
            }
            val closing = (balance - principalPart).coerceAtLeast(0.0)
            rows.add(ScheduleRow(m, balance, paid, interest, principalPart, closing))
            balance = closing
            m++
        }
        return rows
    }

    /**
     * Fixed-payment schedule for a tenure already resolved by [LoanSolve].
     *
     * Whole-rupee UI rounding can make the entered payment differ from the
     * formula by a few paise. The regular payment is preserved for every row
     * except the closing row, which absorbs that tiny residual so the table
     * reconciles exactly to principal and still has the resolved row count.
     */
    fun scheduleAtPaymentForMonths(
        principal: Double,
        annualRatePct: Double,
        payment: Double,
        months: Int
    ): List<ScheduleRow> {
        val rows = mutableListOf<ScheduleRow>()
        if (principal <= 0.0 || payment <= 0.0 || months <= 0) return rows

        // If the only difference is whole-rupee display rounding, use the exact
        // contractual EMI internally. Every visible rupee remains identical,
        // while the final row no longer invents a ₹200-style rounding residue.
        val exactPayment = emi(principal, annualRatePct, months)
        if (abs(exactPayment - payment) <= 0.5) {
            return schedule(principal, annualRatePct, months)
        }

        val r = annualRatePct / 12.0 / 100.0
        if (r > 0.0 && payment <= principal * r) return rows

        var balance = principal
        for (m in 1..months) {
            val interest = balance * r
            var principalPart = payment - interest
            var paid = payment
            if (m == months || principalPart >= balance) {
                principalPart = balance
                paid = balance + interest
            }
            val closing = (balance - principalPart).coerceAtLeast(0.0)
            rows.add(ScheduleRow(m, balance, paid, interest, principalPart, closing))
            balance = closing
            if (balance <= 0.0) break
        }
        return rows
    }

    /**
     * Interest for the gap between disbursement and the first instalment.
     *
     * A schedule normally assumes the first period is a full month, because
     * the EMI formula itself is built on that assumption. In practice a loan
     * disbursed on the 25th with the first instalment due on the 10th only
     * accrues interest for 15 days, not thirty, so the standard schedule
     * overstates month one's interest and understates its principal.
     *
     * This is simple interest on the full principal for the actual number of
     * days elapsed, using an actual/365 day count -- the convention most
     * Indian retail lenders quote against. It is not compounded, because
     * nothing has been repaid yet for compounding to apply to.
     */
    fun stubInterest(principal: Double, annualRatePct: Double, disbursement: LocalDate, firstInstalment: LocalDate): Double {
        val days = ChronoUnit.DAYS.between(disbursement, firstInstalment)
        if (days <= 0) return 0.0
        return principal * (annualRatePct / 100.0) * (days / 365.0)
    }

    /**
     * A schedule that starts with a broken-period first instalment and then
     * proceeds exactly like [scheduleAtPayment] from month two onward: the
     * contracted EMI is held fixed, and whatever principal the shorter or
     * longer first period actually paid off is carried forward rather than
     * recomputed, so the last instalment may end up a little larger or
     * smaller than the rest to close the balance out exactly.
     *
     * The instalment itself is the ordinary EMI for [principal], [annualRatePct]
     * and [months] -- the day count changes how that first payment splits
     * between interest and principal, not what the lender quotes.
     */
    fun scheduleWithDisbursement(
        principal: Double,
        annualRatePct: Double,
        months: Int,
        disbursement: LocalDate,
        firstInstalment: LocalDate,
        cap: Int = 600
    ): List<ScheduleRow> {
        if (principal <= 0.0 || months <= 0) return emptyList()
        val payment = emi(principal, annualRatePct, months)
        return scheduleWithDisbursementAtPayment(
            principal = principal,
            annualRatePct = annualRatePct,
            payment = payment,
            disbursement = disbursement,
            firstInstalment = firstInstalment,
            cap = cap
        )
    }

    /**
     * Broken-period schedule that preserves a payment supplied by the user.
     * This is required when tenure is the unknown: rounding the solved tenure
     * and deriving a new EMI would no longer be the same loan.
     */
    fun scheduleWithDisbursementAtPayment(
        principal: Double,
        annualRatePct: Double,
        payment: Double,
        disbursement: LocalDate,
        firstInstalment: LocalDate,
        cap: Int = 600
    ): List<ScheduleRow> {
        val rows = mutableListOf<ScheduleRow>()
        if (principal <= 0.0 || payment <= 0.0 || cap <= 0) return rows
        if (!firstInstalment.isAfter(disbursement)) return rows

        val interest1 = stubInterest(principal, annualRatePct, disbursement, firstInstalment)
        if (interest1 >= payment) return rows // this instalment can't even cover the stub interest

        var principalPart1 = payment - interest1
        var paid1 = payment
        if (principalPart1 >= principal) {
            principalPart1 = principal
            paid1 = principal + interest1
        }
        val closing1 = (principal - principalPart1).coerceAtLeast(0.0)
        rows.add(ScheduleRow(1, principal, paid1, interest1, principalPart1, closing1))
        if (closing1 <= 0.0) return rows

        val rest = scheduleAtPayment(closing1, annualRatePct, payment, cap - 1)
        rest.forEach { rows.add(it.copy(month = it.month + 1)) }
        return rows
    }

    fun totalInterest(rows: List<ScheduleRow>): Double = rows.sumOf { it.interest }
    fun totalPaid(rows: List<ScheduleRow>): Double = rows.sumOf { it.payment }

    /**
     * Groups a schedule into Indian financial years (April to March).
     * [startMonth] is 1..12 for the calendar month of the first instalment.
     */
    fun byFinancialYear(
        rows: List<ScheduleRow>,
        startMonth: Int,
        startYear: Int
    ): List<FinancialYear> {
        if (rows.isEmpty()) return emptyList()
        data class Acc(var interest: Double, var principal: Double, var paid: Double, var closing: Double)

        val buckets = LinkedHashMap<String, Acc>()
        rows.forEach { row ->
            val offset = row.month - 1
            val absoluteMonth = (startMonth - 1) + offset
            val year = startYear + absoluteMonth / 12
            val month = absoluteMonth % 12 + 1
            val fyStart = if (month >= 4) year else year - 1
            val label = "FY ${fyStart}-${((fyStart + 1) % 100).toString().padStart(2, '0')}"
            val acc = buckets.getOrPut(label) { Acc(0.0, 0.0, 0.0, 0.0) }
            acc.interest += row.interest
            acc.principal += row.principal
            acc.paid += row.payment
            acc.closing = row.closing
        }
        return buckets.map { (label, acc) ->
            FinancialYear(label, acc.interest, acc.principal, acc.paid, acc.closing)
        }
    }
}
