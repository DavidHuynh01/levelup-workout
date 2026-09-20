package com.davidhuynh.levelup.domain.security

/** What gets stored for one account. The plaintext password is never persisted. */
data class PasswordHash(
    val hash: String,
    val salt: String,
    val iterations: Int,
)

interface PasswordHasher {
    fun hash(password: String): PasswordHash

    fun verify(password: String, stored: PasswordHash): Boolean

    /** True when [stored] was made with weaker settings than the current ones. */
    fun needsRehash(stored: PasswordHash): Boolean
}
