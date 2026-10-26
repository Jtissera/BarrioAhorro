package com.barrioahorro.app.ui.screens.client.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorContainer
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.PlaceholderGray
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SurfaceBottomBar
import com.barrioahorro.app.ui.theme.TertiaryContainer
import com.barrioahorro.app.ui.theme.WarmGray

private val UpvotesHighlight = Color(0xFF745000)
private val InfoPillBg = Color(0xFFECE1D7)

/** Pantalla "Editar publicación" del mockup: el comercio es fijo; texto, precio y ubicación editables. */
@Composable
fun EditPostScreen(
    onNavigateBack: () -> Unit,
    onPostSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditPostViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onPostSaved()
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceBottomBar)
                    .statusBarsPadding()
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
                    text = "Editar publicación",
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontSize = 18.sp,
                )
            }
        },
        bottomBar = {
            if (state.post != null) {
                EditPostBottomBar(
                    isSaveEnabled = state.isSaveEnabled,
                    isSaving = state.isSaving,
                    errorMessage = state.errorMessage,
                    onCancel = onNavigateBack,
                    onSave = viewModel::save,
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) { padding ->
        val post = state.post
        when {
            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CircularProgressIndicator(color = Primary)
            }

            post == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text(
                    text = state.loadError ?: "No pudimos cargar la publicación",
                    color = WarmGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::loadPost,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Text("Reintentar")
                }
            }

            else -> Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                LinkedBusinessCard(post)

                EditCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FieldLabel(text = "¿Qué querés contar? *", color = Primary, modifier = Modifier.weight(1f))
                        Text(
                            text = "${state.text.length}/$FORUM_POST_MAX_LENGTH",
                            color = WarmGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 112.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Cream)
                            .padding(12.dp),
                    ) {
                        BasicTextField(
                            value = state.text,
                            onValueChange = viewModel::onTextChanged,
                            textStyle = TextStyle(color = Ink, fontSize = 14.sp, lineHeight = 20.sp),
                            cursorBrush = SolidColor(Primary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = "Tip: Mencioná calidad, frescura o formas de pago.",
                        color = WarmGray,
                        fontSize = 12.sp,
                    )
                }

                EditCard {
                    FieldLabel(text = "Precio de la oferta")
                    SingleLineCreamField(
                        value = state.price,
                        onValueChange = viewModel::onPriceChanged,
                        placeholder = "Ej: 1200",
                        keyboardType = KeyboardType.Number,
                        leading = {
                            Text(text = "$", color = WarmGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FieldLabel(text = "Ubicación o intersección")
                    SingleLineCreamField(
                        value = state.locationDescription,
                        onValueChange = viewModel::onLocationDescriptionChanged,
                        placeholder = "Ej: Yrigoyen y Pavón",
                        keyboardType = KeyboardType.Text,
                        leading = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = WarmGray,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }

                UpvotesKeptCard(upvotesCount = post.upvotesCount)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(InfoPillBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = WarmGray,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Los cambios son visibles al instante en el mapa del barrio",
                        color = WarmGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier, color: Color = WarmGray) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = modifier,
    )
}

@Composable
private fun LinkedBusinessCard(post: ForumPostDto) {
    EditCard {
        FieldLabel(text = "Comercio vinculado")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Cream)
                .padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(CardWhite, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = post.comercioNombre,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                post.comercioRubro?.takeIf { it.isNotBlank() }?.let { rubro ->
                    Text(text = rubro, color = WarmGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = WarmGray,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "El comercio asociado no se puede cambiar.", color = WarmGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SingleLineCreamField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    leading: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Cream)
            .padding(horizontal = 12.dp),
    ) {
        leading()
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = PlaceholderGray, fontSize = 14.sp, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                cursorBrush = SolidColor(Primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UpvotesKeptCard(upvotesCount: Int) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Cream)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(TertiaryContainer, CircleShape),
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = null,
                tint = UpvotesHighlight,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Conservás los upvotes recibidos",
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                text = buildAnnotatedString {
                    append("Esta publicación ya cuenta con ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = UpvotesHighlight)) {
                        append("$upvotesCount ${if (upvotesCount == 1) "recomendación" else "recomendaciones"}")
                    }
                    append(" de vecinos que no se perderán con los cambios.")
                },
                color = WarmGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun EditPostBottomBar(
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    // El error vive en la barra fija para que se vea aunque el formulario esté scrolleado.
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBottomBar)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        errorMessage?.let { message ->
            Text(
                text = message,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorContainer)
                    .padding(12.dp),
            )
        }
        EditPostActions(isSaveEnabled = isSaveEnabled, isSaving = isSaving, onCancel = onCancel, onSave = onSave)
    }
}

@Composable
private fun EditPostActions(
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onCancel,
            enabled = !isSaving,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Ink),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.height(56.dp),
        ) {
            Text(text = "Cancelar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Button(
            onClick = onSave,
            enabled = isSaveEnabled,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White,
                disabledContainerColor = Primary.copy(alpha = 0.5f),
                disabledContentColor = Color.White,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Guardar cambios", fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
            }
        }
    }
}
