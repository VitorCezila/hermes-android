package com.cezila.hermes.core.data.crypto

import com.cezila.hermes.core.domain.crypto.PgpDecryptor
import com.cezila.hermes.core.domain.model.DecryptionResult
import com.cezila.hermes.core.domain.model.SignatureStatus
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPCompressedData
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPOnePassSignature
import org.bouncycastle.openpgp.PGPOnePassSignatureList
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class BouncyCastlePgpDecryptor : PgpDecryptor {

    private val provider = BouncyCastleProvider()

    override suspend fun decryptAndVerify(
        ciphertext: String,
        recipientArmoredPrivateKey: String,
        passphrase: CharArray,
        knownPublicKeys: List<String>,
    ): Result<DecryptionResult> = runCatching {
        val stream = PGPUtil.getDecoderStream(ciphertext.byteInputStream(Charsets.UTF_8))
        val keyRing = parseKeyRing(recipientArmoredPrivateKey)
        decryptInternal(stream, keyRing, passphrase, knownPublicKeys, isFile = false)
    }

    override suspend fun decryptFileAndVerify(
        ciphertextBytes: ByteArray,
        recipientArmoredPrivateKey: String,
        passphrase: CharArray,
        knownPublicKeys: List<String>,
    ): Result<DecryptionResult> = runCatching {
        val stream = PGPUtil.getDecoderStream(ByteArrayInputStream(ciphertextBytes))
        val keyRing = parseKeyRing(recipientArmoredPrivateKey)
        decryptInternal(stream, keyRing, passphrase, knownPublicKeys, isFile = true)
    }

    private fun decryptInternal(
        encryptedStream: InputStream,
        keyRing: PGPSecretKeyRing,
        passphrase: CharArray,
        knownPublicKeys: List<String>,
        isFile: Boolean,
    ): DecryptionResult {
        val factory = JcaPGPObjectFactory(encryptedStream)

        // Unwrap encrypted layer
        val encDataList = when (val first = factory.nextObject()) {
            is PGPEncryptedDataList -> first
            else -> factory.nextObject() as? PGPEncryptedDataList
                ?: throw IllegalArgumentException("No encrypted data found in message")
        }

        // Find a PKESK whose keyID matches any key in our ring (master or subkey)
        val pkesks = encDataList.encryptedDataObjects.asSequence()
            .filterIsInstance<PGPPublicKeyEncryptedData>()
            .toList()

        val (matchedSecretKey, pkesk) = run {
            for (candidate in pkesks) {
                val sk = keyRing.secretKeys.asSequence()
                    .firstOrNull { it.keyID == candidate.keyID }
                if (sk != null) return@run Pair(sk, candidate)
            }
            throw IllegalArgumentException("Message not encrypted for this key")
        }

        val decryptor = JcePBESecretKeyDecryptorBuilder().setProvider(provider).build(passphrase)
        val privateKey = matchedSecretKey.extractPrivateKey(decryptor)

        val decryptedStream = pkesk.getDataStream(
            JcePublicKeyDataDecryptorFactoryBuilder().setProvider(provider).build(privateKey)
        )

        // Parse inner objects
        var innerFactory = JcaPGPObjectFactory(decryptedStream)
        var obj = innerFactory.nextObject()

        // Unwrap compression if present
        if (obj is PGPCompressedData) {
            innerFactory = JcaPGPObjectFactory(obj.dataStream)
            obj = innerFactory.nextObject()
        }

        // OnePassSignature: look up signer key immediately so we can init before reading literal data
        var ops: PGPOnePassSignature? = null
        var signerPublicKey: PGPPublicKey? = null
        var signerKeyId: Long = 0L
        var opsInitialized = false

        if (obj is PGPOnePassSignatureList) {
            ops = obj.get(0)
            signerKeyId = ops.keyID
            signerPublicKey = findPublicKey(signerKeyId, knownPublicKeys)
            if (signerPublicKey != null) {
                ops.init(
                    JcaPGPContentVerifierBuilderProvider().setProvider(provider),
                    signerPublicKey,
                )
                opsInitialized = true
            }
            obj = innerFactory.nextObject()
        }

        // Read literal data
        val literalData = obj as? PGPLiteralData
            ?: throw IllegalArgumentException("No literal data found in message")

        val fileName = literalData.fileName
        val plainBytes = ByteArrayOutputStream().also { out ->
            val buffer = ByteArray(8192)
            val inputStream = literalData.inputStream
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
                if (opsInitialized) ops?.update(buffer, 0, bytesRead)
            }
        }.toByteArray()

        // Signature list
        val sigList = innerFactory.nextObject() as? PGPSignatureList

        val signatureStatus = buildSignatureStatus(
            ops = ops,
            sigList = sigList,
            signerKeyId = signerKeyId,
            signerPublicKey = signerPublicKey,
            opsInitialized = opsInitialized,
        )

        return if (isFile) {
            DecryptionResult(
                plaintext = "",
                plaintextBytes = plainBytes,
                fileName = fileName.takeIf { it.isNotBlank() },
                signatureStatus = signatureStatus,
            )
        } else {
            DecryptionResult(
                plaintext = plainBytes.toString(Charsets.UTF_8),
                plaintextBytes = null,
                fileName = null,
                signatureStatus = signatureStatus,
            )
        }
    }

    private fun buildSignatureStatus(
        ops: PGPOnePassSignature?,
        sigList: PGPSignatureList?,
        signerKeyId: Long,
        signerPublicKey: PGPPublicKey?,
        opsInitialized: Boolean,
    ): SignatureStatus {
        if (ops == null || sigList == null || sigList.isEmpty) return SignatureStatus.None

        val hexKeyId = signerKeyId.toHexKeyId()

        if (!opsInitialized || signerPublicKey == null) {
            return SignatureStatus.UnknownSigner(hexKeyId)
        }

        return runCatching {
            val sig = sigList.get(0)
            if (ops.verify(sig)) {
                val fingerprint = signerPublicKey.fingerprint.joinToString("") { "%02X".format(it) }
                SignatureStatus.Valid(signerKeyId = hexKeyId, signerFingerprint = fingerprint)
            } else {
                SignatureStatus.Invalid(signerKeyId = hexKeyId, reason = "Signature does not match")
            }
        }.getOrElse { e ->
            SignatureStatus.Invalid(signerKeyId = hexKeyId, reason = e.message ?: "Verification failed")
        }
    }

    private fun findPublicKey(keyId: Long, knownPublicKeys: List<String>): PGPPublicKey? =
        knownPublicKeys.firstNotNullOfOrNull { armored ->
            runCatching {
                parsePublicKeyRing(armored).publicKeys.asSequence()
                    .firstOrNull { it.keyID == keyId }
            }.getOrNull()
        }

    private fun parseKeyRing(armored: String): PGPSecretKeyRing {
        val stream = PGPUtil.getDecoderStream(armored.byteInputStream(Charsets.UTF_8))
        return PGPSecretKeyRing(stream, JcaKeyFingerprintCalculator())
    }

    private fun parsePublicKeyRing(armored: String): PGPPublicKeyRing {
        val stream = PGPUtil.getDecoderStream(armored.byteInputStream(Charsets.UTF_8))
        val collection = PGPPublicKeyRingCollection(stream, JcaKeyFingerprintCalculator())
        return collection.keyRings.asSequence().firstOrNull()
            ?: throw IllegalArgumentException("No public key ring found")
    }

    private fun Long.toHexKeyId(): String =
        "0x" + toULong().toString(16).uppercase().padStart(16, '0')
}
