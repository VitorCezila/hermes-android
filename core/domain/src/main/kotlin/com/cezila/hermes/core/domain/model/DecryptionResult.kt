package com.cezila.hermes.core.domain.model

data class DecryptionResult(
    val plaintext: String,
    val plaintextBytes: ByteArray?,
    val fileName: String?,
    val signatureStatus: SignatureStatus,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecryptionResult) return false
        return plaintext == other.plaintext &&
            plaintextBytes.contentEquals(other.plaintextBytes) &&
            fileName == other.fileName &&
            signatureStatus == other.signatureStatus
    }

    override fun hashCode(): Int {
        var result = plaintext.hashCode()
        result = 31 * result + (plaintextBytes?.contentHashCode() ?: 0)
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + signatureStatus.hashCode()
        return result
    }
}

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean {
    if (this == null && other == null) return true
    if (this == null || other == null) return false
    return this.contentEquals(other)
}

sealed interface SignatureStatus {
    data object None : SignatureStatus

    data class Valid(
        val signerKeyId: String,
        val signerFingerprint: String,
    ) : SignatureStatus

    data class Invalid(
        val signerKeyId: String,
        val reason: String,
    ) : SignatureStatus

    data class UnknownSigner(val signerKeyId: String) : SignatureStatus
}
