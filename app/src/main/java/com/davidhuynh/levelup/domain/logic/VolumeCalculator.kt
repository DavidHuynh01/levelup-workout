package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.ExerciseSet
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.model.WorkoutExercise

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
