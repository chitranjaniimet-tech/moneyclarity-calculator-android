package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.engine.*
import com.moneyclarity.calc.ui.components.*

private class Quote(amount: String, rate: String, months: Int, fee: String, flat: Boolean) {
    var label by mutableStateOf("")
    var amount by mutableStateOf(amount)
    var rate by mutableStateOf(rate)
    var months by mutableIntStateOf(months)
    var fee by mutableStateOf(fee)
    var flat by mutableStateOf(flat)
}

@Composable
fun CompareScreen() {
    val quotes = remember {
        mutableStateListOf(
            Quote("500000", "10.5", 60, "5000", false),
            Quote("500000", "8.0", 60, "2000", true)
        )
    }

    val results = quotes.map { q ->
        EffectiveCost.compute(
            CostInput(
                amount = q.amount.toDoubleOrNull() ?: 0.0,
                quotedRate = q.rate.toDoubleOrNull() ?: 0.0,
                rateType = if (q.flat) RateType.FLAT else RateType.REDUCING,
                months = q.months,
                processingFee = q.fee.toDoubleOrNull() ?: 0.0
            )
        )
    }

    val cheapestIndex = results.indices.minByOrNull { results[it].totalOutgo } ?: -1

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Instalments can be made to look small by stretching the tenure. Ranking is on total money paid out, which cannot be dressed up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        quotes.forEachIndexed { index, quote ->
            val result = results[index]
            val cheapest = index == cheapestIndex && quotes.size > 1
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    if (cheapest) 2.dp else 1.dp,
                    if (cheapest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Eyebrow("Quote ${index + 1}")
                        if (cheapest) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "Lowest total",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (quotes.size > 1) {
                            TextButton(onClick = { quotes.removeAt(index) }) { Text("Remove") }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = quote.label,
                        onValueChange = { quote.label = it },
                        label = { Text("Name it, e.g. dealer finance") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    ValueField("Amount", quote.amount, { quote.amount = it }, step = 25000.0, max = 100000000.0)
                    Spacer(Modifier.height(10.dp))
                    ValueField("Rate", quote.rate, { quote.rate = it }, prefix = "", suffix = "% p.a.", step = 0.05, min = 0.0, max = 60.0, decimals = 2)
                    Spacer(Modifier.height(10.dp))
                    ToggleRow(
                        options = listOf("Flat", "Reducing"),
                        selectedIndex = if (quote.flat) 0 else 1,
                        onSelect = { quote.flat = it == 0 }
                    )
                    Spacer(Modifier.height(10.dp))
                    ValueField("Processing fee", quote.fee, { quote.fee = it }, step = 500.0, max = 500000.0)
                    Spacer(Modifier.height(10.dp))
                    TenureField(quote.months) { quote.months = it }

                    Spacer(Modifier.height(14.dp))
                    HairlineDivider()
                    StatRow("Instalment", rupees(result.instalment))
                    HairlineDivider()
                    StatRow(
                        "Effective annual cost",
                        result.effectiveRate?.let { percent(it) } ?: "—",
                        valueColor = MaterialTheme.colorScheme.secondary
                    )
                    HairlineDivider()
                    StatRow("Total paid out", rupees(result.totalOutgo), emphasis = true)
                }
            }
        }

        if (quotes.size < 3) {
            OutlinedButton(
                onClick = { quotes.add(Quote("500000", "11.0", 60, "3000", false)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add another quote") }
        }

        if (cheapestIndex >= 0 && quotes.size > 1) {
            val cheapest = results[cheapestIndex]
            val dearest = results.maxByOrNull { it.totalOutgo }
            val gap = (dearest?.totalOutgo ?: 0.0) - cheapest.totalOutgo
            SectionCard(title = "The spread") {
                Text(
                    "Between the cheapest and the dearest quote here, the difference over the full tenure is ${rupees(gap)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
