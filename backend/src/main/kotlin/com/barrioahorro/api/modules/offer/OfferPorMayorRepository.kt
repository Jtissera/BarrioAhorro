package com.barrioahorro.api.modules.offer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OfferPorMayorRepository : JpaRepository<OfferPorMayorEntity, Long>
