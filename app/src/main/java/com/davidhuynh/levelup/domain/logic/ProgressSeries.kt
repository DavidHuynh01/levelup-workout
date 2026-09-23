package com.davidhuynh.levelup.domain.logic

import java.time.LocalDate

data class ProgressPoint(
    val date: LocalDate,
    val estimated1rmKg: Double,
    val volumeKg: Double,
    val bestSetReps: Int,
    val bestSetWeightKg: Double,
)

object ProgressSeries {

    fun sessionBests(sets: List<PrInputSet>): List<ProgressPoint> {
        val working = sets.filter { !it.isWarmup && it.reps > 0 }
        if (working.isEmpty()) return emptyList()

        return working
            .groupBy { it.workoutId }
            .mapNotNull { (_, sessionSets) ->

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
