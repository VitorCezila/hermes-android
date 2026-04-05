package com.cezila.hermes.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Suppress("ArrayInDataClass")
@Entity(tableName = "pgp_keys")
data class PgpKeyEntity(
    @PrimaryKey val id: String,
    val fingerprint: String,
    val ownerName: String,
    val ownerEmail: String,
    val algorithmName: String,
    val createdAt: Long,
    val expiresAt: Long?,
    val isSecret: Boolean,
    val armoredPublicKey: String,
    val encryptedPrivateKeyBlob: ByteArray?,
    val privateKeyIv: ByteArray?,
)
