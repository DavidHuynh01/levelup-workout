package com.davidhuynh.levelup.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidhuynh.levelup.data.local.LevelUpDatabase
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.local.entity.UserStatsEntity
import com.davidhuynh.levelup.data.repository.FriendRepositoryImpl
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.security.SecureTokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class FriendFlowTest {

    private lateinit var database: LevelUpDatabase
    private lateinit var friends: FriendRepositoryImpl

    private val clock = object : AppClock {
        override fun now(): Instant = Instant.parse("2026-03-15T18:00:00Z")
        override fun zone(): ZoneId = ZoneId.of("America/Chicago")
    }

    private val davidId = "user-david"
    private val jaylinId = "user-jaylin"

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LevelUpDatabase::class.java,
        ).build()

        friends = FriendRepositoryImpl(
            friendDao = database.friendDao(),
            userDao = database.userDao(),
            tokens = SecureTokenGenerator(),
            clock = clock,
        )

        database.userDao().insert(user(davidId, "David", "david@test.com"))
        database.userDao().insert(user(jaylinId, "Jaylin", "jaylin@test.com"))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun acceptingARequestMakesBothPeopleFriends() = runBlocking {
        assertTrue(friends.sendRequest(davidId, jaylinId) is DataResult.Success)

        val incoming = friends.observeIncomingRequests(jaylinId).first()
        assertEquals(1, incoming.size)
        assertEquals("David", incoming.single().fromDisplayName)

        friends.acceptRequest(incoming.single().id)

        assertEquals(1, friends.observeFriends(davidId).first().size)
        assertEquals(1, friends.observeFriends(jaylinId).first().size)
        assertEquals(0, friends.observePendingRequestCount(jaylinId).first())
    }

    @Test
    fun decliningLeavesNobodyAsAFriendAndClearsTheBadge() = runBlocking {
        friends.sendRequest(davidId, jaylinId)
        val request = friends.observeIncomingRequests(jaylinId).first().single()

        friends.declineRequest(request.id)

        assertTrue(friends.observeFriends(jaylinId).first().isEmpty())
        assertEquals(0, friends.observePendingRequestCount(jaylinId).first())
    }

    @Test
    fun theSameRequestCannotBeSentTwice() = runBlocking {
        friends.sendRequest(davidId, jaylinId)
        val second = friends.sendRequest(davidId, jaylinId)

        assertTrue(second is DataResult.Failure)
        assertEquals(1, friends.observePendingRequestCount(jaylinId).first())
    }

    @Test
    fun addingSomeoneWhoAlreadyAskedYouAcceptsTheirRequest() = runBlocking {
        friends.sendRequest(jaylinId, davidId)

        val result = friends.sendRequest(davidId, jaylinId)

        assertTrue(result is DataResult.Success)
        assertEquals(1, friends.observeFriends(davidId).first().size)
        assertEquals(0, friends.observePendingRequestCount(davidId).first())
    }

    @Test
    fun anExistingFriendCannotBeAddedAgain() = runBlocking {
        friends.sendRequest(davidId, jaylinId)
        friends.acceptRequest(friends.observeIncomingRequests(jaylinId).first().single().id)

        assertTrue(friends.sendRequest(davidId, jaylinId) is DataResult.Failure)
    }

    @Test
    fun youCannotAddYourself() = runBlocking {
        assertTrue(friends.sendRequest(davidId, davidId) is DataResult.Failure)
    }

    @Test
    fun removingAFriendClearsBothDirections() = runBlocking {
        friends.sendRequest(davidId, jaylinId)
        friends.acceptRequest(friends.observeIncomingRequests(jaylinId).first().single().id)

        friends.removeFriend(davidId, jaylinId)

        assertTrue(friends.observeFriends(davidId).first().isEmpty())
        assertTrue(friends.observeFriends(jaylinId).first().isEmpty())
    }

    @Test
    fun theRecordsBoardIsOrderedByRecordsBeforeTheLimitApplies() = runBlocking {
        database.userDao().insert(user("user-heavy", "Heavy", "heavy@test.com"))
        statsFor(davidId, volumeKg = 1_000.0, prCount = 50)
        statsFor("user-heavy", volumeKg = 900_000.0, prCount = 1)
        statsFor(jaylinId, volumeKg = 500_000.0, prCount = 2)

        val topTwo = database.userStatsDao().observeGlobalByPrCount(limit = 2).first()

        assertTrue("the record holder must be on the board", topTwo.any { it.userId == davidId })
        assertEquals(davidId, topTwo.first().userId)
    }

    @Test
    fun searchExcludesYourselfAndMatchesNameOrEmail() = runBlocking {
        assertEquals(listOf("Jaylin"), friends.searchUsers(davidId, "").map { it.displayName })
        assertEquals(1, friends.searchUsers(davidId, "jay").size)
        assertEquals(1, friends.searchUsers(davidId, "jaylin@test").size)
        assertTrue(friends.searchUsers(davidId, "david").isEmpty())
    }

    private suspend fun statsFor(userId: String, volumeKg: Double, prCount: Int) {
        database.userStatsDao().upsert(
            UserStatsEntity(
                userId = userId,
                totalVolumeKg = volumeKg,
                totalWorkouts = 1,
                totalSets = 1,
                totalReps = 1,
                prCount = prCount,
                currentStreakDays = 0,
                longestStreakDays = 0,
                workoutsThisWeek = 0,
                lastWorkoutLocalDate = null,
                updatedAt = 0,
            )
        )
    }

    private fun user(id: String, name: String, email: String) = UserEntity(
        id = id,
        email = email,
        displayName = name,
        passwordHash = "hash",
        passwordSalt = "salt",
        passwordIterations = 1,
        avatarEmoji = null,
        weightUnit = WeightUnit.LB.name,
        createdAt = 0,
        isDemo = false,
    )
}
