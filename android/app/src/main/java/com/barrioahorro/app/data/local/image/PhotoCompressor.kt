package com.barrioahorro.app.data.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SIDE_PX = 1600
private const val JPEG_QUALITY = 85

/** Achica la foto elegida y la pasa a JPEG para no subir archivos de varios MB. */
class PhotoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun compress(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = runCatching { decode(uri) }.getOrNull() ?: return@withContext null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private fun decode(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder además respeta la rotación EXIF de las fotos de la cámara.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = scaleFor(info.size.width, info.size.height)
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeLegacy(uri)
        }

    private fun decodeLegacy(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE_PX) sampleSize *= 2
        val sampled = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: return null

        val scale = scaleFor(sampled.width, sampled.height)
        if (scale >= 1f) return sampled
        return Bitmap.createScaledBitmap(
            sampled,
            (sampled.width * scale).roundToInt(),
            (sampled.height * scale).roundToInt(),
            true,
        )
    }

    private fun scaleFor(width: Int, height: Int): Float =
        minOf(1f, MAX_SIDE_PX.toFloat() / max(width, height))
}
