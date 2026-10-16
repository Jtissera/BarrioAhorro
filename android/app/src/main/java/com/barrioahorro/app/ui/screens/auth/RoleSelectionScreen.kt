package com.barrioahorro.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.WarmGray
import com.barrioahorro.app.domain.model.UserType
import androidx.compose.foundation.layout.sizeIn

@Composable
fun RoleSelectionScreen(
    onContinue: (UserType) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var selected by rememberSaveable {
        mutableStateOf(UserType.CLIENTE)
    }

    RoleSelectionContent(
        selected = selected,
        onSelect = { selected = it },
        onContinue = { onContinue(selected) },
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
private fun RoleSelectionContent(
    selected: UserType,
    onSelect: (UserType) -> Unit,
    onContinue: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    Scaffold(
        containerColor = Cream,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = "¿Cómo querés usar\nBarrio Ahorro?",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp),
            )

            Text(
                text = "Elegí cómo querés participar. Podés cambiarlo más adelante.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                modifier = Modifier.padding(top = 8.dp),
            )

            RoleCard(
                title = "Soy vecino",
                description = "Encontrá ofertas cerca de casa, guardalas en tu lista y sumá puntos participando en la comunidad.",
                tag = "Ofertas y comercios cerca tuyo",
                icon = Icons.Outlined.ShoppingBasket,
                tagIcon = Icons.Outlined.LocalOffer,
                isSelected = selected == UserType.CLIENTE,
                onClick = { onSelect(UserType.CLIENTE) },
                modifier = Modifier.padding(top = 28.dp),
            )

            RoleCard(
                title = "Soy comercio",
                description = "Mostrale tus ofertas al barrio, fidelizá a tus vecinos y sumá clientes cada semana.",
                tag = "Tu comercio en el mapa del barrio",
                icon = Icons.Outlined.Storefront,
                tagIcon = Icons.Outlined.Place,
                isSelected = selected == UserType.COMERCIO,
                onClick = { onSelect(UserType.COMERCIO) },
                modifier = Modifier.padding(top = 14.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinue,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = MutedLabel,
                    disabledContentColor = OnPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 52.dp),
            ) {
                Text(
                    text = "Continuar →",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "¿Ya tenés cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )

                Text(
                    text = "Iniciá sesión",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    modifier = Modifier.clickable(
                        onClick = onNavigateToLogin,
                    ),
                )
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    tag: String,
    icon: ImageVector,
    tagIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = Primary
    val cardBackground = CardWhite
    val borderColor = MutedLabel
    val textPrimary = Ink
    val textSecondary = WarmGray
    val onPrimary = OnPrimary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            PrimaryContainer
        } else {
            cardBackground
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) primary else borderColor.copy(alpha = 0.5f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = primary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )

                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = primary,
                    ) {
                        Text(
                            text = "✓ Elegido",
                            color = onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp,
                            ),
                        )
                    }
                } else {
                    RadioButton(
                        selected = false,
                        onClick = null,
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )

            HorizontalDivider(
                color = borderColor.copy(alpha = 0.35f),
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = tagIcon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(14.dp),
                )

                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = primary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoleSelectionPreview() {
    RoleSelectionContent(
        selected = UserType.CLIENTE,
        onSelect = {},
        onContinue = {},
        onNavigateToLogin = {},
    )
}
