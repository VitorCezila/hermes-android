---
paths:
  - "app/src/main/java/com/cezila/hermes/data/keystore/**"
  - "app/src/main/java/com/cezila/hermes/data/pgp/**"
---
# Cryptographic Security Rules

## Key material

- Private key bytes must NEVER appear in logs, UI, or network calls.
- Use Android Keystore (`KeyStore.getInstance("AndroidKeyStore")`) for storing key material.
- Do not write raw private key bytes to SharedPreferences, files, or Room.
- Public keys and fingerprints are safe to display and log.

## Algorithms

- Supported: RSA-4096 and Ed25519 only (per project spec).
- Do not introduce new algorithms without explicit user approval.
- Ed25519 is preferred for new keys; RSA-4096 for legacy compatibility.

## Bouncy Castle (planned)

- PGP operations will use Bouncy Castle (`org.bouncycastle`).
- Wrap all BC operations in try/catch — BC throws checked exceptions.
- Do not expose BC types (`PGPPublicKey`, etc.) beyond the `data/pgp/` boundary. Map to domain models before returning.

## Error handling

- Cryptographic errors must never expose internal details to the UI layer.
- Map exceptions to sealed `CryptoError` domain types before crossing the data→domain boundary.
- Log errors with `Log.e` (tag, message) but never include key material in the message.
