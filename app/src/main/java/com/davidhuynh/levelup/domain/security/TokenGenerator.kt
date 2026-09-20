package com.davidhuynh.levelup.domain.security

import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Random identifiers.
 *
 * Note on the session token: with no server there is nothing to replay it against, so it
 * is a local marker that a sign-in happened and when it expires, not a bearer credential.
 * When Firebase Auth replaces this, the token comes from FirebaseAuth instead and only
 * SessionStore changes.
 */
interface TokenGenerator {
    fun newSessionToken(): String
    fun newId(): String
}

class SecureTokenGenerator(
    private val random: SecureRandom = SecureRandom(),
) : TokenGenerator {

    override fun newSessionToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    override fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
