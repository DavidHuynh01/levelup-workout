package com.davidhuynh.levelup.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.common.SectionHeader
import com.davidhuynh.levelup.ui.common.StatCard
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import com.davidhuynh.levelup.ui.theme.StreakOrange
import java.time.format.DateTimeFormatter

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogWorkout: () -> Unit,
    onOpenWorkout: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
    ) {
        Text(
            text = greeting(state.displayName),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = Spacing.lg),
        )
        Text(
            text = streakLine(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = onLogWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Log a workout", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatCard(
                label = "Streak",
                value = "${state.stats.currentStreakDays}d",
                caption = "Best ${state.stats.longestStreakDays}d",
                accent = if (state.stats.currentStreakDays > 0) StreakOrange else null,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "This week",
                value = "${state.stats.workoutsThisWeek}",
                caption = if (state.stats.workoutsThisWeek == 1) "workout" else "workouts",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatCard(
                label = "Total volume",
                value = WeightConverter.formatVolume(state.stats.totalVolumeKg, state.weightUnit),
                caption = "${state.stats.totalWorkouts} sessions",
                accent = AccentLime,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Records",
                value = "${state.stats.prCount}",
                caption = "standing PRs",
                modifier = Modifier.weight(1f),
            )
        }

        SectionHeader("Recent workouts")

        if (state.recentWorkouts.isEmpty()) {
            EmptyState(
                emoji = "💪",
                title = "Nothing logged yet",
                body = "Log your first workout and your volume, records and streak start tracking themselves.",
            )
        } else {
            state.recentWorkouts.forEach { workout ->
                WorkoutRow(
                    workout = workout,
                    unit = state.weightUnit,
                    onClick = { onOpenWorkout(workout.id) },
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }

        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun WorkoutRow(workout: Workout, unit: WeightUnit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${workout.localDate.format(dateFormat)} · " +
                        "${workout.exerciseCount} exercises · ${workout.setCount} sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = WeightConverter.formatVolume(workout.totalVolumeKg, unit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentLime,
            )
        }
    }
}

private fun greeting(name: String): String =
    if (name.isBlank()) "Welcome back" else "Hey ${name.substringBefore(' ')}"

private fun streakLine(state: HomeUiState): String = when {
    state.stats.totalWorkouts == 0 -> "Let's get the first one in."
    state.stats.currentStreakDays == 0 -> "Streak's reset — one workout starts it again."
    state.stats.currentStreakDays == 1 -> "1 day streak. Keep it going."
    else -> "${state.stats.currentStreakDays} day streak. Don't break it."
}
