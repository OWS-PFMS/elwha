---
description: Remove content from existing user story
---

Remove acceptance criteria, technical notes, or other content from an existing GitHub issue in user story format.

## Usage

```
/story:author:remove
/story:author:remove 39
/story:author:remove 39 criteria
```

**Arguments**:
- `[issue-number]`: Issue to update (optional, will prompt if not provided)
- `[section]`: Section to remove from (optional, will show menu if not provided)

## Workflow

### Step 1 - Select Issue

**If issue number provided**:
1. Validate it's a valid issue number
2. Show brief summary and ask for confirmation
3. Continue to Step 2

**If no issue number provided**:
1. Run: `gh issue list --state open --limit 20`
2. Display list to user
3. Ask: "Which issue number would you like to update?"
4. Wait for user response

### Step 2 - Fetch Issue Details

1. **Fetch full issue data**:
   ```bash
   gh issue view {NUMBER} --json number,title,body,labels,milestone,url
   ```

2. **Parse JSON output** to extract:
   - Issue number, title, URL
   - Current body (full markdown)
   - Labels and milestone

3. **Display current issue summary**:
   ```
   ═══════════════════════════════════════════════════════
   📋 ISSUE #{NUMBER}: {Title}
   ═══════════════════════════════════════════════════════

   🏷️  Labels: {labels}
   📅 Milestone: {milestone}
   🔗 URL: {url}
   ```

4. **Error handling**:
   - If issue not found: Report error and exit
   - If API fails: Show error and suggest retry
   - If body is empty: Report error and exit (nothing to remove)

### Step 3 - Parse Issue Body Structure

1. **Split body into sections** using `## ` as delimiter

2. **Expected sections** (from _PATTERNS.md):
   - User Story
   - Current Behavior
   - Expected Behavior
   - Steps to Reproduce / Acceptance Criteria
   - Technical Notes
   - Acceptance Criteria
   - Priority
   - Related Issues

3. **Extract items from each section**:
   - Bullet points (`- ` or `* `)
   - Checkboxes (`- [ ]` or `- [x]`)
   - Numbered lists (`1.`, `2.`, etc.)
   - Paragraphs (for prose sections)

4. **Store item mapping**:
   - Section name → list of items
   - Preserve original formatting
   - Track item positions

5. **Warn if non-standard format**:
   ```
   ⚠️  NOTE: Issue body doesn't match standard user story format.

   This command works best with issues created by /story:author:create.
   Section detection may be limited.

   Continue anyway? (yes/no)
   ```

### Step 4 - Present Section Menu

**If section argument provided**, validate and skip to Step 5.

**Otherwise, show interactive menu**:

```
Which section would you like to remove from?

CURRENT SECTIONS:

1. Current Behavior (3 items)
   [1] Many .java files lack complete JavaDoc
   [2] Class-level documentation is inconsistent
   [3] Method-level documentation is incomplete

2. Expected Behavior (4 items)
   [1] All .java files in src/ have comprehensive JavaDoc
   [2] Class-level JavaDoc includes version, since, author
   [3] Method-level JavaDoc includes param, return, throws
   [4] Documentation follows standard JavaDoc conventions

3. Acceptance Criteria (7 items)
   [1] All class-level JavaDoc includes @version
   [2] All class-level JavaDoc includes @since
   [3] All class-level JavaDoc includes @author
   [4] All public methods have JavaDoc
   [5] Generated JavaDoc HTML is readable
   [6] No JavaDoc warnings when running mvn javadoc:javadoc
   [7] Package-private methods have JavaDoc where needed

4. Technical Notes (4 items)
   [1] Scope: All *.java files in src/
   [2] Tags Required: @version, @since, @author
   [3] Build Integration: mvn javadoc:javadoc
   [4] Estimate: Comprehensive task covering entire codebase

5. Related Issues (1 item)
   [1] #21 - Related issue

6. User Story (cannot remove - core section)

7. Priority (cannot remove - core section)

8. [Remove entire section]

Enter section number (1-8) or 'q' to quit:
```

**Menu Guidelines**:
- Number all items within sections
- Show item counts
- Mark core sections as non-removable (can only modify)
- Allow entire section removal for non-core sections
- Support 'q' to cancel

### Step 5 - Select Items to Remove

**Based on selected section**, present items for removal:

#### For List Sections (Current Behavior, Expected Behavior, Technical Notes, etc.):

```
SECTION: Acceptance Criteria

Select items to remove (comma-separated numbers, ranges OK):

[1] All class-level JavaDoc includes @version
[2] All class-level JavaDoc includes @since
[3] All class-level JavaDoc includes @author Charles Bryan
[4] All public methods have JavaDoc with @param, @return, @throws
[5] Generated JavaDoc HTML is readable
[6] No JavaDoc warnings when running mvn javadoc:javadoc
[7] Package-private methods have JavaDoc where complexity warrants

Examples:
  3       - Remove item 3
  1,4,7   - Remove items 1, 4, and 7
  2-5     - Remove items 2 through 5
  all     - Remove all items (delete section)
  q       - Cancel

Enter selection:
```

**Parse user input**:
- Single number: `3` → remove item 3
- Comma-separated: `1,4,7` → remove items 1, 4, 7
- Ranges: `2-5` → remove items 2, 3, 4, 5
- Combined: `1,3-5,7` → remove items 1, 3, 4, 5, 7
- Special: `all` → remove entire section
- Special: `q` → cancel operation

**Validation**:
- Check all numbers are within valid range
- Reject invalid input with clear error
- Confirm if user selects "all"

#### For Related Issues:

```
SECTION: Related Issues

Current related issues:
[1] #21 - Related issue (context for documentation effort)
[2] #15 - Testing strategy
[3] #14 - Event system migration

Enter issue numbers to remove (comma-separated):
```

#### For Core Sections (User Story, Priority):

```
SECTION: User Story (Core Section)

Core sections cannot be removed, but you can modify them.

Current:
As a maintainer
I want comprehensive JavaDoc documentation
So that the code is self-documenting

Options:
1. Use /story:author:add to modify this section
2. Choose different section
3. Cancel

Enter option (1-3):
```

### Step 6 - Confirm Removal

Before proceeding, confirm with user:

```
═══════════════════════════════════════════════════════
⚠️  CONFIRM REMOVAL
═══════════════════════════════════════════════════════

SECTION: Acceptance Criteria

ITEMS TO REMOVE:
✗ [3] All class-level JavaDoc includes @author Charles Bryan
✗ [7] Package-private methods have JavaDoc where complexity warrants

REMAINING ITEMS (5):
✓ [1] All class-level JavaDoc includes @version
✓ [2] All class-level JavaDoc includes @since
✓ [4] All public methods have JavaDoc with @param, @return, @throws
✓ [5] Generated JavaDoc HTML is readable
✓ [6] No JavaDoc warnings when running mvn javadoc:javadoc

═══════════════════════════════════════════════════════

⚠️  This action cannot be undone (but you can re-add items later).

Proceed with removal? (yes/no):
```

### Step 7 - Preview Changes

Show full preview of updated issue body:

```
═══════════════════════════════════════════════════════
📋 PREVIEW: Changes to Issue #{NUMBER}
═══════════════════════════════════════════════════════

SECTION: Acceptance Criteria

BEFORE (7 items):
- [ ] All class-level JavaDoc includes @version
- [ ] All class-level JavaDoc includes @since
- [ ] All class-level JavaDoc includes @author Charles Bryan
- [ ] All public methods have JavaDoc with @param, @return, @throws
- [ ] Generated JavaDoc HTML is readable
- [ ] No JavaDoc warnings when running mvn javadoc:javadoc
- [ ] Package-private methods have JavaDoc where complexity warrants

AFTER (5 items):
- [ ] All class-level JavaDoc includes @version
- [ ] All class-level JavaDoc includes @since
- [ ] All public methods have JavaDoc with @param, @return, @throws
- [ ] Generated JavaDoc HTML is readable
- [ ] No JavaDoc warnings when running mvn javadoc:javadoc

REMOVED:
✗ All class-level JavaDoc includes @author Charles Bryan
✗ Package-private methods have JavaDoc where complexity warrants

═══════════════════════════════════════════════════════

Apply these changes? (yes/no):
  yes  - Update issue immediately
  no   - Cancel, keep current issue unchanged
```

### Step 8 - Apply Changes

**If user confirms "yes"**:

1. **Reconstruct issue body** with selected items removed

2. **Update issue via GitHub CLI**:
   ```bash
   gh issue edit {NUMBER} --body "$(cat <<'EOF'
   {reconstructed body with items removed}
   EOF
   )"
   ```

3. **Verify update succeeded**:
   ```bash
   gh issue view {NUMBER} --json updatedAt
   ```

4. **Show success confirmation**:
   ```
   ═══════════════════════════════════════════════════════
   ✅ ISSUE UPDATED SUCCESSFULLY
   ═══════════════════════════════════════════════════════

   📋 Issue #{NUMBER}: {Title}
   🔗 URL: {url}

   CHANGES APPLIED:
   - Removed 2 items from Acceptance Criteria
   - 5 acceptance criteria remain

   ═══════════════════════════════════════════════════════

   View updated issue:  gh issue view {NUMBER}
   Open in browser:     gh issue view {NUMBER} --web
   ```

**If user cancels**:
- No changes made
- Show: "No changes made to issue #{NUMBER}"
- Exit

### Step 9 - Optional: Remove More Content

After successful update, ask:

```
Remove more content from this issue? (yes/no):
```

- If yes: Return to Step 4 (section menu)
- If no: Exit with success message

## Examples

### Example 1: Remove specific acceptance criteria
```
> /story:author:remove 39

Fetching issue #39...

═══════════════════════════════════════════════════════
📋 ISSUE #39: Add comprehensive JavaDoc documentation
═══════════════════════════════════════════════════════

Which section would you like to remove from?

3. Acceptance Criteria (7 items)
   [1] All class-level JavaDoc includes @version
   [2] All class-level JavaDoc includes @since
   ...

> 3

Select items to remove: 3,7

⚠️  CONFIRM REMOVAL
ITEMS TO REMOVE:
✗ [3] All class-level JavaDoc includes @author
✗ [7] Package-private methods have JavaDoc

Proceed? yes

[Preview shown]

Apply changes? yes

✅ Issue updated successfully!
```

### Example 2: Remove technical note
```
> /story:author:remove 39

> 4 (Technical Notes)

[1] Scope: All *.java files in src/
[2] Tags Required: @version, @since, @author
[3] Build Integration: mvn javadoc:javadoc
[4] Estimate: Comprehensive task

Enter selection: 4

[Confirmation and preview]

Apply changes? yes

✅ Removed 1 item from Technical Notes
```

### Example 3: Remove entire section
```
> /story:author:remove 39

> 8 (Remove entire section)

Which section to remove entirely?
1. Steps to Reproduce
2. Implementation Details
3. Cancel

> 1

⚠️  Remove entire "Steps to Reproduce" section?
This will delete all 5 items in this section.

Confirm? yes

[Preview shown]

Apply changes? yes

✅ Section removed successfully!
```

## Important Notes

- **Destructive operation**: Removed content is lost (but can be re-added)
- **Preview required**: Always shows preview before updating
- **Confirmation required**: Asks twice (selection + preview)
- **Range support**: Can remove multiple items at once
- **Core sections**: User Story and Priority cannot be removed
- **Empty sections**: Sections with all items removed are deleted
- **Validation**: Checks item numbers are valid before removing

## Safety

- Never remove content without explicit confirmation
- Show clear preview of what will be removed
- Ask twice before removing (selection + preview)
- Validate item numbers before proceeding
- Provide clear undo guidance (use /story:author:add to restore)
- Allow cancellation at any step

## Error Handling

**Issue not found**:
```
❌ ERROR: Issue #999 not found

Please verify the issue number and try again.
```

**Empty issue body**:
```
❌ ERROR: Issue #39 has no content to remove

The issue body is empty or contains only a title.
```

**Invalid item selection**:
```
❌ ERROR: Invalid item number

You entered: 12
Valid range for this section: 1-7

Please try again or enter 'q' to quit.
```

**Invalid range syntax**:
```
❌ ERROR: Invalid range syntax

You entered: 3-1
Ranges must be ascending (e.g., 1-3, not 3-1)

Please try again.
```

**Attempt to remove core section**:
```
❌ ERROR: Cannot remove core section

The "User Story" section is required and cannot be removed.
You can modify it using /story:author:add.

Please select a different section.
```

## Related Commands

- `/story:author:create` - Create new user story issue
- `/story:author:add` - Add content to user story
- `/story:author:update` - Menu router for update operations
- `/story:author:refactor` - Split epic into multiple stories

## Reference

See `.claude/commands/_PATTERNS.md` for implementation patterns.
