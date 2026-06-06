# ElwhaTextField — Design Decisions

**Status:** **Phase 0 — design draft for review.** Decisions below are proposed; the load-bearing **decorator-boundary** call (§2) is locked by the first implementation story's spike. No stories filed until this doc is approved.

**Drafted:** 2026-06-04. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-textfield-research.md`](elwha-textfield-research.md) — the full M3 spec capture this doc decides against. **Read it for any anatomy/token/measurement/a11y detail; this doc references rather than duplicates it.**
- [`elwha-dialog-design.md`](elwha-dialog-design.md) — the design-doc shape + the S1-spike-locks-the-architecture precedent this epic mirrors.
- [`FlatLafKeyMapping`](../../src/main/java/com/owspfm/elwha/theme/FlatLafKeyMapping.java) — already themes `TextField`/`TextComponent` onto Elwha tokens (the decorator builds on it).
- [M3 Text fields](https://m3.material.io/components/text-fields) + Material Web `text-field.md` + MDC-Android `TextField.md`.

**Epic:** [#286](https://github.com/OWS-PFMS/elwha/issues/286) (V1). **Follow-on (to file):** ElwhaTextField **select / exposed-dropdown** stub (composes `ElwhaMenu` [#298](https://github.com/OWS-PFMS/elwha/issues/298)).

**Milestone:** v0.4.0 (the active dev milestone).

---

## TL;DR — the locked decisions

1. **What it is:** `ElwhaTextField` — the M3 **text field**: a labeled, token-themed input with two chrome variants, floating-label motion, supporting/error text, icon + prefix/suffix slots. The first-class form input Elwha lacks (31 internal raw-`JTextField` sites + the OWS consumer).
2. **Two variants `FILLED` / `OUTLINED`** — both Expressive-current, a **chrome-only axis with one identical API** ([[project_elwha_m3_expressive]]; research §G). Per-variant factories `filled()` / `outlined()` mirror `ElwhaCard`/`ElwhaChip`.
3. **Architecture (load-bearing, §2):** a **thin decorator** — `ElwhaTextField extends JComponent` **owning chrome paint + floating label + slots + token mapping**, **embedding a real `JTextField`** as the editor. *Not* a `JTextField` subclass, *not* a from-scratch editor. Justified by the a11y/editing surface coming free from `JTextComponent` (research §X). **Final decorator boundary locked by the §11 S1 spike.**
4. **Zero new theme tokens (LOCKED).** Every color/type/shape/space need maps onto existing `ColorRole`/`TypeRole`/`ShapeScale`/`SpaceScale`/`StateLayer` — table in research §Tokens.
5. **No overlay host.** Unlike `ElwhaMenu`/`ElwhaDialog`, the text field is an **embeddable inline component** — no `JLayeredPane`, no z-band, no light-dismiss. Simpler primitive.
6. **States (LOCKED):** enabled · hover · focus · error · disabled · **read-only**. Filled active-indicator `on-surface-variant`→hover `on-surface`→focus `primary` (1dp→**3dp** focus, Expressive); Outlined outline `OUTLINE`→focus `PRIMARY`. Disabled `on-surface` @ 0.04/0.38 (`StateLayer` disabled keys). **Error beats focus.** Read-only = `setEditable(false)` (not dimmed). (research §T1–§T5, §O2, §GD2)
7. **Error contract (LOCKED):** **visual-only** — `setError(boolean)` + `setErrorText(String)`; error text **replaces** the supporting line (reserve the row height to avoid layout shift), auto-shows the non-color **error icon**, and fires the a11y **"alert"** announcement. **No validation engine in the lib** (consumer-owned).
8. **Floating label (LOCKED):** label optional (adjacent-label pattern); empty = centered, focused/populated = floated to top (`BODY_LARGE`→`BODY_SMALL`); short scale/move, honoring reduced-motion.
9. **A11y (LOCKED):** role `Textbox` (`AccessibleRole.TEXT`), name = label (+`*` if required, + supporting text), description = supporting text, interactive trailing icon = `ElwhaIconButton` (Button role), error → "alert". The **error→alert announcement is the one Swing gap** to engineer (§X).
10. **V/Phase split:** **V1 (#286)** = the field incl. multi-line + counter across internal phases; **follow-on epic** = select/exposed-dropdown. See §11.
11. **Out of scope:** select/dropdown (→ stub epic, composes #298), search field + formatted/numeric (documented deferrals), **density** (M3 says don't-by-default — §GD3), web validation engine.

---

## §1. Scope — what V1 ships

✅ `FILLED` + `OUTLINED` chrome · floating label (+ placeholder, optional label) · **single-line** input · leading/trailing icon slots (`MaterialIcons` / `ElwhaIconButton`) · prefix/suffix · supporting text · **error** state (`setError`/`setErrorText` + auto error-icon) · required-asterisk · read-only + disabled · full token theming + dark mode · **multi-line + text-area** (later phase) · **character counter** + **supporting-text visibility mode** (later phase) · Showcase leaf.

❌ Select / exposed-dropdown → **stub epic** (composes #298) · search field, formatted/numeric → **documented deferrals** · density → out (§GD3) · regex/`ValidityState` validation engine → consumer-owned.

## §2. Architecture — the load-bearing decision [RECOMMENDED; lock via S1 spike]

**`ElwhaTextField extends JComponent` as a decorator over an embedded `javax.swing.JTextField`.** The wrapped editor owns caret, selection, IME, copy/paste, undo, editing keys, Tab traversal, and the `AccessibleJTextComponent` surface (role/name/state) — **all free** (research §X). Elwha owns:
- **Chrome paint** — filled fill (`SURFACE_CONTAINER_HIGHEST`, top-XS) + bottom active indicator, OR outlined stroke (`OUTLINE`, all-XS) with the **label-notch**; stroke color/width by state.
- **Floating label** + placeholder + the float motion.
- **Slots** — leading/trailing icon, prefix/suffix, supporting text, character counter — laid out around the editor per the §M redlines.
- **Token mapping** + the **error→"alert"** a11y wiring (the one gap).

**Why not extend `JTextField`?** A subclass fights the UI delegate for painting and can't host the label/slots cleanly. **Why not from-scratch?** Re-deriving caret/IME/a11y is the trap the research explicitly rejects.

**Filled fill:** the spike decides whether to compose an internal `ElwhaSurface` for the filled container or paint the fill directly (it's a simple rounded rect) — lean **paint-direct** (no shadow, no clip needed). No `ShadowPainter` (text fields are not elevated).

**S1 spike (first story):** prototype the decorator — embed a `JTextField`, paint both variant chromes, prove (a) token theming + dark mode, (b) hover/focus/disabled/read-only drive the chrome, (c) the **error→alert** AccessibleContext announcement works under a screen reader, (d) focus/blur drives the label float. Mirrors the dialog epic's `DialogModalityDemo` S1 lock. Documented fallback: if `JComponent`+embedded editor fights layout, fall back to a `JPanel` with a `BorderLayout`-hosted editor (heavier, documented).

## §3. Anatomy / slots

Per research §A2 (Filled, 10-part) / §O1 (Outlined, 9-part). The decorator exposes typed slots: **leading icon · label (empty/floated) · prefix · input (editor) · suffix · trailing icon · supporting text · character counter**. Icons via `MaterialIcons` (24dp glyph); an interactive trailing icon (clear / show-password) is an `ElwhaIconButton` for free Button a11y. Leading/trailing positions respect `ComponentOrientation` (RTL).

## §4. Tokens & color

**Zero new tokens** (research §Tokens). Container `SURFACE_CONTAINER_HIGHEST` (filled); input `ON_SURFACE`; label/icons/supporting/prefix/suffix/placeholder `ON_SURFACE_VARIANT`; label-focused/caret/focus-indicator `PRIMARY`; outlined resting outline `OUTLINE`; error `ERROR`; corners `ShapeScale.XS` (filled top-only); paddings `SpaceScale` 4/8/12/16; disabled + hover via `StateLayer`. Type: label `BODY_LARGE`→`BODY_SMALL`, input `BODY_LARGE`, supporting/counter `BODY_SMALL`.

**State color rules (LOCKED, research §T1–§T5/§O2):** hover deepens the filled indicator to `on-surface` (+`StateLayer.HOVER` fill on filled only; outlined has no container fill); focus → `primary` indicator/label, **3dp** (Expressive; eye-confirm) / outlined 2–3dp; **error beats focus** — indicator/label/supporting/**trailing**-icon/caret → `error`, input stays `on-surface`, leading icon stays neutral; error-hover deepens via the hover layer over error. Disabled = `on-surface` @ 0.04 container / 0.38 content.

## §5. Layout / measurements

Research §M/§O3: **56dp** height/target, `8dp` top-bottom, `16dp`/`12dp` L-R (no-icon/icon), `16dp` icon↔text, `4dp` supporting-top, outlined **`4dp` label-notch**, **`488dp` max width**. Optional `setMaxContentWidth(int)` (mirrors dialog `contentMaxWidth` #291) — document 488dp default guidance; not a Phase-1 must.

## §6. States & motion

Six states (§6 TL;DR). **Label-float motion**: empty-centered ↔ top-floated on focus/populate, a short scale+move; honor reduced-motion (instant). No ripple (text fields have none). Reserve the supporting-text row height so error↔supporting swaps don't shift layout.

## §7. Error & validation

**Visual-only.** `setError(boolean)` flips the error chrome; `setErrorText(String)` **replaces** the supporting line and auto-shows the **error icon** (non-color cue) + fires the a11y alert. The lib ships **no** regex/`ValidityState` engine — consumers run their own validation and call `setError*`. (research §GD/§X)

## §8. Accessibility

Per research §X: role `TEXT`; `accessibleName = label (+ "*" + supporting)`; `accessibleDescription = supportingText`; trailing interactive icon = `ElwhaIconButton`; non-actionable error icon named "Error"; prefix/suffix announced with readable labels; counter "character count, N of M". **Error→"alert"** has no native Swing role → approximate via description + an accessible alert fire (S5 + S1 spike proves the mechanism). 3:1 outline contrast (palette-verified).

## §9. Showcase pattern

A text field **is** an embeddable surface (no overlay), so it fits the standard `ComponentWorkbench` + `Gallery` directly: a live field on the stage with variant / icon / prefix-suffix / error / supporting / placeholder / read-only controls, and a Gallery matrix of variant × state. **Dogfood Elwha components** in the controls ([[feedback_dogfood_elwha_components]]).

## §10. Out of scope (filed, not silently cut)

- **Select / exposed-dropdown** — a text field with a nested selection menu (research §GD2). **File a stub epic** (composes `ElwhaMenu` #298). Not V1.
- **Search field** (`type=search`) — a recipe (field + leading search + trailing clear), **documented deferral**, not a primitive.
- **Formatted / numeric** (`JFormattedTextField`, spinner) — **documented deferral**; revisit on consumer need.
- **Density** — M3 says don't-apply-by-default (research §GD3); out of V1.
- **Web validation engine** (`pattern`/`ValidityState`) — consumer-owned.

## §11. Phasing → stories

- **S1 — Decorator spike + chrome skeleton (§2).** `ElwhaTextField` wrapping a `JTextField`; both variant chromes; token theming + dark mode; hover/focus/disabled/read-only drive chrome; **error→alert** a11y proven; label-float driven by focus/blur. *Locks §2.*
- **S2 — Floating label + placeholder + motion (§6/§8).** Empty-center ↔ floated; optional label (adjacent-label support); reduced-motion.
- **S3 — Variant chrome + all states + color rules (§4).** Filled top-XS + active indicator (1/3dp); Outlined full-XS + label-notch; the full state color table incl. error-beats-focus.
- **S4 — Slots + measurements (§3/§5).** Leading/trailing icon (`MaterialIcons`/`ElwhaIconButton`), prefix/suffix, supporting text, required-asterisk; the §M redlines; RTL.
- **S5 — Error API + a11y alert (§7/§8).** `setError`/`setErrorText` (replaces supporting, reserved row), auto error-icon, the alert announcement, full AccessibleContext wiring.
- **S6 — Multi-line + text-area (§1).** Swap embedded editor to `JTextArea` (auto-grow + fixed-scroll). *[Phase 2]*
- **S7 — Character counter + supporting-text visibility mode (§GD).** `setMaxLength` counter; `ALWAYS | ON_FOCUS`. *[Phase 3]*
- **S8 — Showcase leaf (§9).** Workbench + Gallery.
- **S9 — Docs:** `textfield/README.md`, CHANGELOG, Javadoc (`@author`/`@version`/`@since`), dogfood pass.

**Phases:**
- **Phase 1 = S1–S5 + S8 + S9** → ships the complete **single-line** field (both variants, icons/prefix/suffix/supporting/error/required, Showcase, docs) — **unblocks the 31 internal sites + OWS forms.**
- **Phase 2 = S6** (multi-line + text-area).
- **Phase 3 = S7** (character counter + supporting-text visibility).

## §12. Open for the S1 spike / Phase 0 sign-off
- The exact **decorator boundary** (`JComponent`+embedded editor vs `JPanel`+`BorderLayout`) and the **error→alert** mechanism — S1 proves both.
- **Focus-stroke 3dp** (Expressive) — eye-confirm against the M3 render in S3.
- Whether the filled fill composes `ElwhaSurface` or paints direct (lean direct).

### S1 spike outcome (2026-06-05, #333) — LOCKED
- **Decorator boundary = `ElwhaTextField extends JComponent` with a `null` layout hosting an embedded `JTextField`** (the editor is the only child; the label and chrome paint around it, future interactive slots attach as children). The `JComponent`+embedded-editor path laid out and painted across every variant×state without fighting layout — the documented `JPanel`+`BorderLayout` fallback was **not** needed. Focus stays on the editor: focus/blur is tracked via a `FocusListener` on the editor, hover via `MouseListener`s on both the field and the editor, and a mouse-press anywhere on the field calls `editor.requestFocusInWindow()`.
- **error→"alert" mechanism = fire an `AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY` change on the embedded editor** (the focusable node AT lands on). Proven by a headless `AccessibleContext` listener in `TextFieldS1SpikeSmoke`; S5 composes the full "supporting text, then error" message.
- **Filled fill painted direct** (a top-rounded `Path2D`, no `ElwhaSurface`, no shadow) — confirmed adequate; **focus stroke 3dp / resting 1dp** carried forward to S3 for the eye-confirm.
- The chrome occupies the top `CONTAINER_HEIGHT` (56px) band; the supporting-text row height is reserved below it from S1 so error↔supporting swaps never shift layout.

### S6 build outcome (Phase 2, #353 — 2026-06-06)

The multi-line build stayed **inside the locked S1 decorator boundary** — no new host, no `JPanel` fallback. As built:

- **Typed `InputMode` enum** (`SINGLE_LINE` / `MULTI_LINE` / `TEXT_AREA`) is the API, not a `setMultiline(boolean)` — it names the research §GD trichotomy directly and matches the `Variant` doctrine. `setRows(int)` sizes `TEXT_AREA`; `isMultiline()` is the convenience predicate.
- **One editor reference, swapped in place.** `SINGLE_LINE` → `JTextField`; the multi-line modes → a wrapping `JTextArea`, hosted bare for `MULTI_LINE` (auto-grow) and inside a borderless transparent `JScrollPane` for `TEXT_AREA` (fixed rows, internal scroll). The swap preserves text + enabled/editable and re-syncs the accessible name. `getEditor()` widened `JTextField` → `JTextComponent` (CHANGELOG break).
- **Geometry generalized:** the fixed `CONTAINER_HEIGHT` became a computed `containerHeight()`. Multi-line reserves a `multiTopInset()` (filled = a floated-label row inside the fill; outlined = top padding, label on the stroke above), top-anchors the editor + icons + trailing button to the first line, and grows the box downward — auto-grow follows the wrapped content height (measured at the laid-out width, `revalidate()` on text change), text-area opens at `rows × lineHeight`. The outlined label-notch and the supporting/error row track the taller box. Zero new tokens.
- The `JTextArea` restores default Tab traversal (Tab moves focus, Enter newlines) so it reads as a form field, not a code editor.
