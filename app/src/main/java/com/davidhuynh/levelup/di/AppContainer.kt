package com.davidhuynh.levelup.di

import android.content.Context
import com.davidhuynh.levelup.BuildConfig
import com.davidhuynh.levelup.data.local.LevelUpDatabase
import com.davidhuynh.levelup.data.local.seed.DemoDataSeeder
import com.davidhuynh.levelup.data.export.WorkoutExporter
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

                if (BuildConfig.DEBUG) {
                    runCatching { demoSeeder.seedIncomingRequestsFor(newUserId) }
                }
            },
        )
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepositoryImpl(database.exerciseDao(), tokens, clock)
    }

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

    val friendRepository: FriendRepositoryImpl by lazy {
        FriendRepositoryImpl(database.friendDao(), database.userDao(), tokens, clock)
    }

    val workoutExporter: WorkoutExporter by lazy {
        WorkoutExporter(context, workoutRepository, authRepository, clock)
    }

    val saveWorkoutUseCase: SaveWorkoutUseCase by lazy { SaveWorkoutUseCase(workoutRepository) }
    val deleteWorkoutUseCase: DeleteWorkoutUseCase by lazy { DeleteWorkoutUseCase(workoutRepository) }

    fun warmUp() {
        applicationScope.launch { database.userDao().count() }
    }

    private fun onDatabaseOpened(db: LevelUpDatabase) {
        applicationScope.launch {
            db.exerciseDao().insertAll(ExerciseCatalogSeed.exercises(clock.nowMillis()))

            if (BuildConfig.DEBUG) demoSeeder.seedIfEmpty()
        }
    }
}
