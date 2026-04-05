package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.crypto.PgpDecryptor
import com.cezila.hermes.core.domain.model.DecryptionResult
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.model.SignatureStatus
import com.cezila.hermes.core.domain.repository.KeyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecryptAndVerifyUseCaseTest {

    private val keyRepository: KeyRepository = mockk()
    private val pgpDecryptor: PgpDecryptor = mockk()
    private val useCase = DecryptAndVerifyUseCase(keyRepository, pgpDecryptor)

    private val ownKey = PgpKey(
        id = "0xMYKEY",
        fingerprint = "AABBCCDDEEFF0011",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\nAlice\n-----END PGP PUBLIC KEY BLOCK-----",
    )

    private val contactKey = PgpKey(
        id = "0xBOBKEY",
        fingerprint = "1122334455667788",
        ownerName = "Bob",
        ownerEmail = "bob@example.com",
        algorithm = KeyAlgorithm.RSA_4096,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = false,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\nBob\n-----END PGP PUBLIC KEY BLOCK-----",
    )

    private val armoredPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n-----END PGP PRIVATE KEY BLOCK-----"
    private val ciphertext = "-----BEGIN PGP MESSAGE-----\nencrypted\n-----END PGP MESSAGE-----"
    private val passphrase = "s3cr3t".toCharArray()

    private val successResult = DecryptionResult(
        plaintext = "Hello, Alice!",
        plaintextBytes = null,
        fileName = null,
        signatureStatus = SignatureStatus.None,
    )

    @Test
    fun invoke_textMode_success_returnsDecryptionResult() = runTest {
        coEvery { keyRepository.getArmoredPrivateKey(ownKey.id) } returns Result.success(armoredPrivateKey)
        every { keyRepository.getAllKeys() } returns flowOf(listOf(ownKey, contactKey))
        coEvery {
            pgpDecryptor.decryptAndVerify(ciphertext, armoredPrivateKey, passphrase, any())
        } returns Result.success(successResult)

        val result = useCase(
            DecryptAndVerifyUseCase.Params(
                ciphertext = ciphertext,
                fileBytes = null,
                recipientKeyId = ownKey.id,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(successResult, result.getOrThrow())
    }

    @Test
    fun invoke_fileMode_callsDecryptFileAndVerify() = runTest {
        val fileBytes = "encrypted content".toByteArray()
        val fileResult = DecryptionResult(
            plaintext = "",
            plaintextBytes = "plaintext".toByteArray(),
            fileName = "document.pdf",
            signatureStatus = SignatureStatus.None,
        )
        coEvery { keyRepository.getArmoredPrivateKey(ownKey.id) } returns Result.success(armoredPrivateKey)
        every { keyRepository.getAllKeys() } returns flowOf(listOf(ownKey))
        coEvery {
            pgpDecryptor.decryptFileAndVerify(fileBytes, armoredPrivateKey, passphrase, any())
        } returns Result.success(fileResult)

        val result = useCase(
            DecryptAndVerifyUseCase.Params(
                ciphertext = "",
                fileBytes = fileBytes,
                recipientKeyId = ownKey.id,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { pgpDecryptor.decryptFileAndVerify(any(), any(), any(), any()) }
        coVerify(exactly = 0) { pgpDecryptor.decryptAndVerify(any(), any(), any(), any()) }
    }

    @Test
    fun invoke_textMode_callsDecryptAndVerify_notFile() = runTest {
        coEvery { keyRepository.getArmoredPrivateKey(ownKey.id) } returns Result.success(armoredPrivateKey)
        every { keyRepository.getAllKeys() } returns flowOf(listOf(ownKey))
        coEvery {
            pgpDecryptor.decryptAndVerify(ciphertext, armoredPrivateKey, passphrase, any())
        } returns Result.success(successResult)

        useCase(
            DecryptAndVerifyUseCase.Params(
                ciphertext = ciphertext,
                fileBytes = null,
                recipientKeyId = ownKey.id,
                passphrase = passphrase,
            )
        )

        coVerify(exactly = 1) { pgpDecryptor.decryptAndVerify(any(), any(), any(), any()) }
        coVerify(exactly = 0) { pgpDecryptor.decryptFileAndVerify(any(), any(), any(), any()) }
    }

    @Test
    fun invoke_whenPrivateKeyNotFound_propagatesFailureWithoutCallingDecryptor() = runTest {
        val error = IllegalArgumentException("Key not found")
        coEvery { keyRepository.getArmoredPrivateKey(ownKey.id) } returns Result.failure(error)

        val result = useCase(
            DecryptAndVerifyUseCase.Params(
                ciphertext = ciphertext,
                fileBytes = null,
                recipientKeyId = ownKey.id,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        coVerify(exactly = 0) { pgpDecryptor.decryptAndVerify(any(), any(), any(), any()) }
    }

    @Test
    fun invoke_whenDecryptorFails_propagatesFailure() = runTest {
        val error = RuntimeException("Wrong passphrase")
        coEvery { keyRepository.getArmoredPrivateKey(ownKey.id) } returns Result.success(armoredPrivateKey)
        every { keyRepository.getAllKeys() } returns flowOf(listOf(ownKey))
        coEvery { pgpDecryptor.decryptAndVerify(any(), any(), any(), any()) } returns Result.failure(error)

        val result = useCase(
            DecryptAndVerifyUseCase.Params(
                ciphertext = ciphertext,
                fileBytes = null,
                recipientKeyId = ownKey.id,
                passphrase = passphrase,
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_passesAllKnownPublicKeysToDecryptor() = runTest {
        val knownKeysSlot = slot<List<String>>()
        coEvery { keyRepository.getArmoredPrivateKey(ownKey.id) } returns Result.success(armoredPrivateKey)
        every { keyRepository.getAllKeys() } returns flowOf(listOf(ownKey, contactKey))
        coEvery {
            pgpDecryptor.decryptAndVerify(any(), any(), any(), capture(knownKeysSlot))
        } returns Result.success(successResult)

        useCase(
            DecryptAndVerifyUseCase.Params(
                ciphertext = ciphertext,
                fileBytes = null,
                recipientKeyId = ownKey.id,
                passphrase = passphrase,
            )
        )

        val captured = knownKeysSlot.captured
        assertTrue(captured.contains(ownKey.armoredPublicKey))
        assertTrue(captured.contains(contactKey.armoredPublicKey))
        assertEquals(2, captured.size)
    }
}
