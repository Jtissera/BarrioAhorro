package com.barrioahorro.api.modules.offer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface Offer2x1Repository : JpaRepository<Offer2x1Entity, Long>
