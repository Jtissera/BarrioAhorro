package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CuponeraCommitmentResponse
import com.barrioahorro.api.modules.cuponera.dto.RatifyCuponeraRequest
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
@RequestMapping("/cuponera/aporte")
class CuponeraController(
    private val service: CuponeraService,
) {

    @GetMapping
    fun current(auth: Authentication): CuponeraCommitmentResponse =
        service.getCurrent(userId(auth))

    @PostMapping("/ratificar")
    fun ratify(
        auth: Authentication,
        @Valid @RequestBody body: RatifyCuponeraRequest,
    ): CuponeraCommitmentResponse = service.ratify(userId(auth), body)

    // ADAPTAR: asume que el name del Authentication es el id del usuario.
    // Si tu core/security expone el id de otra forma, cambialo solo acá.
    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}