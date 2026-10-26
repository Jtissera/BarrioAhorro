package com.barrioahorro.app.ui.screens.client.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.WarmGray

/**
 * Botón "⋯" con el menú de acciones sobre contenido propio del foro.
 * [circular] dibuja el botón de 32dp con fondo crema de Mis publicaciones;
 * si no, solo el ícono (como en los comentarios del hilo).
 */
@Composable
fun ForumOptionsMenu(
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

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
        }
    }
}
