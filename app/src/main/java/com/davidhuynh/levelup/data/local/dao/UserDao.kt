package com.davidhuynh.levelup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.local.entity.UserStatsEntity
import com.davidhuynh.levelup.data.local.relation.LeaderboardRow
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    /**
     * Sign-in looks the account up by email and then verifies the password in Kotlin.
     * Never "WHERE email = ? AND passwordHash = ?" — that would mean comparing derived
     * key material in SQL, which cannot be done in constant time.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun findById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeById(userId: String): Flow<UserEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun emailExists(email: String): Boolean

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    /**
     * Counted separately from real accounts: if a user signs up before seeding finishes,
     * a plain "any users?" check would skip the demo data forever.
     */
    @Query("SELECT COUNT(*) FROM users WHERE isDemo = 1")
    suspend fun demoCount(): Int

    /**
     * Demo accounts are searchable on purpose: on a fresh install they are the only other
     * lifters on the device, so excluding them would leave the friend flow with nobody to
     * add. They stay flagged, so the UI can label them.
     */
    @Query(
        """
        SELECT * FROM users
        WHERE id != :currentUserId
          AND (displayName LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%')
        ORDER BY isDemo ASC, displayName ASC
        LIMIT 30
        """
    )
    suspend fun search(currentUserId: String, query: String): List<UserEntity>
}

@Dao
interface UserStatsDao {

    @Upsert
    suspend fun upsert(stats: UserStatsEntity)

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    fun observe(userId: String): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): UserStatsEntity?

    /**
     * Phase 4's global leaderboard. Because user_stats already holds each user's totals,
     * ranking is a plain ORDER BY rather than an aggregate across every workout ever
     * logged. Rank is computed in Kotlin from the row order.
     */
    @Query(
        """
        SELECT u.id AS userId, u.displayName AS displayName, u.avatarEmoji AS avatarEmoji,
               u.isDemo AS isDemo, s.totalVolumeKg AS totalVolumeKg,
               s.totalWorkouts AS totalWorkouts, s.prCount AS prCount,
               s.currentStreakDays AS currentStreakDays
        FROM user_stats s
        JOIN users u ON u.id = s.userId
        ORDER BY s.totalVolumeKg DESC, u.displayName ASC
        LIMIT :limit
        """
    )
    fun observeGlobalByVolume(limit: Int): Flow<List<LeaderboardRow>>

    @Query(
        """
        SELECT u.id AS userId, u.displayName AS displayName, u.avatarEmoji AS avatarEmoji,
               u.isDemo AS isDemo, s.totalVolumeKg AS totalVolumeKg,
               s.totalWorkouts AS totalWorkouts, s.prCount AS prCount,
               s.currentStreakDays AS currentStreakDays
        FROM user_stats s
        JOIN users u ON u.id = s.userId
        ORDER BY s.currentStreakDays DESC, s.totalVolumeKg DESC
        LIMIT :limit
        """
    )
    fun observeGlobalByStreak(limit: Int): Flow<List<LeaderboardRow>>

    @Query(
        """
        SELECT u.id AS userId, u.displayName AS displayName, u.avatarEmoji AS avatarEmoji,
               u.isDemo AS isDemo, s.totalVolumeKg AS totalVolumeKg,
               s.totalWorkouts AS totalWorkouts, s.prCount AS prCount,
               s.currentStreakDays AS currentStreakDays
        FROM user_stats s
        JOIN users u ON u.id = s.userId
        WHERE s.userId = :userId
           OR s.userId IN (SELECT friendUserId FROM friendships WHERE userId = :userId)
        ORDER BY s.totalVolumeKg DESC, u.displayName ASC
        """
    )
    fun observeFriendsByVolume(userId: String): Flow<List<LeaderboardRow>>
}
