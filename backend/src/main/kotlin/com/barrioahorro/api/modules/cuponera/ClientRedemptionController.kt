package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.RedeemCouponRequest
import com.barrioahorro.api.modules.cuponera.dto.RedemptionResponse
import jakarta.validation.Valid
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
@RequestMapping("/api/cuponera/canjes")
class ClientRedemptionController(
    private val service: ClientRedemptionService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun redeem(auth: Authentication, @Valid @RequestBody body: RedeemCouponRequest): RedemptionResponse =
        service.redeem(userId(auth), body.couponId)

    @GetMapping
    fun list(auth: Authentication): List<RedemptionResponse> = service.list(userId(auth))

    @GetMapping("/{id}")
    fun detail(auth: Authentication, @PathVariable id: Long): RedemptionResponse =
        service.detail(userId(auth), id)

    @PostMapping("/{id}/cancelar")
    fun cancel(auth: Authentication, @PathVariable id: Long): RedemptionResponse =
        service.cancel(userId(auth), id)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}