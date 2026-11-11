package com.barrioahorro.app.ui.screens.client.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.data.forum.MyForumCommentDto
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.WarmGray
import java.text.NumberFormat
import java.util.Locale


private enum class MyPostsTab(val label: String) {
    PUBLICACIONES("Publicaciones"),
    COMENTARIOS("Comentarios"),
}

@Composable
fun MyPostsScreen(
    onNavigateBack: () -> Unit,
    onPostClick: (Long) -> Unit,
    onEditPost: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPostsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(MyPostsTab.PUBLICACIONES) }

    // Al volver de editar (u otra pantalla) se recarga la actividad (comentado mientras probamos)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshOnResume() }

    // Temporizador: cuando el ViewModel emite un mensaje, espera 3 segundos y lo limpia
    LaunchedEffect(state.feedbackMessage) {
        if (state.feedbackMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearFeedbackMessage()
        }
    }

    state.editingComment?.let { comment ->
        EditCommentDialog(
            initialText = comment.texto,
            isSaving = state.isSavingComment,
            errorMessage = state.editCommentError,
            onDismiss = viewModel::cancelEditComment,
            onSave = viewModel::saveCommentEdit,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // 1. CAPA DE FONDO: Toda tu pantalla y listas
        Column(modifier = Modifier.fillMaxSize()) {
            ToldoConFranjas(stripes = 12, height = 10.dp, altColor = Cream)

            // Header con flecha atrás y título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Ink,
                    )
                }
                Text(
                    text = "Mis publicaciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontSize = 20.sp,
                )
            }

            MyPostsTabs(
                selectedTab = selectedTab,
                postsCount = state.posts.size,
                commentsCount = state.comments.size,
                onTabSelected = { selectedTab = it },
            )

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                state.errorMessage != null -> {
                    MyPostsMessageView(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "No pudimos cargar tu actividad",
                        message = state.errorMessage.orEmpty(),
                        actionLabel = "Reintentar",
                        onAction = { viewModel.loadMyActivity() },
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "puntos") { PointsSummaryCard(puntos = state.puntos) }

                        when {
                            selectedTab == MyPostsTab.PUBLICACIONES && state.posts.isEmpty() -> item(key = "empty") {
                                MyPostsMessageView(
                                    icon = Icons.Outlined.ChatBubbleOutline,
                                    title = "Todavía no publicaste nada",
                                    message = "Cuando compartas un precio u oferta en el foro, lo vas a ver acá.",
                                    fillScreen = false,
                                )
                            }
                            selectedTab == MyPostsTab.COMENTARIOS && state.comments.isEmpty() -> item(key = "empty") {
                                MyPostsMessageView(
                                    icon = Icons.Outlined.ChatBubbleOutline,
                                    title = "Todavía no comentaste",
                                    message = "Tus comentarios y respuestas en el foro van a aparecer acá.",
                                    fillScreen = false,
                                )
                            }
                            selectedTab == MyPostsTab.PUBLICACIONES -> items(state.posts, key = { "post-${it.id}" }) { post ->
                                MyPostCard(
                                    post = post,
                                    onClick = { onPostClick(post.id) },
                                    onEdit = { onEditPost(post.id) },
                                    onDelete = { viewModel.deletePost(post.id) },
                                )
                            }
                            else -> items(state.comments, key = { "comment-${it.id}" }) { comment ->
                                MyCommentCard(
                                    comment = comment,
                                    onClick = { onPostClick(comment.publicacionId) },
                                    onEdit = { viewModel.startEditComment(comment) },
                                    onDelete = { viewModel.deleteComment(comment.id) },
                                )
                            }
                        }

                        // Espacio extra al final de la lista para hacer scroll cómodo
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }
        }

        // 2. CAPA SUPERIOR: El banner flotando por encima, leyendo DIRECTAMENTE de state.feedbackMessage
        state.feedbackMessage?.let { message ->
            DeletedFeedbackBanner(
                message = message,
                onDismiss = { viewModel.clearFeedbackMessage() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Suficiente elevación para quedar sobre la BottomBar
            )
        }
    }
}
@Composable
private fun MyPostsTabs(
    selectedTab: MyPostsTab,
    postsCount: Int,
    commentsCount: Int,
    onTabSelected: (MyPostsTab) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Cream)
            .padding(4.dp),
    ) {
        MyPostsTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val count = if (tab == MyPostsTab.PUBLICACIONES) postsCount else commentsCount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) CardWhite else Color.Transparent)
                    .clickable { onTabSelected(tab) },
            ) {
                Text(
                    text = tab.label,
                    color = if (selected) Primary else WarmGray,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) PrimaryContainer else Cream)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "$count",
                        color = if (selected) Primary else WarmGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PointsSummaryCard(puntos: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceSecondary, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$puntos puntos",
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                )
            }
            Text(
                text = "Ganás puntos con los upvotes que recibís.",
                color = WarmGray,
                fontSize = 14.sp,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Brush.horizontalGradient(listOf(Primary, Tertiary))),
        )
    }
}

@Composable
private fun MyActivityCard(
    onClick: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

/** Fila superior: chip del comercio + fecha relativa + menú "⋯". */
@Composable
private fun MyActivityHeader(comercioNombre: String, createdAt: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Chip + fecha ocupan todo el ancho libre; el chip solo se recorta si el nombre no entra.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(Cream)
                    .padding(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Store,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comercioNombre,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatRelativeTime(createdAt),
                color = WarmGray,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ForumOptionsMenu(onEdit = onEdit, onDelete = onDelete, circular = true)
    }
}

@Composable
private fun MyPostCard(
    post: ForumPostDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    MyActivityCard(onClick = onClick, onDelete = onDelete) {
        MyActivityHeader(
            comercioNombre = post.comercioNombre,
            createdAt = post.createdAt,
            onEdit = onEdit,
            onDelete = onDelete,
        )

        Text(
            text = post.texto,
            color = Ink,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )

        MyActivityFooter(upvotesCount = post.upvotesCount, commentsCount = post.comentariosCount)
    }
}
@Composable
private fun MyCommentCard(
    comment: MyForumCommentDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    MyActivityCard(onClick = onClick, onDelete = onDelete ) {
        MyActivityHeader(comercioNombre = comment.comercioNombre, createdAt = comment.createdAt, onEdit = onEdit, onDelete = onDelete)

        Text(
            text = comment.texto,
            color = Ink,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )

        Text(
            text = "En: ${comment.publicacionTexto}",
            color = WarmGray,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        MyActivityFooter(upvotesCount = comment.upvotesCount, commentsCount = null)
    }
}

@Composable
private fun MyActivityFooter(
    upvotesCount: Int,
    commentsCount: Int?,
) {
    val highlighted = upvotesCount > 0
    val pillBg = if (highlighted) PrimaryContainer else Cream
    val pillContent = if (highlighted) Primary else Ink

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Pill de upvotes: resaltada si recibió al menos uno
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(28.dp)
                .clip(CircleShape)
                .background(pillBg)
                .padding(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Upvotes",
                tint = pillContent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$upvotesCount",
                color = pillContent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }

        if (commentsCount != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = WarmGray,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (commentsCount == 1) "1 comentario" else "$commentsCount comentarios",
                color = WarmGray,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Ver hilo",
            tint = WarmGray,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun DeletedFeedbackBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF261D16),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF2E7D52), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )

            // Cruz para cerrar
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color(0xFFC7BDB5),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() },
            )
        }
    }
}

@Composable
private fun MyPostsMessageView(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    fillScreen: Boolean = true,
) {
    Column(
        modifier = (if (fillScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(PrimaryContainer, CircleShape),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
            textAlign = TextAlign.Center,
        )

        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }

}
