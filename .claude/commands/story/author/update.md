---
description: Update existing user story (menu router)
---

Update an existing GitHub issue with guided workflows for adding, removing, or refactoring content.

## Usage

```
/story:author:update
/story:author:update 39
/story:author:update 39 add
/story:author:update 39 remove
```

**Arguments**:
- `[issue-number]`: Issue to update (optional, will prompt if not provided)
- `[mode]`: Update mode - add, remove, or refactor (optional, will show menu if not provided)

## Overview

This is a **router command** that presents a menu of update operations and delegates to specialized commands:

- **add**: Add acceptance criteria, technical notes, or requirements → `/story:author:add`
- **remove**: Remove sections or criteria → `/story:author:remove`
- **refactor**: Split epic into multiple stories → `/story:author:refactor`

Think of this as a convenience wrapper - power users can skip this and call the specific commands directly.

## Workflow

### Step 1 - Select Issue

**If issue number provided**:
1. Validate it's a valid issue number
2. Show brief summary
3. Continue to Step 2

**If no issue number provided**:
1. Run: `gh issue list --state open --limit 20`
2. Display list to user
3. Ask: "Which issue number would you like to update?"
4. Wait for user response

### Step 2 - Fetch Issue Details

1. **Fetch issue data**:
   ```bash
   gh issue view {NUMBER} --json number,title,body,labels,milestone,url
   ```

2. **Display issue summary**:
   ```
   ═══════════════════════════════════════════════════════
   📋 ISSUE #{NUMBER}: {Title}
   ═══════════════════════════════════════════════════════

   🏷️  Labels: {labels}
   📅 Milestone: {milestone}
   🔗 URL: {url}

   {Brief summary of issue - first 3 lines of body}
   ...
   ```

3. **Error handling**:
   - If issue not found: Report error and exit
   - If API fails: Show error and suggest retry

### Step 3 - Present Update Mode Menu

**If mode argument provided** (add/remove/refactor), validate and skip to Step 4.

**Otherwise, show menu**:

```
═══════════════════════════════════════════════════════
🔧 UPDATE OPTIONS
═══════════════════════════════════════════════════════

How would you like to update this issue?

1. ADD - Add new content
   Add acceptance criteria, technical notes, expected behaviors,
   or other requirements to existing sections.

   Use when: Expanding scope, adding forgotten requirements,
   incorporating feedback from code review.

2. REMOVE - Remove existing content
   Remove acceptance criteria, technical notes, or other items
   from sections. Can remove individual items or entire sections.

   Use when: Reducing scope, removing duplicate items, cleaning
   up outdated requirements.

3. REFACTOR - Split epic into multiple stories
   Break down a large epic into smaller, focused user stories.
   Creates multiple new issues with sub-issue relationships.

   Use when: Issue is too large, covers multiple features,
   or needs better organization.

4. CANCEL - Don't update

═══════════════════════════════════════════════════════

Enter option (1-4):
```

**Mode Selection Guidelines**:
- Provide clear descriptions of each mode
- Give usage examples for each
- Number options for easy selection
- Support aliases (add/remove/refactor/cancel)
- Support 'q' to quit

### Step 4 - Route to Specialized Command

Based on selected mode, **invoke the appropriate command**:

#### Mode: ADD (1)
```
Routing to /story:author:add...

═══════════════════════════════════════════════════════
📝 ADD CONTENT TO USER STORY
═══════════════════════════════════════════════════════
```

**Then execute**: `/story:author:add {NUMBER}`

This delegates to the full `/story:author:add` workflow with:
- Section menu presentation
- Guided content addition
- Preview and confirmation
- Issue update

#### Mode: REMOVE (2)
```
Routing to /story:author:remove...

═══════════════════════════════════════════════════════
✂️  REMOVE CONTENT FROM USER STORY
═══════════════════════════════════════════════════════
```

**Then execute**: `/story:author:remove {NUMBER}`

This delegates to the full `/story:author:remove` workflow with:
- Section and item selection
- Confirmation prompts
- Preview and validation
- Issue update

#### Mode: REFACTOR (3)
```
Routing to /story:author:refactor...

═══════════════════════════════════════════════════════
🔀 REFACTOR EPIC INTO MULTIPLE STORIES
═══════════════════════════════════════════════════════
```

**Then execute**: `/story:author:refactor {NUMBER}`

This delegates to the full `/story:author:refactor` workflow with:
- Parent issue validation and suitability analysis
- Setting parent type to "Epic" (GitHub native Issue Type)
- Interactive sub-story identification
- Native parent-child sub-issue linking via GraphQL
- Label and milestone inheritance
- Automatic project board add with parent-status inheritance

#### Mode: CANCEL (4)
```
No changes made to issue #{NUMBER}.
```
Exit without any operations.

## Examples

### Example 1: Router with no arguments (full interactive)
```
> /story:author:update

Fetching open issues...

[Shows list of open issues]

Which issue? 39

[Fetches and displays issue #39]

How would you like to update?
1. ADD
2. REMOVE
3. REFACTOR
4. CANCEL

> 1

[Routes to /story:author:add 39]
```

### Example 2: With issue number only
```
> /story:author:update 39

[Fetches and displays issue #39]

How would you like to update?
1. ADD
2. REMOVE
...

> 2

[Routes to /story:author:remove 39]
```

### Example 3: With issue number and mode (skip menu)
```
> /story:author:update 39 add

[Immediately routes to /story:author:add 39]
```

### Example 4: Direct command (skip router entirely)
```
> /story:author:add 39

[Directly executes add workflow - fastest for power users]
```

## When to Use This Command

**Use `/story:author:update` when**:
- You're not sure which operation you need
- You want to see all update options
- You're new to the userstory command API
- You want a guided experience

**Skip it and use direct commands when**:
- You know exactly what you need (e.g., `/story:author:add 39`)
- You're a power user comfortable with the API
- You want the fastest path to your goal
- You're scripting or automating workflows

## Command Relationships

```
/story:author:update (ROUTER)
    ├── /story:author:add (Delegated: add content)
    ├── /story:author:remove (Delegated: remove content)
    └── /story:author:refactor (Delegated: split epic)
```

This router provides **discoverability** while specialized commands provide **efficiency**.

## Important Notes

- **Lightweight**: This command just routes, doesn't do the actual work
- **Optional**: Can always use specialized commands directly
- **Consistent UX**: Maintains same interaction patterns
- **Mode shortcuts**: Supports both numbers (1, 2, 3) and names (add, remove, refactor)
- **Exit anytime**: Can cancel at any step

## Safety

- No destructive operations in router itself
- All safety checks handled by specialized commands
- Can cancel before any changes are made
- Clear routing messages show which command is being invoked

## Error Handling

**Issue not found**:
```
❌ ERROR: Issue #999 not found

Please verify the issue number.
List open issues: gh issue list --state open
```

**Invalid mode**:
```
❌ ERROR: Invalid mode

You entered: modify
Valid modes: add, remove, refactor

Try: /story:author:update 39 add
```

**API failure**:
```
❌ ERROR: GitHub API request failed

Please try again in a moment.
```

## Related Commands

### User Story API (all commands):
- `/story:author:create` - Create new user story issue
- `/story:author:add` - Add content to user story
- `/story:author:remove` - Remove content from user story
- `/story:author:update` - **Router** (this command)
- `/story:author:refactor` - Split epic into multiple stories

### Story Implementation API (different purpose):
- `/story:start` - Begin code work on issue
- `/story:implement` - Autonomous implementation
- `/story:close` - Finish work and create PR

## Design Philosophy

This router follows the **progressive disclosure** principle:
1. Start simple: Show what's possible
2. Get specific: Narrow to user's intent
3. Delegate work: Hand off to specialized tool

Power users can skip steps 1-2 by using specialized commands directly.

## Reference

See `.claude/commands/_PATTERNS.md` for implementation patterns.
