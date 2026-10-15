package com.barrioahorro.api.modules.offer

import com.barrioahorro.api.modules.offer.dto.CreateOffer2x1Request
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorMayorRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferCantidadRequest
import com.barrioahorro.api.modules.offer.dto.CreateOfferPorcentajeRequest
import com.barrioahorro.api.modules.offer.dto.OfferResponse
import com.barrioahorro.api.modules.offer.dto.OfferPhotoUploadResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/business")
class OfferController(
    private val offerService: OfferService,
) {

    // ==========================================
    // Panel Privado del Comercio ("Mis Ofertas")
    // ==========================================

    @PostMapping("/me/offers/2x1")
    fun createOffer2x1(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOffer2x1Request,
    ): ResponseEntity<OfferResponse> {
        val userId = authentication.principal as Long
        val response = offerService.createOffer2x1(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/me/offers/por-mayor")
    fun createOfferPorMayor(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOfferPorMayorRequest,
    ): ResponseEntity<OfferResponse> {
        val userId = authentication.principal as Long
        val response = offerService.createOfferPorMayor(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/me/offers/cantidad")
    fun createOfferCantidad(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOfferCantidadRequest,
    ): ResponseEntity<OfferResponse> {
        val userId = authentication.principal as Long
        val response = offerService.createOfferCantidad(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/me/offers/porcentaje")
    fun createOfferPorcentaje(
        authentication: Authentication,
        @Valid @RequestBody request: CreateOfferPorcentajeRequest,
    ): ResponseEntity<OfferResponse> {
        val userId = authentication.principal as Long
        val response = offerService.createOfferPorcentaje(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/me/offers")
    fun getMyOffers(authentication: Authentication): ResponseEntity<List<OfferResponse>> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(offerService.getMyOffers(userId))
    }

    @GetMapping("/me/offers/{id}")
    fun getMyOfferById(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<OfferResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(offerService.getMyOfferById(userId, id))
    }

    @PatchMapping("/me/offers/{id}/toggle")
    fun toggleOfferStatus(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<OfferResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(offerService.toggleOfferStatus(userId, id))
    }

    @PostMapping("/me/offers/fotos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadOfferPhoto(
        authentication: Authentication,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<OfferPhotoUploadResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.uploadPhoto(userId, file))
    }

    // ==========================================
    // Vidriera Pública / Clientes
    // ==========================================

    @GetMapping("/{businessId}/offers")
    fun getOffersByBusiness(
        @PathVariable businessId: Long,
    ): ResponseEntity<List<OfferResponse>> {
        return ResponseEntity.ok(offerService.getOffersByBusiness(businessId))
    }
}
