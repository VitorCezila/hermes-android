package com.cezila.hermes.core.data.crypto

import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.SignatureStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BouncyCastlePgpDecryptorTest {

    private val generator = BouncyCastlePgpKeyGenerator()
    private val encryptor = BouncyCastlePgpEncryptor()
    private val decryptor = BouncyCastlePgpDecryptor()

    private val passphrase = "test-passphrase-123".toCharArray()
    private val plaintext = "Hello, this is a secret message!"

    // RSA_4096 is used because the single-key ring it produces has isEncryptionKey=true,
    // which is required by BouncyCastlePgpEncryptor.parseRecipientPublicKey.
    // (Ed25519/EdDSA keys return isEncryptionKey=false regardless of key flags.)
    private suspend fun generateKeyMaterial(name: String, email: String) =
        generator.generate(name, email, KeyAlgorithm.RSA_4096, passphrase).getOrThrow()

    // --- Round-trip: text ---

    @Test
    fun decryptAndVerify_roundTrip_returnsOriginalPlaintext() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")

        val ciphertext = encryptor.encryptAndSign(
            plaintext = plaintext,
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptAndVerify(
            ciphertext = ciphertext,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = listOf(bob.armoredPublicKey),
        ).getOrThrow()

        assertEquals(plaintext, result.plaintext)
        assertNull(result.plaintextBytes)
        assertNull(result.fileName)
    }

    @Test
    fun decryptAndVerify_signerInKnownKeys_returnsValidStatus() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")

        val ciphertext = encryptor.encryptAndSign(
            plaintext = plaintext,
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptAndVerify(
            ciphertext = ciphertext,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = listOf(bob.armoredPublicKey),
        ).getOrThrow()

        assertTrue(result.signatureStatus is SignatureStatus.Valid)
    }

    @Test
    fun decryptAndVerify_signerNotInKnownKeys_returnsUnknownSigner() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")

        val ciphertext = encryptor.encryptAndSign(
            plaintext = plaintext,
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptAndVerify(
            ciphertext = ciphertext,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = emptyList(),
        ).getOrThrow()

        assertTrue(result.signatureStatus is SignatureStatus.UnknownSigner)
    }

    @Test
    fun decryptAndVerify_wrongPassphrase_returnsFailure() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")

        val ciphertext = encryptor.encryptAndSign(
            plaintext = plaintext,
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptAndVerify(
            ciphertext = ciphertext,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = "wrongpassword".toCharArray(),
            knownPublicKeys = emptyList(),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun decryptAndVerify_corruptedCiphertext_returnsFailure() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")

        val result = decryptor.decryptAndVerify(
            ciphertext = "not a pgp message",
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = emptyList(),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun decryptAndVerify_messageEncryptedForOtherKey_returnsFailure() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")
        val carol = generateKeyMaterial("Carol", "carol@example.com")

        // Encrypt for Bob, try to decrypt with Alice
        val ciphertext = encryptor.encryptAndSign(
            plaintext = plaintext,
            recipientArmoredPublicKey = bob.armoredPublicKey,
            signerArmoredPrivateKey = carol.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptAndVerify(
            ciphertext = ciphertext,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = emptyList(),
        )

        assertTrue(result.isFailure)
    }

    // --- Round-trip: file ---

    @Test
    fun decryptFileAndVerify_roundTrip_returnsOriginalBytes() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")
        val fileBytes = "file content goes here".toByteArray()
        val fileName = "document.txt"

        val encrypted = encryptor.encryptFileAndSign(
            bytes = fileBytes,
            fileName = fileName,
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptFileAndVerify(
            ciphertextBytes = encrypted,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = listOf(bob.armoredPublicKey),
        ).getOrThrow()

        assertNotNull(result.plaintextBytes)
        assertTrue(result.plaintextBytes!!.contentEquals(fileBytes))
        assertEquals(fileName, result.fileName)
        assertTrue(result.plaintext.isEmpty())
    }

    @Test
    fun decryptFileAndVerify_signerInKnownKeys_returnsValidStatus() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")
        val fileBytes = "binary data".toByteArray()

        val encrypted = encryptor.encryptFileAndSign(
            bytes = fileBytes,
            fileName = "data.bin",
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptFileAndVerify(
            ciphertextBytes = encrypted,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = passphrase,
            knownPublicKeys = listOf(bob.armoredPublicKey),
        ).getOrThrow()

        assertTrue(result.signatureStatus is SignatureStatus.Valid)
    }

    @Test
    fun decryptFileAndVerify_wrongPassphrase_returnsFailure() = runTest {
        val alice = generateKeyMaterial("Alice", "alice@example.com")
        val bob = generateKeyMaterial("Bob", "bob@example.com")

        val encrypted = encryptor.encryptFileAndSign(
            bytes = "data".toByteArray(),
            fileName = "file.bin",
            recipientArmoredPublicKey = alice.armoredPublicKey,
            signerArmoredPrivateKey = bob.armoredPrivateKey,
            passphrase = passphrase,
        ).getOrThrow()

        val result = decryptor.decryptFileAndVerify(
            ciphertextBytes = encrypted,
            recipientArmoredPrivateKey = alice.armoredPrivateKey,
            passphrase = "wrongpassword".toCharArray(),
            knownPublicKeys = emptyList(),
        )

        assertTrue(result.isFailure)
    }
}
