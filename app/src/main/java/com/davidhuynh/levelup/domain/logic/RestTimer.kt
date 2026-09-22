package com.davidhuynh.levelup.domain.logic

/**
 * Rest between sets, computed from when it started rather than counted down in a variable.
 *
 * A ticking counter drifts and, worse, stops being true the moment the screen sleeps or the
 * app is backgrounded. Deriving the remaining time from the start instant means the timer is
 * right whenever it is next read, however long the app was away.
 */
object RestTimer {

    val PRESETS_SECONDS = listOf(60, 90, 120, 180)

    const val DEFAULT_SECONDS = 90

    /** Never negative: once it reaches zero it stays there until the timer is cleared. */
    fun remainingSeconds(startedAtMillis: Long, durationSeconds: Int, nowMillis: Long): Int {
        val elapsedMillis = nowMillis - startedAtMillis
        if (elapsedMillis <= 0) return durationSeconds
        val remaining = durationSeconds - (elapsedMillis / 1000L)
        return remaining.coerceAtLeast(0L).toInt()
    }

    fun isFinished(startedAtMillis: Long, durationSeconds: Int, nowMillis: Long): Boolean =
        remainingSeconds(startedAtMillis, durationSeconds, nowMillis) == 0

    /** m:ss, so 90 reads as 1:30 rather than "90s". */
    fun format(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
    }

    /** Extending a running timer keeps the original start, so elapsed rest still counts. */
    fun extend(durationSeconds: Int, bySeconds: Int = 30): Int =
        (durationSeconds + bySeconds).coerceAtMost(MAX_SECONDS)

    private const val MAX_SECONDS = 60 * 30
}
