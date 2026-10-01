package com.davidhuynh.levelup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidhuynh.levelup.ui.workout.log.RestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestBarStateTest {

    private val startedAt = 1_700_000_000_000L

    private fun rest(durationSeconds: Int, remainingSeconds: Int) = RestState(
        startedAtMillis = startedAt,
        durationSeconds = durationSeconds,
        remainingSeconds = remainingSeconds,
    )

    @Test
    fun labelReadsAsMinutesAndSeconds() {
        assertEquals("1:30", rest(90, 90).label)
        assertEquals("0:45", rest(90, 45).label)
        assertEquals("0:05", rest(90, 5).label)
        assertEquals("0:00", rest(90, 0).label)
    }

    @Test
    fun fractionIsFullAtTheStartAndEmptyAtTheEnd() {
        assertEquals(1f, rest(90, 90).fraction, 0.0001f)
        assertEquals(0f, rest(90, 0).fraction, 0.0001f)
    }

    @Test
    fun fractionShrinksAsTheRestRunsDown() {
        assertEquals(0.5f, rest(90, 45).fraction, 0.0001f)
        assertEquals(0.25f, rest(120, 30).fraction, 0.0001f)
    }

    @Test
    fun fractionStaysSafeWhenTheDurationIsNotUsable() {
        assertEquals(0f, rest(0, 0).fraction, 0.0001f)
        assertEquals(0f, rest(-30, 10).fraction, 0.0001f)
    }

    @Test
    fun isFinishedOnlyOnceNothingIsLeft() {
        assertFalse(rest(90, 90).isFinished)
        assertFalse(rest(90, 1).isFinished)
        assertTrue(rest(90, 0).isFinished)
    }

    @Test
    fun extendingKeepsTheBarFromOverflowing() {
        val extended = rest(120, 90)
        assertEquals(0.75f, extended.fraction, 0.0001f)
        assertTrue(extended.fraction <= 1f)
    }
}
