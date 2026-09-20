package com.davidhuynh.levelup.data.repository

import com.davidhuynh.levelup.data.local.dao.FriendDao
import com.davidhuynh.levelup.data.local.dao.UserDao
import com.davidhuynh.levelup.data.local.dao.UserStatsDao
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.FriendshipEntity
import com.davidhuynh.levelup.data.local.relation.LeaderboardRow
import com.davidhuynh.levelup.data.mapper.toDomain
import com.davidhuynh.levelup.domain.model.Friend
import com.davidhuynh.levelup.domain.model.FriendRequest
import com.davidhuynh.levelup.domain.model.FriendRequestStatus
import com.davidhuynh.levelup.domain.model.LeaderboardEntry
import com.davidhuynh.levelup.domain.model.LeaderboardMetric
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.repository.FriendRepository
import com.davidhuynh.levelup.domain.repository.LeaderboardRepository
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Leaderboards read the denormalised user_stats table, so ranking is an ORDER BY over one
 * row per user rather than an aggregate over every set ever logged. That is also the shape
 * a Firestore leaderboard collection would take.
 */
class LeaderboardRepositoryImpl(
    private val statsDao: UserStatsDao,
    private val friendDao: FriendDao,
) : LeaderboardRepository {

    override fun observeGlobal(
        currentUserId: String,
        metric: LeaderboardMetric,
        limit: Int,
    ): Flow<List<LeaderboardEntry>> {
        val rows = when (metric) {
            LeaderboardMetric.CURRENT_STREAK -> statsDao.observeGlobalByStreak(limit)
            else -> statsDao.observeGlobalByVolume(limit)
        }
        return rows.map { list -> list.rank(metric, currentUserId) }
    }

    /**
     * The friends board is the same projection with one extra predicate, and it always
     * includes the current user so there is something to compare against.
     */
    override fun observeFriends(
        currentUserId: String,
        metric: LeaderboardMetric,
    ): Flow<List<LeaderboardEntry>> =
        statsDao.observeFriendsByVolume(currentUserId).map { list -> list.rank(metric, currentUserId) }

    /**
     * Rank is assigned here rather than in SQL. A correlated subquery would do it, but it
     * costs a scan per row for a number the list order already implies.
     */
    private fun List<LeaderboardRow>.rank(
        metric: LeaderboardMetric,
        currentUserId: String,
    ): List<LeaderboardEntry> = sortedByDescending {
        when (metric) {
            LeaderboardMetric.TOTAL_VOLUME -> it.totalVolumeKg
            LeaderboardMetric.CURRENT_STREAK -> it.currentStreakDays.toDouble()
            LeaderboardMetric.PR_COUNT -> it.prCount.toDouble()
        }
    }.mapIndexed { index, row ->
        LeaderboardEntry(
            rank = index + 1,
            userId = row.userId,
            displayName = row.displayName,
            avatarEmoji = row.avatarEmoji,
            totalVolumeKg = row.totalVolumeKg,
            totalWorkouts = row.totalWorkouts,
            prCount = row.prCount,
            currentStreakDays = row.currentStreakDays,
            isCurrentUser = row.userId == currentUserId,
            isDemo = row.isDemo,
        )
    }
}

class FriendRepositoryImpl(
    private val friendDao: FriendDao,
    private val userDao: UserDao,
    private val tokens: TokenGenerator,
    private val clock: AppClock,
) : FriendRepository {

    override fun observeFriends(userId: String): Flow<List<Friend>> =
        friendDao.observeFriendUsers(userId).map { users ->
            users.map { user ->
                Friend(
                    userId = user.id,
                    displayName = user.displayName,
                    avatarEmoji = user.avatarEmoji,
                    friendsSince = user.createdAt,
                )
            }
        }

    override fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> =
        friendDao.observeIncomingWithSender(userId).map { rows ->
            rows.map { it.request.toDomain(it.fromUser.displayName) }
        }

    override fun observePendingRequestCount(userId: String): Flow<Int> =
        friendDao.observePendingCount(userId)

    fun observeOutgoingTargets(userId: String): Flow<List<String>> =
        friendDao.observeOutgoingTargets(userId)

    override suspend fun searchUsers(currentUserId: String, query: String): List<User> =
        userDao.search(currentUserId, query.trim()).map { it.toDomain() }

    override suspend fun sendRequest(fromUserId: String, toUserId: String): DataResult<Unit> {
        if (fromUserId == toUserId) {
            return DataResult.Failure("You cannot add yourself")
        }
        if (friendDao.areFriends(fromUserId, toUserId)) {
            return DataResult.Failure("You are already friends")
        }
        // Checked in both directions, so two people adding each other at once cannot end
        // up with a pair of mirror-image pending requests.
        friendDao.pendingRequestBetween(fromUserId, toUserId)?.let { existing ->
            return if (existing.fromUserId == fromUserId) {
                DataResult.Failure("Request already sent")
            } else {
                // They asked first: accepting theirs is what the user actually means.
                acceptRequest(existing.id)
            }
        }

        friendDao.insertRequest(
            FriendRequestEntity(
                id = tokens.newId(),
                fromUserId = fromUserId,
                toUserId = toUserId,
                status = FriendRequestStatus.PENDING.name,
                createdAt = clock.nowMillis(),
                respondedAt = null,
            )
        )
        return DataResult.Success(Unit)
    }

    override suspend fun acceptRequest(requestId: String): DataResult<Unit> {
        val request = friendDao.getRequest(requestId)
            ?: return DataResult.Failure("That request is no longer here")
        val now = clock.nowMillis()

        friendDao.updateRequestStatus(requestId, FriendRequestStatus.ACCEPTED.name, now)
        // Two rows, one per direction: each side's friends list is then a single indexed
        // lookup with no OR across columns.
        friendDao.insertFriendships(
            listOf(
                FriendshipEntity(tokens.newId(), request.fromUserId, request.toUserId, now),
                FriendshipEntity(tokens.newId(), request.toUserId, request.fromUserId, now),
            )
        )
        return DataResult.Success(Unit)
    }

    override suspend fun declineRequest(requestId: String): DataResult<Unit> {
        friendDao.getRequest(requestId)
            ?: return DataResult.Failure("That request is no longer here")
        friendDao.updateRequestStatus(
            requestId,
            FriendRequestStatus.DECLINED.name,
            clock.nowMillis(),
        )
        return DataResult.Success(Unit)
    }

    override suspend fun removeFriend(userId: String, friendUserId: String) {
        friendDao.deleteFriendship(userId, friendUserId)
    }
}
