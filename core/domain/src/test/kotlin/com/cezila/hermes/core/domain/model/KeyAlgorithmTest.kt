package com.cezila.hermes.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class KeyAlgorithmTest {

    @Test
    fun rsa4096_hasCorrectDisplayName() {
        assertEquals("RSA-4096", KeyAlgorithm.RSA_4096.displayName)
    }

    @Test
    fun rsa4096_bcAlgorithmTag_isOne() {
        assertEquals(1, KeyAlgorithm.RSA_4096.bcAlgorithmTag)
    }

    @Test
    fun ed25519_hasCorrectDisplayName() {
        assertEquals("Ed25519", KeyAlgorithm.ED25519.displayName)
    }

    @Test
    fun ed25519_bcAlgorithmTag_is22() {
        assertEquals(22, KeyAlgorithm.ED25519.bcAlgorithmTag)
    }

    @Test
    fun entries_containsExactlyTwoValues() {
        assertEquals(2, KeyAlgorithm.entries.size)
    }

    @Test
    fun valueOf_rsa4096_returnsCorrectEntry() {
        assertSame(KeyAlgorithm.RSA_4096, KeyAlgorithm.valueOf("RSA_4096"))
    }

    @Test
    fun valueOf_ed25519_returnsCorrectEntry() {
        assertSame(KeyAlgorithm.ED25519, KeyAlgorithm.valueOf("ED25519"))
    }
}
