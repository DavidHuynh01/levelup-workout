package com.davidhuynh.levelup.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
