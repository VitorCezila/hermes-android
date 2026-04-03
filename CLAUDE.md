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

Clean Architecture with MVI pattern, three layers:

```
Presentation → Domain → Data
```

- `domain/` — Pure Kotlin. Use cases, repository interfaces, domain models. Zero Android dependencies.
- `data/` — Repository implementations, Android Keystore, Bouncy Castle PGP wrapper, mappers.
- `presentation/` — Compose screens + ViewModels following MVI. Each screen has `State`, `Intent`, and `SideEffect`.
- `ui/theme/` — Material 3 theme only (Color, Type, Theme).

Package root: `com.cezila.hermes`

## Conventions

- MVI: never mutate state directly. Emit new state via `reduce()` or equivalent.
- Use cases are single-responsibility. One use case = one operation.
- Repository interfaces live in `domain/`, implementations in `data/`.
- Mappers in `data/mapper/` handle all domain ↔ data model conversions.
- Compose: stateless composables receive state and callbacks, never touch ViewModel directly.
- Coroutines: use `viewModelScope` in ViewModels, `Dispatchers.IO` for keystore/crypto operations.
- Min SDK 24. Do not use APIs above it without `@RequiresApi` + runtime check.

## Security rules

- Private keys never leave the device. Never log, serialize to a network call, or expose in any UI.
- Use Android Keystore for key material storage — never write raw private key bytes to SharedPreferences or files.
- Fingerprints and public keys are safe to display; private key material is not.
- No `Log.d/e` with cryptographic material in production paths.

## Design system — The Tactile Digital Archive

- Palette: Paper `#F9F9F6` · Charcoal `#1A1C1B` · Clay `#944925`
- UI text: Inter. Cryptographic data (hashes, fingerprints): Monospace.
- Depth via tonal layering, not shadows. No decorative animations.

## Watch out for

- Instrumented tests require a connected device or emulator — `connectedAndroidTest` will fail otherwise.
- Bouncy Castle integration is planned but not yet implemented. Do not assume crypto APIs exist.
- Android Keystore has hardware-backed vs software-backed key behavior differences across API levels.
