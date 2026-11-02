package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CouponsPage
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/cuponera")
class CouponCatalogController(
    private val service: CouponCatalogService,
) {

    @GetMapping("/cupones")
    fun list(
        auth: Authentication,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) radioKm: Double?,
        @RequestParam(required = false) rubros: List<Int>?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "DISTANCE") orden: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CouponsPage {
        if (page < 0) bad("La página no es válida")
        if (size !in 1..CouponCatalogService.MAX_PAGE_SIZE) {
            bad("El tamaño de página debe estar entre 1 y ${CouponCatalogService.MAX_PAGE_SIZE}")
        }
        if ((lat == null) != (lng == null)) bad("Enviá latitud y longitud juntas")
        if (lat != null && lng != null && (lat !in -90.0..90.0 || lng !in -180.0..180.0)) {
            bad("La ubicación no es válida")
        }
        if (radioKm != null && (radioKm <= 0.0 || radioKm.isNaN())) bad("La distancia no es válida")
        if ((rubros?.size ?: 0) > 20) bad("Demasiados filtros")
        val order = runCatching { CouponOrder.valueOf(orden.uppercase()) }.getOrNull() ?: bad("El orden no es válido")

        val filters = CouponFilters(
            latitude = lat,
            longitude = lng,
            radiusKm = radioKm,
            categoryIds = rubros.orEmpty().distinct(),
            query = q?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
        )
        return service.list(userId(auth), filters, order, page, size)
    }

    private fun bad(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}