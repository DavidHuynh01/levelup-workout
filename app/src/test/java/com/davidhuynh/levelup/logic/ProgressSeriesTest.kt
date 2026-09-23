package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.ProgressSeries
import com.davidhuynh.levelup.fake.prSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressSeriesTest {

    @Test
    fun `no sets means no points to plot`() {
        assertTrue(ProgressSeries.sessionBests(emptyList()).isEmpty())
    }

    @Test
    fun `each workout becomes one point`() {
        val points = ProgressSeries.sessionBests(
            listOf(
                prSet("a1", reps = 8, weightKg = 80.0, day = "2026-03-01", workoutId = "w1"),
                prSet("a2", reps = 6, weightKg = 90.0, day = "2026-03-01", workoutId = "w1"),
                prSet("b1", reps = 5, weightKg = 100.0, day = "2026-03-08", workoutId = "w2"),
            )
        )
        assertEquals(2, points.size)
    }

    @Test
    fun `the session point uses the best set, not the last one`() {
        val points = ProgressSeries.sessionBests(
            listOf(
                prSet("heavy", reps = 5, weightKg = 100.0, day = "2026-03-01", workoutId = "w1"),
                prSet("backoff", reps = 8, weightKg = 70.0, day = "2026-03-01", workoutId = "w1", completedAtOffset = 60),
            )
        )

        assertEquals(116.67, points.single().estimated1rmKg, 0.01)
    }

    @Test
    fun `points come back in date order regardless of input order`() {
        val points = ProgressSeries.sessionBests(
            listOf(
                prSet("c", reps = 5, weightKg = 110.0, day = "2026-03-15", workoutId = "w3"),
                prSet("a", reps = 5, weightKg = 90.0, day = "2026-03-01", workoutId = "w1"),
                prSet("b", reps = 5, weightKg = 100.0, day = "2026-03-08", workoutId = "w2"),
            )
        )
        assertEquals(
            listOf("2026-03-01", "2026-03-08", "2026-03-15"),
            points.map { it.date.toString() },
        )
    }

    @Test
    fun `a bad session shows as a dip rather than being hidden`() {
        val points = ProgressSeries.sessionBests(
            listOf(
                prSet("a", reps = 5, weightKg = 100.0, day = "2026-03-01", workoutId = "w1"),
                prSet("b", reps = 5, weightKg = 80.0, day = "2026-03-08", workoutId = "w2"),
                prSet("c", reps = 5, weightKg = 105.0, day = "2026-03-15", workoutId = "w3"),
            )
        )
        val estimates = points.map { it.estimated1rmKg }
        assertTrue("the middle session should be lower", estimates[1] < estimates[0])
        assertTrue("and the last one higher again", estimates[2] > estimates[1])
    }

    @Test
    fun `warmups do not count toward the session point or its volume`() {
        val points = ProgressSeries.sessionBests(
            listOf(
                prSet("warm", reps = 10, weightKg = 200.0, day = "2026-03-01", workoutId = "w1", isWarmup = true),
                prSet("work", reps = 5, weightKg = 100.0, day = "2026-03-01", workoutId = "w1"),
            )
        )
        assertEquals(116.67, points.single().estimated1rmKg, 0.01)
        assertEquals(500.0, points.single().volumeKg, 0.01)
    }

    @Test
    fun `high rep sets fall back to raw weight instead of dropping out`() {

        val points = ProgressSeries.sessionBests(
            listOf(prSet("a", reps = 20, weightKg = 60.0, day = "2026-03-01", workoutId = "w1"))
        )
        assertEquals(60.0, points.single().estimated1rmKg, 0.01)
    }

    @Test
    fun `the point carries the best set so the UI can show what produced it`() {
        val points = ProgressSeries.sessionBests(
            listOf(
                prSet("a", reps = 3, weightKg = 120.0, day = "2026-03-01", workoutId = "w1"),
                prSet("b", reps = 10, weightKg = 80.0, day = "2026-03-01", workoutId = "w1"),
            )
        )
        assertEquals(3, points.single().bestSetReps)
        assertEquals(120.0, points.single().bestSetWeightKg, 0.01)
    }
}
