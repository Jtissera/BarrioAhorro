package com.barrioahorro.api.modules.offer

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.business.PhotoStorage
import com.barrioahorro.api.modules.offer.dto.ActiveOfferItemResponse
import com.barrioahorro.api.modules.offer.dto.ActiveOffersResponse
import com.barrioahorro.api.modules.offer.dto.CreateOffer2x1Request
import com.barrioahorro.api.modules.offer.dto.CreateOfferCantidadRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorMayorRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorcentajeRequest
import com.barrioahorro.api.modules.offer.dto.Offer2x1DetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferCantidadDetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferPhotoUploadResponse
import com.barrioahorro.api.modules.offer.dto.OfferPorMayorDetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferPorcentajeDetailResponse
import com.barrioahorro.api.modules.offer.dto.OfferResponse
import com.barrioahorro.api.modules.offer.enum.TipoBeneficio
import com.barrioahorro.api.modules.offer.enum.TipoVigencia
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
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
    private val photoStorage: PhotoStorage,
    private val offerConfirmationQueries: OfferConfirmationQueries,
    private val jdbc: JdbcTemplate,
) {

    // ------------------------------------------------------------------
    // Crear
    // ------------------------------------------------------------------

    @Transactional
    fun createOffer2x1(userId: Long, request: CreateOffer2x1Request): OfferResponse {
        requireActiveBusiness(userId, "publicar ofertas")
        validate2x1(request.unidadesALlevar, request.unidadesAPagar)

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.DOS_POR_UNO,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta),
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
        requireActiveBusiness(userId, "publicar ofertas")
        validatePorMayor(request.cantidadMinima, request.precioUnitarioMayorista, request.precioUnitarioRegular)

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.POR_MAYOR,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta),
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
        requireActiveBusiness(userId, "publicar ofertas")

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.CANTIDAD_ULTIMA_UNIDAD,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta),
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
        requireActiveBusiness(userId, "publicar ofertas")

        val offer = offerRepository.save(
            OfferEntity(
                comercioId = userId,
                tipoOferta = TipoBeneficio.PORCENTAJE,
                nombreProducto = request.nombreProducto.trim(),
                fotoUrl = request.fotoUrl?.trim(),
                vigenciaTipo = request.vigenciaTipo,
                vigenteHasta = calcularVigencia(request.vigenciaTipo, request.vigenteHasta),
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

    // ------------------------------------------------------------------
    // Editar (el tipo de oferta no se puede cambiar; guardar la deja activa)
    // ------------------------------------------------------------------

    @Transactional
    fun updateOffer2x1(userId: Long, offerId: Long, request: CreateOffer2x1Request): OfferResponse {
        val offer = loadOwnedOffer(userId, offerId, TipoBeneficio.DOS_POR_UNO, "editar ofertas")
        validate2x1(request.unidadesALlevar, request.unidadesAPagar)
        applyCommon(offer, request.nombreProducto, request.fotoUrl, request.vigenciaTipo, request.vigenteHasta)

        val detail = offer2x1Repository.findById(offerId).orElseThrow { missingDetail() }
        detail.unidadesAPagar = request.unidadesAPagar
        detail.unidadesALlevar = request.unidadesALlevar
        detail.precioUnitario = request.precioUnitario
        offer2x1Repository.save(detail)

        return offerRepository.save(offer).toResponse(detail2x1 = detail)
    }

    @Transactional
    fun updateOfferPorMayor(userId: Long, offerId: Long, request: CreateOfferPorMayorRequest): OfferResponse {
        val offer = loadOwnedOffer(userId, offerId, TipoBeneficio.POR_MAYOR, "editar ofertas")
        validatePorMayor(request.cantidadMinima, request.precioUnitarioMayorista, request.precioUnitarioRegular)
        applyCommon(offer, request.nombreProducto, request.fotoUrl, request.vigenciaTipo, request.vigenteHasta)

        val detail = offerPorMayorRepository.findById(offerId).orElseThrow { missingDetail() }
        detail.cantidadMinima = request.cantidadMinima
        detail.precioUnitarioMayorista = request.precioUnitarioMayorista
        detail.precioUnitarioRegular = request.precioUnitarioRegular
        offerPorMayorRepository.save(detail)

        return offerRepository.save(offer).toResponse(detailPorMayor = detail)
    }

    @Transactional
    fun updateOfferCantidad(userId: Long, offerId: Long, request: CreateOfferCantidadRequest): OfferResponse {
        val offer = loadOwnedOffer(userId, offerId, TipoBeneficio.CANTIDAD_ULTIMA_UNIDAD, "editar ofertas")
        applyCommon(offer, request.nombreProducto, request.fotoUrl, request.vigenciaTipo, request.vigenteHasta)

        val detail = offerCantidadRepository.findById(offerId).orElseThrow { missingDetail() }
        detail.cantidadRequerida = request.cantidadRequerida
        detail.precioUnitario = request.precioUnitario
        detail.porcentajeDescuentoUltimaUnidad = request.porcentajeDescuentoUltimaUnidad
        offerCantidadRepository.save(detail)

        return offerRepository.save(offer).toResponse(detailCantidad = detail)
    }

    @Transactional
    fun updateOfferPorcentaje(userId: Long, offerId: Long, request: CreateOfferPorcentajeRequest): OfferResponse {
        val offer = loadOwnedOffer(userId, offerId, TipoBeneficio.PORCENTAJE, "editar ofertas")
        applyCommon(offer, request.nombreProducto, request.fotoUrl, request.vigenciaTipo, request.vigenteHasta)

        val detail = offerPorcentajeRepository.findById(offerId).orElseThrow { missingDetail() }
        detail.precioOriginal = request.precioOriginal
        detail.porcentajeDescuento = request.porcentajeDescuento
        offerPorcentajeRepository.saveAndFlush(detail)

        return offerRepository.save(offer).toResponse(detailPorcentaje = detail)
    }

    // ------------------------------------------------------------------
    // Eliminar (definitivo: la base borra detalles, validaciones, favoritos y vistas en cascada)
    // ------------------------------------------------------------------

    @Transactional
    fun deleteOffer(userId: Long, offerId: Long) {
        requireActiveBusiness(userId, "eliminar ofertas")
        val offer = offerRepository.findById(offerId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada")
        }
        if (offer.comercioId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta oferta")
        }
        offerRepository.delete(offer)
    }

    // ------------------------------------------------------------------
    // Resto
    // ------------------------------------------------------------------

    fun uploadPhoto(userId: Long, file: MultipartFile): OfferPhotoUploadResponse {
        requireActiveBusiness(userId, "subir fotos de ofertas")
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió ninguna imagen")
        }
        val url = photoStorage.store(file.bytes)
            ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "La foto debe ser JPEG, PNG o WebP")
        return OfferPhotoUploadResponse(url)
    }

    @Transactional(readOnly = true)
    fun getMyOffers(userId: Long): List<OfferResponse> =
        offerRepository.findByComercioIdOrderByCreatedAtDesc(userId).map { mapOfferToResponse(it) }

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
        return offerRepository.findByComercioIdAndActivaTrueOrderByCreatedAtDesc(businessId)
            .filter { it.vigenteHasta == null || it.vigenteHasta!!.isAfter(now) }
            .map { mapOfferToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getMyActiveOffers(userId: Long): ActiveOffersResponse {
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden ver sus ofertas")
        }
        val offers = getOffersByBusiness(userId)
        val confirmations = offerConfirmationQueries.countByOffer(userId)
        return ActiveOffersResponse(
            activeCount = offers.size,
            items = offers.map { ActiveOfferItemResponse(offer = it, confirmationsCount = confirmations[it.id] ?: 0) },
        )
    }

    @Transactional
    fun toggleOfferStatus(userId: Long, offerId: Long): OfferResponse {
        requireActiveBusiness(userId, "modificar ofertas")
        val offer = offerRepository.findById(offerId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada")
        }
        if (offer.comercioId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta oferta")
        }
        offer.activa = !offer.activa
        return mapOfferToResponse(offerRepository.save(offer))
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Solo un comercio habilitado (sin aportes vencidos) puede crear, editar o borrar ofertas. */
    private fun requireActiveBusiness(userId: Long, action: String) {
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden $action")
        }
        val restricted = jdbc.queryForList(
            "SELECT acceso_restringido FROM v_comercio_acceso_restringido WHERE comercio_id = ?",
            Boolean::class.javaObjectType,
            userId,
        ).firstOrNull() ?: false
        if (restricted) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Tu acceso está limitado hasta que ratifiques tu aporte",
            )
        }
    }

    private fun loadOwnedOffer(userId: Long, offerId: Long, expected: TipoBeneficio, action: String): OfferEntity {
        requireActiveBusiness(userId, action)
        val offer = offerRepository.findById(offerId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada")
        }
        if (offer.comercioId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta oferta")
        }
        if (offer.tipoOferta != expected) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cambiar el tipo de una oferta")
        }
        return offer
    }

    private fun applyCommon(
        offer: OfferEntity,
        nombre: String,
        foto: String?,
        vigenciaTipo: TipoVigencia,
        vigenteHasta: OffsetDateTime?,
    ) {
        offer.nombreProducto = nombre.trim()
        offer.fotoUrl = foto?.trim()?.takeIf { it.isNotEmpty() }
        offer.vigenciaTipo = vigenciaTipo
        offer.vigenteHasta = calcularVigencia(vigenciaTipo, vigenteHasta)
        offer.activa = true
    }

    private fun missingDetail() =
        ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La oferta no tiene su detalle")

    private fun validate2x1(llevar: Short, pagar: Short) {
        if (llevar <= pagar) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las unidades a llevar ($llevar) deben ser mayores a las unidades a pagar ($pagar)",
            )
        }
    }

    private fun validatePorMayor(cantidadMinima: Short, mayorista: BigDecimal, regular: BigDecimal?) {
        if (cantidadMinima < 2) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cantidad mínima para venta por mayor debe ser al menos 2 unidades",
            )
        }
        if (regular != null && mayorista >= regular) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El precio mayorista (\$$mayorista) debe ser menor que el precio regular (\$$regular)",
            )
        }
    }

    private fun mapOfferToResponse(offer: OfferEntity): OfferResponse {
        val detail2x1 = if (offer.tipoOferta == TipoBeneficio.DOS_POR_UNO) {
            offer2x1Repository.findById(offer.id).orElse(null)
        } else null
        val detailPorMayor = if (offer.tipoOferta == TipoBeneficio.POR_MAYOR) {
            offerPorMayorRepository.findById(offer.id).orElse(null)
        } else null
        val detailCantidad = if (offer.tipoOferta == TipoBeneficio.CANTIDAD_ULTIMA_UNIDAD) {
            offerCantidadRepository.findById(offer.id).orElse(null)
        } else null
        val detailPorcentaje = if (offer.tipoOferta == TipoBeneficio.PORCENTAJE) {
            offerPorcentajeRepository.findById(offer.id).orElse(null)
        } else null

        return offer.toResponse(
            detail2x1 = detail2x1,
            detailPorMayor = detailPorMayor,
            detailCantidad = detailCantidad,
            detailPorcentaje = detailPorcentaje,
        )
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
                Offer2x1DetailResponse(it.unidadesAPagar, it.unidadesALlevar, it.precioUnitario)
            },
            detallePorMayor = detailPorMayor?.let {
                OfferPorMayorDetailResponse(it.cantidadMinima, it.precioUnitarioMayorista, it.precioUnitarioRegular)
            },
            detalleCantidad = detailCantidad?.let {
                OfferCantidadDetailResponse(it.cantidadRequerida, it.precioUnitario, it.porcentajeDescuentoUltimaUnidad)
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