package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.location.LocationService
import com.barrioahorro.api.modules.location.dto.AddressCandidateResponse
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import com.barrioahorro.api.modules.user.UserEntity
import com.barrioahorro.api.modules.user.UserRepository
import com.barrioahorro.api.modules.user.enum.TipoUsuario
import com.barrioahorro.backend.BackendApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest(classes = [BackendApplication::class])
@Transactional
class ForumEditControllerTest {

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

    // Geocodificación determinística: nunca se llama a Nominatim desde los tests.
    @MockitoBean
    private lateinit var locationService: LocationService

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private lateinit var mockMvc: MockMvc
    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var thirdUserId: Long = 0
    private var businessId: Long = 0

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        // El trigger de la base crea el perfil (cliente o comercio) al insertar el usuario.
        userId = newUser(TipoUsuario.CLIENTE)
        otherUserId = newUser(TipoUsuario.CLIENTE)
        thirdUserId = newUser(TipoUsuario.CLIENTE)
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

    private fun newPost(autorId: Long, texto: String = "Texto original", eliminado: Boolean = false) =
        forumPostRepository.saveAndFlush(
            ForumPostEntity(
                autorId = autorId,
                comercioId = businessId,
                texto = texto,
                precioObservado = BigDecimal("1000.00"),
                eliminadoEn = if (eliminado) OffsetDateTime.now() else null,
            ),
        )

    private fun newComment(postId: Long, autorId: Long, padreId: Long? = null) =
        forumCommentRepository.saveAndFlush(
            ForumCommentEntity(
                publicacionId = postId,
                comentarioPadreId = padreId,
                autorId = autorId,
                texto = "Comentario original",
            ),
        )

    /** Mockito.any() devuelve null; el elvis satisface el parámetro no nulo de Kotlin. */
    private fun anyAddressRequest(): ValidateAddressRequest =
        Mockito.any(ValidateAddressRequest::class.java) ?: ValidateAddressRequest(direccion = "")

    private fun givenGeocodingReturns(candidates: List<AddressCandidateResponse>) {
        Mockito.`when`(locationService.buscarCandidatos(anyAddressRequest())).thenReturn(candidates)
    }

    private fun newPostWithLocation(autorId: Long) =
        forumPostRepository.saveAndFlush(
            ForumPostEntity(
                autorId = autorId,
                comercioId = businessId,
                texto = "Texto original",
                ubicacionObservada = geometryFactory.createPoint(Coordinate(-58.43, -34.61)),
                direccionObservada = "Rivadavia 5000",
            ),
        )

    private fun upvotePost(postId: Long, usuarioId: Long) {
        jdbcTemplate.update(
            "INSERT INTO foro_publicacion_upvotes (publicacion_id, usuario_id) VALUES (?, ?)",
            postId, usuarioId,
        )
    }

    private fun patchJson(url: String, body: String, asUserId: Long) =
        mockMvc.perform(
            patch(url).with(asUser(asUserId)).contentType(MediaType.APPLICATION_JSON).content(body),
        )

    @Test
    fun `el autor edita texto, precio y ubicacion conservando comercio, id y upvotes`() {
        val post = newPost(userId)
        upvotePost(post.id, otherUserId)
        upvotePost(post.id, thirdUserId)

        patchJson(
            "/api/foro/publicaciones/${post.id}",
            """{"texto":"Texto corregido","precioObservado":1500.5,"direccion":"Av. Corrientes y Callao","latitud":-34.6037,"longitud":-58.3816}""",
            userId,
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(post.id.toInt()))
            .andExpect(jsonPath("$.comercioId").value(businessId.toInt()))
            .andExpect(jsonPath("$.comercioNombre").value("Almacén Don Pepe"))
            .andExpect(jsonPath("$.texto").value("Texto corregido"))
            .andExpect(jsonPath("$.precioObservado").value(1500.5))
            .andExpect(jsonPath("$.direccion").value("Av. Corrientes y Callao"))
            .andExpect(jsonPath("$.latitud").value(-34.6037))
            .andExpect(jsonPath("$.longitud").value(-58.3816))
            .andExpect(jsonPath("$.upvotesCount").value(2))
            .andExpect(jsonPath("$.esUsuarioActual").value(true))

        // La fila se actualiza en el lugar: misma id, nuevo texto.
        assertEquals("Texto corregido", forumPostRepository.findById(post.id).orElseThrow().texto)
    }

    @Test
    fun `sin precio ni direccion se limpian ambos campos`() {
        val post = newPost(userId)

        patchJson("/api/foro/publicaciones/${post.id}", """{"texto":"Solo texto","direccion":"  "}""", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.precioObservado").doesNotExist())
            .andExpect(jsonPath("$.direccion").doesNotExist())
            .andExpect(jsonPath("$.latitud").doesNotExist())
    }

    @Test
    fun `otro usuario no puede editar la publicacion`() {
        val post = newPost(userId)

        patchJson("/api/foro/publicaciones/${post.id}", """{"texto":"Intento ajeno"}""", otherUserId)
            .andExpect(status().isForbidden)
    }

    @Test
    fun `el texto de la publicacion es obligatorio y tiene maximo 300 caracteres`() {
        val post = newPost(userId)

        patchJson("/api/foro/publicaciones/${post.id}", """{"texto":"   "}""", userId)
            .andExpect(status().isBadRequest)
        patchJson("/api/foro/publicaciones/${post.id}", """{"texto":"${"a".repeat(301)}"}""", userId)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `una publicacion eliminada no se puede editar`() {
        val post = newPost(userId, eliminado = true)

        patchJson("/api/foro/publicaciones/${post.id}", """{"texto":"Nuevo"}""", userId)
            .andExpect(status().isNotFound)
    }

    @Test
    fun `el detalle indica si el usuario actual es el autor`() {
        val post = newPost(userId)

        mockMvc.perform(get("/api/foro/publicaciones/${post.id}").with(asUser(userId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.esUsuarioActual").value(true))
        mockMvc.perform(get("/api/foro/publicaciones/${post.id}").with(asUser(otherUserId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.esUsuarioActual").value(false))
    }

    @Test
    fun `el autor edita su comentario conservando hilo y upvotes`() {
        val post = newPost(otherUserId)
        val padre = newComment(post.id, otherUserId)
        val comment = newComment(post.id, userId, padreId = padre.id)
        jdbcTemplate.update(
            "INSERT INTO foro_comentario_upvotes (comentario_id, usuario_id) VALUES (?, ?)",
            comment.id, otherUserId,
        )

        patchJson("/api/foro/comentarios/${comment.id}", """{"texto":"Comentario corregido"}""", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(comment.id.toInt()))
            .andExpect(jsonPath("$.texto").value("Comentario corregido"))
            .andExpect(jsonPath("$.publicacionId").value(post.id.toInt()))
            .andExpect(jsonPath("$.comentarioPadreId").value(padre.id.toInt()))
            .andExpect(jsonPath("$.upvotesCount").value(1))
            .andExpect(jsonPath("$.esUsuarioActual").value(true))
            .andExpect(jsonPath("$.esAutorDePublicacion").value(false))
    }

    @Test
    fun `otro usuario no puede editar el comentario`() {
        val post = newPost(userId)
        val comment = newComment(post.id, userId)

        patchJson("/api/foro/comentarios/${comment.id}", """{"texto":"Intento ajeno"}""", otherUserId)
            .andExpect(status().isForbidden)
    }

    @Test
    fun `el texto del comentario es obligatorio`() {
        val post = newPost(userId)
        val comment = newComment(post.id, userId)

        patchJson("/api/foro/comentarios/${comment.id}", """{"texto":""}""", userId)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `crear con direccion la devuelve en la respuesta`() {
        mockMvc.perform(
            post("/api/foro/publicaciones").with(asUser(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"comercioId":$businessId,"texto":"Buen precio","direccion":" Rivadavia 5000 ","latitud":-34.61,"longitud":-58.43}""",
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.direccion").value("Rivadavia 5000"))
    }

    @Test
    fun `una direccion nueva que no se puede geocodificar devuelve 400 y no modifica la publicacion`() {
        val post = newPostWithLocation(userId)
        givenGeocodingReturns(emptyList())

        patchJson(
            "/api/foro/publicaciones/${post.id}",
            """{"texto":"Texto nuevo","direccion":"Av. Corrientes 1500, CABA"}""",
            userId,
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error").value(
                    "No encontramos esa dirección. Probá con calle y altura (ej: Av. Corrientes 1500).",
                ),
            )

        val row = jdbcTemplate.queryForMap(
            """
            SELECT texto, direccion_observada,
                   ST_Y(ubicacion_observada::geometry) AS lat, ST_X(ubicacion_observada::geometry) AS lng
            FROM foro_publicaciones WHERE id = ?
            """.trimIndent(),
            post.id,
        )
        assertEquals("Texto original", row["texto"])
        assertEquals("Rivadavia 5000", row["direccion_observada"])
        assertEquals(-34.61, row["lat"] as Double, 1e-6)
        assertEquals(-58.43, row["lng"] as Double, 1e-6)
    }

    @Test
    fun `una direccion nueva geocodificada actualiza el punto`() {
        val post = newPostWithLocation(userId)
        givenGeocodingReturns(
            listOf(
                AddressCandidateResponse(
                    label = "Avenida Corrientes 1500, CABA",
                    locality = "CABA",
                    latitude = -34.6045,
                    longitude = -58.3870,
                    exact = true,
                ),
            ),
        )

        patchJson(
            "/api/foro/publicaciones/${post.id}",
            """{"texto":"Texto nuevo","direccion":"Av. Corrientes 1500, CABA"}""",
            userId,
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.direccion").value("Av. Corrientes 1500, CABA"))
            .andExpect(jsonPath("$.latitud").value(-34.6045))
            .andExpect(jsonPath("$.longitud").value(-58.3870))
    }
}
