package com.dnaucash.app

enum class ForgeLevel(
    val wireValue: String,
    val displayName: String,
    val targetSeconds: Int
) {
    CAST(
        wireValue = PublicCoinRecord.FORGE_CAST,
        displayName = "Cast",
        targetSeconds = 1
    ),
    FORGED(
        wireValue = PublicCoinRecord.FORGE_FORGED,
        displayName = "Forged",
        targetSeconds = 3
    ),
    TEMPERED(
        wireValue = PublicCoinRecord.FORGE_TEMPERED,
        displayName = "Tempered",
        targetSeconds = 7
    ),
    HARDENED(
        wireValue = PublicCoinRecord.FORGE_HARDENED,
        displayName = "Hardened",
        targetSeconds = 21
    );

    companion object {
        fun fromWireValue(value: String?): ForgeLevel {
            return entries.firstOrNull { it.wireValue == value } ?: CAST
        }

        fun displayLabel(value: String?): String {
            return fromWireValue(value).displayName
        }

        fun targetSeconds(value: String?): Int {
            return fromWireValue(value).targetSeconds
        }
    }
}