package com.davidhuynh.levelup.ui.workout.detail

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.logic.OneRepMax
import com.davidhuynh.levelup.domain.logic.VolumeCalculator
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.model.Workout
import com.davidhuynh.levelup.domain.model.WorkoutExercise
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.usecase.DeleteWorkoutUseCase
import com.davidhuynh.levelup.ui.common.ConfirmDialog
import com.davidhuynh.levelup.ui.common.KeyValueRow
import com.davidhuynh.levelup.ui.common.LevelUpTopBar
import com.davidhuynh.levelup.ui.common.LoadingScreen
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isLoading: Boolean = true,
)

class WorkoutDetailViewModel(
    private val userId: String,
    private val workoutId: String,
    workoutRepository: WorkoutRepositoryImpl,
    private val deleteWorkout: DeleteWorkoutUseCase,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<WorkoutDetailUiState> = combine(
        workoutRepository.observeWorkout(workoutId),
        authRepository.observeUser(userId),
    ) { workout, user ->
        WorkoutDetailUiState(
            workout = workout,
            weightUnit = user?.weightUnit ?: WeightUnit.LB,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /**
     * Deleting also rebuilds records and stats, so a record set in this workout drops back
     * to the previous best rather than lingering.
     */
    fun delete() {
        viewModelScope.launch {
            deleteWorkout(userId, workoutId)
            _deleted.value = true
        }
    }
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")

@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onRepeat: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = { LevelUpTopBar(title = "Workout", onBack = onBack) },
    ) { padding ->
        val workout = state.workout
        when {
            state.isLoading -> LoadingScreen()
            workout == null -> Text(
                text = "This workout is no longer here.",
                modifier = Modifier.padding(padding).padding(Spacing.lg),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md),
            ) {
                Text(text = workout.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = workout.localDate.format(dateFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(Spacing.md))

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        KeyValueRow(
                            "Total volume",
                            WeightConverter.formatVolume(workout.totalVolumeKg, state.weightUnit),
                        )
                        KeyValueRow("Exercises", workout.exerciseCount.toString())
                        KeyValueRow("Sets", workout.setCount.toString())
                        workout.durationMinutes?.let { KeyValueRow("Duration", "$it min") }
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                workout.exercises.forEach { exercise ->
                    ExerciseDetailCard(exercise = exercise, unit = state.weightUnit)
                    Spacer(Modifier.height(Spacing.sm))
                }

                Spacer(Modifier.height(Spacing.md))

                // Most sessions repeat a previous one, so this is the primary action here:
                // it opens the log screen already filled in, dated today.
                Button(
                    onClick = { onRepeat(workout.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text("Repeat this workout") }

                Spacer(Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = { onEdit(workout.id) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Edit") }

                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }

                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this workout?",
            body = "Your totals, streak and records will be recalculated without it.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ExerciseDetailCard(exercise: WorkoutExercise, unit: WeightUnit) {
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
                Text(text = exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = WeightConverter.formatVolume(
                        VolumeCalculator.exerciseVolumeKg(exercise),
                        unit,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentLime,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            exercise.sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (set.isWarmup) "Warmup" else "Set ${set.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${set.reps} × ${WeightConverter.format(set.weightKg, unit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (set.isWarmup) FontWeight.Normal else FontWeight.SemiBold,
                    )
                }
            }

            // The best estimated 1RM in this session, for context on how hard it was.
            val bestEstimate = exercise.sets
                .filter { it.countsAsWorking }
                .mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }
                .maxOrNull()

            if (bestEstimate != null) {
                Text(
                    text = "Best estimated 1RM this session: ${WeightConverter.format(bestEstimate, unit)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}
