package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.business.BusinessEntity
import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.forum.dto.CreateCommentRequest
import com.barrioahorro.api.modules.forum.dto.CreateForumPostRequest
import com.barrioahorro.api.modules.forum.dto.ForumBusinessResponse
import com.barrioahorro.api.modules.forum.dto.ForumCommentResponse
import com.barrioahorro.api.modules.forum.dto.ForumPostResponse
import com.barrioahorro.api.modules.forum.dto.MyForumActivityResponse
import com.barrioahorro.api.modules.forum.dto.MyForumCommentResponse
import com.barrioahorro.api.modules.forum.dto.UpdateCommentRequest
import com.barrioahorro.api.modules.forum.dto.UpdateForumPostRequest
import com.barrioahorro.api.modules.location.LocationService
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import com.barrioahorro.api.modules.user.UserEntity
import com.barrioahorro.api.modules.user.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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
    private val jdbc: NamedParameterJdbcTemplate,
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

        val direccion = request.direccion?.trim()?.takeIf { it.isNotEmpty() }
        val point = if (request.latitud != null && request.longitud != null) {
            geometryFactory.createPoint(Coordinate(request.longitud, request.latitud))
        } else {
            direccion?.let { geocode(it) }
        }

        val post = ForumPostEntity(
            autorId = autorId,
            comercioId = business.usuarioId,
            texto = texto,
            precioObservado = request.precioObservado,
            ubicacionObservada = point,
            direccionObservada = direccion,
        )

        val saved = forumPostRepository.save(post)

        return toPostResponse(saved, business, user, autorId)
    }

    @Transactional
    fun updatePost(userId: Long, postId: Long, request: UpdateForumPostRequest): ForumPostResponse {
        val post = findActivePost(postId)
        if (post.autorId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede editar la publicación")
        }

        val texto = request.texto?.trim()
        if (texto.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto de la publicación es obligatorio")
        }

        // El cliente manda el formulario completo: precio o dirección vacíos los borran.
        val direccion = request.direccion?.trim()?.takeIf { it.isNotEmpty() }
        val point = when {
            request.latitud != null && request.longitud != null ->
                geometryFactory.createPoint(Coordinate(request.longitud, request.latitud))
            direccion == null -> null
            // Misma dirección: se conserva el punto ya geocodificado.
            direccion == post.direccionObservada -> post.ubicacionObservada
            // Dirección nueva sin resultados: se rechaza para no borrar el punto en silencio.
            else -> geocode(direccion) ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No encontramos esa dirección. Probá con calle y altura (ej: Av. Corrientes 1500).",
            )
        }

        post.texto = texto
        post.precioObservado = request.precioObservado
        post.direccionObservada = direccion
        post.ubicacionObservada = point
        // updated_at lo actualiza el trigger trg_foro_publicaciones_updated_at.
        val saved = forumPostRepository.saveAndFlush(post)

        val business = businessRepository.findById(saved.comercioId).orElse(null)
        val user = userRepository.findById(saved.autorId).orElse(null)
        return toPostResponse(saved, business, user, userId)
    }

    // ---------- Feed con filtros ----------

    @Transactional(readOnly = true)
    fun getFeed(currentUserId: Long, filters: ForumFeedFilters = ForumFeedFilters()): List<ForumPostResponse> {
        val ids = findFeedIds(filters)
        if (ids.isEmpty()) return emptyList()

        // findAllById no garantiza orden: se reordena según el SQL (más nuevas primero).
        val postsById = forumPostRepository.findAllById(ids).associateBy { it.id }
        val posts = ids.mapNotNull { postsById[it] }

        val businessIds = posts.map { it.comercioId }.distinct()
        val userIds = posts.map { it.autorId }.distinct()

        val businesses = businessRepository.findAllById(businessIds).associateBy { it.usuarioId }
        val users = userRepository.findAllById(userIds).associateBy { it.id }

        return posts.map { post ->
            toPostResponse(post, businesses[post.comercioId], users[post.autorId], currentUserId)
        }
    }

    /** Comercios que tienen al menos una publicación activa (para el selector de filtro). */
    @Transactional(readOnly = true)
    fun getFeedBusinesses(): List<ForumBusinessResponse> {
        val ids = forumPostRepository.findActiveBusinessIds()
        if (ids.isEmpty()) return emptyList()
        return businessRepository.findAllById(ids)
            .map {
                ForumBusinessResponse(
                    id = it.usuarioId,
                    nombre = it.nombreNegocio ?: "Comercio del barrio",
                    rubro = it.rubro?.nombre,
                )
            }
            .sortedBy { it.nombre.lowercase() }
    }

    private fun findFeedIds(f: ForumFeedFilters): List<Long> {
        val where = mutableListOf("p.eliminado_en IS NULL")
        val params = MapSqlParameterSource()

        f.comercioId?.let {
            where += "p.comercio_id = :comercioId"
            params.addValue("comercioId", it)
        }

        f.dias?.let {
            if (it <= 0) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de días debe ser mayor a cero")
            }
            where += "p.created_at >= now() - (:dias * interval '1 day')"
            params.addValue("dias", it)
        }

        if ((f.lat == null) != (f.lng == null)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Para filtrar por cercanía se necesitan latitud y longitud",
            )
        }
        if (f.lat != null && f.lng != null) {
            where += "p.ubicacion_observada IS NOT NULL"
            where += "ST_DWithin(p.ubicacion_observada, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radioMetros)"
            params.addValue("lat", f.lat)
            params.addValue("lng", f.lng)
            params.addValue("radioMetros", f.radioKm.coerceIn(0.1, 50.0) * 1000)
        }

        val sql = "SELECT p.id FROM foro_publicaciones p WHERE ${where.joinToString(" AND ")} ORDER BY p.created_at DESC"
        return jdbc.queryForList(sql, params, Long::class.javaObjectType) as List<Long>
    }

    @Transactional(readOnly = true)
    fun getPostById(postId: Long, currentUserId: Long? = null): ForumPostResponse {
        val post = findActivePost(postId)

        val business = businessRepository.findById(post.comercioId).orElse(null)
        val user = userRepository.findById(post.autorId).orElse(null)

        return toPostResponse(post, business, user, currentUserId)
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
            if (padre.eliminadoEn != null) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "El comentario al que responde fue eliminado")
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
        val post = findActivePost(postId)

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
                upvotedByMe = forumCommentRepository.hasUpvoted(comment.id, currentUserId),
                createdAt = comment.createdAt,
            )
        }
    }

    @Transactional
    fun updateComment(userId: Long, commentId: Long, request: UpdateCommentRequest): ForumCommentResponse {
        val comment = forumCommentRepository.findById(commentId)
            .filter { it.eliminadoEn == null }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "El comentario no existe") }
        val post = findActivePost(comment.publicacionId)

        if (comment.autorId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede editar el comentario")
        }

        val texto = request.texto?.trim()
        if (texto.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto del comentario es obligatorio")
        }

        // Se edita la fila existente: publicación, comentario padre y upvotes no cambian.
        comment.texto = texto
        val saved = forumCommentRepository.saveAndFlush(comment)
        val user = userRepository.findById(userId).orElse(null)

        return ForumCommentResponse(
            id = saved.id,
            publicacionId = saved.publicacionId,
            comentarioPadreId = saved.comentarioPadreId,
            autorId = saved.autorId,
            autorNombre = formatAuthorName(user?.email ?: "Vecino"),
            texto = saved.texto,
            upvotesCount = forumCommentRepository.countUpvotesByCommentId(saved.id),
            respuestasCount = forumCommentRepository.countByComentarioPadreIdAndEliminadoEnIsNull(saved.id),
            esAutorDePublicacion = (post.autorId == saved.autorId),
            esUsuarioActual = true,
            createdAt = saved.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun getMyActivity(userId: Long): MyForumActivityResponse {
        val user = userRepository.findById(userId).orElse(null)
        val myPosts = forumPostRepository.findByAutorIdAndEliminadoEnIsNullOrderByCreatedAtDesc(userId)
        val myComments = forumCommentRepository.findByAutorIdAndEliminadoEnIsNullOrderByCreatedAtDesc(userId)

        // Publicaciones (propias o ajenas) donde comentó el usuario; se descartan las eliminadas.
        val commentedPosts = forumPostRepository.findAllById(myComments.map { it.publicacionId }.distinct())
            .filter { it.eliminadoEn == null }
            .associateBy { it.id }

        val businessIds = (myPosts.map { it.comercioId } + commentedPosts.values.map { it.comercioId }).distinct()
        val businesses = businessRepository.findAllById(businessIds).associateBy { it.usuarioId }

        val publicaciones = myPosts.map { post -> toPostResponse(post, businesses[post.comercioId], user, userId) }

        val comentarios = myComments.mapNotNull { comment ->
            val post = commentedPosts[comment.publicacionId] ?: return@mapNotNull null
            MyForumCommentResponse(
                id = comment.id,
                publicacionId = comment.publicacionId,
                comentarioPadreId = comment.comentarioPadreId,
                texto = comment.texto,
                upvotesCount = forumCommentRepository.countUpvotesByCommentId(comment.id),
                publicacionTexto = post.texto,
                comercioNombre = businesses[post.comercioId]?.nombreNegocio ?: "Comercio del barrio",
                createdAt = comment.createdAt,
            )
        }

        // Saldo de puntos del vecino (0 si no tiene perfil de cliente).
        val puntos = jdbc.queryForList(
            "SELECT saldo_puntos FROM perfiles_cliente WHERE usuario_id = :id",
            MapSqlParameterSource("id", userId),
            Int::class.java,
        ).firstOrNull() ?: 0

        return MyForumActivityResponse(puntos = puntos, publicaciones = publicaciones, comentarios = comentarios)
    }

    private fun findActivePost(postId: Long): ForumPostEntity {
        val post = forumPostRepository.findById(postId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación no existe")
        }
        if (post.eliminadoEn != null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación fue eliminada")
        }
        return post
    }

    /** Geocodifica la dirección escrita; si el servicio falla o no hay candidatos, queda sin punto. */
    private fun geocode(direccion: String): Point? {
        val candidato = runCatching {
            locationService.buscarCandidatos(ValidateAddressRequest(direccion = direccion))
        }.getOrNull()?.firstOrNull() ?: return null

        return geometryFactory.createPoint(Coordinate(candidato.longitude, candidato.latitude))
    }

    private fun toPostResponse(
        post: ForumPostEntity,
        business: BusinessEntity?,
        user: UserEntity?,
        currentUserId: Long? = null,
    ): ForumPostResponse {
        return ForumPostResponse(
            id = post.id,
            autorId = post.autorId,
            autorNombre = formatAuthorName(user?.email ?: "Vecino"),
            comercioId = post.comercioId,
            comercioNombre = business?.nombreNegocio ?: "Comercio del barrio",
            comercioRubro = business?.rubro?.nombre,
            texto = post.texto,
            precioObservado = post.precioObservado,
            direccion = post.direccionObservada,
            latitud = post.ubicacionObservada?.y,
            longitud = post.ubicacionObservada?.x,
            upvotesCount = forumPostRepository.countUpvotesByPostId(post.id),
            comentariosCount = forumPostRepository.countCommentsByPostId(post.id),
            esUsuarioActual = (currentUserId == post.autorId),
            upvotedByMe = currentUserId?.let { forumPostRepository.hasUpvoted(post.id, it) } ?: false,
            createdAt = post.createdAt,
        )
    }

    private fun formatAuthorName(email: String): String {
        val username = email.substringBefore("@")
        return username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    @Transactional
    fun deletePost(userId: Long, postId: Long) {
        val post = findActivePost(postId)
        if (post.autorId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede eliminar la publicación")
        }

        val now = java.time.OffsetDateTime.now()
        post.eliminadoEn = now
        forumPostRepository.save(post)

        val comentarios = forumCommentRepository.findByPublicacionIdAndEliminadoEnIsNull(postId)
        if (comentarios.isNotEmpty()) {
            comentarios.forEach { it.eliminadoEn = now }
            forumCommentRepository.saveAll(comentarios)
        }
    }

    @Transactional
    fun deleteComment(userId: Long, commentId: Long) {
        val comment = forumCommentRepository.findById(commentId)
            .filter { it.eliminadoEn == null }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "El comentario no existe o ya fue eliminado") }

        if (comment.autorId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede eliminar el comentario")
        }

        comment.eliminadoEn = java.time.OffsetDateTime.now()
        forumCommentRepository.save(comment)
    }
}
