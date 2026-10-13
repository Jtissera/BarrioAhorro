package com.barrioahorro.app.ui.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R


const val ONBOARDING_TOTAL_STEPS = 5

@Composable
fun OnboardingTopBar(
    currentStep: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    totalSteps: Int = ONBOARDING_TOTAL_STEPS,
) {
    val filled = colorResource(R.color.primary)
    val empty = colorResource(R.color.neutral_inactive).copy(alpha = 0.35f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = colorResource(R.color.text_primary),
                )
            }
        } else {
            // Primer paso: sin flecha, pero la barra queda alineada con las demás pantallas.
            Spacer(modifier = Modifier.width(48.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (index < currentStep) filled else empty),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

/** Botón principal "Siguiente →" de todo el onboarding. */
@Composable
fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val onPrimary = colorResource(R.color.on_primary)

    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.primary),
            contentColor = onPrimary,
            disabledContainerColor = colorResource(R.color.neutral_inactive),
            disabledContentColor = onPrimary,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
fun onboardingFieldColors(): TextFieldColors {
    val card = colorResource(R.color.surface_card)
    val primary = colorResource(R.color.primary)
    val inactive = colorResource(R.color.neutral_inactive)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = card,
        unfocusedContainerColor = card,
        disabledContainerColor = card,
        focusedBorderColor = primary,
        unfocusedBorderColor = inactive.copy(alpha = 0.5f),
        disabledBorderColor = inactive.copy(alpha = 0.3f),
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        disabledTextColor = textSecondary,
        cursorColor = primary,
        focusedLabelColor = primary,
        unfocusedLabelColor = textSecondary,
        disabledLabelColor = textSecondary,
    )
}