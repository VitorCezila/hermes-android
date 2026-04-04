package com.cezila.hermes.core.data.db.mapper

import com.cezila.hermes.core.data.db.entity.PgpKeyEntity
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey

fun PgpKeyEntity.toDomain(): PgpKey = PgpKey(
    id = id,
    fingerprint = fingerprint,
    ownerName = ownerName,
    ownerEmail = ownerEmail,
    algorithm = KeyAlgorithm.valueOf(algorithmName),
    createdAt = createdAt,
    expiresAt = expiresAt,
    isSecret = isSecret,
    armoredPublicKey = armoredPublicKey,
)
