package com.barrioahorro.app.data.forum

data class CreateForumPostDto(
    val comercioId: Long,
    val texto: String,
    val precioObservado: Double? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
)

data class ForumPostDto(
    val id: Long,
    val autorId: Long,
    val autorNombre: String,
    val comercioId: Long,
    val comercioNombre: String,
    val comercioRubro: String? = null,
    val texto: String,
    val precioObservado: Double?,
    val direccion: String? = null,
    val latitud: Double?,
    val longitud: Double?,
    val upvotesCount: Int,
    val comentariosCount: Int,
    val esUsuarioActual: Boolean = false,
    val createdAt: String,
)

/** Estado completo del formulario: precio o dirección en null los borran. */
data class UpdateForumPostDto(
    val texto: String,
    val precioObservado: Double? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
)

data class CreateCommentDto(
    val texto: String,
    val comentarioPadreId: Long? = null,
)

data class UpdateCommentDto(
    val texto: String,
)

data class ForumCommentDto(
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
    val createdAt: String,
)

data class MyForumCommentDto(
    val id: Long,
    val publicacionId: Long,
    val comentarioPadreId: Long?,
    val texto: String,
    val upvotesCount: Int,
    val publicacionTexto: String,
    val comercioNombre: String,
    val createdAt: String,
)

data class MyForumActivityDto(
    val puntos: Int = 0,
    val publicaciones: List<ForumPostDto>,
    val comentarios: List<MyForumCommentDto>,
)
