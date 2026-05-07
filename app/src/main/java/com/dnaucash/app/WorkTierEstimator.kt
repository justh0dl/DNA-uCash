package com.dnaucash.app

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WorkTierEstimator {

    private const val PREFS_NAME = "dnaucash_benchmark"
    private const val KEY_MS_PER_UNIT = "ms_per_unit"
    private const val DEFAULT_MS_PER_UNIT = 0.05f

    private val BENCHMARK_KEY = "DNAuCash-benchmark".toByteArray()

    fun ensureBenchmarked(context: Context) {
        val prefs = prefs(context)
        if (prefs.contains(KEY_MS_PER_UNIT)) return

        // Warmup
        benchmarkUnitWork(1000)

        val samples = listOf(
            benchmarkUnitWork(2000),
            benchmarkUnitWork(2000),
            benchmarkUnitWork(2000)
        ).sorted()

        val medianMsPerUnit = samples[1]
        prefs.edit().putFloat(KEY_MS_PER_UNIT, medianMsPerUnit.toFloat()).apply()
    }

    /**
     * We keep the object name for compatibility, but conceptually this is now
     * estimating Forge Level timing, not the old workTier terminology.
     */
    fun estimateSeconds(context: Context, forgeLevel: String?): Int {
        val prefs = prefs(context)
        val msPerUnit = prefs.getFloat(KEY_MS_PER_UNIT, DEFAULT_MS_PER_UNIT).toDouble()

        val targetSeconds = when ((forgeLevel ?: PublicCoinRecord.FORGE_CAST).lowercase()) {
            PublicCoinRecord.FORGE_CAST -> 1.0
            PublicCoinRecord.FORGE_FORGED -> 3.0
            PublicCoinRecord.FORGE_TEMPERED -> 7.0
            PublicCoinRecord.FORGE_HARDENED -> 21.0
            else -> 1.0
        }

        // Right now this returns the Forge Level target as the UI estimate.
        // The stored benchmark remains ready for the actual guarded KDF wiring.
        return targetSeconds.toInt().coerceAtLeast(1)
    }

    fun displayLabel(forgeLevel: String?): String {
        return when ((forgeLevel ?: PublicCoinRecord.FORGE_CAST).lowercase()) {
            PublicCoinRecord.FORGE_CAST -> "Cast"
            PublicCoinRecord.FORGE_FORGED -> "Forged"
            PublicCoinRecord.FORGE_TEMPERED -> "Tempered"
            PublicCoinRecord.FORGE_HARDENED -> "Hardened"
            else -> "Cast"
        }
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns approximate milliseconds per standard work unit.
     */
    private fun benchmarkUnitWork(rounds: Int): Double {
        val key = BENCHMARK_KEY
        var out = ByteArray(32) { it.toByte() }

        val start = SystemClock.elapsedRealtimeNanos()
        repeat(rounds) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            out = mac.doFinal(out)
        }
        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
        return elapsedMs / rounds
    }
}