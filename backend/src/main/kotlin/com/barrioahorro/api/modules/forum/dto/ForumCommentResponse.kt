package com.barrioahorro.api.modules.forum.dto

import java.time.OffsetDateTime

data class ForumCommentResponse(
    val id: Long,
    val publicacionId: Long,
    val comentarioPadreId: Long?,
    val autorId: Long,
    val autorNombre: String,
    val texto: String,
    val upvotesCount: Int,
    val respuestasCount: Int = 0,
    val esAutorDePublicacion: Boolean,
    val esUsuarioActual: Boolean,
    val upvotedByMe: Boolean = false,
    val createdAt: OffsetDateTime,
)
