package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpEncryptor
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository

class EncryptFileAndSignUseCase(
    private val keyRepository: KeyRepository,
    private val pgpEncryptor: PgpEncryptor,
) {
    data class Params(
        val recipientKey: PgpKey,
        val fileBytes: ByteArray,
        val fileName: String,
        val signerKeyId: String,
        val passphrase: CharArray,
    )

    suspend operator fun invoke(params: Params): Result<ByteArray> {
        val armoredPrivateKey = keyRepository.getArmoredPrivateKey(params.signerKeyId)
            .getOrElse { return Result.failure(it) }

        return pgpEncryptor.encryptFileAndSign(
            bytes = params.fileBytes,
            fileName = params.fileName,
            recipientArmoredPublicKey = params.recipientKey.armoredPublicKey,
            signerArmoredPrivateKey = armoredPrivateKey,
            passphrase = params.passphrase,
        )
    }
}
