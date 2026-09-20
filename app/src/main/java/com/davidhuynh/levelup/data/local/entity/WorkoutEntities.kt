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
    /**
     * The calendar day this workout belongs to, in the zone it was logged in, captured at
     * write time as yyyy-MM-dd.
     *
     * Stored rather than derived on read: SQLite cannot do time zone arithmetic correctly,
     * and deriving it later would silently move a workout to a different day if the user
     * travels, breaking a streak they actually earned.
     */
    val localDate: String,
    val zoneId: String,
    val notes: String?,
    val durationMinutes: Int?,
    /** Cached aggregates, rebuilt on every write by DerivedDataRecomputer. */
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

/**
 * userId, workoutId and exerciseId are denormalised onto every set on purpose. The two
 * headline features are SQL aggregates — SUM(reps * weightKg) for volume and a scan by
 * exercise for records — and these columns let both run against one indexed table with no
 * joins at all.
 */
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
    /** Copied from the parent workout so record history needs no join to know the day. */
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
    /**
     * Null means this record currently stands. Beaten records are kept rather than
     * deleted, which gives the per-exercise record timeline for free.
     */
    val supersededAt: Long?,
)
