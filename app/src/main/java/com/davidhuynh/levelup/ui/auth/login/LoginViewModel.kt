package com.davidhuynh.levelup.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.domain.logic.PasswordPolicy
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
) {
    val canSubmit: Boolean get() = !isSubmitting && email.isNotBlank() && password.isNotBlank()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    /**
     * On success nothing is navigated from here: saving the session makes AuthViewModel
     * emit Authenticated, and the graph follows. One source of truth for "signed in".
     */
    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = authRepository.signIn(current.email, current.password)) {
                is DataResult.Success -> _state.update { it.copy(isSubmitting = false) }
                is DataResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, formError = result.message)
                }
            }
        }
    }
}

data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
) {
    val passwordStrength: Int get() = PasswordPolicy.strength(password)

    val canSubmit: Boolean
        get() = !isSubmitting && displayName.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && confirmPassword.isNotBlank()
}

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update {
        it.copy(displayName = value, nameError = null, formError = null)
    }

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = null, formError = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun onConfirmChange(value: String) = _state.update {
        it.copy(confirmPassword = value, confirmError = null, formError = null)
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.clearedErrors().copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.signUp(
                email = current.email,
                displayName = current.displayName,
                password = current.password,
                confirmPassword = current.confirmPassword,
            )
            when (result) {
                is DataResult.Success -> _state.update { it.copy(isSubmitting = false) }
                is DataResult.Failure -> _state.update { state ->
                    // The failure names the field it belongs to, so the message lands under
                    // the input that caused it instead of in a generic banner.
                    when (result.field) {
                        "displayName" -> state.copy(isSubmitting = false, nameError = result.message)
                        "email" -> state.copy(isSubmitting = false, emailError = result.message)
                        "password" -> state.copy(isSubmitting = false, passwordError = result.message)
                        "confirmPassword" -> state.copy(isSubmitting = false, confirmError = result.message)
                        else -> state.copy(isSubmitting = false, formError = result.message)
                    }
                }
            }
        }
    }

    private fun SignUpUiState.clearedErrors() = copy(
        formError = null,
        nameError = null,
        emailError = null,
        passwordError = null,
        confirmError = null,
    )
}
