package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.LeaderboardRanker
import com.davidhuynh.levelup.domain.model.LeaderboardMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardRankerTest {

    private fun row(
        id: String,
        name: String = id,
        volume: Double = 0.0,
        streak: Int = 0,
        prs: Int = 0,
        isDemo: Boolean = false,
    ) = LeaderboardRanker.Row(
        userId = id,
        displayName = name,
        avatarEmoji = null,
        isDemo = isDemo,
        totalVolumeKg = volume,
        totalWorkouts = 0,
        prCount = prs,
        currentStreakDays = streak,
    )

    @Test
    fun `an empty board ranks to an empty list`() {
        assertTrue(LeaderboardRanker.rank(emptyList(), LeaderboardMetric.TOTAL_VOLUME, "me").isEmpty())
    }

    @Test
    fun `volume ranks highest first`() {
        val ranked = LeaderboardRanker.rank(
            listOf(row("a", volume = 100.0), row("b", volume = 300.0), row("c", volume = 200.0)),
            LeaderboardMetric.TOTAL_VOLUME,
            currentUserId = "c",
        )
        assertEquals(listOf("b", "c", "a"), ranked.map { it.userId })
        assertEquals(listOf(1, 2, 3), ranked.map { it.rank })
    }

    @Test
    fun `switching the metric reorders the board`() {
        val rows = listOf(
            row("heavy", volume = 9_000.0, streak = 1, prs = 2),
            row("consistent", volume = 1_000.0, streak = 30, prs = 1),
            row("collector", volume = 2_000.0, streak = 2, prs = 40),
        )

        assertEquals("heavy", LeaderboardRanker.rank(rows, LeaderboardMetric.TOTAL_VOLUME, "x").first().userId)
        assertEquals("consistent", LeaderboardRanker.rank(rows, LeaderboardMetric.CURRENT_STREAK, "x").first().userId)
        assertEquals("collector", LeaderboardRanker.rank(rows, LeaderboardMetric.PR_COUNT, "x").first().userId)
    }

    @Test
    fun `equal scores share a rank`() {
        val ranked = LeaderboardRanker.rank(
            listOf(
                row("a", prs = 12),
                row("b", prs = 12),
                row("c", prs = 5),
            ),
            LeaderboardMetric.PR_COUNT,
            currentUserId = "x",
        )
        assertEquals(listOf(1, 1, 3), ranked.map { it.rank })
    }

    @Test
    fun `a brand new board of zeroes still ranks without crashing`() {
        val ranked = LeaderboardRanker.rank(
            listOf(row("a"), row("b")),
            LeaderboardMetric.TOTAL_VOLUME,
            currentUserId = "a",
        )
        assertEquals(listOf(1, 1), ranked.map { it.rank })
    }

    @Test
    fun `ties break on name so the order does not shuffle between refreshes`() {
        val first = LeaderboardRanker.rank(
            listOf(row("2", name = "Zoe", volume = 5.0), row("1", name = "Adam", volume = 5.0)),
            LeaderboardMetric.TOTAL_VOLUME,
            currentUserId = "x",
        )
        val second = LeaderboardRanker.rank(
            listOf(row("1", name = "Adam", volume = 5.0), row("2", name = "Zoe", volume = 5.0)),
            LeaderboardMetric.TOTAL_VOLUME,
            currentUserId = "x",
        )
        assertEquals(first.map { it.userId }, second.map { it.userId })
        assertEquals("Adam", first.first().displayName)
    }

    @Test
    fun `the current user is flagged wherever they land`() {
        val ranked = LeaderboardRanker.rank(
            listOf(row("a", volume = 10.0), row("me", volume = 1.0)),
            LeaderboardMetric.TOTAL_VOLUME,
            currentUserId = "me",
        )
        assertTrue(ranked.single { it.userId == "me" }.isCurrentUser)
        assertFalse(ranked.single { it.userId == "a" }.isCurrentUser)
        assertEquals(2, ranked.single { it.userId == "me" }.rank)
    }

    @Test
    fun `demo lifters stay flagged so the UI can label them`() {
        val ranked = LeaderboardRanker.rank(
            listOf(row("demo", volume = 10.0, isDemo = true), row("me", volume = 1.0)),
            LeaderboardMetric.TOTAL_VOLUME,
            currentUserId = "me",
        )
        assertTrue(ranked.first().isDemo)
        assertFalse(ranked.last().isDemo)
    }
}
