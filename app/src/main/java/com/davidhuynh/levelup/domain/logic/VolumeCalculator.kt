package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.ExerciseSet
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.model.WorkoutExercise

/**
 * Volume is reps x weight, summed over working sets.
 *
 * Bodyweight sets (0 kg) are legal and contribute no volume, but they still count as
 * sets performed and can still set rep-based records.
 */
object VolumeCalculator {

    fun setVolumeKg(set: ExerciseSet): Double =
        if (set.countsAsWorking) set.reps * set.weightKg else 0.0

    fun setsVolumeKg(sets: List<ExerciseSet>): Double =
        sets.sumOf { setVolumeKg(it) }

    fun exerciseVolumeKg(exercise: WorkoutExercise): Double =
        setsVolumeKg(exercise.sets)

    fun workoutVolumeKg(exercises: List<WorkoutExercise>): Double =
        exercises.sumOf { exerciseVolumeKg(it) }

    fun workoutVolumeKg(workout: Workout): Double =
        workoutVolumeKg(workout.exercises)

    fun workingSetCount(exercises: List<WorkoutExercise>): Int =
        exercises.sumOf { ex -> ex.sets.count { it.countsAsWorking } }

    fun totalRepCount(exercises: List<WorkoutExercise>): Int =
        exercises.sumOf { ex -> ex.sets.filter { it.countsAsWorking }.sumOf { it.reps } }
}
