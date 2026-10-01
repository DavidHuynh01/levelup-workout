package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.PasswordPolicy
import com.davidhuynh.levelup.domain.security.PasswordHash
import com.davidhuynh.levelup.domain.security.Pbkdf2PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FIELD_CURRENT = "currentPassword"
private const val FIELD_NEW = "newPassword"
private const val FIELD_CONFIRM = "confirmPassword"

class ChangePasswordRulesTest {

    private val hasher = Pbkdf2PasswordHasher(iterations = 1_000)

    private val currentPassword = "squat225lbs"

    private val stored: PasswordHash = hasher.hash(currentPassword)

    private fun rejectedField(
        current: String,
        new: String,
        confirm: String,
    ): String? {
        if (current.isEmpty()) return FIELD_CURRENT
        if (!hasher.verify(current, stored)) return FIELD_CURRENT
        if (PasswordPolicy.validate(new).isNotEmpty()) return FIELD_NEW
        if (new == current) return FIELD_NEW
        if (new != confirm) return FIELD_CONFIRM
        return null
    }

    @Test
    fun `the correct current password verifies against the stored hash`() {
        assertTrue(hasher.verify(currentPassword, stored))
    }

    @Test
    fun `a wrong current password does not verify`() {
        assertFalse(hasher.verify("squat226lbs", stored))
        assertFalse(hasher.verify(currentPassword.uppercase(), stored))
    }

    @Test
    fun `a valid change passes every rule`() {
        assertNull(rejectedField(currentPassword, "bench185lbs", "bench185lbs"))
    }

    @Test
    fun `a wrong current password is blamed on the current password field`() {
        assertEquals(
            FIELD_CURRENT,
            rejectedField("deadlift315", "bench185lbs", "bench185lbs"),
        )
    }

    @Test
    fun `an empty current password is blamed on the current password field`() {
        assertEquals(FIELD_CURRENT, rejectedField("", "bench185lbs", "bench185lbs"))
    }

    @Test
    fun `a weak new password is rejected by the policy`() {
        assertTrue(PasswordPolicy.validate("abc1").contains(PasswordPolicy.Violation.TOO_SHORT))
        assertEquals(FIELD_NEW, rejectedField(currentPassword, "abc1", "abc1"))
    }

    @Test
    fun `a common new password is rejected even when it is long enough`() {
        assertTrue(
            PasswordPolicy.validate("password123").contains(PasswordPolicy.Violation.TOO_COMMON)
        )
        assertEquals(FIELD_NEW, rejectedField(currentPassword, "password123", "password123"))
    }

    @Test
    fun `a new password with no digits is rejected`() {
        assertEquals(FIELD_NEW, rejectedField(currentPassword, "benchpress", "benchpress"))
    }

    @Test
    fun `a mismatched confirmation is blamed on the confirmation field`() {
        assertEquals(
            FIELD_CONFIRM,
            rejectedField(currentPassword, "bench185lbs", "bench186lbs"),
        )
    }

    @Test
    fun `an empty confirmation is blamed on the confirmation field`() {
        assertEquals(FIELD_CONFIRM, rejectedField(currentPassword, "bench185lbs", ""))
    }

    @Test
    fun `reusing the current password is rejected`() {
        assertEquals(
            FIELD_NEW,
            rejectedField(currentPassword, currentPassword, currentPassword),
        )
    }

    @Test
    fun `reuse is reported before a mismatched confirmation`() {
        assertEquals(
            FIELD_NEW,
            rejectedField(currentPassword, currentPassword, "something else"),
        )
    }

    @Test
    fun `a bad current password is reported before anything about the new one`() {
        assertEquals(FIELD_CURRENT, rejectedField("wrongpass1", "abc1", "nope"))
    }

    @Test
    fun `a leading space in the new password is surfaced rather than trimmed away`() {
        assertEquals(FIELD_NEW, rejectedField(currentPassword, " bench100", " bench100"))
    }
}

class ChangePasswordHashingTest {

    private val hasher = Pbkdf2PasswordHasher(iterations = 1_000)

    @Test
    fun `the rehashed password replaces the old material and verifies`() {
        val old = hasher.hash("squat225lbs")
        val updated = hasher.hash("bench185lbs")

        assertNotEquals(old.hash, updated.hash)
        assertNotEquals(old.salt, updated.salt)

        assertTrue(hasher.verify("bench185lbs", updated))
        assertFalse(hasher.verify("squat225lbs", updated))
    }

    @Test
    fun `a change made on an old hash lands on the current iteration count`() {
        val legacy = Pbkdf2PasswordHasher(iterations = 500).hash("squat225lbs")
        assertTrue(hasher.needsRehash(legacy))

        val updated = hasher.hash("bench185lbs")
        assertEquals(1_000, updated.iterations)
        assertFalse(hasher.needsRehash(updated))
    }

    @Test
    fun `the old password stops working once the new hash is stored`() {
        val updated = hasher.hash("deadlift315kg")
        assertFalse(hasher.verify("squat225lbs", updated))
        assertTrue(hasher.verify("deadlift315kg", updated))
    }
}
