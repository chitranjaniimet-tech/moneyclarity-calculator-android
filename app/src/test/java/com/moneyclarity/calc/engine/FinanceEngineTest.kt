package com.moneyclarity.calc.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class FinanceEngineTest {

    @Test
    fun standardScheduleClosesAndReconciles() {
        val principal = 4_450_000.0
        val rows = Finance.schedule(principal, 7.4, 240)

        assertEquals(240, rows.size)
        assertEquals(0.0, rows.last().closing, 0.01)
        assertEquals(principal, rows.sumOf { it.principal }, 0.01)
        assertEquals(
            principal + rows.sumOf { it.interest },
            rows.sumOf { it.payment },
            0.01
        )
        rows.forEach {
            assertTrue(it.opening >= 0.0)
            assertTrue(it.payment >= 0.0)
            assertTrue(it.interest >= 0.0)
            assertTrue(it.principal >= 0.0)
            assertTrue(it.closing >= 0.0)
        }
    }

    @Test
    fun zeroRateScheduleDoesNotDivideByZero() {
        val rows = Finance.schedule(120_000.0, 0.0, 12)

        assertEquals(12, rows.size)
        assertEquals(10_000.0, rows.first().payment, 0.001)
        assertEquals(0.0, Finance.totalInterest(rows), 0.001)
        assertEquals(0.0, rows.last().closing, 0.001)
    }

    @Test
    fun solvedTenureKeepsExactEnteredPayment() {
        val principal = 500_000.0
        val rate = 11.5
        val payment = 12_000.0
        val solved = LoanSolve.months(principal, rate, payment) as Solved.Ok
        val rows = Finance.scheduleAtPayment(principal, rate, payment)

        assertEquals(kotlin.math.ceil(solved.value).toInt(), rows.size)
        assertEquals(payment, rows.first().payment, 0.001)
        assertTrue(rows.last().payment <= payment)
        assertEquals(0.0, rows.last().closing, 0.001)
    }

    @Test
    fun reverseLoanSolversRoundTrip() {
        val principal = 2_500_000.0
        val rate = 8.25
        val months = 180
        val emi = Finance.emi(principal, rate, months)

        val solvedPrincipal = (LoanSolve.principal(rate, months, emi) as Solved.Ok).value
        val solvedRate = (LoanSolve.rate(principal, months, emi) as Solved.Ok).value
        val solvedMonths = (LoanSolve.months(principal, rate, emi) as Solved.Ok).value

        assertEquals(principal, solvedPrincipal, 0.01)
        assertEquals(rate, solvedRate, 1e-7)
        assertEquals(months.toDouble(), solvedMonths, 1e-7)
    }

    @Test
    fun brokenPeriodUsesActualDaysAndStillCloses() {
        val disbursement = LocalDate.of(2026, 7, 25)
        val firstEmi = LocalDate.of(2026, 8, 10)
        val principal = 1_000_000.0
        val payment = Finance.emi(principal, 9.0, 120)
        val rows = Finance.scheduleWithDisbursementAtPayment(
            principal,
            9.0,
            payment,
            disbursement,
            firstEmi
        )
        val expectedStub = principal * 0.09 * (16.0 / 365.0)

        assertEquals(expectedStub, rows.first().interest, 0.001)
        assertEquals(principal, rows.sumOf { it.principal }, 0.01)
        assertEquals(0.0, rows.last().closing, 0.01)
    }

    @Test
    fun financialYearBucketsReconcileToMonthlyRows() {
        val rows = Finance.schedule(800_000.0, 9.2, 84)
        val years = Finance.byFinancialYear(rows, startMonth = 8, startYear = 2026)

        assertTrue(years.isNotEmpty())
        assertEquals(rows.sumOf { it.interest }, years.sumOf { it.interest }, 0.01)
        assertEquals(rows.sumOf { it.principal }, years.sumOf { it.principal }, 0.01)
        assertTrue(abs(years.last().closing) < 0.01)
    }

    @Test
    fun tenureCutPrepaymentSavesInterestAndCloses() {
        val input = PrepayInput(
            principal = 2_000_000.0,
            annualRate = 8.5,
            months = 180,
            lumpSum = 300_000.0,
            lumpSumAtMonth = 12,
            monthlyExtra = 2_000.0,
            mode = PrepayMode.CUT_TENURE
        )
        val result = Prepayment.simulate(input)

        assertTrue(result.newMonths < result.baseMonths)
        assertTrue(result.newInterest < result.baseInterest)
        assertTrue(result.netSaved > 0.0)
        assertEquals(0.0, result.rows.last().closing, 0.01)
        assertEquals(input.principal, result.rows.sumOf { it.principal }, 0.01)
    }

    @Test
    fun oversizedPrepaymentChargeUsesOnlyAmountActuallyAccepted() {
        val principal = 100_000.0
        val rate = 12.0
        val months = 12
        val regularPrincipalMonthOne =
            Finance.emi(principal, rate, months) - principal * rate / 1200.0
        val maximumAccepted = principal - regularPrincipalMonthOne

        val result = Prepayment.simulate(
            PrepayInput(
                principal = principal,
                annualRate = rate,
                months = months,
                lumpSum = 1_000_000.0,
                lumpSumAtMonth = 1,
                mode = PrepayMode.CUT_TENURE,
                prepayChargePct = 3.54
            )
        )

        assertEquals(maximumAccepted * 0.0354, result.prepayCharge, 0.01)
        assertEquals(1, result.newMonths)
        assertEquals(0.0, result.rows.last().closing, 0.01)
    }
}
