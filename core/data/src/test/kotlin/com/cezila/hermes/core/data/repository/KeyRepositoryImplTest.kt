package com.cezila.hermes.core.data.repository

import app.cash.turbine.test
import com.cezila.hermes.core.data.db.dao.PgpKeyDao
import com.cezila.hermes.core.data.db.entity.PgpKeyEntity
import com.cezila.hermes.core.data.keystore.KeystoreManager
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyRepositoryImplTest {

    private val dao: PgpKeyDao = mockk()
    private val keystoreManager: KeystoreManager = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = KeyRepositoryImpl(dao, keystoreManager, testDispatcher)

    private val fakePgpKey = PgpKey(
        id = "0xAABB",
        fingerprint = "AABB",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
    )
    private val fakeArmoredPrivate = "-----BEGIN PGP PRIVATE KEY BLOCK-----"
    private val fakePassphrase = "correct-horse".toCharArray()
    private val fakeCiphertext = ByteArray(32) { it.toByte() }
    private val fakeIv = ByteArray(12) { it.toByte() }

    private fun buildEntity(
        id: String = "0xAABB",
        algorithmName: String = "ED25519",
    ) = PgpKeyEntity(
        id = id,
        fingerprint = "AABB",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithmName = algorithmName,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
        encryptedPrivateKeyBlob = fakeCiphertext,
        privateKeyIv = fakeIv,
    )

    @Test
    fun saveKeyPair_encryptsWithCorrectKeyId() = runTest {
        every { keystoreManager.encrypt(any(), eq("0xAABB")) } returns Pair(fakeCiphertext, fakeIv)
        coEvery { dao.insert(any()) } just Runs

        repository.saveKeyPair(fakePgpKey, fakeArmoredPrivate, fakePassphrase)

        verify { keystoreManager.encrypt(any(), "0xAABB") }
    }

    @Test
    fun saveKeyPair_wipesPrivateBytesAfterEncrypt() = runTest {
        val capturedBytes = slot<ByteArray>()
        every { keystoreManager.encrypt(capture(capturedBytes), any()) } returns Pair(fakeCiphertext, fakeIv)
        coEvery { dao.insert(any()) } just Runs

        repository.saveKeyPair(fakePgpKey, fakeArmoredPrivate, fakePassphrase)

        assertTrue(capturedBytes.captured.all { it == 0.toByte() })
    }

    @Test
    fun saveKeyPair_insertsEntityWithCorrectFields() = runTest {
        val entitySlot = slot<PgpKeyEntity>()
        every { keystoreManager.encrypt(any(), any()) } returns Pair(fakeCiphertext, fakeIv)
        coEvery { dao.insert(capture(entitySlot)) } just Runs

        repository.saveKeyPair(fakePgpKey, fakeArmoredPrivate, fakePassphrase)

        assertEquals("ED25519", entitySlot.captured.algorithmName)
        assertTrue(entitySlot.captured.encryptedPrivateKeyBlob.contentEquals(fakeCiphertext))
        assertTrue(entitySlot.captured.privateKeyIv.contentEquals(fakeIv))
    }

    @Test
    fun saveKeyPair_whenDaoThrows_returnsFailure() = runTest {
        every { keystoreManager.encrypt(any(), any()) } returns Pair(fakeCiphertext, fakeIv)
        coEvery { dao.insert(any()) } throws RuntimeException("constraint violation")

        val result = repository.saveKeyPair(fakePgpKey, fakeArmoredPrivate, fakePassphrase)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun getAllKeys_mapsEntitiesToDomainCorrectly() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(buildEntity()))

        repository.getAllKeys().test {
            val keys = awaitItem()
            assertEquals(1, keys.size)
            assertEquals(KeyAlgorithm.ED25519, keys[0].algorithm)
            assertEquals("0xAABB", keys[0].id)
            awaitComplete()
        }
    }

    @Test
    fun getAllKeys_withEmptyList_emitsEmptyList() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())

        repository.getAllKeys().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun getKeyById_whenEntityExists_returnsMappedDomain() = runTest {
        coEvery { dao.findById("0xAABB") } returns buildEntity()

        val result = repository.getKeyById("0xAABB")

        assertTrue(result.isSuccess)
        assertEquals("0xAABB", result.getOrThrow()?.id)
        assertEquals(KeyAlgorithm.ED25519, result.getOrThrow()?.algorithm)
    }

    @Test
    fun getKeyById_whenEntityNotFound_returnsSuccessNull() = runTest {
        coEvery { dao.findById(any()) } returns null

        val result = repository.getKeyById("nonexistent")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun getKeyById_whenDaoThrows_returnsFailure() = runTest {
        coEvery { dao.findById(any()) } throws RuntimeException("IO error")

        val result = repository.getKeyById("0xAABB")

        assertTrue(result.isFailure)
    }

    @Test
    fun deleteKey_callsDaoAndKeystoreWithSameId() = runTest {
        coEvery { dao.deleteById("0xAABB") } returns 1
        every { keystoreManager.deleteKey("0xAABB") } just Runs

        repository.deleteKey("0xAABB")

        coVerify { dao.deleteById("0xAABB") }
        verify { keystoreManager.deleteKey("0xAABB") }
    }

    @Test
    fun deleteKey_whenDaoThrows_returnsFailureAndSkipsKeystoreDelete() = runTest {
        coEvery { dao.deleteById(any()) } throws RuntimeException("foreign key")

        val result = repository.deleteKey("0xAABB")

        assertTrue(result.isFailure)
        verify(exactly = 0) { keystoreManager.deleteKey(any()) }
    }
}
