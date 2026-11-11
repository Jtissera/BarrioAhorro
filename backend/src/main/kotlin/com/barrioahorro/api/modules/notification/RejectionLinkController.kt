package com.barrioahorro.api.modules.notification

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class RejectionLinkController {

    /**
     * Enlace de los mails de rechazo. No muestra nada: redirige al deep link de la app.
     * Solo acepta cuponera o sorteo, no refleja datos del usuario y no usa token.
     */
    @GetMapping("/app/rechazo/{kind}")
    fun open(@PathVariable kind: String): ResponseEntity<Void> {
        val section = kind.lowercase()
        if (section != "cuponera" && section != "sorteo") {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create("barrioahorro://rechazo/$section"))
            .build()
    }
}