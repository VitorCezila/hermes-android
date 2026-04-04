package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.GeneratedKeyMaterial
import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.repository.KeyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateKeyPairUseCaseTest {

    private val keyRepository: KeyRepository = mockk()
    private val pgpKeyGenerator: PgpKeyGenerator = mockk()
    private val useCase = GenerateKeyPairUseCase(keyRepository, pgpKeyGenerator)

    private val validParams = GenerateKeyPairUseCase.Params(
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        passphrase = "correct-horse".toCharArray(),
    )

    private val fakeMaterial = GeneratedKeyMaterial(
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
        armoredPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----",
        fingerprint = "AABBCCDDEEFF00112233445566778899AABBCCDD",
        keyId = "0xAABBCCDDEEFF0011",
        createdAt = 1_700_000_000_000L,
    )

    @Test
    fun invoke_happyPath_returnsPgpKeyWithCorrectFields() = runTest {
        coEvery { pgpKeyGenerator.generate(any(), any(), any(), any()) } returns Result.success(fakeMaterial)
        coEvery { keyRepository.saveKeyPair(any(), any(), any()) } returns Result.success(Unit)

        val result = useCase(validParams)

        assertTrue(result.isSuccess)
        val key = result.getOrThrow()
        assertEquals(fakeMaterial.keyId, key.id)
        assertEquals(fakeMaterial.fingerprint, key.fingerprint)
        assertEquals("Alice", key.ownerName)
        assertEquals("alice@example.com", key.ownerEmail)
        assertEquals(KeyAlgorithm.ED25519, key.algorithm)
        assertEquals(fakeMaterial.createdAt, key.createdAt)
        assertNull(key.expiresAt)
        assertTrue(key.isSecret)
        assertEquals(fakeMaterial.armoredPublicKey, key.armoredPublicKey)
    }

    @Test
    fun invoke_generatorFails_returnsFailureWithoutCallingRepository() = runTest {
        coEvery { pgpKeyGenerator.generate(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("BC error"))

        val result = useCase(validParams)

        assertTrue(result.isFailure)
        assertEquals("BC error", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { keyRepository.saveKeyPair(any(), any(), any()) }
    }

    @Test
    fun invoke_repositorySaveFails_returnsFailure() = runTest {
        coEvery { pgpKeyGenerator.generate(any(), any(), any(), any()) } returns Result.success(fakeMaterial)
        coEvery { keyRepository.saveKeyPair(any(), any(), any()) } returns
            Result.failure(RuntimeException("DB full"))

        val result = useCase(validParams)

        assertTrue(result.isFailure)
        assertEquals("DB full", result.exceptionOrNull()?.message)
    }

    @Test
    fun invoke_passesCorrectParamsToGenerator() = runTest {
        coEvery { pgpKeyGenerator.generate(any(), any(), any(), any()) } returns Result.success(fakeMaterial)
        coEvery { keyRepository.saveKeyPair(any(), any(), any()) } returns Result.success(Unit)

        useCase(validParams)

        coVerify {
            pgpKeyGenerator.generate(
                ownerName = "Alice",
                ownerEmail = "alice@example.com",
                algorithm = KeyAlgorithm.ED25519,
                passphrase = validParams.passphrase,
            )
        }
    }

    @Test
    fun invoke_passesArmoredPrivateKeyToRepository() = runTest {
        coEvery { pgpKeyGenerator.generate(any(), any(), any(), any()) } returns Result.success(fakeMaterial)
        coEvery { keyRepository.saveKeyPair(any(), any(), any()) } returns Result.success(Unit)

        useCase(validParams)

        coVerify {
            keyRepository.saveKeyPair(
                pgpKey = any(),
                armoredPrivateKey = fakeMaterial.armoredPrivateKey,
                passphrase = validParams.passphrase,
            )
        }
    }

    @Test
    fun invoke_withRsaAlgorithm_resultHasRsaAlgorithm() = runTest {
        val rsaParams = validParams.copy(algorithm = KeyAlgorithm.RSA_4096)
        coEvery { pgpKeyGenerator.generate(any(), any(), any(), any()) } returns Result.success(fakeMaterial)
        coEvery { keyRepository.saveKeyPair(any(), any(), any()) } returns Result.success(Unit)

        val result = useCase(rsaParams)

        assertEquals(KeyAlgorithm.RSA_4096, result.getOrThrow().algorithm)
    }
}
