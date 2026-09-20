package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.StreakInfo
import java.time.LocalDate

/**
 * Streaks count calendar days in the user's own time zone, taken from the date stored on
 * each workout. Adjacency goes through [LocalDate.minusDays], so daylight saving, month
 * ends and leap years are java.time's problem rather than ours.
 */
object StreakCalculator {

    /**
     * @param workoutDates every distinct day the user trained, in any order.
     * @param today the current date in the user's zone, from AppClock.
     *
     * A streak stays alive if the last workout was today or yesterday. Without that
     * one-day grace the app would show "streak: 0" every morning before training, which
     * is the fastest way to make a streak feature feel punitive.
     */
    fun calculate(workoutDates: Collection<LocalDate>, today: LocalDate): StreakInfo {
        if (workoutDates.isEmpty()) {
            return StreakInfo(currentStreakDays = 0, longestStreakDays = 0, lastWorkoutDate = null)
        }

        val days = workoutDates.distinct().sortedDescending()
        val mostRecent = days.first()

        val current = if (mostRecent == today || mostRecent == today.minusDays(1)) {
            countRunFrom(days, mostRecent)
        } else {
            0
        }

        return StreakInfo(
            currentStreakDays = current,
            longestStreakDays = longestRun(days),
            lastWorkoutDate = mostRecent,
        )
    }

    /** Length of the unbroken run of days ending at [start], walking backwards. */
    private fun countRunFrom(descendingDays: List<LocalDate>, start: LocalDate): Int {
        var expected = start
        var run = 0
        for (day in descendingDays) {
            if (day > expected) continue
            if (day == expected) {
                run++
                expected = expected.minusDays(1)
            } else {
                break
            }
        }
        return run
    }

    private fun longestRun(descendingDays: List<LocalDate>): Int {
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in descendingDays) {
            run = if (previous != null && previous.minusDays(1) == day) run + 1 else 1
            if (run > longest) longest = run
            previous = day
        }
        return longest
    }
}
