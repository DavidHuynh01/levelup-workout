package com.davidhuynh.levelup.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.davidhuynh.levelup.ui.auth.AuthViewModel
import com.davidhuynh.levelup.ui.auth.login.LoginViewModel
import com.davidhuynh.levelup.ui.auth.login.SignUpViewModel
import com.davidhuynh.levelup.ui.friends.FriendsViewModel
import com.davidhuynh.levelup.ui.history.HistoryViewModel
import com.davidhuynh.levelup.ui.home.HomeViewModel
import com.davidhuynh.levelup.ui.leaderboard.LeaderboardViewModel
import com.davidhuynh.levelup.ui.profile.ProfileViewModel
import com.davidhuynh.levelup.ui.progress.ExerciseRecordsViewModel
import com.davidhuynh.levelup.ui.progress.ProgressViewModel
import com.davidhuynh.levelup.ui.workout.detail.WorkoutDetailViewModel
import com.davidhuynh.levelup.ui.workout.log.LogWorkoutViewModel

/**
 * One factory for every ViewModel, built from the AppContainer.
 *
 * This is the cost of skipping Hilt: a list of constructors in one place. Worth it — the
 * list is explicit and a wrong wiring is a compile error rather than a runtime one.
 */
fun levelUpViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {

    initializer { AuthViewModel(container.authRepository) }

    initializer { LoginViewModel(container.authRepository) }

    initializer { SignUpViewModel(container.authRepository) }

    initializer {
        HomeViewModel(
            userId = requireUserId(),
            workoutRepository = container.workoutRepository,
            statsRepository = container.statsRepository,
            authRepository = container.authRepository,
        )
    }

    initializer {
        HistoryViewModel(
            userId = requireUserId(),
            workoutRepository = container.workoutRepository,
            authRepository = container.authRepository,
        )
    }

    initializer {
        LogWorkoutViewModel(
            userId = requireUserId(),
            workoutId = get(WORKOUT_ID_KEY),
            exerciseRepository = container.exerciseRepository,
            workoutRepository = container.workoutRepository,
            saveWorkout = container.saveWorkoutUseCase,
            authRepository = container.authRepository,
            clock = container.clock,
        )
    }

    initializer {
        WorkoutDetailViewModel(
            userId = requireUserId(),
            workoutId = requireNotNull(get(WORKOUT_ID_KEY)) { "workoutId is required" },
            workoutRepository = container.workoutRepository,
            deleteWorkout = container.deleteWorkoutUseCase,
            authRepository = container.authRepository,
        )
    }

    initializer {
        ProgressViewModel(
            userId = requireUserId(),
            recordRepository = container.personalRecordRepository,
            statsRepository = container.statsRepository,
            authRepository = container.authRepository,
        )
    }

    initializer {
        ExerciseRecordsViewModel(
            userId = requireUserId(),
            exerciseId = requireNotNull(get(EXERCISE_ID_KEY)) { "exerciseId is required" },
            recordRepository = container.personalRecordRepository,
            authRepository = container.authRepository,
        )
    }

    initializer {
        LeaderboardViewModel(
            userId = requireUserId(),
            leaderboardRepository = container.leaderboardRepository,
            authRepository = container.authRepository,
        )
    }

    initializer {
        FriendsViewModel(
            userId = requireUserId(),
            friendRepository = container.friendRepository,
        )
    }

    initializer {
        ProfileViewModel(
            userId = requireUserId(),
            authRepository = container.authRepository,
            statsRepository = container.statsRepository,
        )
    }
}

/**
 * Screen arguments are passed as CreationExtras rather than through SavedStateHandle so
 * the ViewModel constructors stay plain and testable.
 */
val USER_ID_KEY = object : CreationExtras.Key<String> {}
val WORKOUT_ID_KEY = object : CreationExtras.Key<String> {}
val EXERCISE_ID_KEY = object : CreationExtras.Key<String> {}

private fun CreationExtras.requireUserId(): String =
    requireNotNull(get(USER_ID_KEY)) { "userId is required for this screen" }
