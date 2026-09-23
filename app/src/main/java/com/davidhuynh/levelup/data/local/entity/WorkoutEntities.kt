package com.davidhuynh.levelup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["muscleGroup"]),
        Index(value = ["ownerUserId"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String?,
    val isCustom: Boolean,
    val ownerUserId: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "performedAt"]),
        Index(value = ["userId", "localDate"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val performedAt: Long,

    val localDate: String,
    val zoneId: String,
    val notes: String?,
    val durationMinutes: Int?,

    val totalVolumeKg: Double,
    val setCount: Int,
    val exerciseCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["workoutId", "orderIndex"]),
        Index(value = ["exerciseId"]),
    ],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val notes: String?,
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["workoutExerciseId"]),
        Index(value = ["userId", "exerciseId"]),
        Index(value = ["workoutId"]),
    ],
)
data class ExerciseSetEntity(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val workoutId: String,
    val userId: String,
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val isWarmup: Boolean,
    val rpe: Double?,
    val completedAt: Long,

    val localDate: String,
)

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "exerciseId", "recordType", "supersededAt"]),
        Index(value = ["userId", "achievedAt"]),
        Index(value = ["exerciseId"]),
    ],
)
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val exerciseId: String,
    val recordType: String,
    val value: Double,
    val reps: Int,
    val weightKg: Double,
    val achievedAt: Long,
    val achievedOnLocalDate: String,
    val workoutId: String?,
    val setId: String?,

    val supersededAt: Long?,
)
