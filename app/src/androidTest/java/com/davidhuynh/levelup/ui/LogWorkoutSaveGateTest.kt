package com.davidhuynh.levelup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.ui.workout.log.ExerciseBlock
import com.davidhuynh.levelup.ui.workout.log.LogWorkoutUiState
import com.davidhuynh.levelup.ui.workout.log.SetRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogWorkoutSaveGateTest {

    private val bench = Exercise(
        id = "ex-bench",
        name = "Barbell Bench Press",
        muscleGroup = MuscleGroup.CHEST,
    )

    private fun state(vararg sets: SetRow) = LogWorkoutUiState(
        isLoading = false,
        blocks = listOf(ExerciseBlock(key = 1, exercise = bench, sets = sets.toList())),
    )

    @Test
    fun anEmptyWorkoutCannotBeSaved() {
        val state = LogWorkoutUiState(isLoading = false)
        assertFalse(state.canSave)
        assertEquals(0, state.workingSetCount)
    }

    @Test
    fun anExerciseWithNoTypedRepsCannotBeSaved() {
        val state = state(SetRow(key = 1), SetRow(key = 2, weight = "185"))
        assertFalse(state.canSave)
        assertEquals(0, state.workingSetCount)
    }

    @Test
    fun warmupSetsAloneCannotBeSaved() {
        val state = state(
            SetRow(key = 1, reps = "10", weight = "45", isWarmup = true),
            SetRow(key = 2, reps = "8", weight = "95", isWarmup = true),
        )
        assertFalse(state.canSave)
        assertEquals(0, state.workingSetCount)
    }

    @Test
    fun oneWorkingSetIsEnoughToSave() {
        val state = state(
            SetRow(key = 1, reps = "10", weight = "45", isWarmup = true),
            SetRow(key = 2, reps = "5", weight = "185"),
        )
        assertTrue(state.canSave)
        assertEquals(1, state.workingSetCount)
    }

    @Test
    fun aWorkingSetWithoutWeightStillCounts() {
        val state = state(SetRow(key = 1, reps = "12"))
        assertTrue(state.canSave)
        assertEquals(1, state.workingSetCount)
    }

    @Test
    fun savingInFlightBlocksASecondSave() {
        val state = state(SetRow(key = 1, reps = "5", weight = "185")).copy(isSaving = true)
        assertFalse(state.canSave)
        assertEquals(1, state.workingSetCount)
    }

    @Test
    fun theExercisePickerFiltersOnTheSearchQuery() {
        val squat = Exercise(id = "ex-squat", name = "Back Squat", muscleGroup = MuscleGroup.LEGS)
        val state = LogWorkoutUiState(
            isLoading = false,
            catalogue = listOf(bench, squat),
        )

        assertEquals(2, state.filteredCatalogue.size)
        assertEquals(listOf(squat), state.copy(searchQuery = "squat").filteredCatalogue)
        assertEquals(listOf(bench), state.copy(searchQuery = "BENCH").filteredCatalogue)
        assertTrue(state.copy(searchQuery = "deadlift").filteredCatalogue.isEmpty())
    }
}
