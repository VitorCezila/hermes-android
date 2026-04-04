package com.cezila.hermes.core.data.crypto

import com.cezila.hermes.core.domain.crypto.PgpEncryptor
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPCompressedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Date

class BouncyCastlePgpEncryptor : PgpEncryptor {

    private val provider = BouncyCastleProvider()

    override suspend fun encryptAndSign(
        plaintext: String,
        recipientArmoredPublicKey: String,
        signerArmoredPrivateKey: String,
        passphrase: CharArray,
    ): Result<String> = runCatching {
        val data = plaintext.toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        ArmoredOutputStream(baos).use { armoredOut ->
            encryptInternal(
                data = data,
                fileName = "message.txt",
                format = PGPLiteralData.TEXT,
                recipientArmoredPublicKey = recipientArmoredPublicKey,
                signerArmoredPrivateKey = signerArmoredPrivateKey,
                passphrase = passphrase,
                output = armoredOut,
            )
        }
        baos.toString(Charsets.UTF_8)
    }

    override suspend fun encryptFileAndSign(
        bytes: ByteArray,
        fileName: String,
        recipientArmoredPublicKey: String,
        signerArmoredPrivateKey: String,
        passphrase: CharArray,
    ): Result<ByteArray> = runCatching {
        val baos = ByteArrayOutputStream()
        encryptInternal(
            data = bytes,
            fileName = fileName,
            format = PGPLiteralData.BINARY,
            recipientArmoredPublicKey = recipientArmoredPublicKey,
            signerArmoredPrivateKey = signerArmoredPrivateKey,
            passphrase = passphrase,
            output = baos,
        )
        baos.toByteArray()
    }

    private fun encryptInternal(
        data: ByteArray,
        fileName: String,
        format: Char,
        recipientArmoredPublicKey: String,
        signerArmoredPrivateKey: String,
        passphrase: CharArray,
        output: OutputStream,
    ) {
        val recipientPublicKey = parseRecipientPublicKey(recipientArmoredPublicKey)
        val (signerSecretKey, signerPublicKey) = parseSignerKey(signerArmoredPrivateKey)
        val decryptor = JcePBESecretKeyDecryptorBuilder().setProvider(provider).build(passphrase)
        val signerPrivateKey = signerSecretKey.extractPrivateKey(decryptor)

        val encDataGen = PGPEncryptedDataGenerator(
            JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                .setWithIntegrityPacket(true)
                .setSecureRandom(SecureRandom())
                .setProvider(provider)
        )
        encDataGen.addMethod(
            JcePublicKeyKeyEncryptionMethodGenerator(recipientPublicKey).setProvider(provider)
        )

        encDataGen.open(output, ByteArray(1 shl 16)).use { encryptedOut ->
            PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP).open(encryptedOut).use { compressedOut ->
                val sigGen = PGPSignatureGenerator(
                    JcaPGPContentSignerBuilder(signerPublicKey.algorithm, HashAlgorithmTags.SHA256)
                        .setProvider(provider)
                )
                sigGen.init(PGPSignature.BINARY_DOCUMENT, signerPrivateKey)

                signerPublicKey.userIDs.asSequence().firstOrNull()?.let { uid ->
                    val spGen = PGPSignatureSubpacketGenerator()
                    spGen.addSignerUserID(false, uid)
                    sigGen.setHashedSubpackets(spGen.generate())
                }

                sigGen.generateOnePassVersion(false).encode(compressedOut)

                PGPLiteralDataGenerator().open(
                    compressedOut,
                    format,
                    fileName,
                    data.size.toLong(),
                    Date(),
                ).use { literalOut ->
                    literalOut.write(data)
                    sigGen.update(data)
                }

                sigGen.generate().encode(compressedOut)
            }
        }
    }

    private fun parseRecipientPublicKey(armored: String): PGPPublicKey {
        val inputStream = PGPUtil.getDecoderStream(armored.byteInputStream(Charsets.UTF_8))
        val collection = PGPPublicKeyRingCollection(inputStream, JcaKeyFingerprintCalculator())
        return collection.keyRings.asSequence()
            .flatMap { it.publicKeys.asSequence() }
            .firstOrNull { it.isEncryptionKey }
            ?: throw IllegalArgumentException("Recipient has no encryption-capable key")
    }

    private fun parseSignerKey(armored: String): Pair<PGPSecretKey, PGPPublicKey> {
        val inputStream = PGPUtil.getDecoderStream(armored.byteInputStream(Charsets.UTF_8))
        val keyRing = PGPSecretKeyRing(inputStream, JcaKeyFingerprintCalculator())
        val secretKey = keyRing.secretKeys.asSequence().firstOrNull { it.isMasterKey }
            ?: throw IllegalArgumentException("No master key found in secret key ring")
        return Pair(secretKey, secretKey.publicKey)
    }
}
