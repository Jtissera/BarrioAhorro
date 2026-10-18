package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.ScanRedemptionRequest
import com.barrioahorro.api.modules.cuponera.dto.ScanRedemptionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/cuponera/canjes")
class RedemptionController(
    private val service: RedemptionService,
) {

    @PostMapping("/validar")
    fun validate(
        auth: Authentication,
        @Valid @RequestBody body: ScanRedemptionRequest,
    ): ScanRedemptionResponse = service.validate(userId(auth), body.code)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}