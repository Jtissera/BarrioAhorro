package com.barrioahorro.api.modules.notification

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class RejectionLinkController {

    @GetMapping("/app/rechazo/{kind}", produces = [MediaType.TEXT_HTML_VALUE])
    fun open(@PathVariable kind: String): ResponseEntity<String> {
        val section = kind.lowercase()
        if (section != "cuponera" && section != "sorteo") {
            return ResponseEntity.notFound().build()
        }
        val deepLink = "barrioahorro://rechazo/$section"

        val html = """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>BarrioAhorro</title>
            </head>
            <body style="font-family:Arial,sans-serif;background:#fcf5e3;text-align:center;padding:48px 24px;color:#2b2118">
              <h2>Abrí BarrioAhorro</h2>
              <p>Tocá el botón para resolver el producto rechazado de tu $section.</p>
              <p style="margin:32px 0">
                <a href="$deepLink"
                   style="background:#d32f12;color:#ffffff;padding:14px 24px;border-radius:28px;text-decoration:none;font-weight:bold">
                  Abrir la app
                </a>
              </p>
              <p style="font-size:12px;color:#7a685d">Si no se abre, verificá que tengas la app instalada.</p>
              <script>setTimeout(function () { window.location.href = "$deepLink"; }, 300);</script>
            </body>
            </html>
        """.trimIndent()

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }
}