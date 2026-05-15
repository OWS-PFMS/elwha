---
description: Execute all phases of a user story sequentially in one session (for short stories)
---

You are **Agent 2** in a multi-agent workflow. Your job is to **autonomously implement** a user story plan that Agent 1 created by executing all phases sequentially in **ONE context session**. You will work phase-by-phase, commit after each phase, and prepare the code for Agent 1's verification.

**When to use this command**:
- ✅ Short stories (2-3 phases, <6 hours total)
- ✅ When you want to run all phases in one session without context loss

**When to use `/story:implement:phase` instead**:
- ✅ Long stories (4+ phases, >6 hours total)
- ✅ When you want fresh context per phase (avoids autocompaction)
- ✅ When resuming work after context loss

## Multi-Agent Workflow Context

```
Agent 1 (/story:start) → Creates plan + branch + verification checklist
         ↓
Agent 2 (THIS COMMAND) → Implements plan autonomously
         ↓
Agent 1 (/story:verify) → Validates against checklist
         ↓ (iterate if needed)
Agent 1 (/story:close) → Submits PR
```

## Usage

```bash
/story:implement           # Auto-detect from branch name
/story:implement 18        # Implement issue #18 explicitly
```

## Workflow

### Step 1 – Identify Target Issue

1. **If argument provided**: Use that issue number
2. **If no argument**: Parse branch name `feature/issue-{N}-...` to extract issue number
3. **If cannot detect**: Ask user which issue to implement

### Step 2 – Locate and Read Plan

1. Check for plan file: `docs/stories/issue-{N}/plan.md`
2. **If not found**: Stop and report:
   ```
   ❌ Plan file not found: docs/stories/issue-{N}/plan.md

   Have you run /story:start yet? That command creates the plan.
   ```
3. **If found**: Read entire plan file using Read tool

### Step 3 – Parse Plan Structure

Extract the following sections:
- **Summary**: High-level overview
- **Phase Blueprint**: Table or sections with phase metadata (type, entry/exit criteria, dependencies, deliverables, verification)
- **Detailed Steps**: Step-by-step actions grouped by phase (with phase numbering: 1.1, 2.1, etc.)
- **Files to Modify/Create**: Complete list of affected files
- **Testing Strategy**: When and how to test
- **TODO List**: Tracking items (use for TodoWrite)
- **Risks & Mitigations**: Potential issues
- **Verification Checklist**: Referenced but NOT used during implementation (prevents test-gaming)

### Step 4 – Understand and Clarify

1. Review the plan for completeness and clarity
2. Check for ambiguities:
   - Vague action descriptions
   - Missing file paths
   - Unclear requirements
   - Conflicting information
3. **If ambiguities found**: Ask clarifying questions and WAIT for answers
4. **If plan is clear**: Display understanding and confirm ready to proceed:
   ```
   ═══════════════════════════════════════════════════════
   📋 IMPLEMENTATION PLAN UNDERSTOOD
   ═══════════════════════════════════════════════════════

   Issue #N: FlatPill drag-reorder semantics

   PHASES:
   1. Reorder-event API research
   2. PillReorderEvent + PillReorderListener
   3. PillListModel reorder integration
   4. FlatPillList drag affordance + cursor swap
   5. Playground exercise + docs

   FILES TO MODIFY: 8 files
   FILES TO CREATE: 3 files
   ESTIMATED TIME: 4 hours

   AUTONOMOUS EXECUTION:
   ✅ Will commit after each phase
   ✅ Will update @version tags to current milestone (validated post-commit; will amend on miss)
   ✅ Will run tests only when specified
   ✅ Will fix compilation errors autonomously
   ✅ Will NOT modify existing tests (will fix code instead)

   Ready to implement autonomously. Starting in 3 seconds...
   ```

### Step 5 – Initialize Todo Tracking

1. Extract TODO list from plan
2. Use TodoWrite to create todos:
   ```markdown
   - [ ] Phase 1: Reorder-event API research
   - [ ] Phase 2: PillReorderEvent + PillReorderListener
   - [ ] Phase 3: PillListModel reorder integration
   - [ ] Phase 4: FlatPillList drag affordance + cursor swap
   - [ ] Phase 5: Playground exercise + docs
   ```
3. Mark first phase as `in_progress`

### Step 6 – Get Current Milestone

1. Determine current milestone for @version tagging:
   ```bash
   gh api repos/OWS-PFMS/elwha/milestones --jq '.[] | select(.state == "open") | select(.title | startswith("v")) | .title' | head -1
   ```
2. Store milestone for use in @version tags (e.g., `v0.1.0`)

### Step 7 – Implement Each Phase Autonomously

**For each phase in the Phase Blueprint:**

#### 7.1 Load Phase Context
- **Phase metadata** from Phase Blueprint (type, goal, entry/exit criteria, deliverables)
- **Detailed steps** for this phase (numbered: 1.1, 1.2 or 2.1, 2.2, etc.)
- **Previous phase summaries** (if dependencies exist - already in memory since same session)

#### 7.2 Mark Phase as In Progress
```
TodoWrite: Update current phase to "in_progress"
```

#### 7.3 Verify Entry Criteria
Check each entry criterion from Phase Blueprint:
- Previous phases complete? ✅ (in same session)
- Required files exist? (verify with Read or ls)
- Milestone confirmed? ✅
- Branch correct? ✅

If ANY fails, stop and report.

#### 7.4 Execute Phase Based on Type

**For Research Phases**:
- Execute detailed steps (investigation, analysis)
- **Produce documentation** in `docs/stories/issue-{N}/` directory
- **DO NOT**: Compile code, run tests, update @version tags
- **Exit artifact**: Documentation file (e.g., `phase-1-research.md`)

**For Implementation Phases**:
- Execute detailed steps (create/modify source code)
- **Update `@version` tags** in ALL modified Java files per `docs/development/versioning.md`:
  ```java
  /**
   * Class description.
   *
   * @author Charles Bryan
   * @version v0.1.0
   * @since v0.1.0
   */
  ```
  - Add `@since` tags for NEW methods/classes
  - Use current milestone for @version value
- **Compile**: Run `mvn compile` - must succeed before continuing
  - If compilation fails: Fix errors autonomously, retry
  - Do NOT ask user for help
- **Run tests** (if plan specifies): `mvn -Dtest=ClassName test`
  - If tests fail: Fix YOUR code (old tests) or debug YOUR implementation (new tests)
  - Retry until passing
- **Exit requires**: Compilation success + tests pass (if specified)

**For Bug Fix Phases**:
- Reference failing verification item from phase goal (e.g., "V3")
- Execute detailed steps (minimal code changes to fix bug)
- **Run failing test** to confirm fix
- **Document** what/why/how in commit message
- **Exit requires**: Previously-failing test now passes

#### 7.5 Verify Exit Criteria
Check each exit criterion from Phase Blueprint:
- Deliverables exist and complete?
- Code compiles (Implementation/Bug Fix)?
- Tests pass (if specified)?
- @version tags updated (Implementation/Bug Fix)?

##### Validate @version tags (Implementation/Bug Fix phases only)

After the phase commit lands (Step 7.6), run the same validator CI uses
to confirm every modified Java file in this branch's diff has the correct
@version tag for the milestone:

```bash
git fetch origin main
python3 scripts/update_javadoc_version.py \
    --check --changed-only \
    --expected "{MILESTONE}" \
    --base-ref origin/main
```

- **On exit 0:** log `✅ @version validation passed` and continue to
  Step 7.7 (append phase summary).
- **On non-zero exit:** the validator prints which files have stale or
  missing tags. Fix the JavaDoc on each listed file, stage the fixes
  (`git add <paths>`), and amend the phase commit:

  ```bash
  git commit --amend --no-edit
  ```

  Then re-run the validator. Loop until pass. **Do not advance to the
  next phase until the gate passes.**

Note: although this gate is documented inside Step 7.5 (Verify Exit
Criteria), its execution is sequenced *after* Step 7.6 so that
`git diff origin/main...HEAD` already includes the phase commit's new
files. Research phases skip this gate entirely (no Java edits).

If ANY fails, stop and report. Do NOT commit incomplete phase.

#### 7.6 Commit Phase with Type-Specific Format

**Research Phase Commit**:
```bash
git add docs/stories/issue-{N}/{deliverable}.md
git commit -m "docs(issue-{N}): Complete Phase {i} - {Phase Name}

{Research findings summary}

Deliverables:
- docs/stories/issue-{N}/{filename}.md

Satisfies verification: {V1} (reference)

Related to #{N}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
"
```

**Implementation Phase Commit**:
```bash
git add [source files] [test files]
git commit -m "feat(issue-{N}): Complete Phase {i} - {Phase Name}

{2-3 sentence description of what was implemented}

Changes:
- {change 1}
- {change 2}

Deliverables:
- {file1}
- {file2}

@version tags updated to {milestone}

Satisfies verification: {V2, V3} (reference)

Related to #{N}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
"
```

**Bug Fix Phase Commit**:
```bash
git add [fixed files]
git commit -m "fix(issue-{N}): Complete Phase {i} - {Phase Name}

{Bug description and fix}

Bug: {What was broken}
Fix: {How it was resolved}

Deliverables:
- {fixed file}

Satisfies verification: {V4} (fix)

Related to #{N}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
"
```

**Commit Message Requirements:**
- Type prefix matches phase type (docs/feat/fix)
- Include issue number in scope: `(issue-{N})`
- Include phase number and name
- List deliverables
- Reference verification items (as labels, not test details)
- Include `Related to #{N}` reference
- Include Claude Code attribution

#### 7.7 Append Phase Summary to Plan

**CRITICAL**: Add phase summary to `docs/stories/issue-{N}/plan.md` so context is preserved.

```markdown
## Phase {i} Summary (Completed: YYYY-MM-DD)

**Goal**: {what was accomplished}

**Deliverables**:
- {file 1}
- {file 2}

**Exit Verification**:
- ✅ {criterion 1}
- ✅ {criterion 2}

**Verification Items Satisfied**: {V1, V2}

**Next Phase Context**:
{1-2 sentences of guidance for next phase}

**Commit**: {hash}
```

Use Edit or Write tool to append summary to plan.md.

#### 7.8 Mark Phase Complete
```
TodoWrite: Mark current phase as "completed"
TodoWrite: Mark next phase as "in_progress" (if exists)
```

#### 7.9 Report Progress
```
✅ PHASE {i} COMPLETE: {Phase Name}

Type: {Research|Implementation|Bug Fix}
Files modified: {count}
Files created: {count}
Tests run: {pass/fail/skipped}
Commit: {commit-hash}

Moving to Phase {i+1}...
```

### Step 8 – Handle Missing Files

**If plan references a file that doesn't exist:**
1. **For files YOU should create**: Create them per plan specifications
2. **For files that should exist but don't**: Report error and stop:
   ```
   ❌ ERROR: File should exist but not found

   Expected: src/com/owspfm/ui/SomeClass.java

   This may indicate:
   - Wrong branch checked out
   - Plan references incorrect file path
   - File was deleted/moved

   Please verify and fix before continuing.
   ```

### Step 9 – Final Verification

After all phases complete:

1. **Run full test suite**:
   ```bash
   mvn test
   ```

2. **Verify all modified Java files have correct @version tags** (final
   sweep — belt-and-suspenders coverage in case any per-phase gate was
   skipped):
   ```bash
   git fetch origin main
   python3 scripts/update_javadoc_version.py \
       --check --changed-only \
       --expected "{MILESTONE}" \
       --base-ref origin/main
   ```
   On non-zero exit, fix the listed files' JavaDoc, stage them, and
   amend the most recent phase commit (`git commit --amend --no-edit`).
   Re-run until pass.

3. **Check compilation**:
   ```bash
   mvn clean compile
   ```

4. **Report status**:
   ```
   ═══════════════════════════════════════════════════════
   ✅ IMPLEMENTATION COMPLETE
   ═══════════════════════════════════════════════════════

   Issue #N: FlatPill drag-reorder semantics

   PHASES COMPLETED: 5/5
   COMMITS CREATED: 5
   FILES MODIFIED: 8
   FILES CREATED: 3

   TEST RESULTS:
   ✅ mvn clean package: BUILD SUCCESS
   ✅ Playground smoke-test: drag-reorder confirmed visually

   @VERSION TAGS:
   ✅ Updated 11 files to v0.2.0

   NEXT STEPS:
   1. Agent 1 should run verification against checklist
   2. If verification passes, Agent 1 runs /story:close
   3. If issues found, Agent 1 provides feedback for fixes

   Ready for verification! 🎉
   ```

## Error Handling

### Compilation Errors
- **Response**: Fix autonomously, retry compilation
- **If cannot fix after 3 attempts**: Stop and report issue

### Test Failures
- **Old tests failing**: Fix your implementation code
- **New tests failing**: Debug and fix your test code
- **If cannot fix after 3 attempts**: Stop and report issue

### Missing Files (Should Exist)
- **Response**: Stop and report - may indicate wrong branch or bad plan

### Ambiguous Plan
- **Response**: Ask clarifying questions BEFORE starting implementation
- **During implementation**: Stop and ask if critical ambiguity discovered

### Git Errors
- **Response**: Stop immediately and report - don't attempt to fix git issues

## Important Rules

### ✅ DO:
- Execute phases sequentially based on Phase Blueprint
- Verify entry criteria before each phase
- Apply type-specific behavior (Research/Implementation/Bug Fix)
- Commit after EACH phase (not each step, not one big commit)
- Append phase summary to plan.md after each phase
- Update @version tags to current milestone (Implementation/Bug Fix phases)
- Update @since tags for NEW methods/classes (Implementation/Bug Fix phases)
- Fix compilation errors autonomously (Implementation/Bug Fix phases)
- Fix YOUR code when old tests fail
- Create files that the plan says you should create
- Run tests ONLY when plan specifies
- Use TodoWrite to track phase progress
- Include issue number and phase number in commit messages
- Use type-specific commit format (docs/feat/fix)
- Work completely autonomously once started
- Run the @version validator after each Implementation/Bug Fix phase commit; amend on miss

### ❌ DON'T:
- Load or reference verify.md during implementation (prevents test-gaming)
- Skip entry/exit criteria verification
- Commit if exit criteria not met
- Compile/test Research phases (docs only)
- Skip compilation for Implementation/Bug Fix phases
- Modify existing tests when they fail (fix your code instead)
- Ask user for help with compilation errors (fix autonomously)
- Skip @version tag updates (Implementation/Bug Fix phases)
- Commit in middle of a phase
- Run tests when plan doesn't specify
- Continue if git commands fail
- Make changes not specified in the plan
- Create one giant commit for everything
- Skip phase summary appending
- Skip the @version validation gate or proceed to the next phase with stale tags

## Safety Checks

Before starting implementation:
- [ ] On correct feature branch (feature/issue-{N}-...)
- [ ] Plan file exists at `docs/stories/issue-{N}/plan.md`
- [ ] Phase Blueprint parsed successfully
- [ ] No uncommitted changes in working directory
- [ ] Current milestone identified for @version tags
- [ ] Plan is clear and unambiguous
- [ ] All phases have required metadata (type, entry/exit criteria, deliverables)
- [ ] @version validator runs and passes after each Implementation/Bug Fix phase commit

If any check fails, stop and report the issue.

## Example Execution

```
User: /story:implement 42

Agent: Reading plan from docs/stories/issue-42/plan.md...
Agent: Phase Blueprint parsed. 3 phases identified:
  - Phase 1 (Research): Reorder-event API survey
  - Phase 2 (Implementation): PillReorderEvent + Listener
  - Phase 3 (Implementation): FlatPillList drag affordance
Agent: Current milestone: v0.2.0
Agent: Starting autonomous implementation...

═══════════════════════════════════════════════════════
📋 PHASE 1 (Research): Reorder-event API survey
═══════════════════════════════════════════════════════

Entry criteria verified:
✅ Issue understood
✅ On correct branch

Executing research steps...
✅ Surveyed existing PillListModel API
✅ Documented event-shape options in phase-1-research.md

Exit criteria verified:
✅ Documentation complete
✅ Chosen event-shape recorded

✅ Committed: docs(issue-42): Complete Phase 1 - Reorder-event API survey
✅ Phase summary appended to plan.md

Moving to Phase 2...

═══════════════════════════════════════════════════════
📋 PHASE 2 (Implementation): PillReorderEvent + Listener
═══════════════════════════════════════════════════════

Entry criteria verified:
✅ Phase 1 complete
✅ Milestone v0.2.0 confirmed

Executing implementation steps...
✅ Created PillReorderEvent.java
✅ Created PillReorderListener.java
✅ Updated @version tags to v0.2.0
✅ Compilation successful (mvn compile)
✅ Playground confirms event fires on drop

Exit criteria verified:
✅ Code compiles
✅ @version tags updated
✅ Playground exercise confirms event flow

✅ Committed: feat(issue-42): Complete Phase 2 - PillReorderEvent + Listener
✅ Phase summary appended to plan.md

Moving to Phase 3...

═══════════════════════════════════════════════════════
📋 PHASE 3 (Implementation): FlatPillList drag affordance
═══════════════════════════════════════════════════════

Entry criteria verified:
✅ Phase 2 complete
✅ Event classes exist

Executing implementation steps...
✅ Modified FlatPillList.java (cursor swap on drag)
✅ Modified PillListModel.java (reorder method)
✅ Updated @version tags
✅ Compilation successful
✅ Playground confirms grab/grabbing cursor swap

Exit criteria verified:
✅ Drag affordance renders correctly
✅ Reorder fires PillReorderEvent

✅ Committed: feat(issue-42): Complete Phase 3 - FlatPillList drag affordance
✅ Phase summary appended to plan.md

═══════════════════════════════════════════════════════
✅ IMPLEMENTATION COMPLETE - Ready for verification
═══════════════════════════════════════════════════════
```

## Integration with Other Commands

- **Receives input from**: `/story:start` (creates plan with Phase Blueprint)
- **Alternative to**: `/story:implement:phase` (for long stories, use phase-by-phase execution)
- **Prepares for**: `/story:verify` (verification by Agent 1)
- **Followed by**: `/story:close` (after verification passes)
- **Uses milestone from**: GitHub API or plan.md metadata (for @version tags)

## When to Use This vs /story:implement:phase

**Use `/story:implement`** (this command):
- Short stories (2-3 phases, <6 hours total work)
- When you want to complete all phases in one session
- When context loss is unlikely

**Use `/story:implement:phase`** instead:
- Long stories (4+ phases, >6 hours total work)
- When you want fresh context per phase (avoids autocompaction)
- When resuming work after interruption
- When iterating on specific phase after feedback

## Notes

- This command operates in "autonomous mode" - minimal user interaction
- Executes ALL phases sequentially in ONE context session
- User can still interrupt with Ctrl+C if needed
- All commits follow conventional commits format with type prefix (docs/feat/fix)
- Commit messages include issue number, phase number, and verification references
- Phase summaries appended to plan.md preserve context for future sessions
- @version tagging follows conventions in `docs/development/versioning.md`
- Type-specific behavior: Research phases skip compilation, Implementation/Bug Fix phases require it
- verify.md is intentionally NOT loaded to prevent test-gaming behavior
- Testing strategy follows plan specifications (not arbitrary)
- Error handling is intelligent but conservative (stops on critical issues)
- Entry/exit criteria verification ensures clean phase boundaries
- @version compliance is enforced via `scripts/update_javadoc_version.py` at phase boundary, mirroring CI. Failures during implementation are amended into the phase commit, not deferred.
