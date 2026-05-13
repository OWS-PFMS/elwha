---
description: Split epic into multiple sub-issues with parent-child relationships
---

Split a large Epic issue into multiple focused sub-issues using GitHub's native Issue Types and sub-issue features.

## Usage

```
/story:author:refactor
/story:author:refactor 41
/story:author:refactor issue-41
```

**Arguments**:
- `[issue-number]`: Epic issue to refactor (optional, will prompt if not provided)

## Overview

This command helps you break down large, complex issues into manageable sub-issues with proper parent-child relationships. It uses GitHub's native Epic issue type and sub-issue API to:

- Set parent issue type to "Epic"
- Guide you through identifying logical sub-stories
- Create multiple sub-issues with appropriate types (Feature/Task/Bug)
- Automatically link sub-issues to parent with native relationships
- Inherit labels and milestone from parent
- Enable automatic progress tracking

**When to use**:
- Issue has 7+ acceptance criteria
- Issue spans multiple features or components
- Issue scope is too large for single PR
- Want to parallelize work across team members

## Prerequisites

**Required**:
- Repository must have Issue Types enabled (public preview feature)
- Repository must have Sub-Issues API enabled (public preview feature)
- User must have write access to repository

**How to verify**:
```bash
# Check if Issue Types are available
gh api graphql -H "GraphQL-Features: issue_types" -f query='
  query {
    repository(owner: "OWS-PFMS", name: "flatcomp") {
      issueTypes(first: 5) { nodes { name } }
    }
  }'
```

If you see an error about `issueTypes` not existing, the feature is not enabled. Contact repository administrator.

## Workflow

### Step 1 - Select Issue

**If issue number provided**:
1. Validate it's a valid issue number
2. Show brief summary
3. Continue to Step 2

**If no issue number provided**:
1. Run: `gh issue list --state open --limit 20`
2. Display list to user
3. Ask: "Which issue number would you like to refactor?"
4. Wait for user response

### Step 2 - Fetch Issue Details

1. **Fetch full issue data**:
   ```bash
   gh issue view {NUMBER} --json number,title,body,labels,milestone,url,state
   ```

2. **Get issue node ID** (required for GraphQL):
   ```bash
   ISSUE_NODE_ID=$(gh api repos/OWS-PFMS/flatcomp/issues/{NUMBER} --jq '.node_id')
   ```

3. **Display current issue summary**:
   ```
   ═══════════════════════════════════════════════════════
   📋 ISSUE #{NUMBER}: {Title}
   ═══════════════════════════════════════════════════════

   🏷️  Labels: {labels}
   📅 Milestone: {milestone}
   🔗 URL: {url}

   {First 5 lines of body...}
   ```

4. **Error handling**:
   - If issue not found: Report error and exit
   - If issue is closed: Warn and ask to continue
   - If API fails: Show error and suggest retry

### Step 3 - Validate Issue Suitability

1. **Analyze issue complexity**:
   - Count acceptance criteria (lines starting with `- [ ]`)
   - Count body length (characters)
   - Count major sections (lines starting with `## `)

2. **Display suitability assessment**:
   ```
   ISSUE ANALYSIS:
   - Acceptance Criteria: 12 items
   - Body Length: 2,450 characters
   - Major Sections: 6

   ASSESSMENT: ✓ Good candidate for Epic refactoring

   Reasons:
   - Large number of acceptance criteria (12)
   - Comprehensive scope across multiple sections
   - Could benefit from parallel work streams
   ```

3. **Ask for confirmation**:
   ```
   Convert this issue to an Epic with sub-issues? (yes/no):
   ```

4. **If user confirms**, proceed to Step 4
5. **If user declines**, exit gracefully

### Step 4 - Verify Issue Types Enabled

**Repository Constants**:
```bash
OWNER="OWS-PFMS"
REPO="flatcomp"
REPO_ID="R_kgDOSb1IHA"
```

1. **Query available Issue Types**:
   ```bash
   ISSUE_TYPES_RESPONSE=$(gh api graphql -H "GraphQL-Features: issue_types" -f query='
     query {
       repository(owner: "'$OWNER'", name: "'$REPO'") {
         id
         issueTypes(first: 10) {
           nodes {
             id
             name
             description
           }
         }
       }
     }' 2>&1)
   ```

2. **Check for errors**:
   ```bash
   if [[ $ISSUE_TYPES_RESPONSE == *"Field 'issueTypes' doesn't exist"* ]]; then
     echo "❌ ERROR: Issue Types not enabled in this repository"
     echo ""
     echo "Issue Types are in public preview (as of January 2025)."
     echo "This feature must be enabled by a repository administrator."
     echo ""
     echo "To enable Issue Types:"
     echo "1. Go to repository Settings"
     echo "2. Navigate to Features → Issues"
     echo "3. Enable 'Issue Types' if available"
     echo ""
     echo "Alternative: Manually create issues and use 'Related to #${ISSUE_NUMBER}' in body"
     exit 1
   fi
   ```

3. **Extract Issue Type IDs**:
   ```bash
   EPIC_TYPE_ID=$(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[] | select(.name == "Epic") | .id')
   FEATURE_TYPE_ID=$(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[] | select(.name == "Feature") | .id')
   TASK_TYPE_ID=$(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[] | select(.name == "Task") | .id')
   BUG_TYPE_ID=$(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[] | select(.name == "Bug") | .id')
   REFACTOR_TYPE_ID=$(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[] | select(.name == "Refactor") | .id')
   RESEARCH_TYPE_ID=$(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[] | select(.name == "Research") | .id')
   ```

   **Cached ID (shortcut to skip the lookup):** `Epic` → `IT_kwDOBlhJXs4BxFMl`

4. **Verify Epic type exists**:
   ```bash
   if [[ -z "$EPIC_TYPE_ID" ]]; then
     echo "⚠️  WARNING: 'Epic' issue type not found"
     echo "Available types: $(echo $ISSUE_TYPES_RESPONSE | jq -r '.data.repository.issueTypes.nodes[].name' | paste -sd, -)"
     echo ""
     echo "Cannot proceed without Epic type. Please configure in repository settings."
     exit 1
   fi
   ```

5. **Query parent issue's project status**:
   ```bash
   # Get parent issue's project item and status
   PARENT_PROJECT_RESPONSE=$(gh api graphql -f query='
     query {
       node(id: "'$ISSUE_NODE_ID'") {
         ... on Issue {
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
               fieldValueByName(name: "Status") {
                 ... on ProjectV2ItemFieldSingleSelectValue {
                   name
                   optionId
                 }
               }
             }
           }
         }
       }
     }')

   # Extract project ID, status field ID, and current status
   PROJECT_ID=$(echo $PARENT_PROJECT_RESPONSE | jq -r '.data.node.projectItems.nodes[0].project.id')
   STATUS_FIELD_ID=$(echo $PARENT_PROJECT_RESPONSE | jq -r '.data.node.projectItems.nodes[0].project.field.id')
   PARENT_STATUS_NAME=$(echo $PARENT_PROJECT_RESPONSE | jq -r '.data.node.projectItems.nodes[0].fieldValueByName.name')
   PARENT_STATUS_OPTION_ID=$(echo $PARENT_PROJECT_RESPONSE | jq -r '.data.node.projectItems.nodes[0].fieldValueByName.optionId')

   if [[ -n "$PARENT_STATUS_NAME" && "$PARENT_STATUS_NAME" != "null" ]]; then
     echo "Parent issue status: ${PARENT_STATUS_NAME}"
     echo "Sub-issues will be set to same status"
   fi
   ```

### Step 5 - Set Parent Issue to Epic Type

1. **Check current issue type**:
   ```bash
   CURRENT_TYPE_RESPONSE=$(gh api graphql -H "GraphQL-Features: issue_types" -f query='
     query {
       node(id: "'$ISSUE_NODE_ID'") {
         ... on Issue {
           issueType {
             id
             name
           }
         }
       }
     }')

   CURRENT_TYPE=$(echo $CURRENT_TYPE_RESPONSE | jq -r '.data.node.issueType.name')
   ```

2. **If already Epic**:
   ```
   ℹ️  Issue is already type 'Epic'

   Continue with sub-issue creation? (yes/no):
   ```

3. **Otherwise, set to Epic**:
   ```bash
   echo "Setting issue #{NUMBER} to type 'Epic'..."

   UPDATE_RESPONSE=$(gh api graphql -H "GraphQL-Features: issue_types" -f query='
     mutation($issueId: ID!, $typeId: ID!) {
       updateIssueIssueType(input: { issueId: $issueId, issueTypeId: $typeId }) {
         issue {
           id
           number
           issueType { name }
         }
       }
     }' -f issueId="$ISSUE_NODE_ID" -f typeId="$EPIC_TYPE_ID")

   # Check for errors
   if echo $UPDATE_RESPONSE | jq -e '.errors' > /dev/null; then
     echo "❌ ERROR: Failed to set issue type"
     echo "Response: $UPDATE_RESPONSE"
     exit 1
   fi

   echo "✓ Issue #{NUMBER} is now type 'Epic'"
   ```

### Step 6 - Guide Sub-Issue Identification

1. **Display current acceptance criteria**:
   ```
   ═══════════════════════════════════════════════════════
   📝 CURRENT ACCEPTANCE CRITERIA
   ═══════════════════════════════════════════════════════

   From parent issue:
   [ ] 1. Extract auth logic into dedicated class
   [ ] 2. Add comprehensive unit tests
   [ ] 3. Implement OAuth support
   [ ] 4. Add password reset functionality
   [ ] 5. Update documentation
   [ ] 6. Ensure backward compatibility
   [ ] 7. Add integration tests
   ```

2. **Ask for number of sub-issues**:
   ```
   How many sub-stories would you like to create?

   Recommendation: Group related criteria into 2-5 focused sub-stories
   Range: 1-10 sub-issues

   Number of sub-stories:
   ```

3. **Validate input**:
   - Must be integer between 1 and 10
   - If invalid, re-prompt

4. **Get parent labels and milestone**:
   ```bash
   PARENT_LABELS=$(gh issue view {NUMBER} --json labels --jq '.labels[].name' | paste -sd, -)
   PARENT_MILESTONE=$(gh issue view {NUMBER} --json milestone --jq '.milestone.title')
   ```

5. **For each sub-issue (loop {N} times)**:
   ```
   ═══════════════════════════════════════════════════════
   SUB-STORY {i}/{N}
   ═══════════════════════════════════════════════════════

   Title:
   > [Wait for input]

   Description (optional, press Enter to skip):
   > [Wait for input]

   Issue Type:
   1. Feature - New functionality or enhancement
   2. Task - Specific piece of work
   3. Bug - Problem or unexpected behavior
   4. Refactor - Code restructuring without behavior change
   5. Research - Investigation, spike, or design exploration

   Select type (1-5):
   > [Wait for input]

   Acceptance criteria for this sub-story:
   (Enter one per line, blank line to finish)
   > [Wait for multiple lines]
   >

   Labels (inherit from parent: {PARENT_LABELS}):
   Add additional labels (comma-separated) or press Enter to use parent labels:
   > [Wait for input, or use PARENT_LABELS]

   Milestone (parent: {PARENT_MILESTONE}):
   Use parent milestone? (yes/no):
   > [Wait for input]

   [Store sub-issue details for creation]
   ```

6. **Show summary before creation**:
   ```
   ═══════════════════════════════════════════════════════
   📋 SUMMARY: {N} Sub-Issues Ready to Create
   ═══════════════════════════════════════════════════════

   1. {Title} (Feature)
      - {Acceptance criterion 1}
      - {Acceptance criterion 2}

   2. {Title} (Task)
      - {Acceptance criterion 1}

   3. {Title} (Task)
      - {Acceptance criterion 1}
      - {Acceptance criterion 2}

   ═══════════════════════════════════════════════════════

   Proceed with creation? (yes/no):
   ```

### Step 7 - Create Sub-Issues

**For each sub-issue**:

1. **Build issue body**:
   ```bash
   SUB_BODY="Related to #${PARENT_NUMBER}

## Description
${SUB_DESCRIPTION}

## Acceptance Criteria
${SUB_CRITERIA}

---
Part of Epic: #${PARENT_NUMBER}"
   ```

2. **Create issue via GraphQL**:
   ```bash
   echo "Creating sub-issue {i}/{N}: ${SUB_TITLE}..."

   CREATE_RESPONSE=$(gh api graphql -f query='
     mutation {
       createIssue(input: {
         repositoryId: "'$REPO_ID'"
         title: "'"${SUB_TITLE}"'"
         body: "'"${SUB_BODY}"'"
       }) {
         issue {
           id
           number
           url
         }
       }
     }')

   # Extract sub-issue details
   SUB_ID=$(echo $CREATE_RESPONSE | jq -r '.data.createIssue.issue.id')
   SUB_NUMBER=$(echo $CREATE_RESPONSE | jq -r '.data.createIssue.issue.number')
   SUB_URL=$(echo $CREATE_RESPONSE | jq -r '.data.createIssue.issue.url')

   echo "✓ Created issue #${SUB_NUMBER}"
   ```

3. **Set issue type**:
   ```bash
   # Map user selection to type ID
   case $SUB_TYPE_SELECTION in
     1) SUB_TYPE_ID=$FEATURE_TYPE_ID; SUB_TYPE_NAME="Feature" ;;
     2) SUB_TYPE_ID=$TASK_TYPE_ID; SUB_TYPE_NAME="Task" ;;
     3) SUB_TYPE_ID=$BUG_TYPE_ID; SUB_TYPE_NAME="Bug" ;;
     4) SUB_TYPE_ID=$REFACTOR_TYPE_ID; SUB_TYPE_NAME="Refactor" ;;
     5) SUB_TYPE_ID=$RESEARCH_TYPE_ID; SUB_TYPE_NAME="Research" ;;
   esac

   echo "  Setting type to ${SUB_TYPE_NAME}..."

   gh api graphql -H "GraphQL-Features: issue_types" -f query='
     mutation($issueId: ID!, $typeId: ID!) {
       updateIssueIssueType(input: { issueId: $issueId, issueTypeId: $typeId }) {
         issue { number issueType { name } }
       }
     }' -f issueId="$SUB_ID" -f typeId="$SUB_TYPE_ID" > /dev/null

   echo "  ✓ Type set to ${SUB_TYPE_NAME}"
   ```

4. **Add labels**:
   ```bash
   if [[ -n "$SUB_LABELS" ]]; then
     echo "  Adding labels..."
     gh issue edit $SUB_NUMBER --add-label "$SUB_LABELS"
     echo "  ✓ Labels added"
   fi
   ```

5. **Set milestone**:
   ```bash
   if [[ -n "$SUB_MILESTONE" ]]; then
     echo "  Setting milestone..."
     gh issue edit $SUB_NUMBER --milestone "$SUB_MILESTONE"
     echo "  ✓ Milestone set"
   fi
   ```

6. **Add to project board and set status**:
   ```bash
   echo "  Adding to project board..."
   gh project item-add 5 --owner OWS-PFMS --url "$SUB_URL" > /dev/null 2>&1

   if [ $? -eq 0 ]; then
     echo "  ✓ Added to project board"

     # Set status to match parent if available
     if [[ -n "$PARENT_STATUS_OPTION_ID" && "$PARENT_STATUS_OPTION_ID" != "null" ]]; then
       echo "  Setting status to '${PARENT_STATUS_NAME}'..."

       # Get the project item ID for this sub-issue
       SUB_PROJECT_ITEM_RESPONSE=$(gh api graphql -f query='
         query {
           node(id: "'$SUB_ID'") {
             ... on Issue {
               projectItems(first: 1) {
                 nodes {
                   id
                 }
               }
             }
           }
         }')

       SUB_PROJECT_ITEM_ID=$(echo $SUB_PROJECT_ITEM_RESPONSE | jq -r '.data.node.projectItems.nodes[0].id')

       if [[ -n "$SUB_PROJECT_ITEM_ID" && "$SUB_PROJECT_ITEM_ID" != "null" ]]; then
         # Set the status field value
         gh api graphql -f query='
           mutation {
             updateProjectV2ItemFieldValue(
               input: {
                 projectId: "'$PROJECT_ID'"
                 itemId: "'$SUB_PROJECT_ITEM_ID'"
                 fieldId: "'$STATUS_FIELD_ID'"
                 value: {
                   singleSelectOptionId: "'$PARENT_STATUS_OPTION_ID'"
                 }
               }
             ) {
               projectV2Item { id }
             }
           }' > /dev/null 2>&1

         if [ $? -eq 0 ]; then
           echo "  ✓ Status set to '${PARENT_STATUS_NAME}'"
         else
           echo "  ⚠️  Could not set status (can be set manually later)"
         fi
       fi
     fi
   else
     echo "  ⚠️  Warning: Could not add to project board"
     echo "  You can manually add it later"
   fi
   ```

7. **Store sub-issue ID for linking**:
   ```bash
   SUB_ISSUE_IDS+=("$SUB_ID")
   SUB_ISSUE_NUMBERS+=("$SUB_NUMBER")
   ```

### Step 8 - Link Sub-Issues to Parent

**After all sub-issues created**:

```bash
echo ""
echo "Linking sub-issues to parent epic..."

for i in "${!SUB_ISSUE_IDS[@]}"; do
  SUB_ID="${SUB_ISSUE_IDS[$i]}"
  SUB_NUM="${SUB_ISSUE_NUMBERS[$i]}"

  echo "  Linking #${SUB_NUM}..."

  LINK_RESPONSE=$(gh api graphql -H "GraphQL-Features: sub_issues" -f query='
    mutation {
      addSubIssue(input: {
        issueId: "'$ISSUE_NODE_ID'"
        subIssueId: "'$SUB_ID'"
      }) {
        issue {
          number
        }
        subIssue {
          number
        }
      }
    }')

  # Check for errors
  if echo $LINK_RESPONSE | jq -e '.errors' > /dev/null; then
    echo "  ⚠️  Warning: Failed to link #${SUB_NUM}"
    echo "  You can manually link it in the GitHub UI"
  else
    echo "  ✓ Linked #${SUB_NUM}"
  fi
done

echo ""
echo "✓ All sub-issues linked to parent"
```

### Step 9 - Summary Report

```bash
echo ""
echo "═══════════════════════════════════════════════════════"
echo "✅ EPIC REFACTORED SUCCESSFULLY"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "📦 Epic: #${PARENT_NUMBER} (${PARENT_TITLE})"
echo "   Type: Epic"
echo "   Status: Open (tracking sub-issue progress)"
echo "   URL: ${PARENT_URL}"
echo ""
echo "📋 Sub-Issues Created (${#SUB_ISSUE_NUMBERS[@]}):"

for i in "${!SUB_ISSUE_NUMBERS[@]}"; do
  echo "   #${SUB_ISSUE_NUMBERS[$i]} - ${SUB_TITLES[$i]} (${SUB_TYPES[$i]})"
done

echo ""
echo "═══════════════════════════════════════════════════════"
echo "🔗 Quick Commands"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "View epic:       gh issue view ${PARENT_NUMBER}"
echo "View epic (web): gh issue view ${PARENT_NUMBER} --web"
echo "List sub-issues: gh issue list --search \"parent:${PARENT_NUMBER}\""
echo ""
echo "═══════════════════════════════════════════════════════"
echo "💡 Progress Tracking"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "GitHub automatically tracks progress in the parent epic!"
echo "As sub-issues are completed, the epic shows completion percentage."
echo ""
echo "Sub-issues are visible in:"
echo "- Parent issue page (sub-issues section)"
echo "- Project board: Material Flat Component Library"
echo "- Project board (can filter/group by parent)"
echo "- Issue search (using parent: operator)"
echo ""
```

### Step 10 - Optional Actions

```
Would you like to:
1. View the epic in browser
2. Add more sub-issues to this epic
3. Finish

Enter option (1-3):
```

**If option 1**: Run `gh issue view ${PARENT_NUMBER} --web`
**If option 2**: Return to Step 6 (but skip Epic type setting)
**If option 3**: Exit

## Examples

### Example 1: Refactor Authentication Epic

```
> /story:author:refactor 41

Fetching issue #41...

═══════════════════════════════════════════════════════
📋 ISSUE #41: Refactor authentication module
═══════════════════════════════════════════════════════

ISSUE ANALYSIS:
- Acceptance Criteria: 7 items
- Body Length: 1,850 characters

ASSESSMENT: ✓ Good candidate for Epic refactoring

Convert to Epic? yes

Setting issue #41 to type 'Epic'... ✓

═══════════════════════════════════════════════════════
📝 CURRENT ACCEPTANCE CRITERIA
═══════════════════════════════════════════════════════

[ ] 1. Extract auth logic into dedicated class
[ ] 2. Add comprehensive unit tests
[ ] 3. Implement OAuth support
...

How many sub-stories? 3

═══════════════════════════════════════════════════════
SUB-STORY 1/3
═══════════════════════════════════════════════════════

Title: Extract authentication logic
Type: 2 (Task)
Criteria:
> Extract auth logic into dedicated class
> Add comprehensive unit tests
>

[Creates issue #43]

...

✅ EPIC REFACTORED SUCCESSFULLY

📦 Epic: #41
📋 Sub-Issues: #43, #44, #45
```

## Important Notes

- **Issue Types Required**: This command requires Issue Types to be enabled in your repository (public preview feature as of January 2025)
- **Sub-Issues API**: Requires Sub-Issues API support (public preview feature)
- **GraphQL Headers**: Uses special headers (`GraphQL-Features: issue_types,sub_issues`)
- **Project Board**: Sub-issues automatically added to "Material Flat Component Library" project board (project #5)
- **Progress Tracking**: GitHub automatically tracks completion percentage in parent epic
- **Limit**: Recommend creating 2-10 sub-issues per epic for manageability
- **Labels**: Sub-issues inherit parent labels by default, can add more
- **Milestone**: Sub-issues can inherit or use different milestone

## Safety

- Never automatically closes or modifies parent issue
- Always asks for confirmation before creating sub-issues
- Shows summary before creation
- Provides rollback guidance if partial failure
- Allows cancellation at any step
- Non-destructive - only creates new issues and relationships
- Parent epic can still be worked on independently

## Error Handling

### Issue Types Not Enabled

```
❌ ERROR: Issue Types not enabled

This repository does not have Issue Types enabled.
Enable in Settings → Features → Issues

Alternative: Create issues manually and link via "Related to #{N}"
```

### GraphQL API Error

```
❌ ERROR: Failed to create sub-issue

GraphQL Error: [error message]

Possible causes:
- Permissions issue
- Rate limit reached
- API version mismatch

Already created sub-issues:
- #43 (successfully created and linked)

You can continue manually or retry later.
```

### Sub-Issue Linking Failed

```
⚠️  WARNING: Sub-issue #44 created but linking failed

The issue was created successfully but couldn't be linked to parent.
You can manually link it in the GitHub UI:
1. Open parent issue #41
2. Click "Create sub-issue" dropdown
3. Select "Add existing issue"
4. Enter #44
```

## Related Commands

- `/story:author:create` - Create new user story issue
- `/story:author:add` - Add content to user story
- `/story:author:remove` - Remove content from user story
- `/story:author:update` - Menu router for update operations

## Reference

See `.claude/commands/.graphql-research-issue-types-subissues.md` for:
- Complete GraphQL command reference
- Tested queries and mutations
- Error handling patterns
- Repository-specific constants

## Troubleshooting

**"Issue Types not found"**:
- Feature not enabled in repository settings
- Contact repository administrator
- Or use manual workflow

**"Permission denied"**:
- Requires write access to repository
- Check authentication: `gh auth status`

**"Rate limit exceeded"**:
- Wait a few minutes before retrying
- Reduce number of sub-issues being created

**"Sub-issue link failed"**:
- Sub-issue still created, just not linked
- Can manually link in GitHub UI
- Check Sub-Issues API is enabled

---

**Implementation Status**: ✅ Complete
**Tested**: GraphQL commands verified working
**Repository**: OWS-PFMS/flatcomp
