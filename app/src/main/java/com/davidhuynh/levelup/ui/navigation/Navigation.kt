package com.davidhuynh.levelup.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.davidhuynh.levelup.ui.common.CountBadge

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val HISTORY = "history"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    /** One screen serves logging, editing, and repeating a past workout. */
    const val LOG_WORKOUT = "workout/log"
    fun logWorkout(workoutId: String? = null): String =
        if (workoutId == null) LOG_WORKOUT else "$LOG_WORKOUT?workoutId=$workoutId"

    fun repeatWorkout(workoutId: String): String = "$LOG_WORKOUT?repeatOf=$workoutId"

    const val WORKOUT_DETAIL = "workout/detail"
    fun workoutDetail(workoutId: String): String = "$WORKOUT_DETAIL/$workoutId"

    const val EXERCISE_RECORDS = "records/exercise"
    fun exerciseRecords(exerciseId: String): String = "$EXERCISE_RECORDS/$exerciseId"

    // Phase 4 and 5.
    const val LEADERBOARD = "leaderboard"
    const val FRIENDS = "friends"
}

enum class BottomTab(val route: String, val label: String, val icon: String) {
    HOME(Routes.HOME, "Home", "🏠"),
    HISTORY(Routes.HISTORY, "History", "📓"),
    PROGRESS(Routes.PROGRESS, "Progress", "📈"),
    LEADERBOARD(Routes.LEADERBOARD, "Board", "🏆"),
    PROFILE(Routes.PROFILE, "Profile", "👤"),
}

@Composable
fun LevelUpBottomBar(
    navController: NavHostController,
    pendingRequestCount: Int = 0,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        BottomTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            // Tabs are siblings, not a stack: going Home then History then Home
                            // should not leave three entries behind.
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Box {
                        Text(tab.icon)
                        if (tab == BottomTab.PROFILE && pendingRequestCount > 0) {
                            CountBadge(pendingRequestCount, modifier = Modifier)
                        }
                    }
                },
                label = { Text(tab.label) },
            )
        }
    }
}
