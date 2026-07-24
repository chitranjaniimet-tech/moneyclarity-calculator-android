package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.data.CalcState
import com.moneyclarity.calc.data.Export
import com.moneyclarity.calc.data.Store
import com.moneyclarity.calc.engine.*
import com.moneyclarity.calc.ui.components.*

private class PrepaySnapshot(val input: PrepayInput, val result: PrepayResult, val modeIndex: Int)

@Composable
fun PrepaymentScreen(state: CalcState) {
    val context = LocalContext.current
    var modeIndex by remember { mutableIntStateOf(0) }
    var lumpSum by remember { mutableStateOf("100000") }
    var lumpMonth by remember { mutableIntStateOf(12) }
    var monthlyExtra by remember { mutableStateOf("0") }
    var chargePct by remember { mutableStateOf("0") }

    var snapshot by remember { mutableStateOf<PrepaySnapshot?>(null) }
    var saved by remember { mutableStateOf(false) }

    fun calculate() {
        val input = PrepayInput(
            principal = state.amountValue,
            annualRate = state.rateValue,
            months = state.months,
            lumpSum = lumpSum.toDoubleOrNull() ?: 0.0,
            lumpSumAtMonth = lumpMonth.coerceIn(1, state.months.coerceAtLeast(1)),
            monthlyExtra =
                if (modeIndex == 0) monthlyExtra.toDoubleOrNull() ?: 0.0 else 0.0,
            mode = if (modeIndex == 0) PrepayMode.CUT_TENURE else PrepayMode.CUT_INSTALMENT,
            prepayChargePct = chargePct.toDoubleOrNull() ?: 0.0
        )
        snapshot = PrepaySnapshot(input, Prepayment.simulate(input), modeIndex)
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
        SectionCard(title = "The loan") {
            ValueField("Outstanding amount", state.amount, { state.amount = it }, step = 25000.0, max = 100000000.0)
            Spacer(Modifier.height(10.dp))
            ValueField("Rate", state.rate, { state.rate = it }, prefix = "", suffix = "% p.a.", step = 0.05, min = 0.0, max = 40.0, decimals = 2)
            Spacer(Modifier.height(10.dp))
            TenureField(state.months) { state.months = it }
        }

        SectionCard(title = "Which way to take the benefit") {
            ToggleRow(
                options = listOf("Cut the tenure", "Cut the instalment"),
                selectedIndex = modeIndex,
                onSelect = { modeIndex = it }
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (modeIndex == 0)
                    "The instalment stays where it is and the loan simply finishes earlier. This is where the interest saving is largest."
                else
                    "The tenure stays where it is and the monthly outgo drops. This frees up cash flow but saves less interest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "The extra payment") {
            ValueField("One time payment", lumpSum, { lumpSum = it }, step = 25000.0)
            Spacer(Modifier.height(14.dp))
            ValueField("Paid at instalment number", lumpMonth.toString(), { lumpMonth = it.toIntOrNull()?.coerceIn(1, 600) ?: 1 }, prefix = "", suffix = "of ${state.months}", step = 1.0, min = 1.0, max = state.months.toDouble(), fineTune = false)
            if (modeIndex == 0) {
                Spacer(Modifier.height(14.dp))
                ValueField(
                    "Extra with every instalment",
                    monthlyExtra,
                    { monthlyExtra = it },
                    step = 500.0,
                    max = 500000.0
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    "A recurring extra payment is a tenure-cut strategy. To reduce the EMI while keeping the end date, enter the one-time payment above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            ValueField("Total prepayment charge", chargePct, { chargePct = it }, prefix = "", suffix = "%", step = 0.25, min = 0.0, max = 10.0, decimals = 2, fineTune = false)
            Spacer(Modifier.height(6.dp))
            Text(
                "Enter the effective charge including GST, if any. Floating-rate loans to individuals normally carry no prepayment charge; fixed-rate and business facilities may.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        CalculateButton(hasResult = snapshot != null, onClick = ::calculate)

        val snap = snapshot
        if (snap == null) {
            CalculatePrompt()
        } else {
            val input = snap.input
            val result = snap.result
            val snapModeIndex = snap.modeIndex

            SectionCard {
                HeroResult(
                    label = "Interest you avoid",
                    value = rupees(result.netSaved.coerceAtLeast(0.0)),
                    caption = if (snapModeIndex == 0)
                        "Tenure falls by ${monthsToTenure(result.monthsSaved)}"
                    else
                        "Instalment falls to ${rupees(result.newInstalment)}",
                    accent = true
                )
            }

            SectionCard(title = "Side by side") {
                CompareLine("Instalment", rupees(result.baseInstalment), rupees(result.newInstalment))
                HairlineDivider()
                CompareLine("Instalments left", "${result.baseMonths}", "${result.newMonths}")
                HairlineDivider()
                CompareLine("Total interest", rupees(result.baseInterest), rupees(result.newInterest))
                Spacer(Modifier.height(10.dp))
                HairlineDivider()
                StatRow(
                    "Interest avoided",
                    rupees(result.interestSaved),
                    emphasis = true,
                    valueColor = MaterialTheme.colorScheme.secondary
                )
                if (result.prepayCharge > 0) {
                    HairlineDivider()
                    StatRow("Prepayment charge", "− " + rupees(result.prepayCharge))
                    HairlineDivider()
                    StatRow("Net benefit", rupees(result.netSaved), emphasis = true)
                }
                if (result.breakEvenMonth != null) {
                    HairlineDivider()
                    StatRow("Outlay recovered by instalment", "#${result.breakEvenMonth}")
                }
            }

            ResultActions(
                saved = saved,
                onSave = {
                    Store.addResult(
                        context,
                        "Prepayment",
                        "Interest avoided: ${rupees(result.interestSaved)}",
                        if (snapModeIndex == 0)
                            "Tenure shorter by ${monthsToTenure(result.monthsSaved)} · Net benefit ${rupees(result.netSaved)}"
                        else
                            "New EMI ${rupees(result.newInstalment)} · Net benefit ${rupees(result.netSaved)}"
                    )
                    saved = true
                },
                onShare = {
                    Export.shareText(
                        context,
                        "Prepayment working",
                        buildString {
                            appendLine("Outstanding: ${rupees(input.principal)} at ${percent(input.annualRate)}")
                            appendLine("Remaining tenure: ${monthsToTenure(input.months)}")
                            appendLine("Extra paid: ${rupees(input.lumpSum)} at instalment #${input.lumpSumAtMonth}")
                            appendLine("Approach: ${if (snapModeIndex == 0) "cut the tenure" else "cut the instalment"}")
                            appendLine("Interest before: ${rupees(result.baseInterest)}")
                            appendLine("Interest after: ${rupees(result.newInterest)}")
                            appendLine("Interest avoided: ${rupees(result.interestSaved)}")
                            if (snapModeIndex == 0) appendLine("Tenure shorter by: ${monthsToTenure(result.monthsSaved)}")
                            else append("New instalment: ${rupees(result.newInstalment)}")
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun CompareLine(label: String, before: String, after: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Eyebrow("Before")
                Spacer(Modifier.height(2.dp))
                Text(before, style = com.moneyclarity.calc.ui.theme.NumberMedium)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "→",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Eyebrow("After", MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(2.dp))
                Text(
                    after,
                    style = com.moneyclarity.calc.ui.theme.NumberMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
