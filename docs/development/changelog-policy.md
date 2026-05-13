# CHANGELOG Update Policy

This document defines when and how to update [CHANGELOG.md](../../CHANGELOG.md) during issue implementation.

## Table of Contents
- [Overview](#overview)
- [When to Update CHANGELOG](#when-to-update-changelog)
- [Entry Format](#entry-format)
- [Workflow Integration](#workflow-integration)
- [Examples](#examples)
- [FAQ](#faq)

## Overview

The project CHANGELOG documents **user-visible changes** at a high level. It follows the [Keep a Changelog](https://keepachangelog.com/) format and complements the detailed git commit history.

**Purpose:**
- Inform users about new features, changes, and fixes
- Provide release notes for version announcements
- Track project evolution at a feature/phase level

**NOT for:**
- Internal refactorings with no user impact
- Individual bug fixes that don't affect user workflows
- Test-only or documentation-only changes
- Line-by-line code changes (use git history instead)

## When to Update CHANGELOG

### Required ✅

CHANGELOG entry is **REQUIRED** if the change:
- Introduces a new user-visible feature or public API surface (FlatComp is a library — every new public class, method, or UIManager key counts)
- Renames or removes existing public API
- Changes default visual behavior (theming, sizing, hover/press/select states, etc.)
- Fixes a bug that consumers could observe (e.g., layout glitch, theming bug, NPE in public API)

Existing labels that align with this:
- `enhancement` — usually required (new public API surface)
- `user-experience` — usually required (visual / interaction defaults shifted)
- `bug` — required if user-observable; skip if purely internal

### Recommended 💡

CHANGELOG entry is **RECOMMENDED** if the change:
- Improves measurable performance in a way consumers would notice
- Adjusts JavaDoc that fixes consumer-visible misinformation

**Decision criteria:** Would a consumer of the library notice this change? If yes, document it.

### Skip ⏭️

CHANGELOG entry is **NOT NEEDED** if the change is:
- Internal refactoring with no public-API or visual delta (`technical-debt`)
- Documentation-only (`documentation`) unless the doc fix corrects misinformation consumers depend on
- Build / CI infrastructure (`developer-experience`)
- Playground or showcase tweaks (consumers don't import them)

**Examples of skip-worthy changes:**
- Fixed NPE in a package-private helper
- Reorganized playground source files
- Updated internal JavaDoc that doesn't describe public API
- Refactored implementation without changing rendered output or public signatures

## Entry Format

Follow [Keep a Changelog](https://keepachangelog.com/) format.

### Categories

Use these standard categories in order:

1. **Added** - New features, capabilities, or APIs
2. **Changed** - Changes to existing functionality
3. **Deprecated** - Features marked for future removal
4. **Removed** - Removed features or APIs
5. **Fixed** - Bug fixes (only user-visible bugs)
6. **Security** - Security vulnerability fixes

### Template

```markdown
### {Category}
- **{Brief Title}** (#{ISSUE_NUMBER}): {User-facing description in 1-2 sentences}
```

**Guidelines:**
- **Brief Title**: 3-5 words summarizing the change
- **Issue Number**: Always include `(#{N})` for traceability
- **Description**: What the user gains, not how it's implemented
- Use active voice: "Adds FlatChip component" not "FlatChip component was added"
- Focus on benefits: "Reduces load time by 40%" not "Optimized algorithm"

### Version Sections

**During development:** Add entries under `## [Unreleased]` section.

```markdown
## [Unreleased]

### Added
- **FlatChip component** (#N): Replaces FlatPill for Material chip taxonomy alignment.
```

**At release time:** Rename `[Unreleased]` to the version number per the release process in [CONTRIBUTING.md](../../CONTRIBUTING.md#release-process):

```markdown
## [0.2.0] - 2026-06-15

### Added
- **FlatChip component** (#N): Replaces FlatPill for Material chip taxonomy alignment.
```

## Workflow Integration

### When to Update

Update CHANGELOG **before creating the PR** as part of `/story:close`:

1. After all commits are complete
2. After tests pass
3. Before running `gh pr create`

This ensures:
- CHANGELOG is included in the PR diff
- Reviewers can verify the entry
- No separate documentation PR needed

### Commit Format

Commit CHANGELOG changes separately with this format:

```bash
git add CHANGELOG.md
git commit -m "docs(issue-{N}): update CHANGELOG for {category}

Added {category} entry documenting {brief description}.

Related to #{ISSUE_NUMBER}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

Example:
```bash
git commit -m "docs(issue-18): update CHANGELOG for Added

Added 'Added' entry documenting FlatChip component.

Related to #18

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

## Examples

### Good Entries ✅

**Example 1: New Feature**
```markdown
### Added
- **FlatChip component** (#N): Compact pill replacement aligned with Material's chip taxonomy. Drop-in for `FlatPill` with the same UIManager keys under the `FlatChip.*` namespace.
```

Why good:
- Names the new public surface (`FlatChip`)
- Tells consumers what they can do with it
- Mentions theming/migration nuance

**Example 2: Breaking API Change**
```markdown
### Changed
- **FlatList<T> selection surface** (#N): `FlatCardList` and `FlatPillList` now share a single `ListSelectionModel<T>` via `FlatList<T>`. **Breaking:** `CardSelectionModel` / `PillSelectionModel` removed — migrate to `ListSelectionModel<T>` (sed-replaceable for most consumers).
```

Why good:
- Clearly marked: "**Breaking:**"
- Explains migration cost in one phrase
- Names the symbols consumers actually import

**Example 3: Bug Fix**
```markdown
### Fixed
- **FlatPillList drag-reorder cursor flicker** (#N): The grab cursor no longer flickers back to default between rows during a drag.
```

Why good:
- Describes the consumer-visible symptom
- Avoids implementation details

**Example 4: Theming Change**
```markdown
### Changed
- **FlatCard default arc** (#N): Default `FlatCard.arc` lowered from 12 to 8 to match FlatLaf 3.x card defaults. Override `UIManager.put("FlatCard.arc", 12)` to restore previous behavior.
```

Why good:
- Names the UIManager key (`FlatCard.arc`)
- Gives the exact override needed to revert

### Bad Entries ❌

**Example 1: Too Technical**
```markdown
### Fixed
- Fixed NPE in FlatPillList.java line 142 (#N)
```

Why bad:
- Internal implementation detail (file name, line number)
- No consumer-visible context

**Better version:**
```markdown
### Fixed
- **FlatPillList NPE on empty model** (#N): `FlatPillList` no longer throws NPE when constructed with an empty `PillListModel`.
```

**Example 2: Too Vague**
```markdown
### Changed
- Improved error handling (#N)
```

Why bad:
- No specifics about what improved
- Doesn't name the affected API

**Better version:**
```markdown
### Changed
- **FlatCard.setHeader IllegalArgumentException** (#N): `FlatCard.setHeader` now throws `IllegalArgumentException` (was: silent no-op) when both title and subtitle are null.
```

**Example 3: Internal Refactoring**
```markdown
### Changed
- Refactored FlatPill painter to share base class with FlatCard (#N)
```

Why bad:
- No public-API or visual delta
- Belongs in commit messages only

**Should be skipped** (no CHANGELOG entry unless rendered output changed).

## FAQ

### Q: Multiple issues in one PR?

If a PR addresses multiple issues, add separate CHANGELOG entries for each:

```markdown
### Added
- **FlatChip component** (#N): ...
- **FlatChipList<T>** (#M): ...
```

### Q: Issue spans multiple milestones?

Add the entry under `[Unreleased]` when the issue is complete, regardless of when work started.

### Q: CHANGELOG merge conflicts?

**During development:**
1. Always work in `[Unreleased]` section
2. When merging, accept both changes
3. Sort entries within each category (optional)
4. Remove duplicates if any

**At release time:**
The maintainer will resolve conflicts when cutting a release.

### Q: Should I update CHANGELOG for hotfixes?

**Critical hotfixes:** Yes, add to `## [Unreleased]` even if released immediately.

**Minor hotfixes:** Use judgment - if user-visible, add entry.

### Q: What if I'm unsure?

When in doubt:
1. Check issue labels first
2. Ask: "Would a user notice this change?"
3. If still unsure, ask in PR or skip (maintainer can add later)

**Err on the side of over-documenting** - easier to remove than add later.

### Q: Difference between CHANGELOG and JavaDoc @version?

- **CHANGELOG.md**: User-facing feature/fix documentation
- **JavaDoc @version**: Developer-facing code version tracking

They serve different audiences and should both be maintained per [versioning.md](./versioning.md).

### Q: Can I use automated changelog generation?

Not currently. The CHANGELOG is curated to focus on **user impact**, not every commit. Automation tools like `conventional-changelog` generate too much noise.

## Related Documentation

- [CHANGELOG.md](../../CHANGELOG.md) — The project changelog
- [CONTRIBUTING.md](../../CONTRIBUTING.md#release-process) — Release process (when `[Unreleased]` becomes `[X.Y.Z]`)
- [versioning.md](./versioning.md) — JavaDoc `@version` / `@since` tagging guide
- [Keep a Changelog](https://keepachangelog.com/) — External specification
