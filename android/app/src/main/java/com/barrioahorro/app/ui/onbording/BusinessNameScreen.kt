package com.barrioahorro.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.WarmGray
import com.barrioahorro.app.ui.components.onboarding.OnboardingPrimaryButton
import com.barrioahorro.app.ui.components.onboarding.OnboardingTopBar
import com.barrioahorro.app.ui.components.onboarding.onboardingFieldColors

private const val CURRENT_STEP = 1
private const val MIN_NAME_LENGTH = 2
private const val MAX_NAME_LENGTH = 80
private const val MAX_DESCRIPTION_LENGTH = 200

@Composable
fun BusinessNameScreen(
    onContinue: (businessName: String, descripcion: String) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialDescription: String = "",
    onBack: (() -> Unit)? = null, // primer paso: por defecto sin flecha
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }

    val textPrimary = Ink
    val textSecondary = WarmGray
    val isNameValid = name.trim().length >= MIN_NAME_LENGTH

    Scaffold(
        containerColor = Cream,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OnboardingTopBar(currentStep = CURRENT_STEP, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "¿Cómo se llama tu comercio?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    text = "Así te van a encontrar los vecinos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )

                // Nombre
                Text(
                    text = "Nombre del comercio",
                    style = MaterialTheme.typography.labelLarge,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    placeholder = { Text("Ej: Almacén La Esquina") },
                    trailingIcon = {
                        if (isNameValid) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Secondary,
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = onboardingFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                NameTipCard(modifier = Modifier.padding(top = 12.dp))

                // Descripción
                Text(
                    text = "Descripción del comercio",
                    style = MaterialTheme.typography.labelLarge,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp),
                ) {
                    Text(
                        text = "Contale al barrio de qué se trata tu local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${description.length}/$MAX_DESCRIPTION_LENGTH",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(MAX_DESCRIPTION_LENGTH) },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = onboardingFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                )

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Un toque cercano genera más confianza entre los vecinos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }
            }

            OnboardingPrimaryButton(
                text = "Siguiente",
                onClick = { onContinue(name.trim(), description.trim()) },
                enabled = isNameValid,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun NameTipCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Usá el nombre que aparece en tu cartel.",
                style = MaterialTheme.typography.bodySmall,
                color = Ink,
            )
        }
    }
}