package com.barrioahorro.api.modules.cuponera

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CommitmentScheduler(
    private val jdbc: NamedParameterJdbcTemplate,
    private val cycles: CommitmentCycleService,
) {
    private val log = LoggerFactory.getLogger(CommitmentScheduler::class.java)

    @Scheduled(cron = "0 5 * * * *", zone = "America/Argentina/Buenos_Aires")
    fun run() {
        val expired = jdbc.queryForObject("SELECT fn_vencer_compromisos()", emptyMap<String, Any>(), Int::class.java)
        log.info("Compromisos vencidos: {}", expired)

        val businessIds = jdbc.queryForList(
            "SELECT usuario_id FROM perfiles_comercio WHERE onboarding_completado",
            emptyMap<String, Any>(),
            Long::class.java,
        )
        for (id in businessIds.filterNotNull()) {
            for (kind in CycleKind.entries) {
                try {
                    cycles.ensure(id, kind)
                } catch (e: Exception) {
                    log.error("No se pudo actualizar {} del comercio {}", kind, id, e)
                }
            }
        }
    }
}