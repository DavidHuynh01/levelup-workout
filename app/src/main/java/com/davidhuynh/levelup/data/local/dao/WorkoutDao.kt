package com.davidhuynh.levelup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.data.local.entity.ExerciseSetEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutExerciseEntity
import com.davidhuynh.levelup.data.local.relation.WorkoutInstant
import com.davidhuynh.levelup.data.local.relation.WorkoutWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Query(
        """
        SELECT * FROM exercises
        WHERE isCustom = 0 OR ownerUserId = :userId
        ORDER BY name ASC
        """
    )
    fun observeCatalogue(userId: String): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (isCustom = 0 OR ownerUserId = :userId)
          AND name LIKE '%' || :query || '%'
        ORDER BY name ASC
        LIMIT 50
        """
    )
    suspend fun search(userId: String, query: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun findById(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}

@Dao
interface WorkoutDao {

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Upsert
    suspend fun upsertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: String)

    /** Children go with it via ON DELETE CASCADE, which Room enables by default. */
    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercises(workoutId: String)

    @Transaction
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY performedAt DESC")
    fun observeHistory(userId: String): Flow<List<WorkoutWithDetails>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY performedAt DESC LIMIT :limit")
    fun observeRecent(userId: String, limit: Int): Flow<List<WorkoutWithDetails>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    fun observeWorkout(workoutId: String): Flow<WorkoutWithDetails?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkout(workoutId: String): WorkoutWithDetails?

    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkoutRow(workoutId: String): WorkoutEntity?

    /** Source data for streaks: one entry per day the user trained. */
    @Query("SELECT DISTINCT localDate FROM workouts WHERE userId = :userId ORDER BY localDate DESC")
    fun observeWorkoutDates(userId: String): Flow<List<String>>

    @Query("SELECT DISTINCT localDate FROM workouts WHERE userId = :userId ORDER BY localDate DESC")
    suspend fun workoutDates(userId: String): List<String>

    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId")
    suspend fun workoutCount(userId: String): Int

    @Query("SELECT id FROM workouts WHERE userId = :userId")
    suspend fun workoutIdsForUser(userId: String): List<String>

    /** Used to keep two sessions on the same day from sharing an instant. */
    @Query("SELECT id, performedAt FROM workouts WHERE userId = :userId AND localDate = :localDate")
    suspend fun performedAtOn(userId: String, localDate: String): List<WorkoutInstant>

    @Query("UPDATE workouts SET totalVolumeKg = :volumeKg, setCount = :setCount, exerciseCount = :exerciseCount, updatedAt = :updatedAt WHERE id = :workoutId")
    suspend fun updateCachedTotals(
        workoutId: String,
        volumeKg: Double,
        setCount: Int,
        exerciseCount: Int,
        updatedAt: Long,
    )
}

@Dao
interface ExerciseSetDao {

    @Upsert
    suspend fun upsertAll(sets: List<ExerciseSetEntity>)

    @Query("DELETE FROM exercise_sets WHERE workoutId = :workoutId")
    suspend fun deleteForWorkout(workoutId: String)

    /** Total volume for one user: the leaderboard's headline number, in one indexed scan. */
    @Query(
        """
        SELECT COALESCE(SUM(reps * weightKg), 0.0) FROM exercise_sets
        WHERE userId = :userId AND isWarmup = 0 AND reps > 0
        """
    )
    suspend fun totalVolumeKg(userId: String): Double

    @Query(
        """
        SELECT COALESCE(SUM(reps * weightKg), 0.0) FROM exercise_sets
        WHERE workoutId = :workoutId AND isWarmup = 0 AND reps > 0
        """
    )
    suspend fun workoutVolumeKg(workoutId: String): Double

    @Query("SELECT COUNT(*) FROM exercise_sets WHERE userId = :userId AND isWarmup = 0 AND reps > 0")
    suspend fun workingSetCount(userId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(reps), 0) FROM exercise_sets
        WHERE userId = :userId AND isWarmup = 0 AND reps > 0
        """
    )
    suspend fun totalReps(userId: String): Int

    /** Feeds PrDetector, which rebuilds the whole record chain for this exercise. */
    @Query(
        """
        SELECT * FROM exercise_sets
        WHERE userId = :userId AND exerciseId = :exerciseId AND isWarmup = 0 AND reps > 0
        ORDER BY completedAt ASC
        """
    )
    suspend fun workingSetsForExercise(userId: String, exerciseId: String): List<ExerciseSetEntity>

    @Query("SELECT DISTINCT exerciseId FROM exercise_sets WHERE workoutId = :workoutId")
    suspend fun exerciseIdsInWorkout(workoutId: String): List<String>

    /** Every exercise this user has ever logged a set for, for a full records rebuild. */
    @Query("SELECT DISTINCT exerciseId FROM exercise_sets WHERE userId = :userId")
    suspend fun exerciseIdsForUser(userId: String): List<String>

    @Query("SELECT * FROM exercise_sets WHERE workoutId = :workoutId ORDER BY setNumber ASC")
    suspend fun setsForWorkout(workoutId: String): List<ExerciseSetEntity>
}
