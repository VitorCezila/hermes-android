package com.cezila.hermes.core.domain.crypto

import com.cezila.hermes.core.domain.model.DecryptionResult

interface PgpDecryptor {
    suspend fun decryptAndVerify(
        ciphertext: String,
        recipientArmoredPrivateKey: String,
        passphrase: CharArray,
        knownPublicKeys: List<String>,
    ): Result<DecryptionResult>

    suspend fun decryptFileAndVerify(
        ciphertextBytes: ByteArray,
        recipientArmoredPrivateKey: String,
        passphrase: CharArray,
        knownPublicKeys: List<String>,
    ): Result<DecryptionResult>
}
