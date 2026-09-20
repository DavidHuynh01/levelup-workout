package com.davidhuynh.levelup.data.local.seed

import com.davidhuynh.levelup.data.local.LevelUpDatabase
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.repository.DerivedDataRecomputer
import com.davidhuynh.levelup.data.repository.WorkoutDraft
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.model.FriendRequestStatus
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.security.PasswordHasher
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.util.AppClock
import java.time.ZoneId
import kotlin.random.Random

/**
 * Fills the database with other lifters so leaderboards and friend search have something
 * in them from the first launch.
 *
 * Debug builds only — see AppContainer. A release build starts empty.
 *
 * Demo accounts are flagged isDemo so the UI can label them. They all share the password
 * "demo1234", which is only acceptable because these accounts exist on one device, hold
 * nothing, and never ship in a release build.
 */
class DemoDataSeeder(
    private val database: LevelUpDatabase,
    private val workoutRepository: WorkoutRepositoryImpl,
    private val recomputer: DerivedDataRecomputer,
    private val hasher: PasswordHasher,
    private val tokens: TokenGenerator,
    private val clock: AppClock,
) {

    /**
     * Gives a newly created account two pending friend requests from demo lifters.
     *
     * Without this the friend flow has nothing to receive: demo accounts are never signed
     * into, so nobody would ever send the real user anything and the accept path and the
     * request badge could not be seen. Debug builds only.
     */
    suspend fun seedIncomingRequestsFor(userId: String) {
        val friendDao = database.friendDao()
        val senders = database.userDao().search(userId, "").filter { it.isDemo }.take(2)
        val now = clock.nowMillis()

        senders.forEachIndexed { index, sender ->
            if (friendDao.pendingRequestBetween(sender.id, userId) != null) return@forEachIndexed
            friendDao.insertRequest(
                FriendRequestEntity(
                    id = tokens.newId(),
                    fromUserId = sender.id,
                    toUserId = userId,
                    status = FriendRequestStatus.PENDING.name,
                    createdAt = now - (index + 1) * 60_000L,
                    respondedAt = null,
                )
            )
        }
    }

    suspend fun seedIfEmpty() {
        val userDao = database.userDao()
        if (userDao.demoCount() > 0) return

        // One hash reused for every demo account: deriving eight separately would add
        // roughly two seconds to first launch for no benefit.
        val sharedHash = hasher.hash(DEMO_PASSWORD)
        val now = clock.nowMillis()
        val zone = clock.zone()

        DEMO_PROFILES.forEachIndexed { index, profile ->
            val userId = tokens.newId()
            userDao.insert(
                UserEntity(
                    id = userId,
                    email = profile.email,
                    displayName = profile.displayName,
                    passwordHash = sharedHash.hash,
                    passwordSalt = sharedHash.salt,
                    passwordIterations = sharedHash.iterations,
                    avatarEmoji = profile.emoji,
                    weightUnit = WeightUnit.LB.name,
                    createdAt = now - DAY_MILLIS * (60 + index),
                    isDemo = true,
                )
            )
            seedWorkouts(userId, profile, zone)
            recomputer.recomputeEverything(userId)
        }
    }

    private suspend fun seedWorkouts(userId: String, profile: DemoProfile, zone: ZoneId) {
        // Seeded per profile so the leaderboard order is stable between installs.
        val random = Random(profile.email.hashCode())
        val now = clock.nowMillis()

        for (session in 0 until profile.sessions) {
            val daysAgo = session * profile.restDays + random.nextInt(0, 2)
            val performedAt = now - DAY_MILLIS * daysAgo - HOUR_MILLIS * 3
            val template = TEMPLATES[session % TEMPLATES.size]

            // Lifts creep up over time, so the earliest sessions are the lightest and the
            // record history has something to walk through.
            val progression = 1.0 - (daysAgo * 0.0015)

            val exercises = template.map { (exerciseName, baseWeightKg) ->
                val working = (baseWeightKg * profile.strength * progression)
                WorkoutDraft.ExerciseDraft(
                    exerciseId = ExerciseCatalogSeed.idOf(exerciseName),
                    sets = listOf(
                        WorkoutDraft.SetDraft(reps = 8, weightKg = roundToPlate(working * 0.5), isWarmup = true),
                        WorkoutDraft.SetDraft(reps = 8, weightKg = roundToPlate(working)),
                        WorkoutDraft.SetDraft(reps = 7, weightKg = roundToPlate(working)),
                        WorkoutDraft.SetDraft(reps = 6, weightKg = roundToPlate(working * 1.05)),
                    ),
                )
            }

            workoutRepository.save(
                WorkoutDraft(
                    userId = userId,
                    name = TEMPLATE_NAMES[session % TEMPLATES.size],
                    performedAt = performedAt,
                    zoneId = zone.id,
                    durationMinutes = 55 + random.nextInt(0, 25),
                    exercises = exercises,
                )
            )
        }
    }

    /** Real gyms load in 2.5 kg jumps, so demo numbers should too. */
    private fun roundToPlate(weightKg: Double): Double =
        (Math.round(weightKg / 2.5) * 2.5).coerceAtLeast(2.5)

    private data class DemoProfile(
        val displayName: String,
        val email: String,
        val emoji: String,
        /** Multiplier on the template weights. */
        val strength: Double,
        val sessions: Int,
        val restDays: Int,
    )

    private companion object {
        const val DEMO_PASSWORD = "demo1234"
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val HOUR_MILLIS = 60L * 60 * 1000

        val DEMO_PROFILES = listOf(
            DemoProfile("Jaylin M.", "jaylin@demo.levelup", "🔥", 1.30, 26, 2),
            DemoProfile("John C.", "john@demo.levelup", "💪", 1.22, 24, 2),
            DemoProfile("Alessandra C.", "alessandra@demo.levelup", "⚡", 1.05, 30, 2),
            DemoProfile("Marcus T.", "marcus@demo.levelup", "🏋", 1.15, 20, 3),
            DemoProfile("Priya R.", "priya@demo.levelup", "🌟", 0.95, 28, 2),
            DemoProfile("Diego S.", "diego@demo.levelup", "🚀", 1.08, 18, 3),
            DemoProfile("Hannah K.", "hannah@demo.levelup", "🦋", 0.88, 22, 3),
            DemoProfile("Tyler B.", "tyler@demo.levelup", "🎯", 1.00, 14, 4),
        )

        val TEMPLATE_NAMES = listOf("Push Day", "Pull Day", "Leg Day")

        /** Exercise name to a base working weight in kg for an average lifter. */
        val TEMPLATES: List<List<Pair<String, Double>>> = listOf(
            listOf(
                "Barbell Bench Press" to 70.0,
                "Overhead Press" to 45.0,
                "Incline Bench Press" to 55.0,
                "Triceps Pushdown" to 30.0,
            ),
            listOf(
                "Deadlift" to 110.0,
                "Barbell Row" to 70.0,
                "Lat Pulldown" to 55.0,
                "Barbell Curl" to 32.5,
            ),
            listOf(
                "Back Squat" to 95.0,
                "Romanian Deadlift" to 80.0,
                "Leg Press" to 140.0,
                "Calf Raise" to 60.0,
            ),
        )
    }
}
