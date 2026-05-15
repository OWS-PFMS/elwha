---
description: Complete and close a GitHub issue story - create PR, close issue, move to Done, cleanup branches
tags: [project]
---

# Close Story Workflow

Complete the current GitHub issue story by creating a PR, closing the issue, updating the project board, and cleaning up branches.

## Your Task

You are completing a GitHub issue-based story. Follow these steps in order:

---

## Step 0: Recommend Running `/story:verify` First (Soft Prompt)

Before opening the PR, confirm with the user that the verification gate has been run:

> Has `/story:verify {NUMBER}` been run successfully against this story? If not, it is recommended (though not required) to run it now and capture the QA report — `/story:close` will still proceed if you decline.

This is **informational, not blocking**. Do not halt or abort the workflow if the user has not run `/story:verify`. Note the answer and continue. The QA report (when present) is helpful evidence to paste into the PR description in Step 6.

---

## Step 1: Verify Preconditions

### 1.1 Determine Issue Number

- **If provided as command argument**: Use that issue number
- **Otherwise**: Parse from current branch name
  - Pattern: `feature/issue-{NUMBER}-{description}`
  - Example: `feature/issue-14-sealed-classes` → Issue #14
  - Use regex or string parsing to extract the number
- **If cannot determine**: Ask the user to provide the issue number

### 1.2 Verify Branch Status

```bash
# Check current branch
git branch --show-current
```

- Must be on a feature branch (NOT `main`)
- If on main, stop and report error

### 1.3 Check for Uncommitted Changes

```bash
# Check git status
git status --porcelain
```

- If there are uncommitted changes, ask user what to do:
  - "There are uncommitted changes. Would you like to commit them first?"
  - If yes, help create a commit
  - If no, stop the workflow

### 1.4 Verify Tests Pass

```bash
# Run full test suite
mvn clean test
```

- If tests fail, stop and report errors
- User must fix tests before proceeding
- Display test failure summary

### 1.5 Check CHANGELOG Requirements

Determine if this issue requires a CHANGELOG.md entry based on user impact.

**Step 1: Fetch issue metadata**
```bash
gh issue view {ISSUE_NUMBER} --json labels,milestone --jq '{labels: [.labels[].name], milestone: .milestone.title}'
```

**Step 2: Evaluate CHANGELOG necessity**

See [docs/development/changelog-policy.md](../../docs/development/changelog-policy.md) for full criteria.

Quick check (Elwha label set):
- **Required** ✅: Change adds/renames/removes public API, changes default visual behavior, or fixes a consumer-observable bug. Typical labels: `enhancement`, `user-experience`, `bug` (when user-observable).
- **Recommended** 💡: Measurable performance change, or doc fix that corrects consumer-visible misinformation.
- **Skip** ⏭️: Issue has only `technical-debt`, `documentation` (internal), `developer-experience`, or `bug` (internal-only).

**Step 3: Prompt user if required/recommended**

```
📝 CHANGELOG UPDATE CHECK

Issue #{ISSUE_NUMBER}: {TITLE}
Labels: {LABELS}

This issue may require a CHANGELOG.md entry.
See docs/development/changelog-policy.md for guidance.

Update CHANGELOG.md? (yes/no)
```

**Step 4: If YES, collect entry details**

Ask user:
1. **Category**: Added / Changed / Deprecated / Removed / Fixed / Security
2. **Description**: 1-2 sentences describing the change from user perspective

**Step 5: Update CHANGELOG.md**

1. Read current CHANGELOG.md
2. Locate or create `## [Unreleased]` section
3. Add entry under appropriate category:

```markdown
### {Category}
- **{Brief Title}** (#{ISSUE_NUMBER}): {User description}
```

4. Verify entry follows format in [changelog-policy.md](../../docs/development/changelog-policy.md)

**Step 6: Commit CHANGELOG update**

```bash
git add CHANGELOG.md
git commit -m "docs(issue-{ISSUE_NUMBER}): update CHANGELOG for {category}

Added {category} entry documenting {brief description}.

Related to #{ISSUE_NUMBER}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

**Step 7: Verify commit**
```bash
git log -1 --oneline
```

**Step 8: Continue to PR creation**

If user said NO or CHANGELOG not needed, proceed directly to Step 2.

---

## Step 2: Create Pull Request

### 2.1 Gather PR Information

Run these commands in parallel:

```bash
# Get commit history since branching from main
git log main..HEAD --oneline

# Get file change statistics
git diff main...HEAD --stat

# Get full diff line counts
git diff main...HEAD --shortstat

# Fetch issue details
gh issue view {ISSUE_NUMBER} --json title,body,labels,milestone
```

### 2.2 Create Comprehensive PR

Use `gh pr create` with a detailed body:

```bash
gh pr create --title "{ISSUE_TITLE} (Issue #{ISSUE_NUMBER})" --body "$(cat <<'EOF'
## Summary

{Brief summary of what was accomplished}

## Changes

{List key changes from git diff and commits}

## Documentation

- **CHANGELOG**: {Updated with {category} entry | Not required for this issue}

## Related Issue

Closes #{ISSUE_NUMBER}

## Test Results

```
Tests run: {COUNT}, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Files Changed

{Include git diff --stat output}

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

**PR Body Guidelines**:
- Include summary of changes
- Reference all commits
- Show test results (passing tests required)
- List files changed with statistics
- Link to issue with "Closes #{NUMBER}"
- Add Claude Code attribution footer

### 2.3 Set PR Milestone (REQUIRED)

**Why this matters**: The repo's PR-checks workflow includes a `Get PR milestone` job that fails the build when a PR has no milestone set. `gh pr create` does NOT inherit the linked issue's milestone — it must be set explicitly on the PR. Skipping this step causes a red CI check and blocks merge.

**Step 2.3.1 — Fetch the issue's milestone**:

```bash
gh issue view {ISSUE_NUMBER} --json milestone --jq '.milestone.title'
```

- If output is a title string (e.g., `v0.1.0`): use it for the PR.
- If output is `null` or empty: stop and ask the user which milestone to assign. Do NOT leave the PR milestone-less — the CI check will fail.

**Step 2.3.2 — Apply milestone to the PR**:

```bash
gh pr edit {PR_NUMBER} --milestone "{MILESTONE_TITLE}"
```

**Step 2.3.3 — Verify it stuck**:

```bash
gh pr view {PR_NUMBER} --json milestone --jq '.milestone.title'
```

Expected: the same title you just set. If empty, re-run the edit and check for typos in the milestone name (milestone titles must match exactly, including the `v` prefix).

### 2.4 Display PR URL

After creating PR and setting milestone, display the URL to the user clearly.

---

## Step 3: Wait for PR Merge

### 3.1 Inform User

Tell the user:
```
✅ Pull Request #{PR_NUMBER} created successfully!

URL: {PR_URL}

Please review and merge the PR when ready. I'll wait for you to confirm it's merged.
```

### 3.2 Provide Status Check Command

Show the user they can check PR status:
```bash
gh pr view {PR_NUMBER} --json state,mergeable,statusCheckRollup
```

### 3.3 Wait for Confirmation

Ask the user: **"Has PR #{PR_NUMBER} been merged? Type 'yes' when ready to continue."**

- Do NOT proceed until user confirms
- This is a **blocking wait** for user input

### 3.4 Verify PR is Actually Merged

Once user confirms, verify:

```bash
gh pr view {PR_NUMBER} --json state --jq '.state'
```

- Expected output: `"MERGED"`
- If not merged, inform user and wait again
- Do not proceed unless state is `MERGED`

---

## Step 4: Close Issue and Update Project Board

### 4.1 Close the GitHub Issue

```bash
gh issue close {ISSUE_NUMBER} --comment "Completed via PR #{PR_NUMBER}.

{Brief summary of what was accomplished}

**Deliverables:**
{List key deliverables}

**Test Results:**
- Tests: {COUNT} passing
- Build: SUCCESS

The {feature description} is now complete and merged to main."
```

### 4.2 Move Issue to "Done" on Project Board

Use the GraphQL API to update project status (reference the successful Issue #14 pattern):

**Step 4.2.1 - Get Project Item and Field IDs**:

```bash
gh api graphql -f query='
  query {
    repository(owner: "OWS-PFMS", name: "elwha") {
      issue(number: {ISSUE_NUMBER}) {
        projectItems(first: 10) {
          nodes {
            id
            project {
              id
              title
              field(name: "Status") {
                ... on ProjectV2SingleSelectField {
                  id
                  options {
                    id
                    name
                  }
                }
              }
            }
          }
        }
      }
    }
  }
'
```

Extract from response:
- `projectItems.nodes[0].id` → ITEM_ID
- `projectItems.nodes[0].project.id` → PROJECT_ID
- `projectItems.nodes[0].project.field.id` → FIELD_ID
- Find option where `name == "Done"` → DONE_OPTION_ID

**Step 4.2.2 - Update Status to Done**:

```bash
gh api graphql -f query='
  mutation {
    updateProjectV2ItemFieldValue(
      input: {
        projectId: "{PROJECT_ID}"
        itemId: "{ITEM_ID}"
        fieldId: "{FIELD_ID}"
        value: {
          singleSelectOptionId: "{DONE_OPTION_ID}"
        }
      }
    ) {
      projectV2Item {
        id
      }
    }
  }
'
```

Confirm success by checking the response contains the item ID.

---

## Step 5: Handle Milestone (Conditional)

### 5.1 Check if Issue Has Milestone

```bash
gh issue view {ISSUE_NUMBER} --json milestone --jq '.milestone'
```

- If `null` or empty: **Skip this entire step**
- If milestone exists: Continue to 5.2

### 5.2 Check Milestone Status

```bash
gh api repos/OWS-PFMS/elwha/milestones/{MILESTONE_NUMBER} --jq '{title: .title, open: .open_issues, closed: .closed_issues}'
```

### 5.3 Prompt for Closure (If Last Open Issue)

**Only if `open_issues == 0`**:

Ask the user:
```
📌 Milestone "{MILESTONE_TITLE}" now has all issues completed (0 open, {CLOSED} closed).

Would you like to close this milestone? (yes/no)
```

### 5.4 Close Milestone (If User Confirms)

If user says yes:

```bash
gh api -X PATCH repos/OWS-PFMS/elwha/milestones/{MILESTONE_NUMBER} -f state=closed
```

Confirm success and display closure message.

---

## Step 6: Clear Todo List

Use the TodoWrite tool to clear all todos:

```
TodoWrite with empty array: []
```

---

## Step 7: Branch Cleanup

### 7.1 Sync with Remote

```bash
# Fetch latest from remote to ensure we have merged changes
git fetch origin
```

### 7.2 Switch to Main Branch

```bash
# Switch to main
git checkout main

# Pull latest (includes our merged PR)
git pull origin main
```

### 7.3 Ask User for Deletion Confirmation

**Important**: Do NOT delete automatically. Ask first:

```
🗑️  Ready to delete feature branch '{BRANCH_NAME}'.

This will delete:
- Local branch: {BRANCH_NAME}
- Remote branch: origin/{BRANCH_NAME}

Delete branches? (yes/no)
```

### 7.4 Delete Branches (If User Confirms)

If user says yes:

```bash
# Delete local branch (safe delete - only if merged)
git branch -d {BRANCH_NAME}

# Delete remote branch
git push origin --delete {BRANCH_NAME}
```

**Critical**: Use `-d` (lowercase) NOT `-D`:
- `-d` is safe: only deletes if fully merged
- `-D` is dangerous: force deletes even if not merged

If `git branch -d` fails, it means the branch isn't fully merged. Report this to the user and do NOT force delete.

---

## Step 8: Final Summary

Display a comprehensive summary:

```
═══════════════════════════════════════════════════════
✅ STORY COMPLETE!
═══════════════════════════════════════════════════════

Issue #{ISSUE_NUMBER}: {ISSUE_TITLE}

COMPLETED ACTIONS:
✅ Pull Request #{PR_NUMBER} created and merged
✅ Issue closed and moved to "Done" on project board
{✅ Milestone "{MILESTONE_TITLE}" closed} (if applicable)
✅ Todo list cleared
✅ Branch "{BRANCH_NAME}" deleted (local + remote)
✅ Switched to main branch

You're ready to start your next story!

═══════════════════════════════════════════════════════
```

---

## Error Handling

### Tests Fail
- **Stop immediately**
- Display test failure output
- Message: "Tests must pass before creating PR. Please fix test failures and run `/story:close` again."

### Cannot Determine Issue Number
- **Stop and ask user**
- Message: "Could not determine issue number from branch name '{BRANCH}'. Please provide the issue number."

### PR Creation Fails
- **Stop and report error**
- Display `gh pr create` error output
- Check if PR already exists for this branch

### PR Not Merged
- **Wait for user**
- Do not proceed with branch deletion or issue closure
- Keep prompting until confirmed merged

### Project Board Update Fails
- **Continue but warn user**
- Message: "Could not update project board automatically. Please manually move Issue #{NUMBER} to 'Done' status."
- Provide link to project board

### Branch Deletion Fails
- **Report but don't force**
- If `-d` fails: "Branch not fully merged. Keeping branch for safety."
- Do NOT use `-D` to force delete
- User can manually delete later if needed

---

## Important Safety Rules

1. ✅ **ALWAYS** verify tests pass before creating PR
2. ✅ **ALWAYS** set the PR milestone immediately after `gh pr create` (Step 2.3). `gh pr create` does NOT inherit the issue's milestone, and the `Get PR milestone` CI job will fail the build without one.
3. ✅ **ALWAYS** verify PR is merged before deleting branches
4. ✅ **ALWAYS** use `git branch -d` (safe delete) never `-D` (force)
5. ✅ **ALWAYS** ask user confirmation before deleting branches
6. ✅ **NEVER** auto-merge PRs - user must merge manually
7. ✅ **NEVER** skip the PR merge verification step
8. ✅ Use parallel tool calls for independent operations (gathering info, checking status)
9. ✅ Use sequential operations when one depends on another (create PR, then wait for merge)

---

## Notes

- Main branch for this project is `main`
- Expected branch pattern: `feature/issue-{N}-{description}`
- All git operations follow the project's git commit protocol
- GraphQL API pattern tested and working (see Issue #14 closure)
- Milestone closure is optional and conditional
- User controls the merge timing and branch deletion
