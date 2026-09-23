package com.davidhuynh.levelup.domain.logic

object PasswordPolicy {

    const val MIN_LENGTH = 8

    const val MAX_LENGTH = 72

    private val COMMON_PASSWORDS = setOf(
        "password", "password1", "password123", "12345678", "123456789",
        "qwerty123", "qwertyuiop", "letmein1", "iloveyou", "welcome1",
        "abc12345", "football1", "baseball1", "trustno1", "changeme",
        "levelup1", "workout1",
    )

    enum class Violation(val message: String) {
        TOO_SHORT("Use at least $MIN_LENGTH characters"),
        TOO_LONG("Use at most $MAX_LENGTH characters"),
        NO_LETTER("Include at least one letter"),
        NO_DIGIT("Include at least one number"),
        SURROUNDING_WHITESPACE("Remove the space at the start or end"),
        TOO_COMMON("That password is too common — pick something less guessable"),
    }

    fun validate(password: String): List<Violation> = buildList {
        if (password.length < MIN_LENGTH) add(Violation.TOO_SHORT)
        if (password.length > MAX_LENGTH) add(Violation.TOO_LONG)
        if (password.none { it.isLetter() }) add(Violation.NO_LETTER)
        if (password.none { it.isDigit() }) add(Violation.NO_DIGIT)
        if (password != password.trim()) add(Violation.SURROUNDING_WHITESPACE)
        if (password.lowercase() in COMMON_PASSWORDS) add(Violation.TOO_COMMON)
    }

    fun isValid(password: String): Boolean = validate(password).isEmpty()

    fun strength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= MIN_LENGTH) score++
        if (password.length >= 12) score++
        if (password.any { it.isLetter() } && password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() } || password.length >= 16) score++
        if (password.lowercase() in COMMON_PASSWORDS) return 0
        return score.coerceIn(0, 4)
    }
}

object EmailValidator {
    private val PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    fun isValid(email: String): Boolean = PATTERN.matches(email.trim())

    fun normalize(email: String): String = email.trim().lowercase()
}
