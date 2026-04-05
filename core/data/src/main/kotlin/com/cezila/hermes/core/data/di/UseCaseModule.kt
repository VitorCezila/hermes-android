package com.cezila.hermes.core.data.di

import com.cezila.hermes.core.domain.crypto.PgpDecryptor
import com.cezila.hermes.core.domain.crypto.PgpEncryptor
import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.repository.KeyRepository
import com.cezila.hermes.core.domain.usecase.ClearAllKeysUseCase
import com.cezila.hermes.core.domain.usecase.DecryptAndVerifyUseCase
import com.cezila.hermes.core.domain.usecase.DeleteKeyUseCase
import com.cezila.hermes.core.domain.usecase.EncryptAndSignUseCase
import com.cezila.hermes.core.domain.usecase.EncryptFileAndSignUseCase
import com.cezila.hermes.core.domain.usecase.GenerateKeyPairUseCase
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import com.cezila.hermes.core.domain.usecase.GetKeyByIdUseCase
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

    @Provides
    fun provideGetKeyByIdUseCase(keyRepository: KeyRepository): GetKeyByIdUseCase =
        GetKeyByIdUseCase(keyRepository)

    @Provides
    fun provideDeleteKeyUseCase(keyRepository: KeyRepository): DeleteKeyUseCase =
        DeleteKeyUseCase(keyRepository)

    @Provides
    fun provideEncryptAndSignUseCase(
        keyRepository: KeyRepository,
        pgpEncryptor: PgpEncryptor,
    ): EncryptAndSignUseCase = EncryptAndSignUseCase(keyRepository, pgpEncryptor)

    @Provides
    fun provideEncryptFileAndSignUseCase(
        keyRepository: KeyRepository,
        pgpEncryptor: PgpEncryptor,
    ): EncryptFileAndSignUseCase = EncryptFileAndSignUseCase(keyRepository, pgpEncryptor)

    @Provides
    fun provideDecryptAndVerifyUseCase(
        keyRepository: KeyRepository,
        pgpDecryptor: PgpDecryptor,
    ): DecryptAndVerifyUseCase = DecryptAndVerifyUseCase(keyRepository, pgpDecryptor)

    @Provides
    fun provideClearAllKeysUseCase(
        keyRepository: KeyRepository,
    ): ClearAllKeysUseCase = ClearAllKeysUseCase(keyRepository)
}
