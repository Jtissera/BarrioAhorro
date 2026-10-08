package com.barrioahorro.api.modules.review

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReviewRepository: JpaRepository<ReviewEntity, Long>{

    @Query(
        """
        SELECT 
            r.id AS id,
            r.clienteId AS clienteId,
            u.email AS emailCliente,
            r.texto AS texto,
            r.estrellas AS estrellas,
            r.createdAt AS createdAt
        FROM ReviewEntity r, UserEntity u
        WHERE r.clienteId = u.id
          AND r.comercioId = :comercioId
        ORDER BY r.createdAt DESC
        """
    )
    fun findReviewsWithAuthorByComercioId(@Param("comercioId") comercioId: Long): List<ReviewWithAuthorProjection>
    
}