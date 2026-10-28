package com.barrioahorro.app.ui.screens.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.WarmGray
import kotlin.math.roundToInt

@Composable
internal fun ProfileReviewsTab(
    state: BusinessProfileUiState,
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
            summary == null -> Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Primary) }
            summary.total == 0 -> ProfileCard {
                Text(
                    text = "Todavía no tenés reseñas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "Cuando tus vecinos te dejen una reseña, la vas a ver acá.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
            else -> {
                SummaryCard(summary)

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
                    Text(text = "Más recientes", style = MaterialTheme.typography.labelMedium, color = WarmGray)
                }

                state.reviews.forEach { review -> ReviewCard(review) }

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
internal fun SummaryCard(summary: ReviewsSummaryUi) {
    ProfileCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                Text(
                    text = summary.averageText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                )
                RatingStars(rating = summary.average.roundToInt(), size = 14.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = reviewsCountText(summary.total),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                summary.bars.forEach { StarBarRow(it) }
            }
        }
    }
}

@Composable
private fun StarBarRow(bar: StarBarUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${bar.stars}",
            style = MaterialTheme.typography.labelMedium,
            color = WarmGray,
            modifier = Modifier.width(10.dp),
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(SurfaceFieldAlt),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(bar.fraction)
                    .fillMaxHeight()
                    .background(Tertiary, CircleShape),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${bar.count}",
            style = MaterialTheme.typography.labelMedium,
            color = WarmGray,
            textAlign = TextAlign.End,
            modifier = Modifier.width(24.dp),
        )
    }
}

@Composable
private fun ReviewCard(review: ProfileReviewUi) {
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
            Column {
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
        }
        RatingStars(rating = review.rating, size = 16.dp)
        Text(text = review.comment, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

/** Five stars, the first [rating] filled. */
@Composable
internal fun RatingStars(rating: Int, size: Dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            val filled = index < rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (filled) Tertiary else MutedLabel,
                modifier = Modifier.size(size),
            )
        }
    }
}