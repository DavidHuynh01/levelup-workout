package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.ConsistencyCalculator
import com.davidhuynh.levelup.domain.logic.StreakCalculator
import com.davidhuynh.levelup.fake.day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakCalculatorTest {

    private val today = day("2026-03-15")

    @Test
    fun `no workouts means no streak and no last date`() {
        val result = StreakCalculator.calculate(emptyList(), today)
        assertEquals(0, result.currentStreakDays)
        assertEquals(0, result.longestStreakDays)
        assertNull(result.lastWorkoutDate)
    }

    @Test
    fun `training today is a streak of one`() {
        val result = StreakCalculator.calculate(listOf(today), today)
        assertEquals(1, result.currentStreakDays)
        assertEquals(1, result.longestStreakDays)
    }

    @Test
    fun `three consecutive days ending today is a streak of three`() {
        val days = listOf(day("2026-03-13"), day("2026-03-14"), today)
        assertEquals(3, StreakCalculator.calculate(days, today).currentStreakDays)
    }

    @Test
    fun `a streak ending yesterday is still alive`() {
        val days = listOf(day("2026-03-12"), day("2026-03-13"), day("2026-03-14"))
        assertEquals(3, StreakCalculator.calculate(days, today).currentStreakDays)
    }

    @Test
    fun `a two day gap breaks the streak`() {
        val days = listOf(day("2026-03-11"), day("2026-03-12"), day("2026-03-13"))
        val result = StreakCalculator.calculate(days, today)
        assertEquals(0, result.currentStreakDays)
        assertEquals(3, result.longestStreakDays)
    }

    @Test
    fun `two workouts on the same day count as one day`() {
        val days = listOf(today, today, day("2026-03-14"))
        assertEquals(2, StreakCalculator.calculate(days, today).currentStreakDays)
    }

    @Test
    fun `the longest streak can be in the past`() {
        val days = listOf(
            day("2026-01-05"), day("2026-01-06"), day("2026-01-07"),
            day("2026-01-08"), day("2026-01-09"),
            day("2026-03-14"), today,
        )
        val result = StreakCalculator.calculate(days, today)
        assertEquals(2, result.currentStreakDays)
        assertEquals(5, result.longestStreakDays)
    }

    @Test
    fun `streaks cross the end of a month`() {
        val days = listOf(day("2026-02-27"), day("2026-02-28"), day("2026-03-01"))
        assertEquals(3, StreakCalculator.calculate(days, day("2026-03-01")).currentStreakDays)
    }

    @Test
    fun `streaks cross the end of a year`() {
        val days = listOf(day("2025-12-30"), day("2025-12-31"), day("2026-01-01"))
        assertEquals(3, StreakCalculator.calculate(days, day("2026-01-01")).currentStreakDays)
    }

    @Test
    fun `february 29 in a leap year is an ordinary day`() {
        val days = listOf(day("2028-02-28"), day("2028-02-29"), day("2028-03-01"))
        assertEquals(3, StreakCalculator.calculate(days, day("2028-03-01")).currentStreakDays)
    }

    @Test
    fun `the day daylight saving starts is still one day long`() {

        val days = listOf(day("2026-03-07"), day("2026-03-08"), day("2026-03-09"))
        assertEquals(3, StreakCalculator.calculate(days, day("2026-03-09")).currentStreakDays)
    }

    @Test
    fun `dates in any order give the same answer`() {
        val shuffled = listOf(today, day("2026-03-13"), day("2026-03-14"))
        assertEquals(3, StreakCalculator.calculate(shuffled, today).currentStreakDays)
    }

    @Test
    fun `the last workout date is the most recent one`() {
        val days = listOf(day("2026-03-01"), day("2026-03-14"), day("2026-03-07"))
        assertEquals(day("2026-03-14"), StreakCalculator.calculate(days, today).lastWorkoutDate)
    }

    @Test
    fun `a workout dated in the future does not break a live streak`() {
        val days = listOf(day("2026-03-14"), today, day("2026-03-20"))
        val result = StreakCalculator.calculate(days, today)
        assertEquals(2, result.currentStreakDays)
    }

    @Test
    fun `future dates do not count toward the longest streak`() {
        val days = listOf(today, day("2026-03-20"), day("2026-03-21"), day("2026-03-22"))
        assertEquals(1, StreakCalculator.calculate(days, today).longestStreakDays)
    }

    @Test
    fun `the last workout date ignores anything in the future`() {
        val days = listOf(day("2026-03-14"), day("2026-03-25"))
        assertEquals(day("2026-03-14"), StreakCalculator.calculate(days, today).lastWorkoutDate)
    }

    @Test
    fun `a history of only future dates reads as no training yet`() {
        val result = StreakCalculator.calculate(listOf(day("2026-04-01")), today)
        assertEquals(0, result.currentStreakDays)
        assertEquals(0, result.longestStreakDays)
        assertNull(result.lastWorkoutDate)
    }
}

class ConsistencyFutureDateTest {

    @Test
    fun `days later this week are not counted as done`() {
        val wednesday = day("2026-03-11")
        val days = listOf(day("2026-03-09"), wednesday, day("2026-03-13"))
        assertEquals(2, ConsistencyCalculator.workoutsThisWeek(days, wednesday))
    }
}

class ConsistencyCalculatorTest {

    @Test
    fun `iso weeks start on monday`() {

        assertEquals(day("2026-03-09"), ConsistencyCalculator.startOfWeek(day("2026-03-15")))
        assertEquals(day("2026-03-09"), ConsistencyCalculator.startOfWeek(day("2026-03-09")))
    }

    @Test
    fun `this week counts monday through sunday`() {
        val days = listOf(day("2026-03-09"), day("2026-03-11"), day("2026-03-15"))
        assertEquals(3, ConsistencyCalculator.workoutsThisWeek(days, day("2026-03-15")))
    }

    @Test
    fun `last sunday belongs to last week`() {
        val days = listOf(day("2026-03-08"), day("2026-03-09"))
        assertEquals(1, ConsistencyCalculator.workoutsThisWeek(days, day("2026-03-11")))
    }

    @Test
    fun `duplicate days within a week count once`() {
        val days = listOf(day("2026-03-11"), day("2026-03-11"))
        assertEquals(1, ConsistencyCalculator.workoutsThisWeek(days, day("2026-03-15")))
    }

    @Test
    fun `a trailing window includes today and excludes the day before it starts`() {
        val days = listOf(day("2026-03-09"), day("2026-03-15"), day("2026-02-01"))
        assertEquals(2, ConsistencyCalculator.daysTrainedInLast(days, day("2026-03-15"), days = 7))
    }
}
