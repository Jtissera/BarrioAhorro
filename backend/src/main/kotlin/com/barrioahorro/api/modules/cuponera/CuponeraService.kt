package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.business.PhotoStorage
import com.barrioahorro.api.modules.cuponera.dto.CuponeraCommitmentResponse
import com.barrioahorro.api.modules.cuponera.dto.CuponeraCycleResponse
import com.barrioahorro.api.modules.cuponera.dto.CuponeraPhotoUploadResponse
import com.barrioahorro.api.modules.cuponera.dto.CuponeraProductResponse
import com.barrioahorro.api.modules.cuponera.dto.RatifyCuponeraRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime

@Service
class CuponeraService(
    private val commitments: CuponeraCommitmentRepository,
    private val rewards: CuponeraRewardRepository,
    private val photoStorage: PhotoStorage,
    private val cycles: CommitmentCycleService,
) {

    companion object {
        const val MIN_PRODUCTS = 2
        const val MAX_PRODUCTS = 4
        const val PESOS_PER_POINT = 500

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
     * Compromiso sobre el que hay que actuar: el pendiente de ratificar
     * (anticipado o tardío) y, si no hay, el vigente.
     */
    @Transactional
    fun getCurrent(userId: Long): CuponeraCommitmentResponse {
        val info = requireBusiness(userId)
        val state = cycles.ensure(userId, CycleKind.CUPONERA)
        val target = state.next?.takeIf { it.status == "PENDIENTE" }
            ?: state.current
            ?: state.next
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el compromiso")
        return toResponse(target.id, info.rubroNombre)
    }

    /** Vigente + próximo, con las banderas que necesita la pestaña "Mi aporte". */
    @Transactional
    fun cycle(userId: Long): CuponeraCycleResponse {
        val info = requireBusiness(userId)
        val state = cycles.ensure(userId, CycleKind.CUPONERA)
        return CuponeraCycleResponse(
            current = state.current?.let { toResponse(it.id, info.rubroNombre) },
            next = state.next?.let { toResponse(it.id, info.rubroNombre) },
            canRatify = state.canRatify,
            ratificationType = state.ratificationType,
        )
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

        val state = cycles.ensure(userId, CycleKind.CUPONERA)
        val target = state.next?.takeIf { it.status == "PENDIENTE" }
            ?: throw ResponseStatusException(
                HttpStatus.CONFLICT,
                if (state.next?.status == "RATIFICADO") {
                    "Ya ratificaste tu aporte del próximo período"
                } else {
                    "Todavía no es momento de ratificar el próximo período"
                },
            )
        if (!OffsetDateTime.now().isBefore(target.deadline)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El plazo para ratificar este período ya venció")
        }

        rewards.purgeByCompromisoId(target.id)
        rewards.saveAll(
            request.productos.map { p ->
                CuponeraRewardEntity(
                    compromisoId = target.id,
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

        if (commitments.markRatified(target.id) != 1) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No se pudo ratificar el aporte, intentá de nuevo")
        }

        return toResponse(target.id, info.rubroNombre)
    }

    @Transactional(readOnly = true)
    fun uploadPhoto(userId: Long, file: MultipartFile): CuponeraPhotoUploadResponse {
        requireBusiness(userId)

        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió ninguna imagen")
        }

        val url = photoStorage.store(file.bytes)
            ?: throw ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "La foto debe ser JPEG, PNG o WebP",
            )

        return CuponeraPhotoUploadResponse(url)
    }

    private fun requireBusiness(userId: Long): BusinessCommitmentInfo =
        commitments.findBusinessInfo(userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden aportar a la cuponera")

    private fun toResponse(commitmentId: Long, rubro: String?): CuponeraCommitmentResponse {
        val c = commitments.findById(commitmentId).orElseThrow {
            ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se encontró el compromiso")
        }
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