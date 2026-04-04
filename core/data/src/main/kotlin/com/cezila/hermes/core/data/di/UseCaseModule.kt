package com.cezila.hermes.core.data.di

import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.repository.KeyRepository
import com.cezila.hermes.core.domain.usecase.GenerateKeyPairUseCase
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import com.cezila.hermes.core.domain.usecase.ImportPublicKeyUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    fun provideGenerateKeyPairUseCase(
        keyRepository: KeyRepository,
        pgpKeyGenerator: PgpKeyGenerator,
    ): GenerateKeyPairUseCase = GenerateKeyPairUseCase(keyRepository, pgpKeyGenerator)

    @Provides
    fun provideGetAllKeysUseCase(keyRepository: KeyRepository): GetAllKeysUseCase =
        GetAllKeysUseCase(keyRepository)

    @Provides
    fun provideImportPublicKeyUseCase(
        keyRepository: KeyRepository,
        pgpKeyParser: PgpKeyParser,
    ): ImportPublicKeyUseCase = ImportPublicKeyUseCase(keyRepository, pgpKeyParser)
}
