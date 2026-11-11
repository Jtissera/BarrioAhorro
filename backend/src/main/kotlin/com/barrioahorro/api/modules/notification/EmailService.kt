package com.barrioahorro.api.modules.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * Envío de mails con la API de Resend. Nunca lanza excepciones: si el mail
 * falla, la acción que lo disparó (por ejemplo el rechazo del admin) sigue
 * adelante y el fallo queda en el log.
 */
@Service
class EmailService(
    restClientBuilder: RestClient.Builder,
    @Value("\${app.email.resend.api-key:}") private val apiKey: String,
    @Value("\${app.email.from}") private val from: String,
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    private val client: RestClient = restClientBuilder
        .baseUrl("https://api.resend.com")
        .build()

    /** @return true si Resend aceptó el mail. */
    fun send(to: String, subject: String, html: String): Boolean {
        if (apiKey.isBlank()) {
            log.warn("RESEND_API_KEY vacía: no se envió el mail '{}' a {}", subject, to)
            return false
        }

        return try {
            client.post()
                .uri("/emails")
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "BarrioAhorro/1.0")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "from" to from,
                        "to" to listOf(to),
                        "subject" to subject,
                        "html" to html,
                    ),
                )
                .retrieve()
                .toBodilessEntity()
            true
        } catch (e: Exception) {
            log.error("No se pudo enviar el mail '{}' a {}: {}", subject, to, e.message)
            false
        }
    }
}