package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
    val tableScroll = rememberScrollState()

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
            Finance.scheduleAtPaymentForMonths(
                state.amountValue,
                state.rateValue,
                fixedPayment,
                state.months
            )
        } else {
            Finance.schedule(state.amountValue, state.rateValue, state.months)
        }
    }
    val years = remember(rows, startMonth, startYear) {
        Finance.byFinancialYear(rows, startMonth + 1, startYear)
    }
    ReportCalculationResult(rows.isNotEmpty())

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
                    subtitle = "Compact table in rupees. Swipe sideways to see every column."
                )
            }
            item {
                MonthlyTableHeader(tableScroll)
            }
            itemsIndexed(rows, key = { _, row -> row.month }) { index, row ->
                val (month, year) = dateFor(row.month)
                MonthlyTableRow(
                    date = shortMonth(month, year),
                    row = row,
                    alternate = index % 2 == 1,
                    scrollState = tableScroll
                )
            }
        } else {
            item {
                BreakdownHeading(
                    title = "${years.size} financial years",
                    subtitle = "April to March totals. Swipe sideways to see every column."
                )
            }
            item {
                FinancialYearTableHeader(tableScroll)
            }
            itemsIndexed(years, key = { _, year -> year.label }) { index, year ->
                FinancialYearTableRow(year, index % 2 == 1, tableScroll)
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

private val DateColumn = 86.dp
private val MoneyColumn = 112.dp

@Composable
private fun MonthlyTableHeader(scrollState: androidx.compose.foundation.ScrollState) {
    ScheduleTableFrame(top = true) {
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            HeaderCell("No. / date", DateColumn, TextAlign.Start)
            HeaderCell("Payment", MoneyColumn)
            HeaderCell("Principal", MoneyColumn)
            HeaderCell("Interest", MoneyColumn)
            HeaderCell("Balance", MoneyColumn)
        }
    }
}

@Composable
private fun MonthlyTableRow(
    date: String,
    row: ScheduleRow,
    alternate: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
) {
    ScheduleTableFrame {
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .background(
                    if (alternate) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surface
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell("#${row.month}\n$date", DateColumn, TextAlign.Start, primary = true)
            TableCell(rupees(row.payment), MoneyColumn)
            TableCell(rupees(row.principal), MoneyColumn)
            TableCell(rupees(row.interest), MoneyColumn, accent = true)
            TableCell(rupees(row.closing), MoneyColumn)
        }
    }
}

@Composable
private fun FinancialYearTableHeader(scrollState: androidx.compose.foundation.ScrollState) {
    ScheduleTableFrame(top = true) {
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            HeaderCell("Financial year", MoneyColumn, TextAlign.Start)
            HeaderCell("Paid", MoneyColumn)
            HeaderCell("Principal", MoneyColumn)
            HeaderCell("Interest", MoneyColumn)
            HeaderCell("Balance", MoneyColumn)
        }
    }
}

@Composable
private fun FinancialYearTableRow(
    year: FinancialYear,
    alternate: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
) {
    ScheduleTableFrame {
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .background(
                    if (alternate) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surface
                )
        ) {
            TableCell(year.label, MoneyColumn, TextAlign.Start, primary = true)
            TableCell(rupees(year.paid), MoneyColumn)
            TableCell(rupees(year.principal), MoneyColumn)
            TableCell(rupees(year.interest), MoneyColumn, accent = true)
            TableCell(rupees(year.closing), MoneyColumn)
        }
    }
}

@Composable
private fun ScheduleTableFrame(
    top: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (top) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.82f))
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign = TextAlign.End
) {
    Text(
        text.uppercase(),
        modifier = Modifier.width(width).padding(horizontal = 10.dp, vertical = 11.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = align,
        maxLines = 1
    )
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign = TextAlign.End,
    primary: Boolean = false,
    accent: Boolean = false
) {
    Text(
        text,
        modifier = Modifier.width(width).padding(horizontal = 10.dp, vertical = 11.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium
        ),
        color = when {
            accent -> MaterialTheme.colorScheme.secondary
            primary -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        textAlign = align,
        maxLines = 2
    )
}

private fun safeDate(day: Int, month0: Int, year: Int): LocalDate {
    val monthStart = LocalDate.of(year, month0 + 1, 1)
    return monthStart.withDayOfMonth(day.coerceIn(1, monthStart.lengthOfMonth()))
}

private fun csvNumber(value: Double): String =
    String.format(Locale.US, "%.2f", value)
