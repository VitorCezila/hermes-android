package com.cezila.hermes.core.domain.crypto

interface PgpEncryptor {
    suspend fun encryptAndSign(
        plaintext: String,
        recipientArmoredPublicKey: String,
        signerArmoredPrivateKey: String,
        passphrase: CharArray,
    ): Result<String>

    suspend fun encryptFileAndSign(
        bytes: ByteArray,
        fileName: String,
        recipientArmoredPublicKey: String,
        signerArmoredPrivateKey: String,
        passphrase: CharArray,
    ): Result<ByteArray>
}
