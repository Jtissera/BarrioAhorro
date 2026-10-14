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
