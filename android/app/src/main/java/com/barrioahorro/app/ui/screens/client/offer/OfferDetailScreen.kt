package com.barrioahorro.app.ui.screens.client.offer

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.barrioahorro.app.ui.components.formatMoney
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.screens.client.ClientBottomBar
import com.barrioahorro.app.ui.screens.client.ClientTab
import com.barrioahorro.app.ui.theme.BadgeGreenSoft
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ForestGreen
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.WarmGray

private val COVER_HEIGHT = 280.dp
private val SHEET_OVERLAP = 28.dp
private val SHEET_SHAPE = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
fun OfferDetailScreen(
    onBack: () -> Unit,
    onOpenBusiness: (Long) -> Unit,
    onTabSelected: (ClientTab) -> Unit,
    viewModel: OfferDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        containerColor = CardWhite,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { ClientBottomBar(selectedTab = ClientTab.EXPLORAR, onTabSelected = onTabSelected) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val offer = state.offer
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)

                offer == null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ErrorHint(text = state.error ?: "No pudimos cargar la oferta")
                    PrimaryPillButton(text = "Reintentar", onClick = viewModel::load, height = 48.dp)
                }

                else -> OfferContent(
                    offer = offer,
                    onOpenBusiness = { onOpenBusiness(offer.businessId) },
                    onConfirm = {
                        Toast.makeText(
                            context,
                            "Próximamente vas a poder confirmar que la oferta sigue vigente",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Ink, onBack)
                state.offer?.let { offer ->
                    RoundIconButton(
                        icon = if (offer.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        description = if (offer.favorite) "Quitar de favoritos" else "Guardar en favoritos",
                        tint = if (offer.favorite) Primary else Ink,
                        onClick = viewModel::toggleFavorite,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferContent(offer: OfferDetailUi, onOpenBusiness: () -> Unit, onConfirm: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth()) {
            Cover(offer)
            Surface(
                shape = SHEET_SHAPE,
                color = CardWhite,
                modifier = Modifier.fillMaxWidth().padding(top = COVER_HEIGHT - SHEET_OVERLAP),
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (!offer.live) EndedBanner()
                    ChipsRow(offer)
                    Text(
                        text = offer.productName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )
                    PriceBlock(offer)
                    InfoCard(offer)
                    BusinessSection(offer, onOpenBusiness)
                    if (offer.live) {
                        ConfirmationsCard(offer, onConfirm)
                    } else {
                        EndedFooter(offer, onOpenBusiness)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cover(offer: OfferDetailUi) {
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    Box(Modifier.fillMaxWidth().height(COVER_HEIGHT).background(SurfaceFieldAlt)) {
        if (offer.photoUrl != null) {
            AsyncImage(
                model = offer.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (offer.live) null else gray,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MutedLabel,
                modifier = Modifier.align(Alignment.Center).size(56.dp),
            )
        }
        if (!offer.live) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CardWhite,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = SHEET_OVERLAP + 12.dp),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Storefront, null, tint = Secondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(offer.businessName, style = MaterialTheme.typography.labelSmall, color = Ink)
                }
            }
        }
    }
}

@Composable
private fun EndedBanner() {
    Surface(shape = RoundedCornerShape(16.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = WarmGray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Esta oferta ya no está vigente",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
        }
    }
}

@Composable
private fun ChipsRow(offer: OfferDetailUi) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (offer.live) {
            Surface(shape = RoundedCornerShape(10.dp), color = Primary) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        offer.badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnPrimary,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            offer.endsInText?.let {
                Surface(shape = CircleShape, color = Cream) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = WarmGray, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, color = Ink)
                    }
                }
            }
        } else {
            Surface(shape = CircleShape, color = SurfaceFieldAlt) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(WarmGray, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "FINALIZADA · ${offer.badgeText}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = WarmGray,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            offer.endedText?.let {
                Icon(Icons.Outlined.EventBusy, null, tint = WarmGray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = WarmGray)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriceBlock(offer: OfferDetailUi) {
    val mainColor = if (offer.live) Primary else WarmGray
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = offer.price?.let(::formatMoney) ?: "$ —",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = mainColor,
        )
        Text(
            text = if (offer.live) "c/u" else "c/u (promo anterior)",
            style = MaterialTheme.typography.labelMedium,
            color = WarmGray,
        )
        offer.originalPrice?.let {
            Text(
                text = formatMoney(it),
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        if (offer.live) {
            offer.savings?.let {
                Surface(shape = RoundedCornerShape(8.dp), color = BadgeOrange) {
                    Text(
                        "Ahorrás ${formatMoney(it)} c/u",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(offer: OfferDetailUi) {
    val tint = if (offer.live) Secondary else WarmGray
    Surface(shape = RoundedCornerShape(16.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow(Icons.Outlined.LocalOffer, offer.ruleText, tint)
            InfoRow(Icons.Outlined.CalendarToday, offer.validityText, tint)
            offer.dateText?.let {
                InfoRow(if (offer.live) Icons.Outlined.Schedule else Icons.Outlined.History, it, tint)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Ink)
    }
}

@Composable
private fun BusinessSection(offer: OfferDetailUi, onOpenBusiness: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "COMERCIO DEL BARRIO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WarmGray,
        )
        Surface(
            onClick = onOpenBusiness,
            shape = RoundedCornerShape(16.dp),
            color = Cream,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(CardWhite),
                    contentAlignment = Alignment.Center,
                ) {
                    if (offer.businessPhotoUrl != null) {
                        AsyncImage(
                            model = offer.businessPhotoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Outlined.Storefront, null, tint = Secondary, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            offer.businessName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (offer.categoryName.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = BadgeOrange) {
                                Text(
                                    offer.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    val sub = listOf(offer.distanceText, offer.businessAddress).filter { it.isNotBlank() }
                    if (sub.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NearMe, null, tint = Primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                sub.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(30.dp).background(CardWhite, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Ink, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ConfirmationsCard(offer: OfferDetailUi, onConfirm: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = BadgeGreenSoft, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(ForestGreen, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when (offer.confirmations) {
                            0 -> "Todavía nadie confirmó esta oferta"
                            1 -> "1 vecino confirmó que sigue vigente"
                            else -> "${offer.confirmations} vecinos confirmaron que sigue vigente"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    offer.lastConfirmationText?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = WarmGray)
                    }
                }
            }
            Surface(
                onClick = onConfirm,
                shape = CircleShape,
                color = CardWhite,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ThumbUp, null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sigue vigente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Check, null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EndedFooter(offer: OfferDetailUi, onOpenBusiness: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (offer.confirmations > 0) {
            Surface(shape = RoundedCornerShape(20.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (offer.confirmations == 1) {
                        "1 vecino confirmó este beneficio mientras estuvo activo en el barrio."
                    } else {
                        "${offer.confirmations} vecinos confirmaron este beneficio mientras estuvo activo en el barrio."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Text(
            "Oferta finalizada. Mirá otras ofertas de este comercio.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            onClick = onOpenBusiness,
            shape = CircleShape,
            color = Cream,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocalOffer, null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ver ofertas de ${offer.businessName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = CardWhite.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}