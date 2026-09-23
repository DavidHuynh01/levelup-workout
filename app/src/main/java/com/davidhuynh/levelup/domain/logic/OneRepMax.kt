package com.davidhuynh.levelup.domain.logic

object OneRepMax {

    const val MAX_REPS_FOR_ESTIMATE = 12

    fun epley(weightKg: Double, reps: Int): Double? {
        if (reps < 1 || reps > MAX_REPS_FOR_ESTIMATE) return null
        if (weightKg <= 0.0) return null
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }
}
