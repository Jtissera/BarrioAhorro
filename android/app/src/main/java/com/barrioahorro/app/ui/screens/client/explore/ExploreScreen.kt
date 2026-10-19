package com.barrioahorro.app.ui.screens.client.explore

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BadgeYellow
import com.barrioahorro.app.ui.theme.BadgeYellowText
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.FieldBackground
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.WarmGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onEditLocation: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFilters by remember { mutableStateOf(false) }

    // El saldo puede cambiar al canjear: se refresca cada vez que se vuelve a la pantalla.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshSummary() }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding(),
    ) {
        ToldoConFranjas(stripes = 18, height = 12.dp, altColor = Cream)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(state, onEditLocation)
            SearchRow(
                query = state.query,
                filterCount = state.activeFilterCount,
                onQueryChange = viewModel::onQueryChange,
                onOpenFilters = { showFilters = true },
            )
            CategoryChips(
                categories = state.categories.map { it.id to it.name },
                selectedIds = state.selectedCategoryIds,
                onToggle = viewModel::toggleCategory,
                onClear = viewModel::clearCategories,
            )
            ModeToggle(state.mode, viewModel::onModeChange)
        }

        when (state.mode) {
            ExploreMode.LIST -> OffersList(
                state = state,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::retry,
                onLoadMore = viewModel::loadMore,
                onToggleFavorite = viewModel::toggleFavorite,
                onSortSelected = viewModel::onSortSelected,
                onClearFilters = viewModel::clearAllFilters,
                onEditLocation = onEditLocation,
                modifier = Modifier.weight(1f),
            )

            ExploreMode.MAP -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "El mapa de comercios llega en la próxima actualización. Mientras tanto, usá la lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showFilters) {
        ExploreFiltersSheet(
            categories = state.categories,
            initialCategoryIds = state.selectedCategoryIds,
            initialTypes = state.selectedTypes,
            initialDistance = state.distance,
            hasLocation = state.hasLocation,
            onApply = { categories, types, distance ->
                viewModel.applyFilters(categories, types, distance)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

// ---- Cabecera, búsqueda, chips ---------------------------------------------------

@Composable
private fun Header(state: ExploreUiState, onEditLocation: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "CERCA TUYO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WarmGray,
            )
            Row(
                modifier = Modifier.clickable(onClick = onEditLocation),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = state.location?.label ?: "Activá tu ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Cambiar ubicación", tint = Ink)
            }
        }
        state.points?.let { points ->
            Surface(shape = RoundedCornerShape(50), color = BadgeYellow) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Tertiary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$points pts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BadgeYellowText)
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    query: String,
    filterCount: Int,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Buscá productos o comercios", color = WarmGray) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = WarmGray) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda", tint = WarmGray)
                    }
                }
                Box {
                    IconButton(onClick = onOpenFilters) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Filtros", tint = Primary)
                    }
                    if (filterCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Primary,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(16.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$filterCount", style = MaterialTheme.typography.labelSmall, color = OnPrimary)
                            }
                        }
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldBackground,
            unfocusedContainerColor = FieldBackground,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoryChips(
    categories: List<Pair<Int, String>>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onClear: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SelectablePill("Todos", selected = selectedIds.isEmpty(), onClick = onClear) }
        items(categories, key = { it.first }) { (id, name) ->
            SelectablePill(name, selected = id in selectedIds, onClick = { onToggle(id) })
        }
    }
}

@Composable
private fun ModeToggle(mode: ExploreMode, onChange: (ExploreMode) -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(50), color = SurfaceFieldAlt) {
            Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModeOption("Mapa", mode == ExploreMode.MAP) { onChange(ExploreMode.MAP) }
                ModeOption("Lista", mode == ExploreMode.LIST) { onChange(ExploreMode.LIST) }
            }
        }
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CardWhite else SurfaceFieldAlt,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Primary else WarmGray,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
        )
    }
}

// ---- Lista -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OffersList(
    state: ExploreUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onSortSelected: (ExploreSort) -> Unit,
    onClearFilters: () -> Unit,
    onEditLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.offers.size) { if (nearEnd) onLoadMore() }

    Box(modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)

            state.error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(state.error, textAlign = TextAlign.Center, color = Ink)
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                ) { Text("Reintentar") }
            }

            else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!state.hasLocation) {
                        item { NoLocationBanner(onEditLocation) }
                    }

                    item {
                        ListHeader(
                            total = state.total,
                            hasLocation = state.hasLocation,
                            sort = state.effectiveSort,
                            options = state.availableSorts,
                            onSortSelected = onSortSelected,
                        )
                    }

                    if (state.offers.isEmpty()) {
                        item { EmptyState(state.hasActiveFilters, onClearFilters) }
                    } else {
                        items(state.offers, key = { it.id }) { offer ->
                            ExploreOfferCard(item = offer, onToggleFavorite = { onToggleFavorite(offer.id) })
                        }
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    total: Long,
    hasLocation: Boolean,
    sort: ExploreSort,
    options: List<ExploreSort>,
    onSortSelected: (ExploreSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val countText = when {
        total == 1L && hasLocation -> "1 oferta cerca tuyo"
        hasLocation -> "$total ofertas cerca tuyo"
        total == 1L -> "1 oferta"
        else -> "$total ofertas"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(countText, style = MaterialTheme.typography.labelMedium, color = WarmGray, modifier = Modifier.weight(1f))
        Box {
            Row(modifier = Modifier.clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
                Text("Ordenar: ", style = MaterialTheme.typography.labelMedium, color = WarmGray)
                Text(sort.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onSortSelected(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoLocationBanner(onEditLocation: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = BadgeYellow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Activá tu ubicación para ver las distancias y las ofertas más cercanas.",
                style = MaterialTheme.typography.bodySmall,
                color = BadgeYellowText,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEditLocation) {
                Text("Activar", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyState(hasActiveFilters: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (hasActiveFilters) "No hay ofertas con esos filtros" else "Todavía no hay ofertas cerca",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (hasActiveFilters) "Probá con otros rubros o ampliá la distancia." else "Volvé a mirar más tarde: los comercios publican nuevas ofertas todos los días.",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray,
            textAlign = TextAlign.Center,
        )
        if (hasActiveFilters) {
            Button(
                onClick = onClearFilters,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) { Text("Limpiar filtros", fontWeight = FontWeight.Bold) }
        }
    }
}