package com.davidhuynh.levelup.domain.model

/**
 * Leaderboard and friend models. Phases 4 and 5 build the screens; the models and the
 * tables behind them exist from v1 so neither phase needs a schema migration.
 */

enum class LeaderboardMetric(val label: String) {
    TOTAL_VOLUME("Total volume"),
    CURRENT_STREAK("Current streak"),
    PR_COUNT("Personal records"),
}

enum class LeaderboardScope(val label: String) {
    GLOBAL("Global"),
    FRIENDS("Friends"),
}

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val avatarEmoji: String?,
    val totalVolumeKg: Double,
    val totalWorkouts: Int,
    val prCount: Int,
    val currentStreakDays: Int,
    val isCurrentUser: Boolean = false,
    val isDemo: Boolean = false,
)

data class Friend(
    val userId: String,
    val displayName: String,
    val avatarEmoji: String?,
    val friendsSince: Long,
)

enum class FriendRequestStatus { PENDING, ACCEPTED, DECLINED }

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromDisplayName: String,
    val toUserId: String,
    val status: FriendRequestStatus,
    val createdAt: Long,
    val respondedAt: Long? = null,
)
