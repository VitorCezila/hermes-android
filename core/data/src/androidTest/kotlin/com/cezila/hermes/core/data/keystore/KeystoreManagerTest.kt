package com.cezila.hermes.core.data.keystore

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class KeystoreManagerTest {

    private val manager = KeystoreManager()
    private val testKeyId = "test_key_${System.currentTimeMillis()}"

    @After
    fun cleanup() {
        runCatching { manager.deleteKey(testKeyId) }
    }

    @Test
    fun getOrCreateKey_returnsNonNull() {
        val key = manager.getOrCreateKey(testKeyId)
        assertNotNull(key)
    }

    @Test
    fun getOrCreateKey_idempotent_doesNotThrowOnSecondCall() {
        manager.getOrCreateKey(testKeyId)
        manager.getOrCreateKey(testKeyId)
    }

    @Test
    fun encrypt_producesNonEmptyOutputAndIvOf12Bytes() {
        val plaintext = "hello world".toByteArray()
        val (ciphertext, iv) = manager.encrypt(plaintext, testKeyId)

        assertTrue(ciphertext.isNotEmpty())
        assertEquals(12, iv.size)
    }

    @Test
    fun encrypt_differentPlaintexts_produceDifferentCiphertexts() {
        val (ciphertext1, _) = manager.encrypt("hello".toByteArray(), testKeyId)
        val (ciphertext2, _) = manager.encrypt("world".toByteArray(), testKeyId)

        assertFalse(ciphertext1.contentEquals(ciphertext2))
    }

    @Test
    fun decrypt_afterEncrypt_returnsOriginalPlaintext() {
        val plaintext = "secret private key material".toByteArray(Charsets.UTF_8)
        val (ciphertext, iv) = manager.encrypt(plaintext, testKeyId)
        val decrypted = manager.decrypt(ciphertext, iv, testKeyId)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test(expected = AEADBadTagException::class)
    fun decrypt_withTamperedCiphertext_throwsAEADBadTagException() {
        val plaintext = "sensitive data".toByteArray()
        val (ciphertext, iv) = manager.encrypt(plaintext, testKeyId)

        val tampered = ciphertext.copyOf()
        tampered[tampered.size / 2] = (tampered[tampered.size / 2].toInt() xor 0xFF).toByte()

        manager.decrypt(tampered, iv, testKeyId)
    }

    @Test(expected = AEADBadTagException::class)
    fun decrypt_withWrongIv_throwsAEADBadTagException() {
        val plaintext = "sensitive data".toByteArray()
        val (ciphertext, _) = manager.encrypt(plaintext, testKeyId)

        val wrongIv = ByteArray(12) { 0x00 }
        manager.decrypt(ciphertext, wrongIv, testKeyId)
    }

    @Test
    fun deleteKey_removesKeyFromKeystore() {
        manager.getOrCreateKey(testKeyId)
        manager.deleteKey(testKeyId)

        // After deletion, getOrCreateKey recreates it without throwing
        val newKey = manager.getOrCreateKey(testKeyId)
        assertNotNull(newKey)
    }

    @Test
    fun deleteKey_whenKeyDidNotExist_doesNotThrow() {
        val neverCreatedId = "never_created_${System.currentTimeMillis()}"
        manager.deleteKey(neverCreatedId)
    }
}
