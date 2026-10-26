package com.barrioahorro.api.modules.forum

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "foro_comentarios")
class ForumCommentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "publicacion_id", nullable = false)
    val publicacionId: Long,

    @Column(name = "comentario_padre_id")
    val comentarioPadreId: Long? = null,

    @Column(name = "autor_id", nullable = false)
    val autorId: Long,

    @Column(nullable = false, columnDefinition = "text")
    var texto: String,

    @Column(name = "eliminado_en")
    var eliminadoEn: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
