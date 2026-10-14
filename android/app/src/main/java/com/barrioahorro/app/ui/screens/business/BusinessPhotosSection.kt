package com.barrioahorro.app.ui.screens.business

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.barrioahorro.app.R
import com.barrioahorro.app.ui.components.profile.CreamPillButton
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import java.io.File

private val COVER_HEIGHT = 220.dp
private val TILE_HEIGHT = 150.dp
private val photoShape = RoundedCornerShape(12.dp)
private val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
private val DarkUmber = Color(0xFF201B15)

/** Which photo the "Cambiar foto" sheet acts on: a new one or the replacement of an existing one. */
private sealed interface PhotoPickTarget {
    data object Add : PhotoPickTarget
    data class Replace(val photoKey: String) : PhotoPickTarget
}

@Composable
fun BusinessPhotosSection(
    photos: List<DraftPhoto>,
    isUpdating: Boolean,
    errorMessage: String?,
    onAddPhoto: (Uri) -> Unit,
    onReplacePhoto: (String, Uri) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onSetCover: (String) -> Unit,
    businessName: String = "",
) {
    var pickTarget by remember { mutableStateOf<PhotoPickTarget?>(null) }
    var pendingReplace by remember { mutableStateOf<String?>(null) }
    var photoToDelete by remember { mutableStateOf<DraftPhoto?>(null) }

    // The system photo picker doesn't need storage permissions.
    val addLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onAddPhoto)
    }
    val replaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val photoKey = pendingReplace
        if (uri != null && photoKey != null) onReplacePhoto(photoKey, uri)
        pendingReplace = null
    }

    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    // Saveable so the capture survives the activity being recreated while the camera app is open.
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cameraReplaceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        val replaceKey = cameraReplaceKey
        if (saved && uri != null) {
            if (replaceKey != null) onReplacePhoto(replaceKey, uri) else onAddPhoto(uri)
        }
        cameraUri = null
        cameraReplaceKey = null
    }

    fun launchCamera() {
        val uri = runCatching { createCameraImageUri(context) }.getOrNull()
        if (uri == null) {
            cameraError = "No pudimos preparar la cámara, probá de nuevo"
            cameraReplaceKey = null
            return
        }
        cameraUri = uri
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            cameraError = "No encontramos una app de cámara en tu celular"
            cameraUri = null
            cameraReplaceKey = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            cameraError = "Necesitamos permiso para usar la cámara. Podés elegir una foto de la galería."
            cameraReplaceKey = null
        }
    }

    fun takePhoto(target: PhotoPickTarget) {
        cameraError = null
        cameraReplaceKey = (target as? PhotoPickTarget.Replace)?.photoKey
        // TakePicture only needs the runtime permission when the app (or a merged library) declares CAMERA.
        val needsPermission = context.declaresPermission(Manifest.permission.CAMERA) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA) else launchCamera()
    }

    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val canAdd = photos.size < MAX_BUSINESS_PHOTOS

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Foto de portada del comercio", style = MaterialTheme.typography.titleMedium, color = textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Es la imagen principal que verán tus vecinos cuando busquen tu negocio en el barrio.",
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary,
        )
        Spacer(modifier = Modifier.height(16.dp))

        val cover = photos.firstOrNull()
        if (cover != null) {
            CoverPhotoCard(
                photo = cover,
                businessName = businessName,
                enabled = !isUpdating,
                onChange = { pickTarget = PhotoPickTarget.Replace(cover.key) },
                onRemove = { photoToDelete = cover },
            )
        } else {
            EmptyCoverCard(enabled = !isUpdating, onClick = { pickTarget = PhotoPickTarget.Add })
        }

        if (photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Galería del comercio", style = MaterialTheme.typography.titleMedium, color = textPrimary)
                    Text(
                        text = "${photos.size} de $MAX_BUSINESS_PHOTOS fotos cargadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PhotoGrid(
                photos = photos,
                canAdd = canAdd,
                enabled = !isUpdating,
                onSetCover = { onSetCover(it.key) },
                onReplace = { pickTarget = PhotoPickTarget.Replace(it.key) },
                onDelete = { photoToDelete = it },
                onAdd = { pickTarget = PhotoPickTarget.Add },
            )
        }

        if (isUpdating) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                color = colorResource(R.color.primary),
                trackColor = colorResource(R.color.surface_muted),
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
            )
        }
        (errorMessage ?: cameraError)?.let {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorHint(text = it)
        }

        Spacer(modifier = Modifier.height(24.dp))
        CoverTipsCard()
    }

    pickTarget?.let { target ->
        PhotoSourceSheet(
            title = if (target is PhotoPickTarget.Add) "Agregar foto del comercio" else "Cambiar foto del comercio",
            onDismiss = { pickTarget = null },
            onTakePhoto = if (hasCamera) {
                {
                    takePhoto(target)
                    pickTarget = null
                }
            } else {
                null
            },
            onPickFromGallery = {
                cameraError = null
                when (target) {
                    PhotoPickTarget.Add -> addLauncher.launch(imageOnly)
                    is PhotoPickTarget.Replace -> {
                        pendingReplace = target.photoKey
                        replaceLauncher.launch(imageOnly)
                    }
                }
                pickTarget = null
            },
        )
    }

    photoToDelete?.let { photo ->
        RemovePhotoDialog(
            photoUrl = photo.url,
            onConfirm = {
                onDeletePhoto(photo.key)
                photoToDelete = null
            },
            onDismiss = { photoToDelete = null },
        )
    }
}

@Composable
private fun CoverPhotoCard(
    photo: DraftPhoto,
    businessName: String,
    enabled: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    val onPrimary = colorResource(R.color.on_primary)

    Surface(
        shape = photoShape,
        color = colorResource(R.color.surface_card),
        border = BorderStroke(1.dp, colorResource(R.color.outline_variant).copy(alpha = 0.3f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(COVER_HEIGHT).background(colorResource(R.color.surface_muted))) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = "Foto de portada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.5f to Color.Transparent,
                                1f to DarkUmber.copy(alpha = 0.5f),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(colorResource(R.color.secondary), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = onPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Foto actual / Portada", style = MaterialTheme.typography.labelSmall, color = onPrimary)
                }
                if (businessName.isNotBlank()) {
                    Text(
                        text = businessName,
                        style = MaterialTheme.typography.labelSmall,
                        color = onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .widthIn(max = 220.dp)
                            .background(DarkUmber.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryPillButton(
                    text = "Cambiar foto",
                    onClick = onChange,
                    enabled = enabled,
                    icon = Icons.Outlined.PhotoCamera,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onRemove,
                    enabled = enabled,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, colorResource(R.color.outline_variant)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.error)),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quitar foto", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun EmptyCoverCard(enabled: Boolean, onClick: () -> Unit) {
    AddPhotoTile(
        label = "Agregar foto de portada",
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(COVER_HEIGHT),
    )
}

/** Two-column gallery. Every photo can be replaced or removed, and any other photo can become the cover (the first one). */
@Composable
private fun PhotoGrid(
    photos: List<DraftPhoto>,
    canAdd: Boolean,
    enabled: Boolean,
    onSetCover: (DraftPhoto) -> Unit,
    onReplace: (DraftPhoto) -> Unit,
    onDelete: (DraftPhoto) -> Unit,
    onAdd: () -> Unit,
) {
    // Non-lazy grid: it lives inside the screen's LazyColumn and never holds more than MAX_BUSINESS_PHOTOS items.
    val cells: List<DraftPhoto?> = if (canAdd) photos + null else photos
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { columnIndex, photo ->
                    val index = rowIndex * 2 + columnIndex
                    val cellModifier = Modifier.weight(1f).height(TILE_HEIGHT)
                    if (photo == null) {
                        AddPhotoTile(label = "Agregar foto", enabled = enabled, onClick = onAdd, modifier = cellModifier)
                    } else {
                        PhotoTile(
                            photo = photo,
                            isCover = index == 0,
                            enabled = enabled,
                            onSetCover = { onSetCover(photo) },
                            onReplace = { onReplace(photo) },
                            onDelete = { onDelete(photo) },
                            modifier = cellModifier,
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photo: DraftPhoto,
    isCover: Boolean,
    enabled: Boolean,
    onSetCover: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeText = colorResource(R.color.surface_secondary)
    Box(
        modifier = modifier
            .shadow(1.dp, photoShape)
            .clip(photoShape)
            .background(colorResource(R.color.surface_muted)),
    ) {
        AsyncImage(
            model = photo.url,
            contentDescription = if (isCover) "Foto de portada" else "Foto del local",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isCover) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0xCC352F29), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = colorResource(R.color.tertiary), modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Portada", style = MaterialTheme.typography.labelSmall, color = badgeText)
            }
        } else {
            SetCoverChip(enabled = enabled, onClick = onSetCover, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TileAction(Icons.Outlined.PhotoCamera, "Reemplazar foto", enabled, onReplace)
            TileAction(Icons.Outlined.Delete, "Quitar foto", enabled, onDelete)
        }
    }
}

/** "Usar como portada" pill shown on every gallery photo that is not the cover. */
@Composable
private fun SetCoverChip(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content = colorResource(R.color.text_primary)
    val tint = if (enabled) content else content.copy(alpha = 0.35f)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.9f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Usar como portada", style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun TileAction(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = colorResource(R.color.text_primary)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.9f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AddPhotoTile(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val textSecondary = colorResource(R.color.text_secondary)
    Column(
        modifier = modifier
            .clip(photoShape)
            .background(colorResource(R.color.surface_secondary))
            .border(1.dp, colorResource(R.color.outline_variant), photoShape)
            .clickable(enabled = enabled, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(colorResource(R.color.splash_background), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = textSecondary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = textSecondary)
    }
}

@Composable
private fun CoverTipsCard() {
    val textSecondary = colorResource(R.color.text_secondary)
    val tips = listOf(
        "Sacá la foto de día con buena luz natural sobre el frente.",
        "Mostrá el cartel del negocio y la mercadería fresca visible.",
        "Evitá imágenes oscuras, borrosas o con reflejos fuertes.",
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorResource(R.color.splash_background),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(32.dp).background(colorResource(R.color.tertiary_container), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = colorResource(R.color.text_primary),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Consejos para tu portada",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(R.color.text_primary),
                )
                Spacer(modifier = Modifier.height(4.dp))
                tips.forEach { tip ->
                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = colorResource(R.color.secondary),
                            modifier = Modifier.padding(top = 2.dp).size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = tip, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }
            }
        }
    }
}

/** Creates an empty JPEG in cacheDir/camera and returns a content Uri the camera app can write to. */
private fun createCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("business_photo_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun Context.declaresPermission(permission: String): Boolean = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    }
    info.requestedPermissions?.contains(permission) == true
}.getOrDefault(false)

/** Bottom sheet from the "Agregar foto" frame: take a photo with the camera (when available) or pick one from the gallery. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    title: String,
    onDismiss: () -> Unit,
    onTakePhoto: (() -> Unit)?,
    onPickFromGallery: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colorResource(R.color.surface_card),
        scrimColor = DarkUmber.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .background(colorResource(R.color.splash_background), CircleShape),
            )
        },
    ) {
        PhotoSourceSheetContent(
            title = title,
            onDismiss = onDismiss,
            onTakePhoto = onTakePhoto,
            onPickFromGallery = onPickFromGallery,
        )
    }
}

@Composable
private fun PhotoSourceSheetContent(
    title: String,
    onDismiss: () -> Unit,
    onTakePhoto: (() -> Unit)?,
    onPickFromGallery: () -> Unit,
) {
    val textPrimary = colorResource(R.color.text_primary)
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = textPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.splash_background))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = textPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Elegí una opción para actualizar la imagen de tu negocio",
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(R.color.text_secondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (onTakePhoto != null) {
            PhotoSourceOption(
                icon = Icons.Outlined.PhotoCamera,
                title = "Sacar foto",
                subtitle = "Usá la cámara de tu celular",
                onClick = onTakePhoto,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        PhotoSourceOption(
            icon = Icons.Filled.PhotoLibrary,
            title = "Elegir de la galería",
            subtitle = "Buscá en tus fotos guardadas",
            onClick = onPickFromGallery,
        )
        Spacer(modifier = Modifier.height(20.dp))
        CreamPillButton(text = "Cancelar", onClick = onDismiss)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PhotoSourceOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorResource(R.color.surface_card),
        shadowElevation = 1.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(colorResource(R.color.primary), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colorResource(R.color.on_primary), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.text_primary),
                )
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colorResource(R.color.text_secondary))
            }
            Box(
                modifier = Modifier.size(32.dp).background(colorResource(R.color.splash_background), CircleShape),
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

/** "¿Quitar esta foto?" confirmation, with a thumbnail of the photo being removed. */
@Composable
private fun RemovePhotoDialog(photoUrl: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        RemovePhotoDialogContent(photoUrl = photoUrl, onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

@Composable
private fun RemovePhotoDialogContent(photoUrl: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colorResource(R.color.surface_card),
        shadowElevation = 24.dp,
        modifier = Modifier.widthIn(max = 340.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(48.dp).background(colorResource(R.color.primary_container), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = colorResource(R.color.primary), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¿Quitar esta foto?",
                style = MaterialTheme.typography.titleLarge,
                color = colorResource(R.color.text_primary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta foto ya no se mostrará en tu perfil público ni en tus ofertas. Podés volver a subirla cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.text_secondary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 80.dp)
                    .shadow(1.dp, photoShape)
                    .clip(photoShape)
                    .background(colorResource(R.color.surface_muted)),
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto a quitar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryPillButton(text = "Quitar foto", onClick = onConfirm, icon = Icons.Outlined.Delete, height = 50.dp)
            Spacer(modifier = Modifier.height(12.dp))
            CreamPillButton(text = "Cancelar", onClick = onDismiss, height = 50.dp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7F7F7F)
@Composable
private fun RemovePhotoDialogPreview() {
    RemovePhotoDialogContent(photoUrl = null, onConfirm = {}, onDismiss = {})
}

@Preview(showBackground = true)
@Composable
private fun PhotoSourceSheetPreview() {
    PhotoSourceSheetContent(title = "Cambiar foto del comercio", onDismiss = {}, onTakePhoto = {}, onPickFromGallery = {})
}
