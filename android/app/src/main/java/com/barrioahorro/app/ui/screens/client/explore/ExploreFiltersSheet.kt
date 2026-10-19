package com.barrioahorro.app.ui.screens.client.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.data.explore.ExploreCategory
import com.barrioahorro.app.ui.theme.BorderGray
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.WarmGray

/** Chip reutilizado por la fila de rubros y por el sheet de filtros. */
@Composable
fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else CardWhite,
        contentColor = if (selected) OnPrimary else Ink,
        border = if (selected) null else BorderStroke(1.dp, BorderGray),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreFiltersSheet(
    categories: List<ExploreCategory>,
    initialCategoryIds: Set<Int>,
    initialTypes: Set<ExploreOfferType>,
    initialDistance: DistanceFilter,
    hasLocation: Boolean,
    onApply: (Set<Int>, Set<ExploreOfferType>, DistanceFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryIds by remember { mutableStateOf(initialCategoryIds) }
    var types by remember { mutableStateOf(initialTypes) }
    var distance by remember { mutableStateOf(initialDistance) }

    val count = categoryIds.size + types.size + if (hasLocation && distance != DEFAULT_DISTANCE) 1 else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Filtros", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = InkBlack)
                    Text("Personalizá las ofertas del barrio", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Ink)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Section("Rubro", "Seleccioná uno o más rubros del barrio") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            SelectablePill(
                                label = category.name,
                                selected = category.id in categoryIds,
                                onClick = {
                                    categoryIds = if (category.id in categoryIds) categoryIds - category.id else categoryIds + category.id
                                },
                            )
                        }
                    }
                }

                Section(
                    title = "Distancia máxima",
                    subtitle = if (hasLocation) null else "Activá tu ubicación para filtrar por distancia.",
                ) {
                    Surface(shape = RoundedCornerShape(50), color = SurfaceFieldAlt, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DistanceFilter.entries.forEach { option ->
                                val selected = distance == option
                                Surface(
                                    onClick = { distance = option },
                                    enabled = hasLocation,
                                    shape = RoundedCornerShape(50),
                                    color = if (selected && hasLocation) Primary else SurfaceFieldAlt,
                                    contentColor = if (selected && hasLocation) OnPrimary else if (hasLocation) Ink else MutedLabel,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Section("Tipo de oferta", "Filtrá por el formato de ahorro que buscás") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExploreOfferType.entries.forEach { type ->
                            SelectablePill(
                                label = type.label,
                                selected = type in types,
                                onClick = { types = if (type in types) types - type else types + type },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = {
                    categoryIds = emptySet()
                    types = emptySet()
                    distance = DEFAULT_DISTANCE
                }) {
                    Text("Limpiar", color = WarmGray, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onApply(categoryIds, types, distance) },
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text(
                        text = if (count > 0) "Aplicar filtros ($count)" else "Aplicar filtros",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WarmGray) }
        }
        content()
    }
}