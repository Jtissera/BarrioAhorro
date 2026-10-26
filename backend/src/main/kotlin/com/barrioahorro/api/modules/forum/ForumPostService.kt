package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.forum.dto.CreateCommentRequest
import com.barrioahorro.api.modules.forum.dto.CreateForumPostRequest
import com.barrioahorro.api.modules.forum.dto.ForumCommentResponse
import com.barrioahorro.api.modules.forum.dto.ForumPostResponse
import com.barrioahorro.api.modules.location.LocationService
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import com.barrioahorro.api.modules.user.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ForumPostService(
    private val forumPostRepository: ForumPostRepository,
    private val forumCommentRepository: ForumCommentRepository,
    private val businessRepository: BusinessRepository,
    private val userRepository: UserRepository,
    private val locationService: LocationService,
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @Transactional
    fun createPost(autorId: Long, request: CreateForumPostRequest): ForumPostResponse {
        val comercioId = request.comercioId
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El comercio asociado es obligatorio")

        val texto = request.texto?.trim()
        if (texto.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto de la publicación es obligatorio")
        }

        val business = businessRepository.findById(comercioId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "El comercio seleccionado no existe")
        }

        val user = userRepository.findById(autorId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")
        }

        val point = if (request.latitud != null && request.longitud != null) {
            geometryFactory.createPoint(Coordinate(request.longitud, request.latitud))
        } else if (!request.direccion.isNullOrBlank()) {
            val candidato = runCatching {
                locationService.buscarCandidatos(ValidateAddressRequest(direccion = request.direccion))
            }.getOrNull()?.firstOrNull()

            if (candidato != null) {
                geometryFactory.createPoint(Coordinate(candidato.longitude, candidato.latitude))
            } else null
        } else null

        val post = ForumPostEntity(
            autorId = autorId,
            comercioId = business.usuarioId,
            texto = texto,
            precioObservado = request.precioObservado,
            ubicacionObservada = point,
        )

        val saved = forumPostRepository.save(post)

        return ForumPostResponse(
            id = saved.id,
            autorId = saved.autorId,
            autorNombre = formatAuthorName(user.email),
            comercioId = business.usuarioId,
            comercioNombre = business.nombreNegocio ?: "Comercio del barrio",
            texto = saved.texto,
            precioObservado = saved.precioObservado,
            latitud = saved.ubicacionObservada?.y,
            longitud = saved.ubicacionObservada?.x,
            upvotesCount = 0,
            comentariosCount = 0,
            createdAt = saved.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun getFeed(): List<ForumPostResponse> {
        val posts = forumPostRepository.findByEliminadoEnIsNullOrderByCreatedAtDesc()
        if (posts.isEmpty()) return emptyList()

        val businessIds = posts.map { it.comercioId }.distinct()
        val userIds = posts.map { it.autorId }.distinct()

        val businesses = businessRepository.findAllById(businessIds).associateBy { it.usuarioId }
        val users = userRepository.findAllById(userIds).associateBy { it.id }

        return posts.map { post ->
            val business = businesses[post.comercioId]
            val user = users[post.autorId]
            val comentariosCount = forumPostRepository.countCommentsByPostId(post.id)
            val upvotesCount = forumPostRepository.countUpvotesByPostId(post.id)

            ForumPostResponse(
                id = post.id,
                autorId = post.autorId,
                autorNombre = formatAuthorName(user?.email ?: "Vecino"),
                comercioId = post.comercioId,
                comercioNombre = business?.nombreNegocio ?: "Comercio del barrio",
                texto = post.texto,
                precioObservado = post.precioObservado,
                latitud = post.ubicacionObservada?.y,
                longitud = post.ubicacionObservada?.x,
                upvotesCount = upvotesCount,
                comentariosCount = comentariosCount,
                createdAt = post.createdAt,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPostById(postId: Long): ForumPostResponse {
        val post = forumPostRepository.findById(postId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación no existe")
        }

        if (post.eliminadoEn != null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación fue eliminada")
        }

        val business = businessRepository.findById(post.comercioId).orElse(null)
        val user = userRepository.findById(post.autorId).orElse(null)
        val comentariosCount = forumPostRepository.countCommentsByPostId(post.id)
        val upvotesCount = forumPostRepository.countUpvotesByPostId(post.id)

        return ForumPostResponse(
            id = post.id,
            autorId = post.autorId,
            autorNombre = formatAuthorName(user?.email ?: "Vecino"),
            comercioId = post.comercioId,
            comercioNombre = business?.nombreNegocio ?: "Comercio del barrio",
            texto = post.texto,
            precioObservado = post.precioObservado,
            latitud = post.ubicacionObservada?.y,
            longitud = post.ubicacionObservada?.x,
            upvotesCount = upvotesCount,
            comentariosCount = comentariosCount,
            createdAt = post.createdAt,
        )
    }

    @Transactional
    fun addComment(userId: Long, postId: Long, request: CreateCommentRequest): ForumCommentResponse {
        val post = forumPostRepository.findById(postId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación no existe")
        }

        if (post.eliminadoEn != null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación fue eliminada")
        }

        val texto = request.texto?.trim()
        if (texto.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto del comentario es obligatorio")
        }

        if (request.comentarioPadreId != null) {
            val padre = forumCommentRepository.findById(request.comentarioPadreId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "El comentario al que responde no existe")
            }
            if (padre.publicacionId != postId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario padre pertenece a otra publicación")
            }
        }

        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")
        }

        val comment = ForumCommentEntity(
            publicacionId = postId,
            comentarioPadreId = request.comentarioPadreId,
            autorId = userId,
            texto = texto,
        )

        val saved = forumCommentRepository.save(comment)

        return ForumCommentResponse(
            id = saved.id,
            publicacionId = saved.publicacionId,
            comentarioPadreId = saved.comentarioPadreId,
            autorId = saved.autorId,
            autorNombre = formatAuthorName(user.email),
            texto = saved.texto,
            upvotesCount = 0,
            esAutorDePublicacion = (post.autorId == userId),
            esUsuarioActual = true,
            createdAt = saved.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun getComments(postId: Long, currentUserId: Long): List<ForumCommentResponse> {
        val post = forumPostRepository.findById(postId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación no existe")
        }

        val comments = forumCommentRepository.findByPublicacionIdAndEliminadoEnIsNullOrderByCreatedAtAsc(postId)
        if (comments.isEmpty()) return emptyList()

        val repliesCountMap = comments.filter { it.comentarioPadreId != null }
            .groupingBy { it.comentarioPadreId!! }
            .eachCount()

        val userIds = comments.map { it.autorId }.distinct()
        val users = userRepository.findAllById(userIds).associateBy { it.id }

        return comments.map { comment ->
            val user = users[comment.autorId]
            val upvotesCount = forumCommentRepository.countUpvotesByCommentId(comment.id)
            val respuestasCount = repliesCountMap[comment.id] ?: 0

            ForumCommentResponse(
                id = comment.id,
                publicacionId = comment.publicacionId,
                comentarioPadreId = comment.comentarioPadreId,
                autorId = comment.autorId,
                autorNombre = formatAuthorName(user?.email ?: "Vecino"),
                texto = comment.texto,
                upvotesCount = upvotesCount,
                respuestasCount = respuestasCount,
                esAutorDePublicacion = (post.autorId == comment.autorId),
                esUsuarioActual = (currentUserId == comment.autorId),
                createdAt = comment.createdAt,
            )
        }
    }

    private fun formatAuthorName(email: String): String {
        val username = email.substringBefore("@")
        return username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
