package com.barrioahorro.api.modules.sorteo

import com.barrioahorro.api.modules.sorteo.dto.RatifySorteoRequest
import com.barrioahorro.api.modules.sorteo.dto.SorteoCommitmentResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
class SorteoService(
    private val commitments: SorteoCommitmentRepository,
) {

    companion object {
        /** Duración del ciclo (la historia dice 4 a 6 meses, a definir). */
        const val CYCLE_MONTHS = 4L
        const val RATIFICATION_GRACE_DAYS = 7L

        private val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }

    /** Devuelve el compromiso del ciclo vigente; si no existe, lo crea en estado PENDIENTE. */
    @Transactional
    fun getCurrent(userId: Long): SorteoCommitmentResponse {
        requireBusiness(userId)
        return toResponse(currentOrCreate(userId))
    }

    @Transactional
    fun ratify(userId: Long, request: RatifySorteoRequest): SorteoCommitmentResponse {
        val onboardingCompleted = requireBusiness(userId)
        if (!onboardingCompleted) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Completá el onboarding antes de ratificar tu premio")
        }

        val commitment = currentOrCreate(userId)
        when {
            commitment.estado == "RATIFICADO" ->
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya ratificaste tu premio de este ciclo y no se puede cambiar hasta el sorteo",
                )
            commitment.estado == "VENCIDO" || !OffsetDateTime.now().isBefore(commitment.fechaLimiteRatificacion) ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "El plazo para ratificar este ciclo ya venció")
        }

        if (commitments.ratify(commitment.id, request.premioDescripcion.trim()) != 1) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No se pudo ratificar el premio, intentá de nuevo")
        }

        val updated = commitments.findById(commitment.id).orElseThrow {
            ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se encontró el compromiso")
        }
        return toResponse(updated)
    }

    /** Devuelve si completó el onboarding. */
    private fun requireBusiness(userId: Long): Boolean =
        commitments.findOnboardingCompleted(userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden aportar premios al sorteo")

    private fun currentOrCreate(userId: Long): SorteoCommitmentEntity {
        val today = LocalDate.now(ZONE)
        val latest = commitments.findFirstByComercioIdOrderByCicloInicioDesc(userId)
        if (latest != null && !today.isAfter(latest.cicloFin)) return latest

        // Nuevo ciclo: arranca hoy (el anterior ya terminó, así que no se solapa).
        val inicio = today
        val fin = inicio.plusMonths(CYCLE_MONTHS).minusDays(1)
        val limite = inicio.plusDays(RATIFICATION_GRACE_DAYS).atStartOfDay(ZONE).toOffsetDateTime()
        commitments.createPending(userId, inicio, fin, limite)

        return commitments.findFirstByComercioIdOrderByCicloInicioDesc(userId)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el compromiso")
    }

    private fun toResponse(c: SorteoCommitmentEntity) = SorteoCommitmentResponse(
        id = c.id,
        cicloInicio = c.cicloInicio,
        cicloFin = c.cicloFin,
        fechaLimiteRatificacion = c.fechaLimiteRatificacion,
        estado = c.estado,
        fechaRatificacion = c.fechaRatificacion,
        premioDescripcion = c.premioDescripcion.takeIf { it.isNotBlank() },
        puedeRatificar = c.estado == "PENDIENTE" && OffsetDateTime.now().isBefore(c.fechaLimiteRatificacion),
    )
}