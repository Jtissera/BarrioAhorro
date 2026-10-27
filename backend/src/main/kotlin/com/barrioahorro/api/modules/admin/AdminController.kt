package com.barrioahorro.api.modules.admin

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/publicaciones")
class AdminController(private val service: AdminService) {

    @GetMapping
    fun list() = service.pending()

    @GetMapping("/{kind}/{id}")
    fun detail(@PathVariable kind: String, @PathVariable id: Long) = service.detail(parse(kind), id)

    @PostMapping("/{kind}/{id}/aprobar")
    fun approve(@PathVariable kind: String, @PathVariable id: Long): ResponseEntity<Void> {
        service.approve(parse(kind), id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{kind}/{id}/rechazar")
    fun reject(
        @PathVariable kind: String,
        @PathVariable id: Long,
        @Valid @RequestBody body: RejectRequest,
    ): ResponseEntity<Void> {
        service.reject(parse(kind), id, body.motivo)
        return ResponseEntity.noContent().build()
    }

    private fun parse(kind: String): PublicationKind =
        PublicationKind.entries.firstOrNull { it.name == kind.uppercase() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de publicación inválido")
}