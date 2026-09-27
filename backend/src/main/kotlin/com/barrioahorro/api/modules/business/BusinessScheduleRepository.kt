package com.barrioahorro.api.modules.business

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BusinessScheduleRepository : JpaRepository<BusinessScheduleEntity, Long> {
    fun findByComercioIdOrderByDiaSemanaAscHoraInicioAsc(comercioId: Long): List<BusinessScheduleEntity>
    fun deleteByComercioId(comercioId: Long)
}