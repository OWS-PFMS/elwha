# ElwhaTooltip — M3 spec research capture

**Status:** SYNTHESIZED (self-researched capture — implementation-source truth, no operator screenshot pass).
**Captured:** 2026-06-11. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parent:** epic [#445](https://github.com/OWS-PFMS/elwha/issues/445). **Design doc:** [`elwha-tooltip-design.md`](elwha-tooltip-design.md) — the decisions; this doc is the captured spec.

---

## §TL;DR — synthesis

1. **Two variants, one component family.** **Plain** tooltips are a single short label describing an icon/control; **rich** tooltips carry an optional subhead, multi-line supporting text, and optional text-button actions. M3 names exactly these two; there is no third variant and no deprecated baseline form (§E).
2. **Token truth (Compose `v0_210` token sheets, §T):** plain = `inverse-surface` container / `inverse-on-surface` `body-small` text / `corner-extra-small` shape, no elevation; rich = `surface-container` / elevation **Level 2** / `corner-medium` / `title-small` subhead + `body-medium` supporting text both `on-surface-variant` / `primary` `label-large` actions. **Every role maps onto an existing Elwha token — zero new tokens.**
3. **Measurements (Compose `Tooltip.kt` constants, §M):** 4dp anchor gap; plain padding 8×4, min 40×24, max width 200; rich horizontal padding 16, subhead first-baseline 28, subhead→text first-baseline 24, text bottom 16, action row min-height 36 + bottom 8, max width 320.
4. **Placement (§P):** above the anchor preferred, flip below on collision; plain aligns flush **start / center / end** with the anchor (center default); rich hangs off the anchor's **start or end corner**; alignment is direction-aware (RTL mirrors start/end); clamp to the viewport with an edge threshold.
5. **Interaction model (MDC Web, the desktop truth — §I):** show on **hover (after a dwell)** and on **keyboard focus** (immediately); hide when hover/focus leaves the anchor *and* the tooltip contents (hover-onto-the-tooltip keeps it open — WCAG 1.4.13); **default rich** behaves like plain; **persistent rich** is toggled by click / Enter / Space and dismisses only on outside-press or focus fully leaving. One tooltip visible at a time. Esc dismisses.
6. **Tooltips never take focus.** The anchor keeps focus throughout; interactive rich content is reachable but never auto-focused (MDC `tabindex="-1"`; Compose forces tooltip focusability only for touch screen-readers).
7. **Motion:** fade-dominant, fast (Compose: FastEffects alpha + FastSpatial scale; material-web/MDC transition ≈ 150 ms in / 75 ms out).
8. **Caret is a platform extra, not M3 anatomy** — parked (§G).

Reading order: §S sources → §V variants → §T tokens → §M measurements → §P placement → §I interaction → §A accessibility → §E expressive check → §G ghosts → §Open.

---

## §S. Sources

| Source | What it provided |
|---|---|
| `androidx` Compose Material3 `tokens/PlainTooltipTokens.kt` (v0_210) | Plain token sheet, verbatim |
| `androidx` Compose Material3 `tokens/RichTooltipTokens.kt` (v0_210) | Rich token sheet, verbatim |
| `androidx` Compose Material3 `Tooltip.kt` | dp constants, positioning algorithm, persistent/dismiss model, a11y semantics |
| MDC Web `packages/mdc-tooltip/README.md` | Desktop hover/focus trigger + delay model, default-vs-persistent rich behavior, ARIA wiring, viewport threshold |
| m3.material.io/components/tooltips/guidelines | Variant definitions + placement nouns (JS-only page; content cross-checked via the above) |
| MDC-Android `docs/components/Tooltip.md` | **Stub** — "yet to be completed"; contributed nothing |

material-web ships **no** tooltip component (open tracking issue) — no `md-comp-*` web token sheet exists; the Compose token files are the authoritative DSDB export.

## §V. Variants & usage

- **Plain tooltip** — a label-only bubble that "briefly describe[s] a UI element"; the canonical affordance for icon-only buttons. Non-interactive: no buttons, no links, no selection. `[CODE]`
- **Rich tooltip** — "provide[s] additional context to a UI element"; optional **subhead**, **supporting text** (multi-line), optional **actions** (text buttons / links). `[CODE]`
- Rich tooltips come in two behavioral flavors `[CODE]`:
  - **Default** — shows on hover/focus of the anchor like plain; *stays* while the pointer/focus is over the tooltip contents; hides when hover/focus leaves both.
  - **Persistent** — toggled by click and Enter/Space on the anchor; stays through arbitrary hovering; dismisses on a press outside the tooltip contents or focus fully leaving. MDC: *"recommended against pairing with anchor elements that have click actions."* `[DOC]`
- Use plain for naming things; rich for explaining things. Don't put information *only* in a tooltip that users need to complete a task. `[DOC]`
- Tooltips label/describe; they are not menus, not dialogs, not toasts. `[DOC]`

## §T. Tokens — verbatim capture + Elwha mapping [zero new tokens]

### Plain tooltip (`PlainTooltipTokens.kt`, v0_210)

| M3 token | Value | Elwha mapping |
|---|---|---|
| `ContainerColor` | `InverseSurface` | `ColorRole.INVERSE_SURFACE` |
| `ContainerShape` | `CornerExtraSmall` | `ShapeScale.XS` (4 px) |
| `SupportingTextColor` | `InverseOnSurface` | `ColorRole.INVERSE_ON_SURFACE` |
| `SupportingTextFont` | `BodySmall` | `TypeRole.BODY_SMALL` |

No elevation token, no state layers (the surface is non-interactive). Plain is a **flat** inverse-surface chip — no shadow. `[CODE]`

### Rich tooltip (`RichTooltipTokens.kt`, v0_210)

| M3 token | Value | Elwha mapping |
|---|---|---|
| `ContainerColor` | `SurfaceContainer` | `ColorRole.SURFACE_CONTAINER` |
| `ContainerElevation` | `Level2` | `ShadowPainter` elevation 2 (+ shadow reserve) |
| `ContainerShape` | `CornerMedium` | `ShapeScale.MD` (12 px) |
| `SubheadColor` | `OnSurfaceVariant` | `ColorRole.ON_SURFACE_VARIANT` |
| `SubheadFont` | `TitleSmall` | `TypeRole.TITLE_SMALL` |
| `SupportingTextColor` | `OnSurfaceVariant` | `ColorRole.ON_SURFACE_VARIANT` |
| `SupportingTextFont` | `BodyMedium` | `TypeRole.BODY_MEDIUM` |
| `ActionLabelTextColor` (default/focus/hover/pressed) | `Primary` | `ElwhaButton.textButton(...)` — the TEXT variant *is* `PRIMARY` `LABEL_LARGE` with its own M3 state layers |
| `ActionLabelTextFont` | `LabelLarge` | (carried by `textButton`) |

⚠️ The action-label state colors are all `Primary` with standard state layers — exactly Elwha's `ButtonVariant.TEXT` contract, so the action row **dogfoods `ElwhaButton.textButton`** rather than re-painting labels. `[CODE]`

## §M. Measurements (Compose `Tooltip.kt`, exact constant names)

| Constant | Value | Applies to |
|---|---|---|
| `SpacingBetweenTooltipAndAnchor` | **4dp** | both — gap between anchor edge and tooltip edge |
| `TooltipMinHeight` | **24dp** | both (plain in practice) |
| `TooltipMinWidth` | **40dp** | both (plain in practice) |
| `PlainTooltipContentPadding` | **8dp horizontal, 4dp vertical** | plain |
| `TooltipDefaults.plainTooltipMaxWidth` | **200dp** | plain — label wraps beyond this |
| `RichTooltipHorizontalPadding` | **16dp** | rich — both edges, all rows |
| `HeightToSubheadFirstLine` | **28dp** | rich — container top → subhead first baseline |
| `HeightFromSubheadToTextFirstLine` | **24dp** | rich — subhead baseline → supporting first baseline |
| `TextBottomPadding` | **16dp** | rich — supporting last baseline → container bottom (no actions) |
| `ActionLabelMinHeight` | **36dp** | rich — action row minimum height |
| `ActionLabelBottomPadding` | **8dp** | rich — action row → container bottom |
| `TooltipDefaults.richTooltipMaxWidth` | **320dp** | rich — subhead + supporting wrap beyond this |

Rich vertical rhythm is **baseline-anchored** (Compose `paddingFromBaseline`), not box-padding — the 28/24/16 values run to text baselines. `[CODE]`

MDC Web adds: *"A threshold distance of 32px is expected to be maintained between the tooltip and the viewport edge"* — a generous browser-viewport rule; an in-window layered pane wants a smaller clamp margin (design call, §design-5). `[DOC]`

## §P. Placement

- **Vertical:** *"Tooltips appear directly below or above [the] anchor element."* Compose prefers **above**, falls back **below** on collision. `[CODE]`
- **Horizontal, plain:** *"can be placed flush with either the end, center, or start of the anchor"* — center is the canonical default; Compose falls back start/end on edge collision. `[CODE]`
- **Horizontal, rich:** *"can be placed at the corner of either the end or start of the anchor"* — the tooltip hangs off one corner rather than centering. `[CODE]`
- **Direction-aware:** start/end resolve through layout direction (RTL mirrors). Compose `TooltipAnchorPosition.Start/.End` respect `LayoutDirection`. `[CODE]`
- Caret/arrow: see §G — not M3 anatomy; Compose-only opt-in extra.

## §I. Interaction & dismiss model (desktop truth = MDC Web)

**Show triggers** `[CODE]`:
- **Pointer hover** over the anchor, after a **show delay** (MDC `setShowDelay(ms)`; foundation default 500 ms — chosen over Swing's legacy 750 ms).
- **Keyboard focus** on the anchor — immediately, no dwell. (Compose: `enableUserInput` covers hover + long-press + focus; long-press is touch-only, N/A on desktop.)
- **Programmatic** `show()` / `dismiss()`.

**Hide triggers** `[CODE]`:
- Hover/focus moving **outside the anchor ∪ tooltip contents** — hovering the tooltip itself keeps it open (both variants; this is WCAG 1.4.13 *hoverable*). Hide runs after a **hide delay** (MDC `setHideDelay(ms)`; foundation default 600 ms) so the pointer can cross the 4dp gap.
- **Esc** key (WCAG 1.4.13 *dismissible*).
- **Default rich:** additionally hides on click within the tooltip content.
- **Persistent rich:** hides **only** on press outside the contents, focus fully leaving, or re-toggle; clicking inside keeps it shown.
- Compose: plain/non-persistent auto-dismiss after `BasicTooltipDefaults.TooltipDuration` (1.5 s) — that is the **touch long-press** flow; on desktop a hovered tooltip must persist while hovered (WCAG 1.4.13 *persistent*). No hover timeout. `[CODE]`

**Exclusivity** `[CODE]`: Compose `MutatorMutex` ensures **one tooltip at a time** across instances; showing a new one dismisses the previous (Swing `ToolTipManager` parity).

**Focus is never stolen** `[CODE]`: the anchor keeps focus; MDC persistent tooltips carry `tabindex="-1"`; Compose forces tooltip focusability only for rich-with-actions under TalkBack (touch screen-reader — N/A desktop V1).

**Motion** `[CODE]`: Compose scales+fades via `FastSpatial`/`FastEffects` motion tokens; MDC/material-web transitions ≈ **150 ms in / 75 ms out**, fade-dominant.

## §A. Accessibility

| Concern | Web (MDC) | Compose | Swing adaptation |
|---|---|---|---|
| Plain / non-interactive rich | anchor `aria-describedby` → `role="tooltip"` element | `paneTitle = tooltipText`, `liveRegion = Assertive` | anchor `AccessibleContext.setAccessibleDescription(text)` (the same wiring `JComponent.setToolTipText` performs); surface role `AccessibleRole.TOOL_TIP` `[CODE]` |
| Interactive (persistent) rich | anchor `aria-haspopup="dialog"` + `aria-expanded`; tooltip `role="dialog"`, `tabindex="-1"` | forced focusable under TalkBack only | surface keeps `TOOL_TIP` role + accessible name; anchor description carries subhead+text; no focus theft `[CODE]` |
| Dismiss | Esc | `dismiss()` | Esc while shown `[CODE]` |
| WCAG 1.4.13 | hoverable / dismissible / persistent | — | hover-onto-tooltip keeps open; Esc; no hover timeout `[CODE]` |

## §E. Expressive check

The M3 Expressive wave (May 2025) published **no tooltip respec** — no new variants, no shape/size classes, no deprecations. The plain/rich pair above *is* the current spec. Nothing to stub, nothing to exclude.

## §G. Token-table ghosts & platform extras (parked, with breadcrumbs)

- **Caret/arrow** — Compose added `TooltipDefaults.caretSize` (16×8dp) as an **opt-in** platform extra; the M3 anatomy diagrams show no arrow, MDC Web draws none, and no caret token exists in either token sheet. **Parked** — revisit only if M3 publishes caret anatomy.
- **Touch long-press trigger + 1.5 s auto-timeout** — touch-platform flow (Compose `TooltipDuration`); meaningless under a mouse. Parked; Swing has no touch story.
- **`TooltipAnchorPosition.Left/Right`** (Compose side placement) — implementation nicety, not in the M3 placement language ("above or below"). Parked.

## §Open — questions resolved into design locks

1. **Reuse `AbstractElwhaOverlay` despite its focus machinery?** → Yes, with a passive-focus opt-out hook; the mount/motion/teardown plumbing is exactly what a tooltip needs. → design §2 (S1 spike locks).
2. **Show/hide delay defaults?** MDC documents the setters but not defaults; its foundation constants are 500/600 ms; Swing legacy is 750/500 reshow. → **500 ms show / 600 ms hide**, both settable. → design §7.
3. **Rich default horizontal alignment, start or end?** The guideline allows either corner. → **END** default (the common M3 render hangs the tooltip toward the anchor's end), `START`/`CENTER`/`END` settable. → design §5.
4. **Wrapping machinery** — `card/WrappingLabels` vs hand-rolled `FontMetrics` wrap? The card path carries HTML-view baggage (#305 storm). → hand-rolled word wrap in the surface paint. → design §3.
5. **Anchor click actions vs persistent toggle conflict** → document MDC's recommendation (persistent anchors should not have their own click actions); no API enforcement. → design §7.
