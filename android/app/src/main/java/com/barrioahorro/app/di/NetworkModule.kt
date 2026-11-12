package com.barrioahorro.app.di

import com.barrioahorro.app.BuildConfig
import com.barrioahorro.app.data.cuponera.CuponeraApi
import com.barrioahorro.app.data.explore.ExploreApi
import com.barrioahorro.app.data.remote.api.AuthApiService
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.api.CategoryApiService
import com.barrioahorro.app.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// La URL sale del buildType (ver app/build.gradle.kts). Debe terminar en "/".
// Se mantiene el nombre para no tocar los archivos que ya lo importan.
val BASE_URL: String = BuildConfig.BASE_URL

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Evita la página de advertencia que ngrok (plan gratuito) pone delante del tráfico de navegadores. */
    private val skipNgrokWarning = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("ngrok-skip-browser-warning", "1")
                .build(),
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        // BODY escribe contraseñas y tokens en logcat: solo en debug.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(skipNgrokWarning)
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

    @Provides
    @Singleton
    fun provideCuponeraApi(retrofit: Retrofit): CuponeraApi =
        retrofit.create(CuponeraApi::class.java)

    @Provides
    @Singleton
    fun provideExploreApi(retrofit: Retrofit): ExploreApi =
        retrofit.create(ExploreApi::class.java)

    @Provides
    @Singleton
    fun provideForumApi(retrofit: Retrofit): com.barrioahorro.app.data.forum.ForumApi =
        retrofit.create(com.barrioahorro.app.data.forum.ForumApi::class.java)

}