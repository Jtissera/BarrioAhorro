package com.barrioahorro.app.di

import com.barrioahorro.app.data.remote.api.OfferApiService
import com.barrioahorro.app.data.repository.OfferRepositoryImpl
import com.barrioahorro.app.domain.repository.IOfferRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfferApiModule {

    @Provides
    @Singleton
    fun provideOfferApiService(retrofit: Retrofit): OfferApiService =
        retrofit.create(OfferApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OfferRepositoryModule {

    @Binds
    abstract fun bindOfferRepository(impl: OfferRepositoryImpl): IOfferRepository
}

