package com.dnaucash.app

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KdfEngine {

    data class Params(
        val iterations: Int,
        val memoryRounds: Int
    )

    fun paramsForTier(workTier: String?): Params {
        return when ((workTier ?: "cast").lowercase()) {
            "cast" -> Params(iterations = 50_000, memoryRounds = 1)
            "forged" -> Params(iterations = 150_000, memoryRounds = 2)
            "tempered" -> Params(iterations = 350_000, memoryRounds = 3)
            "hardened" -> Params(iterations = 900_000, memoryRounds = 4)
            else -> Params(iterations = 50_000, memoryRounds = 1)
        }
    }

    fun deriveKey(
        password: ByteArray?,
        uid: ByteArray,
        salt: ByteArray,
        workTier: String?
    ): ByteArray {

        val params = paramsForTier(workTier)

        var state = sha256(
            (password ?: ByteArray(0)) +
                    uid +
                    salt
        )

        repeat(params.memoryRounds) {
            state = memoryMix(state)
        }

        repeat(params.iterations) {
            state = hmacSha256(state, state)
        }

        return state
    }

    private fun memoryMix(input: ByteArray): ByteArray {
        var out = input
        repeat(1024) {
            out = sha256(out + input)
        }
        return out
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}