package com.barrioahorro.app.ui.screens.client.business

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.screens.business.OpenStatusPill
import com.barrioahorro.app.ui.screens.business.ProfileTab
import com.barrioahorro.app.ui.screens.business.ProfileTabSelector
import com.barrioahorro.app.ui.screens.business.reviewsCountText
import com.barrioahorro.app.ui.screens.client.ClientBottomBar
import com.barrioahorro.app.ui.screens.client.ClientTab
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

private val COVER_HEIGHT = 240.dp
private val SHEET_OVERLAP = 28.dp
private val SHEET_SHAPE = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Perfil de un comercio visto por el vecino. Mantiene la barra inferior del vecino:
 * tocar una pestaña (Explorar incluida) vuelve al home en esa pestaña.
 */
@Composable
fun ClientBusinessProfileScreen(
    onBack: () -> Unit,
    onTabSelected: (ClientTab) -> Unit,
    viewModel: ClientBusinessProfileViewModel = hiltViewModel(),
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
        containerColor = Cream,
        // La portada llega hasta arriba de todo, detrás de la barra de estado.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { ClientBottomBar(selectedTab = ClientTab.EXPLORAR, onTabSelected = onTabSelected) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)

                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ErrorHint(text = state.error.orEmpty())
                    PrimaryPillButton(text = "Reintentar", onClick = viewModel::load, height = 48.dp)
                }

                else -> ProfileContent(state = state, viewModel = viewModel)
            }

            BackButton(onBack, Modifier.align(Alignment.TopStart))
        }
    }

    if (state.showReviewForm) {
        ReviewFormDialog(
            initialRating = state.myReview?.rating ?: 0,
            initialComment = state.myReview?.comment.orEmpty(),
            isEdit = state.myReview != null,
            isSaving = state.isSavingReview,
            serverError = state.reviewFormError,
            onSave = viewModel::saveReview,
            onDismiss = viewModel::closeReviewForm,
        )
    }
}

@Composable
private fun ProfileContent(state: ClientBusinessProfileUiState, viewModel: ClientBusinessProfileViewModel) {
    // Columna sin lazy: cada pestaña tiene pocas tarjetas (ofertas vigentes o una página de reseñas).
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth()) {
            Cover(state.photoUrl)
            Column(Modifier.padding(top = COVER_HEIGHT - SHEET_OVERLAP)) {
                Sheet(state = state, onSelectTab = viewModel::selectTab)
            }
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.selectedTab) {
                ProfileTab.OFFERS -> ClientOffersTab(
                    state = state,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRetry = viewModel::retryOffers,
                )

                ProfileTab.REVIEWS -> ClientReviewsTab(
                    state = state,
                    onWriteReview = viewModel::openReviewForm,
                    onDeleteReview = viewModel::deleteReview,
                    onLoadMore = viewModel::loadMoreReviews,
                    onRetry = viewModel::retryReviews,
                )

                ProfileTab.INFO -> ClientInfoTab(state)
            }
        }
    }
}

@Composable
private fun Sheet(state: ClientBusinessProfileUiState, onSelectTab: (ProfileTab) -> Unit) {
    Surface(shape = SHEET_SHAPE, color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.businessName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.size(52.dp).background(SecondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Secondary, modifier = Modifier.size(26.dp))
                }
            }

            CategoryAndRating(state)
            OpenStatusPill(state.openStatus)

            ProfileTabSelector(
                selected = state.selectedTab,
                offersCount = state.offers?.size,
                reviewsCount = state.reviewsSummary?.total,
                onSelect = onSelectTab,
            )
        }
    }
}

@Composable
private fun CategoryAndRating(state: ClientBusinessProfileUiState) {
    val summary = state.reviewsSummary
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.categoryName.isNotBlank()) {
            Surface(shape = CircleShape, color = Cream) {
                Text(
                    text = state.categoryName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        when {
            summary == null -> Unit
            summary.total == 0 -> Text(
                text = "Sin reseñas todavía",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )

            else -> {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = summary.averageText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "(${reviewsCountText(summary.total)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
        }
    }
}

@Composable
private fun Cover(url: String?) {
    Box(
        modifier = Modifier.fillMaxWidth().height(COVER_HEIGHT).background(SurfaceFieldAlt),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = MutedLabel,
                modifier = Modifier.padding(top = 72.dp).size(56.dp),
            )
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBack,
        shape = CircleShape,
        color = CardWhite.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = modifier.statusBarsPadding().padding(12.dp).size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Ink, modifier = Modifier.size(22.dp))
        }
    }
}
