package com.barrioahorro.api.modules.sorteos

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RaffleScheduler(
    private val draws: RaffleDrawService,
) {
    private val log = LoggerFactory.getLogger(RaffleScheduler::class.java)

    @Scheduled(cron = "0 * * * * *", zone = "America/Argentina/Buenos_Aires")
    fun run() {
        try {
            val programmed = draws.programNextDraw()
            if (programmed > 0) {
                log.info("Sorteos nuevos programados: {}", programmed)
            }
        } catch (e: Exception) {
            log.error("No se pudieron programar sorteos", e)
        }

        try {
            val executed = draws.executeDue()
            if (executed > 0) {
                log.info("Sorteos ejecutados: {}", executed)
            }
        } catch (e: Exception) {
            log.error("No se pudieron ejecutar sorteos", e)
        }
    }
}