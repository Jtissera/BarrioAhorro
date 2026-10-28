package com.barrioahorro.app.di

import com.barrioahorro.app.data.profile.BusinessProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BusinessProfileModule {

    @Provides
    @Singleton
    fun provideBusinessProfileApi(retrofit: Retrofit): BusinessProfileApi =
        retrofit.create(BusinessProfileApi::class.java)
}
