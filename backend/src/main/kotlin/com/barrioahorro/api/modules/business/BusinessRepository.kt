package com.barrioahorro.api.modules.business

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BusinessRepository : JpaRepository<BusinessEntity, Long>