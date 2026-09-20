package com.davidhuynh.levelup.domain.logic

/**
 * Epley estimated one-rep max: weight x (1 + reps / 30).
 *
 * Only evaluated for 1 to 12 reps. Epley diverges badly above that, so letting a set of
 * 30 light reps mint a 1RM record is how this feature stops meaning anything.
 */
object OneRepMax {

    const val MAX_REPS_FOR_ESTIMATE = 12

    /** Returns null when the set is outside the range where the formula is trustworthy. */
    fun epley(weightKg: Double, reps: Int): Double? {
        if (reps < 1 || reps > MAX_REPS_FOR_ESTIMATE) return null
        if (weightKg <= 0.0) return null
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }
}
