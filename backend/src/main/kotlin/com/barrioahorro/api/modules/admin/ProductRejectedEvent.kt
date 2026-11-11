package com.barrioahorro.api.modules.admin

data class ProductRejectedEvent(
    val kind: PublicationKind,
    val comercioId: Long,
    val productName: String,
    val reason: String,
)