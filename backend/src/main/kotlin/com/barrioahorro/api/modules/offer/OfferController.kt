package com.barrioahorro.api.modules.offer

import com.barrioahorro.api.modules.offer.dto.ActiveOffersResponse
import com.barrioahorro.api.modules.offer.dto.CreateOffer2x1Request
import com.barrioahorro.api.modules.offer.dto.CreateOfferCantidadRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorMayorRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorcentajeRequest
import com.barrioahorro.api.modules.offer.dto.OfferPhotoUploadResponse
import com.barrioahorro.api.modules.offer.dto.OfferResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/business")
class OfferController(
    private val offerService: OfferService,
) {

    // ===== Panel privado del comercio =====

    @PostMapping("/me/offers/2x1")
    fun createOffer2x1(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOffer2x1Request,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOffer2x1(userId(authentication), request))

    @PostMapping("/me/offers/por-mayor")
    fun createOfferPorMayor(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOfferPorMayorRequest,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOfferPorMayor(userId(authentication), request))

    @PostMapping("/me/offers/cantidad")
    fun createOfferCantidad(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOfferCantidadRequest,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOfferCantidad(userId(authentication), request))

    @PostMapping("/me/offers/porcentaje")
    fun createOfferPorcentaje(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOfferPorcentajeRequest,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOfferPorcentaje(userId(authentication), request))

    // ----- Editar -----

    @PutMapping("/me/offers/{id}/2x1")
    fun updateOffer2x1(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateOffer2x1Request,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.ok(offerService.updateOffer2x1(userId(authentication), id, request))

    @PutMapping("/me/offers/{id}/por-mayor")
    fun updateOfferPorMayor(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateOfferPorMayorRequest,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.ok(offerService.updateOfferPorMayor(userId(authentication), id, request))

    @PutMapping("/me/offers/{id}/cantidad")
    fun updateOfferCantidad(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateOfferCantidadRequest,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.ok(offerService.updateOfferCantidad(userId(authentication), id, request))

    @PutMapping("/me/offers/{id}/porcentaje")
    fun updateOfferPorcentaje(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateOfferPorcentajeRequest,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.ok(offerService.updateOfferPorcentaje(userId(authentication), id, request))

    // ----- Eliminar -----

    @DeleteMapping("/me/offers/{id}")
    fun deleteOffer(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        offerService.deleteOffer(userId(authentication), id)
        return ResponseEntity.noContent().build()
    }

    // ----- Consultas -----

    @GetMapping("/me/offers/active")
    fun getMyActiveOffers(authentication: Authentication): ResponseEntity<ActiveOffersResponse> =
        ResponseEntity.ok(offerService.getMyActiveOffers(userId(authentication)))

    @GetMapping("/me/offers")
    fun getMyOffers(authentication: Authentication): ResponseEntity<List<OfferResponse>> =
        ResponseEntity.ok(offerService.getMyOffers(userId(authentication)))

    @GetMapping("/me/offers/{id}")
    fun getMyOfferById(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.ok(offerService.getMyOfferById(userId(authentication), id))

    @PatchMapping("/me/offers/{id}/toggle")
    fun toggleOfferStatus(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<OfferResponse> =
        ResponseEntity.ok(offerService.toggleOfferStatus(userId(authentication), id))

    @PostMapping("/me/offers/fotos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadOfferPhoto(
        authentication: Authentication,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<OfferPhotoUploadResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(offerService.uploadPhoto(userId(authentication), file))

    // ===== Vidriera pública =====

    @GetMapping("/{businessId}/offers")
    fun getOffersByBusiness(@PathVariable businessId: Long): ResponseEntity<List<OfferResponse>> =
        ResponseEntity.ok(offerService.getOffersByBusiness(businessId))

    private fun userId(authentication: Authentication): Long = authentication.principal as Long
}