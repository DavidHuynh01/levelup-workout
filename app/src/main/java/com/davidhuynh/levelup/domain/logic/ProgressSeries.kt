package com.davidhuynh.levelup.domain.logic

import java.time.LocalDate

/** One session's best effort on an exercise, for the progress chart. */
data class ProgressPoint(
    val date: LocalDate,
    val estimated1rmKg: Double,
    val volumeKg: Double,
    val bestSetReps: Int,
    val bestSetWeightKg: Double,
)

/**
 * Turns every working set on an exercise into one point per session.
 *
 * Plotted from sessions rather than from the record chain on purpose: records only ever
 * go up, so a chart of them is a staircase that hides the plateaus and bad weeks that are
 * the actual reason to look.
 */
object ProgressSeries {

    fun sessionBests(sets: List<PrInputSet>): List<ProgressPoint> {
        val working = sets.filter { !it.isWarmup && it.reps > 0 }
        if (working.isEmpty()) return emptyList()

        return working
            .groupBy { it.workoutId }
            .mapNotNull { (_, sessionSets) ->
                // The best set of the session is the one with the highest estimated 1RM;
                // sets too high in reps to estimate fall back to raw weight.
                val best = sessionSets.maxByOrNull { set ->
                    OneRepMax.epley(set.weightKg, set.reps) ?: set.weightKg
                } ?: return@mapNotNull null

                val estimate = OneRepMax.epley(best.weightKg, best.reps) ?: best.weightKg

                ProgressPoint(
                    date = sessionSets.maxBy { it.completedAt }.localDate,
                    estimated1rmKg = estimate,
                    volumeKg = sessionSets.sumOf { it.reps * it.weightKg },
                    bestSetReps = best.reps,
                    bestSetWeightKg = best.weightKg,
                )
            }
            .sortedBy { it.date }
    }
}
