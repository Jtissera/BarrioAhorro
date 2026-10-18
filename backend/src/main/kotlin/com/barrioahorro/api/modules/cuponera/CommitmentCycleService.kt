package com.barrioahorro.api.modules.cuponera

import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Los nombres de tabla y columna son constantes del código: nunca vienen del cliente. */
enum class CycleKind(
    val table: String,
    val startColumn: String,
    val endColumn: String,
    val lengthDays: Long,
    val earlyWindowDays: Long,
) {
    CUPONERA("compromisos_cuponera", "periodo_inicio", "periodo_fin", lengthDays = 15, earlyWindowDays = 5),
    RAFFLE("compromisos_sorteo", "ciclo_inicio", "ciclo_fin", lengthDays = 120, earlyWindowDays = 14),
}

data class CycleRow(
    val id: Long,
    val start: LocalDate,
    val end: LocalDate,
    val deadline: OffsetDateTime,
    /** PENDIENTE | RATIFICADO | VENCIDO */
    val status: String,
)

/**
 * current: compromiso ratificado que rige hoy.
 * next: compromiso a ratificar (pendiente) o ya ratificado para un período futuro.
 */
data class CycleState(val current: CycleRow?, val next: CycleRow?) {
    val canRatify: Boolean
        get() = next != null && next.status == "PENDIENTE" && OffsetDateTime.now().isBefore(next.deadline)

    /** EARLY: se renueva con el actual vigente. LATE: no hay vigente (cuenta restringida). */
    val ratificationType: String?
        get() = when {
            !canRatify -> null
            current != null -> "EARLY"
            else -> "LATE"
        }
}

@Service
class CommitmentCycleService(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    companion object {
        const val GRACE_DAYS = 3L
        val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }

    /**
     * Deja los compromisos del comercio al día y devuelve su estado:
     * vence los pendientes cuyo plazo pasó, abre el próximo período cuando
     * entra en la ventana de ratificación anticipada y, si el último venció,
     * recorta su fin y abre uno nuevo que empieza hoy (ratificación tardía).
     */
    @Transactional
    fun ensure(userId: Long, kind: CycleKind): CycleState {
        val today = LocalDate.now(ZONE)
        lock(userId, kind)
        expirePending(userId, kind)

        val latest = loadLatest(userId, kind, 1).firstOrNull()
        when {
            latest == null -> create(userId, kind, today)

            latest.status == "VENCIDO" -> {
                if (!latest.end.isBefore(today)) trimEnd(latest.id, kind, today.minusDays(1))
                create(userId, kind, today)
            }

            latest.status == "RATIFICADO" && !latest.start.isAfter(today) -> when {
                // El período terminó y nadie abrió el siguiente (p. ej. el comercio estuvo ausente).
                today.isAfter(latest.end) -> create(userId, kind, today)
                ChronoUnit.DAYS.between(today, latest.end) <= kind.earlyWindowDays ->
                    create(userId, kind, latest.end.plusDays(1))
            }
        }

        return stateOf(userId, kind, today)
    }

    private fun stateOf(userId: Long, kind: CycleKind, today: LocalDate): CycleState {
        val rows = loadLatest(userId, kind, 2)
        val latest = rows.firstOrNull()
        val current = rows.firstOrNull {
            it.status == "RATIFICADO" && !it.start.isAfter(today) && !it.end.isBefore(today)
        }
        val next = latest?.takeIf {
            it.id != current?.id &&
                    (it.status == "PENDIENTE" || (it.status == "RATIFICADO" && it.start.isAfter(today)))
        }
        return CycleState(current, next)
    }

    private fun loadLatest(userId: Long, kind: CycleKind, limit: Int): List<CycleRow> =
        jdbc.query(
            """
            SELECT id,
                   ${kind.startColumn} AS start_date,
                   ${kind.endColumn}   AS end_date,
                   fecha_limite_ratificacion AS deadline,
                   estado::text        AS status
            FROM ${kind.table}
            WHERE comercio_id = :userId
            ORDER BY ${kind.startColumn} DESC
            LIMIT $limit
            """,
            MapSqlParameterSource("userId", userId),
        ) { rs, _ ->
            CycleRow(
                id = rs.getLong("id"),
                start = rs.getObject("start_date", LocalDate::class.java),
                end = rs.getObject("end_date", LocalDate::class.java),
                deadline = rs.getObject("deadline", OffsetDateTime::class.java),
                status = rs.getString("status"),
            )
        }

    private fun expirePending(userId: Long, kind: CycleKind) {
        jdbc.update(
            """
            UPDATE ${kind.table}
            SET estado = 'VENCIDO'
            WHERE comercio_id = :userId
              AND estado = 'PENDIENTE'
              AND fecha_limite_ratificacion <= now()
            """,
            MapSqlParameterSource("userId", userId),
        )
    }

    private fun trimEnd(id: Long, kind: CycleKind, newEnd: LocalDate) {
        jdbc.update(
            "UPDATE ${kind.table} SET ${kind.endColumn} = :newEnd WHERE id = :id AND estado = 'VENCIDO'",
            MapSqlParameterSource().addValue("newEnd", newEnd).addValue("id", id),
        )
    }

    private fun create(userId: Long, kind: CycleKind, start: LocalDate) {
        val end = start.plusDays(kind.lengthDays - 1)
        val deadline = start.plusDays(GRACE_DAYS).atStartOfDay(ZONE).toOffsetDateTime()
        val prizeColumn = if (kind == CycleKind.RAFFLE) ", premio_descripcion" else ""
        val prizeValue = if (kind == CycleKind.RAFFLE) ", ''" else ""

        // ON CONFLICT DO NOTHING cubre la unicidad y la exclusión de períodos.
        jdbc.update(
            """
            INSERT INTO ${kind.table}
                (comercio_id, ${kind.startColumn}, ${kind.endColumn}, fecha_limite_ratificacion$prizeColumn)
            VALUES (:userId, :start, :end, :deadline$prizeValue)
            ON CONFLICT DO NOTHING
            """,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("start", start)
                .addValue("end", end)
                .addValue("deadline", deadline),
        )
    }

    /** Serializa las operaciones de un mismo comercio y tipo (se libera al terminar la transacción). */
    private fun lock(userId: Long, kind: CycleKind) {
        jdbc.query(
            "SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
            MapSqlParameterSource("key", "${kind.name}:$userId"),
            RowCallbackHandler { },
        )
    }
}