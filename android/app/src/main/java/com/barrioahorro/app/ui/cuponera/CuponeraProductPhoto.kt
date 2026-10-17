package com.barrioahorro.app.ui.cuponera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.barrioahorro.app.ui.screens.business.PhotoSourceSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuponeraProductPhoto(
    nombre: String,
    fotoUrl: String?,
    fotoSubiendo: Boolean,
    editable: Boolean,
    onPhotoCaptured: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSheet by rememberSaveable { mutableStateOf(false) }

    // Se guarda la ruta como String con rememberSaveable, por si la Activity se recrea mientras está abierta la cámara.
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingPath?.let { File(it) }
        pendingPath = null
        if (success && file != null && file.exists() && file.length() > 0L) {
            onPhotoCaptured(file)
        } else {
            file?.delete()
        }
    }

    // La galería devuelve una Uri: se copia a la caché para poder subirla igual que una foto de cámara.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { copyToCache(context, uri) }
                if (file != null) onPhotoCaptured(file)
            }
        }
    }

    fun openCamera() {
        val directory = File(context.cacheDir, "cuponera_photos").apply { mkdirs() }
        val file = File.createTempFile("producto_", ".jpg", directory)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        pendingPath = file.absolutePath
        cameraLauncher.launch(uri)
    }

    fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }


    val canTakePhoto = editable && !fotoSubiendo
    val hasPhoto = !fotoUrl.isNullOrBlank()

    val imageUrl = fotoUrl?.takeIf { it.isNotBlank() }?.let { url ->
        if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
        }
    }

    Box(modifier = modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryContainer)
                .clickable(enabled = canTakePhoto, onClickLabel = "Agregar foto") {
                    showSheet = true
                },
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Foto de $nombre",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    onError = { state ->
                        Log.e("CuponeraPhoto", "No se pudo cargar $imageUrl", state.result.throwable)
                    },
                )
            } else {
                Text(
                    text = nombre.trim().firstOrNull()?.uppercase() ?: "+",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }

            if (fotoSubiendo) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = Primary,
                    strokeWidth = 3.dp,
                )
            }
        }

        if (canTakePhoto) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Primary)
                    .border(2.dp, CardWhite, CircleShape)
                    .clickable { showSheet = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasPhoto) Icons.Default.PhotoCamera else Icons.Default.Add,
                    contentDescription = "Agregar foto",
                    tint = OnPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }

    if (showSheet && editable) {
        PhotoSourceSheet(
            title = "Foto del producto",
            subtitle = "Elegí una opción para agregar la imagen de tu producto",
            onDismiss = { showSheet = false },
            onTakePhoto = {
                showSheet = false
                openCamera()
            },
            onPickFromGallery = {
                showSheet = false
                openGallery()
            },
        )
    }
}

private fun copyToCache(context: Context, uri: Uri): File? {
    val directory = File(context.cacheDir, "cuponera_photos").apply { mkdirs() }
    val file = File.createTempFile("producto_", ".jpg", directory)
    return try {
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) {
            file.delete()
            null
        } else {
            input.use { source ->
                file.outputStream().use { target -> source.copyTo(target) }
            }
            file
        }
    } catch (e: Exception) {
        Log.e("CuponeraPhoto", "No se pudo leer la foto elegida", e)
        file.delete()
        null
    }
}