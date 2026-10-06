package com.barrioahorro.api.modules.business

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "fotos_comercio")
class BusinessPhotoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "comercio_id", nullable = false)
    val comercioId: Long,

    @Column(name = "url", nullable = false)
    var url: String,

    @Column(name = "orden", nullable = false)
    var orden: Short,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
