package com.cezila.hermes.core.data.repository

import com.cezila.hermes.core.data.db.dao.PgpKeyDao
import com.cezila.hermes.core.data.db.entity.PgpKeyEntity
import com.cezila.hermes.core.data.db.mapper.toDomain
import com.cezila.hermes.core.data.di.IoDispatcher
import com.cezila.hermes.core.data.keystore.KeystoreManager
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Arrays
import javax.inject.Inject

class KeyRepositoryImpl @Inject constructor(
    private val dao: PgpKeyDao,
    private val keystoreManager: KeystoreManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : KeyRepository {

    override suspend fun saveKeyPair(
        pgpKey: PgpKey,
        armoredPrivateKey: String,
        passphrase: CharArray,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val privateBytes = armoredPrivateKey.toByteArray(Charsets.UTF_8)
            val (ciphertext, iv) = keystoreManager.encrypt(privateBytes, pgpKey.id)
            Arrays.fill(privateBytes, 0)

            val entity = PgpKeyEntity(
                id = pgpKey.id,
                fingerprint = pgpKey.fingerprint,
                ownerName = pgpKey.ownerName,
                ownerEmail = pgpKey.ownerEmail,
                algorithmName = pgpKey.algorithm.name,
                createdAt = pgpKey.createdAt,
                expiresAt = pgpKey.expiresAt,
                isSecret = pgpKey.isSecret,
                armoredPublicKey = pgpKey.armoredPublicKey,
                encryptedPrivateKeyBlob = ciphertext,
                privateKeyIv = iv,
            )
            dao.insert(entity)
        }
    }

    override fun getAllKeys(): Flow<List<PgpKey>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getKeyById(id: String): Result<PgpKey?> = withContext(ioDispatcher) {
        runCatching { dao.findById(id)?.toDomain() }
    }

    override suspend fun importPublicKey(pgpKey: PgpKey): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val entity = PgpKeyEntity(
                id = pgpKey.id,
                fingerprint = pgpKey.fingerprint,
                ownerName = pgpKey.ownerName,
                ownerEmail = pgpKey.ownerEmail,
                algorithmName = pgpKey.algorithm.name,
                createdAt = pgpKey.createdAt,
                expiresAt = pgpKey.expiresAt,
                isSecret = false,
                armoredPublicKey = pgpKey.armoredPublicKey,
                encryptedPrivateKeyBlob = null,
                privateKeyIv = null,
            )
            dao.insert(entity)
        }
    }

    override suspend fun deleteKey(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            dao.deleteById(id)
            keystoreManager.deleteKey(id)
        }
    }

    override suspend fun clearAllKeys(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val secretKeyIds = dao.getAllSecretKeyIds()
            secretKeyIds.forEach { id -> keystoreManager.deleteKey(id) }
            dao.deleteAll()
        }
    }

    override suspend fun getArmoredPrivateKey(id: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val entity = dao.findById(id)
                ?: throw IllegalArgumentException("Key not found: $id")
            val blob = entity.encryptedPrivateKeyBlob
                ?: throw IllegalStateException("Key has no private key material")
            val iv = entity.privateKeyIv
                ?: throw IllegalStateException("Key has no IV for private key")
            val decryptedBytes = keystoreManager.decrypt(blob, iv, id)
            String(decryptedBytes, Charsets.UTF_8)
        }
    }
}
