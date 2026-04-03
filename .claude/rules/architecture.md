---
paths:
  - "app/src/main/java/com/cezila/hermes/domain/**"
  - "app/src/main/java/com/cezila/hermes/data/**"
  - "app/src/main/java/com/cezila/hermes/presentation/**"
---
# Clean Architecture Rules

## Layer boundaries

- `domain/` has NO Android imports. No `Context`, no `android.*`. Pure Kotlin only.
- `data/` depends on `domain/` — implements its interfaces. Never the reverse.
- `presentation/` depends on `domain/` use cases only. Never imports from `data/` directly.

## Use cases

- One public function per use case, named `invoke()` or `execute()`.
- Use cases return `Result<T>` or a sealed class — never throw exceptions to the caller.
- Side effects (keystore writes, file I/O) belong in `data/`, not use cases.

## Repository pattern

- Interfaces in `domain/repository/` define the contract.
- Implementations in `data/repository/` fulfill it.
- Mappers in `data/mapper/` convert between `data` models and `domain` models. No raw data models in `domain/`.

## MVI

- ViewModel exposes: `state: StateFlow<ScreenState>`, `sideEffects: Flow<SideEffect>`.
- UI sends intents via a single `onIntent(intent: ScreenIntent)` function.
- State is immutable data classes. Use `.copy()` to produce new state.
- `SideEffect` is for one-time events: navigation, toasts, dialogs.
