package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.RestTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {

    private val start = 1_000_000L

    @Test
    fun `a fresh timer has its whole duration left`() {
        assertEquals(90, RestTimer.remainingSeconds(start, 90, start))
    }

    @Test
    fun `time remaining counts down with the clock`() {
        assertEquals(60, RestTimer.remainingSeconds(start, 90, start + 30_000))
        assertEquals(1, RestTimer.remainingSeconds(start, 90, start + 89_000))
    }

    @Test
    fun `it stops at zero rather than going negative`() {
        assertEquals(0, RestTimer.remainingSeconds(start, 90, start + 90_000))
        assertEquals(0, RestTimer.remainingSeconds(start, 90, start + 600_000))
    }

    /** The whole point of deriving from the start instant. */
    @Test
    fun `a timer read after the app was away is still correct`() {
        // Backgrounded for two minutes on a 90 second rest.
        assertTrue(RestTimer.isFinished(start, 90, start + 120_000))
        assertFalse(RestTimer.isFinished(start, 180, start + 120_000))
        assertEquals(60, RestTimer.remainingSeconds(start, 180, start + 120_000))
    }

    @Test
    fun `a clock that jumps backwards does not inflate the time left`() {
        assertEquals(90, RestTimer.remainingSeconds(start, 90, start - 5_000))
    }

    @Test
    fun `seconds format as minutes and seconds`() {
        assertEquals("1:30", RestTimer.format(90))
        assertEquals("0:05", RestTimer.format(5))
        assertEquals("2:00", RestTimer.format(120))
        assertEquals("0:00", RestTimer.format(0))
        assertEquals("0:00", RestTimer.format(-5))
    }

    @Test
    fun `extending adds to the duration and is capped`() {
        assertEquals(120, RestTimer.extend(90))
        assertEquals(1_800, RestTimer.extend(1_790))
    }

    @Test
    fun `extending a running timer keeps the elapsed rest`() {
        val extended = RestTimer.extend(90)
        // 30 seconds in, extended to 2 minutes: 90 left, not 120.
        assertEquals(90, RestTimer.remainingSeconds(start, extended, start + 30_000))
    }

    @Test
    fun `the presets are the usual gym rests and include the default`() {
        assertTrue(RestTimer.DEFAULT_SECONDS in RestTimer.PRESETS_SECONDS)
        assertEquals(listOf(60, 90, 120, 180), RestTimer.PRESETS_SECONDS)
    }
}
