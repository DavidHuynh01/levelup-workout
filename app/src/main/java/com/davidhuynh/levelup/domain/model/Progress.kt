package com.davidhuynh.levelup.domain.model

import java.time.LocalDate

enum class PrType(val label: String, val shortLabel: String) {
    /** Heaviest working set ever performed on this exercise. */
    MAX_WEIGHT("Heaviest weight", "Weight"),

    /** Best Epley-estimated one-rep max, from sets of 1 to 12 reps. */
    MAX_ESTIMATED_1RM("Estimated 1RM", "1RM"),

    /** Most volume accumulated on this exercise within a single workout. */
    MAX_SESSION_VOLUME("Best session volume", "Volume"),
}

data class PersonalRecord(
    val id: String,
    val userId: String,
    val exerciseId: String,
    val exerciseName: String,
    val recordType: PrType,
    val value: Double,
    val reps: Int,
    val weightKg: Double,
    val achievedAt: Long,
    val achievedOnLocalDate: LocalDate,
    val workoutId: String?,
    val setId: String?,
    /** Null means this is the record that currently stands. */
    val supersededAt: Long? = null,
) {
    val isCurrent: Boolean get() = supersededAt == null
}

/** A record broken by the workout that was just saved, used for the celebration dialog. */
data class PrAward(
    val exerciseId: String,
    val exerciseName: String,
    val recordType: PrType,
    val newValue: Double,
    val previousValue: Double?,
    val reps: Int,
    val weightKg: Double,
)

data class UserStats(
    val userId: String,
    val totalVolumeKg: Double = 0.0,
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val prCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val workoutsThisWeek: Int = 0,
    val lastWorkoutLocalDate: LocalDate? = null,
    val updatedAt: Long = 0L,
)

data class StreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val lastWorkoutDate: LocalDate?,
)
