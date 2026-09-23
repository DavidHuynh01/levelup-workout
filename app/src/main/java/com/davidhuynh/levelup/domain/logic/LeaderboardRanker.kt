package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.LeaderboardEntry
import com.davidhuynh.levelup.domain.model.LeaderboardMetric

object LeaderboardRanker {

    data class Row(
        val userId: String,
        val displayName: String,
        val avatarEmoji: String?,
        val isDemo: Boolean,
        val totalVolumeKg: Double,
        val totalWorkouts: Int,
        val prCount: Int,
        val currentStreakDays: Int,
    )

    fun rank(
        rows: List<Row>,
        metric: LeaderboardMetric,
        currentUserId: String,
    ): List<LeaderboardEntry> {
        val ordered = rows.sortedWith(
            compareByDescending<Row> { it.scoreFor(metric) }

                .thenBy { it.displayName.lowercase() }
                .thenBy { it.userId }
        )

        var lastScore: Double? = null
        var lastRank = 0

        return ordered.mapIndexed { index, row ->
            val score = row.scoreFor(metric)

            val rank = if (lastScore != null && score == lastScore) lastRank else index + 1
            lastScore = score
            lastRank = rank

            LeaderboardEntry(
                rank = rank,
                userId = row.userId,
                displayName = row.displayName,
                avatarEmoji = row.avatarEmoji,
                totalVolumeKg = row.totalVolumeKg,
                totalWorkouts = row.totalWorkouts,
                prCount = row.prCount,
                currentStreakDays = row.currentStreakDays,
                isCurrentUser = row.userId == currentUserId,
                isDemo = row.isDemo,
            )
        }
    }

    private fun Row.scoreFor(metric: LeaderboardMetric): Double = when (metric) {
        LeaderboardMetric.TOTAL_VOLUME -> totalVolumeKg
        LeaderboardMetric.CURRENT_STREAK -> currentStreakDays.toDouble()
        LeaderboardMetric.PR_COUNT -> prCount.toDouble()
    }
}
