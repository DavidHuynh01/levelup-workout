package com.davidhuynh.levelup.domain.repository

import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.Friend
import com.davidhuynh.levelup.domain.model.FriendRequest
import com.davidhuynh.levelup.domain.model.LeaderboardEntry
import com.davidhuynh.levelup.domain.model.LeaderboardMetric
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.PersonalRecord
import com.davidhuynh.levelup.domain.model.Session
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * Every interface here speaks only in domain models — no Room entities, no Cursor, no
 * Firebase types. That is the whole point: a Firebase implementation can be written later
 * and swapped in inside AppContainer without a single UI file changing.
 */

interface AuthRepository {
    val session: Flow<Session?>

    suspend fun signUp(
        email: String,
        displayName: String,
        password: String,
        confirmPassword: String,
    ): DataResult<User>

    suspend fun signIn(email: String, password: String): DataResult<User>

    suspend fun signOut()

    /** Extends the stored session on app open, so an active user is never logged out. */
    suspend fun refreshSession()

    fun observeUser(userId: String): Flow<User?>

    suspend fun getUser(userId: String): User?

    suspend fun updateWeightUnit(userId: String, unit: WeightUnit)

    suspend fun updateProfile(userId: String, displayName: String, avatarEmoji: String?): DataResult<User>
}

interface ExerciseRepository {
    fun observeCatalogue(userId: String): Flow<List<Exercise>>

    suspend fun search(userId: String, query: String): List<Exercise>

    suspend fun createCustom(
        userId: String,
        name: String,
        muscleGroup: MuscleGroup,
        equipment: String?,
    ): DataResult<Exercise>

    suspend fun getById(exerciseId: String): Exercise?
}

interface WorkoutRepository {
    fun observeHistory(userId: String): Flow<List<Workout>>

    fun observeWorkout(workoutId: String): Flow<Workout?>

    suspend fun getWorkout(workoutId: String): Workout?

    fun observeRecentWorkouts(userId: String, limit: Int): Flow<List<Workout>>

    /** Every training day, for the streak calculation. */
    fun observeWorkoutDates(userId: String): Flow<List<String>>
}

interface PersonalRecordRepository {
    /** Records that currently stand, newest first. */
    fun observeCurrentRecords(userId: String): Flow<List<PersonalRecord>>

    /** The full chain for one exercise, including beaten records. */
    fun observeRecordHistory(userId: String, exerciseId: String): Flow<List<PersonalRecord>>

    suspend fun currentRecords(userId: String): List<PersonalRecord>
}

interface StatsRepository {
    fun observeStats(userId: String): Flow<UserStats>

    suspend fun recomputeAll(userId: String)
}

/** Phase 4. The tables and queries exist from v1; only the screen is still missing. */
interface LeaderboardRepository {
    fun observeGlobal(currentUserId: String, metric: LeaderboardMetric, limit: Int): Flow<List<LeaderboardEntry>>

    fun observeFriends(currentUserId: String, metric: LeaderboardMetric): Flow<List<LeaderboardEntry>>
}

/** Phase 5. Same story: schema is ready, screen comes later. */
interface FriendRepository {
    fun observeFriends(userId: String): Flow<List<Friend>>

    fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>>

    fun observePendingRequestCount(userId: String): Flow<Int>

    suspend fun searchUsers(currentUserId: String, query: String): List<User>

    suspend fun sendRequest(fromUserId: String, toUserId: String): DataResult<Unit>

    suspend fun acceptRequest(requestId: String): DataResult<Unit>

    suspend fun declineRequest(requestId: String): DataResult<Unit>

    suspend fun removeFriend(userId: String, friendUserId: String)
}
