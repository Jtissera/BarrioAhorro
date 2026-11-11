package com.barrioahorro.app.ui.screens.client.forum

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.forum.ForumBusinessDto
import com.barrioahorro.app.data.forum.ForumFeedFilters
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BorderGray
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import java.text.NumberFormat
import java.util.Locale

private val YellowAccent = Color(0xFFF2B134)
private val LightSalmon = Color(0xFFFFEDE6)
private val SageGreen = Color(0xFFEBF5F0)
private val SageGreenText = Color(0xFF2F6B55)
private val PillBeige = Color(0xFFF5EFE9)
private val PillBeigeText = Color(0xFF7A685D)
private val CommercePillBg = Color(0xFFFBF4ED)

private val DIAS_OPTIONS = listOf(
    1 to "Últimas 24 h",
    7 to "Últimos 7 días",
    30 to "Últimos 30 días",
)

@Composable
fun ForumFeedScreen(
    onCreatePost: () -> Unit,
    onPostClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ForumViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current


    LaunchedEffect(state.upvoteError) {
        state.upvoteError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onUpvoteErrorShown()
        }
    }
    LaunchedEffect(state.locationError) {
        state.locationError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onLocationErrorShown()

        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Cream,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("+ Publicar", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                ToldoConFranjas()

                // Header del foro
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(
                            text = "Foro del barrio",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 22.sp,
                        )
                        Text(
                            text = "Precios y ofertas que ven tus vecinos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmGray,
                            fontSize = 14.sp,
                        )
                    }

                    IconButton(onClick = viewModel::refreshFeed) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = Primary,
                        )
                    }
                }

                ForumFiltersBar(
                    filters = state.filters,
                    businesses = state.businesses,
                    onComercioSelected = viewModel::onComercioSelected,
                    onCercaClick = viewModel::onCercaToggled,
                    onDiasSelected = viewModel::onDiasSelected,
                    onClearAll = viewModel::clearFilters,
                )

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (state.posts.isEmpty()) {
                    if (state.filters.hayFiltros) {
                        NoResultsView(onClearFilters = viewModel::clearFilters)
                    } else {
                        EmptyFeedView(onCreatePost = onCreatePost)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.posts, key = { it.id }) { post ->
                            ForumPostCard(
                                post = post,
                                onClick = { onPostClick(post.id) },
                                onUpvote = { viewModel.toggleUpvote(post.id) },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

    }
}

// ---------------------------------------------------------------------------
// Filtros
// ---------------------------------------------------------------------------

@Composable
private fun ForumFiltersBar(
    filters: ForumFeedFilters,
    businesses: List<ForumBusinessDto>,
    onComercioSelected: (Long?, String?) -> Unit,
    onCercaClick: () -> Unit,
    onDiasSelected: (Int?) -> Unit,
    onClearAll: () -> Unit,
) {
    var comercioMenu by remember { mutableStateOf(false) }
    var fechaMenu by remember { mutableStateOf(false) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        // Comercio
        item {
            Box {
                FilterPill(
                    label = filters.comercioNombre ?: "Comercio",
                    active = filters.comercioId != null,
                    hasMenu = true,
                    onClick = { comercioMenu = true },
                    onClear = { onComercioSelected(null, null) },
                )
                DropdownMenu(expanded = comercioMenu, onDismissRequest = { comercioMenu = false }) {
                    if (businesses.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin comercios todavía") },
                            onClick = { comercioMenu = false },
                        )
                    }
                    businesses.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b.nombre) },
                            onClick = {
                                onComercioSelected(b.id, b.nombre)
                                comercioMenu = false
                            },
                        )
                    }
                }
            }
        }

        // Cercanía
        item {
            FilterPill(
                label = "Cerca tuyo",
                active = filters.cercaActivo,
                hasMenu = false,
                onClick = onCercaClick,
                onClear = onCercaClick,
            )
        }

        // Fecha
        item {
            Box {
                FilterPill(
                    label = DIAS_OPTIONS.firstOrNull { it.first == filters.dias }?.second ?: "Fecha",
                    active = filters.dias != null,
                    hasMenu = true,
                    onClick = { fechaMenu = true },
                    onClear = { onDiasSelected(null) },
                )
                DropdownMenu(expanded = fechaMenu, onDismissRequest = { fechaMenu = false }) {
                    DIAS_OPTIONS.forEach { (dias, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onDiasSelected(dias)
                                fechaMenu = false
                            },
                        )
                    }
                }
            }
        }

        if (filters.hayFiltros) {
            item {
                Text(
                    text = "Quitar filtros",
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    active: Boolean,
    hasMenu: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Primary else CardWhite)
            .border(1.dp, if (active) Primary else BorderGray, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        when {
            active -> {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar filtro",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onClear),
                )
            }
            hasMenu -> {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = WarmGray,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun NoResultsView(onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No hay publicaciones con estos filtros",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Probá con otro comercio, una zona más amplia o un rango de fechas mayor.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onClearFilters, shape = RoundedCornerShape(20.dp)) {
            Text("Quitar filtros", color = Primary, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------
// Tarjeta de publicación
// ---------------------------------------------------------------------------

@Composable
private fun ForumPostCard(
    post: ForumPostDto,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Barra de acento amarillo/ámbar en el lateral izquierdo
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        color = YellowAccent,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )

            // Contenido de la tarjeta
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Header: Avatar con iniciales + Nombre + Tiempo + Badge "Aporte de vecino"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEDE6DF), CircleShape),
                    ) {
                        Text(
                            text = getInitials(post.autorNombre),
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 13.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.autorNombre,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = formatRelativeTime(post.createdAt),
                            color = WarmGray,
                            fontSize = 12.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PillBeige)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Aporte de vecino",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PillBeigeText,
                        )
                    }
                }

                // Píldora del Comercio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CommercePillBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.comercioNombre,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF382319),
                    )
                }

                // Cuerpo del texto
                Text(
                    text = post.texto,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                // Badges opcionales: Precio observado y Ubicación
                val locationLabel = post.locationLabel()
                if (post.precioObservado != null || locationLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        post.precioObservado?.let { precio ->
                            val formattedPrice = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).apply {
                                maximumFractionDigits = 0
                            }.format(precio)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LightSalmon)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formattedPrice,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        if (locationLabel != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SageGreen)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = SageGreenText,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = locationLabel,
                                    color = SageGreenText,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                // Footer: Upvotes y Comentarios
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    val upvoted = post.upvotedByMe
                    val own = post.esUsuarioActual
                    val upvoteColor = when {
                        upvoted -> Primary
                        own -> WarmGray
                        else -> Ink
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (upvoted) LightSalmon else Color(0xFFF7F3EE))
                            .clickable(enabled = !own && !upvoted, onClick = onUpvote)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "↑",
                            fontWeight = FontWeight.Bold,
                            color = upvoteColor,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.upvotesCount}",
                            fontWeight = FontWeight.Bold,
                            color = upvoteColor,
                            fontSize = 12.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7F3EE))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.comentariosCount}",
                            color = WarmGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedView(onCreatePost: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFFFEDE6), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "El foro está tranquilo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sé el primero en compartir un precio, dato u oferta de algún comercio de tu barrio con otros vecinos.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onCreatePost,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
        ) {
            Text("Crear primera publicación", fontWeight = FontWeight.Bold)
        }
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "V"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
    }
}

