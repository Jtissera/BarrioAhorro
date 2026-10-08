package com.barrioahorro.api.modules.offer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OfferRepository : JpaRepository<OfferEntity, Long> {
    fun findByComercioIdOrderByCreatedAtDesc(comercioId: Long): List<OfferEntity>
    fun findByComercioIdAndActivaTrueOrderByCreatedAtDesc(comercioId: Long): List<OfferEntity>
}
