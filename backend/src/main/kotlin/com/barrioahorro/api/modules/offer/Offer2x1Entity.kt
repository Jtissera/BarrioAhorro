package com.barrioahorro.api.modules.offer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "ofertas_2x1")
class Offer2x1Entity(
    @Id
    @Column(name = "oferta_id")
    val ofertaId: Long = 0,

    @Column(name = "unidades_a_pagar", nullable = false)
    var unidadesAPagar: Short,

    @Column(name = "unidades_a_llevar", nullable = false)
    var unidadesALlevar: Short,

    @Column(name = "precio_unitario", precision = 12, scale = 2)
    var precioUnitario: BigDecimal? = null,
)
