package com.barrioahorro.api.modules.auth.dto

import com.barrioahorro.api.modules.user.enum.TipoUsuario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size


data class RegisterRequest(
    @field:NotBlank(message = "El email es obligatorio")
    @field:Email(message = "El email no es válido")
    @field:Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$",
        message = "Por ahora solo aceptamos direcciones @gmail.com",
    )
    val email: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    val password: String,

    @field:NotBlank(message = "Repetí la contraseña")
    val confirmPassword: String,

    @field:NotNull(message = "Indicá si sos cliente o comerciante")
    val tipoUsuario: TipoUsuario,
) {
    val passwordsMatch: Boolean
        get() = password == confirmPassword
}