package com.davidhuynh.levelup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.FriendshipEntity
import com.davidhuynh.levelup.data.local.entity.PersonalRecordEntity
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.local.relation.FriendRequestWithUser
import com.davidhuynh.levelup.data.local.relation.PersonalRecordWithExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {

    @Insert
    suspend fun insertAll(records: List<PersonalRecordEntity>)

    @Query("DELETE FROM personal_records WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun deleteForExercise(userId: String, exerciseId: String)

    @Transaction
    @Query(
        """
        SELECT * FROM personal_records
        WHERE userId = :userId AND supersededAt IS NULL
        ORDER BY achievedAt DESC
        """
    )
    fun observeCurrent(userId: String): Flow<List<PersonalRecordWithExercise>>

    @Transaction
    @Query(
        """
        SELECT * FROM personal_records
        WHERE userId = :userId AND exerciseId = :exerciseId
        ORDER BY achievedAt DESC
        """
    )
    fun observeHistoryForExercise(userId: String, exerciseId: String): Flow<List<PersonalRecordWithExercise>>

    @Transaction
    @Query("SELECT * FROM personal_records WHERE userId = :userId AND supersededAt IS NULL")
    suspend fun currentRecords(userId: String): List<PersonalRecordWithExercise>

    @Query(
        """
        SELECT * FROM personal_records
        WHERE userId = :userId AND exerciseId IN (:exerciseIds) AND supersededAt IS NULL
        """
    )
    suspend fun currentRecordsForExercises(userId: String, exerciseIds: List<String>): List<PersonalRecordEntity>

    @Query("SELECT COUNT(*) FROM personal_records WHERE userId = :userId AND supersededAt IS NULL")
    suspend fun currentRecordCount(userId: String): Int
}

@Dao
interface FriendDao {

    @Insert
    suspend fun insertFriendships(friendships: List<FriendshipEntity>)

    @Insert
    suspend fun insertRequest(request: FriendRequestEntity)

    @Query("UPDATE friend_requests SET status = :status, respondedAt = :respondedAt WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: String, status: String, respondedAt: Long)

    @Query("SELECT * FROM friend_requests WHERE id = :requestId LIMIT 1")
    suspend fun getRequest(requestId: String): FriendRequestEntity?

    @Query(
        """
        SELECT * FROM friend_requests
        WHERE toUserId = :userId AND status = 'PENDING'
        ORDER BY createdAt DESC
        """
    )
    fun observeIncoming(userId: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT COUNT(*) FROM friend_requests WHERE toUserId = :userId AND status = 'PENDING'")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("SELECT * FROM friendships WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeFriendships(userId: String): Flow<List<FriendshipEntity>>

    @Query(
        """
        SELECT u.* FROM friendships f
        JOIN users u ON u.id = f.friendUserId
        WHERE f.userId = :userId
        ORDER BY u.displayName ASC
        """
    )
    fun observeFriendUsers(userId: String): Flow<List<UserEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM friend_requests
        WHERE toUserId = :userId AND status = 'PENDING'
        ORDER BY createdAt DESC
        """
    )
    fun observeIncomingWithSender(userId: String): Flow<List<FriendRequestWithUser>>

    @Query("SELECT EXISTS(SELECT 1 FROM friendships WHERE userId = :userId AND friendUserId = :otherId)")
    suspend fun areFriends(userId: String, otherId: String): Boolean

    @Query(
        """
        SELECT * FROM friend_requests
        WHERE ((fromUserId = :userId AND toUserId = :otherId)
            OR (fromUserId = :otherId AND toUserId = :userId))
          AND status = 'PENDING'
        LIMIT 1
        """
    )
    suspend fun pendingRequestBetween(userId: String, otherId: String): FriendRequestEntity?

    @Query("SELECT toUserId FROM friend_requests WHERE fromUserId = :userId AND status = 'PENDING'")
    fun observeOutgoingTargets(userId: String): Flow<List<String>>

    @Query("DELETE FROM friendships WHERE (userId = :userId AND friendUserId = :friendUserId) OR (userId = :friendUserId AND friendUserId = :userId)")
    suspend fun deleteFriendship(userId: String, friendUserId: String)
}
