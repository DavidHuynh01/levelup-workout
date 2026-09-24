package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.StreakInfo
import java.time.LocalDate

object StreakCalculator {

    fun calculate(workoutDates: Collection<LocalDate>, today: LocalDate): StreakInfo {
        val days = workoutDates.distinct().filter { it <= today }.sortedDescending()
        if (days.isEmpty()) {
            return StreakInfo(currentStreakDays = 0, longestStreakDays = 0, lastWorkoutDate = null)
        }

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
