package com.davidhuynh.levelup.fake

import com.davidhuynh.levelup.domain.logic.PrInputSet
import com.davidhuynh.levelup.domain.util.AppClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A clock pinned to a fixed date, so streak assertions mean the same thing every day. */
class FakeClock(
    var instant: Instant = Instant.parse("2026-03-15T12:00:00Z"),
    private val zone: ZoneId = ZoneId.of("America/Chicago"),
) : AppClock {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zone

    fun setToday(date: LocalDate) {
        instant = date.atTime(LocalTime.NOON).atZone(zone).toInstant()
    }
}

/** Builds a working set for record tests, with only the fields each test cares about. */
fun prSet(
    setId: String,
    reps: Int,
    weightKg: Double,
    day: String,
    workoutId: String = "workout-$day",
    isWarmup: Boolean = false,
    completedAtOffset: Long = 0,
): PrInputSet {
    val date = LocalDate.parse(day)
    val base = date.atTime(LocalTime.of(18, 0)).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    return PrInputSet(
        setId = setId,
        workoutId = workoutId,
        reps = reps,
        weightKg = weightKg,
        isWarmup = isWarmup,
        completedAt = base + completedAtOffset,
        localDate = date,
    )
}

fun day(value: String): LocalDate = LocalDate.parse(value)
