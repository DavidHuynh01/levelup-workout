package com.davidhuynh.levelup.ui.profile

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.data.export.WorkoutExporter
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.StatsRepository
import com.davidhuynh.levelup.domain.util.DataResult
import com.davidhuynh.levelup.ui.common.CountBadge
import com.davidhuynh.levelup.ui.common.KeyValueRow
import com.davidhuynh.levelup.ui.common.LoadingScreen
import com.davidhuynh.levelup.ui.common.SectionHeader
import com.davidhuynh.levelup.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ProfileUiState(
    val user: User? = null,
    val stats: UserStats = UserStats(userId = ""),
    val isLoading: Boolean = true,
)

class ProfileViewModel(
    private val userId: String,
    private val authRepository: AuthRepository,
    statsRepository: StatsRepository,
    private val exporter: WorkoutExporter,
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(
        authRepository.observeUser(userId),
        statsRepository.observeStats(userId),
    ) { user, stats ->
        ProfileUiState(user = user, stats = stats, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /** Display only: stored weights are always kilograms, so nothing is converted on disk. */
    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { authRepository.updateWeightUnit(userId, unit) }
    }

    private val _editError = MutableStateFlow<String?>(null)
    val editError: StateFlow<String?> = _editError.asStateFlow()

    fun saveProfile(displayName: String, avatarEmoji: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.updateProfile(userId, displayName, avatarEmoji)) {
                is DataResult.Success -> {
                    _editError.value = null
                    onDone()
                }
                is DataResult.Failure -> _editError.value = result.message
            }
        }
    }

    fun clearEditError() { _editError.value = null }

    private val _export = MutableStateFlow<ExportState>(ExportState.Idle)
    val export: StateFlow<ExportState> = _export.asStateFlow()

    fun exportCsv() {
        if (_export.value is ExportState.Working) return
        _export.value = ExportState.Working
        viewModelScope.launch {
            _export.value = when (val result = exporter.exportCsv(userId)) {
                is DataResult.Success -> ExportState.Ready(result.value)
                is DataResult.Failure -> ExportState.Failed(result.message)
            }
        }
    }

    /** Called once the share sheet has been launched, so it does not fire again. */
    fun clearExport() { _export.value = ExportState.Idle }

    fun shareIntent(export: WorkoutExporter.Export) = exporter.shareIntent(export)
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Working : ExportState
    data class Ready(val export: WorkoutExporter.Export) : ExportState
    data class Failed(val message: String) : ExportState
}

/**
 * Emoji avatars rather than uploaded photos: no storage permission, no file copying, no
 * broken image when a content URI goes stale — which is exactly what the Foodie app's
 * profile pictures ran into.
 */
private val AVATAR_CHOICES = listOf(
    "💪", "🔥", "⚡", "🏋", "🦍",
    "🐻", "🦅", "🚀", "🌟", "🎯",
    "🦋", "🐯", "🦄", "🌈", "👑",
)

private val joinFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    pendingRequestCount: Int,
    onOpenFriends: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editError by viewModel.editError.collectAsStateWithLifecycle()
    val exportState by viewModel.export.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val user = state.user
    val unit = user?.weightUnit ?: WeightUnit.LB
    var isEditing by remember { mutableStateOf(false) }

    if (state.isLoading) {
        LoadingScreen()
        return
    }

    LaunchedEffect(exportState) {
        val ready = exportState as? ExportState.Ready ?: return@LaunchedEffect
        // Sharing is a side effect, so it belongs in an effect rather than in composition.
        context.startActivity(
            Intent.createChooser(viewModel.shareIntent(ready.export), "Share your workouts")
        )
        viewModel.clearExport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
    ) {
        Text(
            text = user?.avatarEmoji ?: "💪",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.lg),
        )
        Text(
            text = user?.displayName.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = user?.email.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(
            onClick = { isEditing = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = Spacing.xs),
        ) {
            Text("Edit profile")
        }

        SectionHeader("Friends")

        Card(
            onClick = onOpenFriends,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Friends and requests", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (pendingRequestCount > 0) {
                            "$pendingRequestCount waiting on you"
                        } else {
                            "Add lifters to compete with"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CountBadge(pendingRequestCount)
            }
        }

        SectionHeader("Lifetime")

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                KeyValueRow("Total volume", WeightConverter.formatVolume(state.stats.totalVolumeKg, unit))
                KeyValueRow("Workouts", state.stats.totalWorkouts.toString())
                KeyValueRow("Working sets", state.stats.totalSets.toString())
                KeyValueRow("Total reps", state.stats.totalReps.toString())
                KeyValueRow("Standing records", state.stats.prCount.toString())
                KeyValueRow("Longest streak", "${state.stats.longestStreakDays} days")
                user?.let {
                    KeyValueRow(
                        "Member since",
                        Instant.ofEpochMilli(it.createdAt)
                            .atZone(ZoneId.systemDefault())
                            .format(joinFormat),
                    )
                }
            }
        }

        SectionHeader("Your data")

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text("Export as CSV", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "One row per set, ready for a spreadsheet.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                (exportState as? ExportState.Failed)?.let { failed ->
                    Text(
                        text = failed.message,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }

                OutlinedButton(
                    onClick = viewModel::exportCsv,
                    enabled = exportState !is ExportState.Working,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                ) {
                    Text(if (exportState is ExportState.Working) "Preparing…" else "Export and share")
                }
            }
        }

        SectionHeader("Units")

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            WeightUnit.entries.forEach { option ->
                FilterChip(
                    selected = unit == option,
                    onClick = { viewModel.setWeightUnit(option) },
                    label = { Text(option.label) },
                )
            }
        }
        Text(
            text = "Weights are stored in kilograms and converted for display, so switching back and forth never changes your history.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        Spacer(Modifier.height(Spacing.lg))

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out", color = MaterialTheme.colorScheme.error)
        }

        Text(
            text = "Signing out keeps your workouts on this device — signing back in restores them.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        Spacer(Modifier.height(Spacing.xl))
    }

    if (isEditing && user != null) {
        EditProfileDialog(
            initialName = user.displayName,
            initialEmoji = user.avatarEmoji,
            error = editError,
            onDismiss = {
                isEditing = false
                viewModel.clearEditError()
            },
            onSave = { name, emoji ->
                viewModel.saveProfile(name, emoji) { isEditing = false }
            },
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialEmoji: String?,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji ?: AVATAR_CHOICES.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }

                Text(
                    text = "Avatar",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(160.dp),
                ) {
                    items(AVATAR_CHOICES) { choice ->
                        val selected = choice == emoji
                        Surface(
                            onClick = { emoji = choice },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.padding(Spacing.xs),
                        ) {
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(Spacing.sm),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, emoji) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
