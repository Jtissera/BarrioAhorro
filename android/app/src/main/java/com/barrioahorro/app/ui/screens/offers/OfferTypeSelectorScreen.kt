package com.barrioahorro.app.ui.screens.offers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R
import com.barrioahorro.app.ui.components.BusinessBottomBar
import com.barrioahorro.app.ui.components.BusinessTab
import com.barrioahorro.app.ui.components.ToldoConFranjas

private data class OfferTypeOption(
    val type: OfferType,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val example: String,
    val enabled: Boolean,
)

private val options = listOf(
    OfferTypeOption(OfferType.PORCENTUAL, Icons.Outlined.Percent, "Porcentual", "Un descuento en % sobre un producto", "-25%", true),
    OfferTypeOption(OfferType.DOS_POR_UNO, Icons.Outlined.Style, "2x1 o equivalente", "Llevá más unidades por el precio de menos", "2×1", true),
    OfferTypeOption(OfferType.POR_CANTIDAD, Icons.Outlined.Layers, "Por cantidad", "Descuento en la última unidad", "2da al 50%", true),
    OfferTypeOption(OfferType.POR_MAYOR, Icons.Outlined.Inventory2, "Por mayor", "Precio especial al comprar varias unidades", "Desde 6 u.", true),
)

@Composable
fun OfferTypeSelectorScreen(
    onSelect: (OfferType) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (BusinessTab) -> Unit = {},
) {
    var selected by rememberSaveable { mutableStateOf<OfferType?>(null) }

    val primary = colorResource(R.color.primary)
    val onPrimary = colorResource(R.color.on_primary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    Scaffold(
        containerColor = colorResource(R.color.splash_background),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ToldoConFranjas(
                modifier = Modifier
                    .background(colorResource(R.color.splash_background)),
            )
        },
        bottomBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = onBack,
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorResource(R.color.surface_secondary),
                            contentColor = textPrimary,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Volver", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { selected?.let(onSelect) },
                        enabled = selected != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = onPrimary,
                            disabledContainerColor = colorResource(R.color.neutral_inactive),
                            disabledContentColor = onPrimary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Text("Continuar", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
                BusinessBottomBar(
                    selectedTab = BusinessTab.OFERTAS,
                    onTabSelected = onTabSelected,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalOffer,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("PASO 1 DE 2", style = MaterialTheme.typography.labelSmall, color = textPrimary)
            }
            Text(
                "¿Qué tipo de oferta querés crear?",
                style = MaterialTheme.typography.headlineMedium,
                color = textPrimary,
            )
            Text(
                "Elegí una. Después completás los datos.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
            )
            Spacer(Modifier.height(4.dp))

            options.forEach { option ->
                OfferTypeCard(
                    option = option,
                    isSelected = selected == option.type,
                    onClick = { selected = option.type },
                )
            }
        }
    }
}

@Composable
private fun OfferTypeCard(option: OfferTypeOption, isSelected: Boolean, onClick: () -> Unit) {
    val primary = colorResource(R.color.primary)
    val primaryContainer = colorResource(R.color.primary_container)
    val onPrimary = colorResource(R.color.on_primary)
    val card = colorResource(R.color.surface_card)
    val soft = colorResource(R.color.surface_secondary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    Card(
        onClick = onClick,
        enabled = option.enabled,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryContainer else card,
            disabledContainerColor = card,
        ),
        border = if (isSelected) BorderStroke(1.dp, primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (option.enabled) 1f else 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) primary else soft,
                contentColor = if (isSelected) onPrimary else textPrimary,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(option.icon, contentDescription = null)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(option.title, style = MaterialTheme.typography.titleMedium, color = textPrimary)
                    if (isSelected) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Seleccionada",
                            tint = primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    if (option.enabled) option.description else "Próximamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) card else soft,
                contentColor = if (isSelected) primary else textPrimary,
            ) {
                Text(
                    option.example,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}