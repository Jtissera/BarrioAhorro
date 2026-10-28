package com.barrioahorro.app.ui.screens.client.explore

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.components.OfferPriceBlock
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BadgeGreenDot
import com.barrioahorro.app.ui.theme.BadgeGreenText
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
fun ExploreOfferCard(
    item: ExploreOfferUi,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offer = item.card

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            ToldoConFranjas(stripes = 20, height = 10.dp)

            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceFieldAlt, modifier = Modifier.size(92.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (offer.photoUrl != null) {
                                AsyncImage(
                                    model = offer.photoUrl.toAbsoluteUrl(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(32.dp))
                            }

                            // Corazón: círculo blanco; vacío y oscuro, o lleno y rojo si está guardada.
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.92f),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onToggleFavorite),
                            ) {
                                Icon(
                                    imageVector = if (item.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (item.favorite) "Quitar de favoritos" else "Guardar en favoritos",
                                    tint = if (item.favorite) Primary else Ink,
                                    modifier = Modifier.padding(4.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            OfferPriceBlock(info = offer.priceInfo)
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
                            if (offer.distanceText.isNotBlank()) {
                                Text("📍 ${offer.distanceText}", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                                Text(" • ", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                            }
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = WarmGray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(offer.vigenciaText, style = MaterialTheme.typography.labelSmall, color = WarmGray)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = SecondaryContainer) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    if (!item.businessOpen) {
                        Text("Cerrado ahora", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                    }
                }
            }
        }
    }
}

private fun String.toAbsoluteUrl(): String =
    if (startsWith("http://") || startsWith("https://")) this else "${BASE_URL.trimEnd('/')}/${trimStart('/')}"