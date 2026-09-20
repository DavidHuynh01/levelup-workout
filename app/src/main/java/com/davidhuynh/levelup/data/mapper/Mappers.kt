package com.davidhuynh.levelup.data.mapper

import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.data.local.entity.ExerciseSetEntity
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.PersonalRecordEntity
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.local.entity.UserStatsEntity
import com.davidhuynh.levelup.data.local.relation.PersonalRecordWithExercise
import com.davidhuynh.levelup.data.local.relation.WorkoutWithDetails
import com.davidhuynh.levelup.domain.logic.PrInputSet
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.ExerciseSet
import com.davidhuynh.levelup.domain.model.FriendRequest
import com.davidhuynh.levelup.domain.model.FriendRequestStatus
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.PersonalRecord
import com.davidhuynh.levelup.domain.model.PrType
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.model.WorkoutExercise
import java.time.LocalDate

/**
 * Entities store enums by name and dates as ISO text, so all parsing lives here. Unknown
 * values fall back rather than throwing: a crash on read would make a bad row permanently
 * unopenable.
 */

fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

fun LocalDate.toStoredDate(): String = toString()

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    displayName = displayName,
    avatarEmoji = avatarEmoji,
    weightUnit = weightUnit.toWeightUnit(),
    createdAt = createdAt,
    isDemo = isDemo,
)

fun String.toWeightUnit(): WeightUnit =
    WeightUnit.entries.firstOrNull { it.name == this } ?: WeightUnit.LB

fun String.toMuscleGroup(): MuscleGroup =
    MuscleGroup.entries.firstOrNull { it.name == this } ?: MuscleGroup.OTHER

fun String.toPrType(): PrType =
    PrType.entries.firstOrNull { it.name == this } ?: PrType.MAX_WEIGHT

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup.toMuscleGroup(),
    equipment = equipment,
    isCustom = isCustom,
    ownerUserId = ownerUserId,
)

fun ExerciseSetEntity.toDomain(): ExerciseSet = ExerciseSet(
    id = id,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    isWarmup = isWarmup,
    rpe = rpe,
    completedAt = completedAt,
)

fun ExerciseSetEntity.toPrInput(): PrInputSet = PrInputSet(
    setId = id,
    workoutId = workoutId,
    reps = reps,
    weightKg = weightKg,
    isWarmup = isWarmup,
    completedAt = completedAt,
    localDate = localDate.toLocalDate(),
)

fun WorkoutWithDetails.toDomain(): Workout = Workout(
    id = workout.id,
    userId = workout.userId,
    name = workout.name,
    performedAt = workout.performedAt,
    localDate = workout.localDate.toLocalDate(),
    zoneId = workout.zoneId,
    notes = workout.notes,
    durationMinutes = workout.durationMinutes,
    totalVolumeKg = workout.totalVolumeKg,
    setCount = workout.setCount,
    exerciseCount = workout.exerciseCount,
    exercises = exercises
        .sortedBy { it.workoutExercise.orderIndex }
        .map { row ->
            WorkoutExercise(
                id = row.workoutExercise.id,
                exercise = row.exercise.toDomain(),
                orderIndex = row.workoutExercise.orderIndex,
                notes = row.workoutExercise.notes,
                sets = row.sets.sortedBy { it.setNumber }.map { it.toDomain() },
            )
        },
)

fun PersonalRecordWithExercise.toDomain(): PersonalRecord = PersonalRecord(
    id = record.id,
    userId = record.userId,
    exerciseId = record.exerciseId,
    exerciseName = exercise.name,
    recordType = record.recordType.toPrType(),
    value = record.value,
    reps = record.reps,
    weightKg = record.weightKg,
    achievedAt = record.achievedAt,
    achievedOnLocalDate = record.achievedOnLocalDate.toLocalDate(),
    workoutId = record.workoutId,
    setId = record.setId,
    supersededAt = record.supersededAt,
)

fun PersonalRecordEntity.toDomain(exerciseName: String): PersonalRecord = PersonalRecord(
    id = id,
    userId = userId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    recordType = recordType.toPrType(),
    value = value,
    reps = reps,
    weightKg = weightKg,
    achievedAt = achievedAt,
    achievedOnLocalDate = achievedOnLocalDate.toLocalDate(),
    workoutId = workoutId,
    setId = setId,
    supersededAt = supersededAt,
)

fun UserStatsEntity.toDomain(): UserStats = UserStats(
    userId = userId,
    totalVolumeKg = totalVolumeKg,
    totalWorkouts = totalWorkouts,
    totalSets = totalSets,
    totalReps = totalReps,
    prCount = prCount,
    currentStreakDays = currentStreakDays,
    longestStreakDays = longestStreakDays,
    workoutsThisWeek = workoutsThisWeek,
    lastWorkoutLocalDate = lastWorkoutLocalDate?.toLocalDate(),
    updatedAt = updatedAt,
)

fun FriendRequestEntity.toDomain(fromDisplayName: String): FriendRequest = FriendRequest(
    id = id,
    fromUserId = fromUserId,
    fromDisplayName = fromDisplayName,
    toUserId = toUserId,
    status = FriendRequestStatus.entries.firstOrNull { it.name == status }
        ?: FriendRequestStatus.PENDING,
    createdAt = createdAt,
    respondedAt = respondedAt,
)
