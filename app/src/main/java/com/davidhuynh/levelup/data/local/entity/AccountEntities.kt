package com.davidhuynh.levelup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Columns are primitives and strings only — no type converters anywhere in this schema.
 * Enums are stored as their name, dates as ISO yyyy-MM-dd text, instants as epoch millis,
 * and every primary key is a client-generated UUID string.
 *
 * The UUID choice is what keeps a later Firebase migration mechanical: Firestore document
 * ids are strings, so the same ids move across unchanged.
 */

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey val id: String,
    /** Always stored trimmed and lowercased, so signup and login can never disagree. */
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

/**
 * One row per user: the denormalised leaderboard document.
 *
 * This is a cache with no authority — every field is recomputable from exercise_sets and
 * workouts. Keeping it means a leaderboard is an ORDER BY instead of an aggregate over
 * every user's full history, which is also exactly the shape a Firestore
 * leaderboard/{userId} document would take.
 */
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

/**
 * Two rows per friendship, one in each direction. That mirrors Firestore's
 * users/{id}/friends/{friendId} subcollection and turns the friends leaderboard into a
 * single indexed lookup with no OR across two columns.
 *
 * Created in v1 but unused until Phase 5, so that phase needs no migration.
 */
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
