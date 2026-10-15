package com.barrioahorro.api.modules.sorteo

import com.barrioahorro.api.modules.sorteo.dto.RatifySorteoRequest
import com.barrioahorro.api.modules.sorteo.dto.SorteoCommitmentResponse
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
@RequestMapping("/sorteo/compromiso")
class SorteoController(
    private val service: SorteoService,
) {

    @GetMapping
    fun current(auth: Authentication): SorteoCommitmentResponse =
        service.getCurrent(userId(auth))

    @PostMapping("/ratificar")
    fun ratify(
        auth: Authentication,
        @Valid @RequestBody body: RatifySorteoRequest,
    ): SorteoCommitmentResponse = service.ratify(userId(auth), body)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}