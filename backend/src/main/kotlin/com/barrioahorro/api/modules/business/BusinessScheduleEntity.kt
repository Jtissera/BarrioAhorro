package com.barrioahorro.api.modules.business

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "horarios_atencion")
class BusinessScheduleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "comercio_id", nullable = false)
    val comercioId: Long,

    @Column(name = "dia_semana", nullable = false)
    val diaSemana: Int, // 1=Lunes ... 7=Domingo

    @Column(name = "hora_inicio", nullable = false)
    val horaInicio: LocalTime,

    @Column(name = "hora_fin", nullable = false)
    val horaFin: LocalTime,

    @Column(name = "activo", nullable = false)
    var activo: Boolean = true,
)