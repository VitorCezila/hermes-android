package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpEncryptor
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptAndSignUseCaseTest {

    private val keyRepository: KeyRepository = mockk()
    private val pgpEncryptor: PgpEncryptor = mockk()
    private val useCase = EncryptAndSignUseCase(keyRepository, pgpEncryptor)

    private val recipientKey = PgpKey(
        id = "0xAABBCCDD",
        fingerprint = "AABBCCDD",
        ownerName = "Bob",
        ownerEmail = "bob@example.com",
        algorithm = KeyAlgorithm.RSA_4096,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = false,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
    )

    private val signerKeyId = "0xEEFF0011"
    private val armoredPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----"
    private val plaintext = "Hello, Bob!"
    private val passphrase = "s3cr3t".toCharArray()
    private val ciphertext = "-----BEGIN PGP MESSAGE-----\nencrypted\n-----END PGP MESSAGE-----"

    @Test
    fun invoke_success_returnsEncryptedCiphertext() = runTest {
        coEvery { keyRepository.getArmoredPrivateKey(signerKeyId) } returns Result.success(armoredPrivateKey)
        coEvery {
            pgpEncryptor.encryptAndSign(plaintext, recipientKey.armoredPublicKey, armoredPrivateKey, passphrase)
        } returns Result.success(ciphertext)

        val result = useCase(
            EncryptAndSignUseCase.Params(
                recipientKey = recipientKey,
                plaintext = plaintext,
                signerKeyId = signerKeyId,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(ciphertext, result.getOrThrow())
    }

    @Test
    fun invoke_whenPrivateKeyNotFound_propagatesFailure() = runTest {
        val error = IllegalArgumentException("Key not found")
        coEvery { keyRepository.getArmoredPrivateKey(signerKeyId) } returns Result.failure(error)

        val result = useCase(
            EncryptAndSignUseCase.Params(
                recipientKey = recipientKey,
                plaintext = plaintext,
                signerKeyId = signerKeyId,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        coVerify(exactly = 0) { pgpEncryptor.encryptAndSign(any(), any(), any(), any()) }
    }

    @Test
    fun invoke_whenEncryptorFails_propagatesFailure() = runTest {
        val error = RuntimeException("Wrong passphrase")
        coEvery { keyRepository.getArmoredPrivateKey(signerKeyId) } returns Result.success(armoredPrivateKey)
        coEvery {
            pgpEncryptor.encryptAndSign(any(), any(), any(), any())
        } returns Result.failure(error)

        val result = useCase(
            EncryptAndSignUseCase.Params(
                recipientKey = recipientKey,
                plaintext = plaintext,
                signerKeyId = signerKeyId,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_passesCorrectArgsToEncryptor() = runTest {
        val passphraseSlot = slot<CharArray>()
        coEvery { keyRepository.getArmoredPrivateKey(signerKeyId) } returns Result.success(armoredPrivateKey)
        coEvery {
            pgpEncryptor.encryptAndSign(any(), any(), any(), capture(passphraseSlot))
        } returns Result.success(ciphertext)

        useCase(
            EncryptAndSignUseCase.Params(
                recipientKey = recipientKey,
                plaintext = plaintext,
                signerKeyId = signerKeyId,
                passphrase = passphrase,
            )
        )

        coVerify {
            pgpEncryptor.encryptAndSign(
                plaintext = plaintext,
                recipientArmoredPublicKey = recipientKey.armoredPublicKey,
                signerArmoredPrivateKey = armoredPrivateKey,
                passphrase = any(),
            )
        }
    }
}
