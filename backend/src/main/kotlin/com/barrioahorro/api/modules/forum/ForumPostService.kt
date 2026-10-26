package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.forum.dto.CreateForumPostRequest
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

        // Si se proveen coordenadas directas, se usan.
        // Si se escribió una dirección en texto, se geocodifica con LocationService a coordenadas GPS (Point 4326)
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
                comentariosCount = comentariosCount,
                createdAt = post.createdAt,
            )
        }
    }

    private fun formatAuthorName(email: String): String {
        val username = email.substringBefore("@")
        return username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
