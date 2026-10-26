package com.barrioahorro.api.modules.forum

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "foro_publicaciones")
class ForumPostEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "autor_id", nullable = false)
    val autorId: Long,

    @Column(name = "comercio_id", nullable = false)
    val comercioId: Long,

    @Column(nullable = false, columnDefinition = "text")
    var texto: String,

    @Column(name = "precio_observado")
    var precioObservado: BigDecimal? = null,

    @Column(name = "ubicacion_observada", columnDefinition = "geography(Point,4326)")
    var ubicacionObservada: Point? = null,

    @Column(name = "eliminado_en")
    var eliminadoEn: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
