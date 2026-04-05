package com.cezila.hermes.core.domain.model

enum class KeyAlgorithm(val displayName: String, val bcAlgorithmTag: Int) {
    RSA_4096("RSA-4096", 1),   // PGPPublicKey.RSA_GENERAL
    ED25519("Ed25519", 22),    // PGPPublicKey.EDDSA
}
