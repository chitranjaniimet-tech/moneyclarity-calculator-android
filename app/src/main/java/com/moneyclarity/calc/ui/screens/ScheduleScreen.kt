package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.data.CalcState
import com.moneyclarity.calc.data.Export
import com.moneyclarity.calc.engine.*
import com.moneyclarity.calc.ui.components.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

/**
 * Repayment schedule designed as one lazy, vertically scrolling document.
 *
 * The earlier layout measured two large setup cards before a nested weighted
 * table. On a phone the cards could consume the available height and leave the
 * table at (or near) zero height even though the rows had been calculated. A
 * single LazyColumn gives controls and rows one unambiguous scroll owner, so a
 * valid schedule is always visible and remains efficient at 600 instalments.
 */
@Composable
fun ScheduleScreen(state: CalcState) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var view by rememberSaveable { mutableIntStateOf(0) }

    val today = remember { Calendar.getInstance() }
    var day by rememberSaveable { mutableStateOf(today.get(Calendar.DAY_OF_MONTH).toString()) }
    var startMonth by rememberSaveable { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var startYear by rememberSaveable { mutableIntStateOf(today.get(Calendar.YEAR)) }

    var useDisbursement by rememberSaveable { mutableStateOf(false) }
    var disbDay by rememberSaveable { mutableStateOf("25") }
    var disbMonth by rememberSaveable {
        mutableIntStateOf(if (today.get(Calendar.MONTH) == 0) 11 else today.get(Calendar.MONTH) - 1)
    }
    var disbYear by rememberSaveable {
        mutableIntStateOf(
            if (today.get(Calendar.MONTH) == 0) today.get(Calendar.YEAR) - 1
            else today.get(Calendar.YEAR)
        )
    }

    val firstEmiDate = remember(day, startMonth, startYear) {
        safeDate(day.toIntOrNull() ?: 1, startMonth, startYear)
    }
    val disbDate = remember(disbDay, disbMonth, disbYear) {
        safeDate(disbDay.toIntOrNull() ?: 1, disbMonth, disbYear)
    }
    val stubDays = remember(disbDate, firstEmiDate) {
        ChronoUnit.DAYS.between(disbDate, firstEmiDate)
    }

    val rows = remember(
        state.amountValue,
        state.rateValue,
        state.months,
        state.schedulePaymentOverride,
        useDisbursement,
        disbDate,
        firstEmiDate
    ) {
        val fixedPayment = state.schedulePaymentOverride
        if (useDisbursement && fixedPayment != null) {
            Finance.scheduleWithDisbursementAtPayment(
                state.amountValue,
                state.rateValue,
                fixedPayment,
                disbDate,
                firstEmiDate
            )
        } else if (useDisbursement) {
            Finance.scheduleWithDisbursement(
                state.amountValue,
                state.rateValue,
                state.months,
                disbDate,
                firstEmiDate
            )
        } else if (fixedPayment != null) {
            Finance.scheduleAtPayment(
                state.amountValue,
                state.rateValue,
                fixedPayment
            )
        } else {
            Finance.schedule(state.amountValue, state.rateValue, state.months)
        }
    }
    val years = remember(rows, startMonth, startYear) {
        Finance.byFinancialYear(rows, startMonth + 1, startYear)
    }

    fun dateFor(instalment: Int): Pair<Int, Int> {
        val absolute = startMonth + (instalment - 1)
        return Pair(absolute % 12, startYear + absolute / 12)
    }

    fun moveFirstEmi(by: Int) {
        val moved = firstEmiDate.plusMonths(by.toLong())
        startMonth = moved.monthValue - 1
        startYear = moved.year
    }

    fun moveDisbursement(by: Int) {
        val moved = disbDate.plusMonths(by.toLong())
        disbMonth = moved.monthValue - 1
        disbYear = moved.year
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScheduleOverview(rows = rows, principal = state.amountValue, rate = state.rateValue)
        }

        item {
            SectionCard(title = "First instalment date") {
                DateEditor(
                    day = day,
                    onDayChange = { day = it },
                    date = firstEmiDate,
                    onPreviousMonth = { moveFirstEmi(-1) },
                    onNextMonth = { moveFirstEmi(1) },
                    supportingText = "Every row and the exported CSV will use this date."
                )
            }
        }

        item {
            SectionCard(title = "Actual first period") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Use disbursement date",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Adjust month-one interest for the exact days before the first EMI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = useDisbursement,
                        onCheckedChange = { useDisbursement = it }
                    )
                }

                if (useDisbursement) {
                    Spacer(Modifier.height(14.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(14.dp))
                    DateEditor(
                        day = disbDay,
                        onDayChange = { disbDay = it },
                        date = disbDate,
                        onPreviousMonth = { moveDisbursement(-1) },
                        onNextMonth = { moveDisbursement(1) },
                        supportingText = "Loan disbursement date"
                    )
                    Spacer(Modifier.height(12.dp))
                    BrokenPeriodNote(
                        rows = rows,
                        stubDays = stubDays,
                        standardInterest = state.amountValue * (state.rateValue / 1200.0)
                    )
                }
            }
        }

        item {
            ToggleRow(
                options = listOf("Monthly", "Financial year"),
                selectedIndex = view,
                onSelect = { view = it }
            )
        }

        item {
            OutlinedButton(
                enabled = rows.isNotEmpty(),
                onClick = {
                    keyboard?.hide()
                    val csv = buildString {
                        appendLine(
                            "Date,Instalment No.,Opening Balance,Payment,Interest,Principal,Closing Balance"
                        )
                        rows.forEach { row ->
                            val (month, year) = dateFor(row.month)
                            append(longDate(day.toIntOrNull() ?: 1, month, year)).append(',')
                                .append(row.month).append(',')
                                .append(csvNumber(row.opening)).append(',')
                                .append(csvNumber(row.payment)).append(',')
                                .append(csvNumber(row.interest)).append(',')
                                .append(csvNumber(row.principal)).append(',')
                                .append(csvNumber(row.closing)).append('\n')
                        }
                    }
                    Export.shareCsv(context, "repayment-schedule.csv", csv)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export complete CSV")
            }
        }

        if (rows.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (useDisbursement && stubDays <= 0)
                            "Choose a disbursement date before the first instalment date."
                        else if (useDisbursement)
                            "The first-period interest is larger than the EMI. Move the first EMI date closer to disbursement or review the loan figures."
                        else
                            "Enter a loan amount and tenure above zero to generate the repayment schedule.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (view == 0) {
            item {
                BreakdownHeading(
                    title = "${rows.size} instalments",
                    subtitle = "Interest is shown separately so the principal reduction is easy to verify."
                )
            }
            items(rows, key = { it.month }) { row ->
                val (month, year) = dateFor(row.month)
                MonthlyScheduleRow(
                    date = shortMonth(month, year),
                    row = row,
                    isYearEnd = row.month % 12 == 0 || row.month == rows.size
                )
            }
        } else {
            item {
                BreakdownHeading(
                    title = "${years.size} financial years",
                    subtitle = "Grouped April to March for easier tax and statement checking."
                )
            }
            items(years, key = { it.label }) { year ->
                FinancialYearRow(year)
            }
        }
    }
}

@Composable
private fun ScheduleOverview(rows: List<ScheduleRow>, principal: Double, rate: Double) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Repayment plan")
                Spacer(Modifier.height(3.dp))
                Text(
                    if (rows.isEmpty()) "Schedule unavailable" else "${rows.size} instalments",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "${rupees(principal)} at ${trim(rate, 2)}% p.a.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (rows.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            HairlineDivider()
            Spacer(Modifier.height(10.dp))
            StatRow("Monthly EMI", rupees(rows.first().payment), emphasis = true)
            StatRow(
                "Total interest",
                rupees(Finance.totalInterest(rows)),
                valueColor = MaterialTheme.colorScheme.secondary
            )
            StatRow("Total repayment", rupees(Finance.totalPaid(rows)))
            Spacer(Modifier.height(10.dp))
            SplitBar(principal = principal, interest = Finance.totalInterest(rows))
        }
    }
}

@Composable
private fun DateEditor(
    day: String,
    onDayChange: (String) -> Unit,
    date: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    supportingText: String
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = day,
            onValueChange = { new ->
                if (new.isEmpty() || new.matches(Regex("^\\d{0,2}$"))) {
                    val n = new.toIntOrNull()
                    if (new.isEmpty() || (n != null && n in 1..31)) onDayChange(new)
                }
            },
            label = { Text("Day") },
            singleLine = true,
            textStyle = com.moneyclarity.calc.ui.theme.NumberMedium,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.width(94.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    longDate(date.dayOfMonth, date.monthValue - 1, date.year),
                    style = com.moneyclarity.calc.ui.theme.NumberMedium
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPreviousMonth, modifier = Modifier.weight(1f)) {
            Text("Previous month")
        }
        OutlinedButton(onClick = onNextMonth, modifier = Modifier.weight(1f)) {
            Text("Next month")
        }
    }
}

@Composable
private fun BrokenPeriodNote(
    rows: List<ScheduleRow>,
    stubDays: Long,
    standardInterest: Double
) {
    val error = stubDays <= 0 || rows.isEmpty()
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            when {
                stubDays <= 0 ->
                    "The disbursement date must be before the first instalment date."
                rows.isEmpty() ->
                    "$stubDays days of first-period interest is more than one EMI, so this schedule cannot close as entered."
                else -> {
                    val actual = rows.first().interest
                    "$stubDays days to the first EMI: ${rupees(actual)} interest instead of the standard-month estimate of ${rupees(standardInterest)}."
                }
            },
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BreakdownHeading(title: String, subtitle: String) {
    Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthlyScheduleRow(date: String, row: ScheduleRow, isYearEnd: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isYearEnd) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isYearEnd) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(72.dp)) {
                    Text(
                        "#${row.month}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(date, style = com.moneyclarity.calc.ui.theme.NumberMedium)
                }
                Column(Modifier.weight(1f)) {
                    MetricLabel("Payment")
                    MetricValue(rupees(row.payment))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    MetricLabel("Balance")
                    MetricValue(rupees(row.closing), TextAlign.End)
                }
            }
            Spacer(Modifier.height(10.dp))
            HairlineDivider()
            Spacer(Modifier.height(9.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    MetricLabel("Interest")
                    MetricValue(rupees(row.interest), color = MaterialTheme.colorScheme.secondary)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    MetricLabel("Principal repaid")
                    MetricValue(rupees(row.principal), TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun FinancialYearRow(year: FinancialYear) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                year.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    MetricLabel("Paid")
                    MetricValue(rupees(year.paid))
                    Spacer(Modifier.height(9.dp))
                    MetricLabel("Interest")
                    MetricValue(rupees(year.interest), color = MaterialTheme.colorScheme.secondary)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    MetricLabel("Principal")
                    MetricValue(rupees(year.principal), TextAlign.End)
                    Spacer(Modifier.height(9.dp))
                    MetricLabel("Closing balance")
                    MetricValue(rupees(year.closing), TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun MetricLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MetricValue(
    text: String,
    textAlign: TextAlign = TextAlign.Start,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text,
        style = com.moneyclarity.calc.ui.theme.NumberMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        textAlign = textAlign
    )
}

private fun safeDate(day: Int, month0: Int, year: Int): LocalDate {
    val monthStart = LocalDate.of(year, month0 + 1, 1)
    return monthStart.withDayOfMonth(day.coerceIn(1, monthStart.lengthOfMonth()))
}

private fun csvNumber(value: Double): String =
    String.format(Locale.US, "%.2f", value)
