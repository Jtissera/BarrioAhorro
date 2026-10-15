package com.barrioahorro.api.modules.sorteo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime

// Compromiso de sorteo (tabla compromisos_sorteo)

@Entity
@Table(name = "compromisos_sorteo")
class SorteoCommitmentEntity(
    @Id
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "comercio_id", nullable = false, insertable = false, updatable = false)
    val comercioId: Long = 0,

    @Column(name = "ciclo_inicio", nullable = false, insertable = false, updatable = false)
    val cicloInicio: LocalDate = LocalDate.of(2026, 10, 1),

    @Column(name = "ciclo_fin", nullable = false, insertable = false, updatable = false)
    val cicloFin: LocalDate = LocalDate.of(2026, 10, 1),

    @Column(name = "fecha_limite_ratificacion", nullable = false, insertable = false, updatable = false)
    val fechaLimiteRatificacion: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "premio_descripcion", nullable = false, insertable = false, updatable = false)
    val premioDescripcion: String = "",

    // PENDIENTE | RATIFICADO | VENCIDO
    @Column(name = "estado", nullable = false, insertable = false, updatable = false)
    val estado: String = "PENDIENTE",

    @Column(name = "fecha_ratificacion", insertable = false, updatable = false)
    val fechaRatificacion: OffsetDateTime? = null,
)