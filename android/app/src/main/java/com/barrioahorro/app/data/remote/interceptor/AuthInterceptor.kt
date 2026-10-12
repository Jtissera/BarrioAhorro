package com.barrioahorro.app.data.remote.interceptor

import com.barrioahorro.app.data.local.datastore.AuthTokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authTokenDataStore: AuthTokenDataStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.url.encodedPath.startsWith("/api/auth/")) {
            return chain.proceed(request)
        }

        // Recuperamos el token guardado
        val token = runBlocking {
            authTokenDataStore.currentToken()
        } ?: return chain.proceed(request)

        // Agregamos el token a la petición
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}