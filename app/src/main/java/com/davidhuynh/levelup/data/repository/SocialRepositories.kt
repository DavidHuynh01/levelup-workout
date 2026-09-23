package com.davidhuynh.levelup.data.repository

import com.davidhuynh.levelup.data.local.dao.FriendDao
import com.davidhuynh.levelup.data.local.dao.UserDao
import com.davidhuynh.levelup.data.local.dao.UserStatsDao
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.FriendshipEntity
import com.davidhuynh.levelup.data.local.relation.LeaderboardRow
import com.davidhuynh.levelup.data.mapper.toDomain
import com.davidhuynh.levelup.domain.logic.LeaderboardRanker
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

class LeaderboardRepositoryImpl(
    private val statsDao: UserStatsDao,
) : LeaderboardRepository {

    override fun observeGlobal(
        currentUserId: String,
        metric: LeaderboardMetric,
        limit: Int,
    ): Flow<List<LeaderboardEntry>> {

        val rows = when (metric) {
            LeaderboardMetric.TOTAL_VOLUME -> statsDao.observeGlobalByVolume(limit)
            LeaderboardMetric.CURRENT_STREAK -> statsDao.observeGlobalByStreak(limit)
            LeaderboardMetric.PR_COUNT -> statsDao.observeGlobalByPrCount(limit)
        }
        return rows.map { list -> list.rank(metric, currentUserId) }
    }

    override fun observeFriends(
        currentUserId: String,
        metric: LeaderboardMetric,
    ): Flow<List<LeaderboardEntry>> =
        statsDao.observeFriendsByVolume(currentUserId).map { list -> list.rank(metric, currentUserId) }

    private fun List<LeaderboardRow>.rank(
        metric: LeaderboardMetric,
        currentUserId: String,
    ): List<LeaderboardEntry> = LeaderboardRanker.rank(
        rows = map { row ->
            LeaderboardRanker.Row(
                userId = row.userId,
                displayName = row.displayName,
                avatarEmoji = row.avatarEmoji,
                isDemo = row.isDemo,
                totalVolumeKg = row.totalVolumeKg,
                totalWorkouts = row.totalWorkouts,
                prCount = row.prCount,
                currentStreakDays = row.currentStreakDays,
            )
        },
        metric = metric,
        currentUserId = currentUserId,
    )
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

        friendDao.pendingRequestBetween(fromUserId, toUserId)?.let { existing ->
            return if (existing.fromUserId == fromUserId) {
                DataResult.Failure("Request already sent")
            } else {

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
