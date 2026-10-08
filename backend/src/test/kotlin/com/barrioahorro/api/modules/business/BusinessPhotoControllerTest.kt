package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.user.UserEntity
import com.barrioahorro.api.modules.user.UserRepository
import com.barrioahorro.api.modules.user.enum.TipoUsuario
import com.barrioahorro.backend.BackendApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

// Cabeceras mínimas para que el archivo se reconozca como imagen.
private val JPEG_BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4)
private val PNG_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

@SpringBootTest(classes = [BackendApplication::class])
@Transactional
class BusinessPhotoControllerTest {

    companion object {
        private val uploadsDir: Path = Files.createTempDirectory("barrioahorro-uploads-test")

        @JvmStatic
        @DynamicPropertySource
        fun uploadsProperties(registry: DynamicPropertyRegistry) {
            registry.add("app.uploads.dir") { uploadsDir.toString() }
        }
    }

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var mockMvc: MockMvc
    private var userId: Long = 0
    private val jsonMapper = JsonMapper.builder().build()

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        userId = createBusinessUser()
    }

    private fun createBusinessUser(): Long =
        userRepository.saveAndFlush(
            UserEntity(
                email = "test-${UUID.randomUUID().toString().take(8)}@gmail.com",
                passwordHash = "hash",
                tipoUsuario = TipoUsuario.COMERCIO,
            ),
        ).id

    private fun asUser(id: Long = userId) = authentication(UsernamePasswordAuthenticationToken(id, null, emptyList()))

    private fun image(bytes: ByteArray = JPEG_BYTES, type: String = MediaType.IMAGE_JPEG_VALUE) =
        MockMultipartFile("file", "foto.jpg", type, bytes)

    private fun upload(file: MockMultipartFile = image(), user: Long = userId): ResultActions =
        mockMvc.perform(multipart("/api/business/me/photos").file(file).with(asUser(user)))

    private fun uploadAndGetId(file: MockMultipartFile = image()): Long {
        val body = upload(file).andExpect(status().isCreated).andReturn().response.contentAsString
        return jsonMapper.readTree(body).get("id").asLong()
    }

    private fun replace(photoId: Long, file: MockMultipartFile = image(PNG_BYTES, MediaType.IMAGE_PNG_VALUE)): ResultActions =
        mockMvc.perform(
            multipart(HttpMethod.PUT, "/api/business/me/photos/$photoId").file(file).with(asUser()),
        )

    private fun listPhotos(): ResultActions =
        mockMvc.perform(get("/api/business/me/photos").with(asUser()))

    private fun storedFile(url: String): Path = uploadsDir.resolve(url.removePrefix("/uploads/"))

    private fun urlOf(photoId: Long): String {
        val body = listPhotos().andReturn().response.contentAsString
        return jsonMapper.readTree(body).first { it.get("id").asLong() == photoId }.get("url").asString()
    }

    @Test
    fun `sube una foto y la devuelve con su url publica`() {
        upload()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.orden").value(0))
            .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/uploads/")))
    }

    @Test
    fun `el archivo subido se guarda en disco`() {
        val id = uploadAndGetId()

        assertTrue(Files.exists(storedFile(urlOf(id))))
    }

    @Test
    fun `las fotos se listan en orden de carga`() {
        val first = uploadAndGetId()
        val second = uploadAndGetId()

        listPhotos()
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(first))
            .andExpect(jsonPath("$[1].id").value(second))
            .andExpect(jsonPath("$[1].orden").value(1))
    }

    @Test
    fun `las fotos aparecen en el perfil del comercio`() {
        val id = uploadAndGetId()

        mockMvc.perform(get("/api/business/me").with(asUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fotos[0].id").value(id))
    }

    @Test
    fun `rechaza un archivo que no es imagen aunque diga serlo`() {
        upload(image("no soy una imagen".toByteArray()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza un archivo vacio`() {
        upload(image(ByteArray(0))).andExpect(status().isBadRequest)
    }

    @Test
    fun `no permite mas de 5 fotos`() {
        repeat(5) { uploadAndGetId() }

        upload().andExpect(status().isConflict)
    }

    @Test
    fun `reemplazar una foto mantiene su lugar, cambia la url y borra el archivo anterior`() {
        uploadAndGetId()
        val id = uploadAndGetId()
        val oldUrl = urlOf(id)

        replace(id)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.orden").value(1))

        val newUrl = urlOf(id)
        assertFalse(newUrl == oldUrl)
        assertTrue(Files.exists(storedFile(newUrl)))
        assertFalse(Files.exists(storedFile(oldUrl)))
    }

    @Test
    fun `borrar una foto la quita del listado, borra el archivo y reacomoda el orden`() {
        val first = uploadAndGetId()
        val second = uploadAndGetId()
        val firstUrl = urlOf(first)

        mockMvc.perform(delete("/api/business/me/photos/$first").with(asUser()))
            .andExpect(status().isNoContent)

        listPhotos()
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(second))
            .andExpect(jsonPath("$[0].orden").value(0))
        assertFalse(Files.exists(storedFile(firstUrl)))
    }

    @Test
    fun `reordena las fotos`() {
        val a = uploadAndGetId()
        val b = uploadAndGetId()
        val c = uploadAndGetId()

        mockMvc.perform(
            put("/api/business/me/photos/order").with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"photoIds": [$c, $a, $b]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(c))
            .andExpect(jsonPath("$[1].id").value(a))
            .andExpect(jsonPath("$[2].id").value(b))
            .andExpect(jsonPath("$[2].orden").value(2))
    }

    @Test
    fun `reordenar exige incluir exactamente todas las fotos`() {
        val a = uploadAndGetId()
        uploadAndGetId()

        mockMvc.perform(
            put("/api/business/me/photos/order").with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"photoIds": [$a]}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `no se puede borrar ni reemplazar la foto de otro comercio`() {
        val otherUser = createBusinessUser()
        val body = upload(user = otherUser).andExpect(status().isCreated).andReturn().response.contentAsString
        val othersPhoto = jsonMapper.readTree(body).get("id").asLong()

        mockMvc.perform(delete("/api/business/me/photos/$othersPhoto").with(asUser()))
            .andExpect(status().isNotFound)
        replace(othersPhoto).andExpect(status().isNotFound)
    }

    @Test
    fun `las fotos se sirven publicamente sin autenticacion`() {
        val id = uploadAndGetId()

        mockMvc.perform(get(urlOf(id)))
            .andExpect(status().isOk)
            .andExpect(content().bytes(JPEG_BYTES))
    }
}
