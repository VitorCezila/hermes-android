package com.cezila.hermes.core.data.crypto

import com.cezila.hermes.core.domain.model.KeyAlgorithm
import kotlinx.coroutines.test.runTest
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BouncyCastlePgpKeyGeneratorTest {

    private val generator = BouncyCastlePgpKeyGenerator()
    private val passphrase = "test-passphrase-123".toCharArray()

    // --- Ed25519 tests (fast) ---

    @Test
    fun generate_ed25519_returnsSuccess() = runTest {
        val result = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase)
        assertTrue(result.isSuccess)
    }

    @Test
    fun generate_ed25519_publicKeyHasPgpHeader() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        assertTrue(material.armoredPublicKey.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----"))
    }

    @Test
    fun generate_ed25519_privateKeyHasPgpHeader() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        assertTrue(material.armoredPrivateKey.startsWith("-----BEGIN PGP PRIVATE KEY BLOCK-----"))
    }

    @Test
    fun generate_ed25519_fingerprintIsNonEmptyHex() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        assertTrue(material.fingerprint.isNotEmpty())
        assertTrue(material.fingerprint == material.fingerprint.uppercase())
        assertTrue(material.fingerprint.all { it.isDigit() || it in 'A'..'F' })
    }

    @Test
    fun generate_ed25519_keyIdStartsWith0x() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        assertTrue(material.keyId.startsWith("0x"))
    }

    @Test
    fun generate_ed25519_createdAtIsReasonableTimestamp() = runTest {
        val before = System.currentTimeMillis()
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        val after = System.currentTimeMillis()
        assertTrue(material.createdAt in before..after)
    }

    @Test
    fun generate_ed25519_consecutiveCalls_produceDifferentFingerprints() = runTest {
        val material1 = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        val material2 = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        assertNotEquals(material1.fingerprint, material2.fingerprint)
        assertNotEquals(material1.keyId, material2.keyId)
    }

    @Test
    fun generate_ed25519_withEmptyPassphrase_doesNotThrow() = runTest {
        val result = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, CharArray(0))
        assertTrue(result.isSuccess)
    }

    // --- Encryption capability tests ---

    @Test
    fun generate_ed25519_publicKeyRingContainsEncryptionCapableKey() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.ED25519, passphrase).getOrThrow()
        val inputStream = PGPUtil.getDecoderStream(material.armoredPublicKey.byteInputStream())
        val collection = PGPPublicKeyRingCollection(inputStream, JcaKeyFingerprintCalculator())
        val hasEncryptionKey = collection.keyRings.asSequence()
            .flatMap { it.publicKeys.asSequence() }
            .any { it.isEncryptionKey }
        assertTrue("Ed25519 key ring must contain at least one encryption-capable key", hasEncryptionKey)
    }

    @Test
    fun generate_rsa4096_publicKeyRingContainsEncryptionCapableKey() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.RSA_4096, passphrase).getOrThrow()
        val inputStream = PGPUtil.getDecoderStream(material.armoredPublicKey.byteInputStream())
        val collection = PGPPublicKeyRingCollection(inputStream, JcaKeyFingerprintCalculator())
        val hasEncryptionKey = collection.keyRings.asSequence()
            .flatMap { it.publicKeys.asSequence() }
            .any { it.isEncryptionKey }
        assertTrue("RSA-4096 key ring must contain at least one encryption-capable key", hasEncryptionKey)
    }

    // --- RSA-4096 tests (slow) ---

    @Test
    fun generate_rsa4096_returnsSuccess() = runTest {
        val result = generator.generate("Alice", "alice@example.com", KeyAlgorithm.RSA_4096, passphrase)
        assertTrue(result.isSuccess)
    }

    @Test
    fun generate_rsa4096_publicKeyHasPgpHeader() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.RSA_4096, passphrase).getOrThrow()
        assertTrue(material.armoredPublicKey.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----"))
    }

    @Test
    fun generate_rsa4096_fingerprintIsNonEmptyHex() = runTest {
        val material = generator.generate("Alice", "alice@example.com", KeyAlgorithm.RSA_4096, passphrase).getOrThrow()
        assertTrue(material.fingerprint.isNotEmpty())
        assertTrue(material.fingerprint.all { it.isDigit() || it in 'A'..'F' })
    }
}
