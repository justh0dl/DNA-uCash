package com.dnaucash.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CoinLogStore {

    private const val PREFS = "dnaucash_database_prefs"
    private const val KEY_DATABASE_ENABLED = "database_enabled"
    private const val KEY_PRIVATE_KEY_LOGGING_UNLOCKED = "private_key_logging_unlocked"
    private const val KEY_LOG_PRIVATE_KEYS = "log_private_keys"

    private const val SCANNED_FILE = "scanned_coins.csv"
    private const val MINTED_FILE = "minted_coins.csv"

    fun isPrivateKeyLoggingUnlocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRIVATE_KEY_LOGGING_UNLOCKED, false)
    }

    fun setPrivateKeyLoggingUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRIVATE_KEY_LOGGING_UNLOCKED, unlocked)
            .apply()
    }

    fun isPrivateKeyLoggingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOG_PRIVATE_KEYS, false)
    }

    fun setPrivateKeyLoggingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOG_PRIVATE_KEYS, enabled)
            .apply()
    }
    fun isDatabaseEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DATABASE_ENABLED, false)
    }

    fun setDatabaseEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DATABASE_ENABLED, enabled)
            .apply()
    }

    fun logScannedCoin(
        context: Context,
        uid: String,
        publicText: String,
        record: PublicCoinRecord?
    ) {
        if (!isDatabaseEnabled(context)) return

        val row = listOf(
            timestamp(),
            "scanned",
            uid,
            record?.a.orEmpty(),
            record?.s.orEmpty(),
            record?.tokenType.orEmpty(),
            record?.forgeLevel.orEmpty(),
            record?.secretGrade.orEmpty(),
            cleanCsv(publicText),
            cleanCsv(record?.m.orEmpty()),
            ""
        )

        appendCsvRow(
            context = context,
            fileName = SCANNED_FILE,
            row = row
        )
    }

    fun logMintedCoin(
        context: Context,
        uid: String,
        record: PublicCoinRecord,
        privateKey: String? = null
    ) {
        if (!isDatabaseEnabled(context)) return

        val row = listOf(
            timestamp(),
            "minted",
            uid,
            record.a,
            record.s,
            record.tokenType,
            record.forgeLevel.orEmpty(),
            record.secretGrade.orEmpty(),
            "",
            cleanCsv(record.m),
            cleanCsv(privateKey.orEmpty())
        )

        appendCsvRow(
            context = context,
            fileName = MINTED_FILE,
            row = row
        )
    }

    fun exportScannedCsv(context: Context): Uri {
        return exportToDownloads(context, SCANNED_FILE)
    }

    fun exportMintedCsv(context: Context): Uri {
        return exportToDownloads(context, MINTED_FILE)
    }

    private fun appendCsvRow(
        context: Context,
        fileName: String,
        row: List<String>
    ) {
        val file = getCsvFile(context, fileName)

        if (!file.exists()) {
            file.writeText(csvHeader() + "\n")
        }

        file.appendText(row.joinToString(",") { csvEscape(it) } + "\n")
    }

    private fun getCsvFile(context: Context, fileName: String): File {
        val dir = File(context.filesDir, "coin_logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return File(dir, fileName)
    }

    private fun csvHeader(): String {
        return listOf(
            "timestamp",
            "event_type",
            "uid",
            "address",
            "state",
            "coin_type",
            "forge_level",
            "secret_grade",
            "public_text",
            "coin_message",
            "private_key"
        ).joinToString(",")
    }

    private fun timestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        ).format(Date())
    }

    private fun exportToDownloads(context: Context, fileName: String): Uri {
        val source = getCsvFile(context, fileName)

        val downloads = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )

        val dest = File(downloads, fileName)

        source.copyTo(dest, overwrite = true)

        return Uri.fromFile(dest)
    }

    private fun cleanCsv(value: String): String {
        return value
            .replace("\r", " ")
            .replace("\n", " ")
            .trim()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}