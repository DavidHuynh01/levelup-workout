package com.davidhuynh.levelup.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.common.LoadingScreen
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class HistoryUiState(
    val workouts: List<Workout> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isLoading: Boolean = true,
)

class HistoryViewModel(
    userId: String,
    workoutRepository: WorkoutRepositoryImpl,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(
        workoutRepository.observeHistory(userId),
        authRepository.observeUser(userId),
    ) { workouts, user ->
        HistoryUiState(
            workouts = workouts,
            weightUnit = user?.weightUnit ?: WeightUnit.LB,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}

private val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenWorkout: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingScreen()
        return
    }

    if (state.workouts.isEmpty()) {
        EmptyState(
            emoji = "📓",
            title = "No workouts yet",
            body = "Everything you log shows up here, newest first, grouped by month.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    // Grouped by month so a long history stays navigable.
    val grouped = state.workouts.groupBy { it.localDate.withDayOfMonth(1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
    ) {
        grouped.forEach { (month, workouts) ->
            item(key = "header-$month") {
                MonthHeader(month = month, workouts = workouts, unit = state.weightUnit)
            }
            // Stable keys let Compose move rows instead of rebuilding the list on every change.
            items(workouts, key = { it.id }) { workout ->
                WorkoutSummaryCard(
                    workout = workout,
                    unit = state.weightUnit,
                    onClick = { onOpenWorkout(workout.id) },
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun MonthHeader(month: LocalDate, workouts: List<Workout>, unit: WeightUnit) {
    val volume = workouts.sumOf { it.totalVolumeKg }
    Column(modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm)) {
        Text(text = month.format(monthFormat), style = MaterialTheme.typography.titleLarge)
        Text(
            text = "${workouts.size} workouts · ${WeightConverter.formatVolume(volume, unit)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WorkoutSummaryCard(workout: Workout, unit: WeightUnit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${workout.localDate.format(dayFormat)} · ${relativeDay(workout.localDate)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = WeightConverter.formatVolume(workout.totalVolumeKg, unit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentLime,
                    )
                    Text(
                        text = "${workout.exerciseCount} ex · ${workout.setCount} sets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (workout.exercises.isNotEmpty()) {
                Text(
                    text = workout.exercises.joinToString(" · ") { it.exercise.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}

private fun relativeDay(date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, LocalDate.now())
    return when {
        days == 0L -> "today"
        days == 1L -> "yesterday"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}
