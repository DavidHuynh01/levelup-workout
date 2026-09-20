package com.davidhuynh.levelup.domain.model

import java.time.LocalDate

enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    LEGS("Legs"),
    CORE("Core"),
    CARDIO("Cardio"),
    FULL_BODY("Full body"),
    OTHER("Other"),
}

/** An entry in the exercise catalogue, either built in or created by a user. */
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: String? = null,
    val isCustom: Boolean = false,
    val ownerUserId: String? = null,
)

data class ExerciseSet(
    val id: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val isWarmup: Boolean = false,
    val rpe: Double? = null,
    val completedAt: Long,
) {
    /** Warmups are logged for the record but never counted toward volume or PRs. */
    val countsAsWorking: Boolean get() = !isWarmup && reps > 0
}

data class WorkoutExercise(
    val id: String,
    val exercise: Exercise,
    val orderIndex: Int,
    val notes: String? = null,
    val sets: List<ExerciseSet> = emptyList(),
)

data class Workout(
    val id: String,
    val userId: String,
    val name: String,
    val performedAt: Long,
    val localDate: LocalDate,
    val zoneId: String,
    val notes: String? = null,
    val durationMinutes: Int? = null,
    val totalVolumeKg: Double = 0.0,
    val setCount: Int = 0,
    val exerciseCount: Int = 0,
    val exercises: List<WorkoutExercise> = emptyList(),
)
