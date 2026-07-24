package com.moneyclarity.calc.engine

import kotlin.math.abs
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Indian digit grouping: last three digits, then pairs.
 * 1234567 -> 12,34,567
 */
fun groupIndian(value: Long): String {
    val neg = value < 0
    val s = abs(value).toString()
    if (s.length <= 3) return if (neg) "-$s" else s
    val last3 = s.substring(s.length - 3)
    var rest = s.substring(0, s.length - 3)
    val parts = mutableListOf<String>()
    while (rest.length > 2) {
        parts.add(0, rest.substring(rest.length - 2))
        rest = rest.substring(0, rest.length - 2)
    }
    if (rest.isNotEmpty()) parts.add(0, rest)
    val out = parts.joinToString(",") + "," + last3
    return if (neg) "-$out" else out
}

/** ₹12,34,567 */
fun rupees(value: Double): String = "₹" + groupIndian(value.roundToLong())

/** ₹12,34,567.89 — used where paise actually matter. */
fun rupeesExact(value: Double): String {
    val whole = value.toLong()
    val paise = ((abs(value) - abs(whole.toDouble())) * 100).roundToLong()
    return "₹" + groupIndian(whole) + "." + paise.toString().padStart(2, '0')
}

/** Compact Indian scale: ₹1.25 Cr, ₹12.5 L, ₹45,000 */
fun rupeesCompact(value: Double): String = when {
    abs(value) >= 1_00_00_000 -> "₹" + trim2(value / 1_00_00_000) + " Cr"
    abs(value) >= 1_00_000 -> "₹" + trim2(value / 1_00_000) + " L"
    abs(value) >= 1_000 -> "₹" + trim2(value / 1_000) + " K"
    else -> "₹" + value.roundToLong()
}

/** Words under the amount field: "Twelve lakh fifty thousand" style, kept short. */
fun scaleHint(value: Double): String = when {
    value >= 1_00_00_000 -> trim2(value / 1_00_00_000) + " crore"
    value >= 1_00_000 -> trim2(value / 1_00_000) + " lakh"
    value >= 1_000 -> trim2(value / 1_000) + " thousand"
    else -> ""
}

fun percent(value: Double, decimals: Int = 2): String = trim(value, decimals) + "%"

fun trim2(v: Double): String = trim(v, 2)

fun trim(v: Double, decimals: Int): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    val factor = Math.pow(10.0, decimals.toDouble())
    val r = (v * factor).roundToLong() / factor
    // Locale.US, not the default locale. A phone set to a language that writes
    // decimals with a comma would otherwise produce "11,5", which is wrong in a
    // rupee context and unparseable when read back into a field.
    val s = String.format(Locale.US, "%.${decimals}f", r)
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

fun monthsToTenure(months: Int): String {
    val y = months / 12
    val m = months % 12
    return when {
        y > 0 && m > 0 -> "${y}y ${m}m"
        y > 0 -> "${y}y"
        else -> "${m}m"
    }
}

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** "Aug 26" for a compact table column. */
fun shortMonth(monthIndex: Int, year: Int): String =
    MONTH_NAMES[((monthIndex % 12) + 12) % 12] + " " + (year % 100).toString().padStart(2, '0')

/** "05 Aug 2026" for exports and captions. */
fun longDate(day: Int, monthIndex: Int, year: Int): String =
    day.toString().padStart(2, '0') + " " + MONTH_NAMES[((monthIndex % 12) + 12) % 12] + " " + year

/** Compact grouping without the currency mark, for dense table columns. */
fun plain(value: Double): String = groupIndian(Math.round(value))
