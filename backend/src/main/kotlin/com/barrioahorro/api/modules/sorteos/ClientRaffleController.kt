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
@RequestMapping("/api/cliente/sorteos")
class ClientRaffleController(
    private val service: ClientRaffleService,
) {

    @GetMapping
    fun overview(auth: Authentication): ClientRafflesResponse = service.overview(userId(auth))

    @GetMapping("/mis-tickets")
    fun myTickets(auth: Authentication): MyTicketsResponse = service.myTickets(userId(auth))

    @PostMapping("/{id}/tickets")
    fun buy(
        auth: Authentication,
        @PathVariable id: Long,
        @RequestBody(required = false) body: BuyTicketRequest?,
    ): BuyTicketResponse = service.buyTicket(userId(auth), id, body?.quantity ?: 1)

    @PostMapping("/desbloqueo-visto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unlockSeen(auth: Authentication) {
        service.markUnlockSeen(userId(auth))
    }

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}