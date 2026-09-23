package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.PrDetector
import com.davidhuynh.levelup.domain.model.PrType
import com.davidhuynh.levelup.fake.prSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrDetectorTest {

    @Test
    fun `no sets means no records`() {
        assertTrue(PrDetector.buildHistory(emptyList()).isEmpty())
    }

    @Test
    fun `the first working set sets every record type`() {
        val history = PrDetector.buildHistory(listOf(prSet("s1", reps = 5, weightKg = 100.0, day = "2026-03-01")))
        val current = PrDetector.currentRecords(history)

        assertEquals(100.0, current[PrType.MAX_WEIGHT]!!.value, 0.0001)
        assertEquals(116.666, current[PrType.MAX_ESTIMATED_1RM]!!.value, 0.01)
        assertEquals(500.0, current[PrType.MAX_SESSION_VOLUME]!!.value, 0.0001)
    }

    @Test
    fun `matching a record is not a record`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s1", reps = 5, weightKg = 100.0, day = "2026-03-01"),
                prSet("s2", reps = 5, weightKg = 100.0, day = "2026-03-08"),
            )
        )
        val weightRecords = history.filter { it.recordType == PrType.MAX_WEIGHT }
        assertEquals(1, weightRecords.size)
        assertEquals("s1", weightRecords.single().setId)
    }

    @Test
    fun `beating a record adds a link to the chain and supersedes the old one`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s1", reps = 5, weightKg = 100.0, day = "2026-03-01"),
                prSet("s2", reps = 5, weightKg = 105.0, day = "2026-03-08"),
            )
        )
        val weightChain = history.filter { it.recordType == PrType.MAX_WEIGHT }.sortedBy { it.achievedAt }

        assertEquals(2, weightChain.size)
        assertEquals("s1", weightChain[0].setId)
        assertEquals(weightChain[1].achievedAt, weightChain[0].supersededAt)
        assertNull(weightChain[1].supersededAt)
    }

    @Test
    fun `warmup sets never set records`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("warm", reps = 3, weightKg = 200.0, day = "2026-03-01", isWarmup = true),
                prSet("work", reps = 5, weightKg = 100.0, day = "2026-03-01"),
            )
        )
        assertEquals(100.0, PrDetector.currentRecords(history)[PrType.MAX_WEIGHT]!!.value, 0.0001)
    }

    @Test
    fun `more reps at the same weight is a one rep max record but not a weight record`() {
        val history = PrDetector.buildHistory(
            listOf
                (
                prSet("s1", reps = 8, weightKg = 100.0, day = "2026-03-01"),
                prSet("s2", reps = 10, weightKg = 100.0, day = "2026-03-08"),
            )
        )
        assertEquals(1, history.count { it.recordType == PrType.MAX_WEIGHT })
        assertEquals(2, history.count { it.recordType == PrType.MAX_ESTIMATED_1RM })
    }

    @Test
    fun `a heavy single beats a light high rep set on one rep max`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s1", reps = 12, weightKg = 80.0, day = "2026-03-01"),
                prSet("s2", reps = 1, weightKg = 130.0, day = "2026-03-08"),
            )
        )
        assertEquals(130.0, PrDetector.currentRecords(history)[PrType.MAX_ESTIMATED_1RM]!!.value, 0.0001)
    }

    @Test
    fun `sets above twelve reps are excluded from one rep max but still count for weight`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s1", reps = 20, weightKg = 90.0, day = "2026-03-01"),
            )
        )
        val current = PrDetector.currentRecords(history)
        assertEquals(90.0, current[PrType.MAX_WEIGHT]!!.value, 0.0001)
        assertNull(current[PrType.MAX_ESTIMATED_1RM])
    }

    @Test
    fun `session volume adds up every set in one workout`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s1", reps = 10, weightKg = 50.0, day = "2026-03-01", workoutId = "w1"),
                prSet("s2", reps = 10, weightKg = 50.0, day = "2026-03-01", workoutId = "w1", completedAtOffset = 60),
            )
        )
        assertEquals(1000.0, PrDetector.currentRecords(history)[PrType.MAX_SESSION_VOLUME]!!.value, 0.0001)
    }

    @Test
    fun `session volume records compare across workouts, not within one`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("a1", reps = 10, weightKg = 60.0, day = "2026-03-01", workoutId = "w1"),
                prSet("b1", reps = 10, weightKg = 50.0, day = "2026-03-08", workoutId = "w2"),
                prSet("b2", reps = 10, weightKg = 50.0, day = "2026-03-08", workoutId = "w2", completedAtOffset = 60),
            )
        )
        val chain = history.filter { it.recordType == PrType.MAX_SESSION_VOLUME }.sortedBy { it.achievedAt }
        assertEquals(2, chain.size)
        assertEquals(600.0, chain[0].value, 0.0001)
        assertEquals(1000.0, chain[1].value, 0.0001)
    }

    @Test
    fun `removing the best set rebuilds the chain at the previous best`() {
        val all = listOf(
            prSet("s1", reps = 5, weightKg = 100.0, day = "2026-03-01"),
            prSet("s2", reps = 5, weightKg = 120.0, day = "2026-03-08"),
        )
        assertEquals(120.0, PrDetector.currentRecords(PrDetector.buildHistory(all))[PrType.MAX_WEIGHT]!!.value, 0.0001)

        val afterDeletingTheBigOne = all.filterNot { it.setId == "s2" }
        val rebuilt = PrDetector.currentRecords(PrDetector.buildHistory(afterDeletingTheBigOne))
        assertEquals(100.0, rebuilt[PrType.MAX_WEIGHT]!!.value, 0.0001)
    }

    @Test
    fun `deleting every set for an exercise leaves no records at all`() {
        assertTrue(PrDetector.buildHistory(emptyList()).isEmpty())
    }

    @Test
    fun `a backdated heavier set takes over the start of the chain`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("late", reps = 5, weightKg = 110.0, day = "2026-03-08"),
                prSet("backdated", reps = 5, weightKg = 130.0, day = "2026-03-01"),
            )
        )
        val chain = history.filter { it.recordType == PrType.MAX_WEIGHT }.sortedBy { it.achievedAt }

        assertEquals(1, chain.size)
        assertEquals("backdated", chain.single().setId)
        assertEquals(130.0, chain.single().value, 0.0001)
        assertNull(chain.single().supersededAt)
    }

    @Test
    fun `sets arriving out of order are still read chronologically`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s3", reps = 5, weightKg = 120.0, day = "2026-03-15"),
                prSet("s1", reps = 5, weightKg = 100.0, day = "2026-03-01"),
                prSet("s2", reps = 5, weightKg = 110.0, day = "2026-03-08"),
            )
        )
        val chain = history.filter { it.recordType == PrType.MAX_WEIGHT }.sortedBy { it.achievedAt }
        assertEquals(listOf("s1", "s2", "s3"), chain.map { it.setId })
    }

    @Test
    fun `only one record per type stands at a time`() {
        val history = PrDetector.buildHistory(
            listOf(
                prSet("s1", reps = 5, weightKg = 100.0, day = "2026-03-01"),
                prSet("s2", reps = 5, weightKg = 110.0, day = "2026-03-08"),
                prSet("s3", reps = 5, weightKg = 120.0, day = "2026-03-15"),
            )
        )
        val standing = history.filter { it.supersededAt == null }
        assertEquals(standing.size, standing.map { it.recordType }.distinct().size)
    }

    @Test
    fun `records carry the day they were achieved on`() {
        val history = PrDetector.buildHistory(listOf(prSet("s1", reps = 3, weightKg = 140.0, day = "2026-02-14")))
        assertEquals("2026-02-14", history.first().achievedOnLocalDate.toString())
    }
}
