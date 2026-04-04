package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository

class GenerateKeyPairUseCase(
    private val keyRepository: KeyRepository,
    private val pgpKeyGenerator: PgpKeyGenerator,
) {
    data class Params(
        val ownerName: String,
        val ownerEmail: String,
        val algorithm: KeyAlgorithm,
        val passphrase: CharArray,
    )

    suspend operator fun invoke(params: Params): Result<PgpKey> {
        val materialResult = pgpKeyGenerator.generate(
            ownerName = params.ownerName,
            ownerEmail = params.ownerEmail,
            algorithm = params.algorithm,
            passphrase = params.passphrase,
        )

        val material = materialResult.getOrElse { return Result.failure(it) }

        val pgpKey = PgpKey(
            id = material.keyId,
            fingerprint = material.fingerprint,
            ownerName = params.ownerName,
            ownerEmail = params.ownerEmail,
            algorithm = params.algorithm,
            createdAt = material.createdAt,
            expiresAt = null,
            isSecret = true,
            armoredPublicKey = material.armoredPublicKey,
        )

        keyRepository.saveKeyPair(
            pgpKey = pgpKey,
            armoredPrivateKey = material.armoredPrivateKey,
            passphrase = params.passphrase,
        ).getOrElse { return Result.failure(it) }

        return Result.success(pgpKey)
    }
}
