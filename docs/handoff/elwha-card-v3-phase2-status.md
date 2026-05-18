# ElwhaCard V3 Phase 2 — handoff status

**Last update:** 2026-05-17 (mid-session)
**Branch:** `feat/80-phase2-v3-buildout`
**Open PR:** [#100](https://github.com/OWS-PFMS/elwha/pull/100)
**Epic:** [#80 — ElwhaCard V3 chrome+composition rebuild](https://github.com/OWS-PFMS/elwha/issues/80)

---

## Where we are

Phase 2 of epic #80 is **functionally complete and on a branch under active visual review.** All 8 stories (#84-#91) shipped on `feat/80-phase2-v3-buildout`. CI is green. The branch sits at PR #100, currently in iterative refinement based on visual feedback against the demo (`ElwhaCardV3Demo`).

Don't merge #100 yet — the visual review is ongoing.

---

## What's complete

**Phase 0 (#81, #82) — merged on `main`:**
- V2 paint pipeline cherry-picked into V1
- 3 Material Symbol SVGs (`expand_more`, `expand_less`, `more_vert`) + `MaterialIcons` accessors
- `ElwhaIconButton` no-click-focus default
- V2 PRs #72-#77 closed as superseded

**Phase 1 (#83) — merged on `main`:**
- V1 card package moved to `com.owspfm.elwha.card.v1.*` (31 files + cursor PNGs)

**Phase 2 (#84-#91) — on `feat/80-phase2-v3-buildout`, awaiting merge:**
- `ElwhaCard` V3 chrome (`ElwhaSurface` subclass) with all setters per spec §3.1
- Layer 2 atoms: `ElwhaCardTitle`, `ElwhaCardSubtitle`, `ElwhaCardSupportingText`, `ElwhaCardLeadingIcon`, `ElwhaCardThumbnail` + `ThumbnailShape` enum
- Layer 3 primitives: `ElwhaCardHeader`, `ElwhaCardMedia`, `ElwhaCardActions`, `ElwhaCardDivider` + `DividerStyle` enum
- Layer 4 disclosure: `ElwhaCardChevron`, `ElwhaCardExpandLink`
- Collapse model with per-child `CollapseRule`, 250ms M3-easing tween, `ExpansionOverflow.SCROLL` with real `JScrollPane` install
- HORIZONTAL orientation via `TwoColumnLayout`; spec §15.3 `setLeadingColumn` / `setTrailingColumn`
- Actionability/selection/a11y: ripple, focus ring, state-layer, selection badge, disabled scrim, `AccessibleRole` PUSH_BUTTON/PANEL
- V3 `ElwhaCardList<T>` with `CardSelectionModel`, mouse drag-reorder (all orientations), keyboard reorder (Cmd+↑/↓ + Delete), right-click context menu, grab/grabbing cursors, soft drop indicator

**`ElwhaSurface` upgraded to v0.2.0** to host elevation natively:
- `setElevation(int)` + `getElevation()` + `MAX_ELEVATION = 5`
- `protected int currentElevationForPaint()` hook for subclass transient lift
- `getInsets()` returns shadow reserve so layouts position children inside the visible body
- `paintComponent` paints shadow + body; `paintChildren` clips children to the rounded body shape

**`SurfacePainter` upgraded to v0.2.0**:
- `shadowInsets(elevation)` returns reserve dims
- `renderShadowImage(w, h, arc, elevation)` returns a `BufferedImage` (two-pass `ConvolveOp` box-blur)
- `paintShadow(...)` is now a thin wrapper around `renderShadowImage` + `drawImage` for non-caching callers

**8 spec/story acceptance gaps fixed in the sweep after the user called out "no invented scope cuts":**
1. Chassis padding actually applied (spec §3.1) — was set but never wired to `setBorder`
2. Header baseline alignment (spec §5.1) — was top-aligned BorderLayout
3. Actions row anchored to bottom in slack cells
4. Atoms HTML-wrap text + report wrapped preferred height (spec §4.1, §4.3)
5. Media corner-clip via `VerticalCardLayout` edge-bleed (spec §5.2)
6. Atoms fade to 0.38 when ancestor disabled (spec §11)
7. `ExpansionOverflow.SCROLL` installs real `JScrollPane` (spec §14.4)
8. Mouse drag in HORIZONTAL + GRID + WRAP (story #91 — earlier shipped VERTICAL-only)

**8 visual issues fixed during user review:**
1. Soft shadow — replaced stacked-RoundRect with real `ConvolveOp` blur (no more stair-step corners)
2. Tops of header icon buttons no longer clipped (baseline target = max-component)
3. Action row pushed to bottom in tall grid cells
4. Supporting text wraps instead of truncating
5a. Left corners of horizontal card now mask leading image (chassis clips children)
5b. Atoms LEFT-aligned in BoxLayout(Y_AXIS) parents
6a. Dragged card visually follows cursor with Z-order to top
6b. Bundled grab / grabbing PNG cursors during drag
- Plus the iteration: shadow lateral-spread reduced; switched to `ConvolveOp` blur; drag-clamp + force-repaint; `ElwhaCard.getInsets()` always reserves `MAX_ELEVATION` so variant bodies match; **shadow image cached per `(size, arc, elevation)` for drag FPS**

## Commit chain on `feat/80-phase2-v3-buildout`

Most recent first (run `git log --oneline main..HEAD` for the live list):
- `3992c7d` — shadow image cached on ElwhaSurface (drag FPS fix)
- `c627022` — Card always reserves MAX_ELEVATION insets (variant bodies match)
- `21a05f3` — clamp dragged card inside list bounds + force repaint
- `2934660` — ConvolveOp soft shadow (no stair-step corners)
- `fc381eb` — tighten shadow geometry (directional drop)
- `6d3b7cc` — drag follows cursor + grab/grabbing cursors
- `bef1339` — atom HTML wrap + LEFT alignment + header baseline = max
- `f71e06e` — elevation moved to ElwhaSurface; chassis insets; child clipping
- `9036905` — FULL divider edge-bleed
- `7149758` — header baseline target = max-baseline
- `9e3dfa5` — chassis padding via VerticalCardLayout + media edge-bleed
- `b22e3f8` — HORIZONTAL/GRID drag drop slots
- `f549f90` — mouse drag VERTICAL
- `52a7760` — V3 ElwhaCardList<T> (#91)
- `c6d5d28` — actionability + a11y (#90)
- ...plus 6 more Phase 2 commits below

## How to launch the demo

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.ElwhaCardV3Demo"
```

The demo (`src/com/owspfm/elwha/card/ElwhaCardV3Demo.java`) is a scrollable single-window
smoke test with ten sections: Variants, Header anatomy, Media + Actions + Divider, Collapse,
Interactivity, Disabled, HORIZONTAL, Two-tier conversation, ExpansionOverflow.SCROLL, and
ElwhaCardList. **Not the standalone playground** (that's Phase 3 / #92).

## Phase 3 — what's NOT done

| # | Story | What |
|---|---|---|
| #92 | Standalone `ElwhaCardPlayground` | Real playground replacing the demo file |
| #93 | ThemePlayground swap | Remove V1 Card tab, add V3 Card tab |
| #94 | V1 → V3 migration doc | For OWS-Local-Search-GUI migration |
| #95 | Release v0.2.0 | Dual-package (V1 at `card.v1.*`, V3 at `card.*`) |

Phase 6 (#96 delete V1, #97 release 1.0.0) waits for OWS-side migration to complete (cross-repo).

## Outstanding visual issues / known limitations

- **Demo:** the `ElwhaCardV3Demo` file is a transitional smoke test, will be replaced by the
  proper `#92` playground in Phase 3
- **Shadow image cache memory:** ~110 KB per card at high elevation; could go static and
  shared if memory becomes a bottleneck (currently per-instance)
- **Drag clamp:** card constrained to list bounds, not viewport bounds — dragging past the
  scrollpane edge of the list itself is still possible
- **Selection/actionable on Outlined card:** focus ring color = `ON_SURFACE` (spec §10.2),
  needs visual verification
- **V3 ElwhaCardList depends on `card.v1.list.Cursors`** for grab/grabbing PNG loading —
  cross-package coupling that 1.0.0 (#96, V1 deletion) needs to resolve

## Key conventions / behavioral rules (memory entries)

- **No invented scope cuts.** Complete what the stories and specs actually say. Never write
  "deferred," "no consumer needs X," "out of scope for v0.2" as cover for cuts not approved by
  the operator. (See `feedback_no_invented_scope_cuts.md`.)
- **Phase handoff cadence.** Work whole phases autonomously; commit per story on a single
  phase branch; hand off PR/merge at phase boundaries, not per story. (See
  `feedback_phase_handoff_cadence.md`.)
- **Surface decisions explicitly.** When accepting a quirk or deferring a fix, say so out loud
  in the same response. Don't silently move on. (See `feedback_surface_decisions.md`.)
- **`@version` bumped on every change** that touches an entity, via the milestone
  (`v0.2.0` for this PR). The `validate-versions` workflow runs
  `scripts/update_javadoc_version.py --check --changed-only --expected v0.2.0`.

## Source-of-truth docs

- **Spec:** `docs/research/elwha-card-v3-spec.md` (22-section implementation contract)
- **Sketch:** `docs/research/elwha-card-v3-sketch.md` (architectural narrative + GO record)
- **M3 reference (organized):** `docs/research/m3-card-spec-organized.md`
- **M3 reference (chronological):** `docs/research/m3-card-spec-findings.md`
- **5-lib survey:** `docs/research/m3-card-lib-survey.md`
- **API conventions (incl. §6 leaf-vs-container):** `docs/development/component-api-conventions.md`

## Operator working-style preferences (always-active)

- Don't auto-merge PRs — after CI green, hand off; merge only on explicit go
- Preserve `Closes #N` keywords in squash-merge commit bodies
- Set the PR milestone at creation
- No `-i` interactive Git commands

---

## Resuming from a fresh session

1. Check `git status` on `feat/80-phase2-v3-buildout`; pull if needed
2. Verify build: `mvn verify` (under JDK 21 — Spotless breaks on JDK 25)
3. Launch the demo to see current state: `mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.ElwhaCardV3Demo"`
4. Wait for the operator to send the next issue / direction — visual review is the active
   workflow right now. Don't proactively change anything that isn't called out
