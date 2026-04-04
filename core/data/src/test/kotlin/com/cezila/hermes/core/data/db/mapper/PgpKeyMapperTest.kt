package com.cezila.hermes.core.data.db.mapper

import com.cezila.hermes.core.data.db.entity.PgpKeyEntity
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PgpKeyMapperTest {

    private fun buildEntity(
        id: String = "0xAABB",
        fingerprint: String = "AABB",
        ownerName: String = "Alice",
        ownerEmail: String = "alice@example.com",
        algorithmName: String = "RSA_4096",
        createdAt: Long = 1_700_000_000_000L,
        expiresAt: Long? = null,
        isSecret: Boolean = true,
        armoredPublicKey: String = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
        encryptedPrivateKeyBlob: ByteArray = ByteArray(16),
        privateKeyIv: ByteArray = ByteArray(12),
    ) = PgpKeyEntity(
        id = id,
        fingerprint = fingerprint,
        ownerName = ownerName,
        ownerEmail = ownerEmail,
        algorithmName = algorithmName,
        createdAt = createdAt,
        expiresAt = expiresAt,
        isSecret = isSecret,
        armoredPublicKey = armoredPublicKey,
        encryptedPrivateKeyBlob = encryptedPrivateKeyBlob,
        privateKeyIv = privateKeyIv,
    )

    @Test
    fun toDomain_mapsAllScalarFieldsCorrectly() {
        val entity = buildEntity()
        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.fingerprint, domain.fingerprint)
        assertEquals(entity.ownerName, domain.ownerName)
        assertEquals(entity.ownerEmail, domain.ownerEmail)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.isSecret, domain.isSecret)
        assertEquals(entity.armoredPublicKey, domain.armoredPublicKey)
    }

    @Test
    fun toDomain_withRsa4096AlgorithmName_mapsToEnum() {
        val domain = buildEntity(algorithmName = "RSA_4096").toDomain()
        assertEquals(KeyAlgorithm.RSA_4096, domain.algorithm)
    }

    @Test
    fun toDomain_withEd25519AlgorithmName_mapsToEnum() {
        val domain = buildEntity(algorithmName = "ED25519").toDomain()
        assertEquals(KeyAlgorithm.ED25519, domain.algorithm)
    }

    @Test
    fun toDomain_withNullExpiresAt_returnsNull() {
        val domain = buildEntity(expiresAt = null).toDomain()
        assertNull(domain.expiresAt)
    }

    @Test
    fun toDomain_withNonNullExpiresAt_preservesTimestamp() {
        val domain = buildEntity(expiresAt = 1_800_000_000_000L).toDomain()
        assertEquals(1_800_000_000_000L, domain.expiresAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toDomain_withUnknownAlgorithmName_throwsIllegalArgumentException() {
        buildEntity(algorithmName = "UNKNOWN").toDomain()
    }
}
