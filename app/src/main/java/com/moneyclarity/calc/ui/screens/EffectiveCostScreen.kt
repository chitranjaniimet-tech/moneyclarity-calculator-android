package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.data.CalcState
import com.moneyclarity.calc.data.Export
import com.moneyclarity.calc.data.Store
import com.moneyclarity.calc.engine.*
import com.moneyclarity.calc.ui.components.*

@Composable
fun EffectiveCostScreen(state: CalcState) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ToggleRow(
            options = listOf("Quoted loan", "No cost EMI"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        if (tab == 0) QuotedLoanPane(state, context) else NoCostPane(context)
    }
}

@Composable
private fun QuotedLoanPane(state: CalcState, context: android.content.Context) {
    var rateTypeIndex by remember { mutableIntStateOf(1) }
    var fee by remember { mutableStateOf("2500") }
    var insurance by remember { mutableStateOf("0") }
    var other by remember { mutableStateOf("0") }
    var advance by remember { mutableStateOf("0") }
    var gst by remember { mutableStateOf(true) }

    var snapshot by remember { mutableStateOf<Pair<CostInput, CostResult>?>(null) }
    var saved by remember { mutableStateOf(false) }

    fun calculate() {
        val input = CostInput(
            amount = state.amountValue,
            quotedRate = state.rateValue,
            rateType = if (rateTypeIndex == 0) RateType.FLAT else RateType.REDUCING,
            months = state.months,
            processingFee = fee.toDoubleOrNull() ?: 0.0,
            gstOnFee = gst,
            insurance = insurance.toDoubleOrNull() ?: 0.0,
            otherCharges = other.toDoubleOrNull() ?: 0.0,
            advanceEmis = advance.toIntOrNull() ?: 0
        )
        snapshot = input to EffectiveCost.compute(input)
        saved = false
    }

    SectionCard(title = "How it was quoted") {
        ToggleRow(
            options = listOf("Flat rate", "Reducing balance"),
            selectedIndex = rateTypeIndex,
            onSelect = { rateTypeIndex = it }
        )
        Spacer(Modifier.height(14.dp))
        ValueField("Amount financed", state.amount, { state.amount = it }, step = 25000.0, max = 100000000.0)
        Spacer(Modifier.height(10.dp))
        ValueField("Quoted rate", state.rate, { state.rate = it }, prefix = "", suffix = "% p.a.", step = 0.05, min = 0.0, max = 60.0, decimals = 2)
        Spacer(Modifier.height(10.dp))
        TenureField(state.months) { state.months = it }
    }

    SectionCard(title = "What they add on top") {
        ValueField("Processing fee", fee, { fee = it }, step = 500.0, max = 500000.0)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = gst, onCheckedChange = { gst = it })
            Text("Add 18% GST on the fee", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(6.dp))
        ValueField("Bundled insurance", insurance, { insurance = it }, step = 1000.0, max = 1000000.0)
        Spacer(Modifier.height(10.dp))
        ValueField("Documentation and other charges", other, { other = it }, step = 500.0, max = 500000.0)
    }

    CalculateButton(hasResult = snapshot != null, onClick = ::calculate)

    val snap = snapshot
    if (snap == null) {
        CalculatePrompt()
    } else {
        val (input, result) = snap
        val quotedFlat = input.rateType == RateType.FLAT

        SectionCard {
            RevealBar(quoted = input.quotedRate, actual = result.effectiveRate)
            Spacer(Modifier.height(18.dp))
            HairlineDivider()
            Spacer(Modifier.height(6.dp))
            StatRow("Monthly instalment", rupees(result.instalment), emphasis = true)
            HairlineDivider()
            StatRow("Charges deducted upfront", rupees(result.totalCharges))
            HairlineDivider()
            StatRow("Cash actually in your hand", rupees(result.netReceived))
            HairlineDivider()
            StatRow("Total you pay back", rupees(result.totalOutgo))
        }

        if (quotedFlat) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Eyebrow("Why the gap", MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A flat rate charges interest on the whole sanctioned amount for the entire tenure, " +
                            "even on the part you have already repaid. On reducing balance, interest is " +
                            "charged only on what is still outstanding.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        ResultActions(
            saved = saved,
            onSave = {
                Store.addResult(
                    context,
                    "Effective cost",
                    result.effectiveRate?.let { "${percent(it)} effective annual cost" }
                        ?: "Effective rate not computable",
                    "EMI ${rupees(result.instalment)} · Charges ${rupees(result.totalCharges)} · Total ${rupees(result.totalOutgo)}"
                )
                saved = true
            },
            onShare = {
                Export.shareText(
                    context,
                    "Effective cost working",
                    buildString {
                        appendLine("Amount financed: ${rupees(input.amount)}")
                        appendLine("Quoted: ${percent(input.quotedRate)} ${if (quotedFlat) "flat" else "reducing"}")
                        appendLine("Tenure: ${monthsToTenure(input.months)}")
                        appendLine("Instalment: ${rupees(result.instalment)}")
                        appendLine("Charges: ${rupees(result.totalCharges)}")
                        appendLine(
                            "Effective annual cost: " +
                                (result.effectiveRate?.let { percent(it) } ?: "not computable")
                        )
                        append("Total repaid: ${rupees(result.totalOutgo)}")
                    }
                )
            }
        )
    }
}

@Composable
private fun NoCostPane(context: android.content.Context) {
    var price by remember { mutableStateOf("60000") }
    var months by remember { mutableStateOf("6") }
    var discount by remember { mutableStateOf("3000") }
    var fee by remember { mutableStateOf("199") }

    var snapshot by remember { mutableStateOf<Pair<NoCostInput, NoCostResult>?>(null) }
    var saved by remember { mutableStateOf(false) }

    fun calculate() {
        val input = NoCostInput(
            price = price.toDoubleOrNull() ?: 0.0,
            months = months.toIntOrNull() ?: 1,
            discountForgone = discount.toDoubleOrNull() ?: 0.0,
            processingFee = fee.toDoubleOrNull() ?: 0.0
        )
        snapshot = input to NoCostEmi.compute(input)
        saved = false
    }

    SectionCard(title = "The offer") {
        ValueField("Sticker price", price, { price = it }, step = 1000.0, max = 10000000.0)
        Spacer(Modifier.height(10.dp))
        ValueField("Discount given up by choosing the plan", discount, { discount = it }, step = 500.0, max = 1000000.0)
        Spacer(Modifier.height(10.dp))
        ValueField("Processing fee", fee, { fee = it }, step = 500.0, max = 500000.0)
    }

    CalculateButton(hasResult = snapshot != null, onClick = ::calculate)

    val snap = snapshot
    if (snap == null) {
        CalculatePrompt()
    } else {
        val (input, result) = snap

        SectionCard {
            RevealBar(quoted = 0.0, actual = result.effectiveRate)
            Spacer(Modifier.height(18.dp))
            HairlineDivider()
            Spacer(Modifier.height(6.dp))
            StatRow("Monthly instalment", rupees(result.instalment), emphasis = true)
            HairlineDivider()
            StatRow("Cash price if you skip the plan", rupees(result.cashPrice))
            HairlineDivider()
            StatRow(
                "What the plan costs you",
                rupees(result.realCost),
                valueColor = MaterialTheme.colorScheme.secondary
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Eyebrow("How to fill this in", MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ask the seller what the price would be for an outright payment. The difference " +
                        "between that and the sticker price is the discount you are giving up, and it is " +
                        "the real cost of a plan advertised as interest free.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        ResultActions(
            saved = saved,
            onSave = {
                Store.addResult(
                    context,
                    "No cost EMI",
                    "Real cost: ${rupees(result.realCost)}",
                    "EMI ${rupees(result.instalment)} · Effective rate ${
                        result.effectiveRate?.let { percent(it) } ?: "not computable"
                    }"
                )
                saved = true
            },
            onShare = {
                Export.shareText(
                    context,
                    "No cost EMI working",
                    buildString {
                        appendLine("Sticker price: ${rupees(input.price)}")
                        appendLine("Instalments: ${input.months} of ${rupees(result.instalment)}")
                        appendLine("Discount given up: ${rupees(input.discountForgone)}")
                        appendLine("Processing fee: ${rupees(input.processingFee)}")
                        appendLine("Real cost of the plan: ${rupees(result.realCost)}")
                        appendLine(
                            "Effective annual rate: " +
                                (result.effectiveRate?.let { percent(it) } ?: "not computable")
                        )
                    }
                )
            }
        )
    }
}
