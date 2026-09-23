package com.davidhuynh.levelup.domain.logic

object RestTimer {

    val PRESETS_SECONDS = listOf(60, 90, 120, 180)

    const val DEFAULT_SECONDS = 90

    fun remainingSeconds(startedAtMillis: Long, durationSeconds: Int, nowMillis: Long): Int {
        val elapsedMillis = nowMillis - startedAtMillis
        if (elapsedMillis <= 0) return durationSeconds
        val remaining = durationSeconds - (elapsedMillis / 1000L)
        return remaining.coerceAtLeast(0L).toInt()
    }

    fun isFinished(startedAtMillis: Long, durationSeconds: Int, nowMillis: Long): Boolean =
        remainingSeconds(startedAtMillis, durationSeconds, nowMillis) == 0

    fun format(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
    }

    fun extend(durationSeconds: Int, bySeconds: Int = 30): Int =
        (durationSeconds + bySeconds).coerceAtMost(MAX_SECONDS)

    private const val MAX_SECONDS = 60 * 30
}
