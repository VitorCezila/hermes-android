Create a Git commit following the Conventional Commits specification.

## Commit convention

Format: `type(scope): short description`

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `style`, `perf`, `ci`

**Scope (optional):** architecture layer or module — e.g., `domain`, `data`, `presentation`, `infra`, `ui`, `crypto`

**Rules:**
- Description in imperative, lowercase, no period at the end
- Max 72 characters in the subject line
- Body optional: explain *why*, not *what*

Examples:
- `feat(domain): add GenerateKeyPairUseCase`
- `fix(presentation): correct key list not refreshing on delete`
- `chore: configure Hilt dependency injection`
- `refactor(data): extract KeyRepository to interface`

## Instructions

The user may provide a hint as $ARGUMENTS (e.g., "I added the key generation use case"). If no arguments, analyze the diff.

## Steps

1. Run `git status` and `git diff --staged` (and `git diff` if nothing staged) to understand the changes.
2. Infer the commit type and scope from the changed files:
   - Files in `domain/` → scope `domain`
   - Files in `data/` → scope `data`
   - Files in `ui/` or `presentation/` → scope `presentation`
   - Build files, DI setup → scope `infra` or no scope
3. Draft a commit message and show it to the user for confirmation.
4. If there are unstaged changes, ask the user which files to stage (or `git add -A` if they want all).
5. After confirmation, run:
   ```
   git add <files>
   git commit -m "<message>"
   ```
6. Print the commit hash after success.
