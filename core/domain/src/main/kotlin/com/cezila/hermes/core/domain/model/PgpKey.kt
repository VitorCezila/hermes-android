package com.cezila.hermes.core.domain.model

data class PgpKey(
    val id: String,
    val fingerprint: String,
    val ownerName: String,
    val ownerEmail: String,
    val algorithm: KeyAlgorithm,
    val createdAt: Long,
    val expiresAt: Long?,
    val isSecret: Boolean,
    val armoredPublicKey: String,
)
