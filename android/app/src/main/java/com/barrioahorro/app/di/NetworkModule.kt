package com.barrioahorro.app.di

import com.barrioahorro.app.data.remote.api.AuthApiService
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.api.CategoryApiService
import com.barrioahorro.app.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// TODO: mover a un archivo de config por variante (debug/release) o a BuildConfig
const val BASE_URL = "http://10.0.2.2:8080/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideBusinessApiService(retrofit: Retrofit): BusinessApiService =
        retrofit.create(BusinessApiService::class.java)

    @Provides
    @Singleton
    fun provideCategoryApiService(retrofit: Retrofit): CategoryApiService =
        retrofit.create(CategoryApiService::class.java)
}