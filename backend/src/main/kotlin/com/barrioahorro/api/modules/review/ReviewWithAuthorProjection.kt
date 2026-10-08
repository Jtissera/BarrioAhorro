package com.barrioahorro.api.modules.review

import java.time.OffsetDateTime

interface ReviewWithAuthorProjection {
    fun getId(): Long
    fun getClienteId(): Long
    fun getEmailCliente(): String
    fun getTexto(): String
    fun getEstrellas(): Short
    fun getCreatedAt(): OffsetDateTime
}