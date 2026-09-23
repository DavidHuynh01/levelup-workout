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
import androidx.compose.material3.Scaffold
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
import com.davidhuynh.levelup.domain.logic.ProgressPoint
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.PersonalRecord
import com.davidhuynh.levelup.domain.model.PrType
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.PersonalRecordRepository
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.common.LevelUpTopBar
import com.davidhuynh.levelup.ui.common.LoadingScreen
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.format.DateTimeFormatter

data class ExerciseRecordsUiState(
    val exerciseName: String = "",
    val records: List<PersonalRecord> = emptyList(),
    val progress: List<ProgressPoint> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isLoading: Boolean = true,
) {

    val byType: List<Pair<PrType, List<PersonalRecord>>>
        get() = records
            .groupBy { it.recordType }
            .toList()
            .sortedBy { (type, _) -> type.ordinal }
            .map { (type, list) -> type to list.sortedByDescending { it.achievedAt } }
}

class ExerciseRecordsViewModel(
    userId: String,
    exerciseId: String,
    recordRepository: PersonalRecordRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<ExerciseRecordsUiState> = combine(
        recordRepository.observeRecordHistory(userId, exerciseId),
        recordRepository.observeProgress(userId, exerciseId),
        authRepository.observeUser(userId),
    ) { records, progress, user ->
        ExerciseRecordsUiState(
            exerciseName = records.firstOrNull()?.exerciseName.orEmpty(),
            records = records,
            progress = progress,
            weightUnit = user?.weightUnit ?: WeightUnit.LB,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseRecordsUiState())
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun ExerciseRecordsScreen(
    viewModel: ExerciseRecordsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LevelUpTopBar(
                title = state.exerciseName.ifBlank { "Records" },
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()

            state.records.isEmpty() -> EmptyState(
                emoji = "🏋",
                title = "No records here yet",
                body = "Log a working set on this exercise and its first records appear.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.md),
            ) {
                item {
                    ProgressChart(points = state.progress, unit = state.weightUnit)
                }

                state.byType.forEach { (type, records) ->
                    item(key = "header-${type.name}") {
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm),
                        )
                    }
                    items(records, key = { it.id }) { record ->
                        RecordHistoryRow(record = record, unit = state.weightUnit)
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }
                item { Spacer(Modifier.height(Spacing.xl)) }
            }
        }
    }
}

@Composable
private fun RecordHistoryRow(record: PersonalRecord, unit: WeightUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayValue(record, unit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (record.isCurrent) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = record.achievedOnLocalDate.format(dateFormat),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (record.isCurrent) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = AccentLime,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

private fun displayValue(record: PersonalRecord, unit: WeightUnit): String =
    when (record.recordType) {
        PrType.MAX_WEIGHT -> "${WeightConverter.format(record.weightKg, unit)} × ${record.reps}"
        PrType.MAX_ESTIMATED_1RM -> WeightConverter.format(record.value, unit)
        PrType.MAX_SESSION_VOLUME -> WeightConverter.formatVolume(record.value, unit)
    }
