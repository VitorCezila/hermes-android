---
paths:
  - "app/src/main/java/com/cezila/hermes/presentation/**"
  - "app/src/main/java/com/cezila/hermes/ui/**"
---
# UI & Compose Conventions

## Composables

- Composables are stateless: receive `state` + `onIntent` (or specific callbacks), never a ViewModel.
- Screen-level composables are named `<Feature>Screen(state, onIntent)`.
- Reusable components go in `presentation/components/`. Keep them generic — no business logic.

## Design system — The Tactile Digital Archive

- Palette: Paper `#F9F9F6` · Charcoal `#1A1C1B` · Clay `#944925`. Use theme tokens, not hardcoded hex.
- UI text: use `Inter` font family. Cryptographic data (fingerprints, hashes, key IDs): use a Monospace font.
- Depth via tonal surface layers (`surfaceVariant`, `surface`), not `elevation` shadows.
- No decorative animations. Motion must be intentional and minimal.

## Material 3

- Use M3 components (`androidx.compose.material3`). Do not mix M2 and M3.
- Color scheme is defined in `ui/theme/Color.kt`. Do not add new colors outside the theme.
- `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes` — always use theme tokens.

## Accessibility

- All interactive elements must have `contentDescription` or semantic labels.
- Minimum touch target: 48dp.
- Never convey information through color alone — use icons or text alongside.
