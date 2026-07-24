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
import com.moneyclarity.calc.data.Calculators
import com.moneyclarity.calc.data.Export
import com.moneyclarity.calc.data.CalcOutput
import com.moneyclarity.calc.data.Store
import com.moneyclarity.calc.ui.components.*

/**
 * Drives all 28 spec-based calculators. Figures go in first, a single
 * Calculate press produces the working, and the working sits underneath the
 * button rather than above it -- so the screen reads top to bottom as
 * "here's what I typed, here's what it means" instead of putting the answer
 * before the question.
 *
 * The result shown is a snapshot taken at the moment Calculate was pressed,
 * not a live recomputation on every keystroke. Editing a figure afterward
 * does not silently move the number already on screen; it waits for another
 * press, which is what "Recalculate" is for.
 */
@Composable
fun CalculatorDetailScreen(id: String) {
    val context = LocalContext.current
    val spec = Calculators.byId(id) ?: return

    val values = remember(id) {
        mutableStateMapOf<String, String>().apply {
            spec.fields.forEach { put(it.key, it.default) }
        }
    }

    var output by remember(id) { mutableStateOf<CalcOutput?>(null) }
    var saved by remember(id) { mutableStateOf(false) }
    ReportCalculationResult(output != null)
    val snapshotValues = remember(id) { mutableStateMapOf<String, String>() }

    fun calculate() {
        val numeric = spec.fields.associate { it.key to (values[it.key]?.toDoubleOrNull() ?: 0.0) }
        output = spec.compute(numeric)
        snapshotValues.clear()
        snapshotValues.putAll(values)
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
        SectionCard(title = "Your figures") {
            spec.fields.forEachIndexed { index, field ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                ValueField(
                    label = field.label,
                    value = values[field.key] ?: field.default,
                    onValueChange = { values[field.key] = it },
                    prefix = field.prefix,
                    suffix = field.suffix,
                    step = field.step,
                    min = field.min,
                    max = field.max,
                    decimals = field.decimals,
                    fineTune = field.max - field.min > 10
                )
            }
        }

        CalculateButton(hasResult = output != null, onClick = ::calculate)

        val result = output
        if (result == null) {
            CalculatePrompt()
        } else {
            SectionCard {
                HeroResult(
                    label = result.heroLabel,
                    value = result.heroValue,
                    caption = result.heroCaption
                )
                if (result.split != null) {
                    Spacer(Modifier.height(18.dp))
                    SplitBar(principal = result.split.first, interest = result.split.second)
                }
            }

            if (result.lines.isNotEmpty()) {
                SectionCard(title = "Detail") {
                    result.lines.forEachIndexed { index, line ->
                        if (index > 0) HairlineDivider()
                        StatRow(
                            line.label,
                            line.value,
                            emphasis = line.emphasis,
                            valueColor = if (line.accent) MaterialTheme.colorScheme.secondary else null
                        )
                    }
                }
            }

            if (result.note != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Eyebrow("Worth knowing", MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            result.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            val working = buildString {
                appendLine(spec.title)
                appendLine()
                spec.fields.forEach { f ->
                    appendLine("${f.label}: ${f.prefix}${snapshotValues[f.key]}${f.suffix ?: ""}")
                }
                appendLine()
                appendLine("${result.heroLabel}: ${result.heroValue}")
                result.lines.forEach { appendLine("${it.label}: ${it.value}") }
            }
            ResultActions(
                saved = saved,
                onSave = {
                    Store.addResult(
                        context,
                        spec.title,
                        "${result.heroLabel}: ${result.heroValue}",
                        result.lines.joinToString(" · ") { "${it.label}: ${it.value}" }
                    )
                    saved = true
                },
                onShare = {
                    Export.shareText(
                        context,
                        spec.title,
                        working
                    )
                }
            )
        }
    }
}
