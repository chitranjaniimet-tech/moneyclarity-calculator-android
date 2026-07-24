package com.moneyclarity.calc.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.moneyclarity.calc.engine.ScheduleRow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

@Serializable
data class SavedCalc(
    val id: Long,
    val name: String,
    val amount: Double,
    val rate: Double,
    val months: Int,
    val rateType: String = "REDUCING",
    val processingFee: Double = 0.0,
    val insurance: Double = 0.0,
    val note: String = "",
    /** LOAN entries can be reopened in the instalment calculator. */
    val kind: String = "LOAN",
    /** Snapshot fields let every calculator keep its result, not only loans. */
    val result: String = "",
    val details: String = ""
)

/**
 * Everything stays on the device. There is no account, no sync and no network
 * permission in the manifest, so a flat JSON blob in shared preferences is both
 * sufficient and easy to audit.
 */
object Store {

    private const val PREFS = "mcc_store"
    private const val KEY_SAVED = "saved_calcs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_HAPTICS = "haptics_on"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun load(context: Context): List<SavedCalc> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SAVED, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedCalc>>(raw) }.getOrDefault(emptyList())
    }

    fun save(context: Context, items: List<SavedCalc>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED, json.encodeToString(items))
            .apply()
    }

    fun add(context: Context, item: SavedCalc): List<SavedCalc> {
        val updated = listOf(item) + load(context)
        save(context, updated)
        return updated
    }

    fun addResult(
        context: Context,
        title: String,
        result: String,
        details: String
    ): List<SavedCalc> = add(
        context,
        SavedCalc(
            id = System.currentTimeMillis(),
            name = title,
            amount = 0.0,
            rate = 0.0,
            months = 0,
            kind = "RESULT",
            result = result,
            details = details
        )
    )

    fun remove(context: Context, id: Long): List<SavedCalc> {
        val updated = load(context).filterNot { it.id == id }
        save(context, updated)
        return updated
    }

    /** 0 = follow system, 1 = light, 2 = dark */
    fun themeMode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_THEME, 0)

    fun setThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, mode).apply()
    }

    /**
     * Touch feedback, on unless turned off. It sits in the same preferences file
     * as the theme, so both survive a restart by the same mechanism and there is
     * only one place to look when either misbehaves.
     */
    fun hapticsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_HAPTICS, true)

    fun setHaptics(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }
}

/**
 * Addresses the app hands out. Neither is fetched in-process: they go to the
 * browser or the Play app through ACTION_VIEW, which is why none of this needs
 * the internet permission.
 */
object Links {

    const val SITE = "https://moneyclaritytech.com"

    /**
     * Hard-coded to the release application id rather than read from
     * context.packageName. Debug builds carry a .debug suffix, and taking the
     * id from the running package would put a dead link into every message
     * shared from a test build.
     */
    const val PLAY = "https://play.google.com/store/apps/details?id=com.moneyclarity.calc"
}

object Export {

    fun scheduleCsv(rows: List<ScheduleRow>): String {
        val sb = StringBuilder("Month,Opening balance,Instalment,Interest,Principal,Closing balance\n")
        rows.forEach { r ->
            sb.append(r.month).append(',')
                .append(round2(r.opening)).append(',')
                .append(round2(r.payment)).append(',')
                .append(round2(r.interest)).append(',')
                .append(round2(r.principal)).append(',')
                .append(round2(r.closing)).append('\n')
        }
        return sb.toString()
    }

    // Fixed to Locale.US so a comma-decimal locale cannot inject separators
    // into a comma-separated file.
    private fun round2(v: Double) = String.format(Locale.US, "%.2f", v)

    /**
     * Closing lines on every shared working. The point of the link is that the
     * person receiving a figure on WhatsApp can install the same calculator and
     * put the inputs in themselves rather than taking the number on trust.
     */
    fun signature(): String = buildString {
        appendLine("Worked out in MoneyClarity Calc")
        appendLine("Check these figures yourself: ${Links.PLAY}")
        append("More free tools: ${Links.SITE}")
    }

    /** Appends the signature to a body, with one blank line before it. */
    fun signed(body: String): String = buildString {
        append(body.trimEnd())
        appendLine()
        appendLine()
        append(signature())
    }

    /** Every shared working goes out signed, so no caller has to remember to do it. */
    fun shareText(context: Context, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, signed(body))
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    /** The app itself, with no figures attached. */
    fun shareApp(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MoneyClarity Calc")
            putExtra(
                Intent.EXTRA_TEXT,
                "MoneyClarity Calc - a loan and investment calculator that asks for no " +
                    "permissions and sends nothing anywhere.\n\n${Links.PLAY}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Share MoneyClarity Calc"))
    }

    fun shareCsv(context: Context, fileName: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share schedule"))
    }
}
