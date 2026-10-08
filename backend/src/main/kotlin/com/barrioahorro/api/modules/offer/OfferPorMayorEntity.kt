package com.barrioahorro.api.modules.offer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "ofertas_por_mayor")
class OfferPorMayorEntity(
    @Id
    @Column(name = "oferta_id")
    val ofertaId: Long = 0,

    @Column(name = "cantidad_minima", nullable = false)
    var cantidadMinima: Short,

    @Column(name = "precio_unitario_mayorista", nullable = false, precision = 12, scale = 2)
    var precioUnitarioMayorista: BigDecimal,

    @Column(name = "precio_unitario_regular", precision = 12, scale = 2)
    var precioUnitarioRegular: BigDecimal? = null,
)
