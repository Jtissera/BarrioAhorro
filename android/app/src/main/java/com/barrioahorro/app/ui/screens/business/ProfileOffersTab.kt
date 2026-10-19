package com.barrioahorro.app.ui.screens.business

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.components.profile.StatusChip
import com.barrioahorro.app.ui.screens.offers.MyOfferItemUi
import com.barrioahorro.app.ui.theme.BadgeGreenDot
import com.barrioahorro.app.ui.theme.BadgeGreenText
import com.barrioahorro.app.ui.theme.BadgeGreen
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.BadgeYellow
import com.barrioahorro.app.ui.theme.BadgeYellowText
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.WarmGray

@Composable
internal fun ProfileOffersTab(
    state: BusinessProfileUiState,
    onManageOffers: () -> Unit,
    onEditOffer: (Long) -> Unit,
    onRetry: () -> Unit,
    readOnly: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Promociones vigentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                modifier = Modifier.weight(1f, fill = false),
            )
            state.activeCount?.let { count ->
                Spacer(Modifier.width(8.dp))
                StatusChip(
                    text = if (count == 1) "1 activa" else "$count activas",
                    containerColor = BadgeGreen,
                    contentColor = BadgeGreenText,
                    dotColor = BadgeGreenDot,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Gestionar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onManageOffers)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }

        when {
            state.offersError != null -> {
                ErrorHint(text = state.offersError)
                PrimaryPillButton(text = "Reintentar", onClick = onRetry, height = 48.dp)
            }
            state.activeCount == null -> Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Primary) }
            state.offers.isEmpty() -> EmptyOffersCard()
            else -> state.offers.forEach { offer ->
                ProfileOfferCard(offer = offer, enabled = !readOnly, onEdit = { onEditOffer(offer.id) })
            }
        }
    }
}

@Composable
private fun EmptyOffersCard() {
    ProfileCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(40.dp))
            Text(
                text = "No tenés ofertas vigentes en este momento",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = "Publicá promociones para que tus vecinos las encuentren.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
            )
        }
    }
}

@Composable
private fun ProfileOfferCard(offer: MyOfferItemUi, enabled: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            ToldoConFranjas(stripes = 20, height = 10.dp)

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceFieldAlt, modifier = Modifier.size(92.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (offer.photoUrl != null) {
                                AsyncImage(
                                    model = resolvePhotoUrl(BASE_URL, offer.photoUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (offer.isYellowBadge) BadgeYellow else BadgeOrange,
                            ) {
                                Text(
                                    text = offer.badgeText,
                                    color = if (offer.isYellowBadge) BadgeYellowText else Primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                            PriceBlock(offer)
                        }

                        Text(
                            text = offer.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = WarmGray, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = offer.businessName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (offer.categoryName.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = SurfaceFieldAlt) {
                                    Text(
                                        text = offer.categoryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarmGray,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = WarmGray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(text = offer.vigenciaText, style = MaterialTheme.typography.labelSmall, color = WarmGray)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = RoundedCornerShape(12.dp), color = SecondaryContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = BadgeGreenDot, modifier = Modifier.size(14.dp))
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
                            .clickable(enabled = enabled, onClick = onEdit)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = if (enabled) Primary else MutedLabel, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Editar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) Primary else MutedLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceBlock(offer: MyOfferItemUi) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = offer.priceMain,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
            if (offer.priceUnit != null) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = offer.priceUnit,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (offer.priceOriginal != null) {
            Text(
                text = offer.priceOriginal,
                style = MaterialTheme.typography.labelSmall,
                color = MutedLabel,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}