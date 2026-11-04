package com.barrioahorro.app.di

import com.barrioahorro.app.data.raffles.RaffleApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RaffleModule {

    @Provides
    @Singleton
    fun provideRaffleApi(retrofit: Retrofit): RaffleApi =
        retrofit.create(RaffleApi::class.java)
}