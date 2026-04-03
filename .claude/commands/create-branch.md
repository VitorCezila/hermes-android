Create a Git branch for a GitHub issue following the project's naming convention.

## Branch convention

Format: `type/issue-number-short-description`

Examples:
- `feat/42-generate-pgp-key`
- `fix/15-key-export-crash`
- `chore/7-setup-hilt-di`

**Type mapping from issue label:**
- `enhancement` → `feat`
- `bug` → `fix`
- `chore` → `chore`
- `documentation` → `docs`
- `refactor` → `refactor`
- `test` → `test`

## Instructions

The user will provide the issue number as $ARGUMENTS.

## Steps

1. Fetch the issue details:
   ```
   gh issue view <issue-number> --json title,labels
   ```
2. Derive the branch type from the issue labels (see mapping above). If ambiguous, ask the user.
3. Generate the short description:
   - Use the issue title, lowercased, words separated by hyphens
   - Remove articles, prepositions, and punctuation
   - Max 4-5 words
   - Example: "Add RSA-4096 key generation" → `generate-rsa-key`
4. Show the proposed branch name and ask for confirmation before creating.
5. Create the branch from `develop`:
   ```
   git checkout develop && git pull origin develop
   git checkout -b <branch-name>
   ```
6. Print the branch name and confirm it was created.
