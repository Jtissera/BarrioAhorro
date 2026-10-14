
package com.barrioahorro.app.ui.cuponera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.barrioahorro.app.R
import com.barrioahorro.app.di.BASE_URL
import java.io.File

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
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            onPhotoCaptured(file)
        }
    }

    val imageUrl = fotoUrl?.let { url ->
        if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorResource(R.color.primary_container)),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Foto de $nombre",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
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

        if (editable && !fotoSubiendo) {
            Text(
                text = if (fotoUrl.isNullOrBlank()) "Sacar foto" else "Cambiar foto",
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.primary),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    val directory = File(
                        context.cacheDir,
                        "cuponera_photos",
                    ).apply { mkdirs() }

                    val file = File.createTempFile(
                        "producto_",
                        ".jpg",
                        directory,
                    )

                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )

                    pendingFile = file
                    cameraLauncher.launch(uri)
                },
            )
        }
    }
}
