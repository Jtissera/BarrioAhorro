package com.barrioahorro.app.ui.cuponera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.barrioahorro.app.R
import com.barrioahorro.app.di.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    fun closeSheet(afterClose: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
            showSheet = false
            afterClose()
        }
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
                .background(colorResource(R.color.primary_container))
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
                    color = colorResource(R.color.primary),
                )
            }

            if (fotoSubiendo) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = colorResource(R.color.primary),
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
                    .background(colorResource(R.color.primary))
                    .border(2.dp, colorResource(R.color.surface_card), CircleShape)
                    .clickable { showSheet = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasPhoto) Icons.Default.PhotoCamera else Icons.Default.Add,
                    contentDescription = "Agregar foto",
                    tint = colorResource(R.color.on_primary),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }

    if (showSheet && editable) {
        PhotoSourceSheet(
            sheetState = sheetState,
            onDismissRequest = { showSheet = false },
            onClose = { closeSheet() },
            onCamera = { closeSheet { openCamera() } },
            onGallery = { closeSheet { openGallery() } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colorResource(R.color.splash_background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Foto del producto",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary),
                    )
                    Text(
                        text = "Elegí una opción para agregar la imagen de tu producto",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.text_secondary),
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = colorResource(R.color.text_primary),
                    )
                }
            }

            PhotoSourceOption(
                icon = Icons.Default.PhotoCamera,
                title = "Sacar foto",
                subtitle = "Usá la cámara de tu celular",
                onClick = onCamera,
            )

            PhotoSourceOption(
                icon = Icons.Default.PhotoLibrary,
                title = "Elegir de la galería",
                subtitle = "Buscá en tus fotos guardadas",
                onClick = onGallery,
            )

            Button(
                onClick = onClose,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.stepper_cream),
                    contentColor = colorResource(R.color.text_primary),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Cancelar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PhotoSourceOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colorResource(R.color.surface_card),
        border = BorderStroke(1.dp, colorResource(R.color.stepper_border)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorResource(R.color.primary)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colorResource(R.color.on_primary),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.text_primary),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.text_secondary),
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.stepper_cream)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colorResource(R.color.text_primary),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
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