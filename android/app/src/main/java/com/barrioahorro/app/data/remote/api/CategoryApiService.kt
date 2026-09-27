package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.CategoryDto
import retrofit2.Response
import retrofit2.http.GET

interface CategoryApiService {
    @GET("api/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>
}