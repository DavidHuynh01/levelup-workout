package com.davidhuynh.levelup.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidhuynh.levelup.data.local.LevelUpDatabase
import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.repository.DerivedDataRecomputer
import com.davidhuynh.levelup.data.repository.WorkoutDraft
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.model.PrType
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.security.SecureTokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The record and stats caches against a real database.
 *
 * The unit tests prove PrDetector rebuilds a chain correctly from a list of sets. These
 * prove the rest of the path: that a mutation really does trigger the rebuild, that
 * foreign keys cascade, and that deleting or back-dating a workout leaves the caches
 * agreeing with the sets that remain.
 */
@RunWith(AndroidJUnit4::class)
class RecomputeIntegrationTest {

    private lateinit var database: LevelUpDatabase
    private lateinit var repository: WorkoutRepositoryImpl
    private lateinit var recomputer: DerivedDataRecomputer

    private val zone: ZoneId = ZoneId.of("America/Chicago")
    private val clock = object : AppClock {
        override fun now(): Instant = Instant.parse("2026-03-15T18:00:00Z")
        override fun zone(): ZoneId = zone
    }

    private val userId = "user-1"
    private val benchId = "ex-bench"
    private val squatId = "ex-squat"

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LevelUpDatabase::class.java,
        ).build()

        val tokens = SecureTokenGenerator()
        recomputer = DerivedDataRecomputer(
            workoutDao = database.workoutDao(),
            setDao = database.exerciseSetDao(),
            recordDao = database.personalRecordDao(),
            statsDao = database.userStatsDao(),
            exerciseDao = database.exerciseDao(),
            tokens = tokens,
            clock = clock,
        )
        repository = WorkoutRepositoryImpl(
            database = database,
            workoutDao = database.workoutDao(),
            setDao = database.exerciseSetDao(),
            recomputer = recomputer,
            tokens = tokens,
            clock = clock,
        )

        database.userDao().insert(
            UserEntity(
                id = userId,
                email = "lifter@test.com",
                displayName = "Lifter",
                passwordHash = "hash",
                passwordSalt = "salt",
                passwordIterations = 1,
                avatarEmoji = null,
                weightUnit = WeightUnit.KG.name,
                createdAt = 0,
                isDemo = false,
            )
        )
        database.exerciseDao().insertAll(
            listOf(
                exercise(benchId, "Barbell Bench Press"),
                exercise(squatId, "Back Squat"),
            )
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun deletingTheRecordWorkoutLowersTheRecordToThePreviousBest() = runBlocking {
        save(day = "2026-03-01", exerciseId = benchId, reps = 5, weightKg = 100.0)
        val heavyId = save(day = "2026-03-08", exerciseId = benchId, reps = 5, weightKg = 120.0)

        assertEquals(120.0, currentRecord(PrType.MAX_WEIGHT)!!.value, 0.001)

        repository.delete(userId, heavyId)

        // The point of rebuilding rather than patching: this number has to come back down.
        assertEquals(100.0, currentRecord(PrType.MAX_WEIGHT)!!.value, 0.001)
        assertEquals(1, database.workoutDao().workoutCount(userId))
    }

    @Test
    fun deletingEveryWorkoutLeavesNoRecordsAndZeroedStats() = runBlocking {
        val id = save(day = "2026-03-01", exerciseId = benchId, reps = 5, weightKg = 100.0)
        repository.delete(userId, id)

        assertNull(currentRecord(PrType.MAX_WEIGHT))
        val stats = database.userStatsDao().get(userId)!!
        assertEquals(0.0, stats.totalVolumeKg, 0.001)
        assertEquals(0, stats.totalWorkouts)
        assertEquals(0, stats.prCount)
        assertEquals(0, stats.currentStreakDays)
    }

    @Test
    fun editingAWorkoutToDropAnExerciseWithdrawsItsRecords() = runBlocking {
        val workoutId = save(day = "2026-03-01", exerciseId = squatId, reps = 5, weightKg = 140.0)
        assertNotNull(currentRecord(PrType.MAX_WEIGHT, squatId))

        // Same workout, edited to be a bench session instead.
        repository.save(
            WorkoutDraft(
                workoutId = workoutId,
                userId = userId,
                name = "Swapped",
                performedAt = epochOf("2026-03-01"),
                zoneId = zone.id,
                exercises = listOf(
                    WorkoutDraft.ExerciseDraft(
                        exerciseId = benchId,
                        sets = listOf(WorkoutDraft.SetDraft(reps = 5, weightKg = 90.0)),
                    )
                ),
            )
        )

        assertNull("the squat record should be gone", currentRecord(PrType.MAX_WEIGHT, squatId))
        assertEquals(90.0, currentRecord(PrType.MAX_WEIGHT, benchId)!!.value, 0.001)
    }

    @Test
    fun aBackdatedHeavierWorkoutReordersTheRecordChain() = runBlocking {
        save(day = "2026-03-08", exerciseId = benchId, reps = 5, weightKg = 110.0)
        save(day = "2026-03-01", exerciseId = benchId, reps = 5, weightKg = 130.0)

        val chain = database.personalRecordDao()
            .currentRecords(userId)
            .filter { it.record.recordType == PrType.MAX_WEIGHT.name }

        // The back-dated 130 came first, so the later 110 never set a record at all.
        assertEquals(1, chain.size)
        assertEquals(130.0, chain.single().record.value, 0.001)
        assertEquals("2026-03-01", chain.single().record.achievedOnLocalDate)
    }

    @Test
    fun warmupSetsAreStoredButNeverCountedInVolumeOrRecords() = runBlocking {
        repository.save(
            WorkoutDraft(
                userId = userId,
                name = "Warmups",
                performedAt = epochOf("2026-03-01"),
                zoneId = zone.id,
                exercises = listOf(
                    WorkoutDraft.ExerciseDraft(
                        exerciseId = benchId,
                        sets = listOf(
                            WorkoutDraft.SetDraft(reps = 10, weightKg = 200.0, isWarmup = true),
                            WorkoutDraft.SetDraft(reps = 5, weightKg = 100.0),
                        ),
                    )
                ),
            )
        )

        assertEquals(500.0, database.exerciseSetDao().totalVolumeKg(userId), 0.001)
        assertEquals(100.0, currentRecord(PrType.MAX_WEIGHT)!!.value, 0.001)
        assertEquals(2, database.exerciseSetDao().setsForWorkout(workoutIds().first()).size)
    }

    @Test
    fun deletingAWorkoutCascadesToItsExercisesAndSets() = runBlocking {
        val id = save(day = "2026-03-01", exerciseId = benchId, reps = 5, weightKg = 100.0)
        assertTrue(database.exerciseSetDao().setsForWorkout(id).isNotEmpty())

        repository.delete(userId, id)

        // Room enables PRAGMA foreign_keys, so the children go with the parent. Asserted
        // rather than assumed: without it, orphan sets would keep inflating the totals.
        assertTrue(database.exerciseSetDao().setsForWorkout(id).isEmpty())
        assertEquals(0, database.exerciseSetDao().exerciseIdsForUser(userId).size)
    }

    @Test
    fun statsMatchTheSetsAfterASequenceOfEdits() = runBlocking {
        save(day = "2026-03-13", exerciseId = benchId, reps = 5, weightKg = 100.0)
        save(day = "2026-03-14", exerciseId = squatId, reps = 5, weightKg = 140.0)
        val last = save(day = "2026-03-15", exerciseId = benchId, reps = 3, weightKg = 120.0)

        var stats = database.userStatsDao().get(userId)!!
        assertEquals(500.0 + 700.0 + 360.0, stats.totalVolumeKg, 0.001)
        assertEquals(3, stats.totalWorkouts)
        assertEquals(3, stats.currentStreakDays)

        repository.delete(userId, last)

        stats = database.userStatsDao().get(userId)!!
        assertEquals(500.0 + 700.0, stats.totalVolumeKg, 0.001)
        assertEquals(2, stats.totalWorkouts)
        // 13th and 14th, with today being the 15th: yesterday still counts.
        assertEquals(2, stats.currentStreakDays)
        assertEquals(100.0, currentRecord(PrType.MAX_WEIGHT, benchId)!!.value, 0.001)
    }

    private suspend fun save(
        day: String,
        exerciseId: String,
        reps: Int,
        weightKg: Double,
    ): String = repository.save(
        WorkoutDraft(
            userId = userId,
            name = "Session $day",
            performedAt = epochOf(day),
            zoneId = zone.id,
            exercises = listOf(
                WorkoutDraft.ExerciseDraft(
                    exerciseId = exerciseId,
                    sets = listOf(WorkoutDraft.SetDraft(reps = reps, weightKg = weightKg)),
                )
            ),
        )
    ).workoutId

    private suspend fun currentRecord(type: PrType, exerciseId: String = benchId) =
        database.personalRecordDao()
            .currentRecords(userId)
            .map { it.record }
            .firstOrNull { it.recordType == type.name && it.exerciseId == exerciseId }

    private suspend fun workoutIds() = database.workoutDao().workoutIdsForUser(userId)

    private fun epochOf(day: String): Long =
        LocalDate.parse(day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun exercise(id: String, name: String) = ExerciseEntity(
        id = id,
        name = name,
        muscleGroup = "CHEST",
        equipment = "Barbell",
        isCustom = false,
        ownerUserId = null,
        createdAt = 0,
    )
}
