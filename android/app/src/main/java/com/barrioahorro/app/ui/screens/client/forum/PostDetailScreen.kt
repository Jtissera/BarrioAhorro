package com.barrioahorro.app.ui.screens.client.forum

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.forum.ForumCommentDto
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BorderGray
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.PlaceholderGray
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

@Composable
fun PostDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedCommentIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isThreadView = state.focusedComment != null
    BackHandler(enabled = state.focusedCommentStack.isNotEmpty()) {
        viewModel.navigateUpComment()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Cream,
        bottomBar = {
            CommentInputBottomBar(
                text = state.commentText,
                onTextChanged = viewModel::onCommentTextChanged,
                replyingTo = state.replyingTo,
                focusedComment = state.focusedComment,
                onCancelReply = viewModel::cancelReply,
                isSendEnabled = state.isSendEnabled,
                isSending = state.isSendingComment,
                onSend = viewModel::sendComment,
            )
        },
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ToldoConFranjas()

            // Header con flecha atrás y título dinámico
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                IconButton(
                    onClick = {
                        if (state.focusedCommentStack.isNotEmpty()) {
                            viewModel.navigateUpComment()
                        } else {
                            onNavigateBack()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Ink,
                    )
                }
                Text(
                    text = if (isThreadView) "Hilo de comentarios" else "Publicación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontSize = 20.sp,
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (isThreadView) {
                val focusedComment = state.focusedComment!!
                val directReplies = state.comments.filter { it.comentarioPadreId == focusedComment.id }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Botón para volver atrás en el hilo o a la publicación
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.navigateUpComment() }
                                .padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.focusedCommentStack.size > 1) "Volver al comentario anterior" else "Volver a la publicación",
                                color = Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Tarjeta destacada del comentario principal (enfocado)
                    item {
                        FocusedPrincipalCommentCard(
                            comment = focusedComment,
                            replyCount = directReplies.size,
                            onReply = { viewModel.onReplyToComment(focusedComment) },
                        )
                    }

                    // Título de sección de respuestas
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                        ) {
                            Text(
                                text = "Respuestas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontSize = 18.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${directReplies.size} ${if (directReplies.size == 1) "respuesta" else "respuestas"}",
                                color = WarmGray,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    if (directReplies.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = WarmGray,
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Todavía no hay respuestas",
                                        color = Ink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sé el primero en responder a este comentario.",
                                        color = WarmGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    } else {
                        directReplies.forEach { child ->
                            val subReplies = state.comments.filter { it.comentarioPadreId == child.id }
                            val childReplyCount = maxOf(child.respuestasCount, subReplies.size)

                            item(key = "thread_reply_${child.id}") {
                                CommentRowItem(
                                    comment = child,
                                    replyCount = childReplyCount,
                                    onReply = { viewModel.onReplyToComment(child) },
                                    onOpenThread = { viewModel.focusComment(child.id) },
                                )
                            }

                            if (subReplies.isNotEmpty()) {
                                val firstSub = subReplies.first()
                                val firstSubCount = maxOf(firstSub.respuestasCount, state.comments.count { it.comentarioPadreId == firstSub.id })
                                item(key = "thread_sub_preview_${firstSub.id}") {
                                    ReplyCommentRowItem(
                                        comment = firstSub,
                                        replyCount = firstSubCount,
                                        onReply = { viewModel.onReplyToComment(firstSub) },
                                        onOpenThread = { viewModel.focusComment(firstSub.id) },
                                    )
                                }

                                if (subReplies.size > 1) {
                                    val moreCount = subReplies.size - 1
                                    item(key = "thread_sub_more_${child.id}") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(start = 34.dp, top = 0.dp, bottom = 4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { viewModel.focusComment(child.id) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(16.dp)
                                                    .height(1.dp)
                                                    .background(WarmGray.copy(alpha = 0.5f)),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ver $moreCount ${if (moreCount == 1) "respuesta más" else "respuestas más"}",
                                                color = Primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                val rootComments = state.comments.filter { it.comentarioPadreId == null }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 1. Tarjeta de la publicación principal
                    state.post?.let { post ->
                        item {
                            PostDetailHeaderCard(post = post)
                        }

                        // 2. Título de sección de comentarios (ej: "Comentarios  5 comentarios")
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                            ) {
                                Text(
                                    text = "Comentarios",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    fontSize = 18.sp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${state.comments.size} comentarios",
                                    color = WarmGray,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        // 3. Comentarios e hilos anidados (Nivel 1 y Nivel 2)
                        rootComments.forEach { rootComment ->
                            val replies = state.comments.filter { it.comentarioPadreId == rootComment.id }
                            val replyCount = maxOf(rootComment.respuestasCount, replies.size)
                            val isExpanded = expandedCommentIds.contains(rootComment.id)
                            val visibleReplies = if (isExpanded) replies else replies.take(1)
                            val hiddenCount = replies.size - visibleReplies.size

                            item(key = "root_${rootComment.id}") {
                                CommentRowItem(
                                    comment = rootComment,
                                    replyCount = replyCount,
                                    onReply = {
                                        viewModel.onReplyToComment(rootComment)
                                        expandedCommentIds = expandedCommentIds + rootComment.id
                                    },
                                    onOpenThread = {
                                        viewModel.focusComment(rootComment.id)
                                    },
                                )
                            }

                            visibleReplies.forEach { reply ->
                                val subReplies = state.comments.filter { it.comentarioPadreId == reply.id }
                                val subReplyCount = maxOf(reply.respuestasCount, subReplies.size)

                                item(key = "reply_${reply.id}") {
                                    ReplyCommentRowItem(
                                        comment = reply,
                                        replyCount = subReplyCount,
                                        onReply = { viewModel.onReplyToComment(reply) },
                                        onOpenThread = { viewModel.focusComment(reply.id) },
                                    )
                                }
                            }

                            if (hiddenCount > 0) {
                                item(key = "expand_${rootComment.id}") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(start = 34.dp, top = 0.dp, bottom = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                viewModel.focusComment(rootComment.id)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(1.dp)
                                                .background(WarmGray.copy(alpha = 0.5f)),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ver $hiddenCount ${if (hiddenCount == 1) "respuesta más" else "respuestas más"}",
                                            color = Primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            } else if (isExpanded && replies.size > 1) {
                                item(key = "collapse_${rootComment.id}") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(start = 34.dp, top = 0.dp, bottom = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                expandedCommentIds = expandedCommentIds - rootComment.id
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(1.dp)
                                                .background(WarmGray.copy(alpha = 0.5f)),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ocultar respuestas",
                                            color = WarmGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailHeaderCard(post: ForumPostDto) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        color = YellowAccent,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Header
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

                // Comercio
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

                // Texto
                Text(
                    text = post.texto,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                // Badges opcionales
                if (post.precioObservado != null || (post.latitud != null && post.longitud != null)) {
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

                        if (post.latitud != null && post.longitud != null) {
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
                                    text = "Visto en ${post.comercioNombre}",
                                    color = SageGreenText,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                // Footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightSalmon)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "↑",
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.upvotesCount}",
                            fontWeight = FontWeight.Bold,
                            color = Primary,
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
                            text = "${post.comentariosCount} comentarios",
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
private fun FocusedPrincipalCommentCard(
    comment: ForumCommentDto,
    replyCount: Int,
    onReply: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        color = Primary,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val avatarBg = when {
                        comment.esUsuarioActual -> Color(0xFFFFF3E0)
                        comment.esAutorDePublicacion -> Color(0xFFFFEBEE)
                        else -> Color(0xFFE8F5E9)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(avatarBg, CircleShape),
                    ) {
                        Text(
                            text = if (comment.esUsuarioActual) "TU" else getInitials(comment.autorNombre),
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 13.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (comment.esUsuarioActual) "Vos" else comment.autorNombre,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontSize = 14.sp,
                            )
                            if (comment.esAutorDePublicacion) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEDE6))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "Autor",
                                        color = Primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            } else if (comment.esUsuarioActual) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFF3E0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "Vos",
                                        color = Color(0xFFE65100),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Text(
                            text = formatRelativeTime(comment.createdAt),
                            color = WarmGray,
                            fontSize = 12.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LightSalmon)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Comentario principal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                    }
                }

                // Texto del comentario
                Text(
                    text = comment.texto,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                // Footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7F3EE))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "↑",
                            fontWeight = FontWeight.Bold,
                            color = WarmGray,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.upvotesCount}",
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGray,
                            fontSize = 12.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightSalmon)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$replyCount ${if (replyCount == 1) "respuesta" else "respuestas"}",
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Responder",
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onReply)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRowItem(
    comment: ForumCommentDto,
    replyCount: Int,
    onReply: () -> Unit,
    onOpenThread: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenThread),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Header del comentario: Avatar + Nombre + Badges + Fecha
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val avatarBg = when {
                    comment.esUsuarioActual -> Color(0xFFFFF3E0)
                    comment.esAutorDePublicacion -> Color(0xFFFFEBEE)
                    else -> Color(0xFFE8F5E9)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(avatarBg, CircleShape),
                ) {
                    Text(
                        text = if (comment.esUsuarioActual) "TU" else getInitials(comment.autorNombre),
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontSize = 11.sp,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (comment.esUsuarioActual) "Vos" else comment.autorNombre,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontSize = 13.sp,
                    )

                    if (comment.esAutorDePublicacion) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFEDE6))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Autor",
                                color = Primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else if (comment.esUsuarioActual) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Vos",
                                color = Color(0xFFE65100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(
                        text = " • ${formatRelativeTime(comment.createdAt)}",
                        color = WarmGray,
                        fontSize = 12.sp,
                    )
                }

                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Opciones",
                    tint = WarmGray,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Texto del comentario
            Text(
                text = comment.texto,
                color = Ink,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            // Footer: Upvotes, Contador de respuestas y botón Responder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "↑",
                        fontWeight = FontWeight.Bold,
                        color = WarmGray,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comment.upvotesCount}",
                        color = WarmGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (replyCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF7F3EE))
                            .clickable(onClick = onOpenThread)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$replyCount ${if (replyCount == 1) "respuesta" else "respuestas"}",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = "Responder",
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onReply)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ReplyCommentRowItem(
    comment: ForumCommentDto,
    replyCount: Int,
    onReply: () -> Unit,
    onOpenThread: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Línea vertical conectora en tono salmón/coral suave (como en Figma)
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 10.dp)
                .width(2.dp)
                .height(44.dp)
                .background(Color(0xFFE8D0C8), RoundedCornerShape(1.dp)),
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenThread),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Header del comentario hijo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val avatarBg = when {
                        comment.esUsuarioActual -> Color(0xFFFFF3E0)
                        comment.esAutorDePublicacion -> Color(0xFFFFEBEE)
                        else -> Color(0xFFE8F5E9)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(avatarBg, CircleShape),
                    ) {
                        Text(
                            text = if (comment.esUsuarioActual) "TU" else getInitials(comment.autorNombre),
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 11.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (comment.esUsuarioActual) "Vos" else comment.autorNombre,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 13.sp,
                        )

                        if (comment.esAutorDePublicacion) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFEDE6))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "Autor",
                                    color = Primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        } else if (comment.esUsuarioActual) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFF3E0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "Vos",
                                        color = Color(0xFFE65100),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                        Text(
                            text = " • ${formatRelativeTime(comment.createdAt)}",
                            color = WarmGray,
                            fontSize = 12.sp,
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Opciones",
                        tint = WarmGray,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Texto del comentario
                Text(
                    text = comment.texto,
                    color = Ink,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )

                // Footer: Upvotes, badge de respuestas y botón Responder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↑",
                            fontWeight = FontWeight.Bold,
                            color = WarmGray,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.upvotesCount}",
                            color = WarmGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (replyCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF7F3EE))
                                .clickable(onClick = onOpenThread)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$replyCount ${if (replyCount == 1) "respuesta" else "respuestas"}",
                                color = Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(
                        text = "Responder",
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onReply)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInputBottomBar(
    text: String,
    onTextChanged: (String) -> Unit,
    replyingTo: ForumCommentDto?,
    focusedComment: ForumCommentDto?,
    onCancelReply: () -> Unit,
    isSendEnabled: Boolean,
    isSending: Boolean,
    onSend: () -> Unit,
) {
    Surface(
        color = Cream,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Banner cuando se está respondiendo a un comentario específico
            if (replyingTo != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFBF3EB))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Respondiendo a ${if (replyingTo.esUsuarioActual) "Vos" else replyingTo.autorNombre}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }

                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar respuesta",
                            tint = WarmGray,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            } else if (focusedComment != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFBF4ED))
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "En el hilo de ${if (focusedComment.esUsuarioActual) "tu comentario" else focusedComment.autorNombre}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarmGray,
                    )
                }
            }

            // Fila de entrada de texto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // Avatar "TU"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFE0B2), CircleShape),
                ) {
                    Text(
                        text = "TU",
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontSize = 12.sp,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Campo de texto con forma de píldora
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardWhite)
                        .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    val placeholderText = when {
                        replyingTo != null -> "Respondiendo a ${if (replyingTo.esUsuarioActual) "Vos" else replyingTo.autorNombre}..."
                        focusedComment != null -> "Responder a ${if (focusedComment.esUsuarioActual) "Vos" else focusedComment.autorNombre}..."
                        else -> "Escribí un comentario..."
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (text.isEmpty()) {
                            Text(
                                text = placeholderText,
                                color = PlaceholderGray,
                                fontSize = 14.sp,
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChanged,
                            singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                            cursorBrush = SolidColor(Primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botón enviar circular
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSendEnabled) Primary else Color(0xFFE59A8E))
                        .clickable(enabled = isSendEnabled, onClick = onSend),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar comentario",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
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
