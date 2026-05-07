package com.dnaucash.app

import org.json.JSONObject

object PublicCoinRecordCodec {

    fun toUnrevealedJson(record: PublicCoinRecord): String {
        val obj = JSONObject()

        obj.put("x", PublicCoinRecord.BRANDING)
        obj.put("v", record.v)

        obj.put("a", record.a)
        obj.put("s", PublicCoinRecord.WIRE_STATE_UNREVEALED)
        obj.put("m", record.m)

        obj.put("t", tokenTypeToWire(record.tokenType))

        if (record.secretGrade != null) {
            obj.put("g", secretGradeToWire(record.secretGrade))
        }

        if (record.forgeLevel != null) {
            obj.put("f", forgeLevelToWire(record.forgeLevel))
        }

        if (record.k != null) {
            obj.put("k", record.k)
        }

        return obj.toString()
    }

    fun parse(json: String): PublicCoinRecord {
        val obj = JSONObject(json)

        val tokenType = parseTokenType(obj)
        val secretGrade = parseSecretGrade(obj, tokenType)

        return PublicCoinRecord(
            x = obj.optString("x", PublicCoinRecord.BRANDING),
            v = obj.optInt("v", 2),

            a = obj.getString("a"),
            s = parseState(obj),
            m = obj.optString("m", ""),

            tokenType = tokenType,
            secretGrade = secretGrade,
            forgeLevel = parseForgeLevel(obj),

            k = obj.optString("k", null)
        )
    }

    // ===== PARSERS =====

    private fun parseState(obj: JSONObject): String {
        return when (obj.optString("s")) {
            PublicCoinRecord.WIRE_STATE_REVEALED,
            PublicCoinRecord.STATE_REVEALED -> PublicCoinRecord.STATE_REVEALED
            else -> PublicCoinRecord.STATE_UNREVEALED
        }
    }

    private fun parseTokenType(obj: JSONObject): String {
        return when (obj.optString("t")) {
            PublicCoinRecord.WIRE_TOKEN_GUARDED,
            PublicCoinRecord.TOKEN_TYPE_GUARDED -> PublicCoinRecord.TOKEN_TYPE_GUARDED

            PublicCoinRecord.WIRE_TOKEN_STEALTH,
            PublicCoinRecord.TOKEN_TYPE_STEALTH -> PublicCoinRecord.TOKEN_TYPE_STEALTH

            else -> PublicCoinRecord.TOKEN_TYPE_BEARER
        }
    }

    private fun parseSecretGrade(obj: JSONObject, tokenType: String): String? {
        if (tokenType != PublicCoinRecord.TOKEN_TYPE_GUARDED &&
            tokenType != PublicCoinRecord.TOKEN_TYPE_STEALTH
        ) return null

        return when (obj.optString("g")) {
            PublicCoinRecord.WIRE_SECRET_BRONZE -> PublicCoinRecord.SECRET_BRONZE
            PublicCoinRecord.WIRE_SECRET_SILVER -> PublicCoinRecord.SECRET_SILVER
            PublicCoinRecord.WIRE_SECRET_GOLD -> PublicCoinRecord.SECRET_GOLD
            PublicCoinRecord.WIRE_SECRET_PLATINUM -> PublicCoinRecord.SECRET_PLATINUM
            else -> PublicCoinRecord.SECRET_BRONZE
        }
    }

    private fun parseForgeLevel(obj: JSONObject): String? {
        return when (obj.optString("f")) {
            PublicCoinRecord.WIRE_FORGE_CAST,
            PublicCoinRecord.FORGE_CAST -> PublicCoinRecord.FORGE_CAST

            PublicCoinRecord.WIRE_FORGE_FORGED,
            PublicCoinRecord.FORGE_FORGED -> PublicCoinRecord.FORGE_FORGED

            PublicCoinRecord.WIRE_FORGE_TEMPERED,
            PublicCoinRecord.FORGE_TEMPERED -> PublicCoinRecord.FORGE_TEMPERED

            PublicCoinRecord.WIRE_FORGE_HARDENED,
            PublicCoinRecord.FORGE_HARDENED -> PublicCoinRecord.FORGE_HARDENED

            else -> null
        }
    }

    // ===== ENCODERS =====

    private fun tokenTypeToWire(tokenType: String): String {
        return when (tokenType) {
            PublicCoinRecord.TOKEN_TYPE_GUARDED -> PublicCoinRecord.WIRE_TOKEN_GUARDED
            PublicCoinRecord.TOKEN_TYPE_STEALTH -> PublicCoinRecord.WIRE_TOKEN_STEALTH
            else -> PublicCoinRecord.WIRE_TOKEN_BEARER
        }
    }

    private fun secretGradeToWire(secret: String): String {
        return when (secret) {
            PublicCoinRecord.SECRET_SILVER -> PublicCoinRecord.WIRE_SECRET_SILVER
            PublicCoinRecord.SECRET_GOLD -> PublicCoinRecord.WIRE_SECRET_GOLD
            PublicCoinRecord.SECRET_PLATINUM -> PublicCoinRecord.WIRE_SECRET_PLATINUM
            else -> PublicCoinRecord.WIRE_SECRET_BRONZE
        }
    }

    private fun forgeLevelToWire(forge: String): String {
        return when (forge) {
            PublicCoinRecord.FORGE_FORGED -> PublicCoinRecord.WIRE_FORGE_FORGED
            PublicCoinRecord.FORGE_TEMPERED -> PublicCoinRecord.WIRE_FORGE_TEMPERED
            PublicCoinRecord.FORGE_HARDENED -> PublicCoinRecord.WIRE_FORGE_HARDENED
            else -> PublicCoinRecord.WIRE_FORGE_CAST
        }
    }
    // ===== FACTORY HELPERS =====

    fun newUnrevealedBearer(
        address: String,
        message: String,
        forgeLevel: String,
        k: String?
    ): PublicCoinRecord {
        return PublicCoinRecord(
            a = address,
            s = PublicCoinRecord.STATE_UNREVEALED,
            m = message,
            tokenType = PublicCoinRecord.TOKEN_TYPE_BEARER,
            forgeLevel = forgeLevel,
            k = k
        )
    }

    fun newUnrevealedGuarded(
        address: String,
        message: String,
        secretGrade: String,
        forgeLevel: String
    ): PublicCoinRecord {
        return PublicCoinRecord(
            a = address,
            s = PublicCoinRecord.STATE_UNREVEALED,
            m = message,
            tokenType = PublicCoinRecord.TOKEN_TYPE_GUARDED,
            secretGrade = secretGrade,
            forgeLevel = forgeLevel
        )
    }

    fun newUnrevealedStealth(
        address: String,
        message: String,
        secretGrade: String,
        forgeLevel: String
    ): PublicCoinRecord {
        return PublicCoinRecord(
            a = address,
            s = PublicCoinRecord.STATE_UNREVEALED,
            m = message,
            tokenType = PublicCoinRecord.TOKEN_TYPE_STEALTH,
            secretGrade = secretGrade,
            forgeLevel = forgeLevel
        )
    }

// ===== STATE TRANSITIONS =====

    fun toRevealed(record: PublicCoinRecord): PublicCoinRecord {
        return record.copy(
            s = PublicCoinRecord.STATE_REVEALED
        )
    }

    fun toRevealedJson(record: PublicCoinRecord): String {
        val revealed = toRevealed(record)
        return toUnrevealedJson(revealed).replace(
            "\"s\":\"${PublicCoinRecord.WIRE_STATE_UNREVEALED}\"",
            "\"s\":\"${PublicCoinRecord.WIRE_STATE_REVEALED}\""
        )
    }
}