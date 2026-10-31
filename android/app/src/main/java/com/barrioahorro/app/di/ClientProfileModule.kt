package com.barrioahorro.app.di

import com.barrioahorro.app.data.profile.ClientProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClientProfileModule {

    @Provides
    @Singleton
    fun provideClientProfileApi(retrofit: Retrofit): ClientProfileApi =
        retrofit.create(ClientProfileApi::class.java)
}