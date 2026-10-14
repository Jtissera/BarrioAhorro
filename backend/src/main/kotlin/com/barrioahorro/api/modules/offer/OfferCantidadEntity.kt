package com.barrioahorro.api.modules.offer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "ofertas_cantidad")
class OfferCantidadEntity(
    @Id
    @Column(name = "oferta_id")
    val ofertaId: Long = 0,

    @Column(name = "cantidad_requerida", nullable = false)
    var cantidadRequerida: Short,

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    var precioUnitario: BigDecimal,

    @Column(name = "porcentaje_descuento_ultima_unidad", nullable = false)
    var porcentajeDescuentoUltimaUnidad: Short,
)