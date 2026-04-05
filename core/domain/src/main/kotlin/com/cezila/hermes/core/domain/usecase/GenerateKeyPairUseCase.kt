package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository
import kotlinx.coroutines.flow.first

class GenerateKeyPairUseCase(
    private val keyRepository: KeyRepository,
    private val pgpKeyGenerator: PgpKeyGenerator,
) {
    data class Params(
        val ownerName: String,
        val ownerEmail: String,
        val algorithm: KeyAlgorithm,
        val passphrase: CharArray,
        val expiryDays: Int = 0,
    )

    suspend operator fun invoke(params: Params): Result<PgpKey> {
        val existing = keyRepository.getAllKeys().first()
        if (existing.any { it.isSecret }) {
            return Result.failure(IllegalStateException("Own key pair already exists"))
        }

        val materialResult = pgpKeyGenerator.generate(
            ownerName = params.ownerName,
            ownerEmail = params.ownerEmail,
            algorithm = params.algorithm,
            passphrase = params.passphrase,
            expiryDays = params.expiryDays,
        )

        val material = materialResult.getOrElse { return Result.failure(it) }

        val expiresAt = if (params.expiryDays > 0) {
            material.createdAt + params.expiryDays * 24 * 60 * 60 * 1000L
        } else {
            null
        }

        val pgpKey = PgpKey(
            id = material.keyId,
            fingerprint = material.fingerprint,
            ownerName = params.ownerName,
            ownerEmail = params.ownerEmail,
            algorithm = params.algorithm,
            createdAt = material.createdAt,
            expiresAt = expiresAt,
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
