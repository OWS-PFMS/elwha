# JavaDoc Versioning Playbook

**Audience**: FlatComp maintainers and contributors working on milestone releases.

This playbook provides step-by-step guidance for managing JavaDoc `@version` tags through the development lifecycle, from feature branches to release milestones.

## Table of Contents
- [Quick Reference](#quick-reference)
- [Milestone Workflow](#milestone-workflow)
- [Common Scenarios](#common-scenarios)
- [Troubleshooting](#troubleshooting)
- [Maintainer Tasks](#maintainer-tasks)

## Quick Reference

| Scenario | Action |
|----------|--------|
| Creating new class | Add `@version` and `@since` with current milestone |
| Modifying an existing class | Bump `@version` to current milestone, keep `@since` (bright-line rule) |
| Adding new public method | Add method-level `@since` with current milestone |
| Bug fix | Bump `@version` to current milestone, keep `@since` |
| PR without milestone | CI will fail — set milestone first |
| Starting a new milestone | No tree-wide bump needed — validator scopes to PR diff |

## Milestone Workflow

### 1. Starting a New Milestone

When beginning work on a new milestone (e.g., transitioning from `v0.1.0` to `v0.2.0`):

> **No tree-wide bump required.** The validator only checks files modified or added by each PR. Files not touched in the new milestone keep their old `@version` indefinitely; that is the correct, intended state. Skip any "bulk update" step.

**Step 1: Create the GitHub Milestone**

```bash
gh api repos/OWS-PFMS/flatcomp/milestones \
  -f title="v0.2.0" \
  -f description="FlatPill → FlatChip rename and FlatList surface extension"
```

Or create manually in GitHub UI: Settings → Milestones → New Milestone.

**Step 2: Set Milestone on Your PR**

```bash
# When creating PR
gh pr create --milestone "v0.2.0" --title "..."

# For existing PR
gh pr edit 42 --milestone "v0.2.0"
```

**Step 3: Update Version Tags in Your Changes**

For **new files**:
```java
/**
 * FlatChip.
 *
 * @version v0.2.0
 * @since v0.2.0    ← same as @version
 */
```

For **modified files**:
```java
/**
 * FlatCard.
 *
 * @version v0.2.0    ← bumped
 * @since v0.1.0      ← unchanged
 */
```

### 2. During Milestone Development

**Before pushing each PR**:

```bash
# Refresh the base ref, then validate the same way CI does
git fetch origin main
python3 scripts/update_javadoc_version.py \
    --check --changed-only \
    --expected v0.2.0 --base-ref origin/main
```

**CI validation**:
- Runs on PR events automatically
- Reads milestone from PR metadata
- Fails if any new/modified files lack correct tags
- Provides file-specific error messages

**Workflow gates** (`/story:*` family):

`/story:implement` and `/story:implement:phase` run the validator after every phase commit; if it fails, the implementing agent amends the phase commit with the JavaDoc fix and re-runs the gate (no rollback, no follow-up commit). `/story:verify` runs a final sweep before any user-defined verification items execute. The validator command at every gate — local pre-push, per-phase, verify-time, and CI — is identical:

```bash
git fetch origin main
python3 scripts/update_javadoc_version.py \
    --check --changed-only \
    --expected v0.N.0 --base-ref origin/main
```

You can run this manually at any time to preview compliance. See [`.claude/commands/story/implement.md`](../../.claude/commands/story/implement.md) (Step 7.5), [`.claude/commands/story/implement/phase.md`](../../.claude/commands/story/implement/phase.md) (Step 10.5), and [`.claude/commands/story/verify.md`](../../.claude/commands/story/verify.md) (Step 4.5) for the gate procedures. The companion guide [`versioning.md`](./versioning.md#workflow-gates) describes how the gates fit into the broader policy.

### 3. Completing a Milestone

Before closing a milestone and creating a release, follow the release process in [`CONTRIBUTING.md`](../../CONTRIBUTING.md#release-process):

1. Bump `<version>` in `pom.xml` to the milestone target.
2. Move `[Unreleased]` → `[X.Y.Z]` in `CHANGELOG.md`.
3. Commit: `chore: release X.Y.Z`.
4. Tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`.
5. Push: `git push origin vX.Y.Z` — the `publish.yml` workflow publishes to GitHub Packages.

**Optional**: a tree-wide snapshot of `@version` compliance at close-out, since the PR-diff-scoped validator only checks per-PR. The legacy command still works:

```bash
python3 scripts/update_javadoc_version.py --check --expected v0.2.0
```

After publish, close the milestone:

```bash
gh api -X PATCH repos/OWS-PFMS/flatcomp/milestones/MILESTONE_NUMBER -f state=closed
```

## Common Scenarios

### Scenario 1: Adding a New Component

**Context**: You're adding `FlatBadge.java` in milestone `v0.3.0`.

```java
package com.owspfm.ui.components.badge;

/**
 * Compact numeric/text badge for adornment of icons and lists.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public class FlatBadge {
    // ...
}
```

**PR checklist**:
- ✅ Set PR milestone to `v0.3.0`
- ✅ Both `@version` and `@since` match milestone
- ✅ CI validation passes

### Scenario 2: Refactoring an Existing Class

**Context**: You're refactoring `FlatCard.java` to introduce the V2 API.

**Before** (last modified in `v0.1.0`):
```java
/**
 * FlatCard primitive.
 *
 * @version v0.1.0
 * @since v0.1.0
 */
public class FlatCard {
    // ...
}
```

**After** (refactored in `v0.3.0`):
```java
/**
 * FlatCard primitive with V2 setter ergonomics.
 *
 * @version v0.3.0    ← updated
 * @since v0.1.0      ← unchanged
 */
public class FlatCard {
    // ...
}
```

**PR checklist**:
- ✅ Update `@version` to current milestone
- ✅ Keep `@since` unchanged
- ✅ Document the new API in `CHANGELOG.md` under `[Unreleased]`

### Scenario 3: Adding a Method to an Existing Class

**Context**: Adding `setSurfaceColor()` to `FlatCard.java`.

```java
/**
 * FlatCard primitive.
 *
 * @version v0.2.0    ← bumped (any modification bumps @version)
 * @since v0.1.0
 */
public class FlatCard {

    /**
     * Sets a custom surface color, overriding the variant's default.
     *
     * @param color the surface color (or null to use the variant default)
     * @since v0.2.0    ← new method tag
     */
    public void setSurfaceColor(Color color) {
        // ...
    }
}
```

**PR checklist**:
- ✅ Add `@since` to new public method
- ✅ Bump class `@version` (any modification triggers the bright-line rule)

### Scenario 4: Bug Fix

**Context**: Fixing a selection bug in `PillSelectionModel.java` during milestone `v0.2.0`.

Under the bright-line rule, bug fixes bump `@version` like any other modification — `@since` doesn't move:

```java
/**
 * Selection model for FlatPillList.
 *
 * @version v0.2.0    ← bumped (any modification bumps @version)
 * @since v0.1.0      ← unchanged (immutable)
 */
public class PillSelectionModel {

    public void clearSelection() {
        // Fixed: was leaving the cursor pointing at a stale index
    }
}
```

**PR checklist**:
- ✅ Bump `@version` to current milestone
- ✅ Leave `@since` unchanged
- ✅ CI validation passes

## Troubleshooting

### CI failing: "MISSING @version"

```
✗ MISSING @version: src/com/owspfm/ui/components/FlatNew.java
```

**Solution**: add a class-level JavaDoc block with `@version` and `@since` matching your PR milestone.

### CI failing: "WRONG @version (v0.1.0 != v0.2.0)"

```
✗ WRONG @version (v0.1.0 != v0.2.0): src/com/owspfm/ui/components/card/FlatCard.java
```

**Solution**: bump `@version` to the PR's milestone. If the file truly should not have been modified, revert your changes to it.

### CI failing: "PR has no milestone"

```
ERROR: Pull request has no milestone set
Please set a milestone before requesting review
```

**Solution**:
```bash
gh pr edit YOUR_PR_NUMBER --milestone "v0.2.0"
```

### Bulk update needed

Bulk updates are not part of the normal flow — the validator only checks files modified by your PR, so untouched files retain their existing tags and that's the correct state.

If you genuinely need to normalize many files (e.g., recovering the extraction baseline, or correcting a botched import), the `--apply` mode of `update_javadoc_version.py` still exists:

```bash
# DESTRUCTIVE — rewrites @version AND @since on every file in scope. Review the diff carefully.
python3 scripts/update_javadoc_version.py --apply --expected v0.1.0 --scope src
```

`--apply --changed-only` is explicitly rejected by the validator because it would overwrite `@since` on every file your branch touches.

## Maintainer Tasks

### Creating a New Milestone Release

**Complete checklist** for transitioning from `v0.N.0` to `v0.N+1.0`:

1. **Pre-release validation**
   ```bash
   # No tree-wide @version action needed — the PR-diff-scoped validator
   # has been enforcing tags on every PR throughout the milestone.

   # Build (no test suite yet — see CLAUDE.md)
   mvn clean package
   ```

2. **Create new milestone**
   - GitHub UI → Milestones → New Milestone
   - Title: `v0.N+1.0`
   - Due date: target release date
   - Description: key features and goals

3. **Update documentation**
   - Move `[Unreleased]` → `[v0.N+1.0]` in `CHANGELOG.md`
   - Update README if needed

4. **Bump and tag**
   ```bash
   # Update <version> in pom.xml first
   git commit -m "chore: release v0.N+1.0"
   git tag -a v0.N+1.0 -m "Release v0.N+1.0"
   git push origin v0.N+1.0
   ```
   The `publish.yml` workflow validates and publishes to GitHub Packages on tag push.

5. **Close old milestone**
   ```bash
   gh api -X PATCH repos/OWS-PFMS/flatcomp/milestones/MILESTONE_NUMBER -f state=closed
   ```

6. **Announce**
   - Create a GitHub Release from the tag
   - Notify consumers (e.g., OWS-PFMS/OWS-Local-Search-GUI) if API shifted

### Handling Hotfixes

For urgent fixes that skip the normal milestone flow:

1. Create hotfix branch from `main`.
2. Make minimal changes.
3. Bump `@version` per the bright-line rule — hotfixes are modifications, and modifications bump `@version`. Use the existing open milestone (e.g., `v0.2.1`) or create a patch milestone (e.g., `v0.2.1`).
4. Set PR milestone before opening review.
5. Merge and tag as patch (`v0.2.1`).

## Additional Resources

- [Main Versioning Guide](./versioning.md) — Tag semantics and examples
- [CHANGELOG.md](../../CHANGELOG.md) — Project changelog
- [CONTRIBUTING.md](../../CONTRIBUTING.md) — Release process and PR conventions
- [GitHub Milestones](https://github.com/OWS-PFMS/flatcomp/milestones) — Active milestones
