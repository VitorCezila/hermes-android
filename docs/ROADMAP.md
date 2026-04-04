# Hermes Android — Project Roadmap

## Context

Hermes is a local-first PGP key manager for Android (no backend, no accounts). The project skeleton already exists (Gradle, multi modules) but has no features implemented yet.

**Design screens ready (Stitch):** Empty State, Keys Management, Key Import, Encrypt & Sign, Decrypt & Verify

**Design corrections:**
- Remove "Refresh Sync" button from Keys Management screen (app is 100% local)
- "Learn about Local Encryption" leads to an internal informative screen

**Screens that need design (not in Stitch yet):**
- Key Generation screen (choosing algorithm, name, email, passphrase)
- Key Detail screen (tapping a key in the list → full details, export, delete, revoke)
- Settings screen (accessed via gear icon on Keys Management)

---

## Roadmap — Implementation Phases

### Phase 0: Foundation & Design System
> Setup the base architecture, theme, and navigation shell

- Material 3 theme: colors (Paper #F9F9F6, Charcoal #1A1C1B, Clay #944925), typography (Inter + Monospace)
- Bottom navigation shell (Keys, Encrypt, Decrypt tabs) with placeholder screens
- Base MVI infrastructure: BaseViewModel, UiState, UiEvent, UiEffect patterns
- Dependency injection setup (Hilt)
- Navigation graph (Compose Navigation)

**Deliverable:** App launches, shows bottom nav with 3 empty tabs, theme is applied.

---

### Phase 1: Empty State & Onboarding
> First screen the user sees when there are no keys

- Empty state screen with illustration, tagline, two CTAs
- "Import or Create Key" → navigates to a choice (import vs generate)
- "Learn about Local Encryption" → internal info screen (simple scrollable content)
- Footer: "SECURE INSTANCE - NO CLOUD TETHER"

**Deliverable:** App launches into empty state, both buttons navigate correctly.

---

### Phase 2: Key Generation
> Core feature — generate a new PGP key pair locally

- **Domain layer:** `PgpKey` model, `KeyPair` model, `GenerateKeyPairUseCase`, `KeyRepository` interface
- **Data layer:** Bouncy Castle PGP wrapper for RSA-4096 and Ed25519 generation, Android Keystore integration, `KeyRepositoryImpl`
- **Presentation:** Key generation screen (needs design — suggest: name, email, algorithm picker, passphrase, confirm passphrase, generate button)
- After generation → navigate to Keys Management list

**Deliverable:** User can generate a PGP key pair and see it appear in the key list.

---

### Phase 3: Keys Management (List)
> Display and manage all stored keys

- Keys list screen with cards (name, email, fingerprint, validity)
- Search/filter functionality ("Archive Search")
- "Export All" button (exports all public keys)
- FAB (+) → navigate to import/generate choice
- "Import New Identity" card at bottom of list
- Conditional: if no keys → show Empty State (Phase 1), else → show list

**Deliverable:** Key list displays generated/imported keys, search works, FAB navigates correctly.

---

### Phase 4: Key Import
> Import existing PGP keys from .asc/.gpg files

- **Domain layer:** `ImportKeyUseCase`
- **Data layer:** File parser for .asc/.gpg formats via Bouncy Castle
- **Presentation:** Import screen (file picker with 2MB limit, passphrase field, "Import to Vault" button)
- Android file picker integration (SAF — Storage Access Framework)
- Validation: file format, size limit, passphrase correctness

**Deliverable:** User can import an existing PGP key from a file and see it in the key list.

---

### Phase 5: Key Detail
> Tapping a key card → full detail view (needs design)

- Full key metadata: name, email, fingerprint, algorithm, creation date, expiry
- Actions: Export public key (share sheet), Delete key, Revoke key
- Key trust level display

**Deliverable:** User can view full details of any key and export/delete it.

---

### Phase 6: Encrypt & Sign
> Encrypt messages and files for selected recipients

- **Domain layer:** `EncryptMessageUseCase`, `SignMessageUseCase`, `EncryptAndSignUseCase`
- **Data layer:** Bouncy Castle encryption/signing implementation
- **Presentation:**
  - Recipient selection (horizontal chip list from imported public keys)
  - Payload: text input area (AES-256 indicator)
  - File archive: file picker for larger payloads
  - "Sign & Encrypt" button
  - Output: encrypted PGP block (copy/share)
- Footer: "DIGITAL SIGNATURE WILL BE APPENDED TO [RECIPIENT]"

**Deliverable:** User can select recipients, type/attach a payload, and produce an encrypted+signed PGP message.

---

### Phase 7: Decrypt & Verify
> Decrypt received messages and verify signatures

- **Domain layer:** `DecryptMessageUseCase`, `VerifySignatureUseCase`
- **Data layer:** Bouncy Castle decryption/verification
- **Presentation:**
  - PGP message paste area (with PASTE/CLEAR actions, "AWAITING VALID HEADER..." status)
  - File drop zone (.pgp/.gpg accepted)
  - Session details panel (Protocol: OpenPGP v4, Entropy Pool, Runtime: Client-Side Only)
  - "Decrypt & Verify" button
  - Output: decrypted plaintext + verification result

**Deliverable:** User can paste or drop an encrypted message and decrypt/verify it.

---

### Phase 8: Settings
> App settings (needs design)

- Suggested: default algorithm preference, key expiry defaults, about/version, clear all keys (with confirmation)
- Accessible via gear icon on Keys Management screen

**Deliverable:** Basic settings screen with core preferences.

---

## Screens Summary

| Screen | Phase | Design Status |
|--------|-------|---------------|
| Empty State | 1 | Done (Stitch) |
| Learn about Encryption | 1 | Needs design (simple info page) |
| Key Generation | 2 | Needs design |
| Keys Management | 3 | Done (Stitch) — remove "Refresh Sync" |
| Key Import | 4 | Done (Stitch) |
| Key Detail | 5 | Needs design |
| Encrypt & Sign | 6 | Done (Stitch) |
| Decrypt & Verify | 7 | Done (Stitch) |
| Settings | 8 | Needs design |

## Key Technical Decisions

- **PGP Library:** Bouncy Castle (as stated in README)
- **DI:** Hilt (standard for Android + Compose)
- **Navigation:** Compose Navigation with type-safe routes
- **Storage:** Room DB for key metadata + encrypted file storage for key material
- **File access:** Storage Access Framework (SAF) for import/export
- **Min SDK 24:** no compatibility concerns with modern Compose

## Verification

Each phase should be verified by:
1. Building and running the app (`./gradlew assembleDebug`)
2. Manual testing of the feature on emulator/device
3. Unit tests for domain layer use cases
4. UI state tests for ViewModels
