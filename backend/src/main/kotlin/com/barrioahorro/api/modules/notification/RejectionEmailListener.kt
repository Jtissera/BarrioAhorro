package com.barrioahorro.api.modules.notification

import com.barrioahorro.api.modules.admin.ProductRejectedEvent
import com.barrioahorro.api.modules.admin.PublicationKind
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.util.HtmlUtils

@Component
class RejectionEmailListener(
    private val jdbc: NamedParameterJdbcTemplate,
    private val emailService: EmailService,
    @Value("\${app.links.base-url}") private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(RejectionEmailListener::class.java)

    private data class Recipient(val email: String, val businessName: String?)

    /** Solo corre si el rechazo se confirmó en la base. Un fallo acá nunca revierte el rechazo. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onRejected(event: ProductRejectedEvent) {
        try {
            val recipient = jdbc.query(
                """SELECT u.email::text AS email, pc.nombre_negocio AS negocio
                   FROM usuarios u
                   JOIN perfiles_comercio pc ON pc.usuario_id = u.id
                   WHERE u.id = :id""",
                MapSqlParameterSource("id", event.comercioId),
            ) { rs, _ -> Recipient(rs.getString("email"), rs.getString("negocio")) }.firstOrNull()

            if (recipient == null) {
                log.warn("Rechazo sin destinatario: comercio {} no encontrado", event.comercioId)
                return
            }

            val isCupon = event.kind == PublicationKind.CUPON
            val section = if (isCupon) "cuponera" else "sorteo"
            val link = "${baseUrl.trimEnd('/')}/app/rechazo/$section"

            val product = HtmlUtils.htmlEscape(event.productName.ifBlank { "tu producto" })
            val reason = HtmlUtils.htmlEscape(event.reason)
            val greeting = recipient.businessName?.let { "Hola ${HtmlUtils.htmlEscape(it)}," } ?: "Hola,"

            val html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#2b2118">
                  <h2 style="color:#d32f12">Rechazaron tu producto</h2>
                  <p>$greeting</p>
                  <p>Un administrador rechazó el producto <b>$product</b>,
                     que presentaste para la <b>$section</b>.</p>
                  <p><b>Motivo:</b> $reason</p>
                  <p>Mientras no lo resuelvas, tu comercio no puede avanzar con la publicación.
                     Ingresá a la app para ratificar nuevamente:</p>
                  <p style="margin:24px 0">
                    <a href="$link"
                       style="background:#d32f12;color:#ffffff;padding:12px 20px;border-radius:24px;text-decoration:none;font-weight:bold">
                      Ir a la app
                    </a>
                  </p>
                  <p style="font-size:12px;color:#7a685d">Este enlace no vence y te pedirá iniciar sesión.</p>
                </div>
            """.trimIndent()

            val subject = "Rechazaron tu producto de la $section"
            emailService.send(recipient.email, subject, html)
        } catch (e: Exception) {
            log.error("No se pudo preparar el mail de rechazo: {}", e.message)
        }
    }
}