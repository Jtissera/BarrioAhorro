package com.barrioahorro.api.modules.auth.dto

import com.barrioahorro.api.modules.user.enum.TipoUsuario

data class AuthResponse(
    val accessToken: String,
    val userId: Long,
    val email: String,
    val tipoUsuario: TipoUsuario,
)