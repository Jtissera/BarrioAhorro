package com.barrioahorro.app.ui.screens.business

import com.barrioahorro.app.data.remote.dto.PhotoResponseDto

/** Tiene que coincidir con el límite del backend. */
const val MAX_BUSINESS_PHOTOS = 5

data class PhotoUi(val id: Long, val url: String)

/** El backend devuelve rutas relativas ("/uploads/x.jpg"); las combinamos con la url base de la API. */
fun resolvePhotoUrl(baseUrl: String, path: String): String =
    if (path.startsWith("http://") || path.startsWith("https://")) {
        path
    } else {
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')
    }

fun List<PhotoResponseDto>.toPhotoUi(baseUrl: String): List<PhotoUi> =
    sortedBy { it.orden }.map { PhotoUi(id = it.id, url = resolvePhotoUrl(baseUrl, it.url)) }

/** Devuelve la lista con la foto desplazada [offset] lugares, o null si no se puede mover. */
fun List<PhotoUi>.moved(photoId: Long, offset: Int): List<PhotoUi>? {
    val from = indexOfFirst { it.id == photoId }
    val to = from + offset
    if (from == -1 || to !in indices) return null
    return toMutableList().apply { add(to, removeAt(from)) }
}
