package com.cezila.hermes.core.domain.crypto

import com.cezila.hermes.core.domain.model.KeyAlgorithm

data class ParsedPublicKeyInfo(
    val fingerprint: String,
    val keyId: String,
    val algorithm: KeyAlgorithm,
    val ownerName: String,
    val ownerEmail: String,
    val createdAt: Long,
    val expiresAt: Long?,
)

interface PgpKeyParser {
    suspend fun parsePublicKey(armoredKey: String): Result<ParsedPublicKeyInfo>
}
