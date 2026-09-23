package com.davidhuynh.levelup.logic

import com.davidhuynh.levelup.domain.logic.CsvExport
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.ExerciseSet
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExportTest {

    private fun workout(
        name: String = "Push Day",
        day: String = "2026-03-01",
        performedAt: Long = 1_000,
        sets: List<ExerciseSet> = listOf(set(reps = 5, weightKg = 100.0)),
        exerciseName: String = "Barbell Bench Press",
    ) = Workout(
        id = "w-$day-$name",
        userId = "u1",
        name = name,
        performedAt = performedAt,
        localDate = LocalDate.parse(day),
        zoneId = "America/Chicago",
        exercises = listOf(
            WorkoutExercise(
                id = "we1",
                exercise = Exercise(id = "e1", name = exerciseName, muscleGroup = MuscleGroup.CHEST),
                orderIndex = 0,
                sets = sets,
            )
        ),
    )

    private fun set(reps: Int, weightKg: Double, isWarmup: Boolean = false, number: Int = 1) =
        ExerciseSet(
            id = "s$number",
            setNumber = number,
            reps = reps,
            weightKg = weightKg,
            isWarmup = isWarmup,
            completedAt = 0,
        )

    private fun lines(csv: String) = csv.trim().lines()

    @Test
    fun `an empty history still produces a header`() {
        val csv = CsvExport.toCsv(emptyList(), WeightUnit.KG)
        assertEquals(1, lines(csv).size)
        assertTrue(lines(csv).first().startsWith("date,workout,exercise"))
    }

    @Test
    fun `one row per set`() {
        val csv = CsvExport.toCsv(
            listOf(
                workout(
                    sets = listOf(
                        set(reps = 5, weightKg = 100.0, number = 1),
                        set(reps = 5, weightKg = 100.0, number = 2),
                        set(reps = 3, weightKg = 110.0, number = 3),
                    )
                )
            ),
            WeightUnit.KG,
        )
        assertEquals(4, lines(csv).size)
    }

    @Test
    fun `weights come out in the requested unit`() {
        val kg = CsvExport.toCsv(listOf(workout()), WeightUnit.KG)
        assertTrue(kg.contains(",5,100,kg,"))

        val lb = CsvExport.toCsv(listOf(workout()), WeightUnit.LB)
        assertTrue("100 kg should export as 220.46 lb", lb.contains(",5,220.46,lb,"))
    }

    @Test
    fun `volume is reps times weight and warmups contribute nothing`() {
        val csv = CsvExport.toCsv(
            listOf(
                workout(
                    sets = listOf(
                        set(reps = 10, weightKg = 40.0, isWarmup = true, number = 1),
                        set(reps = 5, weightKg = 100.0, number = 2),
                    )
                )
            ),
            WeightUnit.KG,
        )
        val rows = lines(csv).drop(1)
        assertTrue("warmup row ends with 0 volume", rows[0].endsWith(",yes,0"))
        assertTrue("working row carries its volume", rows[1].endsWith(",no,500"))
    }

    @Test
    fun `names containing commas and quotes are quoted properly`() {
        val csv = CsvExport.toCsv(
            listOf(workout(name = "Push, heavy \"top set\"")),
            WeightUnit.KG,
        )
        val row = lines(csv)[1]
        assertTrue(row.contains("\"Push, heavy \"\"top set\"\"\""))

        assertEquals(10, splitCsvRow(row).size)
    }

    @Test
    fun `rows are oldest first`() {
        val csv = CsvExport.toCsv(
            listOf(
                workout(name = "Newer", day = "2026-03-08", performedAt = 2_000),
                workout(name = "Older", day = "2026-03-01", performedAt = 1_000),
            ),
            WeightUnit.KG,
        )
        val rows = lines(csv).drop(1)
        assertTrue(rows[0].contains("Older"))
        assertTrue(rows[1].contains("Newer"))
    }

    @Test
    fun `the file name carries the date it was exported`() {
        assertEquals("levelup-workouts-2026-03-15.csv", CsvExport.fileName("2026-03-15"))
    }

    private fun splitCsvRow(row: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < row.length) {
            val char = row[index]
            when {
                inQuotes && char == '"' && row.getOrNull(index + 1) == '"' -> {
                    current.append('"'); index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields += current.toString(); current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()
        return fields
    }
}
