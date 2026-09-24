package com.davidhuynh.levelup.ui.workout.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidhuynh.levelup.domain.logic.RestTimer
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.PrAward
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.ui.common.ErrorBanner
import com.davidhuynh.levelup.ui.common.LevelUpTopBar
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutScreen(
    viewModel: LogWorkoutViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedWorkoutId, state.awards.isEmpty()) {
        if (state.savedWorkoutId != null && state.awards.isEmpty()) onSaved()
    }

    Scaffold(
        topBar = {
            LevelUpTopBar(
                title = if (state.isEdit) "Edit workout" else "Log workout",
                onBack = onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Spacing.md)
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${state.workingSetCount} working sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.totalVolumeDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentLime,
                    )
                }
                RestBar(
                    rest = state.rest,
                    restSeconds = state.restSeconds,
                    onStart = viewModel::startRest,
                    onExtend = viewModel::extendRest,
                    onStop = viewModel::stopRest,
                )

                Spacer(Modifier.height(Spacing.sm))

                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(if (state.isEdit) "Save changes" else "Finish workout")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
        ) {
            item {
                ErrorBanner(state.error)
                if (state.error != null) Spacer(Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Workout name") },
                    placeholder = { Text("Push day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("📅  ${state.date.format(dateFormat)}")
                }
                Spacer(Modifier.height(Spacing.md))
            }

            items(state.blocks, key = { it.key }) { block ->
                ExerciseBlockCard(
                    block = block,
                    unit = state.weightUnit,
                    onAddSet = { viewModel.addSet(block.key) },
                    onRemoveSet = { setKey -> viewModel.removeSet(block.key, setKey) },
                    onRemoveExercise = { viewModel.removeExercise(block.key) },
                    onUpdateSet = { setKey, reps, weight, warmup ->
                        viewModel.updateSet(block.key, setKey, reps, weight, warmup)
                    },
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            item {
                OutlinedButton(
                    onClick = viewModel::openPicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("+  Add exercise")
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    if (state.isPickerOpen) {
        ExercisePickerSheet(
            query = state.searchQuery,
            exercises = state.filteredCatalogue,
            onQueryChange = viewModel::onSearchChange,
            onPick = viewModel::addExercise,
            onCreate = { name -> viewModel.createAndAddExercise(name, muscleGroupGuess()) },
            onDismiss = viewModel::closePicker,
        )
    }

    if (showDatePicker) {
        WorkoutDatePicker(
            initial = state.date,
            onPick = {
                viewModel.onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (state.awards.isNotEmpty()) {
        PrCelebrationDialog(
            awards = state.awards,
            unit = state.weightUnit,
            onDismiss = {
                viewModel.dismissAwards()
                onSaved()
            },
        )
    }
}

@Composable
private fun ExerciseBlockCard(
    block: ExerciseBlock,
    unit: WeightUnit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onRemoveExercise: () -> Unit,
    onUpdateSet: (Long, String?, String?, Boolean?) -> Unit,
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = block.exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = block.exercise.muscleGroup.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRemoveExercise) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SET", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
                Text("REPS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(
                    text = unit.suffix.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(88.dp))
            }

            block.sets.forEachIndexed { index, set ->
                SetRowEditor(
                    number = index + 1,
                    row = set,
                    onRepsChange = { onUpdateSet(set.key, it, null, null) },
                    onWeightChange = { onUpdateSet(set.key, null, it, null) },
                    onToggleWarmup = { onUpdateSet(set.key, null, null, !set.isWarmup) },
                    onRemove = { onRemoveSet(set.key) },
                )
            }

            TextButton(onClick = onAddSet, modifier = Modifier.padding(top = Spacing.xs)) {
                Text("+ Add set")
            }
        }
    }
}

@Composable
private fun SetRowEditor(
    number: Int,
    row: SetRow,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onToggleWarmup: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
        )
        OutlinedTextField(
            value = row.reps,
            onValueChange = onRepsChange,
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.xs),
        )
        OutlinedTextField(
            value = row.weight,
            onValueChange = onWeightChange,
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.xs),
        )
        FilterChip(
            selected = row.isWarmup,
            onClick = onToggleWarmup,
            label = { Text("W") },
        )
        TextButton(onClick = onRemove) { Text("✕") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    query: String,
    exercises: List<Exercise>,
    onQueryChange: (String) -> Unit,
    onPick: (Exercise) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))

            if (exercises.isEmpty() && query.isNotBlank()) {
                TextButton(onClick = { onCreate(query) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create \"$query\"")
                }
            }

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(exercises, key = { it.id }) { exercise ->
                    TextButton(
                        onClick = { onPick(exercise) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = listOfNotNull(
                                    exercise.muscleGroup.label,
                                    exercise.equipment,
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDatePicker(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayUtcMillis = remember {
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val pastOrToday = remember(todayUtcMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
            override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
        }
    }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = pastOrToday,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {

                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } else {
                        onDismiss()
                    }
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun PrCelebrationDialog(
    awards: List<PrAward>,
    unit: WeightUnit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (awards.size == 1) "New personal record" else "${awards.size} new records") },
        text = {
            Column {
                awards.forEach { award ->
                    Box(modifier = Modifier.padding(vertical = Spacing.xs)) {
                        Column {
                            Text(
                                text = "${award.exerciseName} · ${award.recordType.label}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = awardDetail(award, unit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Nice") } },
    )
}

private fun awardDetail(award: PrAward, unit: WeightUnit): String {
    val newValue = WeightConverter.format(award.newValue, unit)
    val previous = award.previousValue?.let { WeightConverter.format(it, unit) }
    return if (previous == null) "$newValue · first record" else "$newValue, up from $previous"
}

private fun muscleGroupGuess() = com.davidhuynh.levelup.domain.model.MuscleGroup.OTHER

@Composable
private fun RestBar(
    rest: RestState?,
    restSeconds: Int,
    onStart: (Int) -> Unit,
    onExtend: () -> Unit,
    onStop: () -> Unit,
) {
    if (rest == null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onStart(restSeconds) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Rest ${RestTimer.format(restSeconds)}")
            }

            RestTimer.PRESETS_SECONDS
                .filter { it != restSeconds }
                .forEach { preset ->
                    TextButton(onClick = { onStart(preset) }) {
                        Text(RestTimer.format(preset))
                    }
                }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (rest.isFinished) "Rest over" else "Rest ${rest.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (rest.isFinished) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Row {
                TextButton(onClick = onExtend) { Text("+30s") }
                TextButton(onClick = onStop) {
                    Text(if (rest.isFinished) "Done" else "Skip")
                }
            }
        }

        LinearProgressIndicator(
            progress = { rest.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
        )
    }
}
