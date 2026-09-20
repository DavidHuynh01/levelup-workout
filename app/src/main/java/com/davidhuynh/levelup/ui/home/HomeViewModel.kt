package com.davidhuynh.levelup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val displayName: String = "",
    val stats: UserStats = UserStats(userId = ""),
    val recentWorkouts: List<Workout> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isLoading: Boolean = true,
) {
    val lastWorkout: Workout? get() = recentWorkouts.firstOrNull()
    val hasTrainedToday: Boolean get() = stats.lastWorkoutLocalDate != null && stats.currentStreakDays > 0
}

class HomeViewModel(
    private val userId: String,
    workoutRepository: WorkoutRepositoryImpl,
    statsRepository: StatsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        authRepository.observeUser(userId),
        statsRepository.observeStats(userId),
        workoutRepository.observeRecentWorkouts(userId, limit = 3),
    ) { user, stats, recent ->
        HomeUiState(
            displayName = user?.displayName ?: "",
            stats = stats,
            recentWorkouts = recent,
            weightUnit = user?.weightUnit ?: WeightUnit.LB,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
