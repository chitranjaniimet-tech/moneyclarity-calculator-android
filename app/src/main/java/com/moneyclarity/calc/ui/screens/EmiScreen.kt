package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.data.CalcState
import com.moneyclarity.calc.data.Export
import com.moneyclarity.calc.data.SavedCalc
import com.moneyclarity.calc.data.Store
import com.moneyclarity.calc.engine.Finance
import com.moneyclarity.calc.engine.LoanSolve
import com.moneyclarity.calc.engine.SolveFor
import com.moneyclarity.calc.engine.Solved
import com.moneyclarity.calc.engine.monthsToTenure
import com.moneyclarity.calc.engine.percent
import com.moneyclarity.calc.engine.rupees
import com.moneyclarity.calc.engine.trim
import com.moneyclarity.calc.ui.components.*
import kotlin.math.roundToLong

/**
 * Amount, rate, tenure and instalment. Fix any three and the fourth follows,
 * so the screen lets you pick which one is the unknown rather than always
 * assuming it is the instalment.
 *
 * As with every other calculator, the working sits below a Calculate press
 * rather than updating live above the inputs. What's shown is a snapshot of
 * the figures at the moment Calculate was pressed; changing an input
 * afterward doesn't move the number already on screen until pressed again.
 */

private val targets = listOf(SolveFor.INSTALMENT, SolveFor.TENURE, SolveFor.RATE, SolveFor.AMOUNT)
private val targetLabels = listOf("Instalment", "Tenure", "Rate", "Amount")

private class EmiSnapshot(
    val target: SolveFor,
    val solved: Solved,
    val principalIn: Double,
    val rateIn: Double,
    val monthsIn: Int,
    val instalmentIn: Double
)

@Composable
fun EmiScreen(state: CalcState, onOpenSchedule: () -> Unit) {
    val context = LocalContext.current
    val tap = haptics()
    var saved by remember { mutableStateOf(false) }

    var targetIndex by rememberSaveable { mutableIntStateOf(0) }
    var instalmentInput by rememberSaveable { mutableStateOf("") }
    val target = targets[targetIndex]

    var snapshot by remember { mutableStateOf<EmiSnapshot?>(null) }

    fun calculate() {
        val principalIn = state.amountValue
        val rateIn = state.rateValue
        val monthsIn = state.months
        val instalmentIn = instalmentInput.toDoubleOrNull() ?: 0.0
        val solved: Solved = when (target) {
            SolveFor.INSTALMENT -> LoanSolve.instalment(principalIn, rateIn, monthsIn)
            SolveFor.TENURE -> LoanSolve.months(principalIn, rateIn, instalmentIn)
            SolveFor.RATE -> LoanSolve.rate(principalIn, monthsIn, instalmentIn)
            SolveFor.AMOUNT -> LoanSolve.principal(rateIn, monthsIn, instalmentIn)
        }
        snapshot = EmiSnapshot(target, solved, principalIn, rateIn, monthsIn, instalmentIn)
        saved = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(title = "Work out the") {
            ToggleRow(
                options = targetLabels,
                selectedIndex = targetIndex,
                onSelect = { index ->
                    if (index != targetIndex) {
                        // Leaving instalment mode seeds the instalment box with the
                        // last calculated figure, so switching modes doesn't drop
                        // the person onto an empty field.
                        if (targetIndex == 0) {
                            val current = Finance.emi(state.amountValue, state.rateValue, state.months)
                            if (current > 0.0) instalmentInput = current.roundToLong().toString()
                        }
                        targetIndex = index
                        snapshot = null
                    }
                }
            )
        }

        SectionCard(title = "Your figures") {
            if (target != SolveFor.AMOUNT) {
                ValueField(
                    "Loan amount", state.amount, { state.amount = it },
                    step = 25000.0, max = 100000000.0
                )
                Spacer(Modifier.height(16.dp))
            }
            if (target != SolveFor.RATE) {
                ValueField(
                    "Interest rate", state.rate, { state.rate = it },
                    prefix = "", suffix = "% p.a.",
                    step = 0.05, min = 0.0, max = 40.0, decimals = 2
                )
                Spacer(Modifier.height(16.dp))
            }
            if (target != SolveFor.INSTALMENT) {
                ValueField(
                    "Monthly instalment", instalmentInput, { instalmentInput = it },
                    step = 500.0, max = 10000000.0
                )
                Spacer(Modifier.height(16.dp))
            }
            if (target != SolveFor.TENURE) {
                TenureField(state.months) { state.months = it }
            }
        }

        CalculateButton(hasResult = snapshot != null, onClick = ::calculate)

        val snap = snapshot
        if (snap == null) {
            CalculatePrompt()
        } else if (snap.solved is Solved.Impossible) {
            ImpossibleCard(snap.solved.reason)
        } else {
            val answer = (snap.solved as Solved.Ok).value
            val rows = remember(snap) {
                when (snap.target) {
                    SolveFor.TENURE -> Finance.scheduleAtPayment(snap.principalIn, snap.rateIn, snap.instalmentIn)
                    SolveFor.RATE -> Finance.schedule(snap.principalIn, answer, snap.monthsIn)
                    SolveFor.AMOUNT -> Finance.schedule(answer, snap.rateIn, snap.monthsIn)
                    else -> Finance.schedule(snap.principalIn, snap.rateIn, snap.monthsIn)
                }
            }
            val principal = if (snap.target == SolveFor.AMOUNT) answer else snap.principalIn
            val ratePct = if (snap.target == SolveFor.RATE) answer else snap.rateIn
            val tenure = if (snap.target == SolveFor.TENURE) rows.size else snap.monthsIn
            val instalment = if (snap.target == SolveFor.INSTALMENT) answer else snap.instalmentIn
            val interest = Finance.totalInterest(rows)
            val total = rows.sumOf { it.payment }
            val finalInstalment = rows.lastOrNull()?.payment ?: 0.0
            val hasStub = snap.target == SolveFor.TENURE && rows.size > 1 && finalInstalment < instalment - 1.0

            SectionCard {
                when (snap.target) {
                    SolveFor.INSTALMENT -> HeroResult(
                        label = "Monthly instalment",
                        value = rupees(instalment),
                        caption = "${monthsToTenure(tenure)} at ${trim(ratePct, 2)}% reducing balance"
                    )
                    SolveFor.TENURE -> HeroResult(
                        label = "Tenure",
                        value = monthsToTenure(tenure),
                        caption = "$tenure instalments of ${rupees(snap.instalmentIn)} at ${trim(ratePct, 2)}%"
                    )
                    SolveFor.RATE -> HeroResult(
                        label = "Interest rate",
                        value = percent(ratePct),
                        caption = "reducing balance, to give ${rupees(snap.instalmentIn)} a month " +
                            "over ${monthsToTenure(tenure)}"
                    )
                    SolveFor.AMOUNT -> HeroResult(
                        label = "Loan amount",
                        value = rupees(principal),
                        caption = "${monthsToTenure(tenure)} at ${trim(ratePct, 2)}% " +
                            "on ${rupees(snap.instalmentIn)} a month"
                    )
                }
                Spacer(Modifier.height(18.dp))
                SplitBar(principal = principal, interest = interest)
            }

            SectionCard(title = "Over the full tenure") {
                StatRow("Instalments", "$tenure")
                HairlineDivider()
                if (hasStub) {
                    StatRow("Closing instalment", rupees(finalInstalment))
                    HairlineDivider()
                }
                StatRow(
                    "Total interest", rupees(interest),
                    valueColor = MaterialTheme.colorScheme.secondary
                )
                HairlineDivider()
                StatRow("Total repayment", rupees(total), emphasis = true)
                HairlineDivider()
                StatRow(
                    "Interest as share of principal",
                    if (principal > 0) "${Math.round(interest / principal * 100)}%" else "—"
                )
            }

            if (hasStub) {
                Text(
                    "Paying exactly ${rupees(snap.instalmentIn)} does not divide evenly into this " +
                        "balance, so the last instalment is smaller. The instalment has been kept " +
                        "as entered rather than adjusted to fit a round tenure.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        tap.commit()
                        // Carry the calculated loan into the shared figures, so the
                        // schedule shows this loan rather than the previous one.
                        // When tenure was solved from a fixed instalment, keep that
                        // exact instalment. Recomputing a standard EMI from the
                        // rounded row count would silently change the user's input.
                        state.set(
                            principal,
                            ratePct,
                            tenure,
                            schedulePaymentOverride =
                                if (snap.target == SolveFor.TENURE) snap.instalmentIn else null
                        )
                        onOpenSchedule()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("See schedule") }

                OutlinedButton(
                    onClick = {
                        tap.commit()
                        Store.add(
                            context,
                            SavedCalc(
                                id = System.currentTimeMillis(),
                                name = "Loan ${rupees(principal)}",
                                amount = principal,
                                rate = ratePct,
                                months = tenure
                            )
                        )
                        saved = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (saved) "Saved" else "Save") }
            }

            OutlinedButton(
                onClick = {
                    tap.commit()
                    Export.shareText(
                        context,
                        "Instalment working",
                        buildString {
                            appendLine("Loan amount: ${rupees(principal)}")
                            appendLine("Rate: ${trim(ratePct, 2)}% p.a. reducing")
                            appendLine("Tenure: ${monthsToTenure(tenure)} ($tenure instalments)")
                            appendLine("Monthly instalment: ${rupees(instalment)}")
                            if (hasStub) appendLine("Closing instalment: ${rupees(finalInstalment)}")
                            appendLine("Total interest: ${rupees(interest)}")
                            append("Total repayment: ${rupees(total)}")
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share these numbers") }
        }
    }
}

/**
 * Some combinations describe a loan that cannot exist: an instalment that never
 * clears the monthly interest, or one below what a zero-interest loan would
 * need. Names the figure that is the problem and what would fix it, rather
 * than showing a dash.
 */
@Composable
private fun ImpossibleCard(reason: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "These figures do not resolve",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
