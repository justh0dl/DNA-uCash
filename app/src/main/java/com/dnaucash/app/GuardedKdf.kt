package com.dnaucash.app

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object GuardedKdf {

    private const val PREFS_NAME = "dnaucash_guarded_kdf"
    private const val KEY_BENCHMARK_UNITS_PER_SEC = "benchmark_units_per_sec"

    private const val DEFAULT_UNITS_PER_SEC = 8000.0

    private val BENCHMARK_KEY = "DNAuCash-Guarded-KDF-Benchmark".toByteArray(StandardCharsets.UTF_8)

    data class DerivedMaterial(
        val authKey: ByteArray,
        val contentKey: ByteArray
    )

    fun ensureBenchmarked(context: Context) {
        val prefs = prefs(context)
        if (prefs.contains(KEY_BENCHMARK_UNITS_PER_SEC)) return

        runUnitWork(1000)

        val roundsPerSample = 4000
        val samples = mutableListOf<Double>()

        repeat(5) {
            val elapsedMs = benchmarkRounds(roundsPerSample)
            val unitsPerSec = roundsPerSample / (elapsedMs / 1000.0)
            samples.add(unitsPerSec)
        }

        val average = samples.average()
        prefs.edit().putFloat(KEY_BENCHMARK_UNITS_PER_SEC, average.toFloat()).apply()
    }

    fun estimatedSeconds(context: Context, forgeLevel: String?): Int {
        return ForgeLevel.targetSeconds(forgeLevel)
    }

    fun describeCountdownStage(secondsRemaining: Int, forgeLevel: String?): String {
        return when (ForgeLevel.fromWireValue(forgeLevel)) {
            ForgeLevel.CAST -> "Casting coin..."
            ForgeLevel.FORGED -> if (secondsRemaining >= 2) "Forging coin..." else "Casting coin..."
            ForgeLevel.TEMPERED -> when {
                secondsRemaining >= 5 -> "Tempering coin..."
                secondsRemaining >= 2 -> "Forging coin..."
                else -> "Casting coin..."
            }
            ForgeLevel.HARDENED -> when {
                secondsRemaining >= 14 -> "Hardening coin..."
                secondsRemaining >= 6 -> "Tempering coin..."
                secondsRemaining >= 2 -> "Forging coin..."
                else -> "Casting coin..."
            }
        }
    }

    fun roundsForMint(context: Context, forgeLevel: String?): Int {
        ensureBenchmarked(context)

        val targetSeconds = ForgeLevel.targetSeconds(forgeLevel)
        val unitsPerSecond = prefs(context).getFloat(
            KEY_BENCHMARK_UNITS_PER_SEC,
            DEFAULT_UNITS_PER_SEC.toFloat()
        ).toDouble()

        return (unitsPerSecond * targetSeconds).toInt().coerceAtLeast(1000)
    }

    fun derive(
        context: Context,
        password: String,
        uid: ByteArray,
        salt: ByteArray,
        forgeLevel: String?
    ): DerivedMaterial {
        val rounds = roundsForMint(context, forgeLevel)

        return deriveWithRounds(
            password = password,
            uid = uid,
            salt = salt,
            rounds = rounds
        )
    }

    fun deriveWithRounds(
        password: String,
        uid: ByteArray,
        salt: ByteArray,
        rounds: Int
    ): DerivedMaterial {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        require(uid.isNotEmpty()) { "UID cannot be empty" }
        require(salt.isNotEmpty()) { "Salt cannot be empty" }
        require(rounds >= 1) { "Rounds must be at least 1" }

        val ikm = deriveMasterMaterial(
            password = password,
            uid = uid,
            salt = salt,
            rounds = rounds
        )

        val authKey = hmac(
            ikm,
            "dnaucash-guarded-auth-v1".toByteArray(StandardCharsets.UTF_8)
        ).copyOf(16)

        val contentKey = hmac(
            ikm,
            "dnaucash-guarded-content-v1".toByteArray(StandardCharsets.UTF_8)
        ).copyOf(16)

        return DerivedMaterial(
            authKey = authKey,
            contentKey = contentKey
        )
    }

    fun randomSalt(size: Int = 16): ByteArray {
        return ByteArray(size).also {
            java.security.SecureRandom().nextBytes(it)
        }
    }

    private fun deriveMasterMaterial(
        password: String,
        uid: ByteArray,
        salt: ByteArray,
        rounds: Int
    ): ByteArray {
        val seed = password.toByteArray(StandardCharsets.UTF_8) + uid + salt
        var out = hmac(seed, "dnaucash-guarded-seed-v1".toByteArray(StandardCharsets.UTF_8))

        repeat(rounds) {
            out = hmac(seed, out)
        }

        return out
    }

    private fun benchmarkRounds(rounds: Int): Double {
        val start = SystemClock.elapsedRealtimeNanos()
        runUnitWork(rounds)
        val end = SystemClock.elapsedRealtimeNanos()
        return (end - start) / 1_000_000.0
    }

    private fun runUnitWork(rounds: Int) {
        var out = ByteArray(32) { it.toByte() }

        repeat(rounds) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(BENCHMARK_KEY, "HmacSHA256"))
            out = mac.doFinal(out)
        }
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}