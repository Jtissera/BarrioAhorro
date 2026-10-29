package com.barrioahorro.api.modules.explore

import com.barrioahorro.api.modules.business.dto.BusinessReviewResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Perfil de un comercio visto por el vecino. */
@RestController
@RequestMapping("/api/comercios")
class BusinessProfileController(
    private val service: BusinessProfileService,
) {

    @GetMapping("/{id}")
    fun profile(
        auth: Authentication,
        @PathVariable id: Long,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
    ): BusinessProfileResponse {
        checkLocation(lat, lng)
        val userId = userId(auth)
        val profile = service.profile(userId, id, lat, lng)
        runCatching { service.recordView(userId, id) }
        return profile
    }

    @GetMapping("/{id}/ofertas")
    fun offers(
        auth: Authentication,
        @PathVariable id: Long,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
    ): List<ExploreOfferResponse> {
        checkLocation(lat, lng)
        return service.offers(userId(auth), id, lat, lng)
    }

    @GetMapping("/{id}/resenas")
    fun reviews(
        auth: Authentication,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): BusinessReviewsPage = service.reviews(userId(auth), id, page, size)

    /** Crea la reseña del vecino o, si ya tenía una, la reemplaza. */
    @PutMapping("/{id}/resena")
    fun saveReview(
        auth: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody body: WriteReviewRequest,
    ): BusinessReviewResponse = service.saveReview(userId(auth), id, body)

    @DeleteMapping("/{id}/resena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteReview(auth: Authentication, @PathVariable id: Long) =
        service.deleteReview(userId(auth), id)

    private fun checkLocation(lat: Double?, lng: Double?) {
        if ((lat == null) != (lng == null)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Enviá latitud y longitud juntas")
        }
        if (lat != null && lng != null && (lat !in -90.0..90.0 || lng !in -180.0..180.0)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La ubicación no es válida")
        }
    }

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}
