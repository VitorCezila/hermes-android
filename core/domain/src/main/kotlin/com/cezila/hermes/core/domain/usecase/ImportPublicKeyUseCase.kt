package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository

class ImportPublicKeyUseCase(
    private val keyRepository: KeyRepository,
    private val pgpKeyParser: PgpKeyParser,
) {
    data class Params(val armoredPublicKey: String)

    suspend operator fun invoke(params: Params): Result<PgpKey> {
        val parsed = pgpKeyParser.parsePublicKey(params.armoredPublicKey)
            .getOrElse { return Result.failure(it) }

        val pgpKey = PgpKey(
            id = parsed.keyId,
            fingerprint = parsed.fingerprint,
            ownerName = parsed.ownerName,
            ownerEmail = parsed.ownerEmail,
            algorithm = parsed.algorithm,
            createdAt = parsed.createdAt,
            expiresAt = parsed.expiresAt,
            isSecret = false,
            armoredPublicKey = params.armoredPublicKey,
        )

        keyRepository.importPublicKey(pgpKey).getOrElse { return Result.failure(it) }
        return Result.success(pgpKey)
    }
}
