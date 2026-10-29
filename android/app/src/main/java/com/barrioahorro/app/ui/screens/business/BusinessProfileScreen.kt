package com.barrioahorro.app.ui.screens.business

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.SurfaceMuted
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.WarmGray

private val COVER_HEIGHT = 240.dp
private val SHEET_OVERLAP = 28.dp
private val SHEET_SHAPE = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
fun BusinessProfileScreen(
    onEditData: () -> Unit,
    onEditPhotos: () -> Unit,
    onManageOffers: () -> Unit,
    onEditOffer: (Long) -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    readOnly: Boolean = false,
    requestedTab: ProfileTab? = null,
    onRequestedTabConsumed: () -> Unit = {},
    viewModel: BusinessProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            viewModel.selectTab(requestedTab)
            onRequestedTabConsumed()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val modifier = Modifier.fillMaxSize()
    val error = state.error

    when {
        state.isLoading -> Box(modifier, Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }

        error != null -> Column(
            modifier = modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ErrorHint(text = error)
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton(text = "Reintentar", onClick = viewModel::load, height = 48.dp)
        }

        else -> ProfileBody(
            state = state,
            onEditData = onEditData,
            onEditPhotos = onEditPhotos,
            onSelectTab = viewModel::selectTab,
            onManageOffers = onManageOffers,
            onEditOffer = onEditOffer,
            onLoadMoreReviews = viewModel::loadMoreReviews,
            onRetry = viewModel::load,
            onLogout = onLogout,
            isLoggingOut = isLoggingOut,
            readOnly = readOnly,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileBody(
    state: BusinessProfileUiState,
    onEditData: () -> Unit,
    onEditPhotos: () -> Unit,
    onSelectTab: (ProfileTab) -> Unit,
    onManageOffers: () -> Unit,
    onEditOffer: (Long) -> Unit,
    onLoadMoreReviews: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    readOnly: Boolean,
    modifier: Modifier,
) {
    // Non-lazy column: each tab holds at most one page of reviews or the active offers, so it stays small.
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // The white sheet is drawn over the bottom edge of the cover.
        Box(modifier = Modifier.fillMaxWidth()) {
            Cover(state.coverUrl)
            Column(modifier = Modifier.padding(top = COVER_HEIGHT - SHEET_OVERLAP)) {
                ProfileSheet(
                    state = state,
                    readOnly = readOnly,
                    onEditData = onEditData,
                    onEditPhotos = onEditPhotos,
                    onSelectTab = onSelectTab,
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.selectedTab) {
                ProfileTab.OFFERS -> ProfileOffersTab(state, onManageOffers, onEditOffer, onRetry, readOnly)
                ProfileTab.REVIEWS -> ProfileReviewsTab(state, onLoadMoreReviews, onRetry)
                ProfileTab.INFO -> ProfileInfoTab(state, onLogout, isLoggingOut)
            }
        }
    }
}

@Composable
private fun ProfileSheet(
    state: BusinessProfileUiState,
    readOnly: Boolean,
    onEditData: () -> Unit,
    onEditPhotos: () -> Unit,
    onSelectTab: (ProfileTab) -> Unit,
) {
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
                    Icon(
                        Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            CategoryAndRating(state)
            OpenStatusPill(state.openStatus)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    text = "Editar datos",
                    icon = Icons.Outlined.Edit,
                    containerColor = Cream,
                    contentColor = Primary,
                    enabled = !readOnly,
                    onClick = onEditData,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    text = "Editar foto",
                    icon = Icons.Outlined.CameraAlt,
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    enabled = !readOnly,
                    onClick = onEditPhotos,
                    modifier = Modifier.weight(1f),
                )
            }

            if (readOnly) {
                Text(
                    text = "Tu perfil está en modo lectura hasta que ratifiques tu aporte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }

            ProfileTabSelector(
                selected = state.selectedTab,
                offersCount = state.activeCount,
                reviewsCount = state.reviewsSummary?.total,
                onSelect = onSelectTab,
            )
        }
    }
}

@Composable
private fun CategoryAndRating(state: BusinessProfileUiState) {
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
internal fun OpenStatusPill(status: OpenStatus) {
    if (status.text.isBlank()) return
    Row(
        modifier = Modifier
            .background(if (status.isOpen) SecondaryContainer else SurfaceMuted, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (status.isOpen) Secondary else MutedLabel, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (status.isOpen) Ink else WarmGray,
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SurfaceMuted,
            disabledContentColor = MutedLabel,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = modifier.height(56.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/** Flat tabs: label + optional count chip, with a red indicator under the selected one. */
@Composable
internal fun ProfileTabSelector(
    selected: ProfileTab,
    offersCount: Int?,
    reviewsCount: Int?,
    onSelect: (ProfileTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileTab.values().forEach { tab ->
                val isSelected = tab == selected
                val count = when (tab) {
                    ProfileTab.OFFERS -> offersCount
                    ProfileTab.REVIEWS -> reviewsCount
                    ProfileTab.INFO -> null
                }
                Column(
                    modifier = Modifier.weight(1f).clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tabTitle(tab),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Primary else Ink,
                        )
                        if (count != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = Cream) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                if (isSelected) Primary else Color.Transparent,
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            ),
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceFieldAlt))
    }
}

private fun tabTitle(tab: ProfileTab): String = when (tab) {
    ProfileTab.OFFERS -> "Ofertas"
    ProfileTab.REVIEWS -> "Reseñas"
    ProfileTab.INFO -> "Info"
}

@Composable
private fun Cover(url: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(COVER_HEIGHT)
            .background(SurfaceFieldAlt),
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
internal fun ProfileCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}