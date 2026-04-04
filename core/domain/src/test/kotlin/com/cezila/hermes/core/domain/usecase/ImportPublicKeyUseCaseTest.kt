package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.ParsedPublicKeyInfo
import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.repository.KeyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportPublicKeyUseCaseTest {

    private val keyRepository: KeyRepository = mockk()
    private val pgpKeyParser: PgpKeyParser = mockk()
    private val useCase = ImportPublicKeyUseCase(keyRepository, pgpKeyParser)

    private val armoredKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\nFAKE\n-----END PGP PUBLIC KEY BLOCK-----"

    private val parsedInfo = ParsedPublicKeyInfo(
        fingerprint = "AABBCCDDEEFF00112233445566778899AABBCCDD",
        keyId = "0xAABBCCDDEEFF0011",
        algorithm = KeyAlgorithm.ED25519,
        ownerName = "Bob",
        ownerEmail = "bob@example.com",
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
    )

    @Test
    fun invoke_happyPath_returnsPgpKeyWithIsSecretFalse() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(armoredKey) } returns Result.success(parsedInfo)
        coEvery { keyRepository.importPublicKey(any()) } returns Result.success(Unit)

        val result = useCase(ImportPublicKeyUseCase.Params(armoredKey))

        assertTrue(result.isSuccess)
        val key = result.getOrThrow()
        assertFalse(key.isSecret)
        assertEquals(parsedInfo.fingerprint, key.fingerprint)
        assertEquals(parsedInfo.keyId, key.id)
        assertEquals("Bob", key.ownerName)
        assertEquals("bob@example.com", key.ownerEmail)
        assertEquals(KeyAlgorithm.ED25519, key.algorithm)
        assertEquals(armoredKey, key.armoredPublicKey)
    }

    @Test
    fun invoke_parserFails_returnsFailureWithoutCallingRepository() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns
            Result.failure(IllegalArgumentException("Malformed armored key"))

        val result = useCase(ImportPublicKeyUseCase.Params(armoredKey))

        assertTrue(result.isFailure)
        assertEquals("Malformed armored key", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { keyRepository.importPublicKey(any()) }
    }

    @Test
    fun invoke_repositoryFails_returnsFailure() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(armoredKey) } returns Result.success(parsedInfo)
        coEvery { keyRepository.importPublicKey(any()) } returns
            Result.failure(RuntimeException("Duplicate key"))

        val result = useCase(ImportPublicKeyUseCase.Params(armoredKey))

        assertTrue(result.isFailure)
        assertEquals("Duplicate key", result.exceptionOrNull()?.message)
    }

    @Test
    fun invoke_passesArmoredKeyToParser() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(armoredKey) } returns Result.success(parsedInfo)
        coEvery { keyRepository.importPublicKey(any()) } returns Result.success(Unit)

        useCase(ImportPublicKeyUseCase.Params(armoredKey))

        coVerify { pgpKeyParser.parsePublicKey(armoredKey) }
    }

    @Test
    fun invoke_keyWithExpiry_preservesExpiresAt() = runTest {
        val expiresAt = 1_800_000_000_000L
        val parsedWithExpiry = parsedInfo.copy(expiresAt = expiresAt)
        coEvery { pgpKeyParser.parsePublicKey(armoredKey) } returns Result.success(parsedWithExpiry)
        coEvery { keyRepository.importPublicKey(any()) } returns Result.success(Unit)

        val result = useCase(ImportPublicKeyUseCase.Params(armoredKey))

        assertEquals(expiresAt, result.getOrThrow().expiresAt)
    }
}
