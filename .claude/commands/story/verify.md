---
description: Verify a completed story against its saved verification checklist and produce a structured QA report
tags: [story, qa, verification]
---

# `/story:verify` — Story Verification Workflow

You are the verifying agent. Your job is to take a story whose implementation work claims to be complete, walk through the saved verification checklist (`docs/stories/issue-{N}/verify.md`), execute every item, capture evidence, and produce a structured QA report. You are the explicit gate between `/implement-*` and `/story:close` in the lifecycle:

```
/story:start → /story:implement (or /story:implement:phase) → /story:verify → /story:close
```

**Authoritative scope:** This command is the Claude-side verification flow. The Codex equivalent at `.codex/prompts/verify-story.md` is maintained in parallel for Codex users; neither file is a strict mirror of the other. Both flows are valid; pick the one that matches your tool.

---

## Usage

```
/story:verify              # Infer issue number from current branch
/story:verify 105          # Verify issue #105 (full story)
/story:verify 105 phase=2  # Verify only Phase 2 items (per-phase mode)
```

**Argument resolution order:**
1. Explicit positional number (`/story:verify 105`).
2. Current branch name pattern `feature/issue-{N}-*`.
3. Most recent `/story:start` output in the conversation.
4. Ask the user to specify.

---

## Context

- **Repository:** `OWS-PFMS/flatcomp`
- **Default branch:** `main`
- **Verification checklist file:** `docs/stories/issue-{N}/verify.md`
- **Implementation plan (for context):** `docs/stories/issue-{N}/plan.md`
- **Branch pattern:** `feature/issue-{N}-{short-description}`

**Success criteria:** Every verification item is marked ✅ pass / ❌ fail / ⚠️ blocked with concrete evidence, and the chat report ends with a single, unambiguous next-action recommendation.

---

## Workflow

### Step 0 — Identify Issue and Branch

1. Resolve the issue number using the priority order above.
2. Run `git branch --show-current` and `git rev-parse --short HEAD`. Record both — they appear in the QA report header for traceability.
3. Confirm the verification will run on either:
   - The feature branch `feature/issue-{N}-*`, or
   - Another branch the user explicitly names (commonly `main` after merge).

### Step 1 — Refresh Workspace

1. Run `git status --short`. If the working tree is **not clean**, pause and ask the user how to proceed (commit/stash/discard). Do not silently auto-stash.
2. Run `git fetch origin`.
3. If verifying on the feature branch and there are no local-only commits, run `git pull --ff-only`. If the pull is non–fast-forward, stop and report.

### Step 2 — Load Verification Artifacts

1. Read `docs/stories/issue-{N}/verify.md`. If the file is missing or unreadable, **stop** and tell the user to run `/story:start` (or otherwise produce the checklist) before retrying.
2. Optionally read `docs/stories/issue-{N}/plan.md` for context (Phase Blueprint, clarifications). Do not re-derive the checklist from the plan — `verify.md` is authoritative.

### Step 3 — Parse the Checklist (format-tolerant)

Two checklist formats are recognized. The parser must auto-detect which one this `verify.md` uses:

#### Format A — Prose Section (current authoritative format)

Each item is a `### {ID}. {short title}` block, with sub-fields written as `**Field:**` lines. Example (excerpt from `docs/stories/issue-105/verify.md`):

```markdown
### A1. Validator only flags new violations
**Covers AC:** "..."
**Evidence required:** ...
**How to verify:**
\`\`\`bash
mvn test -Dtest=...
\`\`\`
Expected: ...
```

Group items by their leading letter (A, B, C, …) — those letters are de-facto sections. There is no `Phase` field by default (see Step 4 fallback).

**Fields to extract per item:**
- `ID` — the heading prefix (e.g., `A1`, `B2`, `C3`).
- `Title` — the rest of the heading line.
- `Covers` — content of the `**Covers AC:**` / `**Covers Q?:**` line, if present.
- `Evidence required` — the `**Evidence required:**` paragraph.
- `How to verify` — the `**How to verify:**` block, including any fenced commands and the `Expected:` line that follows.
- `Phase` — usually absent in this format; treat as unknown.
- `Status` — usually absent in the source file (added by this command on output).

#### Format B — Pipe Table (forward-compatible)

A pipe table whose header row matches `| ID | Requirement | Phase | Verification Steps | Evidence | Status |`. Each non-header row is one item. The `Phase` column may carry values like `1`, `2,3`, or `2-4`.

Use Format B if the file contains such a table. Otherwise, default to Format A.

**If neither format parses cleanly,** stop and report: `verify.md exists but does not match the prose-section or pipe-table format. Cannot proceed safely.`

### Step 4 — Determine Verification Mode

1. **Default mode:** Full-story. Verify every parsed item.
2. **Per-phase mode:** triggered by `phase=K` arg, or by an interactive user request after Step 3.
3. **Fallback when no phase mapping exists** (Format A, prose-only):
   - Print verbatim or close to: `No Phase column found in verify.md; running full-story verification instead.`
   - Continue with **all** parsed items (do not silently drop any).
   - This is a soft fallback, not an error.

Echo the selected scope before executing: e.g., `Mode: full-story (15 items)` or `Mode: per-phase=2 (3 items)`.

### Step 4.5 — Mandatory `@version` validation gate

Before executing any user-defined verification item, run the same
`@version` validator CI uses. This catches stale tags from per-phase
implementations or any flow that bypassed the phase-level gate in
`/story:implement` / `/story:implement:phase`. The gate runs
**unconditionally**, regardless of verification mode (full-story or
per-phase=K).

```bash
MILESTONE=$(gh issue view {N} --json milestone --jq '.milestone.title')
git fetch origin main
python3 scripts/update_javadoc_version.py \
    --check --changed-only \
    --expected "{MILESTONE}" \
    --base-ref origin/main
```

- **On exit 0:** record this as an automatic ✅ pre-check pass. Log
  `✅ @version validation gate passed`. Continue to Step 5.
- **On non-zero exit:** treat as a verify failure that blocks
  `/story:close`. Append to the QA report's DETAIL table as item `V0`
  (or the next available slot above user items) with status ❌ and the
  validator's file list as evidence. Continue to Step 5 to gather other
  evidence, but the conclusion line at Step 7 MUST be the failure
  variant:

  `→ Failures present. Fix the listed @version tags and re-run /story:verify {N}.`

  Do **not** auto-amend during verify — surface the failure and require
  the contributor to re-run `/story:implement:phase` (or fix manually)
  and then re-run `/story:verify`. The verify-time gate is a quality
  gate, not a fix-it loop.

- **On null/empty milestone:** prompt the user inline and block until
  they supply a milestone. Do **not** skip the gate — a missing
  milestone here is exactly the failure mode this gate exists to
  prevent at close-time.

This step is unconditional even in per-phase mode: a stale `@version`
tag anywhere on the branch will fail CI later, regardless of which
phase introduced it.

### Step 5 — Execute Each Item

For each item in scope, in checklist order:

1. **Run the `How to verify` commands** in the repository root using your shell tool. Capture:
   - Exit code.
   - Last 20 lines of combined stdout/stderr.
   - Any file diffs the command produced.
2. **For manual / UI items** (no commands, or commands that say "manual"):
   - Describe the expected observation to the user.
   - Ask the user to confirm what they observed.
   - Record their answer verbatim as evidence.
3. **Compare against `Evidence required`** and the `Expected:` line:
   - **✅ Pass** — observed evidence matches the expected outcome.
   - **❌ Fail** — observed evidence contradicts the expected outcome.
   - **⚠️ Blocked** — could not run (missing dependency, prerequisite item failed, environment issue). Prefer this over a false ✅ when uncertain.

Never mark an item ✅ without concrete evidence (command output, observed UI, file content). When in doubt, mark ⚠️.

### Step 6 — Update verify.md

Modify `docs/stories/issue-{N}/verify.md` in place, additively:

#### Format A (prose) updates

- Under each `### {ID}.` block, append a new line:
  ```
  **Status:** ✅ Verified — <one-line evidence summary>
  ```
  (or `❌ Failed — <reason>` / `⚠️ Blocked — <reason>`).
- Do **not** rewrite or delete the existing requirement headings, prose, or `How to verify` blocks. Append only.

#### Format B (table) updates

- Set the `Status` cell for each verified row to `[x]` (pass), `[!]` (fail), or `[ ]` (blocked / not run).

#### Both formats — append a verification notes section

At the end of the file, append (or update if already present):

```markdown
## Verification Notes — {YYYY-MM-DD}

**Mode:** {full-story | per-phase=K}
**Branch:** {branch} @ {short-commit}
**Counts:** ✅ {pass} / ❌ {fail} / ⚠️ {blocked}

- {ID1}: ✅ <evidence summary>
- {ID2}: ❌ <failure summary + suggested fix>
- {ID3}: ⚠️ <blocker description>
```

Use today's date in `YYYY-MM-DD` format. If a `## Verification Notes` section for an earlier date already exists, leave it intact and add a new dated section beneath it.

**Refusal rule:** If any item in scope cannot be assigned a definitive ✅ / ❌ / ⚠️ status — for example, an item with no `How to verify` block and no manual confirmation — stop before writing to verify.md and tell the user explicitly:

```
Cannot mark verification complete: item {ID} has no executable evidence and no manual confirmation. Please add evidence or remove the item, then re-run /story:verify.
```

This refusal is an absolute rule: better to surface a gap than to ✅ a stub.

### Step 7 — Compile QA Report (chat output)

Print a single structured report to chat:

```
═══════════════════════════════════════════════════════
{✅ VERIFICATION COMPLETE | ❌ VERIFICATION FAILED | ⚠️ VERIFICATION INCOMPLETE}
Issue #{N} — {issue title if known}
═══════════════════════════════════════════════════════

Branch: {branch} @ {short-commit}
Mode:   {full-story | per-phase=K}

SUMMARY
- ✅ Passed:  {X}
- ❌ Failed:  {Y}
- ⚠️ Blocked: {Z}
- Total:    {X + Y + Z}

{If Phase mapping was present, include a Phase Breakdown section.
 Otherwise OMIT this section entirely.}

DETAIL
| ID | Status | Evidence / Follow-up |
|----|--------|----------------------|
| A1 | ✅     | mvn test passed (8/8) |
| A2 | ❌     | Expected "X" in log, got "Y" — see Step 5 output |
| ...

{Conclusion line — exactly one of:}

→ All items passed. Run /story:close {N} to open the PR.
→ Failures present. Fix the listed items and re-run /story:verify {N}.
→ Blocked items remain. Resolve blockers before re-running /story:verify {N}.
```

The conclusion line is the **only** automated next-action signal — keep it on its own line for easy grep.

### Step 8 — Notify the Implementing Agent (if any)

1. If failed items reference specific files / tests / docs, list them with paths so the implementing agent can fix them.
2. For per-phase mode passes, suggest continuing to the next phase via `/story:implement:phase {N} {K+1}` (when applicable).
3. For full-story mode passes, recommend `/story:close {N}`.

---

## Safety Rules

- **Do not modify source code** to make checks pass. The job is verification, not implementation. If a check fails, report it; the implementer fixes it.
- **Never** mark an item ✅ without concrete evidence.
- **Never** rewrite the original checklist content in `verify.md` — only append (Status lines, Verification Notes section).
- **Stop immediately** if a verification command fails for an unrelated reason (e.g., `mvn` not on PATH, network failure). Surface the error; do not improvise.
- **Do not** maintain unofficial side checklists — `verify.md` is the single source of truth.
- **Always run the `@version` validation gate in Step 4.5, regardless of verification mode.** Never bypass it — even on per-phase verifies, a stale tag anywhere on the branch will fail CI later. The gate is workflow-level enforcement of the policy in `docs/development/versioning.md`, not a per-story checklist item.

---

## Integration with Other Commands

- **Predecessor:** `/story:start` (creates `verify.md`).
- **Predecessor:** `/story:implement` or `/story:implement:phase` (produces the implementation under verification).
- **Successor:** `/story:close` (opens the PR — recommends but does not currently *require* a passing `/story:verify` first).

`/story:close` carries a soft prompt asking whether `/story:verify {N}` has been run; this command is the source of that recommendation.

The mandatory `@version` validation gate (Step 4.5) is the verify-side enforcement of the policy documented in `docs/development/versioning.md`. It complements the per-phase gates in `/story:implement` Step 7.5 and `/story:implement:phase` Step 10.5 — together those three gates ensure stale `@version` tags never reach the open PR. See `docs/development/versioning-playbook.md` for the canonical local-check command, which is identical at every gate.

---

## Notes for Maintainers

- This Claude command and `.codex/prompts/verify-story.md` are **parallel** implementations. Neither is authoritative over the other; both are independently maintained. Codex contributors: see `.codex/prompts/verify-story.md` for the Codex-flavored equivalent.
- The format-tolerant parser exists because today's `verify.md` files (e.g., issue-105) are prose-section format. The pipe-table format is supported for forward compatibility — newer stories may switch to it.
- Per-phase mode is a development aid only. **Story closure requires a full-story pass**, never a per-phase pass.
- The refusal rule (Step 6) is deliberate: a noisy ⚠️ is always preferable to a silent ✅. If the rule fires often, that's a signal that `verify.md` itself needs better evidence prompts in `/story:start`, not that this command is too strict.
