package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.RaffleCommitmentResponse
import com.barrioahorro.api.modules.cuponera.dto.RaffleCycleResponse
import com.barrioahorro.api.modules.cuponera.dto.RatifyRaffleRequest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class RaffleService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val cycles: CommitmentCycleService,
    private val commitments: CuponeraCommitmentRepository,
) {

    @Transactional
    fun cycle(userId: Long): RaffleCycleResponse {
        requireBusiness(userId)
        val state = cycles.ensure(userId, CycleKind.RAFFLE)
        return RaffleCycleResponse(
            current = state.current?.let(::load),
            next = state.next?.let(::load),
            canRatify = state.canRatify,
            ratificationType = state.ratificationType,
        )
    }

    @Transactional
    fun ratify(userId: Long, request: RatifyRaffleRequest): RaffleCycleResponse {
        val info = requireBusiness(userId)
        if (!info.onboardingCompletado) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Completá el onboarding antes de ratificar el sorteo")
        }

        val state = cycles.ensure(userId, CycleKind.RAFFLE)
        val target = state.next?.takeIf { it.status == "PENDIENTE" }
            ?: throw ResponseStatusException(
                HttpStatus.CONFLICT,
                if (state.next?.status == "RATIFICADO") {
                    "Ya ratificaste el premio del próximo sorteo"
                } else {
                    "Todavía no es momento de ratificar el próximo sorteo"
                },
            )
        if (!OffsetDateTime.now().isBefore(target.deadline)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El plazo para ratificar este sorteo ya venció")
        }

        val params = MapSqlParameterSource()
            .addValue("id", target.id)
            .addValue("name", request.prizeName.trim())
            .addValue("prize", request.prizeDescription.trim())
            .addValue("value", request.prizeValue)
        jdbc.update(
            """
            UPDATE compromisos_sorteo
            SET premio_nombre = :name, premio_descripcion = :prize, premio_valor = :value
            WHERE id = :id AND estado = 'PENDIENTE'
            """,
            params,
        )
        val updated = jdbc.update(
            "UPDATE compromisos_sorteo SET estado = 'RATIFICADO' WHERE id = :id AND estado = 'PENDIENTE'",
            params,
        )
        if (updated != 1) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No se pudo ratificar el sorteo, intentá de nuevo")
        }

        return cycle(userId)
    }

    private fun load(row: CycleRow): RaffleCommitmentResponse {
        val prize = jdbc.queryForObject(
            "SELECT premio_nombre, premio_descripcion, premio_valor FROM compromisos_sorteo WHERE id = :id",
            MapSqlParameterSource("id", row.id),
        ) { rs, _ -> Triple(rs.getString(1), rs.getString(2), rs.getBigDecimal(3)) }!!

        return RaffleCommitmentResponse(
            id = row.id,
            cycleStart = row.start,
            cycleEnd = row.end,
            ratificationDeadline = row.deadline,
            status = row.status,
            prizeName = prize.first?.takeIf { it.isNotBlank() },
            prizeDescription = prize.second?.takeIf { it.isNotBlank() },
            prizeValue = prize.third,
        )
    }

    private fun requireBusiness(userId: Long): BusinessCommitmentInfo =
        commitments.findBusinessInfo(userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios participan del sorteo")
}