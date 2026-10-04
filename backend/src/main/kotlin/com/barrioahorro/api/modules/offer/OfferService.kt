package com.barrioahorro.api.modules.offer

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.offer.dto.CreateOffer2x1Request
import com.barrioahorro.api.modules.offer.dto.Offer2x1DetailResponse
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

        return offer.toResponse(detail)
    }

    @Transactional(readOnly = true)
    fun getMyOffers(userId: Long): List<OfferResponse> {
        val offers = offerRepository.findByComercioIdOrderByCreatedAtDesc(userId)
        return offers.map { offer ->
            val detail2x1 = if (offer.tipoOferta == TipoBeneficio.DOS_POR_UNO) {
                offer2x1Repository.findById(offer.id).orElse(null)
            } else {
                null
            }
            offer.toResponse(detail2x1)
        }
    }

    @Transactional(readOnly = true)
    fun getOffersByBusiness(businessId: Long): List<OfferResponse> {
        if (!businessRepository.existsById(businessId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Comercio no encontrado")
        }

        val now = OffsetDateTime.now()
        val offers = offerRepository.findByComercioIdAndActivaTrueOrderByCreatedAtDesc(businessId)
            .filter { it.vigenteHasta == null || it.vigenteHasta!!.isAfter(now) }

        return offers.map { offer ->
            val detail2x1 = if (offer.tipoOferta == TipoBeneficio.DOS_POR_UNO) {
                offer2x1Repository.findById(offer.id).orElse(null)
            } else {
                null
            }
            offer.toResponse(detail2x1)
        }
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
        val detail2x1 = offer2x1Repository.findById(saved.id).orElse(null)
        return saved.toResponse(detail2x1)
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

    private fun OfferEntity.toResponse(detail: Offer2x1Entity?): OfferResponse =
        OfferResponse(
            id = id,
            comercioId = comercioId,
            nombreProducto = nombreProducto,
            fotoUrl = fotoUrl,
            tipoOferta = tipoOferta,
            vigenciaTipo = vigenciaTipo,
            vigenteHasta = vigenteHasta,
            activa = activa,
            createdAt = createdAt,
            detalle2x1 = detail?.let {
                Offer2x1DetailResponse(
                    unidadesAPagar = it.unidadesAPagar,
                    unidadesALlevar = it.unidadesALlevar,
                    precioUnitario = it.precioUnitario,
                )
            },
        )
}
