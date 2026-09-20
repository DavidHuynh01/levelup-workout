package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.EmailValidator
import com.davidhuynh.levelup.domain.logic.PasswordPolicy
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.security.PasswordHash
import com.davidhuynh.levelup.domain.security.Pbkdf2PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test
    fun `a reasonable password passes`() {
        assertTrue(PasswordPolicy.validate("squat225lbs").isEmpty())
    }

    @Test
    fun `short passwords are rejected`() {
        assertTrue(PasswordPolicy.validate("abc1").contains(PasswordPolicy.Violation.TOO_SHORT))
    }

    @Test
    fun `absurdly long passwords are rejected so key derivation stays bounded`() {
        val violations = PasswordPolicy.validate("a1" + "x".repeat(100))
        assertTrue(violations.contains(PasswordPolicy.Violation.TOO_LONG))
    }

    @Test
    fun `digits alone are not enough`() {
        assertTrue(PasswordPolicy.validate("12345678").contains(PasswordPolicy.Violation.NO_LETTER))
    }

    @Test
    fun `letters alone are not enough`() {
        assertTrue(PasswordPolicy.validate("deadlifts").contains(PasswordPolicy.Violation.NO_DIGIT))
    }

    @Test
    fun `common passwords are blocked even when they satisfy the other rules`() {
        val violations = PasswordPolicy.validate("password123")
        assertTrue(violations.contains(PasswordPolicy.Violation.TOO_COMMON))
    }

    @Test
    fun `a stray leading space is called out rather than silently trimmed`() {
        assertTrue(PasswordPolicy.validate(" bench100").contains(PasswordPolicy.Violation.SURROUNDING_WHITESPACE))
    }

    /** The form shows every problem at once, so all of them must be reported together. */
    @Test
    fun `every violation is reported in one pass`() {
        val violations = PasswordPolicy.validate("ab")
        assertTrue(violations.contains(PasswordPolicy.Violation.TOO_SHORT))
        assertTrue(violations.contains(PasswordPolicy.Violation.NO_DIGIT))
        assertEquals(2, violations.size)
    }

    @Test
    fun `strength rises with length and falls to zero for blocked passwords`() {
        assertEquals(0, PasswordPolicy.strength(""))
        assertTrue(PasswordPolicy.strength("bench100") < PasswordPolicy.strength("bench100kgToday!"))
        assertEquals(0, PasswordPolicy.strength("password1"))
    }
}

class EmailValidatorTest {

    @Test
    fun `ordinary addresses pass`() {
        assertTrue(EmailValidator.isValid("david@uta.edu"))
        assertTrue(EmailValidator.isValid("david.huynh+gym@mavs.uta.edu"))
    }

    @Test
    fun `addresses missing a domain or an at sign fail`() {
        assertFalse(EmailValidator.isValid("david"))
        assertFalse(EmailValidator.isValid("david@localhost"))
        assertFalse(EmailValidator.isValid("david @uta.edu"))
        assertFalse(EmailValidator.isValid(""))
    }

    @Test
    fun `normalising trims and lowercases so signup and login always agree`() {
        assertEquals("david@uta.edu", EmailValidator.normalize("  David@UTA.edu "))
    }
}

class Pbkdf2PasswordHasherTest {

    // Far below the production count: these tests only check the mechanics, and 120k
    // iterations per assertion would make the suite crawl.
    private val hasher = Pbkdf2PasswordHasher(iterations = 1_000)

    @Test
    fun `the right password verifies`() {
        val stored = hasher.hash("squat225lbs")
        assertTrue(hasher.verify("squat225lbs", stored))
    }

    @Test
    fun `a wrong password does not verify`() {
        val stored = hasher.hash("squat225lbs")
        assertFalse(hasher.verify("squat226lbs", stored))
        assertFalse(hasher.verify("", stored))
    }

    @Test
    fun `the same password hashed twice gives different output`() {
        val first = hasher.hash("bench185lbs")
        val second = hasher.hash("bench185lbs")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        // Both still verify: the salt is what differs, not the password.
        assertTrue(hasher.verify("bench185lbs", first))
        assertTrue(hasher.verify("bench185lbs", second))
    }

    @Test
    fun `verification uses the iteration count the hash was stored with`() {
        val weak = Pbkdf2PasswordHasher(iterations = 500).hash("deadlift315")
        assertEquals(500, weak.iterations)
        // A hasher configured for more iterations still verifies an older, weaker hash.
        assertTrue(hasher.verify("deadlift315", weak))
    }

    @Test
    fun `an old hash is flagged for upgrade and a current one is not`() {
        assertTrue(hasher.needsRehash(PasswordHash("x", "y", 500)))
        assertFalse(hasher.needsRehash(hasher.hash("overhead95")))
    }

    @Test
    fun `corrupt stored material fails instead of crashing`() {
        assertFalse(hasher.verify("anything", PasswordHash("not-base64!!", "also-not!!", 1_000)))
    }
}

class WeightConverterTest {

    @Test
    fun `pounds round trip back to the same kilograms`() {
        val lbs = WeightConverter.fromKg(100.0, WeightUnit.LB)
        assertEquals(220.46, lbs, 0.01)
        assertEquals(100.0, WeightConverter.toKg(lbs, WeightUnit.LB), 0.0001)
    }

    @Test
    fun `kilograms pass through untouched`() {
        assertEquals(82.5, WeightConverter.toKg(82.5, WeightUnit.KG), 0.0001)
        assertEquals(82.5, WeightConverter.fromKg(82.5, WeightUnit.KG), 0.0001)
    }

    @Test
    fun `whole numbers display without a trailing decimal`() {
        assertEquals("100 kg", WeightConverter.format(100.0, WeightUnit.KG))
        assertEquals("102.5 kg", WeightConverter.format(102.5, WeightUnit.KG))
    }

    @Test
    fun `large volumes are abbreviated`() {
        assertEquals("450 kg", WeightConverter.formatVolume(450.0, WeightUnit.KG))
        assertEquals("12.5k kg", WeightConverter.formatVolume(12_500.0, WeightUnit.KG))
        assertEquals("1.2M kg", WeightConverter.formatVolume(1_200_000.0, WeightUnit.KG))
    }
}
