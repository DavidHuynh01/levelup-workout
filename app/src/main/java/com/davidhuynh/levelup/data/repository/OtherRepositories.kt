package com.davidhuynh.levelup.data.repository

import com.davidhuynh.levelup.data.local.dao.ExerciseDao
import com.davidhuynh.levelup.data.local.dao.ExerciseSetDao
import com.davidhuynh.levelup.data.local.dao.PersonalRecordDao
import com.davidhuynh.levelup.data.local.dao.UserStatsDao
import com.davidhuynh.levelup.data.local.dao.WorkoutDao
import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.data.mapper.toDomain
import com.davidhuynh.levelup.data.mapper.toLocalDate
import com.davidhuynh.levelup.data.mapper.toPrInput
import com.davidhuynh.levelup.domain.logic.ConsistencyCalculator
import com.davidhuynh.levelup.domain.logic.ProgressPoint
import com.davidhuynh.levelup.domain.logic.ProgressSeries
import com.davidhuynh.levelup.domain.logic.StreakCalculator
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.PersonalRecord
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.repository.ExerciseRepository
import com.davidhuynh.levelup.domain.repository.PersonalRecordRepository
import com.davidhuynh.levelup.domain.repository.StatsRepository
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao,
    private val tokens: TokenGenerator,
    private val clock: AppClock,
) : ExerciseRepository {

    override fun observeCatalogue(userId: String): Flow<List<Exercise>> =
        exerciseDao.observeCatalogue(userId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun search(userId: String, query: String): List<Exercise> =
        exerciseDao.search(userId, query.trim()).map { it.toDomain() }

    override suspend fun createCustom(
        userId: String,
        name: String,
        muscleGroup: MuscleGroup,
        equipment: String?,
    ): DataResult<Exercise> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return DataResult.Failure("Enter an exercise name", "name")
        if (trimmed.length > 60) return DataResult.Failure("Keep the name under 60 characters", "name")

        exerciseDao.findByName(trimmed)?.let { existing ->

            return DataResult.Success(existing.toDomain())
        }

        val entity = ExerciseEntity(
            id = tokens.newId(),
            name = trimmed,
            muscleGroup = muscleGroup.name,
            equipment = equipment?.takeIf { it.isNotBlank() },
            isCustom = true,
            ownerUserId = userId,
            createdAt = clock.nowMillis(),
        )
        exerciseDao.insert(entity)
        return DataResult.Success(entity.toDomain())
    }

    override suspend fun getById(exerciseId: String): Exercise? =
        exerciseDao.findById(exerciseId)?.toDomain()
}

class PersonalRecordRepositoryImpl(
    private val recordDao: PersonalRecordDao,
    private val setDao: ExerciseSetDao,
) : PersonalRecordRepository {

    override fun observeProgress(userId: String, exerciseId: String): Flow<List<ProgressPoint>> =
        setDao.observeWorkingSetsForExercise(userId, exerciseId)
            .map { sets -> ProgressSeries.sessionBests(sets.map { it.toPrInput() }) }

    override fun observeCurrentRecords(userId: String): Flow<List<PersonalRecord>> =
        recordDao.observeCurrent(userId).map { rows -> rows.map { it.toDomain() } }

    override fun observeRecordHistory(userId: String, exerciseId: String): Flow<List<PersonalRecord>> =
        recordDao.observeHistoryForExercise(userId, exerciseId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun currentRecords(userId: String): List<PersonalRecord> =
        recordDao.currentRecords(userId).map { it.toDomain() }
}

class StatsRepositoryImpl(
    private val statsDao: UserStatsDao,
    private val workoutDao: WorkoutDao,
    private val recomputer: DerivedDataRecomputer,
    private val clock: AppClock,
) : StatsRepository {

    override fun observeStats(userId: String): Flow<UserStats> = combine(
        statsDao.observe(userId),
        workoutDao.observeWorkoutDates(userId),
    ) { cached, storedDates ->
        val base = cached?.toDomain() ?: UserStats(userId = userId)
        val dates = storedDates.map { it.toLocalDate() }
        val today = clock.today()
        val streak = StreakCalculator.calculate(dates, today)

        base.copy(
            currentStreakDays = streak.currentStreakDays,
            longestStreakDays = streak.longestStreakDays,
            workoutsThisWeek = ConsistencyCalculator.workoutsThisWeek(dates, today),
            lastWorkoutLocalDate = streak.lastWorkoutDate,
        )
    }

    override suspend fun recomputeAll(userId: String) {
        recomputer.recomputeEverything(userId)
    }
}
