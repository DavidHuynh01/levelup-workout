package com.davidhuynh.levelup.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Pbkdf2PasswordHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) : PasswordHasher {

    override fun hash(password: String): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = derive(password, salt, iterations)
        return PasswordHash(
            hash = encoder.encodeToString(derived),
            salt = encoder.encodeToString(salt),
            iterations = iterations,
        )
    }

    override fun verify(password: String, stored: PasswordHash): Boolean {
        val salt = runCatching { decoder.decode(stored.salt) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(stored.hash) }.getOrNull() ?: return false
        val actual = derive(password, salt, stored.iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    override fun needsRehash(stored: PasswordHash): Boolean = stored.iterations < iterations

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"

        const val DEFAULT_ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val KEY_BITS = 256

        val encoder: Base64.Encoder = Base64.getEncoder()
        val decoder: Base64.Decoder = Base64.getDecoder()
    }
}
