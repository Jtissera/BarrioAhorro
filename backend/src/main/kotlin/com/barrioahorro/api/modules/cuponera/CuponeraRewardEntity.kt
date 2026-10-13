package com.barrioahorro.api.modules.cuponera

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Premio concreto de un compromiso de cuponera (tabla cupones_cuponera).
 *
 * Los SMALLINT se mapean como Short a propósito (mismo problema que tuvo
 * dia_semana con Int). stock_canjeado lo maneja la base al validar canjes.
 */
@Entity
@Table(name = "cupones_cuponera")
class CuponeraRewardEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "compromiso_id", nullable = false)
    var compromisoId: Long = 0,

    @Column(name = "nombre_producto", nullable = false, columnDefinition = "text")
    var nombreProducto: String = "",

    @Column(name = "descripcion", columnDefinition = "text")
    var descripcion: String? = null,

    @Column(name = "foto_url", columnDefinition = "text")
    var fotoUrl: String? = null,

    @Column(name = "valor_comercio", precision = 12, scale = 2)
    var valorComercio: BigDecimal? = null,

    @Column(name = "costo_puntos", nullable = false)
    var costoPuntos: Int = 0,

    @Column(name = "stock_comprometido", nullable = false)
    var stockComprometido: Short = 0,

    @Column(name = "stock_canjeado", nullable = false, insertable = false, updatable = false)
    val stockCanjeado: Short = 0,
)