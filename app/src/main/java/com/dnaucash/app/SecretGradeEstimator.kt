package com.dnaucash.app

object SecretGradeEstimator {

    fun estimate(secret: String): String {
        val trimmed = secret.trim()

        if (trimmed.isEmpty()) {
            return PublicCoinRecord.SECRET_BRONZE
        }

        if (isBronzePin(trimmed)) {
            return PublicCoinRecord.SECRET_BRONZE
        }

        if (isPlatinum(trimmed)) {
            return PublicCoinRecord.SECRET_PLATINUM
        }

        if (isGold(trimmed)) {
            return PublicCoinRecord.SECRET_GOLD
        }

        return PublicCoinRecord.SECRET_SILVER
    }

    fun displayLabel(secretGrade: String?): String {
        return when ((secretGrade ?: PublicCoinRecord.SECRET_BRONZE).lowercase()) {
            PublicCoinRecord.SECRET_BRONZE -> "Bronze"
            PublicCoinRecord.SECRET_SILVER -> "Silver"
            PublicCoinRecord.SECRET_GOLD -> "Gold"
            PublicCoinRecord.SECRET_PLATINUM -> "Platinum"
            else -> "Bronze"
        }
    }

    fun description(secretGrade: String?): String {
        return when ((secretGrade ?: PublicCoinRecord.SECRET_BRONZE).lowercase()) {
            PublicCoinRecord.SECRET_BRONZE ->
                "4–6 digit PIN. Very fast to enter, but low entropy."
            PublicCoinRecord.SECRET_SILVER ->
                "Basic password. Suitable for moderate protection."
            PublicCoinRecord.SECRET_GOLD ->
                "Strong password. Recommended for most guarded tokens."
            PublicCoinRecord.SECRET_PLATINUM ->
                "Long passphrase. Highest secret strength."
            else ->
                "4–6 digit PIN. Very fast to enter, but low entropy."
        }
    }

    private fun isBronzePin(secret: String): Boolean {
        return secret.length in 4..6 && secret.all { it.isDigit() }
    }

    private fun isGold(secret: String): Boolean {
        val hasUpper = secret.any { it.isUpperCase() }
        val hasLower = secret.any { it.isLowerCase() }
        val hasDigit = secret.any { it.isDigit() }
        val hasSymbol = secret.any { !it.isLetterOrDigit() }

        return when {
            secret.length >= 12 && hasUpper && hasLower && hasDigit -> true
            secret.length >= 12 && hasLower && hasDigit && hasSymbol -> true
            secret.length >= 14 && hasLower && hasDigit -> true
            else -> false
        }
    }

    private fun isPlatinum(secret: String): Boolean {
        val words = secret.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size >= 3) return true
        if (secret.length >= 16) return true
        return false
    }
}