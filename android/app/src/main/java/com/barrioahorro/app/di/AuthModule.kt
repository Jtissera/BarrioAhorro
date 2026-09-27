package com.barrioahorro.app.di

import com.barrioahorro.app.data.repository.AuthRepositoryImpl
import com.barrioahorro.app.domain.repository.IAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): IAuthRepository
}