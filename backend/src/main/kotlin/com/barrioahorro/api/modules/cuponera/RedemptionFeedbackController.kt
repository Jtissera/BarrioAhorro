package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.RedemptionFeedbackResponse
import com.barrioahorro.api.modules.cuponera.dto.ReportRedemptionRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/cuponera/canjes/{id}")
class RedemptionFeedbackController(
    private val service: RedemptionFeedbackService,
) {

    @GetMapping("/recepcion")
    fun status(auth: Authentication, @PathVariable id: Long): RedemptionFeedbackResponse =
        service.status(userId(auth), id)

    @PostMapping("/recepcion")
    fun confirm(auth: Authentication, @PathVariable id: Long): RedemptionFeedbackResponse =
        service.confirm(userId(auth), id)

    @PostMapping("/reporte")
    fun report(
        auth: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody body: ReportRedemptionRequest,
    ): RedemptionFeedbackResponse = service.report(userId(auth), id, body)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}