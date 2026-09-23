package com.davidhuynh.levelup.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.domain.model.AuthState
import com.davidhuynh.levelup.domain.model.User
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val currentUser: StateFlow<User?> = state
        .flatMapLatest { authState ->
            when (authState) {
                is AuthState.Authenticated -> authRepository.observeUser(authState.userId)
                else -> flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weightUnit: StateFlow<WeightUnit> = currentUser
        .map { it?.weightUnit ?: WeightUnit.LB }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.LB)

    init {
        viewModelScope.launch {
            authRepository.session.collect { session ->
                _state.value = if (session == null) {
                    AuthState.Unauthenticated
                } else {

                    authRepository.refreshSession()
                    AuthState.Authenticated(session.userId)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun setWeightUnit(unit: WeightUnit) {
        val userId = (state.value as? AuthState.Authenticated)?.userId ?: return
        viewModelScope.launch { authRepository.updateWeightUnit(userId, unit) }
    }
}
