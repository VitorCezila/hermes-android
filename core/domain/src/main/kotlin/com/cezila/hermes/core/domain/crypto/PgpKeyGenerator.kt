package com.cezila.hermes.core.domain.crypto

import com.cezila.hermes.core.domain.model.KeyAlgorithm

data class GeneratedKeyMaterial(
    val armoredPublicKey: String,
    val armoredPrivateKey: String,
    val fingerprint: String,
    val keyId: String,
    val createdAt: Long,
)

interface PgpKeyGenerator {
    suspend fun generate(
        ownerName: String,
        ownerEmail: String,
        algorithm: KeyAlgorithm,
        passphrase: CharArray,
    ): Result<GeneratedKeyMaterial>
}
