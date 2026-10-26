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
    val texto: String,
    val precioObservado: Double?,
    val latitud: Double?,
    val longitud: Double?,
    val comentariosCount: Int,
    val createdAt: String,
)
