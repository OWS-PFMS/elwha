---
description: Spec out an M3 Expressive component epic stub — capture the M3 spec into a research doc, write the Phase 0 design doc, and file the follow-on epics + Phase-1 stories.
---

Promote an Elwha **epic stub** (e.g. ElwhaMenu #298, ElwhaTextField #286, ElwhaTopAppBar #287, ElwhaSideSheet #308, FAB Menu #185) from a soft-spec placeholder into a build-ready epic: an M3 spec capture, a Phase 0 design doc, and filed stories.

**Reference implementation:** ElwhaMenu #298 → `docs/research/elwha-menu-research.md` + `elwha-menu-design.md`, epics #322/#323, stories #325–#330, PR #324. Mirror those artifacts.

Epic number from: $ARGUMENTS (if absent, ask which stub).

This is a **collaborative, multi-turn** process: phases A/D/E are autonomous; phase B is operator-driven (they paste M3 spec screenshots). Don't try to do it all in one turn. Move at the operator's pace.

---

## Phase A — Gather (autonomous)

1. **Read the epic stub** (`gh issue view <N> --repo OWS-PFMS/elwha`) and every issue it references — consumers (the issue that's *blocked by* this one), siblings, the z-band/overlay issues (#221 `ElwhaLayers.OVERLAY_LAYER`). Note the milestone.
2. **Survey the codebase** for the patterns this component reuses:
   - Analogous **dedicated primitives** — `ElwhaNavRailDestination` (the `extends JComponent implements IconBearing` template), `AbstractElwhaDialog` (the overlay host: mount + focus-trap + focus-restore + `MorphAnimator`).
   - **Theme tokens** — `ColorRole`, `ShapeScale`, `SpaceScale`, `TypeRole`, `StateLayer`, `ShadowPainter`, `ShapeMorphPainter` (#176), `RipplePainter`. Goal: **map every M3 need onto an existing token — zero new theme tokens.**
   - **Format templates** — `docs/research/elwha-navigation-rail-research.md` (research-doc shape) and `elwha-dialog-design.md` (design-doc shape). Match them.
3. **Pull the authoritative M3 spec text from GitHub raw docs** (WebFetch):
   - `https://raw.githubusercontent.com/material-components/material-web/main/docs/components/<component>.md`
   - `https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/<Component>.md`
   - ⚠️ **`m3.material.io` is JS-only — WebFetch returns just the page title.** That's *why* the operator screen-caps it. Don't rely on it; use the GitHub raw docs for text + the operator's screenshots for the visual spec.
4. **Create the research-capture doc** `docs/research/elwha-<component>-research.md` with: status header (RAW CAPTURE, links the epic), source URLs, the web-sourced API shape (§B/§C), an **Elwha token-mapping table** (zero-new-tokens goal), an **open-questions** section, and a **screenshot log** at the end. Then tell the operator you're ready for screenshots.

## Phase B — Capture (operator pastes M3 spec screenshots, you transcribe)

The operator screen-caps each M3 spec page (Overview, Variants, Configurations, Anatomy, Color, States, Measurements, **Tokens**, Guidelines, Menu/Items, Flexibility & slots, Placement, Submenus, Adaptive, Behavior, Focus, Density, Accessibility). For **each batch**:

- **READ THE IMAGES, not just the text.** The load-bearing findings come from the renders, not captions: gap-vs-divider layouts, corner shape-morph, which color role the selected fill actually is, anatomy part numbering. The operator *will* call this out — honor it.
- **Append a section** per page. Transcribe token tables **verbatim** (exact dp/pt values).
- **Tag every line `[CODE]`** (enforceable/affordable in the component API) **or `[DOC]`** (Javadoc/README guidance, not enforced). Not all spec guidance is code.
- **Map to Elwha tokens** as you go; keep the zero-new-tokens table current.
- **Cross-check measurements**: when the Measurements redline page arrives, verify each redline against the Tokens-page values. Confirm agreements; flag mismatches you can't read rather than asserting.
- **Correct earlier guesses out loud.** Better data supersedes — e.g. a selected fill you guessed "secondary-container" turns out `TERTIARY`. Add a ⚠️ CORRECTION note and fix the stale rows. (Memory: [[feedback_surface_decisions]].)
- **Watch for token-table ghosts.** Tokens can exist with no example or implementation (the M3 menu's "horizontal" block). Before treating one as scope, verify against the spec examples + Material Web/MDC/Angular; if absent everywhere, park it (likely a shared/generated token or a different component) and note the breadcrumb.
- Maintain a **terminology→API lock** table (operator rule: *the API must mirror M3's exact nouns*) and an **architecture-insight** note when the operator spots one (e.g. "this item is a dedicated primitive like the nav destination").

## Phase C — Synthesize & decide (interactive)

1. Add a **`§TL;DR` synthesis + reading-order TOC** at the top of the research doc (sections accrue in capture order, not logical order).
2. Surface the **open design questions**. Present them to the operator **as plain-text numbered lists with thorough explanations + your recommendation each** — **never UI pickers** (project rule). Group by weight (architecture / scope / housekeeping).
3. Lock the **scope + versioning** decisions with the operator:
   - **Expressive-first**: ship the M3 *Expressive* variant; skip deprecated/baseline forms ([[project_elwha_m3_expressive]]). File excluded variants as **stub epics**, don't silently cut ([[feedback_no_invented_scope_cuts]]).
   - **Distinguish V from Phase.** **V** = epic/version (V1 = this epic; V2 = a separate follow-on epic). **Phase** = a stage *within* an epic. A feature can be "V1, later phase" (in scope, not Phase 1) — don't conflate. Get the operator's V1/V2/deferred split explicit.

## Phase D — Phase 0 design doc (autonomous, then review)

Write `docs/research/elwha-<component>-design.md` — **decisions, not a catalog**; reference the research doc for captured detail. Mirror `elwha-dialog-design.md`. Sections: TL;DR (numbered locks) · scope · **host/architecture** (the load-bearing call — recommend, and lock it via the first story's *spike*, per the dialog's S1 precedent) · anatomy/primitive · tokens · color · layout · states/motion · placement · selection · a11y · Showcase pattern · **phasing → stories** · out-of-scope.

Apply Elwha doctrine:
- **Zero new theme tokens** unless truly unavoidable (say so loudly if so).
- **Dedicated `JComponent` primitives** (mirror `ElwhaNavRailDestination`) — never a styled `JMenuItem`/`JButton`/Swing widget when M3 anatomy is involved.
- **Overlay/popup host**: reuse/extract from `AbstractElwhaDialog`; place on the right `JLayeredPane` band (dialogs `MODAL_LAYER`; Elwha overlays `ElwhaLayers.OVERLAY_LAYER` 190; transient popups `POPUP_LAYER` 300 — above dialogs). Coordinate extraction with the other pending consumer (side-sheet #308).
- **Dogfood Elwha components** in the Showcase ([[feedback_dogfood_elwha_components]]); component-api-conventions + versioning Javadoc.
- Mark genuinely-open architecture calls as **RECOMMENDED, locked by the S1 spike** — don't pretend to resolve what needs code.

Hand the design doc to the operator for approval **before filing any stories**.

## Phase E — File (on operator go)

1. **Follow-on + excluded-variant epics** — `gh issue create` (V2 epic, baseline/variant stub), then **add each to Project #5**: `gh project item-add 5 --owner OWS-PFMS --url <url>`.
2. **Commit both docs via a branch + PR** — `main` is protected (branch first). PR needs a **milestone at creation** (`--milestone v0.X.0`) or the `validate-versions` workflow fails. Docs-only ⇒ no `@version` bumps. **Do not auto-merge** — hand off after CI green ([[feedback_honor_stated_workflow]]). Conventional-commit title; end the commit body with the `Co-Authored-By: Claude Opus 4.8 (1M context)` line and the PR body with the Claude Code footer.
3. **Pointer comment on the epic** linking the docs/PR + the new epics + the locked scope.
4. **File the Phase-1 stories** in dependency order (so blocked-by references resolve to real numbers). Title `feat(#<epic>): …` / `docs(#<epic>): …`; body = Part-of-epic + goal + acceptance checklist + design-doc §refs + Blocked-by. Set `--milestone`, `--label`, and **add each to Project #5**. Default to **Phase-1 only** (don't pre-file later-phase stories) unless the operator says otherwise — keeps the board honest about what's queued.
5. **Wire native sub-issues**: append a Stories checklist (`- [ ] #325 — S1 …`) to the **epic's body** (`gh issue edit <epic> --body-file`) — task-list issue refs in the body create GitHub's tracked-by relationship.

Honor [[feedback_phase_handoff_cadence]]: file/hand off at phase boundaries, not per story.

---

## Guardrails (the things that bit us on #298)
- **Read images, not captions.** Re-state visual findings.
- **CODE vs DOC** on every guideline line.
- **Cross-check** measurement redlines against the token values.
- **Correct stale guesses explicitly** and fix the rows they touched.
- **Token-table ghosts**: verify before scoping.
- **V ≠ Phase.** Keep them separate in every recommendation.
- **No silent cuts**: every exclusion is a filed stub or a documented deferral.
- **Plain-text numbered questions**, never pickers.
- **Branch + PR + milestone; never push to `main`; never auto-merge.**
- **Add every new issue to Project #5.**
