package com.barrioahorro.api.modules.business

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Column

@Entity
@Table(name = "rubros")
class CategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short = 0,

    @Column(nullable = false, unique = true)
    val nombre: String = "",
)