package com.barrioahorro.api.modules.cuponera

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime


 //Compromiso quincenal de cuponera (tabla compromisos_cuponera).

@Entity
@Table(name = "compromisos_cuponera")
class CuponeraCommitmentEntity(
    @Id
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "comercio_id", nullable = false, insertable = false, updatable = false)
    val comercioId: Long = 0,

    @Column(name = "periodo_inicio", nullable = false, insertable = false, updatable = false)
    val periodoInicio: LocalDate = LocalDate.of(2026, 10, 1),

    @Column(name = "periodo_fin", nullable = false, insertable = false, updatable = false)
    val periodoFin: LocalDate = LocalDate.of(2026, 10, 1),

    @Column(name = "fecha_limite_ratificacion", nullable = false, insertable = false, updatable = false)
    val fechaLimiteRatificacion: OffsetDateTime = OffsetDateTime.now(),

    // PENDIENTE | RATIFICADO | VENCIDO. Se lee como String (el enum de Postgres
    // se convierte solo) para no depender de la versión de Hibernate.
    @Column(name = "estado", nullable = false, insertable = false, updatable = false)
    val estado: String = "PENDIENTE",

    @Column(name = "fecha_ratificacion", insertable = false, updatable = false)
    val fechaRatificacion: OffsetDateTime? = null,
)