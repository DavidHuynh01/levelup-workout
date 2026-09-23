package com.davidhuynh.levelup.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.data.local.entity.ExerciseSetEntity
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.PersonalRecordEntity
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutExerciseEntity

data class WorkoutWithDetails(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val exercises: List<WorkoutExerciseWithSets>,
)

data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<ExerciseSetEntity>,
)

data class FriendRequestWithUser(
    @Embedded val request: FriendRequestEntity,
    @Relation(parentColumn = "fromUserId", entityColumn = "id")
    val fromUser: UserEntity,
)

data class PersonalRecordWithExercise(
    @Embedded val record: PersonalRecordEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class WorkoutInstant(
    val id: String,
    val performedAt: Long,
)

data class LeaderboardRow(
    val userId: String,
    val displayName: String,
    val avatarEmoji: String?,
    val isDemo: Boolean,
    val totalVolumeKg: Double,
    val totalWorkouts: Int,
    val prCount: Int,
    val currentStreakDays: Int,
)
