package com.davidhuynh.levelup.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Every "what time is it" read in the app goes through here.
 *
 * Streaks depend on today's date, so calling [LocalDate.now] inline would make the
 * streak tests unpinnable — they would pass or fail depending on the day they run.
 * Tests inject a fixed clock instead.
 */
interface AppClock {
    fun now(): Instant
    fun zone(): ZoneId

    fun today(): LocalDate = LocalDate.ofInstant(now(), zone())
    fun nowMillis(): Long = now().toEpochMilli()
}

class SystemAppClock : AppClock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
