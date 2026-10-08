package com.barrioahorro.api.modules.review.dto

import java.time.OffsetDateTime

data class ReviewResponse(
    val id: Long,
    val clienteId: Long,
    val correoCliente: String,
    val texto: String,
    val estrellas: Short,
    val createdAt: OffsetDateTime,
)
