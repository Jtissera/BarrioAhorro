package com.barrioahorro.app.di

import com.barrioahorro.app.data.cuponera.CuponeraApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Asume que ya tenés un Retrofit singleton provisto en otro módulo (el mismo que
 * usa el onboarding, con el interceptor del token). Si no, adaptá esta función.
 */
@Module
@InstallIn(SingletonComponent::class)
object CuponeraModule {

    @Provides
    @Singleton
    fun provideCuponeraApi(retrofit: Retrofit): CuponeraApi =
        retrofit.create(CuponeraApi::class.java)
}