package com.cezila.hermes.core.data.crypto

import com.cezila.hermes.core.domain.crypto.ParsedPublicKeyInfo
import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.util.encoders.Hex

class BouncyCastlePgpKeyParser : PgpKeyParser {

    private val uidRegex = Regex("""^(.+?) <(.+?)>$""")

    override suspend fun parsePublicKey(armoredKey: String): Result<ParsedPublicKeyInfo> = runCatching {
        val inputStream = PGPUtil.getDecoderStream(armoredKey.byteInputStream(Charsets.UTF_8))
        val keyRingCollection = PGPPublicKeyRingCollection(inputStream, JcaKeyFingerprintCalculator())

        val keyRing = keyRingCollection.keyRings.asSequence().firstOrNull()
            ?: throw IllegalArgumentException("No public key ring found in armored input")

        val masterKey: PGPPublicKey = keyRing.publicKeys.asSequence()
            .firstOrNull { it.isMasterKey }
            ?: throw IllegalArgumentException("No master key found in key ring")

        val algorithm = KeyAlgorithm.entries.firstOrNull { it.bcAlgorithmTag == masterKey.algorithm }
            ?: throw IllegalArgumentException("Unsupported key algorithm: ${masterKey.algorithm}")

        val uid = masterKey.userIDs.asSequence().firstOrNull()
            ?: throw IllegalArgumentException("No user ID found on key")

        val matchResult = uidRegex.matchEntire(uid)
        val ownerName = matchResult?.groupValues?.get(1)?.trim() ?: uid
        val ownerEmail = matchResult?.groupValues?.get(2)?.trim() ?: ""

        val fingerprint = Hex.toHexString(masterKey.fingerprint).uppercase()
        val keyId = "0x${java.lang.Long.toHexString(masterKey.keyID).uppercase().padStart(16, '0')}"
        val createdAt = masterKey.creationTime.time
        val expiresAt = if (masterKey.validSeconds > 0) createdAt + masterKey.validSeconds * 1000 else null

        ParsedPublicKeyInfo(
            fingerprint = fingerprint,
            keyId = keyId,
            algorithm = algorithm,
            ownerName = ownerName,
            ownerEmail = ownerEmail,
            createdAt = createdAt,
            expiresAt = expiresAt,
        )
    }
}
