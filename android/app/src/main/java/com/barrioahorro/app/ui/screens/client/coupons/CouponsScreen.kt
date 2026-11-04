package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

private val BrandRed = Primary
private val TextMuted = WarmGray
private val Cream = Color(0xFFFDF3DC)
private val CreamSoft = Color(0xFFFFF8EC)
private val GoldPill = Color(0xFFFBE7B0)
private val Disabled = Color(0xFFE6DDD0)
private val GreenSoft = Color(0xFFDDF0E3)

private data class EarnWay(val title: String, val description: String)

private val EARN_WAYS = listOf(
    EarnWay("Colaborá en el foro vecinal", "Compartí ofertas o ayudá a vecinos contestando dudas locales."),
)

@Composable
fun CouponsScreen(
    modifier: Modifier = Modifier,
    viewModel: CouponsViewModel = hiltViewModel(),
    onOpenRedemption: (Long) -> Unit = {},
    onMyRedemptions: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3
        }.collect { nearEnd -> if (nearEnd) viewModel.loadMore() }
    }

    Column(modifier.fillMaxSize()) {

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column {
                        Text("Cupones", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Canjeá tus puntos por productos de comercios del barrio.",
                            color = TextMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
                item { PointsCard(points = state.points, onMyRedemptions = onMyRedemptions) }
                item { SearchField(value = state.query, onChange = viewModel::onQueryChange) }
                item { FiltersRow(state = state, viewModel = viewModel) }

                when {
                    state.isLoading && state.coupons.isEmpty() -> item { LoadingBlock() }

                    state.error != null && state.coupons.isEmpty() -> item {
                        ErrorBlock(message = state.error.orEmpty(), onRetry = viewModel::retry)
                    }

                    state.coupons.isEmpty() && state.hasActiveFilters -> item {
                        NoResultsBlock(onClear = viewModel::clearFilters)
                    }

                    state.coupons.isEmpty() -> {
                        item { EmptyBlock(onRefresh = viewModel::refresh) }
                        item { HowToEarnCard() }
                    }

                    else -> {
                        items(state.coupons, key = { it.id }) { coupon ->
                            CouponCard(coupon = coupon, points = state.points, onRedeem = { viewModel.requestRedeem(coupon) })
                        }
                        if (state.isLoadingMore) item { LoadingBlock() }
                    }
                }
            }

            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))

            state.confirming?.let { coupon ->
                RedeemConfirmSheet(
                    coupon = coupon,
                    points = state.points,
                    isLoading = state.isRedeeming,
                    onConfirm = { viewModel.confirmRedeem(onOpenRedemption) },
                    onDismiss = viewModel::dismissRedeem,
                )
            }
        }
    }
}

// ---- Encabezado y filtros -----------------------------------------------------------

@Composable
private fun PointsCard(points: Int?, onMyRedemptions: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(GoldPill, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (points != null) "$points puntos" else "— puntos", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Disponibles para canjear", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onMyRedemptions) {
                Text("Mis canjes ›", color = BrandRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("Buscar comercio") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Borrar búsqueda",
                    modifier = Modifier.clickable { onChange("") },
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFEADFCF),
            focusedBorderColor = BrandRed,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FiltersRow(state: CouponsUiState, viewModel: CouponsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DropdownChip(
                label = "Comercio",
                options = listOf("Todos" to { viewModel.selectCategory(null) }) +
                        state.categories.map { c -> c.name to { viewModel.selectCategory(c.id) } },
            )
            if (state.hasLocation) {
                DropdownChip(
                    label = if (state.distance == CouponDistance.ANY) "Cerca tuyo" else state.distance.label,
                    options = CouponDistance.entries.map { d -> d.label to { viewModel.selectDistance(d) } },
                )
            }
        }

        val category = state.selectedCategory
        if (category != null || state.hasActiveFilters) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (category != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BrandRed,
                        modifier = Modifier.clickable { viewModel.selectCategory(null) },
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Comercio: ${category.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::clearFilters) {
                    Text("Limpiar filtros", color = BrandRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DropdownChip(label: String, options: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White,
            modifier = Modifier
                .border(1.dp, Color(0xFFEADFCF), RoundedCornerShape(50))
                .clickable { open = true },
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() })
            }
        }
    }
}

// ---- Tarjeta de cupón ---------------------------------------------------------------

@Composable
private fun CouponCard(coupon: CouponUi, points: Int?, onRedeem: () -> Unit) {
    val canRedeem = points != null && points >= coupon.pointsCost
    val missing = points?.let { coupon.pointsCost - it }?.takeIf { it > 0 }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumb(photoUrl = coupon.photoUrl, name = coupon.productName, size = 68.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            coupon.businessName,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        coupon.distance?.let {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(50), color = CreamSoft) {
                                Text(it, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        coupon.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(50), color = GoldPill) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${coupon.pointsCost} pts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0E8DA)))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (missing != null) {
                        Text(
                            "Te faltan $missing puntos para canjear",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                        )
                        coupon.description?.let {
                            Text(it, fontSize = 11.sp, color = TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Text(
                            coupon.description ?: "Válido hasta el ${coupon.validUntil}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (coupon.description != null) {
                            Text("Válido hasta el ${coupon.validUntil}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
                if (canRedeem) {
                    Button(
                        onClick = onRedeem,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(50),
                    ) { Text("Canjear", fontWeight = FontWeight.SemiBold) }
                } else {
                    Surface(shape = RoundedCornerShape(50), color = Disabled) {
                        Text(
                            if (missing != null) "Te faltan $missing pts" else "Canjear",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---- Estados ------------------------------------------------------------------------

@Composable
private fun StatePanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    action: @Composable () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = Cream, modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFFFAD9CF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, contentDescription = null, tint = BrandRed, modifier = Modifier.size(28.dp))
            }
            if (badge != null) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(50), color = Color.White) {
                    Text(
                        badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
private fun EmptyBlock(onRefresh: () -> Unit) {
    StatePanel(
        title = "Todavía no hay cupones",
        subtitle = "Volvé pronto, los comercios del barrio los van sumando.",
    ) {
        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(50)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Actualizar lista", color = BrandRed)
        }
    }
}

@Composable
private fun NoResultsBlock(onClear: () -> Unit) {
    StatePanel(
        title = "No hay cupones con esos filtros",
        subtitle = "Probá cambiando los filtros o buscando otro comercio del barrio.",
        badge = "0 cupones",
    ) {
        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Quitar filtros", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    StatePanel(title = "No pudimos cargar los cupones", subtitle = message) {
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            shape = RoundedCornerShape(50),
        ) { Text("Reintentar") }
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandRed, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun HowToEarnCard() {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(GreenSoft, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2E7D4F), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("¿Cómo sumar más puntos?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text(
                "Mientras preparamos nuevos beneficios, podés seguir acumulando puntos para canjes futuros:",
                color = TextMuted,
                fontSize = 13.sp,
            )
            EARN_WAYS.forEach { way ->
                Surface(shape = RoundedCornerShape(12.dp), color = CreamSoft, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(way.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(way.description, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}