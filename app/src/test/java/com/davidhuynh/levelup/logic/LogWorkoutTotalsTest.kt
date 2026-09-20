package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.ui.workout.log.ExerciseBlock
import com.davidhuynh.levelup.ui.workout.log.LogWorkoutUiState
import com.davidhuynh.levelup.ui.workout.log.SetRow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test for the running total on the log screen.
 *
 * The first version summed the typed numbers and handed them to the formatter as though
 * they were kilograms, so a pounds user saw a total about 2.2x too high: two sets of
 * 8 x 185 lb displayed as 6526 lb instead of 2960 lb.
 */
class LogWorkoutTotalsTest {

    private fun state(unit: WeightUnit, vararg sets: Pair<Int, String>) = LogWorkoutUiState(
        weightUnit = unit,
        blocks = listOf(
            ExerciseBlock(
                key = 1,
                exercise = Exercise(id = "ex", name = "Barbell Bench Press", muscleGroup = MuscleGroup.CHEST),
                sets = sets.mapIndexed { index, (reps, weight) ->
                    SetRow(key = index.toLong(), reps = reps.toString(), weight = weight)
                },
            )
        ),
    )

    @Test
    fun `pounds total is not inflated by a stray conversion`() {
        val state = state(WeightUnit.LB, 8 to "185", 8 to "185")
        assertEquals("2960 lb", state.totalVolumeDisplay)
        assertEquals(2, state.workingSetCount)
    }

    @Test
    fun `kilograms total matches the typed numbers exactly`() {
        val state = state(WeightUnit.KG, 5 to "100", 5 to "100")
        assertEquals("1000 kg", state.totalVolumeDisplay)
    }

    @Test
    fun `warmup sets are excluded from the running total`() {
        val state = LogWorkoutUiState(
            weightUnit = WeightUnit.KG,
            blocks = listOf(
                ExerciseBlock(
                    key = 1,
                    exercise = Exercise(id = "ex", name = "Back Squat", muscleGroup = MuscleGroup.LEGS),
                    sets = listOf(
                        SetRow(key = 1, reps = "10", weight = "60", isWarmup = true),
                        SetRow(key = 2, reps = "5", weight = "100"),
                    ),
                )
            ),
        )
        assertEquals("500 kg", state.totalVolumeDisplay)
        assertEquals(1, state.workingSetCount)
    }

    @Test
    fun `half typed numbers do not break the total`() {
        // Reps with no weight is a bodyweight set: it counts as a working set and adds no
        // volume. A trailing decimal point is mid-typing and parses as zero.
        val state = state(WeightUnit.LB, 8 to "", 0 to "185.")
        assertEquals("0 lb", state.totalVolumeDisplay)
        assertEquals(1, state.workingSetCount)
    }
}
