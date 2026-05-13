---
description: Create a GitHub issue with user story format and add to project board
---

Create a comprehensive GitHub issue using user story format and automatically add it to the "Material Flat Component Library" project board.

## Steps

1. **Display Available Milestones**: First, fetch and display available milestones:
   ```bash
   gh api repos/OWS-PFMS/flatcomp/milestones --jq '.[] | select(.state == "open") | {number: .number, title: .title, open_issues: .open_issues}'
   ```
   Present the list to the user in a readable format.

2. **Gather Information**: Ask user for:
   - Issue title (short, descriptive)
   - Priority (Low/Medium/High)
   - **Issue Type**: What kind of work is this?
     - `Epic` - Parent issue containing multiple related stories (will have sub-issues)
     - `Feature` - New functionality or enhancement
     - `Bug` - Defect, error, or incorrect behavior
     - `Task` - General work item, chore, or maintenance
     - `Refactor` - Code restructuring without behavior change
     - `Research` - Investigation, spike, or design exploration
   - **Labels** (optional): Domain/priority tags (e.g., technical-debt, documentation, file-io, user-experience)
   - **Milestone**: Which milestone/version should this be assigned to? (Show list from step 1, or "none" to skip)
   - Context (where discovered, during what activity)
   - Current behavior (what's happening now)
   - Expected behavior (what should happen)
   - Steps to reproduce (for bugs) or acceptance criteria (for enhancements)
   - Technical notes (optional - affected components, root cause hypothesis, etc.)

3. **Create Issue Body**: Format as user story following repository conventions:
   ```markdown
   ## User Story
   As a [role]
   I want [goal]
   So that [benefit]

   **Discovered during:** [context]

   ## Current Behavior
   - [bullet points describing current state]

   ## Expected Behavior
   - [bullet points describing desired state]

   ## Steps to Reproduce (for bugs) / Acceptance Criteria (for enhancements)
   1. [step/criterion]
   2. [step/criterion]
   3. [observed result]

   ## Technical Notes
   - [relevant technical details]
   - [affected components]
   - [potential root causes or implementation approach]

   ## Acceptance Criteria
   - [ ] [testable criterion]
   - [ ] [testable criterion]

   ## Priority
   [Low/Medium/High] - [justification]

   ## Related Issues
   - #[number] - [description]
   ```

4. **Set Issue Type and Labels**:

   **Issue Type** (structured classification):
   - Set via GraphQL after issue creation (GitHub native Issue Type)
   - Epic, Feature, Bug, or Task based on user selection

   **Labels** (flexible tags):
   - Use labels provided by user
   - Common suggestions:
     - Domain: `file-io`, `ui`, `algorithms`, `testing`
     - Category: `technical-debt`, `documentation`, `refactoring`
     - Priority: `user-experience`, `critical`, `nice-to-have`

   > ⚠️ **Do NOT apply `backlog` as a label to groomed/milestoned issues.**
   > `Backlog` is a board *column* (Status field value), not a label. The flow
   > sets the board column in Step 7. The `backlog` label exists only for truly
   > ungroomed items without a milestone — rare, and not the default.

5. **Create GitHub Issue**:

   **Step 5a: Create issue with labels**:
   ```bash
   gh issue create --title "[title]" \
     --label "[labels-comma-separated]" \
     --milestone "[milestone-number]" \
     --body "[formatted body]"
   ```
   - Include `--milestone` flag with the milestone number if user selected one
   - Omit `--milestone` flag if user selected "none"
   - Capture the issue URL and issue number from output

   **Step 5b: Set Issue Type via GraphQL**:

   Issue type is not a label and cannot be set with `gh issue create`. It must be
   set via the dedicated `updateIssueIssueType` mutation after the issue exists.

   **Known type IDs in this repo** (cached from `issueTypes` query):
   - `Epic`     → `IT_kwDOBlhJXs4BxFMl`
   - `Feature`, `Bug`, `Task`, `Refactor`, `Research` → look up by name (below)

   ```bash
   # 1. Get the issue node ID
   ISSUE_NODE_ID=$(gh api repos/OWS-PFMS/flatcomp/issues/[NUMBER] --jq '.node_id')

   # 2. Look up the issue type ID by name (skip if using the cached Epic ID above)
   TYPE_ID=$(gh api graphql -f query='
     query {
       repository(owner: "OWS-PFMS", name: "flatcomp") {
         issueTypes(first: 20) { nodes { id name } }
       }
     }
   ' --jq '.data.repository.issueTypes.nodes[] | select(.name == "[SELECTED_TYPE]") | .id')

   # 3. Set the issue type via the dedicated mutation
   gh api graphql -f query='
     mutation($issueId: ID!, $typeId: ID!) {
       updateIssueIssueType(input: { issueId: $issueId, issueTypeId: $typeId }) {
         issue { number issueType { name } }
       }
     }
   ' -f issueId="$ISSUE_NODE_ID" -f typeId="$TYPE_ID"
   ```

   **Note**: If GraphQL fails (Issue Types not enabled), warn user but continue:
   ```
   ⚠️  Could not set Issue Type - this repository may not have Issue Types enabled.
   You can manually set the issue type in GitHub UI after creation.

   Issue created successfully with labels.
   ```

6. **Add to Project Board**:
   - Project: "Material Flat Component Library" (project #5)
   - Command: `gh project item-add 5 --owner OWS-PFMS --url [issue-url]`

7. **Set Status to Backlog**:

   New issues must land in the **Backlog** column, not wherever the board's default routes them. The board has four Status columns — `Backlog`, `Todo`, `In Progress`, `Done` — and grooming a Backlog item into Todo is a separate, manual step. Setting status explicitly here makes the intent durable and unaffected by future changes to the board's default column.

   **Step 7a: Query project item + Status field + Backlog option ID**:
   ```bash
   gh api graphql -f query='
     query {
       repository(owner: "OWS-PFMS", name: "flatcomp") {
         issue(number: [NUMBER]) {
           projectItems(first: 10) {
             nodes {
               id
               project {
                 id
                 title
                 field(name: "Status") {
                   ... on ProjectV2SingleSelectField {
                     id
                     options { id name }
                   }
                 }
               }
             }
           }
         }
       }
     }
   '
   ```

   From the response, filter to the project whose `title == "Material Flat Component Library"` and extract:
   - `ITEM_ID` — `projectItems.nodes[*].id`
   - `PROJECT_ID` — `projectItems.nodes[*].project.id`
   - `FIELD_ID` — `projectItems.nodes[*].project.field.id`
   - `BACKLOG_OPTION_ID` — option whose `name == "Backlog"`

   **Step 7b: Update status to Backlog**:
   ```bash
   gh api graphql -f query='
     mutation {
       updateProjectV2ItemFieldValue(
         input: {
           projectId: "[PROJECT_ID]"
           itemId: "[ITEM_ID]"
           fieldId: "[FIELD_ID]"
           value: { singleSelectOptionId: "[BACKLOG_OPTION_ID]" }
         }
       ) {
         projectV2Item { id }
       }
     }
   '
   ```

   **Note**: If the project item isn't found yet (board sync lag) or the `Backlog` option name no longer exists (board schema changed), warn but don't fail — the item is still on the board, just without an explicit status. Report the fallback to the user so they can set it manually.

8. **Verify Success**:
   ```bash
   gh issue view [NUMBER] --json projectItems | jq -r '.projectItems[] | .title'
   ```
   Confirm "Material Flat Component Library" appears in the output.

   Also confirm the Status field is set to `Backlog`:
   ```bash
   gh api graphql -f query='
     query {
       repository(owner: "OWS-PFMS", name: "flatcomp") {
         issue(number: [NUMBER]) {
           projectItems(first: 10) {
             nodes {
               fieldValueByName(name: "Status") {
                 ... on ProjectV2ItemFieldSingleSelectValue { name }
               }
             }
           }
         }
       }
     }
   ' --jq '.data.repository.issue.projectItems.nodes[].fieldValueByName.name'
   ```
   Expected: `Backlog`.

9. **Report Results**:
   ```
   ✅ ISSUE CREATED AND ADDED TO PROJECT BOARD

   📋 Issue #[N]: [Title]
   🔗 URL: [url]
   📝 Type: [Epic/Feature/Bug/Task]
   🏷️  Labels: [labels] (if any)
   🎯 Milestone: [milestone] (if assigned)
   📊 Project: Material Flat Component Library
   📍 Column: Backlog
   ⚡ Priority: [priority]

   The issue is now visible on your project board in the Backlog column.
   Groom it into Todo when ready to pull into the current sprint.
   ```

## Example Usage

```
User: /create-userstory
Assistant: Gathering issue details...
[Interactive prompts follow]

Assistant: Creating issue and adding to project board...

✅ ISSUE CREATED: Issue #21
📊 Added to: Material Flat Component Library
```

## Important Notes

- **Project Board:** Always uses project #5 (Material Flat Component Library)
- **Format:** Follows repository user story conventions
- **Verification:** Confirms issue appears on project board before completing
- **Error Handling:** If project add fails, provides manual command to retry

## Safety

- Never create duplicate issues - search existing issues first if uncertain
- Always verify correct project board (#5 — "Material Flat Component Library" in the OWS-PFMS organization)
- Provide clear, actionable acceptance criteria
- Include technical context to aid future developers
