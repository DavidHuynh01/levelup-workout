package com.davidhuynh.levelup.domain.usecase

import com.davidhuynh.levelup.data.repository.WorkoutDraft
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.model.PrAward
import com.davidhuynh.levelup.domain.util.DataResult

class SaveWorkoutUseCase(private val repository: WorkoutRepositoryImpl) {

    suspend operator fun invoke(draft: WorkoutDraft): DataResult<Result> {
        if (draft.exercises.isEmpty()) {
            return DataResult.Failure("Add at least one exercise")
        }

        val cleaned = draft.exercises.mapNotNull { exercise ->
            val sets = exercise.sets.filter { it.reps > 0 }
            if (sets.isEmpty()) null else exercise.copy(sets = sets)
        }
        if (cleaned.isEmpty()) {
            return DataResult.Failure("Add at least one set with reps")
        }
        if (cleaned.any { ex -> ex.sets.any { it.reps < 0 || it.weightKg < 0 } }) {
            return DataResult.Failure("Reps and weight cannot be negative")
        }
        if (cleaned.any { ex -> ex.sets.any { it.reps > MAX_REPS } }) {
            return DataResult.Failure("That is a lot of reps — check the number")
        }
        if (cleaned.any { ex -> ex.sets.any { it.weightKg > MAX_WEIGHT_KG } }) {
            return DataResult.Failure("That weight looks wrong — check the number")
        }
        if (cleaned.none { ex -> ex.sets.any { !it.isWarmup } }) {
            return DataResult.Failure("Add at least one set that is not a warmup")
        }

        val outcome = repository.save(draft.copy(exercises = cleaned))
        return DataResult.Success(Result(outcome.workoutId, outcome.awards))
    }

    data class Result(val workoutId: String, val awards: List<PrAward>)

    private companion object {
        const val MAX_REPS = 500
        const val MAX_WEIGHT_KG = 600.0
    }
}

class DeleteWorkoutUseCase(private val repository: WorkoutRepositoryImpl) {
    suspend operator fun invoke(userId: String, workoutId: String) {
        repository.delete(userId, workoutId)
    }
}
