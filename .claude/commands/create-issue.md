Create a GitHub issue for the Hermes Android project following the project's conventions.

## Instructions

The user will provide a description of the issue as $ARGUMENTS. If no arguments are given, ask the user for:
- **Title:** Short, imperative sentence (e.g., "Add RSA-4096 key generation")
- **Type:** feature, bug, or task
- **Architecture layer(s):** domain, data, presentation, infra (can be multiple)
- **Body:** Acceptance criteria or description of the problem

## Label conventions

**Type labels:**
- feature → `enhancement`
- bug → `bug`
- task → `chore`

**Architecture layer labels (create if they don't exist):**
- `layer: domain`
- `layer: data`
- `layer: presentation`
- `layer: infra`

## Steps

1. Confirm the issue details with the user before creating.
2. Ensure the required labels exist. Create missing labels using:
   ```
   gh label create "layer: domain" --color "0075ca" --description "Domain layer"
   gh label create "layer: data" --color "e4e669" --description "Data layer"
   gh label create "layer: presentation" --color "d93f0b" --description "Presentation layer"
   gh label create "layer: infra" --color "0e8a16" --description "Infrastructure layer"
   ```
3. Create the issue:
   ```
   gh issue create --title "<title>" --body "<body>" --label "<type-label>" --label "<layer-label>"
   ```
4. Print the issue URL and number after creation.
5. Ask if the user wants to create a branch for this issue right away (suggest running `/project:create-branch <issue-number>`).
