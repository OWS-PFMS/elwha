# JavaDoc Versioning Guide

This document describes the JavaDoc `@version` and `@since` tagging conventions for the Elwha library.

## Table of Contents
- [Overview](#overview)
- [Milestone Naming](#milestone-naming)
- [Tag Semantics](#tag-semantics)
- [Tagging Rules](#tagging-rules)
- [Examples](#examples)
- [PR Responsibilities](#pr-responsibilities)
- [Automation & Validation](#automation--validation)
- [Related Documentation](#related-documentation)

## Overview

All Java source files in this library use JavaDoc `@version` and `@since` tags to track when code was introduced or last modified. These tags:

- Help library consumers understand which API was added when
- Integrate with GitHub milestones for release planning
- Are enforced automatically via CI on every PR
- Align with the library's semantic versioning (see [CONTRIBUTING.md](../../CONTRIBUTING.md#versioning) and [CHANGELOG.md](../../CHANGELOG.md))

## Milestone Naming

Milestones are named `vMAJOR.MINOR.PATCH` — no `-alpha` / `-beta` suffix. While the library is pre-1.0, minor bumps may include breaking changes (documented in `CHANGELOG.md`) and patch bumps are bug-fix only.

Examples: `v0.1.0`, `v0.2.0`, `v0.2.1`, `v1.0.0`.

## Tag Semantics

### `@version`
Indicates the milestone when the class / interface / enum was last modified.

- **Update when**: any modification to the file. The bright-line rule is "any change worth committing is significant enough to bump `@version`." No per-PR debate; the validator enforces this.
- **Format**: `@version v0.N.0` — matches the PR's milestone title exactly.

### `@since`
Indicates the milestone when the class / interface / enum (or specific public method) was **first introduced**.

- **For files extracted from OWS-Local-Search-GUI at v0.1.0**: carries the parent-repo `@since` they had at extraction. Files still on parent-repo alpha tags (`v1.1.0-alpha.N`) will be migrated to a elwha baseline at the first repo-wide normalization pass; until then the PR-diff-scoped validator ignores them.
- **For new classes**: set to the milestone where it's first added.
- **For new public methods in existing classes**: add method-level `@since`.
- **Never changes**: once set, `@since` is immutable. The PR-diff-scoped validator enforces byte-equality against the base ref for modified files.

## Tagging Rules

### Class-Level Tags (Required)

Every top-level class, interface, and enum must have all three tags in its JavaDoc block:

```java
/**
 * Brief description of the class.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.1.0
 */
public class MyClass {
    // ...
}
```

### Method-Level Tags (Optional)

Add `@since` tags to **new public methods** added to existing classes:

```java
/**
 * Sets the pill's leading icon affordance.
 *
 * @param affordance the affordance to apply
 * @since v0.2.0
 */
public void setIconAffordance(IconAffordance affordance) {
    // ...
}
```

**Do not** add `@since` to:
- Private methods (unless part of important internal API)
- Methods present in the file's `@since` baseline
- Overridden methods (inherit from parent)

### Inner Classes and Enums

Inner classes and enums inherit versioning from their enclosing class. Do not add separate `@version` / `@since` tags unless the inner type has independent significance.

## Examples

### New Class in v0.2.0

```java
package com.owspfm.elwha.chip;

/**
 * Compact pill replacement aligned with Material chip taxonomy.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public class ElwhaChip {
    // ...
}
```

### Existing Class Modified in v0.2.0

```java
/**
 * Card primitive with header, body, and configurable surface variants.
 *
 * @author Charles Bryan
 * @version v0.2.0    ← bumped (any modification bumps @version)
 * @since v0.1.0      ← unchanged (immutable)
 */
public class ElwhaCard {

    /**
     * Sets the keep-summary-when-expanded behavior.
     *
     * @param keep whether the summary remains visible after expanding
     * @since v0.2.0    ← new method
     */
    public void setKeepSummaryWhenExpanded(boolean keep) {
        // ...
    }
}
```

### Bug Fix Bumps `@version`

Under the bright-line rule, any change worth committing bumps `@version`. Bug fixes therefore bump `@version` while leaving `@since` alone:

```java
/**
 * Selection model for FlatPillList with tab-strip semantics.
 *
 * @author Charles Bryan
 * @version v0.2.0    ← bumped (any modification bumps @version)
 * @since v0.1.0      ← unchanged (immutable)
 */
public class PillSelectionModel {

    public void clearSelection() {
        // Fixed off-by-one when last item was selected
    }
}
```

## PR Responsibilities

When creating a pull request:

1. **Set the PR milestone** to match the target release (e.g., `v0.2.0`).

2. **Update `@version` tags** in every modified Java file:
   ```bash
   # Change @version from previous milestone (e.g. v0.1.0) to current
   - @version v0.2.0
   ```

3. **Add `@since` tags** to new classes and public methods:
   ```java
   // New class
   @version v0.2.0
   @since v0.2.0

   // New public method in existing class
   @since v0.2.0
   ```

4. **Validation** — the CI workflow will:
   - Detect the PR milestone automatically
   - Fail if new/modified code lacks correct version tags
   - Provide specific file-level error messages

## Automation & Validation

### CI Workflow

The CI build runs the `@version` validator on every PR. It:
- Reads the PR's milestone title
- Diffs the PR against `origin/main` (triple-dot, merge-base semantics — matches GitHub's PR diff)
- Validates `@version` and `@since` on every modified or added Java file
- Fails with actionable file-level error messages if violations are found

### Manual Validation

Validate the same way CI does — scoped to the files your branch actually changed:

```bash
# Refresh origin/main locally first
git fetch origin main

# Validate tags on files this branch modified or added
python3 scripts/update_javadoc_version.py \
    --check --changed-only \
    --expected v0.2.0 --base-ref origin/main
```

The legacy tree-wide check (`--check --expected V` without `--changed-only`) still works for emergency normalization but is no longer the preferred flow. The `--apply` mode is a maintenance tool only — running it tree-wide would overwrite `@since` on files unrelated to your PR, so the validator rejects `--apply --changed-only`.

### Branch Protection

The `build` check is required on the `main` branch. PRs cannot merge until:
- A milestone is set on the PR
- All version tags on modified/added files match the milestone
- The build workflow (including the validator) passes

### Workflow Gates

Beyond the CI workflow, the `/story:*` command family enforces `@version` compliance at three points before a PR is ever opened:

1. **`/story:implement`** — after each Implementation/Bug Fix phase commit, runs `update_javadoc_version.py --check --changed-only` and blocks the next phase until the validator passes. Misses are amended into the same phase commit (preserving the "one commit per phase" invariant). See [`.claude/commands/story/implement.md`](../../.claude/commands/story/implement.md), Step 7.5.
2. **`/story:implement:phase`** — same gate at the phase boundary, placed as Step 10.5 between commit and plan-summary append. See [`.claude/commands/story/implement/phase.md`](../../.claude/commands/story/implement/phase.md), Step 10.5.
3. **`/story:verify`** — a mandatory pre-check (Step 4.5) that re-runs the validator across the full branch diff before any user-defined verification items execute. Runs unconditionally regardless of mode. Failure surfaces as `V0` ❌ in the QA report and blocks `/story:close`. See [`.claude/commands/story/verify.md`](../../.claude/commands/story/verify.md), Step 4.5.

These gates do **not** change the [Tagging Rules](#tagging-rules) — they enforce them earlier in the workflow so stale tags don't reach the open PR. The validator command at every gate is identical to CI's invocation; see the [playbook](./versioning-playbook.md#2-during-milestone-development) for the canonical command line.

## Related Documentation

- [Versioning Playbook](./versioning-playbook.md) — Detailed guide for milestone bumps and common scenarios
- [CHANGELOG.md](../../CHANGELOG.md) — Project changelog (Keep a Changelog format)
- [CONTRIBUTING.md](../../CONTRIBUTING.md) — Release process and PR conventions

## Questions or Issues?

- **Missing milestone?** Set the PR milestone before requesting review — CI will fail without one.
- **Validation failing?** The error message names the file and the expected vs. actual tag value; fix the listed files and re-push.
- **Unsure if version should change?** The bright-line rule says yes: any file you committed is a file whose `@version` must equal the PR's milestone.
