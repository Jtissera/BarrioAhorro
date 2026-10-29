package com.barrioahorro.api.modules.forum.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class ForumPostResponse(
    val id: Long,
    val autorId: Long,
    val autorNombre: String,
    val comercioId: Long,
    val comercioNombre: String,
    val comercioRubro: String? = null,
    val texto: String,
    val precioObservado: BigDecimal?,
    val direccion: String? = null,
    val latitud: Double?,
    val longitud: Double?,
    val upvotesCount: Int,
    val comentariosCount: Int,
    val esUsuarioActual: Boolean = false,
    val upvotedByMe: Boolean = false,
    val createdAt: OffsetDateTime,
)
