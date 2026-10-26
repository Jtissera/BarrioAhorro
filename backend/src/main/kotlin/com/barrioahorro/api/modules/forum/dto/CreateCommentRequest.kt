package com.barrioahorro.api.modules.forum.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCommentRequest(
    @field:NotBlank(message = "El comentario no puede estar vacío")
    @field:Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    val texto: String?,

    val comentarioPadreId: Long? = null,
)
