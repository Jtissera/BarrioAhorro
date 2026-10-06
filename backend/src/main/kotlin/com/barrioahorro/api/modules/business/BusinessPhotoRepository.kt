package com.barrioahorro.api.modules.business

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BusinessPhotoRepository : JpaRepository<BusinessPhotoEntity, Long> {
    fun findByComercioIdOrderByOrdenAscIdAsc(comercioId: Long): List<BusinessPhotoEntity>
    fun findByIdAndComercioId(id: Long, comercioId: Long): BusinessPhotoEntity?
    fun countByComercioId(comercioId: Long): Long
}
