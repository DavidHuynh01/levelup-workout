package com.davidhuynh.levelup.domain.security

data class PasswordHash(
    val hash: String,
    val salt: String,
    val iterations: Int,
)

interface PasswordHasher {
    fun hash(password: String): PasswordHash

    fun verify(password: String, stored: PasswordHash): Boolean

    fun needsRehash(stored: PasswordHash): Boolean
}
