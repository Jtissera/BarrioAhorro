package com.barrioahorro.api.modules.sorteo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.OffsetDateTime

interface SorteoCommitmentRepository : JpaRepository<SorteoCommitmentEntity, Long> {

    fun findFirstByComercioIdOrderByCicloInicioDesc(comercioId: Long): SorteoCommitmentEntity?

    /** null si el usuario no es comercio. */
    @Query(
        nativeQuery = true,
        value = "SELECT onboarding_completado FROM perfiles_comercio WHERE usuario_id = :comercioId",
    )
    fun findOnboardingCompleted(@Param("comercioId") comercioId: Long): Boolean?

    /** Crea el compromiso del ciclo con el premio vacío. ON CONFLICT cubre dos requests simultáneos. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            INSERT INTO compromisos_sorteo
                (comercio_id, ciclo_inicio, ciclo_fin, fecha_limite_ratificacion, premio_descripcion)
            VALUES (:comercioId, :inicio, :fin, :limite, '')
            ON CONFLICT DO NOTHING
        """,
    )
    fun createPending(
        @Param("comercioId") comercioId: Long,
        @Param("inicio") inicio: LocalDate,
        @Param("fin") fin: LocalDate,
        @Param("limite") limite: OffsetDateTime,
    ): Int

    /** Guarda el premio y ratifica en un solo UPDATE; los triggers validan plazo, onboarding y premio no vacío. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            UPDATE compromisos_sorteo
            SET premio_descripcion = :premio, estado = 'RATIFICADO'
            WHERE id = :id AND estado = 'PENDIENTE'
        """,
    )
    fun ratify(@Param("id") id: Long, @Param("premio") premio: String): Int
}