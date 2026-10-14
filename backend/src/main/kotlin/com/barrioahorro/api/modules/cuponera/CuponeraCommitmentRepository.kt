package com.barrioahorro.api.modules.cuponera

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.OffsetDateTime

interface BusinessCommitmentInfo {
    val onboardingCompletado: Boolean
    val rubroNombre: String?
}

interface CuponeraCommitmentRepository : JpaRepository<CuponeraCommitmentEntity, Long> {

    fun findFirstByComercioIdOrderByPeriodoInicioDesc(comercioId: Long): CuponeraCommitmentEntity?

    @Query(
        nativeQuery = true,
        value = """
            SELECT pc.onboarding_completado AS "onboardingCompletado",
                   r.nombre                 AS "rubroNombre"
            FROM perfiles_comercio pc
            LEFT JOIN rubros r ON r.id = pc.rubro_id
            WHERE pc.usuario_id = :comercioId
        """,
    )
    fun findBusinessInfo(@Param("comercioId") comercioId: Long): BusinessCommitmentInfo?

    /**
     * Crea el compromiso del período. ON CONFLICT DO NOTHING cubre la carrera
     * de dos requests simultáneos (unique y exclusion constraint del período).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            INSERT INTO compromisos_cuponera
                (comercio_id, periodo_inicio, periodo_fin, fecha_limite_ratificacion)
            VALUES (:comercioId, :inicio, :fin, :limite)
            ON CONFLICT DO NOTHING
        """,
    )
    fun createPending(
        @Param("comercioId") comercioId: Long,
        @Param("inicio") inicio: LocalDate,
        @Param("fin") fin: LocalDate,
        @Param("limite") limite: OffsetDateTime,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = "UPDATE compromisos_cuponera SET estado = 'RATIFICADO' WHERE id = :id AND estado = 'PENDIENTE'",
    )
    fun markRatified(@Param("id") id: Long): Int
}