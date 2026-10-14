package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CuponeraCommitmentResponse
import com.barrioahorro.api.modules.cuponera.dto.CuponeraProductResponse
import com.barrioahorro.api.modules.cuponera.dto.RatifyCuponeraRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
class CuponeraService(
    private val commitments: CuponeraCommitmentRepository,
    private val rewards: CuponeraRewardRepository,
) {

    companion object {
        const val MIN_PRODUCTS = 2
        const val MAX_PRODUCTS = 4
        const val PESOS_PER_POINT = 500
        const val PERIOD_DAYS = 15L
        const val RATIFICATION_GRACE_DAYS = 3L

        private val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

        /** 1 punto cada 500 pesos, redondeando hacia arriba y con mínimo 1 (costo_puntos > 0). */
        fun pointsFor(valor: BigDecimal): Int =
            valor.divide(BigDecimal(PESOS_PER_POINT), 0, RoundingMode.CEILING)
                .toInt()
                .coerceAtLeast(1)

        private val DEFAULT_SUGGESTIONS = listOf("Combo de productos", "Producto de estación")

        private val SUGGESTIONS_BY_CATEGORY = mapOf(
            "Almacén" to listOf("Combo de almacén", "Pack de fideos y salsa"),
            "Carnicería" to listOf("Bandeja de milanesas", "Pack de carne molida"),
            "Verdulería" to listOf("Bolsón de verduras", "Cajón de frutas"),
            "Panadería" to listOf("Docena de facturas", "Pan casero"),
            "Farmacia" to listOf("Kit de cuidado personal", "Pack de vitaminas"),
            "Limpieza" to listOf("Pack de limpieza", "Combo de higiene"),
            "Kiosco" to listOf("Combo de golosinas", "Pack de bebidas"),
        )
    }

    /**
     * Devuelve el compromiso vigente del comercio. Si todavía no existe uno
     * para el período actual, lo crea en estado PENDIENTE.
     */
    @Transactional
    fun getCurrent(userId: Long): CuponeraCommitmentResponse {
        val info = requireBusiness(userId)
        return toResponse(currentOrCreate(userId), info.rubroNombre)
    }

    /**
     * Reemplaza los productos del compromiso pendiente y lo ratifica, todo en
     * una sola transacción.
     */
    @Transactional
    fun ratify(userId: Long, request: RatifyCuponeraRequest): CuponeraCommitmentResponse {
        val info = requireBusiness(userId)

        if (request.productos.size !in MIN_PRODUCTS..MAX_PRODUCTS) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tenés que aportar entre $MIN_PRODUCTS y $MAX_PRODUCTS productos",
            )
        }
        if (!info.onboardingCompletado) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Completá el onboarding antes de ratificar tu aporte")
        }

        val commitment = currentOrCreate(userId)
        when {
            commitment.estado == "RATIFICADO" ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "Ya ratificaste tu aporte de este período")
            commitment.estado == "VENCIDO" || !OffsetDateTime.now().isBefore(commitment.fechaLimiteRatificacion) ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "El plazo para ratificar este período ya venció")
        }

        rewards.purgeByCompromisoId(commitment.id)
        rewards.saveAll(
            request.productos.map { p ->
                CuponeraRewardEntity(
                    compromisoId = commitment.id,
                    nombreProducto = p.nombreProducto.trim(),
                    descripcion = p.descripcion?.trim(),
                    fotoUrl = p.fotoUrl?.trim()?.takeIf { it.isNotEmpty() },
                    valorComercio = p.valorComercio,
                    costoPuntos = pointsFor(p.valorComercio),
                    stockComprometido = p.unidadesDisponibles.toShort(),
                )
            },
        )
        rewards.flush()

        if (commitments.markRatified(commitment.id) != 1) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No se pudo ratificar el aporte, intentá de nuevo")
        }

        val updated = commitments.findById(commitment.id).orElseThrow {
            ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se encontró el compromiso")
        }
        return toResponse(updated, info.rubroNombre)
    }

    private fun requireBusiness(userId: Long): BusinessCommitmentInfo =
        commitments.findBusinessInfo(userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden aportar a la cuponera")

    private fun currentOrCreate(userId: Long): CuponeraCommitmentEntity {
        val today = LocalDate.now(ZONE)
        val latest = commitments.findFirstByComercioIdOrderByPeriodoInicioDesc(userId)
        if (latest != null && !today.isAfter(latest.periodoFin)) return latest

        // Nuevo período: arranca hoy (siempre posterior al anterior, que ya terminó).
        val inicio = today
        val fin = inicio.plusDays(PERIOD_DAYS - 1)
        val limite = inicio.plusDays(RATIFICATION_GRACE_DAYS).atStartOfDay(ZONE).toOffsetDateTime()
        commitments.createPending(userId, inicio, fin, limite)

        return commitments.findFirstByComercioIdOrderByPeriodoInicioDesc(userId)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el compromiso")
    }

    private fun toResponse(c: CuponeraCommitmentEntity, rubro: String?): CuponeraCommitmentResponse {
        val products = rewards.findByCompromisoIdOrderByIdAsc(c.id).map {
            CuponeraProductResponse(
                id = it.id,
                nombreProducto = it.nombreProducto,
                descripcion = it.descripcion,
                fotoUrl = it.fotoUrl,
                valorComercio = it.valorComercio,
                costoPuntos = it.costoPuntos,
                unidadesDisponibles = it.stockComprometido.toInt(),
                unidadesCanjeadas = it.stockCanjeado.toInt(),
            )
        }
        return CuponeraCommitmentResponse(
            id = c.id,
            periodoInicio = c.periodoInicio,
            periodoFin = c.periodoFin,
            fechaLimiteRatificacion = c.fechaLimiteRatificacion,
            estado = c.estado,
            fechaRatificacion = c.fechaRatificacion,
            puedeRatificar = c.estado == "PENDIENTE" && OffsetDateTime.now().isBefore(c.fechaLimiteRatificacion),
            minProductos = MIN_PRODUCTS,
            maxProductos = MAX_PRODUCTS,
            pesosPorPunto = PESOS_PER_POINT,
            productos = products,
            sugerencias = SUGGESTIONS_BY_CATEGORY[rubro] ?: DEFAULT_SUGGESTIONS,
        )
    }
}