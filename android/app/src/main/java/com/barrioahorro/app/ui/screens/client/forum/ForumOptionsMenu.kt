package com.barrioahorro.app.ui.screens.client.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

/**
 * Botón "⋯" con el menú de acciones sobre contenido propio del foro.
 * [circular] dibuja el botón de 32dp con fondo crema de Mis publicaciones;
 * si no, solo el ícono (como en los comentarios del hilo).
 */
@Composable
fun ForumOptionsMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    deleteDialogTitle: String = "¿Eliminar publicación?",
    deleteDialogDescription: String = "Se dejarán de mostrar sus comentarios. Los puntos que ya ganaste no se pierden.",
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = deleteDialogTitle,
            description = deleteDialogDescription,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (circular) 32.dp else 24.dp)
                .clip(CircleShape)
                .then(if (circular) Modifier.background(Cream) else Modifier)
                .clickable { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Opciones",
                tint = WarmGray,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = CardWhite,
        ) {
            DropdownMenuItem(
                text = {
                    Text(text = "Editar", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )

            DropdownMenuItem(
                text = {
                    Text(text = "Eliminar", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    expanded = false
                    showDeleteDialog = true
                },
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Primary, RoundedCornerShape(2.dp)),
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFFEAEA), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary,
                    ),
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cream,
                        contentColor = Ink,
                    ),
                ) {
                    Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}