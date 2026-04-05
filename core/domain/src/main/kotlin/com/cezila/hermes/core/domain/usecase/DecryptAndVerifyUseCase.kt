package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpDecryptor
import com.cezila.hermes.core.domain.model.DecryptionResult
import com.cezila.hermes.core.domain.repository.KeyRepository
import kotlinx.coroutines.flow.first

class DecryptAndVerifyUseCase(
    private val keyRepository: KeyRepository,
    private val pgpDecryptor: PgpDecryptor,
) {
    data class Params(
        val ciphertext: String,
        val fileBytes: ByteArray?,
        val recipientKeyId: String,
        val passphrase: CharArray,
    )

    suspend operator fun invoke(params: Params): Result<DecryptionResult> {
        val armoredPrivateKey = keyRepository.getArmoredPrivateKey(params.recipientKeyId)
            .getOrElse { return Result.failure(it) }

        val allKeys = keyRepository.getAllKeys().first()
        val knownPublicKeys = allKeys.map { it.armoredPublicKey }

        return if (params.fileBytes != null) {
            pgpDecryptor.decryptFileAndVerify(
                ciphertextBytes = params.fileBytes,
                recipientArmoredPrivateKey = armoredPrivateKey,
                passphrase = params.passphrase,
                knownPublicKeys = knownPublicKeys,
            )
        } else {
            pgpDecryptor.decryptAndVerify(
                ciphertext = params.ciphertext,
                recipientArmoredPrivateKey = armoredPrivateKey,
                passphrase = params.passphrase,
                knownPublicKeys = knownPublicKeys,
            )
        }
    }
}
