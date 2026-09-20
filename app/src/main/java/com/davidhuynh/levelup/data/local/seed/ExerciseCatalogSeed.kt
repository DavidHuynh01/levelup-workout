package com.davidhuynh.levelup.data.local.seed

import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.domain.model.MuscleGroup

/**
 * The built-in exercise list. Ids are fixed strings rather than random UUIDs so the same
 * movement keeps the same id across installs, which keeps demo data and any future remote
 * sync from creating duplicates.
 */
object ExerciseCatalogSeed {

    fun exercises(createdAt: Long): List<ExerciseEntity> = RAW.map { (name, group, equipment) ->
        ExerciseEntity(
            id = "builtin-" + name.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
            name = name,
            muscleGroup = group.name,
            equipment = equipment,
            isCustom = false,
            ownerUserId = null,
            createdAt = createdAt,
        )
    }

    /** Named ids for the demo data, so it can reference specific lifts. */
    fun idOf(name: String): String =
        "builtin-" + name.lowercase().replace(Regex("[^a-z0-9]+"), "-")

    private val RAW: List<Triple<String, MuscleGroup, String?>> = listOf(
        Triple("Barbell Bench Press", MuscleGroup.CHEST, "Barbell"),
        Triple("Incline Bench Press", MuscleGroup.CHEST, "Barbell"),
        Triple("Dumbbell Bench Press", MuscleGroup.CHEST, "Dumbbell"),
        Triple("Dumbbell Fly", MuscleGroup.CHEST, "Dumbbell"),
        Triple("Cable Crossover", MuscleGroup.CHEST, "Cable"),
        Triple("Push-Up", MuscleGroup.CHEST, "Bodyweight"),
        Triple("Dip", MuscleGroup.CHEST, "Bodyweight"),

        Triple("Deadlift", MuscleGroup.BACK, "Barbell"),
        Triple("Barbell Row", MuscleGroup.BACK, "Barbell"),
        Triple("Dumbbell Row", MuscleGroup.BACK, "Dumbbell"),
        Triple("Lat Pulldown", MuscleGroup.BACK, "Cable"),
        Triple("Seated Cable Row", MuscleGroup.BACK, "Cable"),
        Triple("Pull-Up", MuscleGroup.BACK, "Bodyweight"),
        Triple("Chin-Up", MuscleGroup.BACK, "Bodyweight"),
        Triple("Face Pull", MuscleGroup.BACK, "Cable"),

        Triple("Overhead Press", MuscleGroup.SHOULDERS, "Barbell"),
        Triple("Dumbbell Shoulder Press", MuscleGroup.SHOULDERS, "Dumbbell"),
        Triple("Lateral Raise", MuscleGroup.SHOULDERS, "Dumbbell"),
        Triple("Front Raise", MuscleGroup.SHOULDERS, "Dumbbell"),
        Triple("Rear Delt Fly", MuscleGroup.SHOULDERS, "Dumbbell"),
        Triple("Shrug", MuscleGroup.SHOULDERS, "Barbell"),

        Triple("Barbell Curl", MuscleGroup.ARMS, "Barbell"),
        Triple("Dumbbell Curl", MuscleGroup.ARMS, "Dumbbell"),
        Triple("Hammer Curl", MuscleGroup.ARMS, "Dumbbell"),
        Triple("Preacher Curl", MuscleGroup.ARMS, "Barbell"),
        Triple("Triceps Pushdown", MuscleGroup.ARMS, "Cable"),
        Triple("Overhead Triceps Extension", MuscleGroup.ARMS, "Dumbbell"),
        Triple("Skull Crusher", MuscleGroup.ARMS, "Barbell"),
        Triple("Close-Grip Bench Press", MuscleGroup.ARMS, "Barbell"),

        Triple("Back Squat", MuscleGroup.LEGS, "Barbell"),
        Triple("Front Squat", MuscleGroup.LEGS, "Barbell"),
        Triple("Romanian Deadlift", MuscleGroup.LEGS, "Barbell"),
        Triple("Leg Press", MuscleGroup.LEGS, "Machine"),
        Triple("Bulgarian Split Squat", MuscleGroup.LEGS, "Dumbbell"),
        Triple("Lunge", MuscleGroup.LEGS, "Dumbbell"),
        Triple("Leg Extension", MuscleGroup.LEGS, "Machine"),
        Triple("Leg Curl", MuscleGroup.LEGS, "Machine"),
        Triple("Hip Thrust", MuscleGroup.LEGS, "Barbell"),
        Triple("Calf Raise", MuscleGroup.LEGS, "Machine"),

        Triple("Plank", MuscleGroup.CORE, "Bodyweight"),
        Triple("Hanging Leg Raise", MuscleGroup.CORE, "Bodyweight"),
        Triple("Cable Crunch", MuscleGroup.CORE, "Cable"),
        Triple("Russian Twist", MuscleGroup.CORE, "Dumbbell"),
        Triple("Ab Wheel Rollout", MuscleGroup.CORE, "Other"),

        Triple("Treadmill Run", MuscleGroup.CARDIO, "Machine"),
        Triple("Stationary Bike", MuscleGroup.CARDIO, "Machine"),
        Triple("Rowing Machine", MuscleGroup.CARDIO, "Machine"),
        Triple("Stair Climber", MuscleGroup.CARDIO, "Machine"),

        Triple("Clean and Jerk", MuscleGroup.FULL_BODY, "Barbell"),
        Triple("Power Clean", MuscleGroup.FULL_BODY, "Barbell"),
        Triple("Snatch", MuscleGroup.FULL_BODY, "Barbell"),
        Triple("Kettlebell Swing", MuscleGroup.FULL_BODY, "Kettlebell"),
        Triple("Burpee", MuscleGroup.FULL_BODY, "Bodyweight"),
    )
}
