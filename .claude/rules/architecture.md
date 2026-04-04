---
paths:
  - "core/domain/**"
  - "core/data/**"
  - "core/ui/**"
  - "app/src/main/java/com/cezila/hermes/**"
---
# Clean Architecture Rules

## Module structure

```
:app              → Application shell: Activity, NavHost, Hilt wiring, feature screens, BaseViewModel
:core:domain      → Pure Kotlin: MVI contracts, use cases, repository interfaces, domain models
:core:data        → Android: repository implementations, Keystore, Bouncy Castle wrapper, mappers
:core:ui          → Android + Compose: theme (Color, Type, Theme), shared composable components
```

## Dependency rules

- `:core:domain` has NO Android dependencies. No `Context`, no `android.*`, no `androidx.*`.
- `:core:data` depends on `:core:domain`. Never the reverse.
- `:core:ui` depends on `:core:domain`. Never imports from `:core:data`.
- `:app` depends on all three core modules. It is the only module that wires everything together.
- Future feature modules depend on `:core:domain` and `:core:ui`. Never directly on `:core:data`.

## Layer boundaries

- `domain` defines contracts (repository interfaces, use cases). Never concrete implementations.
- `data` implements domain contracts. Raw data models never leak into domain.
- `presentation` (in `:app`) depends on domain use cases only. Never imports from `data` directly.

## Use cases

- One public function per use case, named `invoke()` or `execute()`.
- Use cases return `Result<T>` or a sealed class — never throw exceptions to the caller.
- Side effects (keystore writes, file I/O) belong in `data/`, not use cases.

## Repository pattern

- Interfaces in `:core:domain` define the contract.
- Implementations in `:core:data` fulfill it.
- Mappers in `:core:data` convert between data models and domain models. No raw data models in domain.

## MVI

- MVI marker interfaces (`UiState`, `UiEvent`, `UiEffect`) live in `:core:domain/mvi/`.
- `BaseViewModel` lives in `:app` (depends on `androidx.lifecycle.ViewModel`).
- ViewModel exposes: `state: StateFlow<ScreenState>`, `effect: Flow<SideEffect>`.
- UI sends events via `onEvent(event: ScreenEvent)`. State is immutable — use `.copy()`.
- `SideEffect` is for one-time events: navigation, toasts, dialogs.

## Convention plugins (build-logic)

- `hermes.android.application` — for `:app` (sets compileSdk, minSdk, Java 11)
- `hermes.android.library` — for Android library modules (`:core:data`, `:core:ui`)
- `hermes.kotlin.library` — for pure Kotlin modules (`:core:domain`)
- `hermes.hilt` — adds Hilt + KSP to a module; compose only to modules that need it
