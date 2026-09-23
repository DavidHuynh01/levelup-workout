package com.davidhuynh.levelup.data.repository

import com.davidhuynh.levelup.data.local.dao.ExerciseDao
import com.davidhuynh.levelup.data.local.dao.ExerciseSetDao
import com.davidhuynh.levelup.data.local.dao.PersonalRecordDao
import com.davidhuynh.levelup.data.local.dao.UserStatsDao
import com.davidhuynh.levelup.data.local.dao.WorkoutDao
import com.davidhuynh.levelup.data.local.entity.PersonalRecordEntity
import com.davidhuynh.levelup.data.local.entity.UserStatsEntity
import com.davidhuynh.levelup.data.mapper.toLocalDate
import com.davidhuynh.levelup.data.mapper.toPrInput
import com.davidhuynh.levelup.domain.logic.ConsistencyCalculator
import com.davidhuynh.levelup.domain.logic.PrDetector
import com.davidhuynh.levelup.domain.logic.StreakCalculator
import com.davidhuynh.levelup.domain.model.PrAward
import com.davidhuynh.levelup.domain.model.PrType
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock

class DerivedDataRecomputer(
    private val workoutDao: WorkoutDao,
    private val setDao: ExerciseSetDao,
    private val recordDao: PersonalRecordDao,
    private val statsDao: UserStatsDao,
    private val exerciseDao: ExerciseDao,
    private val tokens: TokenGenerator,
    private val clock: AppClock,
) {

    suspend fun snapshotRecords(userId: String, exerciseIds: Collection<String>): Map<RecordKey, Double> {
        if (exerciseIds.isEmpty()) return emptyMap()
        return recordDao.currentRecordsForExercises(userId, exerciseIds.toList())
            .associate { RecordKey(it.exerciseId, it.recordType) to it.value }
    }

    suspend fun recomputeAfterMutation(
        userId: String,
        exerciseIds: Collection<String>,
        workoutId: String?,
    ) {
        exerciseIds.distinct().forEach { exerciseId ->
            rebuildRecordChain(userId, exerciseId)
        }
        workoutId?.let { refreshWorkoutTotals(it) }
        recomputeUserStats(userId)
    }

    suspend fun awardsFrom(
        userId: String,
        exerciseIds: Collection<String>,
        before: Map<RecordKey, Double>,
    ): List<PrAward> {
        if (exerciseIds.isEmpty()) return emptyList()
        val after = recordDao.currentRecordsForExercises(userId, exerciseIds.toList())
        val names = exerciseIds.distinct().associateWith { id ->
            exerciseDao.findById(id)?.name ?: "Exercise"
        }

        return after.mapNotNull { record ->
            val key = RecordKey(record.exerciseId, record.recordType)
            val previous = before[key]
            val improved = previous == null || record.value > previous + EPSILON
            if (!improved) return@mapNotNull null
            PrAward(
                exerciseId = record.exerciseId,
                exerciseName = names[record.exerciseId] ?: "Exercise",
                recordType = PrType.entries.firstOrNull { it.name == record.recordType }
                    ?: PrType.MAX_WEIGHT,
                newValue = record.value,
                previousValue = previous,
                reps = record.reps,
                weightKg = record.weightKg,
            )
        }.sortedBy { it.recordType.ordinal }
    }

    private suspend fun rebuildRecordChain(userId: String, exerciseId: String) {
        recordDao.deleteForExercise(userId, exerciseId)

        val sets = setDao.workingSetsForExercise(userId, exerciseId)
        if (sets.isEmpty()) return

        val history = PrDetector.buildHistory(sets.map { it.toPrInput() })
        if (history.isEmpty()) return

        recordDao.insertAll(
            history.map { draft ->
                PersonalRecordEntity(
                    id = tokens.newId(),
                    userId = userId,
                    exerciseId = exerciseId,
                    recordType = draft.recordType.name,
                    value = draft.value,
                    reps = draft.reps,
                    weightKg = draft.weightKg,
                    achievedAt = draft.achievedAt,
                    achievedOnLocalDate = draft.achievedOnLocalDate.toString(),
                    workoutId = draft.workoutId,
                    setId = draft.setId,
                    supersededAt = draft.supersededAt,
                )
            }
        )
    }

    private suspend fun refreshWorkoutTotals(workoutId: String) {

        workoutDao.getWorkoutRow(workoutId) ?: return
        val sets = setDao.setsForWorkout(workoutId)
        val working = sets.filter { !it.isWarmup && it.reps > 0 }
        workoutDao.updateCachedTotals(
            workoutId = workoutId,
            volumeKg = working.sumOf { it.reps * it.weightKg },
            setCount = sets.size,
            exerciseCount = sets.map { it.exerciseId }.distinct().size,
            updatedAt = clock.nowMillis(),
        )
    }

    suspend fun recomputeUserStats(userId: String) {
        val dates = workoutDao.workoutDates(userId).map { it.toLocalDate() }
        val today = clock.today()
        val streak = StreakCalculator.calculate(dates, today)

        statsDao.upsert(
            UserStatsEntity(
                userId = userId,
                totalVolumeKg = setDao.totalVolumeKg(userId),
                totalWorkouts = workoutDao.workoutCount(userId),
                totalSets = setDao.workingSetCount(userId),
                totalReps = setDao.totalReps(userId),
                prCount = recordDao.currentRecordCount(userId),
                currentStreakDays = streak.currentStreakDays,
                longestStreakDays = streak.longestStreakDays,
                workoutsThisWeek = ConsistencyCalculator.workoutsThisWeek(dates, today),
                lastWorkoutLocalDate = streak.lastWorkoutDate?.toString(),
                updatedAt = clock.nowMillis(),
            )
        )
    }

    suspend fun recomputeEverything(userId: String) {
        setDao.exerciseIdsForUser(userId).forEach { exerciseId ->
            rebuildRecordChain(userId, exerciseId)
        }
        workoutDao.workoutIdsForUser(userId).forEach { refreshWorkoutTotals(it) }
        recomputeUserStats(userId)
    }

    data class RecordKey(val exerciseId: String, val recordType: String)

    private companion object {
        const val EPSILON = 1e-6
    }
}
