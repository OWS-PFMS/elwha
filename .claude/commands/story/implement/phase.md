---
description: Execute a single phase from a user story plan autonomously in a fresh context
---

You are an implementing agent in a phase-based workflow. Your job is to execute **ONE SPECIFIC PHASE** from a user story plan autonomously. You start with **ZERO context** (fresh session) and must load all necessary story understanding from files before beginning work.

## Purpose

Execute a single phase independently in a fresh context window to avoid autocompaction issues. This command enables:
- **Context isolation**: Each phase runs in a clean session
- **Resumability**: Work can continue after context loss
- **Focused execution**: Agent works on one well-scoped task
- **Clear handoffs**: Entry/exit criteria ensure clean phase transitions

## Multi-Agent Workflow Context

```
Agent 1 (/story:start)     → Creates plan with Phase Blueprint
         ↓
Agent 2 (/story:implement:phase) → Executes Phase 1 in fresh context
         ↓ (new session)
Agent 2 (/story:implement:phase) → Executes Phase 2 in fresh context
         ↓ (new session)
Agent 2 (/story:implement:phase) → Executes Phase 3 in fresh context
         ↓ (iterate for all phases)
Agent 1 (/story:verify)    → Validates against checklist
         ↓
Agent 1 (/story:close)     → Submits PR
```

## Usage

```bash
/story:implement:phase 42 1      # Execute Phase 1 of Issue #42
/story:implement:phase 18 3      # Execute Phase 3 of Issue #18
/story:implement:phase 7 2       # Execute Phase 2 of Issue #7
```

**Arguments**:
- **First**: Issue number (required)
- **Second**: Phase number (required)

## When to Use This Command

**Use `/story:implement:phase`**:
- ✅ Long stories (4+ phases, >6 hours total)
- ✅ After context loss/autocompaction
- ✅ When you want fresh context per phase
- ✅ When iterating on specific phase after feedback

**Use `/story:implement` instead**:
- ✅ Short stories (2-3 phases, <6 hours total)
- ✅ When you want to run all phases in one session

## Workflow

### Step 1 – Validate Arguments

1. **Check both arguments provided**:
   - Issue number: Must be provided
   - Phase number: Must be provided
2. **If missing**: Stop and report:
   ```
   ❌ Missing required arguments

   Usage: /story:implement:phase <issue-number> <phase-number>
   Example: /story:implement:phase 42 2
   ```

### Step 2 – Locate and Load Plan

1. **Construct plan path**: `docs/stories/issue-{N}/plan.md`
2. **Use Read tool** to load entire plan file
3. **If not found**: Stop and report:
   ```
   ❌ Plan file not found: docs/stories/issue-{N}/plan.md

   This may indicate:
   - /story:start has not been run yet
   - Issue number is incorrect
   - Plan file was moved/deleted

   Please verify and run /story:start first.
   ```

### Step 3 – Parse Phase Blueprint

1. **Locate Phase Blueprint section** in plan.md (table or expanded format)
2. **Extract target phase metadata**:
   - Phase number, name, type (Research/Implementation/Bug Fix)
   - Goal (what this phase accomplishes)
   - Entry criteria (prerequisites to verify)
   - Exit criteria (how to know phase is complete)
   - Dependencies (which phases must finish first)
   - Deliverables (concrete outputs)
   - Verification (which V# items satisfied - reference only, not test details)
   - Estimate (expected duration)
3. **If target phase not found**: Stop and report:
   ```
   ❌ Phase {N} not found in Phase Blueprint

   Available phases: {list phase numbers and names}
   ```

### Step 4 – Load Story Context

**CRITICAL**: You have ZERO memory from previous phases. Load full story understanding from files.

#### 4.1 Extract Story Overview
From plan.md, extract:
- **Issue title** and **milestone**
- **Summary** (high-level goals)
- **Acceptance criteria** (what success looks like)
- **Clarifications applied** (if any)
- **Reconnaissance findings** (codebase context)

#### 4.2 Load Phase Progression Context
From plan.md Phase Blueprint:
- **Total phase count** (e.g., "4 phases")
- **All phase names and types**
- **Which phases are complete** (look for "## Phase {i} Summary" sections)
- **Current phase position** (e.g., "Phase 3 of 4")

#### 4.3 Load Previous Phase Summaries
For each completed phase (Phase 1 through {current-1}):
- **Goal accomplished**
- **Deliverables produced** (files created/modified)
- **Commit hash**
- **Next phase context** notes (guidance for following phase)

These summaries preserve context across sessions.

#### 4.4 Load Context Files
If phase metadata specifies context files:
- **Use Read tool** on each referenced file
- Examples: `phase-1-research.md`, `docs/development/versioning.md`

#### 4.5 Load Detailed Steps for Current Phase
From plan.md, locate section: `### Phase {N} – {Name}`
Extract numbered steps (e.g., 3.1, 3.2, 3.3) with:
- Action description
- File paths
- Commands to run
- Time estimates

#### 4.6 DO NOT Load verify.md
**IMPORTANT**: Do NOT load `docs/stories/issue-{N}/verify.md` during implementation.
- Prevents "teaching to the test" behavior
- Agent should solve problem correctly, not optimize for specific test steps
- Verification is separate step performed by different agent
- Phase Blueprint "Verification: Satisfies V4, V5" is a reference label only

### Step 5 – Display Story and Phase Context

Before execution, show comprehensive context so user can verify correct phase loading:

```
═══════════════════════════════════════════════════════
📋 LOADING STORY CONTEXT: Issue #{N}
═══════════════════════════════════════════════════════

## Story Overview
Title: {issue title}
Milestone: {milestone version}
Summary: {1-2 sentence story summary}

Acceptance Criteria:
- {AC1}
- {AC2}
- {AC3}

## Phase Progression ({current} of {total} phases)
{List all phases with completion status}
✅ Phase 1 ({Type}): {Name} → Commit {hash}
✅ Phase 2 ({Type}): {Name} → Commit {hash}
→  Phase 3 ({Type}): {Name} ← YOU ARE HERE
   Phase 4 ({Type}): {Name}

## Phase {N} Details
Type: {Research|Implementation|Bug Fix}
Goal: {what this phase accomplishes}
Estimate: {time estimate}

Dependencies: {list dependent phases or "None"}
Deliverables:
- {deliverable 1}
- {deliverable 2}

Verification: Satisfies {V1, V2, V3}

Entry Criteria:
{List each criterion with verification status}
✅ {criterion 1 - verified}
✅ {criterion 2 - verified}
⚠️  {criterion 3 - NOT MET} ← BLOCKER

Exit Criteria (what "done" means):
- {exit criterion 1}
- {exit criterion 2}
- {exit criterion 3}

## Context from Previous Phases
{For each completed dependency phase:}
Phase {i} produced:
- {key deliverables}
- {important findings/notes}
- Next phase guidance: "{guidance text}"

## Detailed Steps for Phase {N}
{N}.1 {Step description} ({estimate})
{N}.2 {Step description} ({estimate})
{N}.3 {Step description} ({estimate})
...

═══════════════════════════════════════════════════════
```

**If any entry criteria NOT met**: Stop and report blocker before proceeding.

### Step 6 – Verify Entry Criteria

**CRITICAL**: Do not start implementation if entry criteria are not satisfied.

For each entry criterion:
1. **Verify the condition** (check files exist, commits present, dependencies met)
2. **Mark as verified** (✅) or **blocker** (⚠️)

**Common entry criteria checks**:
- Previous phase complete → Check for "## Phase {i-1} Summary" in plan.md
- Files exist → Use Read tool or Bash ls to verify
- Milestone confirmed → Check milestone variable
- Branch correct → Run `git branch --show-current`
- No uncommitted changes → Run `git status --porcelain`

**If ANY criterion fails**:
```
❌ ENTRY CRITERIA NOT MET

Phase {N} cannot start. Blockers:
⚠️  {criterion description} - {why it failed}

Please resolve blockers and re-run /story:implement:phase {issue} {phase}.
```

### Step 7 – Get Current Milestone

If not already extracted from plan.md metadata:
```bash
gh api repos/OWS-PFMS/elwha/milestones --jq '.[] | select(.state == "open") | select(.title | startswith("v")) | .title' | head -1
```

Store for @version tag updates (Implementation and Bug Fix phases only).

### Step 8 – Execute Phase Based on Type

#### For Research Phases:

**Characteristics**:
- No code modifications (docs only)
- Produces documentation in `docs/stories/issue-{N}/`
- Skips compilation and test steps
- Exit artifact is always a document

**Execution**:
1. Execute detailed steps (investigation, analysis, documentation)
2. **Produce deliverable** in `docs/stories/issue-{N}/` directory:
   - Research notes (e.g., `phase-1-research.md`)
   - Design documents
   - Analysis reports
3. **Verify exit criteria** (document exists, contains required content)
4. **Skip**: Compilation, testing, @version updates
5. **Commit**: Documentation only

---

#### For Implementation Phases:

**Characteristics**:
- Creates or modifies source code
- Must compile successfully
- Updates @version/@since JavaDoc tags
- May include tests
- Exit requires working code

**Execution**:
1. Execute detailed steps (create/modify source files)
2. **Update @version tags** in ALL modified Java files:
   ```java
   /**
    * Class description.
    *
    * @author Charles Bryan
    * @version {milestone}
    * @since {milestone}
    */
   ```
   - Add `@since` tags for NEW methods/classes
   - Reference: `docs/development/versioning.md`

3. **Compile code**:
   ```bash
   mvn compile
   ```
   - **If compilation fails**: Fix errors autonomously, retry
   - **Do NOT ask user for help** with compilation errors
   - Retry until successful

4. **Run tests** (if plan specifies for this phase):
   ```bash
   mvn -Dtest={TestClass} test
   ```
   - **If tests fail**:
     - Determine if test is OLD (pre-existing) or NEW (you just wrote it)
     - **OLD test fails**: Fix YOUR code, NOT the test
     - **NEW test fails**: Debug and fix your implementation
   - Retry until passing

5. **Verify exit criteria**:
   - Code compiles ✅
   - Tests pass (if specified) ✅
   - @version tags updated ✅
   - Deliverables complete ✅

6. **Commit**: Source code + tests + related docs

---

#### For Bug Fix Phases:

**Characteristics**:
- Fixes specific failing verification or test
- References verification item (e.g., "V3")
- Minimal code changes
- Must include before/after test evidence
- Exit requires verification passing

**Execution**:
1. **Identify failing verification item** from phase goal/exit criteria
2. **Reproduce the bug** (run failing test/verification)
3. Execute detailed steps (fix code, update tests)
4. **Verify fix**:
   ```bash
   mvn -Dtest={FailingTest} test
   ```
   - Test must now PASS
5. **Document fix** in commit message (what/why/how)
6. **Verify exit criteria**: Previously-failing verification now passes
7. **Commit**: Fixed code + test evidence

### Step 9 – Verify Exit Criteria

**CRITICAL**: Do not commit if exit criteria are not satisfied.

For each exit criterion:
1. **Check the condition** (run command, verify file, check test results)
2. **Mark as satisfied** (✅) or **failed** (❌)

**Common exit criteria checks**:
- Code compiles → `mvn compile` exit code 0
- Tests pass → `mvn -Dtest=... test` shows "BUILD SUCCESS"
- Files exist → Use Read or ls to verify deliverables
- @version tags validated → see Step 10.5 (runs post-commit; gate output is the authoritative check, not a manual `grep`)
- Documentation complete → Read file, verify content sections present

**If ANY criterion fails**:
```
❌ EXIT CRITERIA NOT MET

Phase {N} is not complete. Failures:
❌ {criterion description} - {why it failed}

Cannot commit incomplete phase. Fix issues and retry.
```

**Do NOT commit** if exit criteria fail.

### Step 10 – Commit Phase Deliverables

**Use type-specific commit message format:**

#### Research Phase Commit:
```bash
git add docs/stories/issue-{N}/{phase-deliverable}.md
git commit -m "$(cat <<'EOF'
docs(issue-{N}): Complete Phase {i} - {Phase Name}

{2-3 sentence description of research findings}

Key Findings:
- {finding 1}
- {finding 2}
- {finding 3}

Deliverables:
- docs/stories/issue-{N}/{filename}.md

Satisfies verification: {V1, V2} (reference)

Related to #{N}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

#### Implementation Phase Commit:
```bash
git add {source files} {test files} {docs}
git commit -m "$(cat <<'EOF'
feat(issue-{N}): Complete Phase {i} - {Phase Name}

{2-3 sentence description of what was implemented}

Changes:
- {change 1}
- {change 2}
- {change 3}

Deliverables:
- {file1}
- {file2}
- {file3}

@version tags updated to {milestone}

Satisfies verification: {V3, V4} (reference)

Related to #{N}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

#### Bug Fix Phase Commit:
```bash
git add {fixed files} {tests}
git commit -m "$(cat <<'EOF'
fix(issue-{N}): Complete Phase {i} - {Phase Name}

{Description of bug and fix}

Bug: {What was broken / which verification failed}
Fix: {How it was resolved}

Deliverables:
- {fixed file}
- {test evidence}

Satisfies verification: {V5} (fix)

Related to #{N}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

**Commit message requirements**:
- Type prefix matches phase type (docs/feat/fix)
- Issue number in scope: `(issue-{N})`
- Phase number and name in title
- Description explains what/why
- Lists deliverables (files)
- References verification items (as labels, not test details)
- Includes `Related to #{N}`
- Includes Claude Code attribution

### Step 10.5 – Validate @version tags (Implementation/Bug Fix phases only)

**CRITICAL**: After the phase commit lands (Step 10) and before appending
the plan summary (Step 11), run the same validator CI uses to confirm
every modified Java file in this branch's diff has the correct @version
tag for the milestone. Research phases skip this step entirely (no Java
edits to validate).

```bash
git fetch origin main
python3 scripts/update_javadoc_version.py \
    --check --changed-only \
    --expected "{MILESTONE}" \
    --base-ref origin/main
```

- **On exit 0:** log `✅ @version validation passed` and continue to
  Step 11 (append phase summary).
- **On non-zero exit:** the validator prints which files have stale or
  missing tags. Fix the JavaDoc on each listed file, stage the fixes
  (`git add <paths>`), and amend the phase commit:

  ```bash
  git commit --amend --no-edit
  ```

  Then re-run the validator. Loop until pass. **Do not advance to
  Step 11 (and do not report the phase complete) until the gate
  passes.**

This gate is structurally placed between Step 10 (commit) and Step 11
(plan summary append) because the validator's `git diff origin/main...HEAD`
must include the phase commit's new Java files. The amend-on-failure
pattern keeps "one commit per phase" invariant — fixes are folded into
the same commit, not appended as a follow-up.

### Step 11 – Append Phase Summary to Plan

**CRITICAL**: Update plan.md so next phase can load context.

Append to end of `docs/stories/issue-{N}/plan.md`:

```markdown
## Phase {i} Summary (Completed: YYYY-MM-DD)

**Goal**: {what was accomplished}

**Deliverables**:
- {file 1}
- {file 2}
- {file 3}

**Exit Verification**:
- ✅ {criterion 1 verified}
- ✅ {criterion 2 verified}
- ✅ {criterion 3 verified}

**Verification Items Satisfied**: {V1, V2, V3} (reference)

**Next Phase Context**:
{1-3 sentences of guidance for Phase i+1. What should next phase know?
Examples:
- "Phase 4 should use the exception classes created here"
- "Pay attention to the logging format documented in phase-2-research.md"
- "UI integration will need to catch these specific exception types"
}

**Commit**: {commit-hash}
```

**Use Edit or Write tool** to append summary (prefer Edit if plan.md already has content after blueprint).

### Step 12 – Report Phase Completion

Display completion summary:

```
═══════════════════════════════════════════════════════
✅ PHASE {i} COMPLETE: {Phase Name}
═══════════════════════════════════════════════════════

Type: {Research|Implementation|Bug Fix}
Deliverables: {count} files
{If Implementation: Compilation: Success}
{If Implementation: Tests: {pass}/{total} passed}
Verification: Satisfies {V1, V2} (reference)
Commit: {hash}

EXIT CRITERIA VERIFIED:
✅ {criterion 1}
✅ {criterion 2}
✅ {criterion 3}

PHASE SUMMARY APPENDED TO: docs/stories/issue-{N}/plan.md

───────────────────────────────────────────────────────
NEXT STEPS
───────────────────────────────────────────────────────

{If more phases exist:}
Phase {i+1} of {total}: {Next Phase Name} ({Type})
Goal: {next phase goal}

To execute next phase, run:
  /story:implement:phase {N} {i+1}

{If this was last phase:}
All {total} phases complete! 🎉

Ready for verification. Run:
  /story:verify {N}
```

## Error Handling

### Entry Criteria Not Met
- **Response**: Stop immediately, report which criterion failed
- **Do NOT proceed** with implementation
- User must resolve blocker

### Compilation Errors (Implementation/Bug Fix Phases)
- **Response**: Fix autonomously, retry up to 3 times
- **If cannot fix**: Stop and report error details
- **Do NOT ask user for help** with compilation errors

### Test Failures (Implementation/Bug Fix Phases)
- **Old tests failing**: Fix YOUR code, not the test
- **New tests failing**: Debug and fix your implementation
- **If cannot fix after 3 attempts**: Stop and report

### Missing Files (Expected to Exist)
- **Response**: Stop and report - may indicate wrong branch, incorrect plan, or missing dependency
- Examples: Plan references file from previous phase that doesn't exist

### Ambiguous Steps
- **Response**: Ask clarifying questions BEFORE implementation starts
- **During implementation**: Stop and ask if critical ambiguity discovered

### Git Errors
- **Response**: Stop immediately and report - don't attempt to fix git issues

### Exit Criteria Not Met
- **Response**: Stop, report which criterion failed
- **Do NOT commit** incomplete phase
- Fix issues and retry verification

## Important Rules

### ✅ DO:
- Load full story context from plan.md (summary + all phase summaries + current phase details)
- Display loaded context before starting (transparency)
- Verify ALL entry criteria before implementing
- Execute type-specific behavior (Research/Implementation/Bug Fix)
- Fix compilation errors autonomously
- Verify ALL exit criteria before committing
- Append phase summary to plan.md after committing
- Use type-specific commit message format
- Include issue number in commit scope
- Reference verification items as labels (not test details)
- Run the @version validator after the phase commit; amend on miss

### ❌ DON'T:
- Assume any context from previous sessions (you have zero memory)
- Load verify.md during implementation (prevents test-gaming)
- Start implementation if entry criteria fail
- Commit if exit criteria fail
- Modify existing tests when they fail (fix your code instead)
- Ask user for help with compilation errors (fix autonomously)
- Skip @version tag updates (Implementation/Bug Fix phases)
- Make changes beyond current phase scope
- Guess at missing context (load from files or ask)
- Skip the @version validation gate or report phase complete with stale tags

## Safety Checks

Before starting implementation:
- [ ] Both arguments provided (issue number + phase number)
- [ ] Plan file exists and loaded successfully
- [ ] Target phase found in Phase Blueprint
- [ ] All entry criteria verified and satisfied
- [ ] Previous phase summaries loaded (if dependencies exist)
- [ ] Current milestone identified (for @version tags)
- [ ] On correct feature branch
- [ ] @version validator runs and passes after the phase commit (Step 10.5)

If any check fails, stop and report the issue.

## Example Execution

```
User: /story:implement:phase 42 3

Agent: Loading story context from docs/stories/issue-42/plan.md...

═══════════════════════════════════════════════════════
📋 LOADING STORY CONTEXT: Issue #42
═══════════════════════════════════════════════════════

## Story Overview
Title: FlatPill drag-reorder semantics
Milestone: v0.2.0
Summary: Add drag-to-reorder support to FlatPillList with PillReorderEvent

Acceptance Criteria:
- PillListModel exposes a reorder method
- FlatPillList swaps to grabbing cursor during drag
- PillReorderEvent fires on drop with from/to indices

## Phase Progression (3 of 4 phases)
✅ Phase 1 (Research): Reorder-event API survey → Commit abc123f
✅ Phase 2 (Implementation): PillReorderEvent + Listener → Commit def456a
→  Phase 3 (Implementation): FlatPillList drag affordance ← YOU ARE HERE
   Phase 4 (Bug Fix): Fix cursor flicker on row boundary

## Phase 3 Details
Type: Implementation
Goal: Wire drag affordance + cursor swap into FlatPillList
Estimate: 2.5h

Dependencies: Phases 1, 2
Deliverables:
- Modified FlatPillList.java
- Modified PillListModel.java
- Playground exercise update

Verification: Satisfies V4, V5

Entry Criteria:
✅ Phase 2 complete (commit def456a exists)
✅ Milestone v0.2.0 confirmed
✅ Reorder event surface designed

Exit Criteria:
- Drag changes cursor to grabbing PNG
- Drop fires PillReorderEvent with correct indices
- @version tags updated to v0.2.0

## Context from Previous Phases
Phase 1 produced:
- docs/stories/issue-42/phase-1-research.md
- Key finding: Reuse existing card/list cursor PNGs (light/dark, 16/32px)

Phase 2 produced:
- PillReorderEvent.java (immutable record)
- PillReorderListener.java (functional interface)
- Next phase guidance: "Fire event only after drop, not during drag"

## Detailed Steps for Phase 3
3.1 Wire grab/grabbing cursor swap into FlatPillList drag handler (45m)
3.2 Add reorder(int from, int to) to PillListModel (30m)
3.3 Fire PillReorderEvent on drop (30m)
3.4 Update @version tags (15m)
3.5 Update FlatPillPlayground to exercise drag-reorder (60m)

═══════════════════════════════════════════════════════
✅ READY TO EXECUTE PHASE 3
═══════════════════════════════════════════════════════

Implementing Phase 3...

[Modifies FlatPillList.java...]
[Modifies PillListModel.java...]
[Updates FlatPillPlayground.java...]
[Compiles: mvn compile → SUCCESS]
[Playground confirms cursor swap + event firing]
[Updates @version tags to v0.2.0]

Exit criteria verified:
✅ Drag affordance renders correctly
✅ PillReorderEvent fires on drop
✅ @version tags updated

Committing phase deliverables...
[git commit → Commit hash: 9876abc]

Running @version validation gate (Step 10.5)...
[git fetch origin main]
[python3 scripts/update_javadoc_version.py --check --changed-only --expected v0.2.0 --base-ref origin/main → 0 issues]
✅ @version validation passed (milestone: v0.2.0)

Appending phase summary to plan.md...

═══════════════════════════════════════════════════════
✅ PHASE 3 COMPLETE: FlatPillList drag affordance
═══════════════════════════════════════════════════════

Type: Implementation
Deliverables: 3 files
Compilation: Success
Playground: visual confirmation
Verification: Satisfies V4, V5
Commit: 9876abc

EXIT CRITERIA VERIFIED:
✅ Drag affordance renders correctly
✅ PillReorderEvent fires on drop
✅ @version tags updated

PHASE SUMMARY APPENDED TO: docs/stories/issue-42/plan.md

───────────────────────────────────────────────────────
NEXT STEPS
───────────────────────────────────────────────────────

Phase 4 of 4: Fix cursor flicker on row boundary (Bug Fix)
Goal: Resolve V3 cursor flicker when drag crosses row boundary

To execute next phase, run:
  /story:implement:phase 42 4
```

## Integration with Other Commands

- **Receives input from**: `/story:start` (creates plan with Phase Blueprint)
- **Coordinates with**: Multiple invocations of `/story:implement:phase` (one per phase)
- **Alternative to**: `/story:implement` (runs all phases in one session)
- **Followed by**: `/story:verify` (after all phases complete)
- **Uses milestone from**: GitHub API or plan.md metadata

## Notes

- **This command assumes ZERO context** - every invocation is a fresh start
- User can interrupt with Ctrl+C if needed
- Phase summaries in plan.md are the ONLY context handoff mechanism
- Verification items (V1, V2, etc.) are referenced as labels, not test details
- verify.md is intentionally NOT loaded to prevent test-gaming behavior
- @version tagging follows conventions in `docs/development/versioning.md`
- Type-specific behavior ensures Research phases don't compile, Implementation phases do
- Entry/exit criteria enable clean phase boundaries and resumability
