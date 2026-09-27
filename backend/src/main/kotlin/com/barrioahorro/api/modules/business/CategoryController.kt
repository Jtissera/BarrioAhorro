package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.CategoryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryRepository: CategoryRepository,
) {
    @GetMapping
    fun getAllCategories(): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryRepository.findAll().sortedBy { it.nombre }.map { it.toResponse() })

    private fun CategoryEntity.toResponse() = CategoryResponse(id = id, name = nombre)
}
