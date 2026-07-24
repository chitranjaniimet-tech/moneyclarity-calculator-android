package com.moneyclarity.calc.engine

import kotlin.math.min

enum class PrepayMode { CUT_TENURE, CUT_INSTALMENT }

data class PrepayInput(
    val principal: Double,
    val annualRate: Double,
    val months: Int,
    val lumpSum: Double = 0.0,
    val lumpSumAtMonth: Int = 12,
    val monthlyExtra: Double = 0.0,
    val mode: PrepayMode = PrepayMode.CUT_TENURE,
    val prepayChargePct: Double = 0.0
)

data class PrepayResult(
    val baseInstalment: Double,
    val baseMonths: Int,
    val baseInterest: Double,
    val newInstalment: Double,
    val newMonths: Int,
    val newInterest: Double,
    val interestSaved: Double,
    val monthsSaved: Int,
    val prepayCharge: Double,
    val netSaved: Double,
    val breakEvenMonth: Int?,
    val rows: List<ScheduleRow>
)

object Prepayment {

    fun simulate(input: PrepayInput): PrepayResult {
        val months = input.months.coerceAtLeast(1)
        val baseRows = Finance.schedule(input.principal, input.annualRate, months)
        val baseInstalment = Finance.emi(input.principal, input.annualRate, months)
        val baseInterest = Finance.totalInterest(baseRows)

        val r = input.annualRate / 12.0 / 100.0
        var balance = input.principal
        var instalment = baseInstalment
        val rows = mutableListOf<ScheduleRow>()
        var month = 0
        var guard = months * 2 + 24
        var appliedLumpSum = 0.0

        while (balance > 0.005 && guard-- > 0) {
            month++
            val opening = balance
            val interest = balance * r

            var extra = 0.0
            if (input.monthlyExtra > 0 && input.mode == PrepayMode.CUT_TENURE) {
                extra += input.monthlyExtra
            }
            if (input.lumpSum > 0 && month == input.lumpSumAtMonth.coerceIn(1, months)) {
                // A prepayment cannot exceed what remains after the regular
                // principal component. Clamp it instead of charging a fee on
                // money that the loan could never accept.
                val regularPrincipal = (instalment - interest).coerceAtLeast(0.0)
                appliedLumpSum = min(input.lumpSum, (balance - regularPrincipal).coerceAtLeast(0.0))
                extra += appliedLumpSum
            }

            var payment = instalment + extra
            var principalPart = payment - interest
            if (principalPart >= balance) {
                principalPart = balance
                payment = balance + interest
            }
            val closing = (balance - principalPart).coerceAtLeast(0.0)
            rows.add(ScheduleRow(month, opening, payment, interest, principalPart, closing))
            balance = closing

            // Re-cut the instalment right after a lump sum when the borrower has
            // chosen to hold the tenure steady instead.
            if (input.mode == PrepayMode.CUT_INSTALMENT &&
                input.lumpSum > 0 &&
                month == input.lumpSumAtMonth.coerceAtLeast(1) &&
                balance > 0
            ) {
                val remaining = (months - month).coerceAtLeast(1)
                instalment = Finance.emi(balance, input.annualRate, remaining)
            }
            if (month >= months && input.mode == PrepayMode.CUT_INSTALMENT) break
        }

        val newInterest = rows.sumOf { it.interest }
        val newMonths = rows.size
        val charge = appliedLumpSum * (input.prepayChargePct / 100.0)
        val saved = baseInterest - newInterest
        val netSaved = saved - charge

        // The instalment number at which the money already handed over stops
        // being ahead of what the untouched loan would have cost. It has to be
        // measured across the whole original tenure, because with a shortened
        // loan the recovery only shows up in the months after the new schedule
        // has already ended. The lump sum needs no separate subtraction: it is
        // part of the payment recorded in the month it was made.
        var breakEven: Int? = null
        if (input.lumpSum > 0 || input.monthlyExtra > 0) {
            var cumBase = 0.0
            var cumNew = 0.0
            for (i in baseRows.indices) {
                cumBase += baseRows[i].payment
                cumNew += if (i < rows.size) rows[i].payment else 0.0
                if (i + 1 >= input.lumpSumAtMonth && cumNew + charge <= cumBase) {
                    breakEven = i + 1
                    break
                }
            }
        }

        return PrepayResult(
            baseInstalment = baseInstalment,
            baseMonths = baseRows.size,
            baseInterest = baseInterest,
            newInstalment = instalment,
            newMonths = newMonths,
            newInterest = newInterest,
            interestSaved = saved,
            monthsSaved = (baseRows.size - newMonths).coerceAtLeast(0),
            prepayCharge = charge,
            netSaved = netSaved,
            breakEvenMonth = breakEven,
            rows = rows
        )
    }
}
