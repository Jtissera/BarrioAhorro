package com.barrioahorro.app.ui.onbording

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R
import com.barrioahorro.app.ui.components.feedback.ErrorCard
import com.barrioahorro.app.ui.components.onboarding.OnboardingPrimaryButton
import com.barrioahorro.app.ui.components.onboarding.OnboardingTopBar

private const val COMMITMENT_STEP = 5

private val commitmentRules = listOf(
    "Cada 15 días ratificás tu aporte.",
    "Sumás productos que tenés en stock para regalar.",
    "Nosotros los ofrecemos a los vecinos a cambio de puntos.",
    "El vecino te muestra su código QR y vos le entregás el producto.",
)

@Composable
fun BusinessCommitmentScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onAccept: () -> Unit,
) {
    val primary = colorResource(R.color.primary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    var accepted by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = colorResource(R.color.splash_background)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OnboardingTopBar(currentStep = COMMITMENT_STEP, onBack = if (isSubmitting) null else onBack)
            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Redeem,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPROMISO COMUNITARIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tu aporte a la cuponera",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Es parte de ser un comercio de Barrio Ahorro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                )
                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colorResource(R.color.surface_card),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        commitmentRules.forEachIndexed { index, rule ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape).background(primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorResource(R.color.on_primary),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = rule,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textPrimary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(R.color.tertiary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Si no ratificás a tiempo, se restringen algunas funciones hasta que lo hagas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isSubmitting) { accepted = !accepted },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = primary,
                            checkmarkColor = colorResource(R.color.on_primary),
                            uncheckedColor = colorResource(R.color.neutral_inactive),
                        ),
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = "Entiendo y acepto el compromiso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textPrimary,
                    )
                }

                ErrorCard(title = errorMessage)
            }

            Spacer(modifier = Modifier.height(16.dp))
            OnboardingPrimaryButton(
                text = "Aceptar y cargar mis productos →",
                onClick = onAccept,
                enabled = accepted && !isSubmitting,
                isLoading = isSubmitting,
            )
        }
    }
}