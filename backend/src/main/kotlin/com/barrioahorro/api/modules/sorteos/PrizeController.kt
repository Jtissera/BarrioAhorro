package com.barrioahorro.api.modules.sorteos

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
class PrizeController(
    private val service: PrizeService,
) {

    // ---- Vecino ----
    @GetMapping("/cliente/sorteos/premios/pendientes")
    fun pending(auth: Authentication): List<WinnerNoticeResponse> = service.pendingNotices(userId(auth))

    @PostMapping("/cliente/sorteos/premios/{ticketId}/visto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun seen(auth: Authentication, @PathVariable ticketId: Long) {
        service.markNoticeSeen(userId(auth), ticketId)
    }

    @GetMapping("/cliente/sorteos/premios/{ticketId}")
    fun detail(auth: Authentication, @PathVariable ticketId: Long): PrizeDetailResponse =
        service.detail(userId(auth), ticketId)

    // ---- Comerciante ----
    @PostMapping("/comercio/sorteos/premios/verificar")
    fun verify(auth: Authentication, @RequestBody body: ScanCodeRequest): PrizeScanResult =
        service.verify(userId(auth), body.code)

    @PostMapping("/comercio/sorteos/premios/canjear")
    fun redeem(auth: Authentication, @RequestBody body: ScanCodeRequest): PrizeScanResult =
        service.redeem(userId(auth), body.code)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}