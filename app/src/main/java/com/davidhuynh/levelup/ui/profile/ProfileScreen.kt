package com.davidhuynh.levelup.ui.profile

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.model.UserStats
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.StatsRepository
import com.davidhuynh.levelup.ui.common.CountBadge
import com.davidhuynh.levelup.ui.common.KeyValueRow
import com.davidhuynh.levelup.ui.common.SectionHeader
import com.davidhuynh.levelup.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
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
}

private val joinFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    pendingRequestCount: Int,
    onOpenFriends: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user
    val unit = user?.weightUnit ?: WeightUnit.LB

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
}
