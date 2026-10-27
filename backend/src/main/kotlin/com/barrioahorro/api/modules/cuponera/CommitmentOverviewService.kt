package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CommitmentOverviewResponse
import com.barrioahorro.api.modules.cuponera.dto.RejectionResponse
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
            rejection = if (restricted) findRejection(userId) else null,
        )
    }

    /** Último rechazo vigente: el compromiso no está ratificado y no existe otro posterior ratificado. */
    private fun findRejection(userId: Long): RejectionResponse? =
        jdbc.query(
            """
        SELECT kind, title, reason FROM (
            SELECT 'CUPON' AS kind, cu.nombre_producto AS title, cu.motivo_rechazo AS reason, cu.revisado_en AS reviewed_at
            FROM cupones_cuponera cu
            JOIN compromisos_cuponera cc ON cc.id = cu.compromiso_id
            WHERE cc.comercio_id = :id AND cu.estado_revision = 'RECHAZADO' AND cc.estado <> 'RATIFICADO'
              AND NOT EXISTS (SELECT 1 FROM compromisos_cuponera p
                              WHERE p.comercio_id = cc.comercio_id AND p.estado = 'RATIFICADO'
                                AND p.periodo_inicio > cc.periodo_inicio)
            UNION ALL
            SELECT 'SORTEO', COALESCE(cs.premio_nombre, cs.premio_descripcion), cs.motivo_rechazo, cs.revisado_en
            FROM compromisos_sorteo cs
            WHERE cs.comercio_id = :id AND cs.estado_revision = 'RECHAZADO' AND cs.estado <> 'RATIFICADO'
              AND NOT EXISTS (SELECT 1 FROM compromisos_sorteo p
                              WHERE p.comercio_id = cs.comercio_id AND p.estado = 'RATIFICADO'
                                AND p.ciclo_inicio > cs.ciclo_inicio)
        ) r
        ORDER BY reviewed_at DESC
        LIMIT 1
        """,
            MapSqlParameterSource("id", userId),
        ) { rs, _ -> RejectionResponse(rs.getString("kind"), rs.getString("title").orEmpty(), rs.getString("reason").orEmpty()) }
            .firstOrNull()
}