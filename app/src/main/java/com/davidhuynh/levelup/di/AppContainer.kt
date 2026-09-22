package com.davidhuynh.levelup.di

import android.content.Context
import com.davidhuynh.levelup.BuildConfig
import com.davidhuynh.levelup.data.local.LevelUpDatabase
import com.davidhuynh.levelup.data.local.seed.DemoDataSeeder
import com.davidhuynh.levelup.data.local.seed.ExerciseCatalogSeed
import com.davidhuynh.levelup.data.prefs.DataStoreSessionStore
import com.davidhuynh.levelup.data.prefs.SessionStore
import com.davidhuynh.levelup.data.prefs.UserPreferencesStore
import com.davidhuynh.levelup.data.repository.AuthRepositoryImpl
import com.davidhuynh.levelup.data.repository.DerivedDataRecomputer
import com.davidhuynh.levelup.data.repository.ExerciseRepositoryImpl
import com.davidhuynh.levelup.data.repository.FriendRepositoryImpl
import com.davidhuynh.levelup.data.repository.LeaderboardRepositoryImpl
import com.davidhuynh.levelup.data.repository.PersonalRecordRepositoryImpl
import com.davidhuynh.levelup.data.repository.StatsRepositoryImpl
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.ExerciseRepository
import com.davidhuynh.levelup.domain.repository.LeaderboardRepository
import com.davidhuynh.levelup.domain.repository.PersonalRecordRepository
import com.davidhuynh.levelup.domain.repository.StatsRepository
import com.davidhuynh.levelup.domain.security.PasswordHasher
import com.davidhuynh.levelup.domain.security.Pbkdf2PasswordHasher
import com.davidhuynh.levelup.domain.security.SecureTokenGenerator
import com.davidhuynh.levelup.domain.security.TokenGenerator
import com.davidhuynh.levelup.domain.usecase.DeleteWorkoutUseCase
import com.davidhuynh.levelup.domain.usecase.SaveWorkoutUseCase
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.SystemAppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hand-rolled dependency container instead of Hilt.
 *
 * With this few objects, Hilt would add a second annotation processor and a layer of
 * generated code between a mistake and its error message. It also would not help with the
 * thing this app actually needs to stay flexible about: swapping the local repositories
 * for Firebase ones later is a one-line change per repository right here.
 */
class AppContainer(private val context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val clock: AppClock = SystemAppClock()
    val tokens: TokenGenerator = SecureTokenGenerator()
    val passwordHasher: PasswordHasher = Pbkdf2PasswordHasher()

    val database: LevelUpDatabase by lazy {
        LevelUpDatabase.build(context) { db -> onDatabaseOpened(db) }
    }

    val sessionStore: SessionStore by lazy { DataStoreSessionStore(context, clock) }
    val userPreferences: UserPreferencesStore by lazy { UserPreferencesStore(context) }

    private val demoSeeder: DemoDataSeeder by lazy {
        DemoDataSeeder(
            database = database,
            workoutRepository = workoutRepository,
            recomputer = recomputer,
            hasher = passwordHasher,
            tokens = tokens,
            clock = clock,
        )
    }

    private val recomputer: DerivedDataRecomputer by lazy {
        DerivedDataRecomputer(
            workoutDao = database.workoutDao(),
            setDao = database.exerciseSetDao(),
            recordDao = database.personalRecordDao(),
            statsDao = database.userStatsDao(),
            exerciseDao = database.exerciseDao(),
            tokens = tokens,
            clock = clock,
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            userDao = database.userDao(),
            sessionStore = sessionStore,
            hasher = passwordHasher,
            tokens = tokens,
            clock = clock,
            statsRecomputer = recomputer,
            onUserCreated = { newUserId ->
                // Wrapped: this is a debug convenience, and it must never be the reason a
                // real signup fails.
                if (BuildConfig.DEBUG) {
                    runCatching { demoSeeder.seedIncomingRequestsFor(newUserId) }
                }
            },
        )
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepositoryImpl(database.exerciseDao(), tokens, clock)
    }

    /** Concrete type: save and delete are commands this class owns, not read queries. */
    val workoutRepository: WorkoutRepositoryImpl by lazy {
        WorkoutRepositoryImpl(
            database = database,
            workoutDao = database.workoutDao(),
            setDao = database.exerciseSetDao(),
            recomputer = recomputer,
            tokens = tokens,
            clock = clock,
        )
    }

    val personalRecordRepository: PersonalRecordRepository by lazy {
        PersonalRecordRepositoryImpl(database.personalRecordDao(), database.exerciseSetDao())
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepositoryImpl(
            statsDao = database.userStatsDao(),
            workoutDao = database.workoutDao(),
            recomputer = recomputer,
            clock = clock,
        )
    }

    val leaderboardRepository: LeaderboardRepository by lazy {
        LeaderboardRepositoryImpl(database.userStatsDao())
    }

    /** Concrete: the Friends screen also observes outgoing requests, beyond the interface. */
    val friendRepository: FriendRepositoryImpl by lazy {
        FriendRepositoryImpl(database.friendDao(), database.userDao(), tokens, clock)
    }

    val saveWorkoutUseCase: SaveWorkoutUseCase by lazy { SaveWorkoutUseCase(workoutRepository) }
    val deleteWorkoutUseCase: DeleteWorkoutUseCase by lazy { DeleteWorkoutUseCase(workoutRepository) }

    /**
     * Opens the database at startup rather than waiting for the first screen that needs it.
     *
     * Without this, nothing touches Room until a user signs up — so the exercise catalogue
     * and the demo leaderboard would still be seeding while the signup was being written.
     */
    fun warmUp() {
        applicationScope.launch { database.userDao().count() }
    }

    /**
     * Runs on every database open. The catalogue insert ignores conflicts, so it is
     * effectively a no-op after the first launch.
     */
    private fun onDatabaseOpened(db: LevelUpDatabase) {
        applicationScope.launch {
            db.exerciseDao().insertAll(ExerciseCatalogSeed.exercises(clock.nowMillis()))

            if (BuildConfig.DEBUG) demoSeeder.seedIfEmpty()
        }
    }
}
