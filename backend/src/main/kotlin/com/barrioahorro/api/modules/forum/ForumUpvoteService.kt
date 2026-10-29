package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.forum.dto.UpvoteResponse
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ForumUpvoteService(
    private val forumPostRepository: ForumPostRepository,
    private val forumCommentRepository: ForumCommentRepository,
    private val jdbc: NamedParameterJdbcTemplate,
) {

    // Publicaciones

    @Transactional
    fun upvotePost(userId: Long, postId: Long): UpvoteResponse {
        val post = findActivePost(postId)
        requireClient(userId)
        if (post.autorId == userId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No podés votar tu propia publicación")
        }
        // si ya lo votó no hace nada (y el trigger no vuelve a sumar puntos).
        jdbc.update(
            """
            INSERT INTO foro_publicacion_upvotes (publicacion_id, usuario_id)
            VALUES (:item, :user)
            ON CONFLICT (publicacion_id, usuario_id) DO NOTHING
            """,
            params(postId, userId),
        )
        return state("foro_publicacion_upvotes", "publicacion_id", postId, userId)
    }

    @Transactional
    fun removePostUpvote(userId: Long, postId: Long): UpvoteResponse {
        findActivePost(postId)
        jdbc.update(
            "DELETE FROM foro_publicacion_upvotes WHERE publicacion_id = :item AND usuario_id = :user",
            params(postId, userId),
        )
        return state("foro_publicacion_upvotes", "publicacion_id", postId, userId)
    }

    // Comentarios

    @Transactional
    fun upvoteComment(userId: Long, commentId: Long): UpvoteResponse {
        val comment = findActiveComment(commentId)
        requireClient(userId)
        if (comment.autorId == userId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No podés votar tu propio comentario")
        }
        jdbc.update(
            """
            INSERT INTO foro_comentario_upvotes (comentario_id, usuario_id)
            VALUES (:item, :user)
            ON CONFLICT (comentario_id, usuario_id) DO NOTHING
            """,
            params(commentId, userId),
        )
        return state("foro_comentario_upvotes", "comentario_id", commentId, userId)
    }

    @Transactional
    fun removeCommentUpvote(userId: Long, commentId: Long): UpvoteResponse {
        findActiveComment(commentId)
        jdbc.update(
            "DELETE FROM foro_comentario_upvotes WHERE comentario_id = :item AND usuario_id = :user",
            params(commentId, userId),
        )
        return state("foro_comentario_upvotes", "comentario_id", commentId, userId)
    }

    // privates

    private fun findActivePost(postId: Long): ForumPostEntity {
        val post = forumPostRepository.findById(postId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación no existe")
        }
        if (post.eliminadoEn != null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación fue eliminada")
        }
        return post
    }

    private fun findActiveComment(commentId: Long): ForumCommentEntity {
        val comment = forumCommentRepository.findById(commentId)
            .filter { it.eliminadoEn == null }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "El comentario no existe") }
        findActivePost(comment.publicacionId)
        return comment
    }

    /** Las tablas de upvotes referencian perfiles_cliente: un comercio no puede votar. */
    private fun requireClient(userId: Long) {
        val esCliente = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM perfiles_cliente WHERE usuario_id = :user)",
            MapSqlParameterSource("user", userId),
            Boolean::class.java,
        ) ?: false
        if (!esCliente) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los vecinos pueden votar en el foro")
        }
    }

    private fun state(table: String, column: String, itemId: Long, userId: Long): UpvoteResponse =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) AS total,
                   COALESCE(BOOL_OR(usuario_id = :user), false) AS mine
            FROM $table WHERE $column = :item
            """,
            params(itemId, userId),
        ) { rs, _ -> UpvoteResponse(rs.getInt("total"), rs.getBoolean("mine")) }!!

    private fun params(itemId: Long, userId: Long) =
        MapSqlParameterSource().addValue("item", itemId).addValue("user", userId)
}