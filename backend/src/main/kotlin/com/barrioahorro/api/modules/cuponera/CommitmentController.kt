package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CommitmentOverviewResponse
import com.barrioahorro.api.modules.cuponera.dto.RaffleCycleResponse
import com.barrioahorro.api.modules.cuponera.dto.RatifyRaffleRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/aportes")
class CommitmentController(
    private val overviewService: CommitmentOverviewService,
    private val raffleService: RaffleService,
) {

    @GetMapping("/resumen")
    fun overview(auth: Authentication): CommitmentOverviewResponse =
        overviewService.overview(userId(auth))

    @GetMapping("/sorteo")
    fun raffle(auth: Authentication): RaffleCycleResponse =
        raffleService.cycle(userId(auth))

    @PostMapping("/sorteo/ratificar")
    fun ratifyRaffle(
        auth: Authentication,
        @Valid @RequestBody body: RatifyRaffleRequest,
    ): RaffleCycleResponse = raffleService.ratify(userId(auth), body)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}