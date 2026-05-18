# ElwhaCard V3 — card-layer fixes + v0.2.0 release status

**Last update:** 2026-05-18
**For:** the next agent picking up the V3 card work.
**This is a handoff prompt — read it cold and pick up where the previous session left off.**

Read this file first, then `CLAUDE.md` (project), then `~/.claude/CLAUDE.md` (user-global), then the auto-memory index at `~/.claude/projects/-Users-charlesbryan-Documents-Bryan-Software-Dev-OWS-Elwha/memory/MEMORY.md`. Then `git status` + `gh pr list`.

---

## TL;DR — where things stand

- **Main has Phase 0/1/2 of V3 ElwhaCard merged.** Foundation works visually but has several silent compliance gaps and one paint bug surfaced during visual review.
- **§3.4 width-constraint contract just merged on main** (PR #104). Locks the policy that 4 of the 6 outstanding stories implement.
- **6 stories filed under epic [#80](https://github.com/OWS-PFMS/elwha/issues/80), all milestone v0.2.0:**
  - **[#105](https://github.com/OWS-PFMS/elwha/issues/105)** §3.4 implementation — chassis honors parent width, atoms reflow (covers PL-2, 14, 16, 17)
  - **[#106](https://github.com/OWS-PFMS/elwha/issues/106)** media corner-clip aligns with chassis rounded outer shape (PL-1)
  - **[#107](https://github.com/OWS-PFMS/elwha/issues/107)** M3 paint compliance audit + fixes (PL-4 through 11)
  - **[#108](https://github.com/OWS-PFMS/elwha/issues/108)** token defaults verification (PL-12)
  - **[#109](https://github.com/OWS-PFMS/elwha/issues/109)** `ElwhaCardMedia.setDecorative` + alt-text API (PL-13)
  - **[#110](https://github.com/OWS-PFMS/elwha/issues/110)** collapse/expand animation lag (PL-15, shadow recompute)
- **Epic [#103](https://github.com/OWS-PFMS/elwha/issues/103) (`ElwhaButton` primitive) blocks v0.2.0 release** — operator's call: "no half-ready API." Don't start it until the 6 card stories above ship.
- **PR [#102](https://github.com/OWS-PFMS/elwha/pull/102) (Phase 3 playground + release-prep) stays open** but should NOT merge in its current form. See "How to handle PR #102" below.
- **v0.2.0 has not been tagged or released.** Won't be until everything above lands.

---

## What just happened (last session context)

V3 ElwhaCard's Phase 2 (#84-#91, chrome + atoms + primitives + collapse + horizontal + actionability + V3 list) merged to main via PR #100. Phase 3 work (#92 standalone playground, #93 ThemePlayground V3 tab, #94 V1→V3 migration doc, #95 release prep) opened as PR #102.

During visual review of the playground, the operator surfaced **17 punch-list items (PL-1 through PL-17)** spanning paint bugs, layout bugs, M3 compliance gaps, missing API, and one perf bug. The previous agent kept reaching for quick playground workarounds instead of fixing the card layer; the operator corrected this multiple times. **Key correction:** the playground is surfacing real card-layer bugs, not playground bugs. Fixes must live in the card.

After research into how other M3 libraries handle width-constrained cards (Compose Material 3 Card, MUI Joy UI, Material Components Web, CSS object-fit), the **§3.4 width-constraint contract** was drafted and merged on main via PR #104. It locks the policy that four of the visible bugs (PL-2 chassis-paints-past-parent, PL-14 cramped spacing, PL-16 atoms-don't-reflow, PL-17 horizontal-misalignment) all derive their fixes from.

Stories #105-#110 were filed grouping the 17 PL items into a coherent batch.

The operator's directive: **all 6 stories land on ONE branch / ONE PR.** Don't piecemeal — that would force re-iterating on the playground every time a card fix lands. Better to fix them all, then rebase PR #102 once.

---

## Spec / policy ground truth

The spec lives at `docs/research/elwha-card-v3-spec.md`. Key sections to honor while working:

- **§3.4 Width-constraint behavior** (just merged) — chassis honors parent-assigned width; atoms reflow via `getMaximumSize() = (Integer.MAX_VALUE, preferredHeight)`; media cover-fits per CSS `object-fit: cover`; no hard minimum width; **universal invariant: no child paints past chassis bounds, ever.**
- **§22 implementation guard-rails** (just expanded) — includes the new "don't let any child paint past chassis bounds" and "atoms must report unbounded `getMaximumSize` X-axis" rules.
- **§5.2 ElwhaCardMedia** — image / painter factories only, no JComponent embedding. Story #109 amends this section to add the decorative-vs-informative API.
- **§14 collapse / disclosure model** — relevant for story #110.

M3 reference (cross-check against these before claiming compliance):
- `docs/research/m3-card-spec-organized.md` — topical reference (canonical)
- `docs/research/m3-card-spec-findings.md` — chronological walkthrough (raw)

Both reference docs are anchored to AndroidX Compose Material3 token source at `androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/{Elevated,Filled,Outlined}CardTokens.kt`.

---

## What to do next

### 1. Cut the card-fix branch

```bash
git fetch origin main && git checkout -b feat/80-card-bug-fixes origin/main
```

### 2. Work the 6 stories on this branch, in this order

1. **#105 first** — largest, unblocks layout-correctness for the rest. Implements §3.4. Likely touches: `ElwhaCard.getInsets` / `getPreferredSize` (chassis honors parent width); `VerticalCardLayout` (inter-element gaps); `ElwhaCardTitle` / `ElwhaCardSubtitle` / `ElwhaCardSupportingText` (`getMaximumSize` override per §22 guard-rail); `ElwhaCardHeader` (header internal padding); `TwoColumnLayout` (HORIZONTAL alignment); `ElwhaSurface.paintChildren` clip is the boundary — verify children never escape it.
2. **#106** — media corner-clip alignment. Likely: extract shared `cornerClipPath(int w, int h, int arc)` helper in `SurfacePainter` consumed by both chassis stroke and `ElwhaCardMedia`'s clip, eliminating the cubic-Bezier drift.
3. **#107** — M3 paint compliance audit + fixes. Read each item against `*CardTokens.kt`, document status ("already correct" or "fix applied"). 8 items.
4. **#108** — token defaults. 3 verifications, likely small fixes if any.
5. **#109** — `ElwhaCardMedia.setDecorative(boolean)` + alt-text. Net-new API; spec §5.2 amendment + impl.
6. **#110** — collapse/expand animation lag. Profile first to confirm shadow recompute is the culprit; pick a fix per the candidate list in #110.

### 3. Commit per story

Each story = one commit (or a tight cluster of commits, but per-story attribution clear). Conventional Commits: `fix(#105): ...`, `audit(#107): ...`, `perf(#110): ...`. Include `Closes #N` keywords; the merge commit will close them properly.

### 4. After all 6 stories on the branch

- `mvn verify` (JDK 21!) green
- Launch the playground and visually re-verify every PL item from the playground gallery
- Open PR titled something like `feat(#80): card-layer bug fixes — §3.4 impl + media clip + M3 compliance + decorative API + perf`
- Milestone v0.2.0
- Hand off to operator for review + merge

### 5. After card-fix PR merges

- Switch to PR #102 branch
- Rebase on updated main (card fixes propagate into the playground demo automatically)
- Verify playground visually — every previously-broken card now renders correctly
- **Revert the release-prep commit on PR #102** (`1ea5d13` originally, may be different SHA after rebase). The pom-bump and CHANGELOG move land separately as part of #95 once ElwhaButton ships.
- **Remove the temporary playground workarounds** added on PR #102 (the `ScrollableViewportPanel` in `GalleryPanel` and the proportional divider deferral in `ElwhaCardPlayground`) — they were band-aids while the card layer was broken; now card honors parent width, the playground doesn't need them.

### 6. Start ElwhaButton epic #103

Per operator: v0.2.0 doesn't ship until ElwhaButton lands. Mirror the `ElwhaIconButton` epic structure (#45). Don't touch v0.2.0 release until ElwhaButton + card playground demonstrating M3-correct buttons in action rows.

### 7. Then and only then: v0.2.0 release

- Bump pom 0.1.0 → 0.2.0
- Move `[Unreleased]` → `[0.2.0]` in CHANGELOG.md (include card fixes, ElwhaButton, playground, migration doc)
- Commit `chore: release 0.2.0`
- **Wait for explicit operator go before tagging + pushing.** Standing rule: don't auto-release.

---

## How to handle PR #102 (Phase 3 playground)

**Currently open, do not merge in current form.** Five commits total, including:

- `1ea5d13` (release-prep) — pom bump + CHANGELOG move to `[0.2.0]`. **Must be reverted before merge** since v0.2.0 isn't shipping yet.
- `c1ae227` (playground workarounds) — `ScrollableViewportPanel` in GalleryPanel (so gallery tracks viewport width) + proportional divider in ElwhaCardPlayground. **Temporary**, marked TEMPORARY in code. Remove after card-fix branch merges to main and the chassis correctly honors parent width.

**Workflow after card fixes land:**
1. Card-fix branch merges to main (closes #105-#110)
2. Switch to PR #102 branch, `git pull --rebase origin main`
3. Verify playground demo visually — confirm every PL bug is now fixed
4. New commit on PR #102: `revert: drop release-prep until ElwhaButton + card fixes complete` (reverts `1ea5d13`)
5. New commit on PR #102: `demo(#92): remove temporary playground viewport-tracking + divider workarounds (chassis now honors parent width per #105)` (reverts the workaround content of `c1ae227`)
6. PR #102 stays open through ElwhaButton epic work — integrate ElwhaButton into the playground gallery + OWS Loop example as part of that epic
7. Only merge PR #102 once ElwhaButton + card-fixes both done and playground demonstrates the complete M3-correct V3 picture

---

## ElwhaButton epic #103

Filed but not started. Blocks v0.2.0. Mirrors `ElwhaIconButton` epic #45's structure.

Why it blocks: the V3 action-row vocabulary needs `Filled` / `Filled Tonal` / `Outlined` / `Text` buttons per M3 §1.4.1. Raw `JButton` renders as M3 Outlined via the Elwha theme + as Filled when made the frame's default button — but **Filled Tonal has no mechanism in the lib**. Without it, M3-correct cards can't be expressed.

Operator decided (verbatim): "i dont want to ship a half ready api." So v0.2.0 holds until ElwhaButton ships.

Sub-stories sketched in #103 body. Open the issue to read them.

---

## Operator working-style preferences (always-active, don't violate)

These are the **most-violated** rules — the previous agent broke each of these at least once and got corrected:

- **Don't auto-merge PRs.** CI green ≠ merge. Hand off to operator.
- **Don't tag / push releases without explicit go.** Same principle, applied to releases.
- **Don't rush. We're playing the long game building this lib.** Quick fixes that paper over real issues are worse than slow, intentional work.
- **Don't invent scope cuts.** Phrases like "no consumer needs X today" or "deferred to post-1.0" are NEVER cover for cuts the operator didn't approve. Complete what the stories and specs actually say.
- **Surface decisions explicitly.** When accepting a quirk, deferring a fix, picking option A over option B — say so out loud in the same response. Don't silently move on.
- **Don't gaslight.** When the visual evidence (a screenshot) contradicts a claim in a research doc, the screenshot wins. Don't argue with the operator's eyes.
- **Plain-text numbered lists for clarifying questions.** No UI pickers.
- **Phase-handoff cadence.** Work whole phases autonomously; commit per story on a single phase branch; hand off PR/merge at phase boundaries, not per story.
- **No force-push to main.** No `-i` flag on git commands (no `rebase -i`, no `add -i`).
- **JDK 21 required.** Spotless 2.46.1 + google-java-format breaks on JDK 25 (`NoSuchMethodError` on `Log$DeferredDiagnosticHandler.getDiagnostics()`). Always run with `JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"`.
- **Set PR milestone at creation.** The `validate-versions` workflow reads the milestone title and hard-fails without one.
- **Preserve `Closes #N` keywords** in squash-merge commit bodies. Custom `--body` flags can overwrite the PR description; sub-issues silently stay open otherwise.
- **`@version` bumped on every change** touching the entity. The script `scripts/update_javadoc_version.py --check --changed-only --expected v0.2.0` runs in CI and hard-fails on misses.

Behavioral memory files at `~/.claude/projects/-Users-charlesbryan-Documents-Bryan-Software-Dev-OWS-Elwha/memory/`:
- `feedback_no_invented_scope_cuts.md`
- `feedback_phase_handoff_cadence.md`
- `feedback_surface_decisions.md`

Read them; they're load-bearing.

---

## Common pitfalls the previous agent hit (don't repeat)

1. **Reaching for playground workarounds when the bug is in the card.** When the chassis paints past its bounds, the fix is in the chassis, NOT a `Scrollable` shim in the playground. The operator caught this twice.
2. **Misreading research docs and propagating the error.** The `m3-card-spec-findings.md §14` note saying "media size stays constant" was wrong/oversimplified per the operator's direct M3 screenshots. **Primary evidence (screenshot) trumps secondary observation (research doc).** When in doubt, ask, don't guess.
3. **Saying "I'll do X" then sitting.** If you say you're starting on something, actually start. The operator will assume you're working and notice if you're not.
4. **Claiming "M3 compliance" without reading the impl.** Several Phase 2 commits claimed M3 compliance (focus ring color, hover lift, disabled role swap) that nobody had verified against the Compose token tables. Story #107 is the audit + fix to find out what's actually wrong. **Don't make claims in CHANGELOG entries you haven't verified.**
5. **Pushing to ship / file / advance.** Asked to fix a card bug, don't propose filing an epic AND merging a PR AND pushing a release in the same response. Stay scoped to what's asked.
6. **Treating "what about X?" as "go file X."** Operator's clarifying questions are often probing whether you've thought about something — answer the question first; act only when explicitly directed.

---

## Resume-from-fresh-session checklist

1. Read this doc, then `CLAUDE.md`, then `~/.claude/CLAUDE.md`, then `MEMORY.md`.
2. `git fetch origin main && git checkout main && git pull --ff-only`
3. `gh pr list` — confirm PR #102 is still open and unmerged
4. `gh issue view 105` through 110 — read the 6 stories
5. `gh issue view 103` — read the ElwhaButton epic (don't start it yet)
6. Launch the playground to see current state:
   ```bash
   JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home" mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
   ```
   Visually note every PL bug per the punch list above so you know what success looks like.
7. Cut `feat/80-card-bug-fixes` from main.
8. Start on #105 (§3.4 implementation). Don't ship anything until all 6 stories done.

---

## Source-of-truth files

- **Spec:** `docs/research/elwha-card-v3-spec.md`
- **Sketch:** `docs/research/elwha-card-v3-sketch.md`
- **M3 reference (topical):** `docs/research/m3-card-spec-organized.md`
- **M3 reference (chronological):** `docs/research/m3-card-spec-findings.md`
- **V1 → V3 migration doc:** `docs/migration/elwha-card-v1-to-v3.md`
- **API conventions:** `docs/development/component-api-conventions.md`
- **Versioning playbook:** `docs/development/versioning-playbook.md`
- **Previous handoff (Phase 2 state):** `docs/handoff/elwha-card-v3-phase2-status.md`
- **This doc:** `docs/handoff/elwha-card-v3-card-fixes-status.md`
