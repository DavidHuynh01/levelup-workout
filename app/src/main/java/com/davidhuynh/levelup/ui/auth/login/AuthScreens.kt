package com.davidhuynh.levelup.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidhuynh.levelup.ui.common.ErrorBanner
import com.davidhuynh.levelup.ui.common.LevelUpTextField
import com.davidhuynh.levelup.ui.common.PasswordField
import com.davidhuynh.levelup.ui.theme.Spacing

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToSignUp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🏋️",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Level Up",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Log your lifts. Climb the board.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xl))

        ErrorBanner(state.formError)
        if (state.formError != null) Spacer(Modifier.height(Spacing.md))

        LevelUpTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(Spacing.md))

        PasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            error = state.passwordError,
            imeAction = ImeAction.Done,
        )

        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Sign in")
            }
        }

        TextButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
        ) {
            Text("New here? Create an account")
        }
    }
}

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "Create your account", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Your workouts stay on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )

        Spacer(Modifier.height(Spacing.lg))

        ErrorBanner(state.formError)
        if (state.formError != null) Spacer(Modifier.height(Spacing.md))

        LevelUpTextField(
            value = state.displayName,
            onValueChange = viewModel::onNameChange,
            label = "Display name",
            error = state.nameError,
            supportingText = "Shown on the leaderboard",
        )
        Spacer(Modifier.height(Spacing.md))

        LevelUpTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(Spacing.md))

        PasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            error = state.passwordError,
            strength = state.passwordStrength,
        )
        Spacer(Modifier.height(Spacing.md))

        PasswordField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmChange,
            label = "Confirm password",
            error = state.confirmError,
            imeAction = ImeAction.Done,
        )

        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Create account")
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
        ) {
            Text("Already have an account? Sign in")
        }
    }
}
