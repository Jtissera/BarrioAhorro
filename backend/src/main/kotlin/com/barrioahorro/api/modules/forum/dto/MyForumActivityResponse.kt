package com.barrioahorro.api.modules.forum.dto

import java.time.OffsetDateTime

data class MyForumActivityResponse(
    val puntos: Int,
    val publicaciones: List<ForumPostResponse>,
    val comentarios: List<MyForumCommentResponse>,
)

data class MyForumCommentResponse(
    val id: Long,
    val publicacionId: Long,
    val comentarioPadreId: Long?,
    val texto: String,
    val upvotesCount: Int,
    val publicacionTexto: String,
    val comercioNombre: String,
    val createdAt: OffsetDateTime,
)
