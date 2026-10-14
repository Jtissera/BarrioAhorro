package com.barrioahorro.api.modules.offer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.math.BigDecimal

@Entity
@Table(name = "ofertas_porcentaje")
class OfferPorcentajeEntity(
    @Id
    @Column(name = "oferta_id")
    val ofertaId: Long = 0,

    @Column(name = "precio_original", nullable = false, precision = 12, scale = 2)
    var precioOriginal: BigDecimal,

    @Column(name = "porcentaje_descuento", nullable = false)
    var porcentajeDescuento: Short,

    @Generated(event = [EventType.INSERT, EventType.UPDATE])
    @Column(name = "precio_final", insertable = false, updatable = false, precision = 12, scale = 2)
    val precioFinal: BigDecimal? = null,
)