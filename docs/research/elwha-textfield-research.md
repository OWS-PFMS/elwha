# ElwhaTextField — M3 Spec Capture (research scratch)

**Status:** RAW CAPTURE — accumulating M3 source material for epic [#286](https://github.com/OWS-PFMS/elwha/issues/286) (ElwhaTextField stub, `v0.4.0`). Not a design doc yet; this is the companion research dump (mirrors [`elwha-menu-research.md`](elwha-menu-research.md) / [`elwha-navigation-rail-research.md`](elwha-navigation-rail-research.md)). Promote a real `elwha-textfield-design.md` when Phase 0 runs.

**Captured:** 2026-06-04. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Consumers / related:**
- **OWS-PFMS/OWS-Local-Search-GUI** — has hand-rolled form inputs that migrate onto `ElwhaTextField` once Phase 1 ships (file a consumer-side migration tracker when the API stabilises).
- **31 raw text-input sites inside this repo** (`new JTextField`/`JTextArea`/`JFormattedTextField`) across Showcase/playgrounds — the dogfood target ([[feedback_dogfood_elwha_components]]).
- [#287](https://github.com/OWS-PFMS/elwha/issues/287) — ElwhaTopAppBar (a search-field variant may compose a text field; coordinate).
- [`FlatLafKeyMapping`](../../src/main/java/com/owspfm/elwha/theme/FlatLafKeyMapping.java) — **already themes** `TextField`/`FormattedTextField`/`TextArea`/`TextComponent` onto Elwha tokens (background, foreground, `placeholderForeground`, selection colors). The decorator builds directly on this.

---

## §0. Scope decision + the load-bearing question

### Variants — Expressive-first
M3 ships two text-field variants — **Filled** and **Outlined** — and **both are current under M3 Expressive** (neither is deprecated baseline, unlike the menu's square variant). So unlike #298, there is **no variant to exclude**: `ElwhaTextField` ships **`FILLED` + `OUTLINED`** (mirrors `ElwhaCard`/`ElwhaChip` per-variant naming). [[project_elwha_m3_expressive]]

### ⚠️ The central architecture question — wrap-vs-extend [LOAD-BEARING, lock via S1 spike]
Unlike `ElwhaButton extends JComponent` (where Elwha owns the whole paint), **text input is genuinely hard to reimplement**: caret, selection, IME/composition, copy-paste, undo, drag-select, and `AccessibleJTextComponent` all live in Swing's `JTextComponent`. Two paths:

- **(A) Thin decorator over a real `JTextField`/`JFormattedTextField`** — Elwha owns the chrome (container/outline/active-indicator paint), the **floating label**, token mapping, and the typed error/supporting-text API; the embedded `JTextComponent` owns the editing surface. **FlatLaf already does much of the styling** (`JTextField.placeholderText`, `TextComponent.arc`, `*.outline`, `leadingComponent`/`trailingComponent`, focus coloring), and `FlatLafKeyMapping` already wires text-component colors to Elwha tokens.
- **(B) Fuller custom** `JComponent` reimplementing the editing surface — rejected on cost/risk (re-deriving caret/IME/a11y) absent a concrete need.

**Provisional recommendation: (A) thin decorator.** Greenfield (no FlatLaf client-property usage in the repo yet), inherits a11y for free, and the existing key-mapping means a raw `JTextField` already half-themes. **Final boundary locked by the S1 spike** (per the dialog/menu S1 precedent). Captured here; decided in Phase 0 / design doc.

---

**Source URLs:**
- M3 spec (overview/specs/guidelines): https://m3.material.io/components/text-fields/overview *(JS-only — screen-cap source)*
- Material Web `text-field.md`: https://github.com/material-components/material-web/blob/main/docs/components/text-field.md
- MDC-Android `TextField.md`: https://github.com/material-components/material-components-android/blob/master/docs/components/TextField.md

---

## §TL;DR — synthesis (read this first)

**What ElwhaTextField is:** the M3 **text field** — a labeled, token-themed input with two chrome variants, supporting/error text, icon + prefix/suffix slots, and the floating-label motion. The first-class form input Elwha lacks (31 raw `JTextField` sites internally + the OWS consumer drop to vanilla Swing today).

**Settled by the capture (well-evidenced):**
- **Two variants `FILLED` / `OUTLINED`** — both Expressive-current (no deferral), a **chrome-only axis with one identical API** ("same functionality", §G). Filled = `surface-container-highest` fill + bottom **active indicator**, **top-rounded** XS. Outlined = transparent fill + full **outline** that **notches around the floated label**, **all-four** XS. (§A, §A2, §O, §G)
- **Architecture = thin decorator over a real `JTextComponent`** (Path A). The states, **read-only** (`setEditable`), and especially **a11y** (role/name/caret/selection/editing-keys/Tab all free; §X) make reimplementation pointless. Elwha owns chrome paint + label-float + slots + token mapping + the error→alert wiring. **The exact decorator boundary is the S1 spike.** (§0, §X)
- **Zero new theme tokens.** Every color/type/shape/space/state need maps onto existing roles — full table in §Tokens. (§T1–§T5, §O2, §M)
- **States:** enabled · hover · focus · error · disabled · **read-only**. Filled active indicator: resting `on-surface-variant` → hover `on-surface` → focus `primary`. Outlined outline: resting `OUTLINE` → focus `PRIMARY`. Disabled = `on-surface` @ 0.04 container / 0.38 content. **Error beats focus** (caret/indicator/label go `error`, input stays `on-surface`, leading icon stays neutral, trailing icon expresses error). (§T1–§T5, §O2)
- **Tokens/type:** label `BODY_LARGE` (resting) → `BODY_SMALL` (floated); input `BODY_LARGE`; supporting/counter `BODY_SMALL`; caret `PRIMARY`; icons **24dp** glyph. (§T1)
- **Measurements:** **56dp** height/target, `16/12dp` L-R (no-icon/icon), `8dp` top-bottom, `16dp` icon↔text, `4dp` supporting-top, `4dp` outlined label-notch, **`488dp` max width** — all on `SpaceScale`. (§M, §O3)
- **Configurations:** supporting text · leading/trailing icons · prefix · suffix · multi-line. (§Cfg)
- **A11y:** role `Textbox`, name = label (+`*` if required, + supporting), description = supporting text, error → "alert" announcement (the **one Swing gap**), interactive trailing icon = `ElwhaIconButton` (Button role), 3:1 outline contrast + non-color error cue. (§X)
- **Deferred by M3 itself:** **density** — "don't apply by default" → out of V1 (§GD3).

**Open decisions (Phase C — see the chat list / §Open):** (1) confirm the **decorator** + S1 spike; (2) the **input-text trichotomy** — single-line vs multi-line auto-grow vs fixed text-area: V1 phases or a follow-on epic; (3) **validation surface** = visual-only `setError`/`setErrorText` with a consumer-owned engine; (4) **Phase-1 vs later-V1** cut for required-asterisk / character-counter / prefix-suffix / supporting-text-visibility / auto-error-icon; (5) **follow-up stub epics** — select/dropdown (composes `ElwhaMenu` #298), search field, formatted/numeric.

**Flagged, unresolved by capture:** focus-stroke **2dp vs 3dp** (lean 3dp Expressive; eye-confirm in build); Filled callout-#10 "on surface" overview vs `on-surface-variant` token sheet (**trust token sheet**); Outlined per-state hexes taken as **parity** with Filled.

### Reading-order TOC (sections accrued in capture order)
- **Scope & architecture:** §0 (variants + wrap-vs-extend) · §G/§G2 (guidelines, variant parity)
- **API shape:** §B (Material Web) · §P (terminology→API lock) · §Q (architecture insight)
- **Anatomy:** §A/§A2 (Filled, 10-part) · §O1 (Outlined, 9-part)
- **Tokens & color:** §T1 Enabled · §T2 Disabled · §T3 Hover · §T4 Focus · §T5 Error · §Color (Filled roles + #10 conflict) · §O2 (Outlined roles) · **§Tokens master map**
- **Measure:** §M (Filled redlines/table) · §O3 (Outlined table)
- **States/config:** §S · §Cfg · §O4
- **Element guidelines:** §GD (containers/label/input/prefix/suffix/supporting/error) · §GD2 (icons/read-only) · §GD3 (adaptive/density)
- **A11y:** §X · **Open questions** · **§F screenshot log**

---

## §B. Material Web API shape `[WEB — material-web/text-field.md]`

Functionally **filled ≡ outlined** (same API, different chrome). Properties (verbatim names):

**Text content / display**
- `label` — floating label. `value` — current text. `placeholder` — empty-state hint. `prefix-text` / `suffix-text` — fixed affixes around the value.

**Type / validation**
- `type` — `text | textarea | email | number | password | search | tel | url` (default `text`).
- `required` (renders asterisk + enforces) · `no-asterisk` (suppress the asterisk) · `pattern` (regex) · `maxlength` / `minlength` (`-1` = none; **`maxlength` shows a character counter**) · `min` / `max` / `step` (numeric/date).

**State / behavior**
- `error` (boolean — force invalid visual) · `error-text` (replaces supporting text when `error`) · `supporting-text` (info line below) · `disabled` · `readonly` · `inputmode` · `autocomplete`.

**Textarea-specific:** `rows` (default 2) · `cols` (default 20).

**Slots:** `leading-icon` · `trailing-icon` (icon describing input / providing function / expressing error).

**Methods:** `select()`, `setSelectionRange()`, `setRangeText()`, `showPicker()`, `stepUp()/stepDown()`, `reset()`, `checkValidity()`, `reportValidity()`, `setCustomValidity()`.
**Events:** `select`, `change`, `input`.
**Validation:** `validity` (ValidityState), `validationMessage`, `willValidate`.

**A11y:** label floats on focus/entry, always visible; use `aria-label` for external labels (`aria-labelledby` unsupported in MW).

> Mapping note: web validation (`pattern`/`checkValidity`/ValidityState) is a browser-platform surface. For Swing, the *visual* error contract (`error` + `error-text`) is the [CODE] part; the validation *engine* is consumer-owned (Elwha exposes `setError(boolean)` / `setErrorText`, not a regex validator) — confirm scope in Phase C.

---

## §C. MDC-Android anatomy + redlines `[WEB — MDC TextField.md]`

**Variants:** **Filled** = higher emphasis, colored container; **Outlined** = lower emphasis, border, better for dense multi-field layouts.

**Anatomy — Filled:** (1) container · (2) leading icon *(opt)* · (3) label (empty + populated) · (4) trailing icon *(opt)* · (5) active indicator (focus) · (6) caret · (7) input text · (8) supporting text *(opt)*.
**Anatomy — Outlined:** (1) container outline (enabled + focus) · (2) leading icon *(opt)* · (3) label (unpopulated + populated, **notches the outline**) · (4) trailing icon *(opt)* · (5) caret · (6) input text · (7) supporting text *(opt)*.
**Both also:** prefix/suffix · helper text · error text + icon · character counter.

**Dimensions `[WEB — confirm against Tokens screenshot]`:**

| Element | Value |
|---|---|
| Default layout width | `245dp` |
| Maximum width | `488dp` |
| Min width (no label) | `56dp` |
| Min width (with label) | `88dp` |
| Stroke width (resting) | `1dp` |
| Stroke width (focused) | `2dp` |
| Leading/trailing icon touch size | `48dp` |
| Corner radius | `shapeAppearanceCornerExtraSmall` → **ShapeScale.XS (4dp)** |

> Filled-corner nuance to verify in screenshots: filled fields round the **top** corners (XS) with a square bottom edge behind the active indicator; outlined fields round **all four** at XS.

**Typography `[WEB — confirm]`:** label `BodySmall` · input text `Body Large` *(MDC says "textAppearanceLarge"; M3 token = Body Large)* · helper/error/counter `BodySmall` · prefix/suffix `TitleMedium`.

**States:** enabled · hover · focused (indicator/outline → primary) · error (error color on indicator/label/supporting + error icon) · disabled (reduced opacity).

**A11y (Android idioms — translate to Swing):** hint/label belongs on the layout wrapper, not the raw editor (Swing analog: label is the decorator's, accessibleName flows to the embedded `JTextComponent`); custom icons need content descriptions; error messages get an accessible description.

---

## §A. Overview `[SCREENSHOT 2026-06-04]`

**Render (read the image):** two populated fields side by side —
- **(1) Filled** — tinted container, **top corners rounded, bottom edge square**, a thin **bottom active indicator** rule; the floating label ("Text field") sits small at top-left *inside* the container with the value below it; **supporting text** beneath the field. ✅ confirms §C filled anatomy (top-only XS, active indicator at bottom).
- **(2) Outlined** — **all four corners rounded** at the same small radius, the label **notches the top stroke** (outline breaks around the label); value inside; supporting text beneath. ✅ confirms §C outlined anatomy (label-notch).

Both shown in the **populated / label-floated** state. The two fields read as the same width — consistent with the `245dp` default layout width (§C).

**Guidelines on this page (CODE vs DOC):**
- `[CODE]` **Two variants: filled and outlined** → `Variant{FILLED, OUTLINED}`.
- `[DOC]` Make sure text fields look interactive.
- `[DOC]` The field's **state** (blank, with input, error, etc.) should be visible at a glance — *drives* the state-painting work (states themselves are `[CODE]`), but as authored guidance it's DOC.
- `[DOC]` Keep labels and error messages brief and easy to act on.
- `[DOC]` Text fields commonly appear in **forms and dialogs** — placement note. Relevant coupling: a field opened inside an `ElwhaDialog` (#254) must theme/behave correctly; reinforces the forms dogfood target.

---

## §T1. Tokens — **Enabled** state, **Filled** variant `[SCREENSHOT 2026-06-04 — verbatim]`

Operator note: *"Those are enabled tokens."* → this batch is the resting/unfocused **Enabled** state of the **Filled** field only. Focus / error / disabled / hover states + the Outlined token set come later. Hex values are the **M3 baseline light** scheme, so each resolves to a named role.

| Token (verbatim) | Value | M3 baseline role | Elwha token |
|---|---|---|---|
| Filled · container color | `#E6E0E9` | surface-container-highest | `ColorRole.SURFACE_CONTAINER_HIGHEST` ✅ |
| Filled · container height ⚠️ | `56dp` | — | layout constant (✅ = §C redline) |
| Filled · container shape | top-rounded rect | cornerExtraSmall (top) | `ShapeScale.XS`, **top corners only** |
| Filled · label text color | `#49454F` | on-surface-variant | `ColorRole.ON_SURFACE_VARIANT` ✅ |
| Filled · label text (resting) | Roboto 16pt / 24pt lh / w400 / 0.5 track | Body Large | `TypeRole.BODY_LARGE` |
| Filled · label text **populated** | 12pt / 16pt lh | Body Small | `TypeRole.BODY_SMALL` |
| Filled · leading icon color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` ✅ |
| Filled · leading icon size | `24dp` | — | **glyph 24dp** (≠ 48dp touch target — see ⚠️ below) |
| Filled · trailing icon color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` ✅ |
| Filled · trailing icon size | `24dp` | — | glyph 24dp |
| Filled · active indicator height | `1dp` | — | resting stroke (focus = 2dp, §C) |
| Filled · active indicator color ⚠️ | `#49454F` | **on-surface-variant** | `ON_SURFACE_VARIANT` — **CORRECTION, see below** |
| Filled · supporting text color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` ✅ |
| Filled · supporting text | Roboto 12pt / 16pt lh / w400 / 0.4 track | Body Small | `TypeRole.BODY_SMALL` |
| Filled · input text color | `#1D1B20` | on-surface | `ColorRole.ON_SURFACE` ✅ |
| Filled · input text | Roboto 16pt / 24pt lh / w400 / 0.5 track | Body Large | `TypeRole.BODY_LARGE` |
| Filled · input prefix color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · input suffix color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · input placeholder color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` (already = FlatLaf `placeholderForeground`) |
| Filled · caret color | `#6750A4` | primary | `ColorRole.PRIMARY` ✅ |

### ⚠️ CORRECTIONS this batch supersedes
1. **Filled active indicator (resting) = `on-surface-variant`, NOT `outline`.** My §Tokens mapping and §C both assumed the resting indicator/outline used `colorOutline`. The token sheet shows the **Filled** field's resting bottom active indicator is `#49454F` = **on-surface-variant**. So: *Filled active indicator (resting)* → `ON_SURFACE_VARIANT`; *Outlined outline (resting)* → `OUTLINE` (to confirm when the Outlined token sheet arrives — they are **different roles**). Rows fixed below.
2. **Label typography is two-state, and my BodyLarge→BodySmall float was right.** §C transcribed MDC's "Label = BodySmall" as a single value — the token sheet shows that's only the **populated** label (12pt); the **resting** label is **Body Large** (16pt). The §Tokens float row stands; §C's single-value line is corrected.
3. **Icon size = 24dp glyph.** §C's "leading/trailing icon size `48dp`" is the **touch target**; the painted **glyph is 24dp** (the menu's visual-vs-touch pattern again). Both true; row clarified.
4. **Prefix/suffix:** color confirmed `on-surface-variant`; the MDC "TitleMedium" **type** claim is **unverified** by this sheet (no prefix/suffix size token shown) — flag, confirm later.

---

## §T2. Tokens — **Disabled** state, **Filled** variant `[SCREENSHOT 2026-06-04 — verbatim]`

Uniform M3 disabled recipe: **all elements use `on-surface` (`#1D1B20`)**, differentiated only by **opacity**. Container is barely-there (0.04); all content is 0.38.

| Token (verbatim) | Color | Opacity | Elwha mapping |
|---|---|---|---|
| Filled · disabled container | `#1D1B20` (on-surface) | `0.04` | `StateLayer.DEFAULT_DISABLED_CONTAINER` ✅ |
| Filled · disabled label text | `#1D1B20` | `0.38` | `StateLayer.DEFAULT_DISABLED_CONTENT` ✅ |
| Filled · disabled leading icon | `#1D1B20` | `0.38` | `DEFAULT_DISABLED_CONTENT` ✅ |
| Filled · disabled trailing icon | `#1D1B20` | `0.38` | `DEFAULT_DISABLED_CONTENT` ✅ |
| Filled · disabled supporting text | `#1D1B20` | `0.38` | `DEFAULT_DISABLED_CONTENT` ✅ |
| Filled · disabled input text | `#1D1B20` | `0.38` | `DEFAULT_DISABLED_CONTENT` ✅ |
| Filled · disabled active indicator | `#1D1B20`, height `1dp` | `0.38` | `DEFAULT_DISABLED_CONTENT` ✅ |

**Finding:** the disabled state is **`on-surface` @ 0.04 (container) / 0.38 (content)** — Elwha's library-wide disabled overlay convention (the same `DEFAULT_DISABLED_CONTAINER`/`DEFAULT_DISABLED_CONTENT` keys `ElwhaButton`/`ElwhaCard` use). **Zero new tokens**; and if the decorator wraps a real `JTextComponent`, `setEnabled(false)` should drive this so Swing's own disabled semantics (no caret, not focusable) come along.

---

## §T3. Tokens — **Hovered** state, **Filled** variant `[SCREENSHOT 2026-06-04 — verbatim]`

| Token (verbatim) | Value | M3 role | Elwha mapping |
|---|---|---|---|
| Filled · hover label text color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` (unchanged from enabled) |
| Filled · hover **state layer** color | `#1D1B20` | on-surface | `StateLayer.HOVER` base |
| Filled · hover **state layer** opacity | `0.08` | — | `StateLayer.HOVER` ✅ (Elwha's standard hover overlay) |
| Filled · hover leading icon color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · hover trailing icon color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · hover input text color | `#1D1B20` | on-surface | `ON_SURFACE` |
| Filled · hover supporting text color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · hover active indicator height | `1dp` | — | unchanged |
| Filled · hover active indicator color ⚠️ | `#1D1B20` | **on-surface** | **darkens** from enabled `on-surface-variant` → hover `on-surface` |

**Findings:**
- Hover adds a **state-layer overlay** = `on-surface @ 0.08` on the container → Elwha `StateLayer.HOVER` exactly. Zero new tokens.
- The **active indicator deepens** on hover: enabled `on-surface-variant` (#49454F) → hover `on-surface` (#1D1B20). The only color that changes on hover (besides the overlay); label/icons/input/supporting are unchanged. [CODE] worth honoring in the chrome paint.
- If the decorator wraps a real `JTextComponent`, FlatLaf can supply the hover overlay; confirm in the S1 spike whether to lean on FlatLaf's hover or paint our own (the active-indicator deepen is ours regardless).

---

## §T4. Tokens — **Focused** state, **Filled** variant `[SCREENSHOT 2026-06-04 — verbatim]`

| Token (verbatim) | Value | M3 role | Elwha mapping |
|---|---|---|---|
| Filled · focus label text color | `#6750A4` | primary | `ColorRole.PRIMARY` — **label goes primary on focus** ✅ |
| Filled · focus leading icon color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` (unchanged) |
| Filled · focus trailing icon color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · focus input text color | `#1D1B20` | on-surface | `ON_SURFACE` |
| Filled · focus supporting text color | `#49454F` | on-surface-variant | `ON_SURFACE_VARIANT` |
| Filled · focus active indicator **height** ⚠️ | `2dp` | — | see ambiguity below |
| Filled · focus active indicator color | `#6750A4` | primary | `ColorRole.PRIMARY` ✅ |
| Filled · focus active indicator **thickness** ⚠️ | `3dp` | — | see ambiguity below |

**⚠️ Ambiguity — focus indicator has TWO size tokens:** `height = 2dp` **and** `thickness = 3dp`. Best read: `height` is the legacy MDC value (2dp) and `thickness` is the **M3 Expressive** bump (3dp) — Elwha targets Expressive ([[project_elwha_m3_expressive]]), so **lean 3dp**, but **confirm against the Measurements/redline page** before locking. Resting indicator stays 1dp (§T1); focus → 2–3dp + primary.

## §T5. Tokens — **Error** family, **Filled** variant `[SCREENSHOT 2026-06-04 — verbatim]`

Three sub-states captured: **Error** (resting), **Error/Focus**, **Error/Hover**. Error color = `#B3261E` = **error**; input stays on-surface; **leading icon stays neutral**; **trailing icon expresses the error**.

**Error (resting):**
| Element | Value | Role |
|---|---|---|
| error active indicator | `#B3261E` | error |
| error label text | `#B3261E` | error |
| error input text | `#1D1B20` | on-surface (unchanged) |
| error supporting text | `#B3261E` | error |
| error leading icon | `#49454F` | on-surface-variant (**stays neutral**) |
| error trailing icon | `#B3261E` | error (**the error-expressing icon**) |

**Error/Focus** — same as Error resting (error beats focus: indicator/label stay error, **not** primary) **plus**: `error focus caret color = #B3261E` (error) — ⚠️ **caret goes error, not primary, in an errored focused field.**

**Error/Hover:**
| Element | Value | Role |
|---|---|---|
| error hover active indicator | `#8C1D18` | **darker error** (see note) |
| error hover label text | `#8C1D18` | darker error |
| error hover input text | `#1D1B20` | on-surface |
| error hover supporting text | `#B3261E` | error (**not** deepened) |
| error hover leading icon | `#49454F` | on-surface-variant |
| error hover trailing icon | `#8C1D18` | darker error |
| error hover state layer | `#1D1B20` @ `0.08` | `StateLayer.HOVER` (on-surface 0.08) |

**Findings / mapping:**
- **Error precedence:** error color overrides focus (no primary when errored) and the **caret turns error** too. [CODE] precedence rule for the chrome.
- **Role split in error:** error tints active-indicator / label / supporting / **trailing** icon; **input text → on-surface**, **leading icon → on-surface-variant** (stay neutral). [CODE]
- **`#8C1D18` is not a clean baseline role** — it's `error` deepened by the hover overlay (≈ `ERROR` composited under `StateLayer.HOVER`). The hover state-layer (on-surface @ 0.08) is unchanged from normal hover; it just composites over error-colored elements. **No new token** — paint `ERROR` + apply the hover state layer (confirm the exact composite in the S1/state-paint story). Note supporting text is **not** deepened (stays `#B3261E`), so the deepen applies to the interactive strokes/icons only.

---

## §A2. Filled anatomy — authoritative 10-part callout `[SCREENSHOT 2026-06-04]`

The spec anatomy page numbers **10** parts (richer than §C's web list — it splits empty/populated label and enabled/focused indicator):

1. **Container** · 2. **Leading icon** (optional) · 3. **Label text in empty field** · 4. **Label text in populated field** · 5. **Trailing icon** (optional) · 6. **Focused active indicator** · 7. **Caret** · 8. **Input text** · 9. **Supporting text** (optional) · 10. **Enabled active indicator**.

Load-bearing reads: parts **6 vs 10** are the *same stroke in two states* (enabled vs focused) — confirms the active indicator is one element with state-driven height+color, not two. Parts **3 vs 4** confirm the label is one element with an empty-position (centered, resting) and a populated-position (floated, top) — the floating-label animation. The render shows the trailing icon as a clear/error (X / !) affordance.

## §Color. Filled color-roles map `[SCREENSHOT 2026-06-04]` — ⚠️ CONFLICT to resolve

The "Filled text field color" page maps each anatomy callout to a role (light + dark identical role names):

| # | Anatomy part | Color-roles page says | §T-token-sheet says |
|---|---|---|---|
| 1 | container | Surface container highest | `#E6E0E9` surface-container-highest ✅ |
| 2 | leading icon | On surface variant | ✅ |
| 3 | label (empty) | On surface variant | ✅ |
| 4 | label (populated) | **Primary** | focus label `#6750A4` primary ✅ *(this render is the focused state)* |
| 5 | trailing icon | On surface variant | ✅ |
| 6 | focused active indicator | Primary | ✅ |
| 7 | caret | Primary | ✅ |
| 8 | input text | On surface | ✅ |
| 9 | supporting text | On surface variant | ✅ |
| 10 | **enabled active indicator** | **On surface** | ⚠️ **§T1 token sheet = `#49454F` on-surface-variant** |

**⚠️ CONFLICT (#10 enabled active indicator):** the color-roles overview labels it **On surface** (`#1D1B20`), but the authoritative **Enabled/Filled token sheet (§T1)** gives the exact value **`#49454F` = on-surface-variant**. These disagree. **Trust the token sheet** (exact hex, per-token) over the overview diagram (simplified, often rounds role names) — so resting active indicator = **`ON_SURFACE_VARIANT`** — **but flagging for the operator** rather than silently picking. (This does not change the zero-new-tokens result either way; both are existing roles.) *Note #4 "populated label = Primary" is not a conflict — the depicted field is focused, and §T4 confirms focus label = primary; the **enabled** populated label is on-surface-variant.*

## §S. States gallery `[SCREENSHOT 2026-06-04]` (visual confirm, light + dark)

**Filled states (×8):** Enabled / Focused / Hovered / Disabled, each in **empty** and **populated** rows. **Filled error states (×6):** Enabled / Focused / Hovered, empty + populated, all showing the **error (!) trailing icon** and red supporting text. Both galleries shown in **light and dark** — dark-mode parity confirmed (auto-themes via tokens; no per-mode special-casing). No new values; visual cross-check of §T1–§T5.

## §M. Measurements — authoritative spec table `[SCREENSHOT 2026-06-04 — verbatim, redline-cross-checked]`

| Attribute (verbatim) | Value | Elwha note |
|---|---|---|
| Default container height | `56dp` | ✅ = §T1 container height |
| Label alignment (unpopulated) | Vertically centered | empty-label resting position |
| Top/bottom padding | `8dp` | `SpaceScale` 8 |
| Left/right padding **without** icons | `16dp` | `SpaceScale` 16 |
| Left/right padding **with** icons | `12dp` | `SpaceScale` 12 |
| Icon alignment | Vertically centered | |
| Padding between icons and text | `16dp` | |
| Supporting text & character-counter top padding | `4dp` | below the field |
| Padding between supporting text and character counter | `16dp` | counter is right-aligned |
| Target size | `56dp` | the field height **is** the touch target |

**Cross-check vs §C web redlines:** height 56dp ✅. The redline images confirm: without icons → 16dp L/R; with icons → 12dp edge-to-icon + 24dp glyph + 16dp icon-to-text; 8dp top/bottom; supporting text 4dp below; counter shows e.g. `5/20`. **All paddings land on existing `SpaceScale` steps (4/8/12/16) — zero new spacing tokens.**

**⚠️ Still unresolved — focus indicator stroke:** the Measurements table **omits stroke width**, so it does **not** settle the §T4 `height 2dp` vs `thickness 3dp` ambiguity. Web §C said focused = 2dp. **Provisional: 3dp (Expressive bump), low confidence — lock in the S1/state-paint story by eye against the M3 render.** Resting = 1dp (firm, §T1).

## §Cfg. Configurations matrix `[SCREENSHOT 2026-06-04]`

The "Filled text field configurations" page enumerates the supported feature combos (empty + populated each):
1. **Supporting text** · 2. **Trailing icon** · 3. **Leading icon** · 4. **Leading and trailing icons** · 5. **Prefix** (e.g. `$1.43`) · 6. **Suffix** (e.g. `25 lbs`) · 7. **Multi-line text field** (wraps overflow onto a new line).

**Scope signal:** prefix, suffix, leading/trailing icons, supporting text are all **first-class M3 configs** (→ V1 anatomy slots). **Multi-line** is shown as a config of the *same* component (not a separate widget) — but it's a meaningfully different editor (`JTextArea` vs `JTextField`); **the wrap-vs-extend S1 spike must decide whether multi-line is a V1 phase or a follow-up** (see §Open Q2). The `$`/`lbs` renders confirm prefix/suffix are **inline, fixed affixes inside the field** (not the label).

---

## §O. Outlined variant — anatomy, color, measurements `[SCREENSHOT 2026-06-04]`

The Outlined spec set (anatomy · color-roles · states · error states · measurements · spec table · configurations). Structurally parallel to Filled; the differences are the load-bearing part.

### §O1. Outlined anatomy — 9-part callout
1. **Enabled container outline** · 2. **Leading icon** (opt) · 3. **Label (empty)** · 4. **Label (populated)** · 5. **Trailing icon** (opt) · 6. **Focused container outline** · 7. **Caret** · 8. **Input text** · 9. **Supporting text** (opt).

**Key difference vs Filled:** the Filled active indicator (parts 6+10) is replaced by a **full container outline** in two states — **enabled outline (1)** and **focused outline (6)**. The **populated label notches the top stroke** (the outline breaks around the floated label). No bottom-only indicator; the stroke wraps all four XS corners.

### §O2. Outlined color-roles — ✅ confirms the §T1 flag
| # | Part | Role |
|---|---|---|
| 1 | **enabled container outline** | **Outline** ✅ |
| 2 | leading icon | On surface variant |
| 3 | label (empty) | On surface variant |
| 4 | label (populated, focused render) | Primary |
| 5 | trailing icon | On surface variant |
| 6 | focused container outline | Primary |
| 7 | caret | Primary |
| 8 | input text | On surface |
| 9 | supporting text | On surface variant |

**✅ CONFIRMED:** the **Outlined resting outline = `OUTLINE`** (callout #1) — resolving the flag I left in §T1. So the two variants genuinely differ on the resting stroke role: **Filled resting active indicator = `ON_SURFACE_VARIANT`** (§T1 token sheet), **Outlined resting outline = `OUTLINE`** (§O2). Both existing roles → still zero new tokens. *(Unlike Filled's §Color #10 "On surface" overview/token conflict, Outlined #1 is internally consistent.)* Focused outline → `PRIMARY` (same as Filled focus).

### §O3. Outlined measurements — spec table `[verbatim]`
| Attribute | Value | vs Filled |
|---|---|---|
| Container height | `56dp` | same |
| Left/right padding without icons | `16dp` | same |
| Left/right padding with icons | `12dp` | same |
| Padding between icons and text | `16dp` | same |
| Icon alignment | Vertically centered | same |
| Supporting text & counter top padding | `4dp` | same |
| Padding between supporting text & counter | `16dp` | same |
| Label alignment | Vertically centered | same |
| **Left/right padding populated label text** | **`4dp`** | ⚠️ **Outlined-only** — the label-notch gap |
| Target size | `56dp` | same |

**New value:** the **populated-label notch padding = 4dp** (horizontal gap the floated label cuts into the outline — visible as the `4`/`4` redlines around the label). `SpaceScale` 4 → no new token. Outlined table **omits Filled's explicit `8dp` top/bottom** (content is vertically centered within the outline). Focused-outline **stroke width is again not dimensioned** — same 2dp/3dp question as §T4/§M (unresolved; lean 3dp Expressive).

### §O4. Outlined states / error / configurations
- **States (×8)** and **error states (×6)** galleries shown light + dark — same matrix as Filled (Enabled/Focused/Hovered/Disabled × empty/populated; error adds the `!` trailing icon). 
- **Hover difference:** Outlined has a **transparent container**, so there's no hover **state-layer fill** like Filled's `on-surface @ 0.08` — the hover read is an **outline color deepen** (to `on-surface`), mirroring Filled's indicator-deepen. [CODE] the chrome paints the outline darker on hover, no container overlay.
- **Configurations (×7):** identical to Filled — supporting text · trailing icon · leading icon · both · prefix (`$1.43`) · suffix (`25 lbs`) · multi-line.
- **No per-state Outlined token sheets were captured** (only color-roles + redlines). Per-state exact values (disabled 0.04/0.38, focus stroke) are taken as **parity with Filled §T2–§T5** on the shared roles — flag if a value needs the exact Outlined sheet.

---

## §G. Guidelines / Usage `[SCREENSHOT 2026-06-04]`

- `[DOC]` **When to use:** "Use a text field when someone needs to enter text into a UI, such as filling in **contact or payment information**." → README intro / class Javadoc.
- `[CODE]` **Two variants, same functionality:** "Both variants use a container to provide a visual cue for interaction and **provide the same functionality**." → confirms `Variant{FILLED, OUTLINED}` is a *chrome-only* axis; the **whole API surface is identical across variants** (no variant-specific methods). Matches `ElwhaCard`/`ElwhaChip` variant doctrine.
- `[DOC]` **Variant choice:** Filled = higher visual emphasis; **Outlined = lower emphasis, better when many fields are placed together (forms)** — "their reduced emphasis helps simplify the layout." → Javadoc on each factory + README guidance.
- `[DOC]` **Form-row icons live outside the field.** The Usage contact-form render shows person/phone/location icons to the **left of** the fields (form-row affordances), *not* in the field's own `leading-icon` slot. Worth a Javadoc note: the field's leading-icon slot is for in-field affordances; decorative row icons are the consumer's layout. (Avoids consumers cramming row icons into the slot.)
- `[DOC→CODE hint]` **Clear affordance pattern.** Both the focused Phone field and the login Username show a trailing **clear (✕)** button on the populated/focused field — i.e. the `trailing-icon` slot commonly hosts a clear action. Not a distinct API (it's just a trailing icon + consumer action), but the Showcase should demonstrate it.
- `[DOC]` Renders confirm **outlined fields stack cleanly in forms** (login: Username + Password) — reinforces dense-form usage.

### §G2. Choosing a variant + mixing rules `[SCREENSHOT 2026-06-04]`
- `[DOC]` **Variant choice is style-only:** "Both variants provide the same functionality. The variant used can depend on **style alone**." Choose the one that: works best with the app's visual style · best accommodates the UI's goals · is **most distinct from other components (like buttons)** and surrounding content. → factory Javadoc.
- `[DOC]` **Don't intermix variants in one region:** if both variants appear in a UI, use each **consistently within different sections** — e.g. outlined in one section, filled in another — **never both next to each other or within the same form** (explicit Do/Don't). → README guidance.
- `[DOC]` **Text-field-in-dialog confirmed:** the "Add custom field" Do-render puts a text field **inside a dialog** → reinforces the `ElwhaDialog` (#254) coupling noted in §A; the field must theme/focus correctly when hosted in an overlay.
- `[DOC]` **Picker trailing-icon pattern:** Email (mail glyph) and Birthday (calendar glyph) rows show specialized fields using the **trailing icon to launch a picker** (date/etc., the web `showPicker()` analog) — a consumer composition, not V1 API; Javadoc can point at it.

**No new tokens / no new scope** from Guidelines — it confirms variant parity (style-only choice, no intermixing) and supplies Javadoc/README copy. The "same functionality across variants" line is the load-bearing API note.

---

## §GD. Element-level guidelines `[SCREENSHOT 2026-06-04]`

*(Anatomy pages in this batch re-confirm §A2 / §O1 — no change. The element guideline pages below add new [CODE] scope signals.)*

**Containers** — `[CODE]` a container has a **fill and a stroke**, "either around the entire container, or just the bottom edge"; stroke **color and thickness change to indicate active**. Corners: **outlined = rounded all four; filled = rounded top, square bottom.** ✅ confirms §A/§O chrome. `[DOC]` containers improve discoverability via contrast.

**Label text** — `[CODE]` "**Every text field should have a label.**" Label is **aligned with input text and always visible**; sits **centered in the empty field**, **moves to the top when the field is selected/populated** (the float). `[DOC]` Don't truncate the label; **don't let it span multiple lines**; keep it short, clear, fully visible.

**Adjacent label** — `[CODE]` **the floating label is OPTIONAL** — "a text field doesn't require a label if the field's purpose is indicated by a separate, adjacent label." → `setLabel(...)` is optional; the field must render correctly **label-less** (no floated label, input vertically centered). `[DOC]` adjacent labels align to the **leading edge** of the container.

**Required text indicator** — `[CODE]` show an **asterisk (`*`) next to the label** for required fields; explain via supporting text or a single note at the form start. `no-asterisk` suppresses it (web §B). `[DOC]` indicate *all* required fields; if required text has a color, use the same color for the asterisk. → maps to M3 `required` / `no-asterisk`. **Scope Q** (V1 or later phase).

**Input text — three behaviors** `[CODE]` (load-bearing for the wrap-vs-extend call):
- **Single-line** — one line; cursor at right edge **scrolls text left**; "not suitable for long responses — use a multi-line field or text area instead." (`JTextField`)
- **Multi-line** — **grows** to accommodate lines; overflow **expands the field, shifting surrounding layout down**; good for compact layouts. (auto-grow `JTextArea`)
- **Text area** — **fixed-height**, **scrolls internally**; larger initial size signals long responses welcome; "use instead of multi-line on web." (`JTextArea` in a `JScrollPane`)
→ This trichotomy is exactly why the S1 spike must scope multi-line/text-area (see §Open Q2). All three are *configurations of the same M3 component*, but **single-line vs multi-line is a `JTextField`-vs-`JTextArea` editor split** in Swing.

**Prefix / Suffix** `[CODE]` — prefix = leading affix (currency `€ 20`); suffix = trailing affix (unit `55/100`, email domain `user@gmail.com`). Inline, inside the field, distinct from label/icons. ✅ confirms §Cfg.

**Supporting text & character counter** `[CODE]`:
- Supporting text — additional info about the field; **ideally one line, may wrap if required**; **either persistently visible OR visible only on focus** → a **visibility mode** worth a typed API (`supportingTextVisibility: ALWAYS | ON_FOCUS`?). **Scope Q.**
- **Character counter** — shown when there's a char/word limit; displays **used/total** ratio (`5/20`), right-aligned below. Driven by `maxLength`. **Scope Q** (V1 or later).

**Error text** `[CODE]` — error text **replaces** supporting text (does **not** add to it) — "swapping prevents new lines of text from bumping content and changing the layout." `[DOC]` describe how to avoid the error (the most likely one if several); **reserve vertical space** between fields so a wrapped multi-line error doesn't bump layout. **Don't** show error text *in addition to* supporting text. → API: `setErrorText` swaps the supporting line; reserve the supporting-text row height even when empty to avoid layout shift.

**Error icon** `[CODE/DOC]` — **strongly recommended** to show an **error icon** (the `!` trailing affordance) in the error state: it's the **non-color sensory cue** for low-vision users (the a11y 3:1 + non-color rule, same as the menu's checkmark). → in error state, the trailing slot should default to an error glyph if none is set. **Scope Q** (auto error-icon vs consumer-supplied).

**New scope questions surfaced here** (added to §Open): label optional (✓ just support it), required-asterisk, supporting-text visibility mode, character counter, multi-line/text-area trichotomy, auto error-icon, reserve-supporting-row-height.

---

## §GD2. Icons, images, read-only `[SCREENSHOT 2026-06-04]`

**Icons & images** — `[DOC]` icons are **optional**; text-field icons can: describe a valid input method (e.g. mic), provide an affordance (e.g. **clear** the field), or **express an error**. `[CODE]` **leading/trailing icons flip position with LTR/RTL** → respect `ComponentOrientation` (FlatLaf/Swing handles this if the decorator uses leading/trailing, not left/right). `[CODE]` **images up to 24dp** can sit inside the field — re-confirms the **24dp** icon/image slot (§T1) and that 24dp gives the right top/bottom padding.

**Icon signifier types** `[DOC]` (trailing/leading affordance taxonomy):
1. **Icon signifier** — describes the required input type; **can be a touch target for a nested component** (e.g. calendar → date picker).
2. **Valid / error icon** — iconography signals valid *and* invalid input → **clear for colorblind users** (non-color cue).
3. **Clear icon** — clears the whole field; `[CODE]` **appears only when input text is present** (a behavior).
4. **Voice input icon** — mic; voice entry.
5. **Dropdown icon** — `[DOC]` a dropdown arrow indicates a text field with a **nested selection component** (the exposed-dropdown / menu-backed *select*) → reinforces the **select/dropdown follow-up stub** (Open Q8); it would compose `ElwhaTextField` + `ElwhaMenu` (#298). **Not V1.**
6. **Image** — contextualizes the input (e.g. credit-card brand).

**Read-only fields** — `[CODE]` **new state:** `readonly` displays pre-filled text the user **cannot edit** but is **styled like a normal field** (not dimmed) and clearly labeled read-only — shown for both Filled and Outlined with "Read only supporting text." **Distinct from `disabled`** (dimmed @ 0.38). → Swing maps cleanly: **`JTextComponent.setEditable(false)` = read-only** (text still selectable/copyable, normal chrome); **`setEnabled(false)` = disabled** (dimmed). Strong **decorator-wins** evidence (both behaviors come free from the wrapped editor). V1, low-cost.

**Net:** confirms 24dp icon slot + RTL handling; adds the **read-only state** (5th+1 state: enabled/hover/focus/error/disabled **+ read-only**); routes **dropdown/select** to a follow-up stub. Still zero new tokens (read-only uses the enabled palette).

---

## §GD3. Adaptive design + Density `[SCREENSHOT 2026-06-04]`

**Adaptive** `[DOC]` — apply **flexible container dimensions**: full-width on **compact** window sizes; on **medium/expanded**, **bound by flexible margins / a max width** (don't keep fixed margins; don't let a field **span the full width of a large screen**, explicit Don't). → ties to the **`488dp` max width** (§C). Mostly consumer-layout guidance; the one [CODE]-adjacent hook is an optional **max-width cap** (mirrors the dialog's `contentMaxWidth` #291) — could expose or just document 488dp. **Lean: document + a simple optional max-width setter; not a V1 must.**

**Density** `[DOC]` — dense fields help scan/act on large amounts of info, **but**: "**Don't apply density by default** — it lowers targets below the recommended **48×48**. Instead let people opt into a denser layout/theme; keep the density toggle's own targets ≥ 48×48." → **Resolves Open Q7: density is deferred / out of V1.** Default = **comfortable, 56dp** (the captured token). If ever added, it's an opt-in, like the menu's deferred density. Matches [[project_elwha_m3_expressive]] (skip optional density tiers for v1).

---

## §X. Accessibility `[SCREENSHOT 2026-06-04]`

**Use cases** `[DOC]`: a user must be able to — navigate to & **activate** the field with AT · **input** text · **receive & understand** supporting + error messages · navigate to & **select interactive icons**.

### §X1. Keyboard navigation
| Keys | Action |
|---|---|
| **Tab** | Focus lands on the (non-disabled) text field |

`[CODE]` Only **Tab** traversal is spec'd at the field level (in/out, skips disabled); **all in-field editing keys** (arrows, Home/End, shift-select, Ctrl-C/V, undo) are the platform's. → **Swing `JTextComponent` provides every one of these for free** — the decorator adds nothing here.

### §X2. Labeling rules `[CODE]` (→ Swing AccessibleContext mapping)
| M3 a11y rule | Swing mapping |
|---|---|
| Field **role = "Textbox"**; accessibility label **= the text-field label** | `AccessibleRole.TEXT` (free from `JTextField`); set `accessibleName = label` |
| **Supporting text** is also used as the field's accessibility label | `accessibleDescription = supportingText` |
| **Required**: label ends with `*`; the **a11y label must include the asterisk** (and any supporting text) | append `*` to `accessibleName`; include supporting text |
| **Error**: role becomes **"alert"**, error message announced; if both present, **supporting text first, then error text** | ⚠️ Swing has **no `ALERT` role** — approximate: set `accessibleDescription = supporting + ", " + error` and fire an accessible alert (`firePropertyChange(ACCESSIBLE_*)` / state change). **Flag for S-story.** |
| **Interactive trailing icon** (e.g. show/hide password) → role **"Button"**, label = its function ("Show password"/"Hide password") | put an **`ElwhaIconButton`** in the trailing slot → `AccessibleRole.PUSH_BUTTON` free; set its name to the function |
| **Non-actionable** icon (error icon) → label **"Error"** | decorative glyph with `accessibleName = "Error"`, not focusable |
| **Prefix / suffix** → role **[none]**, label = a **readable** form ("Euro" for `€`, "At gmail dot com" for `@gmail.com`) | announce as text via the field's description; give a spoken-friendly label, not the glyph |
| **Character counter** → label **"character count, N of M characters entered"**, role [none] | compose the counter's accessible text from the limit |

### §X3. Contrast `[CODE/DOC]`
- `[DOC]` Outlined fields should have **≥ 3:1 contrast between the container outline and the background** (Do/Don't). Elwha's `OUTLINE` role vs surface already clears 3:1 in the baseline palette — **document + verify per-palette**; no token change.
- `[CODE]` Error state carries a **non-color cue** (the error icon, §GD) in addition to the error color — the 3:1 + non-color doctrine (same as the menu's checkmark).

**Headline:** the a11y surface is **overwhelmingly satisfied by the wrapped `JTextComponent`** (role, name, caret, selection, editing keys, Tab). Elwha's a11y work is *wiring*: `name = label (+ * + supporting)`, `description = supporting text`, the **error→alert announcement** (the one Swing gap to engineer), and **`ElwhaIconButton` trailing icons** for free button semantics. **This is the single biggest argument for the decorator (Path A).**

---

## §Tokens — Elwha mapping (zero-new-tokens goal) `[updated 2026-06-04 vs all Filled token sheets + Filled/Outlined spec pages + Measurements]`

| M3 need | M3 attr | Elwha token | Notes |
|---|---|---|---|
| Filled container | `colorSurfaceContainerHighest` | `ColorRole.SURFACE_CONTAINER_HIGHEST` | ✅ exists |
| Input text | `colorOnSurface` | `ColorRole.ON_SURFACE` | ✅ |
| Label (resting) / icons / supporting | `colorOnSurfaceVariant` | `ColorRole.ON_SURFACE_VARIANT` | ✅ |
| Label (focused) / active indicator (focus) / caret | `colorPrimary` | `ColorRole.PRIMARY` | ✅ caret confirmed §T1 |
| **Filled** active indicator (resting) | ~~`colorOutline`~~ → `onSurfaceVariant` | `ColorRole.ON_SURFACE_VARIANT` | ⚠️ corrected §T1 |
| **Outlined** outline (resting) | `colorOutline` | `ColorRole.OUTLINE` | ✅ confirmed §O2 (focused → PRIMARY) |
| Error: indicator / label / supporting / **trailing** icon / caret | `colorError` | `ColorRole.ERROR` | ✅ §T5 — input stays on-surface, leading icon stays neutral; error beats focus |
| Corner radius | `cornerExtraSmall` | `ShapeScale.XS` (4dp) | ✅ filled = top-only |
| Paddings (8 top/bottom · 16 or 12 L/R · 16 icon-text · 4 supporting-top) | redlines | `SpaceScale` 4/8/12/16 | ✅ §M — zero new spacing tokens |
| Container height / target | `56dp` | layout constant | ✅ §M |
| Hover/focus/disabled overlays | state layers | `StateLayer.HOVER` / `FOCUS` / disabled | ✅ |
| Label type (resting+floating) | BodyLarge→BodySmall | `TypeRole.BODY_LARGE` → `BODY_SMALL` | ✅ float shrinks role |
| Input type | Body Large | `TypeRole.BODY_LARGE` | ✅ |
| Supporting / counter type | BodySmall | `TypeRole.BODY_SMALL` | ✅ |
| Prefix / suffix type | TitleMedium | `TypeRole.TITLE_MEDIUM` | ✅ |

**Result so far: zero new theme tokens.** (Matches the menu epic.) Confirm exact dp/opacity values when the **Tokens** screenshot batch arrives.

---

## §P. Terminology → API lock (operator rule: API mirrors M3's exact nouns)

| M3 noun | ElwhaTextField API | Status |
|---|---|---|
| Filled / Outlined | `ElwhaTextField.Variant{FILLED, OUTLINED}` + `filled()`/`outlined()` factories | proposed |
| Label | `setLabel(String)` (the floating label) | proposed |
| Placeholder | `setPlaceholder(String)` | proposed |
| Supporting text | `setSupportingText(String)` | proposed |
| Error / Error text | `setError(boolean)` + `setErrorText(String)` | proposed |
| Leading / trailing icon | `setLeadingIcon(...)` / `setTrailingIcon(...)` (via `MaterialIcons`) | proposed |
| Prefix / suffix text | `setPrefixText` / `setSuffixText` | proposed |
| Character counter | driven by `setMaxLength(int)` | proposed |
| Active indicator / outline | internal chrome (not consumer API) | — |

*(Fill in / correct as screenshots confirm the exact M3 nouns.)*

---

## §Q. Architecture-insight log
- **2026-06-04 (from the stub + codebase):** the wrap-vs-extend call is the analog of the menu's host decision — it shapes the whole API. `FlatLafKeyMapping` already theming text components + FlatLaf's native `placeholderText`/`leadingComponent`/`arc`/`outline` support strongly favors the **decorator**. S1 spike to lock.

---

## §Open questions (for Phase C)

> **RESOLVED 2026-06-04 (operator: "go with your recs").** Decisions: **(1)** decorator Path A, locked by S1 spike. **(2)** Phase 1 = single-line; multi-line + text-area = a **later V1 phase** (same class, swap editor), not a separate epic. **(3)** visual-only error contract (`setError`/`setErrorText`), validation engine consumer-owned. **(4)** Phase 1 features = leading/trailing icons · supporting text · placeholder · error + auto error-icon · prefix/suffix · required-asterisk; **later V1 phase** = character counter + supporting-text visibility mode. **(5)** file a **select/exposed-dropdown stub epic** (composes #298); **documented deferrals** for search + formatted/numeric. **(6)** focus stroke **3dp** (Expressive), eye-confirm in build; resting 1dp. Full rationale → `elwha-textfield-design.md`.

1. **Wrap-vs-extend** — decorator over `JTextField` (rec) vs custom. *(S1 spike.)* Does `JFormattedTextField` enter V1 or defer with formatted/numeric fields?
2. **Multiline / textarea** — V1 phase, V2 epic, or deferred stub? (Stub lists it as a possible follow-up.)
3. **Validation surface** — visual-only `setError`/`setErrorText` in V1 (rec), with the validation engine consumer-owned? Or a typed validator hook?
4. **`required` asterisk** — render it (M3 `required`/`no-asterisk`) in V1 or defer?
5. **Character counter** — V1 (driven by `setMaxLength`) or later phase?
6. **Prefix/suffix** — V1 or later phase?
7. ~~**Density**~~ — **RESOLVED (§GD3):** deferred / out of V1. M3 itself says don't apply density by default (keeps 48×48 targets); default = comfortable 56dp. Opt-in only, if ever.
8. **Follow-up stubs to file** — select/dropdown (menu-backed), search field, formatted/numeric, textarea — which become V2/sibling stub epics vs documented deferrals ([[feedback_no_invented_scope_cuts]])?
9. **Supporting-text visibility mode** (§GD) — support persistent **and** focus-only supporting text (`ALWAYS | ON_FOCUS`) in V1, or persistent-only? Either way **reserve the supporting row height** so error/supporting swaps don't shift layout.
10. **Auto error-icon** (§GD) — in the error state, auto-default the trailing slot to an error `!` glyph (the non-color a11y cue) when the consumer hasn't set one, or require the consumer to supply it?
11. **Label optional** (§GD) — confirmed *supported* (adjacent-label pattern): `setLabel` optional, field renders label-less. Not really a question — just don't force a label. Flagging so the API doesn't assume one.

**Refines Q2:** the input-text guideline splits into **three** behaviors — single-line (`JTextField`), multi-line auto-grow (`JTextArea`), fixed text-area (`JTextArea`+scroll). The V1/scope cut must address all three, not just "multiline yes/no."

---

## §F. Screenshot log
| # | Page | Captured | → section |
|---|---|---|---|
| 1 | Overview (variants + guidelines) | 2026-06-04 | §A |
| 2–7 | Tokens — **Enabled** state, Filled (container, label, icons, active indicator, supporting, input, caret) | 2026-06-04 | §T1 |
| 8–10 | Tokens — **Disabled** state, Filled | 2026-06-04 | §T2 |
| 11–12 | Tokens — **Hovered** state, Filled | 2026-06-04 | §T3 |
| 13–14 | Tokens — **Focused** state, Filled | 2026-06-04 | §T4 |
| 15–16 | Tokens — **Error** family (Error / Error·Focus / Error·Hover), Filled | 2026-06-04 | §T5 |
| 17 | Filled **Anatomy** (10-part callout) | 2026-06-04 | §A2 |
| 18–19 | Filled **Color** (roles map, light+dark) | 2026-06-04 | §Color |
| 20–21 | Filled **States** gallery (light+dark) | 2026-06-04 | §S |
| 22–23 | Filled **Error states** gallery (light+dark) | 2026-06-04 | §S |
| 24–26 | Filled **Measurements** redlines + spec table | 2026-06-04 | §M |
| 27–28 | Filled **Configurations** matrix | 2026-06-04 | §Cfg |
| 29–40 | **Outlined** set — anatomy, color+roles, states (l/d), error states (l/d), measurements+table, configurations | 2026-06-04 | §O |
| 41–43 | **Guidelines / Usage** (when-to-use, variant choice, forms) | 2026-06-04 | §G |
| 44–45 | **Guidelines** — choosing a variant + don't-intermix rule | 2026-06-04 | §G2 |
| 46–47 | Anatomy (Filled + Outlined) — re-confirm | 2026-06-04 | §A2/§O1 |
| 48–58 | **Element guidelines** — Containers · Label · Adjacent label · Required indicator · Input text (single/multi/text-area) · Prefix · Suffix · Supporting+counter · Error text · Error icon | 2026-06-04 | §GD |
| 59–61 | **Icons & images · icon-signifier types · Read-only fields** | 2026-06-04 | §GD2 |
| 62–65 | **Adaptive design · Density** (don't-apply-density-by-default) | 2026-06-04 | §GD3 |
| 66–73 | **Accessibility** — use cases · interaction/style · 3:1 contrast · keyboard (Tab) · labeling · prefix/suffix/error/counter/required a11y labels | 2026-06-04 | §X |

**Captured:** **Filled** — full token set (×5 states) + anatomy + color + states + error + measurements + configurations. **Outlined** — anatomy + color-roles + states + error + measurements + configurations (per-state token *hexes* taken as parity with Filled; only color-roles + redlines captured for Outlined). **Both variants are now substantively complete for design.** **Optional remaining** (capture only if you want them in the doc): a dedicated **Accessibility** page, **label-motion/behavior** page, **Adaptive/responsive** page, **Input types** page. Read the images, not just captions.
