---
description: Add requirements or content to existing user story
---

Add new acceptance criteria, technical notes, or other content to an existing GitHub issue in user story format.

## Usage

```
/story:author:add
/story:author:add 39
/story:author:add 39 criteria
```

**Arguments**:
- `[issue-number]`: Issue to update (optional, will prompt if not provided)
- `[section]`: Section to add to (optional, will show menu if not provided)

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
   - If body is empty: Warn user and ask to continue

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

3. **Store section mapping**:
   - Section name → content
   - Preserve original order
   - Track which sections exist

4. **Warn if non-standard format**:
   ```
   ⚠️  NOTE: Issue body doesn't match standard user story format.

   This command works best with issues created by /story:author:create.
   You can still add content, but section detection may be limited.

   Continue anyway? (yes/no)
   ```

### Step 4 - Present Section Menu

**If section argument provided**, validate and skip to Step 5.

**Otherwise, show interactive menu**:

```
Which section would you like to add to?

CURRENT SECTIONS:

1. User Story
   As a maintainer
   I want comprehensive JavaDoc documentation
   So that the code is self-documenting...

2. Current Behavior (3 items)
   - Many .java files lack complete JavaDoc
   - Class-level documentation is inconsistent
   - Method-level documentation is incomplete

3. Expected Behavior (4 items)
   - All .java files in src/ have comprehensive JavaDoc
   - Class-level JavaDoc includes version, since, author
   ...

4. Acceptance Criteria (7 items)
   - [ ] All class-level JavaDoc includes @version
   - [ ] All class-level JavaDoc includes @since
   ...

5. Technical Notes (4 items)
   - Scope: All *.java files in src/
   - Tags Required: @version, @since, @author
   ...

6. Priority
   Low - Important for long-term maintainability

7. Related Issues (1 item)
   - #21 - Related issue

8. Steps to Reproduce / Implementation Details
   [Not currently in issue]

9. [Add custom section]

Enter section number (1-9) or 'q' to quit:
```

**Menu Guidelines**:
- Show truncated preview of current content
- Number all available sections
- Show item counts for list sections
- Allow custom section creation
- Support 'q' to cancel

### Step 5 - Guide Adding Content

**Based on selected section**, guide user through appropriate input:

#### For "User Story" (1):
```
CURRENT USER STORY:
As a maintainer
I want comprehensive JavaDoc documentation
So that the code is self-documenting

Options:
1. Replace entire user story
2. Modify role (currently: maintainer)
3. Modify goal (currently: comprehensive JavaDoc...)
4. Modify benefit (currently: code is self-documenting...)
5. Cancel

Enter option (1-5):
```

#### For "Current Behavior" (2) or "Expected Behavior" (3):
```
CURRENT ITEMS:
- Many .java files lack complete JavaDoc
- Class-level documentation is inconsistent
- Method-level documentation is incomplete

What would you like to add? (Enter multiple lines, blank line to finish)
>
```

After user enters content:
```
You entered:
- New item 1
- New item 2

Add these items? (yes/no/edit):
```

#### For "Acceptance Criteria" (4):
```
CURRENT CRITERIA:
- [ ] All class-level JavaDoc includes @version
- [ ] All class-level JavaDoc includes @since
- [ ] All method-level JavaDoc includes @param

What acceptance criteria would you like to add?
(Enter multiple criteria, one per line, blank line to finish)
>
```

Automatically format as checkboxes: `- [ ] {user input}`

#### For "Technical Notes" (5):
```
CURRENT NOTES:
- Scope: All *.java files in src/
- Tags Required: @version, @since, @author

Add technical notes (bullet points, blank line to finish):
>
```

#### For "Priority" (6):
```
CURRENT PRIORITY:
Low - Important for long-term maintainability

Options:
1. Change priority level (Low/Medium/High)
2. Add to justification
3. Replace entire priority section
4. Cancel

Enter option (1-4):
```

#### For "Related Issues" (7):
```
CURRENT RELATED ISSUES:
- #21 - Related issue

Add related issue:
Issue number: 21
Relationship description: Depends on authentication work

Format:
- #{number} - {description}

Add this? (yes/no):
```

#### For "Custom Section" (9):
```
Enter section name (e.g., "Implementation Strategy", "Dependencies"):
> Migration Plan

Enter section content (markdown supported, blank line to finish):
>
```

### Step 6 - Preview Changes

1. **Reconstruct full issue body** with new content added
2. **Show side-by-side or diff view**:

```
═══════════════════════════════════════════════════════
📋 PREVIEW: Changes to Issue #{NUMBER}
═══════════════════════════════════════════════════════

SECTION: Acceptance Criteria

CURRENT (7 items):
- [ ] All class-level JavaDoc includes @version
- [ ] All class-level JavaDoc includes @since
- [ ] All public methods have JavaDoc
- [ ] Generated JavaDoc HTML is readable
- [ ] No JavaDoc warnings when running mvn javadoc:javadoc
- [ ] JavaDoc formatting is consistent
- [ ] Package-private methods have JavaDoc where needed

PROPOSED (9 items):
- [ ] All class-level JavaDoc includes @version
- [ ] All class-level JavaDoc includes @since
- [ ] All public methods have JavaDoc
- [ ] Generated JavaDoc HTML is readable
- [ ] No JavaDoc warnings when running mvn javadoc:javadoc
- [ ] JavaDoc formatting is consistent
- [ ] Package-private methods have JavaDoc where needed
- [ ] All method-level JavaDoc includes @return tags    [NEW]
- [ ] All method-level JavaDoc includes @throws tags   [NEW]

═══════════════════════════════════════════════════════

Apply these changes? (yes/no/edit):
  yes   - Update issue immediately
  no    - Cancel, keep current issue unchanged
  edit  - Make additional modifications
```

### Step 7 - Apply Changes

**If user selects "yes"**:

1. **Update issue via GitHub CLI**:
   ```bash
   gh issue edit {NUMBER} --body "$(cat <<'EOF'
   {reconstructed body with new content}
   EOF
   )"
   ```

2. **Verify update succeeded**:
   ```bash
   gh issue view {NUMBER} --json updatedAt
   ```

3. **Show success confirmation**:
   ```
   ═══════════════════════════════════════════════════════
   ✅ ISSUE UPDATED SUCCESSFULLY
   ═══════════════════════════════════════════════════════

   📋 Issue #{NUMBER}: {Title}
   🔗 URL: {url}

   CHANGES APPLIED:
   - Added 2 new acceptance criteria
   - Section: Acceptance Criteria

   ═══════════════════════════════════════════════════════

   View updated issue:  gh issue view {NUMBER}
   Open in browser:     gh issue view {NUMBER} --web
   ```

**If user selects "no"**:
- Cancel operation
- Show: "No changes made to issue #{NUMBER}"
- Exit

**If user selects "edit"**:
- Return to Step 4 (section menu)
- Allow additional modifications
- Accumulate all changes before final preview

### Step 8 - Optional: Add More Content

After successful update, ask:
```
Add more content to this issue? (yes/no):
```

- If yes: Return to Step 4 (section menu)
- If no: Exit with success message

## Examples

### Example 1: Add acceptance criteria to issue #39
```
> /story:author:add 39

Fetching issue #39...

═══════════════════════════════════════════════════════
📋 ISSUE #39: Add comprehensive JavaDoc documentation
═══════════════════════════════════════════════════════

Which section would you like to add to?
...
4. Acceptance Criteria (7 items)
...

> 4

What acceptance criteria would you like to add?
> All @throws tags document checked exceptions
> Constructor JavaDoc includes purpose and parameter descriptions
>

You entered:
- [ ] All @throws tags document checked exceptions
- [ ] Constructor JavaDoc includes purpose and parameter descriptions

Add these criteria? (yes/no/edit): yes

[Preview shown]

Apply these changes? (yes/no/edit): yes

✅ Issue updated successfully!
```

### Example 2: Add technical notes
```
> /story:author:add 39

> 5 (Technical Notes)

Add technical notes (bullet points, blank line to finish):
> Verify with Checkstyle: mvn checkstyle:check
> Consider using Maven JavaDoc plugin configuration
>

Add these notes? yes

[Preview and confirmation]

✅ Issue updated successfully!

Add more content to this issue? no
```

## Important Notes

- **Preserves formatting**: Original markdown formatting is maintained
- **Non-destructive**: Only adds content, doesn't remove existing items
- **Preview required**: Always shows preview before updating
- **Section detection**: Works best with /story:author:create formatted issues
- **Multiple additions**: Can add to multiple sections in one session
- **Validation**: Checks issue exists before prompting for content

## Safety

- Never update issue without user confirmation
- Always show preview of changes
- Validate issue number before fetching
- Handle malformed issue bodies gracefully
- Provide clear error messages
- Allow cancellation at any step

## Error Handling

**Issue not found**:
```
❌ ERROR: Issue #999 not found

Please verify the issue number and try again.
List open issues: gh issue list --state open
```

**API failure**:
```
❌ ERROR: GitHub API request failed

Command: gh issue view 39
Error: HTTP 500: Internal Server Error

Please try again in a moment.
```

**Invalid section number**:
```
❌ ERROR: Invalid selection

You entered: 12
Valid range: 1-9

Please try again or enter 'q' to quit.
```

**Empty input**:
```
⚠️  WARNING: No content entered

You didn't enter any content to add.

Options:
1. Try again
2. Cancel

Enter option (1-2):
```

## Related Commands

- `/story:author:create` - Create new user story issue
- `/story:author:remove` - Remove content from user story
- `/story:author:update` - Menu router for update operations
- `/story:author:refactor` - Split epic into multiple stories

## Reference

See `.claude/commands/_PATTERNS.md` for implementation patterns.
