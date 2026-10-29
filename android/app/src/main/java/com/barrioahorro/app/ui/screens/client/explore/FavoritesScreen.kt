package com.barrioahorro.app.ui.screens.client.explore

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.WarmGray
import androidx.compose.ui.graphics.Color

@Composable
fun FavoritesScreen(onOpenOffer: (Long) -> Unit = {}, viewModel: FavoritesViewModel = hiltViewModel()){
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Cada vez que se entra a la pestaña se vuelve a leer la lista.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding()) {
        ToldoConFranjas(stripes = 18, height = 12.dp, altColor = Cream)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mis favoritos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkBlack,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${state.savedCount} guardados",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
            Text(
                "Ofertas que guardaste para comprar más tarde.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavFilterPill("Todas", state.items.size, state.filter == FavoritesFilter.ALL) {
                    viewModel.setFilter(FavoritesFilter.ALL)
                }
                FavFilterPill("Vigentes", state.liveCount, state.filter == FavoritesFilter.LIVE) {
                    viewModel.setFilter(FavoritesFilter.LIVE)
                }
                FavFilterPill("Finalizadas", state.endedCount, state.filter == FavoritesFilter.ENDED) {
                    viewModel.setFilter(FavoritesFilter.ENDED)
                }
            }
        }

        when {
            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }

            state.error != null && state.items.isEmpty() -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text(state.error.orEmpty(), textAlign = TextAlign.Center, color = Ink)
                Button(
                    onClick = viewModel::load,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                ) { Text("Reintentar") }
            }

            state.visible.isEmpty() -> EmptyFavorites(
                filter = state.filter,
                hasAny = state.items.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.visible, key = { it.id }) { offer ->
                    ExploreOfferCard(
                        item = offer,
                        onToggleFavorite = { viewModel.toggleFavorite(offer.id) },
                        onClick = { onOpenOffer(offer.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavFilterPill(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else Color.Transparent,
        contentColor = if (selected) OnPrimary else Ink,
    ) {
        Text(
            "$title ($count)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EmptyFavorites(filter: FavoritesFilter, hasAny: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(48.dp))
        Text(
            text = when {
                !hasAny -> "Todavía no guardaste ofertas"
                filter == FavoritesFilter.LIVE -> "No tenés favoritos vigentes"
                else -> "No tenés favoritos finalizados"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        if (!hasAny) {
            Text(
                "Tocá el corazón de una oferta en Explorar para verla acá.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}