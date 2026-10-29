package com.barrioahorro.app.ui.screens.client.business

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.CreamPillButton
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.TrackBeige
import com.barrioahorro.app.ui.theme.WarmGray

private const val MIN_COMMENT = 10
private const val MAX_COMMENT = 300

private val RATING_LABELS = listOf("Muy malo", "Malo", "Regular", "Bueno", "¡Excelente!")

/** Panel para escribir o editar la reseña propia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewSheet(
    businessName: String,
    categoryName: String,
    photoUrl: String?,
    initialRating: Int,
    initialComment: String,
    isEdit: Boolean,
    isSaving: Boolean,
    serverError: String?,
    onSave: (rating: Int, comment: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rating by rememberSaveable { mutableIntStateOf(initialRating) }
    var comment by rememberSaveable { mutableStateOf(initialComment) }
    val trimmed = comment.trim()
    val valid = rating in 1..5 && trimmed.length in MIN_COMMENT..MAX_COMMENT

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardWhite,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .background(Cream, CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetHeader(businessName, categoryName, photoUrl, isEdit, enabled = !isSaving, onClose = onDismiss)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEdit) "Editá tu reseña" else "¿Cómo fue tu experiencia en $businessName?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                Text(
                    text = if (isEdit) {
                        "Contale a tus vecinos cómo fue tu experiencia de compra."
                    } else {
                        "Tu experiencia ayuda a que otros vecinos elijan con confianza."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }

            Surface(shape = RoundedCornerShape(16.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (star == 1) "1 estrella" else "$star estrellas",
                                tint = if (star <= rating) Tertiary else TrackBeige,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !isSaving) { rating = star },
                            )
                        }
                    }
                    Text(
                        text = if (rating == 0) "Tocá las estrellas para calificar" else RATING_LABELS[rating - 1],
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (rating == 0) WarmGray else Ink,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tu opinión", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Ink)
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= MAX_COMMENT) comment = it },
                    enabled = !isSaving,
                    placeholder = {
                        Text("Contale a tus vecinos qué te pareció la atención, la calidad y los precios…", color = MutedLabel)
                    },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        disabledContainerColor = Cream,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${comment.length}/$MAX_COMMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (comment.length > MAX_COMMENT) ErrorRed else WarmGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            serverError?.let { ErrorHint(text = it) }

            Button(
                onClick = { if (!isSaving) onSave(rating, trimmed) },
                enabled = valid,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = Cream,
                    disabledContentColor = WarmGray,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        if (isEdit) Icons.Filled.Check else Icons.Outlined.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEdit) "Guardar cambios" else "Publicar reseña",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (isEdit) {
                TextButton(
                    onClick = onDelete,
                    enabled = !isSaving,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar reseña", color = Primary, fontWeight = FontWeight.Bold)
                }
            } else if (!valid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = WarmGray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Seleccioná al menos 1 estrella y escribí un comentario",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    businessName: String,
    categoryName: String,
    photoUrl: String?,
    isEdit: Boolean,
    enabled: Boolean,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(SecondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Outlined.Storefront, null, tint = Secondary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (isEdit) {
                Text("COMERCIO", style = MaterialTheme.typography.labelSmall, color = WarmGray)
            }
            Text(
                businessName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isEdit && categoryName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = BadgeOrange) {
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("• Comercio de barrio", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Cream)
                .clickable(enabled = enabled, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Ink, modifier = Modifier.size(18.dp))
        }
    }
}

/** Confirmación centrada, con un trazo rojo arriba como en el mockup. */
@Composable
internal fun DeleteReviewDialog(businessName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            shadowElevation = 24.dp,
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.padding(top = 10.dp)
                        .size(width = 56.dp, height = 4.dp)
                        .background(Primary, CircleShape),
                )
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).background(PrimaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Delete, null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "¿Eliminar tu reseña?",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu reseña ya no va a aparecer en el perfil de $businessName. Podés escribir otra cuando quieras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGray,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    PrimaryPillButton(text = "Eliminar", onClick = onConfirm, icon = Icons.Outlined.Delete, height = 52.dp)
                    Spacer(Modifier.height(10.dp))
                    CreamPillButton(text = "Cancelar", onClick = onDismiss, height = 52.dp)
                }
            }
        }
    }
}