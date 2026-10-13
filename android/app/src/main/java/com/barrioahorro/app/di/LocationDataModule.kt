package com.barrioahorro.app.di

import com.barrioahorro.app.data.remote.api.LocationApiService
import com.barrioahorro.app.data.repository.LocationRepositoryImpl
import com.barrioahorro.app.domain.repository.ILocationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationApiModule {

    @Provides
    @Singleton
    fun provideLocationApiService(retrofit: Retrofit): LocationApiService =
        retrofit.create(LocationApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationRepositoryModule {

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): ILocationRepository
}

