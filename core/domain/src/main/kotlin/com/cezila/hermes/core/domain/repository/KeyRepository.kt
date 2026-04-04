package com.cezila.hermes.core.domain.repository

import com.cezila.hermes.core.domain.model.PgpKey
import kotlinx.coroutines.flow.Flow

interface KeyRepository {
    suspend fun saveKeyPair(
        pgpKey: PgpKey,
        armoredPrivateKey: String,
        passphrase: CharArray,
    ): Result<Unit>

    suspend fun importPublicKey(pgpKey: PgpKey): Result<Unit>

    fun getAllKeys(): Flow<List<PgpKey>>

    suspend fun getKeyById(id: String): Result<PgpKey?>

    suspend fun deleteKey(id: String): Result<Unit>
}
