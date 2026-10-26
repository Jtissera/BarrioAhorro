package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.user.UserEntity
import com.barrioahorro.api.modules.user.UserRepository
import com.barrioahorro.api.modules.user.enum.TipoUsuario
import com.barrioahorro.backend.BackendApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest(classes = [BackendApplication::class])
@Transactional
class ForumMyPostsControllerTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var businessRepository: BusinessRepository

    @Autowired
    private lateinit var forumPostRepository: ForumPostRepository

    @Autowired
    private lateinit var forumCommentRepository: ForumCommentRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var mockMvc: MockMvc
    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var businessId: Long = 0

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        // El trigger de la base crea el perfil (cliente o comercio) al insertar el usuario.
        userId = newUser(TipoUsuario.CLIENTE)
        otherUserId = newUser(TipoUsuario.CLIENTE)
        businessId = newUser(TipoUsuario.COMERCIO)
        val business = businessRepository.findById(businessId).orElseThrow()
        business.nombreNegocio = "Almacén Don Pepe"
        businessRepository.saveAndFlush(business)
    }

    private fun newUser(tipo: TipoUsuario): Long =
        userRepository.saveAndFlush(
            UserEntity(
                email = "test-${UUID.randomUUID().toString().take(8)}@gmail.com",
                passwordHash = "hash",
                tipoUsuario = tipo,
            ),
        ).id

    private fun asUser(id: Long) = authentication(UsernamePasswordAuthenticationToken(id, null, emptyList()))

    private fun newPost(autorId: Long, texto: String, createdAt: OffsetDateTime, eliminado: Boolean = false) =
        forumPostRepository.saveAndFlush(
            ForumPostEntity(
                autorId = autorId,
                comercioId = businessId,
                texto = texto,
                eliminadoEn = if (eliminado) createdAt else null,
                createdAt = createdAt,
            ),
        )

    private fun newComment(
        postId: Long,
        autorId: Long,
        texto: String,
        createdAt: OffsetDateTime,
        padreId: Long? = null,
        eliminado: Boolean = false,
    ) = forumCommentRepository.saveAndFlush(
        ForumCommentEntity(
            publicacionId = postId,
            comentarioPadreId = padreId,
            autorId = autorId,
            texto = texto,
            eliminadoEn = if (eliminado) createdAt else null,
            createdAt = createdAt,
        ),
    )

    @Test
    fun `devuelve solo las publicaciones y comentarios propios no eliminados, mas recientes primero`() {
        val base = OffsetDateTime.now().minusDays(1)

        val oldPost = newPost(userId, "Mi publicación vieja", base)
        val newPostEntity = newPost(userId, "Mi publicación nueva", base.plusHours(1))
        val deletedPost = newPost(userId, "Mi publicación borrada", base.plusHours(2), eliminado = true)
        val otherPost = newPost(otherUserId, "Publicación de otro vecino", base.plusHours(3))

        val myComment = newComment(otherPost.id, userId, "Mi comentario", base.plusHours(4))
        val othersComment = newComment(otherPost.id, otherUserId, "Comentario de otro", base.plusHours(5))
        val myReply = newComment(otherPost.id, userId, "Mi respuesta", base.plusHours(6), padreId = othersComment.id)
        newComment(otherPost.id, userId, "Mi comentario borrado", base.plusHours(7), eliminado = true)
        newComment(deletedPost.id, userId, "Comentario en publicación borrada", base.plusHours(8))

        jdbcTemplate.update(
            "INSERT INTO foro_publicacion_upvotes (publicacion_id, usuario_id) VALUES (?, ?)",
            newPostEntity.id, otherUserId,
        )
        jdbcTemplate.update(
            "INSERT INTO foro_comentario_upvotes (comentario_id, usuario_id) VALUES (?, ?)",
            myReply.id, otherUserId,
        )

        mockMvc.perform(get("/api/foro/mis-publicaciones").with(asUser(userId)))
            .andExpect(status().isOk)
            // Cada upvote recibido suma un punto vía el ledger de movimientos_puntos.
            .andExpect(jsonPath("$.puntos").value(2))
            .andExpect(jsonPath("$.publicaciones.length()").value(2))
            .andExpect(jsonPath("$.publicaciones[0].id").value(newPostEntity.id.toInt()))
            .andExpect(jsonPath("$.publicaciones[0].upvotesCount").value(1))
            .andExpect(jsonPath("$.publicaciones[0].comercioNombre").value("Almacén Don Pepe"))
            .andExpect(jsonPath("$.publicaciones[1].id").value(oldPost.id.toInt()))
            .andExpect(jsonPath("$.publicaciones[1].upvotesCount").value(0))
            .andExpect(jsonPath("$.comentarios.length()").value(2))
            .andExpect(jsonPath("$.comentarios[0].id").value(myReply.id.toInt()))
            .andExpect(jsonPath("$.comentarios[0].publicacionId").value(otherPost.id.toInt()))
            .andExpect(jsonPath("$.comentarios[0].comentarioPadreId").value(othersComment.id.toInt()))
            .andExpect(jsonPath("$.comentarios[0].upvotesCount").value(1))
            .andExpect(jsonPath("$.comentarios[0].publicacionTexto").value("Publicación de otro vecino"))
            .andExpect(jsonPath("$.comentarios[0].comercioNombre").value("Almacén Don Pepe"))
            .andExpect(jsonPath("$.comentarios[1].id").value(myComment.id.toInt()))
            .andExpect(jsonPath("$.comentarios[1].upvotesCount").value(0))
    }

    @Test
    fun `devuelve listas vacias si el usuario no tiene actividad`() {
        newPost(otherUserId, "Publicación de otro vecino", OffsetDateTime.now())

        mockMvc.perform(get("/api/foro/mis-publicaciones").with(asUser(userId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.puntos").value(0))
            .andExpect(jsonPath("$.publicaciones.length()").value(0))
            .andExpect(jsonPath("$.comentarios.length()").value(0))
    }
}
