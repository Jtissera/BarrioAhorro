package com.barrioahorro.api.modules.business

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.time.OffsetDateTime

@Entity
@Table(name = "perfiles_comercio")
class BusinessEntity(
    @Id
    @Column(name = "usuario_id")
    val usuarioId: Long = 0,

    @Column(name = "nombre_negocio")
    var nombreNegocio: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubro_id")
    var rubro: CategoryEntity? = null,

    @Column(name = "direccion")
    var direccion: String? = null,

    @Column(name = "descripcion", columnDefinition = "text")
    var descripcion: String? = null,

    @Column(name = "ubicacion", columnDefinition = "geography(Point,4326)")
    var ubicacion: Point? = null,

    @Column(name = "onboarding_completado", nullable = false)
    var onboardingCompletado: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)