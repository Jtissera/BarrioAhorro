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
        val token = runBlocking { authTokenDataStore.currentToken() }

        val request = chain.request()
        if (token == null) {
            return chain.proceed(request)
        }

        val authenticatedRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}