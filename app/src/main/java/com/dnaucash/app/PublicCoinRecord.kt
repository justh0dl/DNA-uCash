package com.dnaucash.app

data class PublicCoinRecord(
    val x: String? = null,          // branding / app marker
    val v: Int = 2,                 // schema version

    val a: String,                  // address
    val s: String,                  // state: unrevealed | revealed
    val m: String = "",             // public message

    val h: String? = null,          // derived/display help text
    val w: String? = null,          // derived/display warning text

    val tokenType: String = TOKEN_TYPE_BEARER,
    val secretGrade: String? = null, // guarded + stealth
    val forgeLevel: String? = null,  // shared
    val k: String? = null            // bearer salt only
) {
    companion object {
        const val BRANDING = "DNAuCashv101"

        const val STATE_UNREVEALED = "unrevealed"
        const val STATE_REVEALED = "revealed"

        const val TOKEN_TYPE_BEARER = "bearer"
        const val TOKEN_TYPE_GUARDED = "guarded"
        const val TOKEN_TYPE_STEALTH = "stealth"

        const val SECRET_BRONZE = "bronze"
        const val SECRET_SILVER = "silver"
        const val SECRET_GOLD = "gold"
        const val SECRET_PLATINUM = "platinum"

        const val FORGE_CAST = "cast"
        const val FORGE_FORGED = "forged"
        const val FORGE_TEMPERED = "tempered"
        const val FORGE_HARDENED = "hardened"

        // Compact wire values
        const val WIRE_STATE_UNREVEALED = "u"
        const val WIRE_STATE_REVEALED = "r"

        const val WIRE_TOKEN_BEARER = "b"
        const val WIRE_TOKEN_GUARDED = "g"
        const val WIRE_TOKEN_STEALTH = "s"

        const val WIRE_SECRET_BRONZE = "b"
        const val WIRE_SECRET_SILVER = "s"
        const val WIRE_SECRET_GOLD = "g"
        const val WIRE_SECRET_PLATINUM = "p"

        const val WIRE_FORGE_CAST = "c"
        const val WIRE_FORGE_FORGED = "f"
        const val WIRE_FORGE_TEMPERED = "t"
        const val WIRE_FORGE_HARDENED = "h"
    }

    val isBearer: Boolean
        get() = tokenType == TOKEN_TYPE_BEARER

    val isGuarded: Boolean
        get() = tokenType == TOKEN_TYPE_GUARDED

    val isStealth: Boolean
        get() = tokenType == TOKEN_TYPE_STEALTH

    val isPasswordProtected: Boolean
        get() = isGuarded || isStealth

    val isUnrevealed: Boolean
        get() = s == STATE_UNREVEALED

    val isRevealed: Boolean
        get() = s == STATE_REVEALED
}