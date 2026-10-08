package com.barrioahorro.api.modules.review

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "resenas_comercio")
class ReviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "comercio_id", nullable = false)
    val comercioId: Long,

    @Column(name = "cliente_id", nullable = false)
    val clienteId: Long,

    @Column(name = "texto", nullable = false, columnDefinition = "text")
    var texto: String,

    @Column(name = "estrellas", nullable = false)
    var estrellas: Short,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)