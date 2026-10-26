package com.barrioahorro.app.ui.screens.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.components.OfferPriceBlock
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BadgeGreen
import com.barrioahorro.app.ui.theme.BadgeGreenDot
import com.barrioahorro.app.ui.theme.BadgeGreenText
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.BadgeYellow
import com.barrioahorro.app.ui.theme.BadgeYellowText
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.WarmGray

private data class MyOffersColors(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val secondary: Color,
    val card: Color,
    val field: Color,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val inactive: Color,
)

@Composable
private fun myOffersColors() = MyOffersColors(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimary = OnPrimary,
    secondary = Secondary,
    card = CardWhite,
    field = SurfaceFieldAlt,
    background = Cream,
    textPrimary = Ink,
    textSecondary = WarmGray,
    inactive = MutedLabel,
)

@Composable
fun MyOffersScreen(
    onCreateOffer: () -> Unit,
    onEditOffer: (Long) -> Unit,
    viewModel: MyOffersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = myOffersColors()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = {
            ToldoConFranjas(
                modifier = Modifier
                    .background(c.background),
                stripes = 18,
                height = 12.dp,
            )
        },
        floatingActionButton = {
            Button(
                onClick = onCreateOffer,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.primary,
                    contentColor = c.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .height(54.dp)
                    .padding(end = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Nueva oferta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Título + Badge de activas
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Mis ofertas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = BadgeGreen,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(BadgeGreenDot, CircleShape),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${state.activeCount} activas",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BadgeGreenText,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Las promociones que ven tus vecinos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )

                    // Filtros de pestañas: Vigentes / Finalizadas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TabPill(
                            title = "Vigentes",
                            count = state.activeCount,
                            icon = Icons.Filled.LocalOffer,
                            isSelected = state.selectedTab == OfferTabFilter.VIGENTES,
                            onClick = { viewModel.selectTab(OfferTabFilter.VIGENTES) },
                        )

                        TabPill(
                            title = "Finalizadas",
                            count = state.finishedCount,
                            icon = Icons.Outlined.History,
                            isSelected = state.selectedTab == OfferTabFilter.FINALIZADAS,
                            onClick = { viewModel.selectTab(OfferTabFilter.FINALIZADAS) },
                        )
                    }
                }
            }

            // Lista de ofertas, estado de carga o estado vacío
            when {
                state.isLoading && state.allOffers.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = c.primary)
                        }
                    }
                }
                state.filteredOffers.isEmpty() -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = c.card),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = c.inactive,
                                    modifier = Modifier.size(48.dp),
                                )
                                Text(
                                    text = if (state.selectedTab == OfferTabFilter.VIGENTES)
                                        "No tenés ofertas vigentes en este momento"
                                    else
                                        "No hay ofertas finalizadas en el historial",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = c.textPrimary,
                                )
                                Text(
                                    text = "Publicá promociones para que tus vecinos del barrio las encuentren en su feed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textSecondary,
                                )
                                if (state.selectedTab == OfferTabFilter.VIGENTES) {
                                    Button(
                                        onClick = onCreateOffer,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = c.primary,
                                            contentColor = c.onPrimary,
                                        ),
                                        modifier = Modifier.padding(top = 8.dp),
                                    ) {
                                        Text("Publicar primera oferta", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    items(state.filteredOffers, key = { it.id }) { offer ->
                        OfferItemCard(
                            offer = offer,
                            onEdit = { onEditOffer(offer.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = myOffersColors()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) c.primary else Color.Transparent,
        contentColor = if (isSelected) c.onPrimary else c.textSecondary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = if (isSelected) 6.dp else 0.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun OfferItemCard(
    offer: MyOfferItemUi,
    onEdit: () -> Unit,
) {
    val c = myOffersColors()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Toldo con franjas decorativo
            ToldoConFranjas(stripes = 20, height = 10.dp)

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row {
                    // Foto del producto
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceFieldAlt,
                        modifier = Modifier.size(92.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (offer.photoUrl != null) {
                                AsyncImage(
                                    model = offer.photoUrl.toAbsoluteUrl(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = c.inactive,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            // Corazón flotante
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(2.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Información principal
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Fila superior: Badge y Precio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Badge de oferta
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (offer.isYellowBadge) BadgeYellow else BadgeOrange,
                            ) {
                                Text(
                                    text = offer.badgeText,
                                    color = if (offer.isYellowBadge) BadgeYellowText else c.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }

                            // Precio
                            OfferPriceBlock(info = offer.priceInfo)
                        }

                        // Título del producto
                        Text(
                            text = offer.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Comercio y Rubro
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = c.textSecondary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = offer.businessName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary,
                            )
                            if (offer.categoryName.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = SurfaceFieldAlt) {
                                    Text(
                                        text = offer.categoryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.textSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }

                        // Distancia y Vigencia
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📍 ${offer.distanceText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textSecondary,
                            )
                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = c.textSecondary,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = offer.vigenciaText,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textSecondary,
                            )
                        }
                    }
                }

                // Fila inferior: Confirmación comunitaria y botón Editar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SecondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = BadgeGreenDot,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = offer.confirmedCountText,
                                style = MaterialTheme.typography.labelSmall,
                                color = BadgeGreenText,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Editar",
                            tint = c.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Editar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.primary,
                        )
                    }
                }
            }
        }
    }
}


private fun String.toAbsoluteUrl(): String =
    if (startsWith("http://") || startsWith("https://")) this else "${BASE_URL.trimEnd('/')}/${trimStart('/')}"
