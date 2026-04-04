package com.cezila.hermes.core.data.crypto

import com.cezila.hermes.core.domain.crypto.GeneratedKeyMaterial
import com.cezila.hermes.core.domain.crypto.PgpKeyGenerator
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Date

class BouncyCastlePgpKeyGenerator : PgpKeyGenerator {

    private val bcProvider = BouncyCastleProvider()

    override suspend fun generate(
        ownerName: String,
        ownerEmail: String,
        algorithm: KeyAlgorithm,
        passphrase: CharArray,
    ): Result<GeneratedKeyMaterial> = runCatching {
        val createdAt = System.currentTimeMillis()
        val creationDate = Date(createdAt)

        val masterKeyPair: PGPKeyPair = when (algorithm) {
            KeyAlgorithm.RSA_4096 -> generateRsaKeyPair(creationDate)
            KeyAlgorithm.ED25519 -> generateEd25519KeyPair(creationDate)
        }

        val uid = "$ownerName <$ownerEmail>"

        val hashAlgo = if (algorithm == KeyAlgorithm.ED25519) {
            HashAlgorithmTags.SHA512
        } else {
            HashAlgorithmTags.SHA256
        }

        val signerBuilder = JcaPGPContentSignerBuilder(
            masterKeyPair.publicKey.algorithm,
            hashAlgo,
        ).setProvider(bcProvider)

        val encryptorBuilder = JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
            .setProvider(bcProvider)

        val subpacketGen = PGPSignatureSubpacketGenerator().apply {
            setKeyFlags(
                false,
                KeyFlags.CERTIFY_OTHER or KeyFlags.SIGN_DATA or
                    KeyFlags.ENCRYPT_COMMS or KeyFlags.ENCRYPT_STORAGE,
            )
            setPreferredSymmetricAlgorithms(
                false,
                intArrayOf(SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_128),
            )
            setPreferredHashAlgorithms(
                false,
                intArrayOf(HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA512),
            )
            setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)
        }

        val keyRingGenerator = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            masterKeyPair,
            uid,
            null,
            subpacketGen.generate(),
            null,
            signerBuilder,
            encryptorBuilder.build(passphrase),
        )

        val secretKeyRing = keyRingGenerator.generateSecretKeyRing()
        val publicKeyRing = keyRingGenerator.generatePublicKeyRing()

        val fingerprint = Hex.toHexString(secretKeyRing.publicKey.fingerprint).uppercase()
        val keyId = "0x${java.lang.Long.toHexString(secretKeyRing.publicKey.keyID).uppercase().padStart(16, '0')}"

        val armoredPublicKey = armorKeyRing { aos -> publicKeyRing.encode(aos) }
        val armoredPrivateKey = armorKeyRing { aos -> secretKeyRing.encode(aos) }

        GeneratedKeyMaterial(
            armoredPublicKey = armoredPublicKey,
            armoredPrivateKey = armoredPrivateKey,
            fingerprint = fingerprint,
            keyId = keyId,
            createdAt = createdAt,
        )
    }

    private fun generateRsaKeyPair(creationDate: Date): PGPKeyPair {
        val generator = RSAKeyPairGenerator()
        generator.init(RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), SecureRandom(), 4096, 80))
        val bcKeyPair = generator.generateKeyPair()
        return BcPGPKeyPair(PGPPublicKey.RSA_GENERAL, bcKeyPair, creationDate)
    }

    private fun generateEd25519KeyPair(creationDate: Date): PGPKeyPair {
        val kpg = KeyPairGenerator.getInstance("Ed25519", bcProvider)
        val javaKeyPair = kpg.generateKeyPair()
        return JcaPGPKeyPair(PGPPublicKey.EDDSA, javaKeyPair, creationDate)
    }

    private fun armorKeyRing(encode: (ArmoredOutputStream) -> Unit): String {
        val baos = ByteArrayOutputStream()
        ArmoredOutputStream(baos).use { encode(it) }
        return baos.toString(Charsets.UTF_8)
    }
}
