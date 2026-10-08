package com.barrioahorro.api.modules.business

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

const val UPLOADS_URL_PREFIX = "/uploads/"

/** Guarda las fotos en una carpeta del servidor. Se sirven públicamente bajo [UPLOADS_URL_PREFIX]. */
@Component
class PhotoStorage(
    @Value("\${app.uploads.dir}") uploadsDir: String,
) {
    val directory: Path = Paths.get(uploadsDir).toAbsolutePath().normalize()

    init {
        Files.createDirectories(directory)
    }

    /** Guarda la imagen con un nombre aleatorio y devuelve su url pública, o null si no es JPEG, PNG ni WebP. */
    fun store(bytes: ByteArray): String? {
        val extension = detectImageExtension(bytes) ?: return null
        val fileName = "${UUID.randomUUID()}.$extension"
        Files.write(directory.resolve(fileName), bytes)
        return UPLOADS_URL_PREFIX + fileName
    }

    fun delete(url: String) {
        val file = directory.resolve(url.removePrefix(UPLOADS_URL_PREFIX)).normalize()
        // Nunca borramos fuera de la carpeta de uploads.
        if (file.parent == directory) Files.deleteIfExists(file)
    }

    // Se mira el contenido real del archivo: el Content-Type lo manda el cliente y no es confiable.
    private fun detectImageExtension(bytes: ByteArray): String? {
        fun startsWith(vararg prefix: Int) =
            bytes.size >= prefix.size && prefix.indices.all { bytes[it] == prefix[it].toByte() }

        return when {
            startsWith(0xFF, 0xD8, 0xFF) -> "jpg"
            startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "png"
            bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
            else -> null
        }
    }
}
