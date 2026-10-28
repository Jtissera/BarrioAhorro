package com.barrioahorro.api.modules.admin

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.sql.ResultSet
import java.time.OffsetDateTime

private const val PENDING_SQL = """
    SELECT 'CUPON' AS kind, cu.id AS id, pc.nombre_negocio AS business_name, pc.direccion AS business_address,
           cu.nombre_producto AS title, cu.descripcion AS description, cu.foto_url AS photo_url,
           cc.fecha_ratificacion AS submitted_at, cu.costo_puntos AS points,
           cu.stock_comprometido::int AS stock, cu.valor_comercio AS amount
    FROM cupones_cuponera cu
    JOIN compromisos_cuponera cc ON cc.id = cu.compromiso_id
    JOIN perfiles_comercio pc ON pc.usuario_id = cc.comercio_id
    WHERE cu.estado_revision = 'PENDIENTE' AND cc.estado = 'RATIFICADO'
    UNION ALL
    SELECT 'SORTEO', cs.id, pc.nombre_negocio, pc.direccion,
       COALESCE(cs.premio_nombre, cs.premio_descripcion), cs.premio_descripcion, cs.premio_foto_url,
       cs.fecha_ratificacion, NULL::int, NULL::int, cs.premio_valor
    FROM compromisos_sorteo cs
    JOIN perfiles_comercio pc ON pc.usuario_id = cs.comercio_id
    WHERE cs.estado_revision = 'PENDIENTE' AND cs.estado = 'RATIFICADO'
"""

@Service
class AdminService(private val jdbc: NamedParameterJdbcTemplate) {

    fun pending(): List<AdminPublicationResponse> =
        jdbc.query("SELECT * FROM ($PENDING_SQL) p ORDER BY submitted_at DESC", MapSqlParameterSource()) { rs, _ -> map(rs) }

    fun detail(kind: PublicationKind, id: Long): AdminPublicationResponse =
        jdbc.query(
            "SELECT * FROM ($PENDING_SQL) p WHERE kind = :kind AND id = :id",
            MapSqlParameterSource().addValue("kind", kind.name).addValue("id", id),
        ) { rs, _ -> map(rs) }.firstOrNull() ?: throw notFound()

    @Transactional
    fun approve(kind: PublicationKind, id: Long) {
        val table = if (kind == PublicationKind.CUPON) "cupones_cuponera" else "compromisos_sorteo"
        val updated = jdbc.update(
            "UPDATE $table SET estado_revision = 'APROBADO', revisado_en = now() WHERE id = :id AND estado_revision = 'PENDIENTE'",
            MapSqlParameterSource("id", id),
        )
        if (updated != 1) throw notFound()
    }

    @Transactional
    fun reject(kind: PublicationKind, id: Long, motivo: String) {
        val reason = motivo.trim()
        if (reason.length !in 15..300) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo debe tener entre 15 y 300 caracteres")
        }
        val params = MapSqlParameterSource().addValue("id", id).addValue("reason", reason)

        when (kind) {
            PublicationKind.CUPON -> {
                val commitmentId = jdbc.queryForList(
                    """UPDATE cupones_cuponera cu
                       SET estado_revision = 'RECHAZADO', motivo_rechazo = :reason, revisado_en = now()
                       FROM compromisos_cuponera cc
                       WHERE cu.id = :id AND cu.estado_revision = 'PENDIENTE'
                         AND cc.id = cu.compromiso_id AND cc.estado = 'RATIFICADO'
                       RETURNING cu.compromiso_id""",
                    params, Long::class.java,
                ).firstOrNull() ?: throw notFound()

                // El compromiso vuelve a PENDIENTE: el comercio debe ratificarlo de nuevo.
                val reopened = jdbc.update(
                    "UPDATE compromisos_cuponera SET estado = 'PENDIENTE' WHERE id = :cid AND estado = 'RATIFICADO'",
                    MapSqlParameterSource("cid", commitmentId),
                )
                if (reopened != 1) throw notFound()
            }
            PublicationKind.SORTEO -> {
                val updated = jdbc.update(
                    """UPDATE compromisos_sorteo
                       SET estado_revision = 'RECHAZADO', motivo_rechazo = :reason,
                           revisado_en = now(), estado = 'PENDIENTE'
                       WHERE id = :id AND estado_revision = 'PENDIENTE' AND estado = 'RATIFICADO'""",
                    params,
                )
                if (updated != 1) throw notFound()
            }
        }
    }

    private fun notFound() =
        ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación ya fue revisada o no existe")

    private fun ResultSet.intOrNull(col: String): Int? = getInt(col).takeUnless { wasNull() }

    private fun map(rs: ResultSet) = AdminPublicationResponse(
        id = rs.getLong("id"),
        kind = rs.getString("kind"),
        businessName = rs.getString("business_name"),
        businessAddress = rs.getString("business_address"),
        title = rs.getString("title").orEmpty(),
        description = rs.getString("description"),
        photoUrl = rs.getString("photo_url"),
        submittedAt = rs.getObject("submitted_at", OffsetDateTime::class.java),
        points = rs.intOrNull("points"),
        stock = rs.intOrNull("stock"),
        amount = rs.getBigDecimal("amount"),
    )
}
