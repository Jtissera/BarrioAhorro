package com.barrioahorro.api.modules.offer

import com.barrioahorro.api.modules.offer.enum.TipoBeneficio
import com.barrioahorro.api.modules.offer.enum.TipoVigencia
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "ofertas")
class OfferEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "comercio_id", nullable = false)
    val comercioId: Long,

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_oferta", nullable = false, columnDefinition = "tipo_beneficio_enum")
    val tipoOferta: TipoBeneficio = TipoBeneficio.DOS_POR_UNO,

    @Column(name = "nombre_producto", nullable = false)
    var nombreProducto: String,

    @Column(name = "foto_url")
    var fotoUrl: String? = null,

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "vigencia_tipo", nullable = false, columnDefinition = "tipo_vigencia_enum")
    var vigenciaTipo: TipoVigencia,

    @Column(name = "vigente_hasta")
    var vigenteHasta: OffsetDateTime? = null,

    @Column(name = "activa", nullable = false)
    var activa: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
