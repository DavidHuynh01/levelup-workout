package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.OneRepMax
import com.davidhuynh.levelup.domain.logic.VolumeCalculator
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.ExerciseSet
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeCalculatorTest {

    private fun set(
        reps: Int,
        weightKg: Double,
        isWarmup: Boolean = false,
        number: Int = 1,
    ) = ExerciseSet(
        id = "set-$number-$reps-$weightKg",
        setNumber = number,
        reps = reps,
        weightKg = weightKg,
        isWarmup = isWarmup,
        completedAt = 0L,
    )

    private fun exercise(sets: List<ExerciseSet>, name: String = "Back Squat") = WorkoutExercise(
        id = "we-$name",
        exercise = Exercise(id = "ex-$name", name = name, muscleGroup = MuscleGroup.LEGS),
        orderIndex = 0,
        sets = sets,
    )

    @Test
    fun `no sets is zero volume`() {
        assertEquals(0.0, VolumeCalculator.setsVolumeKg(emptyList()), 0.0001)
    }

    @Test
    fun `volume is reps times weight`() {
        assertEquals(800.0, VolumeCalculator.setVolumeKg(set(reps = 8, weightKg = 100.0)), 0.0001)
    }

    @Test
    fun `warmup sets do not count toward volume`() {
        val sets = listOf(
            set(reps = 10, weightKg = 40.0, isWarmup = true),
            set(reps = 5, weightKg = 100.0, number = 2),
        )
        assertEquals(500.0, VolumeCalculator.setsVolumeKg(sets), 0.0001)
    }

    @Test
    fun `zero reps contributes nothing even with weight loaded`() {
        assertEquals(0.0, VolumeCalculator.setVolumeKg(set(reps = 0, weightKg = 150.0)), 0.0001)
    }

    @Test
    fun `bodyweight sets are legal and contribute zero volume`() {
        val pullUps = set(reps = 12, weightKg = 0.0)
        assertEquals(0.0, VolumeCalculator.setVolumeKg(pullUps), 0.0001)
        assertEquals(1, VolumeCalculator.workingSetCount(listOf(exercise(listOf(pullUps)))))
    }

    @Test
    fun `workout volume sums across exercises`() {
        val exercises = listOf(
            exercise(listOf(set(reps = 5, weightKg = 100.0)), name = "Back Squat"),
            exercise(listOf(set(reps = 10, weightKg = 60.0)), name = "Barbell Row"),
        )
        assertEquals(1100.0, VolumeCalculator.workoutVolumeKg(exercises), 0.0001)
    }

    @Test
    fun `fractional plate weights keep their precision`() {
        assertEquals(206.25, VolumeCalculator.setVolumeKg(set(reps = 3, weightKg = 68.75)), 0.0001)
    }

    @Test
    fun `rep count ignores warmups`() {
        val exercises = listOf(
            exercise(
                listOf(
                    set(reps = 15, weightKg = 20.0, isWarmup = true),
                    set(reps = 8, weightKg = 80.0, number = 2),
                    set(reps = 6, weightKg = 80.0, number = 3),
                )
            )
        )
        assertEquals(14, VolumeCalculator.totalRepCount(exercises))
    }
}

class OneRepMaxTest {

    @Test
    fun `a single rep is its own one rep max`() {
        assertEquals(140.0, OneRepMax.epley(140.0, 1)!!, 0.0001)
    }

    @Test
    fun `epley adds a third of a percent per rep`() {
        // 100 x 6 -> 100 * (1 + 6/30) = 120
        assertEquals(120.0, OneRepMax.epley(100.0, 6)!!, 0.0001)
    }

    @Test
    fun `twelve reps is the last trustworthy estimate`() {
        assertEquals(140.0, OneRepMax.epley(100.0, 12)!!, 0.0001)
    }

    @Test
    fun `above twelve reps there is no estimate`() {
        assertNull(OneRepMax.epley(60.0, 13))
        assertNull(OneRepMax.epley(20.0, 30))
    }

    @Test
    fun `zero or negative reps has no estimate`() {
        assertNull(OneRepMax.epley(100.0, 0))
        assertNull(OneRepMax.epley(100.0, -3))
    }

    @Test
    fun `bodyweight sets have no weight to estimate from`() {
        assertNull(OneRepMax.epley(0.0, 10))
    }
}
