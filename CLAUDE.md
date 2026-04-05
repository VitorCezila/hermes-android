# Hermes Android

Local PGP key manager for Android. No backend, no accounts, no cloud. All cryptographic operations happen on-device.

## Commands

```bash
./gradlew assembleDebug       # Build debug APK
./gradlew assembleRelease     # Build release APK
./gradlew test                # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests (device required)
./gradlew lint                # Run lint checks
./gradlew ktlintCheck         # Kotlin style check (if configured)
```

## Architecture

Multi-module Clean Architecture with MVI pattern:

```
:app → :core:domain, :core:data, :core:ui
:core:data → :core:domain
:core:ui   → :core:domain
```

| Module | Package | Conteúdo |
|--------|---------|----------|
| `:app` | `com.cezila.hermes` | Activity, NavHost, screens, BaseViewModel, Hilt wiring |
| `:core:domain` | `com.cezila.hermes.core.domain` | MVI contracts, use cases, repository interfaces, models |
| `:core:data` | `com.cezila.hermes.core.data` | Repository impls, Keystore, Bouncy Castle wrapper, mappers |
| `:core:ui` | `com.cezila.hermes.core.ui` | Theme (Color, Type, Theme), shared Compose components |

Convention plugins in `build-logic/`: `hermes.android.application`, `hermes.android.library`, `hermes.kotlin.library`, `hermes.hilt`.

## Conventions

- MVI: never mutate state directly. Emit new state via `.copy()`.
- Use cases are single-responsibility. One use case = one operation.
- Repository interfaces live in `:core:domain`, implementations in `:core:data`.
- Mappers in `:core:data` handle all domain ↔ data model conversions. Raw data models never leak into domain.
- Compose: stateless composables receive state and callbacks, never touch ViewModel directly.
- Coroutines: use `viewModelScope` in ViewModels, `Dispatchers.IO` for keystore/crypto operations.
- Min SDK 24. Do not use APIs above it without `@RequiresApi` + runtime check.
- `:core:domain` must have zero Android dependencies (`android.*`, `androidx.*`).

## Security rules

- Private keys never leave the device. Never log, serialize to a network call, or expose in any UI.
- Use Android Keystore for key material storage — never write raw private key bytes to SharedPreferences or files.
- Fingerprints and public keys are safe to display; private key material is not.
- No `Log.d/e` with cryptographic material in production paths.

## Design system — The Tactile Digital Archive

- Palette: Paper `#F9F9F6` · Charcoal `#1A1C1B` · Clay `#944925`
- UI text: Inter. Cryptographic data (hashes, fingerprints): Monospace.
- Depth via tonal layering, not shadows. No decorative animations.

## Key model — own key vs contacts

The app follows the Kleopatra model: one identity key pair per device (`isSecret = true`), plus unlimited imported contact public keys (`isSecret = false`).

- `GenerateKeyPairUseCase` enforces the one-own-key rule: returns `Result.failure` if an `isSecret = true` key already exists.
- `ImportPublicKeyUseCase` saves contact keys via `KeyRepository.importPublicKey()` — no private key material involved.
- `PgpKeyParser` (domain interface) / `BouncyCastlePgpKeyParser` (data impl) parses armored public keys for import.
- `OnboardingViewModel` checks `keys.any { it.isSecret }` to decide whether to skip onboarding — having only contact keys does not skip it.

## Watch out for

- Instrumented tests require a connected device or emulator — `connectedAndroidTest` will fail otherwise.
- The Room schema is at version 2. Always write an explicit `Migration` class in `core/data/.../db/migration/` when modifying `PgpKeyEntity` — do not rely on destructive migration.
- `PgpKeyEntity.encryptedPrivateKeyBlob` and `.privateKeyIv` are nullable — they are null for contact keys (`isSecret = false`).
- Android Keystore has hardware-backed vs software-backed key behavior differences across API levels.
