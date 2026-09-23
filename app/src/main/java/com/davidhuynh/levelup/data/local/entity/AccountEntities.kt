package com.davidhuynh.levelup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey val id: String,

    val email: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val passwordIterations: Int,
    val avatarEmoji: String?,
    val weightUnit: String,
    val createdAt: Long,
    val isDemo: Boolean,
)

@Entity(
    tableName = "user_stats",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["totalVolumeKg"]),
        Index(value = ["currentStreakDays"]),
        Index(value = ["prCount"]),
    ],
)
data class UserStatsEntity(
    @PrimaryKey val userId: String,
    val totalVolumeKg: Double,
    val totalWorkouts: Int,
    val totalSets: Int,
    val totalReps: Int,
    val prCount: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val workoutsThisWeek: Int,
    val lastWorkoutLocalDate: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "friendships",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["friendUserId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "friendUserId"], unique = true),
        Index(value = ["friendUserId"]),
    ],
)
data class FriendshipEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val friendUserId: String,
    val createdAt: Long,
)

@Entity(
    tableName = "friend_requests",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromUserId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["toUserId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fromUserId", "toUserId"], unique = true),
        Index(value = ["toUserId", "status"]),
    ],
)
data class FriendRequestEntity(
    @PrimaryKey val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long?,
)
