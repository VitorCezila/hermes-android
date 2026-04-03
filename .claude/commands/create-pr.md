Create a GitHub Pull Request for the current branch, linking it to the related issue.

## PR conventions

- **Title:** Same format as commits — `type(scope): description` — or a clear summary sentence
- **Base branch:** Always `main`
- **Issue link:** Body must contain `Closes #<issue-number>` to auto-close the issue on merge
- **Labels:** Inherit from the linked issue

## Instructions

The user may provide the issue number as $ARGUMENTS. If not provided, try to infer it from the branch name (e.g., `feat/42-generate-pgp-key` → issue #42).

## Steps

1. Get current branch: `git branch --show-current`
2. Extract the issue number from the branch name (format: `type/NUMBER-description`).
3. Fetch the issue title and labels: `gh issue view <issue-number> --json title,labels`
4. Check that all commits are pushed: `git status`; if not, push first:
   ```
   git push -u origin <branch-name>
   ```
5. Draft a PR title and body. Body template:
   ```
   ## Summary
   <brief description of what was implemented>

   ## Changes
   - <bullet points of main changes>

   Closes #<issue-number>
   ```
6. Show the draft to the user and ask for confirmation.
7. Create the PR:
   ```
   gh pr create --title "<title>" --body "<body>" --base main --label "<labels>"
   ```
8. Print the PR URL after creation.
