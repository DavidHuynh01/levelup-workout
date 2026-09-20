package com.davidhuynh.levelup.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.LeaderboardEntry
import com.davidhuynh.levelup.domain.model.LeaderboardMetric
import com.davidhuynh.levelup.domain.model.LeaderboardScope
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.LeaderboardRepository
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.common.LoadingScreen
import com.davidhuynh.levelup.ui.theme.AccentLime
import com.davidhuynh.levelup.ui.theme.Spacing
import com.davidhuynh.levelup.ui.theme.StreakOrange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class LeaderboardUiState(
    val scope: LeaderboardScope = LeaderboardScope.GLOBAL,
    val metric: LeaderboardMetric = LeaderboardMetric.TOTAL_VOLUME,
    val entries: List<LeaderboardEntry> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isLoading: Boolean = true,
) {
    val currentUserEntry: LeaderboardEntry? get() = entries.firstOrNull { it.isCurrentUser }

    /** A friends board holding only you means there is nobody to compete with yet. */
    val friendsBoardIsEmpty: Boolean
        get() = scope == LeaderboardScope.FRIENDS && entries.size <= 1
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModel(
    private val userId: String,
    leaderboardRepository: LeaderboardRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val scope = MutableStateFlow(LeaderboardScope.GLOBAL)
    private val metric = MutableStateFlow(LeaderboardMetric.TOTAL_VOLUME)

    val state: StateFlow<LeaderboardUiState> = combine(scope, metric) { s, m -> s to m }
        .flatMapLatest { (currentScope, currentMetric) ->
            val entries = when (currentScope) {
                LeaderboardScope.GLOBAL ->
                    leaderboardRepository.observeGlobal(userId, currentMetric, limit = 50)

                LeaderboardScope.FRIENDS ->
                    leaderboardRepository.observeFriends(userId, currentMetric)
            }
            combine(entries, authRepository.observeUser(userId)) { list, user ->
                LeaderboardUiState(
                    scope = currentScope,
                    metric = currentMetric,
                    entries = list,
                    weightUnit = user?.weightUnit ?: WeightUnit.LB,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaderboardUiState())

    fun setScope(value: LeaderboardScope) { scope.value = value }

    fun setMetric(value: LeaderboardMetric) { metric.value = value }
}

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onFindFriends: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Leaderboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.lg, bottom = Spacing.md),
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
        ) {
            LeaderboardScope.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = state.scope == option,
                    onClick = { viewModel.setScope(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, LeaderboardScope.entries.size),
                ) { Text(option.label) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            LeaderboardMetric.entries.forEach { option ->
                FilterChip(
                    selected = state.metric == option,
                    onClick = { viewModel.setMetric(option) },
                    label = { Text(option.label) },
                )
            }
        }

        // The scope and metric controls stay put while the list below them swaps, so
        // switching boards does not make the whole screen jump.
        when {
            state.isLoading -> LoadingScreen()

            state.friendsBoardIsEmpty -> EmptyState(
                emoji = "👥",
                title = "No friends yet",
                body = "Add a few lifters and this board fills up with people you actually know.",
                action = { TextButton(onClick = onFindFriends) { Text("Find friends") } },
            )

            else -> LazyColumn(modifier = Modifier.padding(horizontal = Spacing.md)) {
                items(state.entries, key = { it.userId }) { entry ->
                    LeaderboardRowCard(
                        entry = entry,
                        metric = state.metric,
                        unit = state.weightUnit,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }
                item { Spacer(Modifier.height(Spacing.xl)) }
            }
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    entry: LeaderboardEntry,
    metric: LeaderboardMetric,
    unit: WeightUnit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Your own row is tinted so it is findable without scrolling for your name.
            containerColor = if (entry.isCurrentUser) {
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
        ) {
            Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = medalFor(entry.rank),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = entry.avatarEmoji ?: "💪",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = Spacing.sm),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.isCurrentUser) "${entry.displayName} (you)" else entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${entry.totalWorkouts} workouts · ${entry.prCount} PRs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = metricValue(entry, metric, unit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (metric == LeaderboardMetric.CURRENT_STREAK) StreakOrange else AccentLime,
            )
        }
    }
}

private fun medalFor(rank: Int): String = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> rank.toString()
}

private fun metricValue(
    entry: LeaderboardEntry,
    metric: LeaderboardMetric,
    unit: WeightUnit,
): String = when (metric) {
    LeaderboardMetric.TOTAL_VOLUME -> WeightConverter.formatVolume(entry.totalVolumeKg, unit)
    LeaderboardMetric.CURRENT_STREAK -> "${entry.currentStreakDays}d"
    LeaderboardMetric.PR_COUNT -> "${entry.prCount}"
}
