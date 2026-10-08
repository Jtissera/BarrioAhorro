package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.user.UserEntity
import com.barrioahorro.api.modules.user.UserRepository
import com.barrioahorro.api.modules.user.enum.TipoUsuario
import com.barrioahorro.backend.BackendApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest(classes = [BackendApplication::class])
@Transactional
class BusinessControllerTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var mockMvc: MockMvc
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        // El trigger de la base crea el perfil de comercio al insertar el usuario.
        val user = userRepository.saveAndFlush(
            UserEntity(
                email = "test-${UUID.randomUUID().toString().take(8)}@gmail.com",
                passwordHash = "hash",
                tipoUsuario = TipoUsuario.COMERCIO,
            ),
        )
        userId = user.id
    }

    private fun asBusiness() = authentication(UsernamePasswordAuthenticationToken(userId, null, emptyList()))

    private fun patchMe(body: String) =
        mockMvc.perform(
            patch("/api/business/me").with(asBusiness())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    @Test
    fun `actualiza horarios, direccion, rubro y descripcion`() {
        val category = categoryRepository.findAll().first()

        patchMe(
            """
            {
              "categoryId": ${category.id},
              "direccion": "  Av. Rivadavia 4520  ",
              "descripcion": "Pan casero todos los días",
              "horarios": [
                {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00"},
                {"diaSemana": 1, "horaInicio": "16:00", "horaFin": "20:00"}
              ]
            }
            """.trimIndent(),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categoryId").value(category.id.toInt()))
            .andExpect(jsonPath("$.categoryName").value(category.nombre))
            .andExpect(jsonPath("$.direccion").value("Av. Rivadavia 4520"))
            .andExpect(jsonPath("$.descripcion").value("Pan casero todos los días"))
            .andExpect(jsonPath("$.horarios.length()").value(2))
            .andExpect(jsonPath("$.horarios[1].horaInicio").value("16:00"))
    }

    @Test
    fun `los cambios se ven al consultar el perfil`() {
        patchMe("""{"direccion": "Calle Falsa 123", "descripcion": "Nueva descripción"}""")
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/business/me").with(asBusiness()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.direccion").value("Calle Falsa 123"))
            .andExpect(jsonPath("$.descripcion").value("Nueva descripción"))
    }

    @Test
    fun `los campos que no se envian no se modifican`() {
        patchMe("""{"direccion": "Calle Falsa 123", "descripcion": "Original"}""")
            .andExpect(status().isOk)

        patchMe("""{"descripcion": "Cambiada"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.direccion").value("Calle Falsa 123"))
            .andExpect(jsonPath("$.descripcion").value("Cambiada"))
    }

    @Test
    fun `rechaza direccion vacia`() {
        patchMe("""{"direccion": "   "}""").andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza descripcion vacia`() {
        patchMe("""{"descripcion": ""}""").andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza rubro inexistente`() {
        patchMe("""{"categoryId": 9999}""").andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza latitud sin longitud`() {
        patchMe("""{"latitud": -34.6}""").andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza dia de la semana fuera de rango`() {
        patchMe("""{"horarios": [{"diaSemana": 8, "horaInicio": "08:00", "horaFin": "12:00"}]}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza hora con formato invalido`() {
        patchMe("""{"horarios": [{"diaSemana": 1, "horaInicio": "8am", "horaFin": "12:00"}]}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza horario que cierra antes de abrir`() {
        patchMe("""{"horarios": [{"diaSemana": 1, "horaInicio": "18:00", "horaFin": "09:00"}]}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rechaza turnos superpuestos en el mismo dia`() {
        patchMe(
            """
            {"horarios": [
              {"diaSemana": 2, "horaInicio": "08:00", "horaFin": "13:00"},
              {"diaSemana": 2, "horaInicio": "12:00", "horaFin": "18:00"}
            ]}
            """.trimIndent(),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `un horario rechazado no borra los horarios anteriores`() {
        patchMe("""{"horarios": [{"diaSemana": 3, "horaInicio": "09:00", "horaFin": "17:00"}]}""")
            .andExpect(status().isOk)

        patchMe("""{"horarios": [{"diaSemana": 3, "horaInicio": "17:00", "horaFin": "09:00"}]}""")
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/business/me").with(asBusiness()))
            .andExpect(jsonPath("$.horarios.length()").value(1))
            .andExpect(jsonPath("$.horarios[0].horaInicio").value("09:00"))
    }
}
