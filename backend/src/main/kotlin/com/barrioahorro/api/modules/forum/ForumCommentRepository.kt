package com.barrioahorro.api.modules.forum

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ForumCommentRepository : JpaRepository<ForumCommentEntity, Long> {
    fun findByPublicacionIdAndEliminadoEnIsNullOrderByCreatedAtAsc(publicacionId: Long): List<ForumCommentEntity>

    @Query(value = "SELECT COUNT(1) FROM foro_comentario_upvotes WHERE comentario_id = :commentId", nativeQuery = true)
    fun countUpvotesByCommentId(@Param("commentId") commentId: Long): Int
}
