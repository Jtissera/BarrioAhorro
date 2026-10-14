package com.barrioahorro.api.modules.cuponera

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CuponeraRewardRepository : JpaRepository<CuponeraRewardEntity, Long> {

    fun findByCompromisoIdOrderByIdAsc(compromisoId: Long): List<CuponeraRewardEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CuponeraRewardEntity r where r.compromisoId = :compromisoId")
    fun purgeByCompromisoId(@Param("compromisoId") compromisoId: Long): Int
}