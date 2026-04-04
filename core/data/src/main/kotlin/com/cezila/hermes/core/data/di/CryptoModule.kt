package com.cezila.hermes.core.data.di

import com.cezila.hermes.core.data.crypto.BouncyCastlePgpKeyGenerator
import com.cezila.hermes.core.data.keystore.KeystoreManager
import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun providePgpKeyGenerator(): PgpKeyGenerator = BouncyCastlePgpKeyGenerator()
}
