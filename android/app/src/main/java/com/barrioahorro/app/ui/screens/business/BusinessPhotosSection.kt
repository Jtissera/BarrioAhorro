package com.barrioahorro.app.ui.screens.business

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val PHOTO_SIZE = 160.dp
private val photoShape = RoundedCornerShape(12.dp)
private val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

@Composable
fun BusinessPhotosSection(
    photos: List<PhotoUi>,
    isUpdating: Boolean,
    errorMessage: String?,
    onAddPhoto: (Uri) -> Unit,
    onReplacePhoto: (Long, Uri) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onMovePhoto: (Long, Int) -> Unit,
) {
    var photoToReplace by remember { mutableStateOf<Long?>(null) }
    var photoToDelete by remember { mutableStateOf<Long?>(null) }
    var lastMovedPhoto by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    // Al reordenar, la fila conserva su posición y la foto movida puede quedar fuera de la vista: la seguimos.
    LaunchedEffect(photos) {
        val index = photos.indexOfFirst { it.id == lastMovedPhoto }
        if (index >= 0) listState.animateScrollToItem(index)
        lastMovedPhoto = null
    }

    // El selector de fotos del sistema no necesita pedir permisos de almacenamiento.
    val addLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onAddPhoto)
    }
    val replaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val photoId = photoToReplace
        if (uri != null && photoId != null) onReplacePhoto(photoId, uri)
        photoToReplace = null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Fotos (${photos.size}/$MAX_BUSINESS_PHOTOS)", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "La primera es la portada de tu perfil.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                PhotoCard(
                    photo = photo,
                    isCover = index == 0,
                    enabled = !isUpdating,
                    canMoveLeft = index > 0,
                    canMoveRight = index < photos.lastIndex,
                    onMoveLeft = {
                        lastMovedPhoto = photo.id
                        onMovePhoto(photo.id, -1)
                    },
                    onMoveRight = {
                        lastMovedPhoto = photo.id
                        onMovePhoto(photo.id, 1)
                    },
                    onReplace = {
                        photoToReplace = photo.id
                        replaceLauncher.launch(imageOnly)
                    },
                    onDelete = { photoToDelete = photo.id },
                )
            }
            if (photos.size < MAX_BUSINESS_PHOTOS) {
                item {
                    AddPhotoTile(enabled = !isUpdating, onClick = { addLauncher.launch(imageOnly) })
                }
            }
        }

        if (isUpdating) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    photoToDelete?.let { photoId ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("¿Borrar esta foto?") },
            text = { Text("Va a dejar de mostrarse en tu perfil.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePhoto(photoId)
                    photoToDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun PhotoCard(
    photo: PhotoUi,
    isCover: Boolean,
    enabled: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.width(PHOTO_SIZE)) {
        Box {
            AsyncImage(
                model = photo.url,
                contentDescription = if (isCover) "Foto de portada" else "Foto del local",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(PHOTO_SIZE).clip(photoShape),
            )
            if (isCover) {
                Text(
                    text = "Portada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PhotoAction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mover antes", enabled && canMoveLeft, onMoveLeft)
            PhotoAction(Icons.Filled.SwapHoriz, "Reemplazar foto", enabled, onReplace)
            PhotoAction(Icons.Filled.Delete, "Borrar foto", enabled, onDelete)
            PhotoAction(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mover después", enabled && canMoveRight, onMoveRight)
        }
    }
}

@Composable
private fun PhotoAction(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun AddPhotoTile(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(PHOTO_SIZE)
            .clip(photoShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, photoShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Agregar foto", style = MaterialTheme.typography.bodySmall)
        }
    }
}
