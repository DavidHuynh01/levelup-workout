package com.davidhuynh.levelup.ui.friends

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.data.repository.FriendRepositoryImpl
import com.davidhuynh.levelup.domain.model.Friend
import com.davidhuynh.levelup.domain.model.FriendRequest
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.util.DataResult
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.common.ErrorBanner
import com.davidhuynh.levelup.ui.common.LevelUpTopBar
import com.davidhuynh.levelup.ui.common.SectionHeader
import com.davidhuynh.levelup.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<Friend> = emptyList(),
    val incoming: List<FriendRequest> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val outgoingTargets: Set<String> = emptySet(),
    val message: String? = null,
    val isLoading: Boolean = true,
) {
    val friendIds: Set<String> get() = friends.map { it.userId }.toSet()
}

class FriendsViewModel(
    private val userId: String,
    private val friendRepository: FriendRepositoryImpl,
) : ViewModel() {

    private val local = MutableStateFlow(FriendsUiState())

    val state: StateFlow<FriendsUiState> = combine(
        friendRepository.observeFriends(userId),
        friendRepository.observeIncomingRequests(userId),
        friendRepository.observeOutgoingTargets(userId),
        local,
    ) { friends, incoming, outgoing, localState ->
        localState.copy(
            friends = friends,
            incoming = incoming,
            outgoingTargets = outgoing.toSet(),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    init {

        search("")
    }

    fun onQueryChange(value: String) {
        local.update { it.copy(searchQuery = value) }
        search(value)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            val results = friendRepository.searchUsers(userId, query)
            local.update { it.copy(searchResults = results) }
        }
    }

    fun sendRequest(toUserId: String) {
        viewModelScope.launch {
            val result = friendRepository.sendRequest(userId, toUserId)
            local.update {
                it.copy(
                    message = when (result) {
                        is DataResult.Success -> null
                        is DataResult.Failure -> result.message
                    }
                )
            }
        }
    }

    fun accept(requestId: String) {
        viewModelScope.launch { friendRepository.acceptRequest(requestId) }
    }

    fun decline(requestId: String) {
        viewModelScope.launch { friendRepository.declineRequest(requestId) }
    }

    fun removeFriend(friendUserId: String) {
        viewModelScope.launch { friendRepository.removeFriend(userId, friendUserId) }
    }

    fun clearMessage() = local.update { it.copy(message = null) }
}

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { LevelUpTopBar(title = "Friends", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
        ) {
            if (state.message != null) {
                item {
                    ErrorBanner(state.message)
                    Spacer(Modifier.height(Spacing.sm))
                }
            }

            if (state.incoming.isNotEmpty()) {
                item { SectionHeader("Requests (${state.incoming.size})") }
                items(state.incoming, key = { it.id }) { request ->
                    RequestCard(
                        request = request,
                        onAccept = { viewModel.accept(request.id) },
                        onDecline = { viewModel.decline(request.id) },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }
            }

            item { SectionHeader("Your friends (${state.friends.size})") }

            if (state.friends.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🤝",
                        title = "No friends yet",
                        body = "Add someone below and you will both show up on each other's friends leaderboard.",
                    )
                }
            }

            items(state.friends, key = { it.userId }) { friend ->
                FriendCard(friend = friend, onRemove = { viewModel.removeFriend(friend.userId) })
                Spacer(Modifier.height(Spacing.sm))
            }

            item {
                SectionHeader("Find lifters")
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Search by name or email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            items(state.searchResults, key = { "search-${it.id}" }) { user ->
                SearchResultCard(
                    user = user,
                    isFriend = user.id in state.friendIds,
                    isPending = user.id in state.outgoingTargets,
                    onAdd = { viewModel.sendRequest(user.id) },
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }
}

@Composable
private fun RequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.fromDisplayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "wants to be friends",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDecline) { Text("Decline") }
            Button(onClick = onAccept) { Text("Accept") }
        }
    }
}

@Composable
private fun FriendCard(friend: Friend, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = friend.avatarEmoji ?: "💪",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = Spacing.sm),
            )
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    user: User,
    isFriend: Boolean,
    isPending: Boolean,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = user.avatarEmoji ?: "💪",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = Spacing.sm),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (user.isDemo) "Demo lifter" else user.email,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                isFriend -> Text(
                    text = "Friends",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                isPending -> Text(
                    text = "Requested",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> OutlinedButton(onClick = onAdd) { Text("Add") }
            }
        }
    }
}
