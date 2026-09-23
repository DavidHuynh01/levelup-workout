package com.davidhuynh.levelup.domain.model

import java.time.LocalDate

enum class PrType(val label: String, val shortLabel: String) {

    MAX_WEIGHT("Heaviest weight", "Weight"),

    MAX_ESTIMATED_1RM("Estimated 1RM", "1RM"),

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

    val supersededAt: Long? = null,
) {
    val isCurrent: Boolean get() = supersededAt == null
}

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
