package com.barrioahorro.api.modules.offer

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.offer.dto.CreateOffer2x1Request
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorMayorRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferCantidadRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorcentajeRequest
import com.barrioahorro.api.modules.offer.dto.Offer2x1DetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferPorMayorDetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferCantidadDetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferPorcentajeDetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferResponse
import com.barrioahorro.api.modules.offer.enum.TipoBeneficio
import com.barrioahorro.api.modules.offer.enum.TipoVigencia
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalTime
import java.time.OffsetDateTime

@Service
class OfferService(
    private val offerRepository: OfferRepository,
    private val offer2x1Repository: Offer2x1Repository,
    private val offerPorMayorRepository: OfferPorMayorRepository,
    private val offerCantidadRepository: OfferCantidadRepository,
    private val offerPorcentajeRepository: OfferPorcentajeRepository,
    private val businessRepository: BusinessRepository,
) {

    @Transactional
    fun createOffer2x1(userId: Long, request: CreateOffer2x1Request): OfferResponse {
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden publicar ofertas")
        }

        if (request.unidadesALlevar <= request.unidadesAPagar) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las unidades a llevar (${request.unidadesALlevar}) deben ser mayores a las unidades a pagar (${request.unidadesAPagar})",
            )
        }

        val vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta)

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.DOS_POR_UNO,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = vigenteHasta,
                activa = true,
            ),
        )

        val detail = offer2x1Repository.save(
            Offer2x1Entity(
                ofertaId = offer.id,
                unidadesAPagar = request.unidadesAPagar,
                unidadesALlevar = request.unidadesALlevar,
                precioUnitario = request.precioUnitario,
            ),
        )

        return offer.toResponse(detail2x1 = detail)
    }

    @Transactional
    fun createOfferPorMayor(userId: Long, request: CreateOfferPorMayorRequest): OfferResponse {
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden publicar ofertas")
        }

        if (request.cantidadMinima < 2) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cantidad mínima para venta por mayor debe ser al menos 2 unidades",
            )
        }

        if (request.precioUnitarioRegular != null && request.precioUnitarioMayorista >= request.precioUnitarioRegular) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El precio mayorista ($${request.precioUnitarioMayorista}) debe ser menor que el precio regular ($${request.precioUnitarioRegular})",
            )
        }

        val vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta)

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.POR_MAYOR,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = vigenteHasta,
                activa = true,
            ),
        )

        val detail = offerPorMayorRepository.save(
            OfferPorMayorEntity(
                ofertaId = offer.id,
                cantidadMinima = request.cantidadMinima,
                precioUnitarioMayorista = request.precioUnitarioMayorista,
                precioUnitarioRegular = request.precioUnitarioRegular,
            ),
        )

        return offer.toResponse(detailPorMayor = detail)
    }

    @Transactional
    fun createOfferCantidad(userId: Long, request: CreateOfferCantidadRequest): OfferResponse {
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden publicar ofertas")
        }

        val vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta)

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.CANTIDAD_ULTIMA_UNIDAD,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = vigenteHasta,
                activa = true,
            ),
        )

        val detail = offerCantidadRepository.save(
            OfferCantidadEntity(
                ofertaId = offer.id,
                cantidadRequerida = request.cantidadRequerida,
                precioUnitario = request.precioUnitario,
                porcentajeDescuentoUltimaUnidad = request.porcentajeDescuentoUltimaUnidad,
            ),
        )

        return offer.toResponse(detailCantidad = detail)
    }

    @Transactional
    fun createOfferPorcentaje(userId: Long, request: CreateOfferPorcentajeRequest): OfferResponse {
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden publicar ofertas")
        }

        val vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta)

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.PORCENTAJE,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = vigenteHasta,
                activa = true,
            ),
        )

        val detail = offerPorcentajeRepository.saveAndFlush(
            OfferPorcentajeEntity(
                ofertaId = offer.id,
                precioOriginal = request.precioOriginal,
                porcentajeDescuento = request.porcentajeDescuento,
            ),
        )

        return offer.toResponse(detailPorcentaje = detail)
    }

    @Transactional(readOnly = true)
    fun getMyOffers(userId: Long): List<OfferResponse> {
        val offers = offerRepository.findByComercioIdOrderByCreatedAtDesc(userId)
        return offers.map { mapOfferToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getMyOfferById(userId: Long, offerId: Long): OfferResponse {
        val offer = offerRepository.findById(offerId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada")
        }

        if (offer.comercioId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver esta oferta")
        }

        return mapOfferToResponse(offer)
    }

    @Transactional(readOnly = true)
    fun getOffersByBusiness(businessId: Long): List<OfferResponse> {
        if (!businessRepository.existsById(businessId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Comercio no encontrado")
        }

        val now = OffsetDateTime.now()
        val offers = offerRepository.findByComercioIdAndActivaTrueOrderByCreatedAtDesc(businessId)
            .filter { it.vigenteHasta == null || it.vigenteHasta!!.isAfter(now) }

        return offers.map { mapOfferToResponse(it) }
    }

    @Transactional
    fun toggleOfferStatus(userId: Long, offerId: Long): OfferResponse {
        val offer = offerRepository.findById(offerId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada")
        }

        if (offer.comercioId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta oferta")
        }

        offer.activa = !offer.activa
        val saved = offerRepository.save(offer)
        return mapOfferToResponse(saved)
    }

    private fun mapOfferToResponse(offer: OfferEntity): OfferResponse {
        val detail2x1 = if (offer.tipoOferta == TipoBeneficio.DOS_POR_UNO) {
            offer2x1Repository.findById(offer.id).orElse(null)
        } else {
            null
        }

        val detailPorMayor = if (offer.tipoOferta == TipoBeneficio.POR_MAYOR) {
            offerPorMayorRepository.findById(offer.id).orElse(null)
        } else {
            null
        }

        val detailCantidad = if (offer.tipoOferta == TipoBeneficio.CANTIDAD_ULTIMA_UNIDAD) {
            offerCantidadRepository.findById(offer.id).orElse(null)
        } else {
            null
        }

        val detailPorcentaje = if (offer.tipoOferta == TipoBeneficio.PORCENTAJE) {
            offerPorcentajeRepository.findById(offer.id).orElse(null)
        } else {
            null
        }

        return offer.toResponse(
            detail2x1 = detail2x1,
            detailPorMayor = detailPorMayor,
            detailCantidad = detailCantidad,
            detailPorcentaje = detailPorcentaje)
    }

    private fun calcularVigencia(vigenciaTipo: TipoVigencia, fechaIndicada: OffsetDateTime?): OffsetDateTime? {
        val now = OffsetDateTime.now()
        return when (vigenciaTipo) {
            TipoVigencia.SOLO_HOY -> now.with(LocalTime.MAX)
            TipoVigencia.TODA_LA_SEMANA -> now.plusDays(7)
            TipoVigencia.HASTA_AGOTAR_STOCK -> null
            TipoVigencia.FECHA_ESPECIFICA -> {
                if (fechaIndicada == null) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe especificar la fecha de vencimiento")
                }
                if (fechaIndicada.isBefore(now)) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento no puede ser en el pasado")
                }
                fechaIndicada
            }
        }
    }

    private fun OfferEntity.toResponse(
        detail2x1: Offer2x1Entity? = null,
        detailPorMayor: OfferPorMayorEntity? = null,
        detailCantidad: OfferCantidadEntity? = null,
        detailPorcentaje: OfferPorcentajeEntity? = null,
    ): OfferResponse {
        val now = OffsetDateTime.now()
        val esVigente = activa && (vigenteHasta == null || vigenteHasta!!.isAfter(now))

        return OfferResponse(
            id = id,
            comercioId = comercioId,
            nombreProducto = nombreProducto,
            fotoUrl = fotoUrl,
            tipoOferta = tipoOferta,
            vigenciaTipo = vigenciaTipo,
            vigenteHasta = vigenteHasta,
            activa = activa,
            vigente = esVigente,
            createdAt = createdAt,
            detalle2x1 = detail2x1?.let {
                Offer2x1DetailResponse(
                    unidadesAPagar = it.unidadesAPagar,
                    unidadesALlevar = it.unidadesALlevar,
                    precioUnitario = it.precioUnitario,
                )
            },
            detallePorMayor = detailPorMayor?.let {
                OfferPorMayorDetailResponse(
                    cantidadMinima = it.cantidadMinima,
                    precioUnitarioMayorista = it.precioUnitarioMayorista,
                    precioUnitarioRegular = it.precioUnitarioRegular,
                )
            },
            detalleCantidad = detailCantidad?.let {
                OfferCantidadDetailResponse(
                    cantidadRequerida = it.cantidadRequerida,
                    precioUnitario = it.precioUnitario,
                    porcentajeDescuentoUltimaUnidad = it.porcentajeDescuentoUltimaUnidad,
                )
            },
            detallePorcentaje = detailPorcentaje?.let {
                OfferPorcentajeDetailResponse(
                    precioOriginal = it.precioOriginal,
                    porcentajeDescuento = it.porcentajeDescuento,
                    precioFinal = requireNotNull(it.precioFinal) { "El precio final no debe ser nulo" },
                )
            },
        )
    }
}
