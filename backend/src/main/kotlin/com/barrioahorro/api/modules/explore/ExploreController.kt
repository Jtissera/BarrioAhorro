package com.barrioahorro.api.modules.explore

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
class ExploreController(
    private val service: ExploreService,
) {

    @GetMapping("/cliente/resumen")
    fun summary(auth: Authentication): ClientSummaryResponse = service.summary(userId(auth))

    @GetMapping("/explorar/ofertas")
    fun offers(
        auth: Authentication,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) radioKm: Double?,
        @RequestParam(required = false) rubros: List<Int>?,
        @RequestParam(required = false) tipos: List<String>?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "DISTANCE") orden: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ExploreOffersPage {
        if (page < 0) bad("La página no es válida")
        if (size !in 1..ExploreService.MAX_PAGE_SIZE) bad("El tamaño de página debe estar entre 1 y ${ExploreService.MAX_PAGE_SIZE}")
        val order = runCatching { ExploreOrder.valueOf(orden.uppercase()) }.getOrNull() ?: bad("El orden no es válido")
        return service.offers(userId(auth), filters(lat, lng, radioKm, rubros, tipos, q), order, page, size)
    }

    @GetMapping("/explorar/comercios")
    fun businesses(
        auth: Authentication,
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) radioKm: Double?,
        @RequestParam(required = false) rubros: List<Int>?,
        @RequestParam(required = false) tipos: List<String>?,
        @RequestParam(required = false) q: String?,
    ): List<ExploreBusinessResponse> =
        service.businesses(userId(auth), filters(lat, lng, radioKm, rubros, tipos, q))

    @PutMapping("/favoritos/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addFavorite(auth: Authentication, @PathVariable offerId: Long) =
        service.addFavorite(userId(auth), offerId)

    @DeleteMapping("/favoritos/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFavorite(auth: Authentication, @PathVariable offerId: Long) =
        service.removeFavorite(userId(auth), offerId)

    private fun filters(
        lat: Double?, lng: Double?, radioKm: Double?,
        rubros: List<Int>?, tipos: List<String>?, q: String?,
    ): ExploreFilters {
        if ((lat == null) != (lng == null)) bad("Enviá latitud y longitud juntas")
        if (lat != null && lng != null &&
            (lat !in -90.0..90.0 || lng !in -180.0..180.0)
        ) {
            bad("La ubicación no es válida")
        }
        if (radioKm != null && (radioKm <= 0.0 || radioKm.isNaN())) bad("La distancia no es válida")
        val types = tipos.orEmpty().map { it.trim().uppercase() }.distinct()
        if (types.any { it !in ExploreService.VALID_TYPES }) bad("El tipo de oferta no es válido")
        if ((rubros?.size ?: 0) > 20 || types.size > 10) bad("Demasiados filtros")

        val query = q?.trim()?.take(80)?.takeIf { it.isNotEmpty() }
        return ExploreFilters(lat, lng, radioKm, rubros.orEmpty().distinct(), types, query)
    }

    private fun bad(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}