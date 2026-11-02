package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.RedemptionFeedbackResponse
import com.barrioahorro.api.modules.cuponera.dto.ReportRedemptionRequest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class RedemptionFeedbackService(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    companion object {
        const val POINTS_DIVISOR = 4

        /** 1/4 del costo, redondeando hacia arriba y con mínimo 1. */
        fun bonusFor(cost: Int): Int =
            ((cost + POINTS_DIVISOR - 1) / POINTS_DIVISOR).coerceAtLeast(1)
    }

    private data class Target(val clientId: Long, val status: String, val cost: Int)

    @Transactional(readOnly = true)
    fun status(userId: Long, redemptionId: Long): RedemptionFeedbackResponse {
        loadTarget(userId, redemptionId, lock = false)
        return currentAnswer(redemptionId)
    }

    @Transactional
    fun confirm(userId: Long, redemptionId: Long): RedemptionFeedbackResponse {
        val target = loadTarget(userId, redemptionId, lock = true)
        ensureAnswerable(target, redemptionId)

        val bonus = bonusFor(target.cost)
        insertAnswer(redemptionId, userId, received = true, points = bonus)
        jdbc.update(
            """
            UPDATE perfiles_cliente
            SET saldo_puntos = saldo_puntos + :points,
                puntos_totales_historicos = puntos_totales_historicos + :points
            WHERE usuario_id = :userId
            """,
            MapSqlParameterSource().addValue("points", bonus).addValue("userId", userId),
        )
        return RedemptionFeedbackResponse(answered = true, received = true, pointsAwarded = bonus)
    }

    @Transactional
    fun report(userId: Long, redemptionId: Long, request: ReportRedemptionRequest): RedemptionFeedbackResponse {
        val description = request.description.trim()
        if (description.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Contanos qué pasó")

        val target = loadTarget(userId, redemptionId, lock = true)
        ensureAnswerable(target, redemptionId)

        insertAnswer(redemptionId, userId, received = false, points = 0)
        jdbc.update(
            """
            INSERT INTO reportes_canje (canje_id, cliente_id, motivo, descripcion)
            VALUES (:canje, :cliente, :motivo, :descripcion)
            """,
            MapSqlParameterSource()
                .addValue("canje", redemptionId)
                .addValue("cliente", userId)
                .addValue("motivo", request.reason)
                .addValue("descripcion", description),
        )
        return RedemptionFeedbackResponse(answered = true, received = false, pointsAwarded = 0)
    }

    // ---- Helpers ------------------------------------------------------------------

    /** 404 si el canje no existe o no es del vecino (no se revela cuál de los dos). */
    private fun loadTarget(userId: Long, redemptionId: Long, lock: Boolean): Target {
        val sql = "SELECT cliente_id, estado::text AS status, puntos_costo FROM canjes_cupon WHERE id = :id" +
                if (lock) " FOR UPDATE" else ""
        val target = jdbc.query(sql, MapSqlParameterSource("id", redemptionId)) { rs, _ ->
            Target(rs.getLong("cliente_id"), rs.getString("status"), rs.getInt("puntos_costo"))
        }.firstOrNull()
        if (target == null || target.clientId != userId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese canje")
        }
        return target
    }

    private fun ensureAnswerable(target: Target, redemptionId: Long) {
        if (target.status != "VALIDADO") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Este canje todavía no fue validado por el comercio")
        }
        if (currentAnswer(redemptionId).answered) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya respondiste por este canje")
        }
    }

    private fun currentAnswer(redemptionId: Long): RedemptionFeedbackResponse =
        jdbc.query(
            "SELECT recibido, puntos_otorgados FROM confirmaciones_canje WHERE canje_id = :id",
            MapSqlParameterSource("id", redemptionId),
        ) { rs, _ ->
            RedemptionFeedbackResponse(
                answered = true,
                received = rs.getBoolean("recibido"),
                pointsAwarded = rs.getInt("puntos_otorgados"),
            )
        }.firstOrNull() ?: RedemptionFeedbackResponse(answered = false, received = null, pointsAwarded = 0)

    private fun insertAnswer(redemptionId: Long, userId: Long, received: Boolean, points: Int) {
        jdbc.update(
            """
            INSERT INTO confirmaciones_canje (canje_id, cliente_id, recibido, puntos_otorgados)
            VALUES (:canje, :cliente, :recibido, :puntos)
            """,
            MapSqlParameterSource()
                .addValue("canje", redemptionId)
                .addValue("cliente", userId)
                .addValue("recibido", received)
                .addValue("puntos", points),
        )
    }
}