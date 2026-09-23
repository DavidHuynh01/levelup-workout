package com.davidhuynh.levelup.domain.logic

import java.time.LocalDate
import java.time.temporal.WeekFields

object ConsistencyCalculator {

    private val ISO = WeekFields.ISO

    fun workoutsThisWeek(workoutDates: Collection<LocalDate>, today: LocalDate): Int {
        val start = startOfWeek(today)
        val end = start.plusDays(6)
        return workoutDates.distinct().count { it >= start && it <= end }
    }

    fun workoutsInWeekOf(workoutDates: Collection<LocalDate>, anyDayInWeek: LocalDate): Int =
        workoutsThisWeek(workoutDates, anyDayInWeek)

    fun startOfWeek(day: LocalDate): LocalDate =
        day.with(ISO.dayOfWeek(), 1L)

    fun daysTrainedInLast(workoutDates: Collection<LocalDate>, today: LocalDate, days: Int): Int {
        val earliest = today.minusDays((days - 1).toLong())
        return workoutDates.distinct().count { it >= earliest && it <= today }
    }
}
