package com.barrioahorro.app.data.repository

import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.dto.toDomain
import com.barrioahorro.app.domain.model.BusinessDashboard
import com.barrioahorro.app.domain.repository.DashboardRepository
import java.io.IOException
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val api: BusinessApiService
) : DashboardRepository {

    override suspend fun getDashboard(): Result<BusinessDashboard> = try {
        val response = api.getDashboard()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(IOException("No se pudo cargar el panel (${response.code()})"))
        }
    } catch (e: IOException) {
        Result.failure(IOException("Sin conexión. Revisá tu internet e intentá de nuevo"))
    }
}