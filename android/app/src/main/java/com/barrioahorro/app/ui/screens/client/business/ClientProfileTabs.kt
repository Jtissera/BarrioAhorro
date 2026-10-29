package com.barrioahorro.app.ui.screens.client.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.components.profile.StatusChip
import com.barrioahorro.app.ui.screens.business.BusinessAddressMap
import com.barrioahorro.app.ui.screens.business.ProfileCard
import com.barrioahorro.app.ui.screens.business.ProfileReviewUi
import com.barrioahorro.app.ui.screens.business.RatingStars
import com.barrioahorro.app.ui.screens.business.ScheduleRow
import com.barrioahorro.app.ui.screens.business.SectionHeader
import com.barrioahorro.app.ui.screens.business.SummaryCard
import com.barrioahorro.app.ui.screens.client.explore.ExploreOfferCard
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceMuted
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.WarmGray

private const val MIN_COMMENT = 10
private const val MAX_COMMENT = 300

// ---- Ofertas ---------------------------------------------------------------------

@Composable
internal fun ClientOffersTab(
    state: ClientBusinessProfileUiState,
    onToggleFavorite: (Long) -> Unit,
    onRetry: () -> Unit,
    onOpenOffer: (Long) -> Unit,
    ) {
    val offers = state.offers

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ofertas del día",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
            if (offers != null) {
                Spacer(Modifier.width(8.dp))
                StatusChip(
                    text = if (offers.size == 1) "1 VIGENTE" else "${offers.size} VIGENTES",
                    containerColor = BadgeOrange,
                    contentColor = Primary,
                )
                Spacer(Modifier.weight(1f))
                Text("Actualizado hoy", style = MaterialTheme.typography.labelSmall, color = WarmGray)
            }
        }

        when {
            state.offersError != null && offers == null -> {
                ErrorHint(text = state.offersError)
                PrimaryPillButton(text = "Reintentar", onClick = onRetry, height = 48.dp)
            }

            offers == null -> LoadingBox()

            offers.isEmpty() -> ProfileCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(40.dp))
                    Text(
                        text = "Este comercio no tiene ofertas vigentes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = "Volvé a mirar más tarde: publican promociones seguido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                    )
                }
            }

            else -> offers.forEach { offer ->
                ExploreOfferCard(
                    item = offer,
                    onToggleFavorite = { onToggleFavorite(offer.id) },
                    onClick = { onOpenOffer(offer.id) },
                )
            }
        }
    }
}

// ---- Reseñas ---------------------------------------------------------------------

@Composable
internal fun ClientReviewsTab(
    state: ClientBusinessProfileUiState,
    onWriteReview: () -> Unit,
    onDeleteRequest: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val summary = state.reviewsSummary

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            summary == null && state.reviewsError != null -> {
                ErrorHint(text = state.reviewsError)
                PrimaryPillButton(text = "Reintentar", onClick = onRetry, height = 48.dp)
            }

            summary == null -> LoadingBox()

            else -> {
                if (summary.total == 0) {
                    ProfileCard {
                        Text(
                            text = "Todavía no hay reseñas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                        Text(
                            text = "Sé el primero en contarle a tus vecinos cómo te fue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray,
                        )
                    }
                } else {
                    SummaryCard(summary)
                }

                if (state.myReview == null) {
                    PrimaryPillButton(
                        text = "Escribir reseña",
                        onClick = onWriteReview,
                        icon = Icons.Outlined.Edit,
                        height = 52.dp,
                    )
                }

                if (state.myReview != null || state.reviews.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Comentarios de vecinos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                        )
                        Text("Más recientes", style = MaterialTheme.typography.labelMedium, color = WarmGray)
                    }
                }

                state.myReview?.let { mine ->
                    ReviewItem(
                        review = mine,
                        isMine = true,
                        busy = state.isDeletingReview,
                        onEdit = onWriteReview,
                        onDelete = onDeleteRequest,
                    )
                }
                state.reviews.forEach { review ->
                    ReviewItem(review = review, isMine = false, busy = false, onEdit = {}, onDelete = {})
                }

                if (state.reviewsError != null) ErrorHint(text = state.reviewsError)

                if (state.hasMoreReviews) {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !state.isLoadingMoreReviews,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        if (state.isLoadingMoreReviews) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Ver más reseñas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: ProfileReviewUi,
    isMine: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ProfileCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = review.initials,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = review.authorName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                if (review.relativeDate.isNotBlank()) {
                    Text(text = review.relativeDate, style = MaterialTheme.typography.labelSmall, color = WarmGray)
                }
            }
            if (isMine) {
                StatusChip(text = "Tu reseña", containerColor = PrimaryContainer, contentColor = Primary)
                OwnReviewMenu(enabled = !busy, onEdit = onEdit, onDelete = onDelete)
            }
        }
        RatingStars(rating = review.rating, size = 16.dp)
        Text(text = review.comment, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

@Composable
private fun OwnReviewMenu(enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de tu reseña", tint = WarmGray)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Editar") },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = ErrorRed) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}



// ---- Info ------------------------------------------------------------------------

@Composable
internal fun ClientInfoTab(state: ClientBusinessProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileCard {
            SectionHeader(icon = Icons.Outlined.Storefront, title = "Sobre ${state.businessName}")
            Text(
                text = state.description.ifBlank { "Este comercio todavía no cargó una descripción." },
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }

        ProfileCard {
            SectionHeader(icon = Icons.Outlined.Schedule, title = "Horarios de atención") {
                if (state.isOpenToday) {
                    StatusChip(
                        text = "Abierto hoy",
                        containerColor = SecondaryContainer,
                        contentColor = Secondary,
                        dotColor = Secondary,
                    )
                } else {
                    StatusChip(text = "Cerrado hoy", containerColor = SurfaceMuted, contentColor = WarmGray)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.schedule.forEach { day ->
                    ScheduleRow(
                        label = day.label,
                        slotsText = if (day.enabled && day.slots.isNotEmpty()) {
                            day.slots.joinToString(" · ") { "${it.horaInicio.take(5)} a ${it.horaFin.take(5)}" }
                        } else {
                            null
                        },
                        isToday = day.diaSemana == state.todayDiaSemana,
                    )
                }
            }
        }

        ProfileCard {
            SectionHeader(icon = Icons.Outlined.LocationOn, title = "Dirección")
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = state.address.ifBlank { "Sin dirección cargada" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                if (state.distanceText.isNotBlank()) {
                    Text(
                        text = state.distanceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary,
                    )
                }
            }
            val latitude = state.latitude
            val longitude = state.longitude
            if (latitude != null && longitude != null) {
                BusinessAddressMap(
                    latitud = latitude,
                    longitud = longitude,
                    isFetchingLocation = false,
                    onUseCurrentLocation = {},
                    showLocationPill = false,
                )
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = Primary) }
}
