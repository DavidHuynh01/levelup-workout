package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.PrType
import java.time.LocalDate

/**
 * A working set, flattened to exactly what record detection needs. Deliberately not a
 * Room entity: this keeps [PrDetector] a pure function that unit tests can drive.
 */
data class PrInputSet(
    val setId: String,
    val workoutId: String,
    val reps: Int,
    val weightKg: Double,
    val isWarmup: Boolean,
    val completedAt: Long,
    val localDate: LocalDate,
)

/** One record in the chain, before it is given an id and written to the database. */
data class PrDraft(
    val recordType: PrType,
    val value: Double,
    val reps: Int,
    val weightKg: Double,
    val achievedAt: Long,
    val achievedOnLocalDate: LocalDate,
    val workoutId: String?,
    val setId: String?,
    /** Null on the record that currently stands. */
    val supersededAt: Long? = null,
)

/**
 * Rebuilds the complete record history for one exercise from every working set the user
 * has ever logged for it.
 *
 * This always rebuilds from scratch rather than comparing a new set against the stored
 * record. Incremental updating cannot handle the three cases that matter: deleting the
 * workout that held a record has to lower it again, removing an exercise from a workout
 * has to withdraw its records, and back-dating a workout reorders which achievement came
 * first. A full rebuild handles all three by construction.
 */
object PrDetector {

    /** Floating point slack. A record must be beaten, not matched. */
    private const val EPSILON = 1e-6

    fun buildHistory(sets: List<PrInputSet>): List<PrDraft> {
        val working = sets.filter { !it.isWarmup && it.reps > 0 }
        if (working.isEmpty()) return emptyList()

        val chronological = working.sortedWith(compareBy({ it.completedAt }, { it.setId }))

        val drafts = buildList {
            addAll(scanSets(chronological, PrType.MAX_WEIGHT) { it.weightKg })
            addAll(scanSets(chronological, PrType.MAX_ESTIMATED_1RM) { OneRepMax.epley(it.weightKg, it.reps) })
            addAll(scanSessionVolume(chronological))
        }

        return stampSupersededAt(drafts)
    }

    /** The records that stand right now, at most one per type. */
    fun currentRecords(history: List<PrDraft>): Map<PrType, PrDraft> =
        history.filter { it.supersededAt == null }.associateBy { it.recordType }

    private fun scanSets(
        chronological: List<PrInputSet>,
        type: PrType,
        metric: (PrInputSet) -> Double?,
    ): List<PrDraft> {
        var best = Double.NEGATIVE_INFINITY
        val records = mutableListOf<PrDraft>()
        for (set in chronological) {
            val value = metric(set) ?: continue
            if (value > best + EPSILON) {
                best = value
                records += PrDraft(
                    recordType = type,
                    value = value,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    achievedAt = set.completedAt,
                    achievedOnLocalDate = set.localDate,
                    workoutId = set.workoutId,
                    setId = set.setId,
                )
            }
        }
        return records
    }

    /**
     * Session volume is a property of a whole workout, so it is credited to the moment
     * that workout's last set was completed.
     */
    private fun scanSessionVolume(chronological: List<PrInputSet>): List<PrDraft> {
        val sessions = chronological
            .groupBy { it.workoutId }
            .map { (workoutId, sets) ->
                val last = sets.maxBy { it.completedAt }
                SessionTotal(
                    workoutId = workoutId,
                    volumeKg = sets.sumOf { it.reps * it.weightKg },
                    totalReps = sets.sumOf { it.reps },
                    completedAt = last.completedAt,
                    localDate = last.localDate,
                )
            }
            .sortedWith(compareBy({ it.completedAt }, { it.workoutId }))

        var best = Double.NEGATIVE_INFINITY
        val records = mutableListOf<PrDraft>()
        for (session in sessions) {
            if (session.volumeKg <= 0.0) continue
            if (session.volumeKg > best + EPSILON) {
                best = session.volumeKg
                records += PrDraft(
                    recordType = PrType.MAX_SESSION_VOLUME,
                    value = session.volumeKg,
                    reps = session.totalReps,
                    weightKg = 0.0,
                    achievedAt = session.completedAt,
                    achievedOnLocalDate = session.localDate,
                    workoutId = session.workoutId,
                    setId = null,
                )
            }
        }
        return records
    }

    /** Within each type, a record stands until the next one beats it. */
    private fun stampSupersededAt(drafts: List<PrDraft>): List<PrDraft> =
        drafts.groupBy { it.recordType }.values.flatMap { chain ->
            val ordered = chain.sortedBy { it.achievedAt }
            ordered.mapIndexed { index, draft ->
                val next = ordered.getOrNull(index + 1)
                draft.copy(supersededAt = next?.achievedAt)
            }
        }.sortedWith(compareBy({ it.recordType.ordinal }, { it.achievedAt }))

    private data class SessionTotal(
        val workoutId: String,
        val volumeKg: Double,
        val totalReps: Int,
        val completedAt: Long,
        val localDate: LocalDate,
    )
}
