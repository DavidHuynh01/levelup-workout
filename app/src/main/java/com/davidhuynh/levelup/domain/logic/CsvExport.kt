package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout

object CsvExport {

    private val HEADER = listOf(
        "date",
        "workout",
        "exercise",
        "muscle_group",
        "set_number",
        "reps",
        "weight",
        "unit",
        "is_warmup",
        "volume",
    )

    fun fileName(dateStamp: String): String = "levelup-workouts-$dateStamp.csv"

    fun toCsv(workouts: List<Workout>, unit: WeightUnit): String {
        val rows = StringBuilder()
        rows.appendLine(HEADER.joinToString(","))

        workouts.sortedBy { it.performedAt }.forEach { workout ->
            workout.exercises.forEach { exercise ->
                exercise.sets.forEach { set ->
                    val weight = WeightConverter.fromKg(set.weightKg, unit)
                    val volume = if (set.countsAsWorking) set.reps * weight else 0.0
                    rows.appendLine(
                        listOf(
                            workout.localDate.toString(),
                            escape(workout.name),
                            escape(exercise.exercise.name),
                            exercise.exercise.muscleGroup.label,
                            set.setNumber.toString(),
                            set.reps.toString(),
                            trim(weight),
                            unit.suffix,
                            if (set.isWarmup) "yes" else "no",
                            trim(volume),
                        ).joinToString(",")
                    )
                }
            }
        }
        return rows.toString()
    }

    private fun escape(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
