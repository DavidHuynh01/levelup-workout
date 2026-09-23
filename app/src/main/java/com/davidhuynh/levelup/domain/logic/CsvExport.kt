package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout

/**
 * The whole training history as CSV, one row per set.
 *
 * One row per set rather than per workout because that is the shape every spreadsheet and
 * analysis tool wants: filter by exercise, pivot by date, sum a column. Rolling sets up
 * into a single cell would make the file unusable for the thing people export data for.
 */
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

        // Oldest first: a history reads forwards, and most tools plot in row order.
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

    /**
     * RFC 4180 quoting. Workout names are free text, so a name like `Push, heavy "top set"`
     * would otherwise split into extra columns and corrupt every row after it.
     */
    private fun escape(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    /** Two decimals at most, and no trailing ".0" to clutter the column. */
    private fun trim(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
