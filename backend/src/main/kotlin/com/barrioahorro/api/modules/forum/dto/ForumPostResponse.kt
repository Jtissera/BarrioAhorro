package com.barrioahorro.api.modules.forum.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class ForumPostResponse(
    val id: Long,
    val autorId: Long,
    val autorNombre: String,
    val comercioId: Long,
    val comercioNombre: String,
    val texto: String,
    val precioObservado: BigDecimal?,
    val latitud: Double?,
    val longitud: Double?,
    val upvotesCount: Int,
    val comentariosCount: Int,
    val createdAt: OffsetDateTime,
)
