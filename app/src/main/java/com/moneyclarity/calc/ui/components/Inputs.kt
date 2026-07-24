package com.moneyclarity.calc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.moneyclarity.calc.engine.monthsToTenure
import com.moneyclarity.calc.engine.scaleHint
import com.moneyclarity.calc.engine.trim

/**
 * A slider sitting in a vertically scrolling column will always be a hazard: a
 * scroll gesture that begins on the track is ambiguous, and the control has no
 * way to know the finger was on its way past. Rather than patch the gesture
 * handling, the sliders were removed from the scrolling path altogether.
 *
 * What replaces them is a typed field with a decrement and increment either
 * side, plus an optional fine tune sheet. The sheet is where a slider still
 * makes sense, because a modal surface has nothing scrolling behind it to
 * confuse the gesture with.
 */
@Composable
fun ValueField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String = "₹",
    suffix: String? = null,
    step: Double = 1000.0,
    min: Double = 0.0,
    max: Double = 1_000_000_000.0,
    decimals: Int = 0,
    fineTune: Boolean = true,
    modifier: Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val current = value.toDoubleOrNull() ?: 0.0

    fun emit(next: Double) {
        val clamped = next.coerceIn(min, max)
        onValueChange(if (decimals == 0) clamped.toLong().toString() else trim(clamped, decimals))
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (fineTune) {
                TextButton(onClick = { sheetOpen = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fine tune", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                if (new.isEmpty() || new.matches(Regex("^\\d*\\.?\\d*$"))) onValueChange(new)
            },
            prefix = if (prefix.isNotEmpty()) {
                { Text(prefix, style = com.moneyclarity.calc.ui.theme.NumberMedium) }
            } else null,
            visualTransformation = if (prefix == "₹" && decimals == 0)
                remember { IndianGroupingTransformation() } else VisualTransformation.None,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    if (suffix != null) {
                        Text(
                            suffix,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    StepButton(Icons.Filled.Remove, "Decrease") { emit(current - step) }
                    Spacer(Modifier.width(4.dp))
                    StepButton(Icons.Filled.Add, "Increase") { emit(current + step) }
                }
            },
            singleLine = true,
            textStyle = com.moneyclarity.calc.ui.theme.NumberLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (prefix == "₹") {
            val hint = scaleHint(current)
            if (hint.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    hint,
                    style = com.moneyclarity.calc.ui.theme.NumberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (sheetOpen) {
        FineTuneSheet(
            label = label,
            value = current,
            min = min,
            max = if (max > 50_000_000.0) 10_000_000.0 else max,
            prefix = prefix,
            suffix = suffix,
            decimals = decimals,
            onDismiss = { sheetOpen = false },
            onApply = { emit(it); sheetOpen = false }
        )
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    // The lightest of the three weights: this fires repeatedly as a value is
    // stepped, so anything heavier would become noise.
    val tap = haptics()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(34.dp)
    ) {
        IconButton(onClick = { tap.tick(); onClick() }, modifier = Modifier.size(34.dp)) {
            Icon(
                icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FineTuneSheet(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    prefix: String,
    suffix: String?,
    decimals: Int,
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    var draft by remember { mutableStateOf(value.toFloat()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Eyebrow(label)
            Spacer(Modifier.height(10.dp))
            Text(
                prefix + (if (decimals == 0) draft.toLong().toString() else trim(draft.toDouble(), decimals)) +
                    (suffix ?: ""),
                style = com.moneyclarity.calc.ui.theme.NumberHero,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            // Safe here: a modal sheet has nothing scrolling behind it.
            Slider(
                value = draft.coerceIn(min.toFloat(), max.toFloat()),
                onValueChange = { draft = it },
                valueRange = min.toFloat()..max.toFloat()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    prefix + min.toLong(),
                    style = com.moneyclarity.calc.ui.theme.NumberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    prefix + max.toLong(),
                    style = com.moneyclarity.calc.ui.theme.NumberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onApply(draft.toDouble()) },
                    modifier = Modifier.weight(1f)
                ) { Text("Apply") }
            }
        }
    }
}

/**
 * Tenure entry where the number typed is read in whatever unit is selected.
 * Years are usually round, months need to be exact, so both accept direct entry
 * rather than only one of them.
 */
@Composable
fun TenureField(months: Int, onChange: (Int) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var inYears by remember { mutableStateOf(months % 12 == 0 && months >= 12) }
    var text by remember(inYears) {
        mutableStateOf(if (inYears) (months / 12).toString() else months.toString())
    }

    LaunchedEffect(months) {
        val expected = if (inYears) (months / 12).toString() else months.toString()
        if (text.toIntOrNull() != expected.toIntOrNull()) text = expected
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Tenure",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(monthsToTenure(months), style = com.moneyclarity.calc.ui.theme.NumberMedium)
        }
        Spacer(Modifier.height(8.dp))
        ToggleRow(
            options = listOf("Years", "Months"),
            selectedIndex = if (inYears) 0 else 1,
            onSelect = { index ->
                val nowYears = index == 0
                if (nowYears != inYears) {
                    inYears = nowYears
                    text = if (nowYears) (months / 12).coerceAtLeast(1).toString() else months.toString()
                    if (nowYears) onChange(((months / 12).coerceAtLeast(1)) * 12)
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                if (new.isEmpty() || new.matches(Regex("^\\d{0,4}$"))) {
                    text = new
                    val n = new.toIntOrNull()
                    if (n != null && n > 0) {
                        onChange((if (inYears) n * 12 else n).coerceIn(1, 600))
                    }
                }
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        if (inYears) "years" else "months",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    StepButton(Icons.Filled.Remove, "Decrease") {
                        val next = (months - if (inYears) 12 else 1).coerceAtLeast(1)
                        onChange(next)
                        text = if (inYears) (next / 12).toString() else next.toString()
                    }
                    Spacer(Modifier.width(4.dp))
                    StepButton(Icons.Filled.Add, "Increase") {
                        val next = (months + if (inYears) 12 else 1).coerceAtMost(600)
                        onChange(next)
                        text = if (inYears) (next / 12).toString() else next.toString()
                    }
                }
            },
            singleLine = true,
            textStyle = com.moneyclarity.calc.ui.theme.NumberLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
