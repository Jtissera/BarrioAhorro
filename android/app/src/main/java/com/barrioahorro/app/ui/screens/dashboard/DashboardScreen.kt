package com.barrioahorro.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.domain.model.ActiveOffer
import com.barrioahorro.app.domain.model.BusinessDashboard
import com.barrioahorro.app.domain.model.BusinessReview
import com.barrioahorro.app.domain.model.DayViews
import com.barrioahorro.app.domain.model.TopOffer
import com.barrioahorro.app.domain.repository.sampleDashboard
import com.barrioahorro.app.ui.components.BusinessBottomBar
import com.barrioahorro.app.ui.components.BusinessTab
import java.text.NumberFormat
import java.util.Locale

// TODO: reemplazar por los tokens reales del tema/paleta crema-rojo de la app.
internal object DashboardColors {
    val Cream = Color(0xFFFFF7EC)
    val Card = Color.White
    val Red = Color(0xFFD9381E)
    val RedSoft = Color(0xFFF6B9AE)
    val RedTint = Color(0xFFFDE8E3)
    val TextDark = Color(0xFF2B1B17)
    val TextMuted = Color(0xFF8A7B74)
    val Divider = Color(0xFFEFE6DC)
    val Star = Color(0xFFF5A623)
}

@Composable
fun DashboardScreen(
    onAddOffer: () -> Unit,
    onSeeAllOffers: () -> Unit,
    onSeeAllReviews: () -> Unit,
    onTabSelected: (BusinessTab) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = DashboardColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BusinessBottomBar(
                selectedTab = BusinessTab.INICIO,
                onTabSelected = onTabSelected,
            )
        },
        modifier = modifier,
    ) { padding ->
        DashboardContent(
            state = state,
            onAddOffer = onAddOffer,
            onSeeAllOffers = onSeeAllOffers,
            onSeeAllReviews = onSeeAllReviews,
            onRetry = viewModel::load,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    onAddOffer: () -> Unit,
    onSeeAllOffers: () -> Unit,
    onSeeAllReviews: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DashboardColors.Cream)) {
        when (state) {
            DashboardUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = DashboardColors.Red
            )
            is DashboardUiState.Error -> Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.message, color = DashboardColors.TextDark, textAlign = TextAlign.Center)
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = DashboardColors.Red)
                ) { Text("Reintentar") }
            }
            is DashboardUiState.Success -> DashboardBody(
                data = state.data,
                onAddOffer = onAddOffer,
                onSeeAllOffers = onSeeAllOffers,
                onSeeAllReviews = onSeeAllReviews
            )
        }
    }
}

@Composable
private fun DashboardBody(
    data: BusinessDashboard,
    onAddOffer: () -> Unit,
    onSeeAllOffers: () -> Unit,
    onSeeAllReviews: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Hola,", fontSize = 13.sp, color = DashboardColors.TextMuted)
            Text(
                data.businessName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardColors.TextDark
            )
        }

        Button(
            onClick = onAddOffer,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DashboardColors.Red,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Agregar oferta", fontWeight = FontWeight.Bold)
        }

        ViewsCard(totalViews = data.totalViews, viewsByDay = data.viewsByDay)

        DashboardSection(title = "Ofertas más vistas") {
            if (data.topOffers.isEmpty()) EmptyHint("Todavía no hay visualizaciones")
            data.topOffers.forEachIndexed { index, offer ->
                if (index > 0) RowDivider()
                TopOfferRow(rank = index + 1, offer = offer)
            }
        }

        DashboardSection(
            title = "Reseñas recientes",
            actionLabel = "Ver todas",
            onAction = onSeeAllReviews
        ) {
            if (data.recentReviews.isEmpty()) EmptyHint("Todavía no recibiste reseñas")
            data.recentReviews.forEachIndexed { index, review ->
                if (index > 0) RowDivider()
                ReviewRow(review)
            }
        }

        DashboardSection(
            title = "Ofertas activas",
            actionLabel = "Ver todas",
            onAction = onSeeAllOffers
        ) {
            if (data.activeOffers.isEmpty()) EmptyHint("No tenés ofertas activas")
            data.activeOffers.forEachIndexed { index, offer ->
                if (index > 0) RowDivider()
                ActiveOfferRow(offer)
            }
        }
    }
}

@Composable
private fun ViewsCard(totalViews: Int, viewsByDay: List<DayViews>) {
    DashboardSection(title = "Visualizaciones") {
        Text(
            formatCount(totalViews),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardColors.TextDark
        )
        Text("Últimos 7 días", fontSize = 12.sp, color = DashboardColors.TextMuted)
        ViewsBarChart(viewsByDay)
    }
}

@Composable
private fun ViewsBarChart(data: List<DayViews>) {
    if (data.isEmpty()) return
    val max = data.maxOf { it.views }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { day ->
                val fraction = (day.views.toFloat() / max).coerceIn(0.04f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (day.views == max) DashboardColors.Red else DashboardColors.RedSoft)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { day ->
                Text(
                    day.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = DashboardColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.Card)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardColors.TextDark
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                        Text(actionLabel, fontSize = 13.sp, color = DashboardColors.Red)
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun TopOfferRow(rank: Int, offer: TopOffer) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(DashboardColors.RedTint),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DashboardColors.Red)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            offer.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = DashboardColors.TextDark
        )
        Spacer(Modifier.width(8.dp))
        Text("${formatCount(offer.views)} vistas", fontSize = 12.sp, color = DashboardColors.TextMuted)
    }
}

@Composable
private fun ReviewRow(review: BusinessReview) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(DashboardColors.RedTint),
            contentAlignment = Alignment.Center
        ) {
            Text(
                review.authorName.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = DashboardColors.Red
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    review.authorName,
                    fontWeight = FontWeight.SemiBold,
                    color = DashboardColors.TextDark
                )
                RatingStars(review.rating)
            }
            Text(
                review.comment,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = DashboardColors.TextMuted
            )
        }
    }
}

@Composable
private fun RatingStars(rating: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (index < rating) DashboardColors.Star else DashboardColors.Divider
            )
        }
    }
}

@Composable
private fun ActiveOfferRow(offer: ActiveOffer) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                offer.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = DashboardColors.TextDark
            )
            offer.expiresLabel?.let {
                Text(it, fontSize = 12.sp, color = DashboardColors.TextMuted)
            }
        }
        offer.priceLabel?.let { price ->
            Spacer(Modifier.width(8.dp))
            Text(
                price,
                fontWeight = FontWeight.Bold,
                color = DashboardColors.Red
            )
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = DashboardColors.Divider)

@Composable
private fun EmptyHint(text: String) =
    Text(text, fontSize = 13.sp, color = DashboardColors.TextMuted)

private fun formatCount(n: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-AR")).format(n)



@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun DashboardPreview() {
    DashboardContent(
        state = DashboardUiState.Success(sampleDashboard),
        onAddOffer = {},
        onSeeAllOffers = {},
        onSeeAllReviews = {},
        onRetry = {}
    )
}