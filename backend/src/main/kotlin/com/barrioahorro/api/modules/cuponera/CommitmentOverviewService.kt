package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CommitmentOverviewResponse
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommitmentOverviewService(
    private val cuponeraService: CuponeraService,
    private val raffleService: RaffleService,
    private val jdbc: NamedParameterJdbcTemplate,
) {

    @Transactional
    fun overview(userId: Long): CommitmentOverviewResponse {
        val cuponera = cuponeraService.cycle(userId)
        val raffle = raffleService.cycle(userId)

        // Se consulta después de actualizar ambos ciclos, para que la vista vea los registros al día.
        val restricted = jdbc.queryForObject(
            "SELECT acceso_restringido FROM v_comercio_acceso_restringido WHERE comercio_id = :id",
            MapSqlParameterSource("id", userId),
            Boolean::class.java,
        ) ?: true

        return CommitmentOverviewResponse(
            restrictedAccess = restricted,
            cuponera = cuponera,
            raffle = raffle,
        )
    }
}