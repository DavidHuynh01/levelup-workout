package com.davidhuynh.levelup.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.davidhuynh.levelup.di.AppContainer
import com.davidhuynh.levelup.di.EXERCISE_ID_KEY
import com.davidhuynh.levelup.di.REPEAT_OF_KEY
import com.davidhuynh.levelup.di.USER_ID_KEY
import com.davidhuynh.levelup.di.WORKOUT_ID_KEY
import com.davidhuynh.levelup.di.levelUpViewModelFactory
import com.davidhuynh.levelup.domain.model.AuthState
import com.davidhuynh.levelup.ui.auth.AuthViewModel
import com.davidhuynh.levelup.ui.auth.login.LoginScreen
import com.davidhuynh.levelup.ui.auth.login.LoginViewModel
import com.davidhuynh.levelup.ui.auth.login.SignUpScreen
import com.davidhuynh.levelup.ui.auth.login.SignUpViewModel
import com.davidhuynh.levelup.ui.common.LoadingScreen
import com.davidhuynh.levelup.ui.friends.FriendsScreen
import com.davidhuynh.levelup.ui.friends.FriendsViewModel
import com.davidhuynh.levelup.ui.history.HistoryScreen
import com.davidhuynh.levelup.ui.history.HistoryViewModel
import com.davidhuynh.levelup.ui.home.HomeScreen
import com.davidhuynh.levelup.ui.home.HomeViewModel
import com.davidhuynh.levelup.ui.leaderboard.LeaderboardScreen
import com.davidhuynh.levelup.ui.leaderboard.LeaderboardViewModel
import com.davidhuynh.levelup.ui.profile.ProfileScreen
import com.davidhuynh.levelup.ui.profile.ProfileViewModel
import com.davidhuynh.levelup.ui.progress.ExerciseRecordsScreen
import com.davidhuynh.levelup.ui.progress.ExerciseRecordsViewModel
import com.davidhuynh.levelup.ui.progress.ProgressScreen
import com.davidhuynh.levelup.ui.progress.ProgressViewModel
import com.davidhuynh.levelup.ui.workout.detail.WorkoutDetailScreen
import com.davidhuynh.levelup.ui.workout.detail.WorkoutDetailViewModel
import com.davidhuynh.levelup.ui.workout.log.LogWorkoutScreen
import com.davidhuynh.levelup.ui.workout.log.LogWorkoutViewModel

/**
 * The single decision point for signed in versus signed out.
 *
 * The graph is only built once the stored session has been read, so the start destination is
 * right the first time. That is what avoids the login screen flashing before Home on a cold
 * start, and it means no popUpTo juggling after sign-in.
 */
@Composable
fun AuthGate(container: AppContainer) {
    val factory = remember(container) { levelUpViewModelFactory(container) }
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthState.Loading -> LoadingScreen()

        AuthState.Unauthenticated -> AuthFlow(factory = factory)

        is AuthState.Authenticated -> SignedInApp(
            userId = state.userId,
            container = container,
            factory = factory,
            onSignOut = authViewModel::signOut,
        )
    }
}

@Composable
private fun AuthFlow(factory: ViewModelProvider.Factory) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = viewModel,
                onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) },
            )
        }
        composable(Routes.SIGN_UP) {
            val viewModel: SignUpViewModel = viewModel(factory = factory)
            SignUpScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun SignedInApp(
    userId: String,
    container: AppContainer,
    factory: ViewModelProvider.Factory,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Pending friend requests drive the badge on the Profile tab. No push notifications:
    // with no server there is nothing to send them.
    val pendingRequests by remember(userId) {
        container.friendRepository.observePendingRequestCount(userId)
    }.collectAsStateWithLifecycle(initialValue = 0)

    // The bottom bar belongs to the tabs, not to full-screen flows like logging a workout.
    val showBottomBar = backStackEntry?.destination?.hierarchy?.any { destination ->
        BottomTab.entries.any { it.route == destination.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LevelUpBottomBar(navController, pendingRequestCount = pendingRequests)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel(
                    factory = factory,
                    extras = extrasFor(userId),
                )
                HomeScreen(
                    viewModel = viewModel,
                    onLogWorkout = { navController.navigate(Routes.logWorkout()) },
                    onOpenWorkout = { navController.navigate(Routes.workoutDetail(it)) },
                )
            }

            composable(Routes.HISTORY) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = factory,
                    extras = extrasFor(userId),
                )
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenWorkout = { navController.navigate(Routes.workoutDetail(it)) },
                )
            }

            composable(Routes.PROGRESS) {
                val viewModel: ProgressViewModel = viewModel(
                    factory = factory,
                    extras = extrasFor(userId),
                )
                ProgressScreen(
                    viewModel = viewModel,
                    onOpenExercise = { navController.navigate(Routes.exerciseRecords(it)) },
                )
            }

            composable(
                route = "${Routes.EXERCISE_RECORDS}/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
            ) { entry ->
                val exerciseId = entry.arguments?.getString("exerciseId").orEmpty()
                val viewModel: ExerciseRecordsViewModel = viewModel(
                    key = "records-$exerciseId",
                    factory = factory,
                    extras = extrasFor(userId, exerciseId = exerciseId),
                )
                ExerciseRecordsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.LEADERBOARD) {
                val viewModel: LeaderboardViewModel = viewModel(
                    factory = factory,
                    extras = extrasFor(userId),
                )
                LeaderboardScreen(
                    viewModel = viewModel,
                    onFindFriends = { navController.navigate(Routes.FRIENDS) },
                )
            }

            composable(Routes.PROFILE) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = factory,
                    extras = extrasFor(userId),
                )
                ProfileScreen(
                    viewModel = viewModel,
                    pendingRequestCount = pendingRequests,
                    onOpenFriends = { navController.navigate(Routes.FRIENDS) },
                    onSignOut = onSignOut,
                )
            }

            composable(Routes.FRIENDS) {
                val viewModel: FriendsViewModel = viewModel(
                    factory = factory,
                    extras = extrasFor(userId),
                )
                FriendsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.LOG_WORKOUT}?workoutId={workoutId}&repeatOf={repeatOf}",
                arguments = listOf(
                    navArgument("workoutId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("repeatOf") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val workoutId = entry.arguments?.getString("workoutId")
                val repeatOf = entry.arguments?.getString("repeatOf")
                // Keyed by the source workout so editing or repeating two in a row does
                // not reuse the first one's draft.
                val viewModel: LogWorkoutViewModel = viewModel(
                    key = "log-${workoutId ?: repeatOf?.let { "repeat-$it" } ?: "new"}",
                    factory = factory,
                    extras = extrasFor(userId, workoutId, repeatOfWorkoutId = repeatOf),
                )
                LogWorkoutScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.WORKOUT_DETAIL}/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType }),
            ) { entry ->
                val workoutId = entry.arguments?.getString("workoutId").orEmpty()
                val viewModel: WorkoutDetailViewModel = viewModel(
                    key = "detail-$workoutId",
                    factory = factory,
                    extras = extrasFor(userId, workoutId),
                )
                WorkoutDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.logWorkout(it)) },
                    onRepeat = { navController.navigate(Routes.repeatWorkout(it)) },
                )
            }
        }
    }
}

private fun extrasFor(
    userId: String,
    workoutId: String? = null,
    exerciseId: String? = null,
    repeatOfWorkoutId: String? = null,
): CreationExtras = MutableCreationExtras().apply {
    set(USER_ID_KEY, userId)
    if (workoutId != null) set(WORKOUT_ID_KEY, workoutId)
    if (exerciseId != null) set(EXERCISE_ID_KEY, exerciseId)
    if (repeatOfWorkoutId != null) set(REPEAT_OF_KEY, repeatOfWorkoutId)
}
