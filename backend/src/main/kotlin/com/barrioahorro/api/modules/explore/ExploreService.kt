package com.barrioahorro.api.modules.explore

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExploreFilters(
    val latitude: Double?,
    val longitude: Double?,
    val radiusKm: Double?,
    val categoryIds: List<Int>,
    val types: List<String>,
    val query: String?,
)

enum class ExploreOrder { DISTANCE, CONFIRMATIONS, RECENT }

@Service
class ExploreService(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    companion object {
        const val MAX_RADIUS_KM = 20.0
        const val MAX_PAGE_SIZE = 50
        const val MAX_MAP_BUSINESSES = 100
        private val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
        val VALID_TYPES = setOf("PORCENTAJE", "DOS_POR_UNO", "CANTIDAD_ULTIMA_UNIDAD", "POR_MAYOR")

        private const val OFFER_IS_LIVE =
            "o.activa AND (o.vigente_hasta IS NULL OR o.vigente_hasta > now())"

        const val BUSINESS_IS_VISIBLE = """
            pc.onboarding_completado
            AND pc.ubicacion IS NOT NULL
            AND NOT COALESCE(
                (SELECT v.acceso_restringido FROM v_comercio_acceso_restringido v WHERE v.comercio_id = pc.usuario_id),
                TRUE)
        """

        private const val BUSINESS_IS_OPEN = """
            EXISTS (SELECT 1 FROM horarios_atencion h
                    WHERE h.comercio_id = pc.usuario_id AND h.activo
                      AND h.dia_semana = :isoDow
                      AND h.hora_inicio <= CAST(:nowTime AS time)
                      AND h.hora_fin > CAST(:nowTime AS time))
        """
    }

    // ---- Cliente --------------------------------------------------------------------

    fun requireClient(userId: Long) {
        val exists = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM perfiles_cliente WHERE usuario_id = :id)",
            MapSqlParameterSource("id", userId),
            Boolean::class.java,
        ) ?: false
        if (!exists) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los vecinos pueden explorar ofertas")
    }

    fun summary(userId: Long): ClientSummaryResponse {
        requireClient(userId)
        val points = jdbc.queryForObject(
            "SELECT saldo_puntos FROM perfiles_cliente WHERE usuario_id = :id",
            MapSqlParameterSource("id", userId),
            Int::class.java,
        ) ?: 0
        return ClientSummaryResponse(points)
    }

    @Transactional
    fun addFavorite(userId: Long, offerId: Long) {
        requireClient(userId)
        val exists = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM ofertas WHERE id = :id)",
            MapSqlParameterSource("id", offerId),
            Boolean::class.java,
        ) ?: false
        if (!exists) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada")

        jdbc.update(
            "INSERT INTO favoritos (cliente_id, oferta_id) VALUES (:c, :o) ON CONFLICT DO NOTHING",
            MapSqlParameterSource().addValue("c", userId).addValue("o", offerId),
        )
    }

    @Transactional
    fun removeFavorite(userId: Long, offerId: Long) {
        requireClient(userId)
        jdbc.update(
            "DELETE FROM favoritos WHERE cliente_id = :c AND oferta_id = :o",
            MapSqlParameterSource().addValue("c", userId).addValue("o", offerId),
        )
    }

    /**
     * Ofertas guardadas por el vecino, las más recientes primero. Incluye las finalizadas.
     */
    @Transactional(readOnly = true)
    fun favorites(userId: Long, lat: Double?, lng: Double?, page: Int, size: Int): ExploreOffersPage {
        requireClient(userId)

        val hasLocation = lat != null && lng != null
        val filters = ExploreFilters(lat, lng, null, emptyList(), emptyList(), null)
        val params = baseParams(filters, userId)
        val distance = if (hasLocation) DISTANCE_EXPR else "CAST(NULL AS double precision)"
        val from = "$OFFER_FROM JOIN favoritos fav ON fav.oferta_id = o.id AND fav.cliente_id = :clientId " +
                "WHERE (${BUSINESS_IS_VISIBLE.trimIndent()})"

        val total = jdbc.queryForObject("SELECT COUNT(*) FROM $from", params, Long::class.java) ?: 0L

        params.addValue("limit", size).addValue("offset", page.toLong() * size)
        val items = jdbc.query(
            """
            SELECT ${selectColumns(distance)}
            FROM $from
            ORDER BY fav.created_at DESC, o.id
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params,
        ) { rs, _ -> rs.toOffer() }

        return ExploreOffersPage(
            items = items,
            total = total,
            page = page,
            size = size,
            hasMore = (page.toLong() + 1) * size < total,
        )
    }

    // ---- Lista de ofertas -----------------------------------------------------------

    @Transactional(readOnly = true)
    fun offers(userId: Long, f: ExploreFilters, order: ExploreOrder, page: Int, size: Int): ExploreOffersPage {
        requireClient(userId)

        val hasLocation = f.latitude != null && f.longitude != null
        val params = baseParams(f, userId)
        val where = offerWhere(f, hasLocation, params)
        val distance = if (hasLocation) DISTANCE_EXPR else "CAST(NULL AS double precision)"

        val effectiveOrder = if (order == ExploreOrder.DISTANCE && !hasLocation) ExploreOrder.RECENT else order
        val orderBy = when (effectiveOrder) {
            ExploreOrder.DISTANCE -> "distancia_m ASC NULLS LAST, o.id"
            ExploreOrder.CONFIRMATIONS -> "confirmaciones DESC, o.id"
            ExploreOrder.RECENT -> "o.created_at DESC, o.id"
        }

        val total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM $OFFER_FROM WHERE $where",
            params,
            Long::class.java,
        ) ?: 0L

        params.addValue("limit", size).addValue("offset", page.toLong() * size)
        val items = jdbc.query(
            """
            SELECT ${selectColumns(distance)}
            FROM $OFFER_FROM
            WHERE $where
            ORDER BY $orderBy
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params,
        ) { rs, _ -> rs.toOffer() }

        return ExploreOffersPage(
            items = items,
            total = total,
            page = page,
            size = size,
            hasMore = (page.toLong() + 1) * size < total,
        )
    }

    // ---- Pines del mapa -------------------------------------------------------------

    @Transactional(readOnly = true)
    fun businesses(userId: Long, f: ExploreFilters): List<ExploreBusinessResponse> {
        requireClient(userId)
        if (f.latitude == null || f.longitude == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Necesitamos tu ubicación para mostrar el mapa")
        }

        val params = baseParams(f, userId)
        params.addValue("limit", MAX_MAP_BUSINESSES)
        val radius = radiusMeters(f)
        params.addValue("radius", radius)

        val categoryClause = if (f.categoryIds.isNotEmpty()) {
            params.addValue("categoryIds", f.categoryIds)
            "AND pc.rubro_id IN (:categoryIds)"
        } else ""
        val typeClause = if (f.types.isNotEmpty()) {
            params.addValue("types", f.types)
            "AND o.tipo_oferta::text IN (:types)"
        } else ""
        val queryClause = if (f.query != null) {
            params.addValue("q", likePattern(f.query))
            "AND (pc.nombre_negocio ILIKE :q ESCAPE '\\' OR o.nombre_producto ILIKE :q ESCAPE '\\')"
        } else ""

        return jdbc.query(
            """
            SELECT pc.usuario_id, pc.nombre_negocio, r.nombre AS rubro,
                   ST_Y(CAST(pc.ubicacion AS geometry)) AS lat,
                   ST_X(CAST(pc.ubicacion AS geometry)) AS lng,
                   $DISTANCE_EXPR AS distancia_m,
                   ($BUSINESS_IS_OPEN) AS abierto,
                   live.n AS ofertas,
                   (SELECT ph.url FROM fotos_comercio ph WHERE ph.comercio_id = pc.usuario_id
                     ORDER BY ph.orden, ph.id LIMIT 1) AS foto
            FROM perfiles_comercio pc
            LEFT JOIN rubros r ON r.id = pc.rubro_id
            CROSS JOIN LATERAL (
                SELECT COUNT(*) AS n FROM ofertas o
                WHERE o.comercio_id = pc.usuario_id AND $OFFER_IS_LIVE $typeClause $queryClause
            ) live
            WHERE $BUSINESS_IS_VISIBLE
              AND live.n > 0
              AND ST_DWithin(pc.ubicacion, $POINT_EXPR, :radius)
              $categoryClause
            ORDER BY distancia_m ASC, pc.usuario_id
            LIMIT :limit
            """.trimIndent(),
            params,
        ) { rs, _ ->
            ExploreBusinessResponse(
                id = rs.getLong("usuario_id"),
                name = rs.getString("nombre_negocio"),
                category = rs.getString("rubro"),
                latitude = rs.getDouble("lat"),
                longitude = rs.getDouble("lng"),
                distanceMeters = rs.getDouble("distancia_m").toInt(),
                open = rs.getBoolean("abierto"),
                activeOffers = rs.getInt("ofertas"),
                photoUrl = rs.getString("foto"),
            )
        }
    }

    /** 404 si el comercio no existe o no es visible (mismo criterio que Explorar). */
    fun requireVisibleBusiness(businessId: Long) {
        val visible = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM perfiles_comercio pc WHERE pc.usuario_id = :id AND $BUSINESS_IS_VISIBLE)",
            MapSqlParameterSource("id", businessId),
            Boolean::class.java,
        ) ?: false
        if (!visible) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Este comercio ya no está disponible")
    }

    /** Ofertas vigentes de un comercio, con la misma forma que las de Explorar (favorita, confirmaciones, distancia). */
    @Transactional(readOnly = true)
    fun businessOffers(userId: Long, businessId: Long, lat: Double?, lng: Double?): List<ExploreOfferResponse> {
        requireClient(userId)
        requireVisibleBusiness(businessId)

        val hasLocation = lat != null && lng != null
        val params = baseParams(ExploreFilters(lat, lng, null, emptyList(), emptyList(), null), userId)
            .addValue("businessId", businessId)
        val distance = if (hasLocation) DISTANCE_EXPR else "CAST(NULL AS double precision)"

        return jdbc.query(
            """
            SELECT ${selectColumns(distance)}
            FROM $OFFER_FROM
            WHERE ($OFFER_IS_LIVE) AND pc.usuario_id = :businessId
            ORDER BY o.created_at DESC, o.id
            """.trimIndent(),
            params,
        ) { rs, _ -> rs.toOffer() }
    }

    // ---- Detalle de oferta ----------------------------------------------------------

    /**
     * Una oferta, vigente o finalizada. Si el comercio no es visible (restringido, sin onboarding
     * o sin ubicación) devuelve 404: sus ofertas no se muestran.
     */
    @Transactional(readOnly = true)
    fun offerDetail(userId: Long, offerId: Long, lat: Double?, lng: Double?): OfferDetailResponse {
        requireClient(userId)

        val hasLocation = lat != null && lng != null
        val params = baseParams(ExploreFilters(lat, lng, null, emptyList(), emptyList(), null), userId)
            .addValue("offerId", offerId)
        val distance = if (hasLocation) DISTANCE_EXPR else "CAST(NULL AS double precision)"

        val row = jdbc.query(
            """
            SELECT ${selectColumns(distance)},
                   pc.direccion AS direccion,
                   (SELECT ph.url FROM fotos_comercio ph WHERE ph.comercio_id = pc.usuario_id
                     ORDER BY ph.orden, ph.id LIMIT 1) AS foto_comercio,
                   CASE WHEN o.activa THEN o.vigente_hasta
                        ELSE LEAST(COALESCE(o.vigente_hasta, o.updated_at), o.updated_at) END AS finalizo
            FROM $OFFER_FROM
            WHERE o.id = :offerId AND (${BUSINESS_IS_VISIBLE.trimIndent()})
            """.trimIndent(),
            params,
        ) { rs, _ ->
            Triple(
                rs.toOffer(),
                rs.getString("direccion") to rs.getString("foto_comercio"),
                rs.getObject("finalizo", OffsetDateTime::class.java),
            )
        }.firstOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Esta oferta ya no está disponible")

        val (offer, extras, finishedAt) = row

        val last = jdbc.query(
            """
            SELECT v.updated_at, u.email
            FROM validaciones_comunitarias_oferta v
            JOIN usuarios u ON u.id = v.cliente_id
            WHERE v.oferta_id = :offerId AND v.sigue_vigente
            ORDER BY v.updated_at DESC
            LIMIT 1
            """.trimIndent(),
            MapSqlParameterSource("offerId", offerId),
        ) { rs, _ ->
            LastConfirmation(
                at = rs.getObject("updated_at", OffsetDateTime::class.java),
                by = rs.getString("email").substringBefore('@'),
            )
        }.firstOrNull()

        return OfferDetailResponse(
            offer = offer,
            businessPhotoUrl = extras.second,
            businessAddress = extras.first,
            endedAt = if (offer.live) null else finishedAt,
            lastConfirmation = last,
        )
    }

    /** Una vista por vecino y oferta cada 30 minutos, para que refrescar no infle el contador. */
    @Transactional
    fun recordOfferView(userId: Long, offerId: Long) {
        jdbc.update(
            """
            INSERT INTO visualizaciones_oferta (oferta_id, usuario_id)
            SELECT CAST(:o AS bigint), CAST(:u AS bigint)
            WHERE NOT EXISTS (
                SELECT 1 FROM visualizaciones_oferta
                WHERE oferta_id = :o AND usuario_id = :u AND created_at > now() - interval '30 minutes'
            )
            """.trimIndent(),
            MapSqlParameterSource().addValue("o", offerId).addValue("u", userId),
        )
    }

    // ---- Helpers --------------------------------------------------------------------

    private val OFFER_FROM = """
        ofertas o
        JOIN perfiles_comercio pc ON pc.usuario_id = o.comercio_id
        LEFT JOIN rubros r ON r.id = pc.rubro_id
        LEFT JOIN ofertas_porcentaje op ON op.oferta_id = o.id
        LEFT JOIN ofertas_2x1 d ON d.oferta_id = o.id
        LEFT JOIN ofertas_cantidad oc ON oc.oferta_id = o.id
        LEFT JOIN ofertas_por_mayor om ON om.oferta_id = o.id
    """.trimIndent()

    private val POINT_EXPR =
        "CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)"

    private val DISTANCE_EXPR = "ST_Distance(pc.ubicacion, $POINT_EXPR)"

    /** Columnas de una oferta de la lista; las comparten Explorar y Favoritos. */
    private fun selectColumns(distance: String) = """
        o.id, o.nombre_producto, o.foto_url,
        o.tipo_oferta::text AS tipo, o.vigencia_tipo::text AS vigencia_tipo, o.vigente_hasta,
        pc.usuario_id AS comercio_id, pc.nombre_negocio, r.nombre AS rubro,
        COALESCE(op.precio_final, d.precio_unitario, oc.precio_unitario, om.precio_unitario_mayorista) AS precio,
        op.precio_original AS precio_original,
        op.porcentaje_descuento AS porcentaje,
        d.unidades_a_llevar AS llevar, d.unidades_a_pagar AS pagar,
        oc.cantidad_requerida AS cantidad_requerida,
        oc.porcentaje_descuento_ultima_unidad AS descuento_ultima,
        om.cantidad_minima AS cantidad_minima,
        $distance AS distancia_m,
        (SELECT COUNT(*) FROM validaciones_comunitarias_oferta v
          WHERE v.oferta_id = o.id AND v.sigue_vigente) AS confirmaciones,
        EXISTS (SELECT 1 FROM favoritos fv WHERE fv.oferta_id = o.id AND fv.cliente_id = :clientId) AS favorita,
        ($BUSINESS_IS_OPEN) AS abierto,
        (SELECT to_char(MAX(h.hora_fin), 'HH24:MI') FROM horarios_atencion h
          WHERE h.comercio_id = pc.usuario_id AND h.activo AND h.dia_semana = :isoDow) AS cierra_hoy,
        ($OFFER_IS_LIVE) AS vigente
    """.trimIndent()

    private fun baseParams(f: ExploreFilters, userId: Long): MapSqlParameterSource {
        val now = LocalDateTime.now(ZONE)
        return MapSqlParameterSource()
            .addValue("clientId", userId)
            .addValue("isoDow", now.dayOfWeek.value)
            .addValue("nowTime", now.toLocalTime().format(TIME_FORMAT))
            .apply {
                if (f.latitude != null && f.longitude != null) {
                    addValue("lat", f.latitude)
                    addValue("lng", f.longitude)
                }
            }
    }

    private fun radiusMeters(f: ExploreFilters): Double =
        minOf(f.radiusKm ?: MAX_RADIUS_KM, MAX_RADIUS_KM) * 1000.0

    private fun offerWhere(f: ExploreFilters, hasLocation: Boolean, params: MapSqlParameterSource): String {
        val clauses = mutableListOf(OFFER_IS_LIVE, BUSINESS_IS_VISIBLE.trimIndent())

        if (hasLocation) {
            params.addValue("radius", radiusMeters(f))
            clauses += "ST_DWithin(pc.ubicacion, $POINT_EXPR, :radius)"
        }
        if (f.categoryIds.isNotEmpty()) {
            params.addValue("categoryIds", f.categoryIds)
            clauses += "pc.rubro_id IN (:categoryIds)"
        }
        if (f.types.isNotEmpty()) {
            params.addValue("types", f.types)
            clauses += "o.tipo_oferta::text IN (:types)"
        }
        if (f.query != null) {
            params.addValue("q", likePattern(f.query))
            clauses += "(o.nombre_producto ILIKE :q ESCAPE '\\' OR pc.nombre_negocio ILIKE :q ESCAPE '\\')"
        }
        return clauses.joinToString(" AND ") { "($it)" }
    }

    private fun likePattern(raw: String): String {
        val escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }

    private fun ResultSet.intOrNull(column: String): Int? {
        val value = getInt(column)
        return if (wasNull()) null else value
    }

    private fun ResultSet.toOffer(): ExploreOfferResponse {
        val distance = getDouble("distancia_m").let { if (wasNull()) null else it.toInt() }
        return ExploreOfferResponse(
            id = getLong("id"),
            productName = getString("nombre_producto"),
            photoUrl = getString("foto_url"),
            type = getString("tipo"),
            validityType = getString("vigencia_tipo"),
            validUntil = getObject("vigente_hasta", OffsetDateTime::class.java),
            closesToday = getString("cierra_hoy"),
            businessId = getLong("comercio_id"),
            businessName = getString("nombre_negocio"),
            category = getString("rubro"),
            businessOpen = getBoolean("abierto"),
            price = getBigDecimal("precio"),
            originalPrice = getBigDecimal("precio_original"),
            percent = intOrNull("porcentaje"),
            take = intOrNull("llevar"),
            pay = intOrNull("pagar"),
            requiredQuantity = intOrNull("cantidad_requerida"),
            lastUnitDiscount = intOrNull("descuento_ultima"),
            minQuantity = intOrNull("cantidad_minima"),
            distanceMeters = distance,
            confirmations = getInt("confirmaciones"),
            favorite = getBoolean("favorita"),
            live = getBoolean("vigente"),
        )
    }
}