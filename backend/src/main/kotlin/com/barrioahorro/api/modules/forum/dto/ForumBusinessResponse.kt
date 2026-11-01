package com.barrioahorro.api.modules.forum.dto

data class ForumBusinessResponse(
    val id: Long,
    val nombre: String,
    val rubro: String? = null,
)