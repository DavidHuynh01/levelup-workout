package com.davidhuynh.levelup.data.repository

import androidx.room.withTransaction
import com.davidhuynh.levelup.data.local.LevelUpDatabase
import com.davidhuynh.levelup.data.local.dao.ExerciseSetDao
import com.davidhuynh.levelup.data.local.dao.WorkoutDao
import com.davidhuynh.levelup.data.local.entity.ExerciseSetEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutExerciseEntity
import com.davidhuynh.levelup.data.mapper.toDomain
import com.davidhuynh.levelup.domain.model.PrAward
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.repository.WorkoutRepository
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the Log Workout screen hands over to be saved. Ids are absent for new rows and
 * present when an existing workout is being edited.
 */
data class WorkoutDraft(
    val workoutId: String? = null,
    val userId: String,
    val name: String,
    val performedAt: Long,
    val zoneId: String,
    val notes: String? = null,
    val durationMinutes: Int? = null,
    val exercises: List<ExerciseDraft>,
) {
    data class ExerciseDraft(
        val exerciseId: String,
        val notes: String? = null,
        val sets: List<SetDraft>,
    )

    data class SetDraft(
        val reps: Int,
        val weightKg: Double,
        val isWarmup: Boolean = false,
        val rpe: Double? = null,
    )
}

class WorkoutRepositoryImpl(
    private val database: LevelUpDatabase,
    private val workoutDao: WorkoutDao,
    private val setDao: ExerciseSetDao,
    private val recomputer: DerivedDataRecomputer,
    private val tokens: TokenGenerator,
    private val clock: AppClock,
) : WorkoutRepository {

    override fun observeHistory(userId: String): Flow<List<Workout>> =
        workoutDao.observeHistory(userId).map { rows -> rows.map { it.toDomain() } }

    override fun observeWorkout(workoutId: String): Flow<Workout?> =
        workoutDao.observeWorkout(workoutId).map { it?.toDomain() }

    override suspend fun getWorkout(workoutId: String): Workout? =
        workoutDao.getWorkout(workoutId)?.toDomain()

    override fun observeRecentWorkouts(userId: String, limit: Int): Flow<List<Workout>> =
        workoutDao.observeRecent(userId, limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeWorkoutDates(userId: String): Flow<List<String>> =
        workoutDao.observeWorkoutDates(userId)

    /**
     * Saves a new workout or replaces an existing one, then rebuilds everything derived
     * from it — all inside one transaction, so a crash halfway cannot leave records that
     * disagree with the sets they came from.
     *
     * Returns the records this save broke.
     */
    suspend fun save(draft: WorkoutDraft): SaveOutcome = database.withTransaction {
        val isEdit = draft.workoutId != null
        val workoutId = draft.workoutId ?: tokens.newId()
        val now = clock.nowMillis()

        // Exercises present before the edit still need their records rebuilt, even if this
        // save removes them — otherwise a withdrawn exercise keeps a record it no longer earns.
        val previousExerciseIds =
            if (isEdit) setDao.exerciseIdsInWorkout(workoutId) else emptyList()
        val newExerciseIds = draft.exercises.map { it.exerciseId }
        val affected = (previousExerciseIds + newExerciseIds).distinct()

        val before = recomputer.snapshotRecords(draft.userId, affected)

        val zone = runCatching { ZoneId.of(draft.zoneId) }.getOrElse { clock.zone() }
        val localDate = LocalDate.ofInstant(Instant.ofEpochMilli(draft.performedAt), zone)
        val existing = if (isEdit) workoutDao.getWorkoutRow(workoutId) else null

        workoutDao.upsertWorkout(
            WorkoutEntity(
                id = workoutId,
                userId = draft.userId,
                name = draft.name.ifBlank { defaultName(localDate) },
                performedAt = draft.performedAt,
                localDate = localDate.toString(),
                zoneId = zone.id,
                notes = draft.notes?.takeIf { it.isNotBlank() },
                durationMinutes = draft.durationMinutes,
                totalVolumeKg = 0.0,
                setCount = 0,
                exerciseCount = 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )

        // Children are rewritten wholesale: simpler than diffing, and CASCADE means the old
        // sets go with the old workout_exercises rows.
        if (isEdit) workoutDao.deleteWorkoutExercises(workoutId)

        val workoutExercises = mutableListOf<WorkoutExerciseEntity>()
        val sets = mutableListOf<ExerciseSetEntity>()

        draft.exercises.forEachIndexed { exerciseIndex, exerciseDraft ->
            val workoutExerciseId = tokens.newId()
            workoutExercises += WorkoutExerciseEntity(
                id = workoutExerciseId,
                workoutId = workoutId,
                exerciseId = exerciseDraft.exerciseId,
                orderIndex = exerciseIndex,
                notes = exerciseDraft.notes?.takeIf { it.isNotBlank() },
            )
            exerciseDraft.sets.forEachIndexed { setIndex, setDraft ->
                sets += ExerciseSetEntity(
                    id = tokens.newId(),
                    workoutExerciseId = workoutExerciseId,
                    workoutId = workoutId,
                    userId = draft.userId,
                    exerciseId = exerciseDraft.exerciseId,
                    setNumber = setIndex + 1,
                    reps = setDraft.reps,
                    weightKg = setDraft.weightKg,
                    isWarmup = setDraft.isWarmup,
                    rpe = setDraft.rpe,
                    // Sets share the workout's instant, offset so their order is stable.
                    completedAt = draft.performedAt + (exerciseIndex * 100L) + setIndex,
                    localDate = localDate.toString(),
                )
            }
        }

        workoutDao.upsertWorkoutExercises(workoutExercises)
        setDao.upsertAll(sets)

        recomputer.recomputeAfterMutation(draft.userId, affected, workoutId)
        val awards = recomputer.awardsFrom(draft.userId, newExerciseIds, before)

        SaveOutcome(workoutId = workoutId, awards = awards)
    }

    suspend fun delete(userId: String, workoutId: String) = database.withTransaction {
        // Captured before the delete: afterwards the sets are gone and so is the evidence
        // of which records need lowering.
        val affected = setDao.exerciseIdsInWorkout(workoutId)
        workoutDao.deleteWorkout(workoutId)
        recomputer.recomputeAfterMutation(userId, affected, workoutId = null)
    }

    private fun defaultName(date: LocalDate): String = "Workout on $date"

    data class SaveOutcome(val workoutId: String, val awards: List<PrAward>)
}
