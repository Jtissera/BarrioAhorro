package com.barrioahorro.api.modules.sorteos

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RaffleScheduler(
    private val draws: RaffleDrawService,
) {
    private val log = LoggerFactory.getLogger(RaffleScheduler::class.java)

    /** Corre cada hora, 5 minutos después del CommitmentScheduler. */
    @Scheduled(cron = "0 10 * * * *", zone = "America/Argentina/Buenos_Aires")
    fun run() {
        try {
            draws.programNextDraw()
        } catch (e: Exception) {
            log.error("No se pudieron programar sorteos", e)
        }
        try {
            draws.executeDue()
        } catch (e: Exception) {
            log.error("No se pudieron ejecutar sorteos", e)
        }
    }
}