---
description: Kick off GitHub issue work session with branch setup, project updates, and planning
---

You are an AI coding assistant automating the “start work on an issue” workflow for the `OWS-PFMS/flatcomp` repository. Beyond preparing git state, you **must** clarify requirements, analyze the codebase, and generate a comprehensive implementation plan with rigorous verification criteria. Another agent will execute the plan, and you will later verify completion, so the verification steps you produce must be detailed enough for airtight validation. Never discard user work without explicit confirmation. Stop immediately and report any command errors.

## Context

- **Repository**: `OWS-PFMS/flatcomp`
- **Default branch**: `main`
- **Project board**: `Material Flat Component Library` (project #5)
- **Branch naming pattern**: `feature/issue-{NUMBER}-{short-description}`
- **Tech stack**: Java 21 + Maven + Swing/FlatLaf (no JUnit test suite yet — see CLAUDE.md)
- **Planning requirement**: Plans must be saved under `docs/stories/issue-{NUMBER}/plan.md` (create directory if needed)
- **Verification requirement**: A separate verification checklist must be saved under `docs/stories/issue-{NUMBER}/verify.md`
- **Multi-agent flow**: You author the plan and verification checklist → a separate agent implements the plan → you verify completion using the checklist

## Usage

```
/story:start
/story:start 6
/story:start issue-6
/story:start FlatPill drag-reorder semantics #42
```

- If no argument is supplied, list candidate issues (Step&nbsp;1).
- If an argument is supplied:
  1. Try to resolve it as an exact issue number or `issue-{number}` slug.
  2. If not an exact match, search open issues for close matches to the provided text (title or number fragment).
  3. If one match is found, show a short summary (title + number) and ask the user to confirm before proceeding.
  4. If multiple close matches exist, present the list and ask the user to pick; if none found, fall back to listing all issues as in Step&nbsp;1.
  5. Continue the workflow once the user confirms the target issue.

## Workflow

### Step 1 – Select Issue
1. If the user supplied an issue argument and it resolved to a single issue, display a brief summary and ask the user to confirm. Continue only after confirmation.
2. If no argument was supplied, or the supplied argument was ambiguous/unresolved:
   - Run: `gh issue list --state open --label enhancement --limit 20`
   - Show the list to the user and ask which issue number to work on. Do not continue until the user responds.

### Step 2 – Fetch Issue Details
1. Run: `gh issue view {ISSUE_NUMBER}`
2. Parse title, labels, milestone, description, acceptance criteria (checkboxes). Store for later display/todo creation.
3. Also fetch issue type and parent/child relationships (needed for Step 2.5 and Step 4):
   ```bash
   gh api graphql -f query='
     query {
       repository(owner:"OWS-PFMS", name:"flatcomp") {
         issue(number:{ISSUE_NUMBER}) {
           issueType { name }
           parent { number title issueType { name } }
           subIssues(first:50) {
             totalCount
             nodes { number title state }
           }
         }
       }
     }
   '
   ```
   Store `issueType.name`, `parent` (may be `null`), and `subIssues.nodes` (will be empty for non-epics; used by Step 2.5 for Epic auto-redirect).

### Step 2.5 – Epic Auto-Redirect (re-target to first OPEN child)
1. **Skip condition.** If `issueType.name != "Epic"`, skip Step 2.5 entirely and proceed to Step 3.
2. **Empty-epic refusal (preserved).** If `subIssues.totalCount == 0`, the epic has no children to redirect to. **Stop the normal workflow** — do NOT create a branch, do NOT touch the project board, do NOT create any `docs/stories/` artifacts. Output a refusal:
   ```
   ⚠️  Issue #{NUMBER} is an Epic with no children — nothing to redirect to.

   Milestone: {milestone title or "None"}

   Suggested:
     • Add children first via /story:author:refactor {NUMBER}
     • Or re-type this issue as a regular story
   ```
   Await user direction before proceeding.
3. **No-OPEN-children refusal.** If `subIssues.totalCount > 0` but every child has `state == "CLOSED"`, also **stop the normal workflow** with the same hands-off behavior — do NOT create a branch, do NOT touch the project board, do NOT create any `docs/stories/` artifacts. Output a refusal listing every (CLOSED) child for context:
   ```
   ⚠️  Issue #{NUMBER} is an Epic with no OPEN children — nothing to redirect to.

   Milestone: {milestone title or "None"}

   Children ({subIssues.totalCount}):
     #{n} CLOSED  {title}
     #{n} CLOSED  {title}
     ...
   ```
   Await user direction before proceeding.
4. **Auto-redirect (the happy path).** Iterate `subIssues.nodes` in the order returned by GraphQL — this is the **user-curated order** the parent issue was reordered to in the GitHub UI. **Do NOT sort by issue number.** Pick the first node whose `state == "OPEN"` (uppercase, matching the GraphQL `IssueState` enum). Call this `{REDIRECT_NUMBER}` and `{REDIRECT_TITLE}`.
5. Print the acknowledgement line, exactly:
   ```
   Issue #{NUMBER} is an Epic — auto-starting first OPEN child #{REDIRECT_NUMBER}: {REDIRECT_TITLE}.
   ```
6. **Re-enter the workflow on the child.** Set `ISSUE_NUMBER := REDIRECT_NUMBER` and **resume from Step 2** — re-fetch issue details, type, parent, and sub-issues for the redirected child. The Step 1 argument-confirmation prompt is **skipped** for the redirected target — the redirect line above is the announcement. All subsequent steps (3, 4 including the parent-epic sweep, 5, 6, 7, 8, 9, 10, 11, 12) run against the redirected child unchanged.
7. **Nested-epic handling (recursion via re-entry).** Because Step 2 is re-run with the redirected number, Step 2.5 fires again whenever the redirected child is itself an Epic. Nested epics therefore walk down to the first OPEN non-Epic descendant via repeated one-hop redirects, with one acknowledgement line per hop. No explicit recursion or depth cap is required; natural termination is "first OPEN non-Epic descendant" or an Epic with no OPEN children (which falls through to the refusal in 2.5.2 / 2.5.3).
8. **Parent-epic sweep is preserved.** Step 4 (sub-step 5, the parent-epic sweep) continues to operate against the `parent` field fetched in the re-entered Step 2. When Step 2 fetches data for the redirected child, its `parent` is the original Epic, so the Step 4 sweep still moves the original Epic to In Progress as part of the child's start flow — satisfying the "parent-epic move-to-In-Progress sweep still runs for the auto-selected child" requirement.

### Step 3 – Handle Git State
1. Check current branch: `git branch --show-current`
2. Check working tree: `git status --short`
3. Apply branch/cleanliness rules:
   - If not on `main` → ask user whether to switch/finish/cancel; wait for instructions.
   - If uncommitted changes → ask user to commit/stash/discard (never discard automatically).
4. To reach clean main:
   - `git checkout main`
   - `git pull origin main`
5. If any command fails, stop and report.

### Step 4 – Move Issue to “In Progress” on Project Board
1. Query project metadata:
   ```bash
   gh api graphql -f query='
     query {
       repository(owner: "OWS-PFMS", name: "flatcomp") {
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
2. Extract from the response: `ITEM_ID`, `PROJECT_ID`, `FIELD_ID`, and the option id whose name == `In Progress` (`IN_PROGRESS_OPTION_ID`). If not found, stop and report.
3. Update status:
   ```bash
   gh api graphql -f query='
     mutation {
       updateProjectV2ItemFieldValue(
         input: {
           projectId: "{PROJECT_ID}"
           itemId: "{ITEM_ID}"
           fieldId: "{FIELD_ID}"
           value: {
             singleSelectOptionId: "{IN_PROGRESS_OPTION_ID}"
           }
         }
       ) {
         projectV2Item { id }
       }
     }
   '
   ```
4. Confirm the mutation succeeded before proceeding.
5. **Parent-epic sweep.** If the `parent` field fetched in Step 2 is non-null AND `parent.issueType.name == "Epic"`, the parent epic should reflect that work is underway on a child. Repeat Steps 4.1–4.3 for the parent epic number (fetch its project item + Status field, then move it to `In Progress` — but only if it is not already `In Progress`). If the parent epic is already `In Progress`, skip the mutation silently. After this sub-step, log one line in the chat output, e.g.:
   - First time: `Also moved parent epic #{PARENT_NUMBER} to "In Progress".`
   - Already set: `Parent epic #{PARENT_NUMBER} already "In Progress" — no change.`
   If the parent is non-null but not an Epic (rare — a sub-issue of a non-epic), skip the sweep. Do not fail the whole command if the parent sweep errors; surface the error and continue with the normal flow.

### Step 5 – Create Feature Branch
1. Derive short description from issue title (lowercase words joined by hyphen, truncate sensibly).
2. Branch name: `feature/issue-{NUMBER}-{short-description}`
3. Verify branch does not already exist locally or remotely (`git branch --list`, `git ls-remote --heads origin`).
4. Run: `git checkout -b feature/issue-{NUMBER}-{short-description}`

### Step 6 – Display Formatted Issue Summary
Output:
```
═══════════════════════════════════════════════════════
📋 ISSUE #{NUMBER}: {Title}
═══════════════════════════════════════════════════════

🏷️  Labels: {comma-separated labels or “None”}
📅 Milestone: {milestone title or “None”}
🔗 URL: {issue URL}

{Issue description body}

✅ ACCEPTANCE CRITERIA
{List acceptance criteria lines}

📝 TASKS
{List checkboxes from issue body. If none, leave placeholder}
```

### Step 7 – Clarify Requirements
1. Review issue details and identify ambiguities or missing information.
2. Ask the user targeted clarifying questions (minimum of two if anything is uncertain).
3. Record user responses for inclusion in the plan. Do not proceed until answers are provided or explicitly deferred.

### Step 8 – Codebase Reconnaissance
1. Identify relevant directories/files (`rg`, `ls`, `find`, etc.).
2. Optionally gather quick metrics (e.g., `cloc`, file counts) **only if** the insight aids planning; note when metrics are skipped and why.
3. Summarize findings (key classes, tests, configuration, existing patterns).
4. Ensure no destructive commands are run.

### Step 9 – Create TODO List
- Convert issue checkboxes to actionable TODO entries.
- If no checkboxes exist, derive TODOs from acceptance criteria.
- Present as `- [ ]` markdown list for tracking.

### Step 10 – Draft Comprehensive Implementation Plan
1. Structure the plan with sections: Summary, Phases, Detailed Steps, Risks & Mitigations, Files to Modify/Create, Testing Strategy, Verification Checklist, TODO list.
2. Each detailed step must specify action, file paths, commands, testing, and time estimate.
3. Translate acceptance criteria, clarifications, and reconnaissance findings into explicit verification activities and expected outcomes. These checks must enable the future verification pass to assert completion with evidence.
4. Make verification steps granular enough that the implementing agent can reference them during development and you can later confirm each one objectively.
5. Present the complete plan in chat **and** save it to `docs/stories/issue-{NUMBER}/plan.md` (create the directory if it doesn’t exist).

### Step 11 – Save Verification Checklist to `verify.md`
- Produce a detailed checklist enumerating every condition that must be satisfied before you can mark the story complete.
- Tie each checklist item to the relevant acceptance criterion or clarification, and specify the evidence or tests required for sign-off.
- Save the checklist to `docs/stories/issue-{NUMBER}/verify.md` (separate from the plan so the implementing agent does not load it — prevents test-gaming).
- Also present the checklist in chat; you will rely on it when performing the final verification pass.

### Step 12 – Provide Next Steps
Output:
```
🚀 READY TO START!

✅ On branch: feature/issue-{NUMBER}-{desc}
✅ Issue moved to "In Progress"
✅ Implementation plan saved to docs/stories/issue-{NUMBER}/plan.md
✅ Verification checklist saved to docs/stories/issue-{NUMBER}/verify.md

Lifecycle: /story:start → /story:implement (or /story:implement:phase) → /story:verify {NUMBER} → /story:close {NUMBER}

Next: Review the plan above, start with the first TODO, and keep the verification checklist handy—it is the single source of truth for the `/story:verify {NUMBER}` pass that gates `/story:close`.
```

## Safety Rules

- Never discard or modify user changes without explicit approval.
- Stop and surface any git/gh errors immediately.
- Always verify GraphQL mutations succeed before proceeding.
- Confirm branch non-existence before `git checkout -b`.
- When gathering metrics, avoid long-running or heavy scans unless the user agrees.

## Final Reminder

After completing all steps and saving the plan, wait for the user to confirm before any implementation begins. When the implementing agent reports completion, use the verification checklist you authored to validate every requirement.
