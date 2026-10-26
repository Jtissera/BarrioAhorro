package com.barrioahorro.api.modules.forum

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ForumPostRepository : JpaRepository<ForumPostEntity, Long> {
    fun findByEliminadoEnIsNullOrderByCreatedAtDesc(): List<ForumPostEntity>

    @Query(value = "SELECT COUNT(1) FROM foro_comentarios WHERE publicacion_id = :postId AND eliminado_en IS NULL", nativeQuery = true)
    fun countCommentsByPostId(@Param("postId") postId: Long): Int
}
