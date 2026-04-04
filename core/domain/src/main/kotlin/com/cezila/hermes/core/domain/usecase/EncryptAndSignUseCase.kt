package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpEncryptor
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository

class EncryptAndSignUseCase(
    private val keyRepository: KeyRepository,
    private val pgpEncryptor: PgpEncryptor,
) {
    data class Params(
        val recipientKey: PgpKey,
        val plaintext: String,
        val signerKeyId: String,
        val passphrase: CharArray,
    )

    suspend operator fun invoke(params: Params): Result<String> {
        val armoredPrivateKey = keyRepository.getArmoredPrivateKey(params.signerKeyId)
            .getOrElse { return Result.failure(it) }

        return pgpEncryptor.encryptAndSign(
            plaintext = params.plaintext,
            recipientArmoredPublicKey = params.recipientKey.armoredPublicKey,
            signerArmoredPrivateKey = armoredPrivateKey,
            passphrase = params.passphrase,
        )
    }
}
