package com.cezila.hermes.core.data.di

import com.cezila.hermes.core.data.repository.KeyRepositoryImpl
import com.cezila.hermes.core.domain.repository.KeyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindKeyRepository(impl: KeyRepositoryImpl): KeyRepository
}
