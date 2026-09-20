package com.davidhuynh.levelup.ui.progress

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.PersonalRecord
import com.davidhuynh.levelup.domain.model.PrType
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.PersonalRecordRepository
import com.davidhuynh.levelup.domain.repository.StatsRepository
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import com.davidhuynh.levelup.ui.theme.StreakOrange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.format.DateTimeFormatter

data class ProgressUiState(
    val stats: UserStats = UserStats(userId = ""),
    val records: List<PersonalRecord> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isLoading: Boolean = true,
) {
    /** One card per exercise, each listing whichever record types it holds. */
    val recordsByExercise: List<Pair<String, List<PersonalRecord>>>
        get() = records
            .groupBy { it.exerciseName }
            .toList()
            .sortedByDescending { (_, records) -> records.maxOf { it.achievedAt } }
}

class ProgressViewModel(
    userId: String,
    recordRepository: PersonalRecordRepository,
    statsRepository: StatsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<ProgressUiState> = combine(
        statsRepository.observeStats(userId),
        recordRepository.observeCurrentRecords(userId),
        authRepository.observeUser(userId),
    ) { stats, records, user ->
        ProgressUiState(
            stats = stats,
            records = records,
            weightUnit = user?.weightUnit ?: WeightUnit.LB,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}

private val recordDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
    ) {
        item {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.md),
            )
            StreakCard(stats = state.stats, unit = state.weightUnit)
            Spacer(Modifier.height(Spacing.lg))
            Text(text = "Personal records", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.sm))
        }

        if (!state.isLoading && state.records.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🏅",
                    title = "No records yet",
                    body = "Log a few sets and records appear on their own — heaviest weight, best estimated 1RM, and best session volume per exercise.",
                )
            }
        }

        items(state.recordsByExercise, key = { it.first }) { (exerciseName, records) ->
            ExerciseRecordCard(
                exerciseName = exerciseName,
                records = records,
                unit = state.weightUnit,
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun StreakCard(stats: UserStats, unit: WeightUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "🔥 ${stats.currentStreakDays} day streak",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (stats.currentStreakDays > 0) {
                            StreakOrange
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = "Longest ${stats.longestStreakDays} days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.workoutsThisWeek}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "this week",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiniStat("Volume", WeightConverter.formatVolume(stats.totalVolumeKg, unit))
                MiniStat("Workouts", stats.totalWorkouts.toString())
                MiniStat("Sets", stats.totalSets.toString())
                MiniStat("Records", stats.prCount.toString())
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseRecordCard(
    exerciseName: String,
    records: List<PersonalRecord>,
    unit: WeightUnit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(text = exerciseName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))

            records.sortedBy { it.recordType.ordinal }.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RecordTypeBadge(record.recordType)
                        Column(modifier = Modifier.padding(start = Spacing.sm)) {
                            Text(
                                text = recordValue(record, unit),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = record.achievedOnLocalDate.format(recordDateFormat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordTypeBadge(type: PrType) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = AccentLime,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = type.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

private fun recordValue(record: PersonalRecord, unit: WeightUnit): String = when (record.recordType) {
    PrType.MAX_WEIGHT ->
        "${WeightConverter.format(record.weightKg, unit)} × ${record.reps}"

    PrType.MAX_ESTIMATED_1RM ->
        "${WeightConverter.format(record.value, unit)} est. 1RM"

    PrType.MAX_SESSION_VOLUME ->
        "${WeightConverter.formatVolume(record.value, unit)} in one session"
}
