package com.barrioahorro.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.components.feedback.ErrorCard
import com.barrioahorro.app.ui.components.inputs.EditorialTextField

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (UserType) -> Unit,
    onForgotPassword: () -> Unit = {}, // TODO: todavía no existe la pantalla de recuperar contraseña
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome -> onLoginSuccess(event.userType)
            }
        }
    }

    LoginContent(
        state = state,
        onBack = onBack,
        onEmailChanged = viewModel::onLoginEmailChanged,
        onPasswordChanged = viewModel::onLoginPasswordChanged,
        onTogglePasswordVisibility = viewModel::onLoginPasswordVisibilityToggled,
        onForgotPassword = onForgotPassword,
        onSubmit = viewModel::submitLogin,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onBack: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPassword: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val background = Cream
    val textPrimary = Ink
    val textSecondary = WarmGray
    val primary = Primary
    val onPrimary = OnPrimary
    val inactive = MutedLabel

    Scaffold(containerColor = background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Barra superior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = textPrimary,
                    )
                }
                Text(
                    text = "Iniciar sesión",
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = "¡Qué bueno verte de nuevo!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimary,
                )
                Text(
                    text = "Ingresá tus datos para ver las ofertas de hoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )

                EditorialTextField(
                    label = "Email",
                    value = state.email,
                    onValueChange = onEmailChanged,
                    placeholder = "tu@email.com",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.padding(top = 24.dp),
                )

                EditorialTextField(
                    label = "Contraseña",
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    placeholder = "Tu contraseña",
                    isPassword = true,
                    isPasswordVisible = state.isPasswordVisible,
                    onToggleVisibility = onTogglePasswordVisibility,
                    modifier = Modifier.padding(top = 18.dp),
                )

                Text(
                    text = "¿Olvidaste tu contraseña? →",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable(onClick = onForgotPassword),
                )

                ErrorCard(
                    title = state.errorMessage,
                    modifier = Modifier.padding(top = 10.dp),
                )

                Button(
                    onClick = onSubmit,
                    enabled = state.isSubmitEnabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = onPrimary,
                        disabledContainerColor = inactive,
                        disabledContentColor = onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = "Ingresar →",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                // Divisor con punto central
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(
                        color = inactive.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(6.dp)
                            .background(inactive, CircleShape),
                    )
                    HorizontalDivider(
                        color = inactive.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                ) {
                    Text(
                        text = "¿No tenés cuenta todavía?",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                    Text(
                        text = "Registrate acá →",
                        style = MaterialTheme.typography.labelLarge,
                        color = primary,
                        modifier = Modifier.clickable(onClick = onNavigateToRegister),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginContent(
        state = LoginUiState(),
        onBack = {},
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onForgotPassword = {},
        onSubmit = {},
        onNavigateToRegister = {},
    )
}