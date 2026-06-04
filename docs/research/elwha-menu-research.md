# ElwhaMenu — M3 Spec Capture (research scratch)

**Status:** RAW CAPTURE — accumulating M3 source material for epic [#298](https://github.com/OWS-PFMS/elwha/issues/298) (ElwhaMenu stub, `v0.4.0`). Not a design doc yet; this is the companion research dump (mirrors [`elwha-navigation-rail-research.md`](elwha-navigation-rail-research.md)). Promote a real `elwha-menu-design.md` when the epic is scheduled and Phase 0 runs.

**Captured:** 2026-06-04. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Consumers / related:**
- [#238](https://github.com/OWS-PFMS/elwha/issues/238) — Nav Rail trailing-actions overflow menu (the concrete consumer; blocked-by #298).
- [#185](https://github.com/OWS-PFMS/elwha/issues/185) — M3 FAB Menu (distinct primitive; open design Q whether ElwhaMenu hosts it).
- [#221](https://github.com/OWS-PFMS/elwha/issues/221) — overlay z-band convention (PALETTE/MODAL layer) the popup host must coordinate with.

---

## §0. Scope decision — Expressive vertical menu only (operator, 2026-06-04)

**ElwhaMenu (#298) implements the M3 Expressive *vertical* menu ONLY. The *baseline* menu is NOT implemented.**

- Rationale: matches Elwha's Expressive-first posture ([[project_elwha_m3_expressive]] — skip legacy baseline forms). The baseline menu is *still allowed* under M3 Expressive (square corners, older selection/motion), but it's the pre-Expressive shape and adds a second visual language for no consumer need (#238 wants a normal vertical popover).
- Everything captured here describes the **vertical** menu. The §G "Baseline variant" notes are kept for reference / contrast only — **not a build target.**
- Baseline menu handling (document-only exclusion vs. discoverability stub epic): **PENDING operator pick** — see end of doc.

---

**Source URLs:**
- M3 spec (overview/specs/guidelines): https://m3.material.io/components/menus/overview *(JS-only — screen-cap source)*
- Material Web `menu.md`: https://github.com/material-components/material-web/blob/main/docs/components/menu.md
- MDC-Android `Menu.md`: https://github.com/material-components/material-components-android/blob/master/docs/components/Menu.md

---

## §TL;DR — synthesis (read this first)

**What ElwhaMenu is:** the M3 Expressive **vertical menu** — a temporary, light-dismissed surface that tops all content, anchored to a trigger, listing `ElwhaMenuItem`s. The general-purpose popover Elwha lacks; unblocks the Nav-Rail overflow (#238). Baseline (square) menu is **out** (§0).

**Settled (well-evidenced by the capture):**
- **Tokens, zero new ones.** Item **44dp** visual / **48dp** touch target, **16/8/12dp** insets, **20dp** icons, **2dp** gap/group-padding, Level-3 shadow. Label `LABEL_LARGE`, supporting `BODY_SMALL`, trailing/shortcut `LABEL_LARGE`. Focus ring `SECONDARY` 3dp inset (−3dp). Container `SURFACE_CONTAINER_LOW`. (§I, §K)
- **Two config axes:** `ColorStyle{STANDARD, VIBRANT}` (surface vs tertiary) × `Layout{STANDARD, GROUPED}`; Grouped separators = `Separator{GAP, DIVIDER}` (gap = expressive default, divider forced when scrollable). (§H, §K, §N)
- **Selection** = `TERTIARY_CONTAINER` fill **+ ✓ checkmark** (3:1 + non-color cue; Vibrant uses bold `TERTIARY`). (§K, §X)
- **`ElwhaMenuItem` = dedicated `JComponent` primitive** (container-with-swappable-slot), mirroring `ElwhaNavRailDestination` — *not* `JMenuItem`. Slots: leading icon · label · supporting text · trailing text/shortcut · trailing icon · badge · swappable content slot. (§J, §Q, §Q′, §P)
- **Active/submenu state = corner shape-morph** (focused rounds more / unfocused squares off) via shared `ShapeMorphPainter` (#176). (§L, §V)
- **A11y:** `POPUP_MENU`/`MENU_ITEM` roles, initial focus = first item, focus-trap + restore-to-trigger, disabled items stay focusable, separators non-focusable, icon decorative, full key map. (§X)
- **Desktop instant-open is on-spec** → low motion bar for v1. (§U)

**Versioning & phasing (LOCKED 2026-06-04 — distinguish V from Phase):**

- **V1 = epic #298** — the first shippable ElwhaMenu. **Includes** color styles (Standard **+ Vibrant**), layout (Standard + Grouped), and `SelectionMode` — these are V1 scope, just landed across *internal phases*, not all in Phase 1.
  - **V1 Phase 1 (unblocks #238):** flat `ElwhaMenu` + dedicated `ElwhaMenuItem` (+ slot setter), `open(anchor)` + flip/shift host, **Standard** color, Standard + Grouped layout, light-dismiss, full keyboard/a11y, instant/short entrance, `SelectionMode.NONE`.
  - **V1 Phase 2:** **Vibrant** color style.
  - **V1 Phase 3:** `SelectionMode.SINGLE/MULTI` (coordinate w/ #252), slot content.
- **V2 = epic [#322](https://github.com/OWS-PFMS/elwha/issues/322)** (filed 2026-06-04) — **submenus + nested item types**: `ElwhaSubMenuItem`, side-placement, active corner shape-morph (`ShapeMorphPainter` #176), hover timing, Left/Right nav (§S, §V, §L). Depends on #298.
- **Baseline (square) menu = stub epic [#323](https://github.com/OWS-PFMS/elwha/issues/323)** (filed 2026-06-04) — deliberate-exclusion record; promote only if a consumer needs square-corner menus (§0).
- **Deferred / separate primitives (not filed):** exposed-dropdown / filtering assembly (§U), context-menu right-click helper (§N), density levels (§W), horizontal "menu" = Toolbar (§I ghost).

**Open decisions still needing the operator:** see **§E** — load-bearing: Host (Q1), z-band (Q2), Item-family (Q7). Resolved by operator 2026-06-04: submenu → V2 epic (Q3); Vibrant → V1 later phase (Q5); SelectionMode → V1 later phase (Q8); filtering/context-menu → out of core (Q6, Q9); baseline → stub epic #323. **§M fully verified** (the "32" = exposed-dropdown field-row height, 32dp, out of core).

### Reading order (sections are in capture order, not logical order)
§0 scope · §TL;DR · **Sources** §A–§C · **mapping** §D · **questions** §E · **Variants** §G · **Configurations** §H · **Anatomy** §J · **Menu items** §O · **Slots** §Q · **Architecture** §Q′ · **Color** §K · **States** §L · **Focus/morph** §V · **Behavior** §U · **Placement** §R · **Submenus** §S · **Adaptive** §T · **Density** §W · **Guidelines** §N · **A11y** §X · **Measurements** §M · **Tokens** §I · **Terminology** §P · **screenshot log** §F.

---

## §A. M3 Expressive update — Nov 2025 (from spec Overview, screen-cap 2026-06-04)

> **Vertical menus** were introduced with new shapes, color styles, selection states, and refined submenu motion. Gaps can be used for a more flexible layout on Android.

- **Variants:**
  - **Vertical menus** — *added; recommended for new designs.*
  - **Baseline menu** — still available (the pre-Expressive flat list).
- **Color styles:** **Standard** and **Vibrant** ("Vibrant colors help selected menu items stand out").
- **Selection state (new in Expressive):** a selected item shows a **leading checkmark + filled highlight** (secondary-container fill in Standard; vibrant tonal fill in Vibrant). Screenshot shows an exposed-dropdown ("Label" field, Item 2 selected) with Item 1 / **✓ Item 2 (filled)** / Item 3.
- **Refined submenu motion** + optional **gaps** between items for flexible layout.

> **→ Elwha takes the vertical-menu variant as the default** (matches our M3-Expressive posture, [[project_elwha_m3_expressive]]). Capture both color styles; Standard is the likely default, Vibrant a flag.

### Overview usage rules (screen-cap 2026-06-04)
- Use a **menu** to show a *temporary* set of actions. To show actions on screen at all times, use a **toolbar** instead.
- Menus can open from many components: **icon buttons, split buttons, text fields** (and our case: a nav-rail overflow icon button).
- **Context menus** provide actions for a specific element (image, highlighted text); usually open with a **secondary (right) click**.

---

## §B. Spec from Material Web `menu.md` (authoritative API shape)

**Anatomy / parts:** container (`md-menu`) → internal list → items (`md-menu-item` extends list-item), dividers (`md-divider`, `role=separator`), submenus (`md-sub-menu` nesting a child menu).

**Item types:** (1) menu item, (2) submenu item (opens nested menu), (3) divider.

**Positioning / anchoring:**
- `anchorCorner` default `END_START`; `menuCorner` default `START_START` (menu hangs below-start of anchor).
- `xOffset` / `yOffset` pixel nudges.
- Auto-reposition (viewport flip) on by default; `noHorizontalFlip` / `noVerticalFlip` disable.
- Positioning contexts: absolute / fixed / document / popover. *(Swing analog: install on JLayeredPane / popup; flip+shift against frame bounds.)*

**Keyboard nav:** Up/Down (prev/next), Home/End (first/last), Enter/Space (select), Esc (close), type-ahead (`typeaheadDelay` 200ms). Focus **wraps** by default (`noNavigationWrap` disables).

**Focus:** `defaultFocus` = FIRST_ITEM (default) / LAST_ITEM / LIST_ROOT / NONE. Focus restored to pre-open element unless `skipRestoreFocus`.

**Dismissal (light dismiss):** outside-click, focusout, or Esc. `stayOpenOnOutsideClick` / `stayOpenOnFocusout` opt out. Item `keepOpen` prevents close-on-select.

**Item props:** `disabled`, `selected` (visual), `href`/`target` (link items — N/A for us), `keepOpen`, `typeaheadText`.

**Submenu:** `anchorCorner` default `START_END`, `hoverOpenDelay`/`hoverCloseDelay` 400ms.

**A11y:** container `role=menu`, items `role=menuitem`, dividers `role=separator tabindex=-1`. Adaptable to `listbox`/`option` for combobox use. ARIA: `aria-expanded`, `aria-activedescendant`, `aria-selected`.

**Lifecycle events:** opening / opened / closing / closed; `close-menu` carries `{initiator, reason, itemPath}`.

## §C. Spec from MDC-Android `Menu.md` (tokens / measurements)

- **Menu types:** dropdown (overflow / context / popup / list-popup-window) + exposed-dropdown (text field + list).
- **Container color:** `colorSurfaceContainer`. **Elevation:** **3dp**. **Typography:** `bodyLarge` default.
- Anatomy: list items w/ leading + trailing icons, optional trailing text, container + divider.
- *(Width/height/padding/corner in dp NOT in the GitHub doc — pull from m3.material.io/components/menus/specs via screen-cap.)*

---

## §D. Elwha token mapping (what already exists — no new tokens expected)

> **Authoritative measurements now live in §I** (verbatim M3 token capture). Table below is the role mapping; §I supersedes any size guess here (notably: item label is **`LABEL_LARGE`**, not `BODY_LARGE`; item height **44dp**; icons **20dp**).

| M3 menu need | Elwha token (exists today) |
|---|---|
| Container color | `ColorRole.SURFACE_CONTAINER_LOW` (Standard) — *see §K; Expressive drops one tier below MDC's surfaceContainer* |
| Container shape | swatch-only in §I; Expressive vertical menu is **visibly rounded** (≈`ShapeScale.SM`/`MD`, *not* the baseline XS/4px) — confirm dp on Specs page |
| Elevation 3dp | shared `ShadowPainter` Level 3 (as `ElwhaDialog` uses) |
| Item label type | **`TypeRole.LABEL_LARGE`** (14/20/+0.1/500) — §I authoritative (*not* `BODY_LARGE`) |
| Item text color | `ColorRole.ON_SURFACE` (label), `ON_SURFACE_VARIANT` (leading/trailing icon + shortcut) |
| Selected item fill (Standard) | **`TERTIARY_CONTAINER`** / text `ON_TERTIARY_CONTAINER` (§K — *not* secondary) |
| Selected item fill (Vibrant) | **`TERTIARY`** / text `ON_TERTIARY` (whole surface tints `TERTIARY_CONTAINER`; §K) |
| State layer (hover/press) | `StateLayer` over `ON_SURFACE` |
| Item padding | `SpaceScale` (XS/SM/MD) — confirm exact dp from specs page |
| Scrim (light-dismiss invisible catcher) | reuse overlay-host input catcher; menus have **no visible scrim** (unlike dialog) |

---

## §E. Open design questions carried from #298 (for Phase 0)

> **Resolutions (operator, 2026-06-04):** Q3 submenu → **V2 epic #322** (not a V1 phase). Q5 Vibrant → **V1, later phase** (in scope, not Phase 1). Q6 context-menu → **out of core**. Q8 SelectionMode → **V1, later phase**. Q9 filtering → **out of core** (future exposed-dropdown). Baseline → **stub epic #323**. Still open for Phase 0: **Q1 host**, **Q2 z-band**, **Q4 FAB-menu relationship**, **Q7 item-family** (lean: single primitive + slot).

1. **Host:** reuse `AbstractElwhaDialog`'s overlay-host (light-dismiss + focus-restore already solved) vs a dedicated popup host. Note: menu has **no scrim** and needs **anchored** (not centered) positioning + viewport flip — the dialog host centers and scrims, so likely a *sibling* host sharing the focus-trap/dismiss plumbing, not a subclass.
2. **z-band:** which layer (coordinate w/ #221 — menus sit *above* dialogs? No — above content, at/below modal; a transient popup typically tops everything while open).
3. **Submenu support** in v1 or deferred.
4. **Relationship to FAB Menu #185** — does ElwhaMenu become the content host a FAB Menu composes, or stay independent.
5. **Vibrant color style** — ship both Standard + Vibrant day one, or Standard only + flag.
6. **Context-menu binding** (§N) — does v1 ship an `installContextMenu(component, menu)` right-click/press-and-hold helper, or is the anchored `open(anchor)` API enough (context-menu deferred)? #238 needs only the latter.
7. **Item primitive family shape** (§Q′) — single `ElwhaMenuItem` (`JComponent`, container-with-swappable-slot, mirrors `ElwhaNavRailDestination`) + slot setter, vs. a family of item primitives. Submenu + divider are the only genuine siblings. Connects to [[project_aggregate_split_question]].
8. **Selection mode** (§U) — `SelectionMode { NONE, SINGLE, MULTI }`? Multi-select keeps the menu open on select; single closes + auto-deselects prior. #238 needs `NONE` (action menu) only. Coordinates with epic #252 (shared selection surface).
9. **Filtering / exposed-dropdown** (§U) — out of core v1 (separate `ElwhaExposedDropdown` assembly), or in? Lean: out; ElwhaMenu stays the popup primitive.

---

## §G. Variants (from spec "Variants" page, screen-cap 2026-06-04 — **read from the images**)

**Two top-level variants, with an availability split:**

| Variant | M3 (baseline) | M3 Expressive |
|---|---|---|
| **Vertical menus** | — (not available) | Available — *recommended for new designs* |
| **Menu (baseline)** | Available | Available |

> Vertical menus only exist in Expressive. Baseline still exists in both. **Elwha ships the vertical menu** as its menu (M3-Expressive posture); baseline is the fallback shape we skip unless a consumer needs square corners.

### Vertical menus — TWO sub-layouts (this is the key visual finding)
The vertical menu has two ways to group items, both shown side-by-side in the spec:

1. **Vertical menu with gap** — each item is an **individually-rounded segment** with **visible gaps** between items (and a larger gap before a trailing group, e.g. before Item 4). Expressive, pill-per-item feel.
2. **Vertical menu with divider** — items are **contiguous** (no inter-item gap) with a **thin divider line** separating groups (between Item 3 and Item 4 in the sample). One rounded outer container.

Both share: rounded outer corners, leading-icon column, label, optional trailing.

**Anatomy observed in the vertical samples** (4 items):
- Item 1 — leading **eye** icon (visibility), label "Item 1".
- Item 2 — leading **copy** icon, label, **trailing keyboard shortcut** text "⌘C" (right-aligned, muted).
- Item 3 — leading **edit/pencil** icon, label — **SELECTED**: filled **vibrant pink/red rounded highlight** spanning the item (this is the *Vibrant* color style's selected fill; checkmark not shown here — fill alone carries selection).
- Item 4 — leading **cloud** icon, label, **trailing submenu caret** "›" → opens a submenu.

### Baseline variant — **NOT a build target (§0)**; captured for contrast only
- **Square corners** (vs vertical menu's round corners) — the defining visual difference.
- "In M3 Expressive, baseline menu is still available… but doesn't have the latest shapes, color styles, selection states, and motion."
- Sample anatomy: item w/ submenu caret ›; a **two-line item** ("Menu item" + **"Supporting text"** second line) that is **SELECTED via leading ✓ checkmark + light purple (secondary-container) tonal fill** — i.e. baseline selection = checkmark + tonal, *not* the vibrant pink fill; item w/ leading **cut/scissors** icon + submenu caret.
- → Confirms **supporting text** (optional second line under the label) is a menu-item feature, and a **leading-checkmark** selection treatment exists distinct from the fill treatment.

**Selection-state summary** *(roles corrected in §K — TERTIARY, not secondary):*
- Vertical / **Standard** → `TERTIARY_CONTAINER` fill + `ON_TERTIARY_CONTAINER` ✓ checkmark + content.
- Vertical / **Vibrant** → bold `TERTIARY` fill + `ON_TERTIARY` (within an already tertiary-tinted surface).
- **Baseline** → leading **✓ checkmark** + tonal fill (older baseline treatment).
- §H dark "Grouped" sample confirmed: vertical-Standard selected shows **✓ checkmark + fill together**.

---

## §H. Configurations (from spec "Configurations" page, screen-cap 2026-06-04)

**Two orthogonal configuration axes** (both Expressive-only on the non-default value):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| **Color** | Standard | Available | Available |
| **Color** | **Vibrant** | — | Available |
| **Layout** | Standard | Available | Available |
| **Layout** | **Grouped** | — | Available |

So ElwhaMenu has **two independent enums**:
- **Color style** = `STANDARD` (default) | `VIBRANT`.
- **Layout** = `STANDARD` (flat contiguous list) | `GROUPED` (items partitioned into groups).

**This reconciles §G's "gap vs divider":** those are the two *group-separator renderings* of the **Grouped** layout — a group boundary is drawn either as a **gap** (segmented pills) or a **divider** line. Standard layout has neither (one flat list).

### "Vertical menus layout" samples (dark surface)
1. **Standard** — opened from a **more_vert (⋮) icon button**. Flat list: Refresh / Settings / Help / More, each with a **trailing icon** (refresh, gear, help-?, ⋮). Contiguous, no groups, no selection.
2. **Grouped** — opened from an **exposed dropdown** ("Label" + edit icon + caret toggle). Group A: Item 1 (eye), Item 2 (copy, ⌘C), Item 3 (**SELECTED** = ✓ **checkmark + filled** maroon/dark-red highlight). Group boundary. Group B: Item 4 (person, submenu ›), Item 5 (gear).

> **Resolves the open §G question:** vertical menu, **Standard color**, selected item shows **✓ checkmark + fill** (both, not fill-only). Vibrant differs by using the more saturated fill; the checkmark convention appears consistent.

---

## §J. Anatomy (from spec "Anatomy → Vertical menus" page, screen-cap 2026-06-04)

**Page is titled "Vertical menus" — the only anatomy M3 documents.** 11 numbered parts:

| # | Part | Notes |
|---|---|---|
| 1 | **Menu item** | the interactive row |
| 2 | Leading icon *(optional)* | 20dp icon column |
| 3 | **Menu item text** | the label (required) |
| 4 | Trailing icon *(optional)* | e.g. submenu caret `›` |
| 5 | **Badge** *(optional)* | **NEW PART** — item can carry a badge (diagram shows a "New" pill on Item 3) |
| 6 | Trailing text *(optional)* | e.g. keyboard shortcut `⌘C` |
| 7 | **Container** | the menu surface |
| 8 | Supporting text *(optional)* | second line under the label |
| 9 | Label text *(optional)* | the **exposed-dropdown field label** above the surface ("Label text") |
| 10 | Gap *(optional)* | group separator — gap style |
| 11 | Divider *(optional)* | group separator — divider style |

**New findings vs prior sections:**
- **Badge (#5)** is a first-class optional menu-item slot — an item can show a badge (the `New` pill). For Elwha this maps onto the existing `ElwhaBadge` / `IconBearing` work (epic #209) — a menu item is potentially a badge **host**. Worth a slot in the item API.
- **Gap (#10) and Divider (#11) are both *optional anatomy parts*** — confirms §H: a Grouped layout's separator is one-or-the-other, and a Standard (flat) layout has neither.
- **Label text (#9)** belongs to the **exposed-dropdown** composition (field label sitting above the surface), not the plain popup menu — flags that the exposed-dropdown is a distinct assembly (text field + menu), likely its own future thing, not core ElwhaMenu.
- Trailing slot is **two independent optional parts** — Trailing icon (#4) *and* Trailing text (#6) — so an item could carry both (shortcut text + submenu caret), not an either/or.

**ElwhaMenuItem slot model (derived):** `[leading icon?] · label (+ supporting text?) · [badge?] · [trailing text?] · [trailing icon?]`, inside a Container, with optional inter-item Gap / Divider for grouping.

---

## §O. Menu items (from spec "Menu items" page, screen-cap 2026-06-04)

- **[DOC]** A menu item can include: **label text, leading icons, trailing icons, and keyboard commands.** *(— "keyboard commands" is M3's name for the trailing shortcut text, e.g. `⌘C`; anatomy part #6 "Trailing text" is that slot.)*
- **[CODE + DOC]** **"When a menu item can only be used under specific conditions, it should appear *disabled* rather than be removed."** → items support a **disabled** state (don't dynamically remove); sample: `Redo` disabled when nothing to redo. Maps to `ElwhaMenuItem.setEnabled(false)` + the §L Disabled treatment (dim, not gone).

---

## §Q. Flexibility & slots (from spec "Flexibility & slots" + "Slot accessibility", screen-cap 2026-06-04)

- **"Menus have custom slots that support more flexible item layouts."**
- **"Think of the menu item as a container with a swappable slot."** ← the core mental model.
- **"Slots can appear anywhere in a menu"** — any item position can be a slot.
- Slots work best with **simple content: images, progress indicators, color swatches.**

### Slot accessibility (Caution panel)
- **[CODE]** **Targets should be 48×48dp or larger.** ✅ **Resolves the §M "48" mystery** — the 48dp is the **minimum touch target**; the **44dp** in §I is the *visual* item height. Both true; Elwha item rows must hit a 48dp interactive target around a 44dp visual.
- **[DOC/CODE]** Keep the **same menu item padding** in a slot (§I insets still apply).
- **[DOC]** Elements must follow the **menu component's interaction patterns** (keyboard nav, light-dismiss).
- **[DOC]** **"Don't add buttons, switches, or other direct actions into the menu item. Nested elements should only perform one action."** Multiple actions break keyboard nav + screen readers. → a slot is **display content**, not a second interactive control.
- Sample: rich items — `Apparel` (+ `Sale` badge) / "Latest trends & styles", `Footwear` / "Step up your game", `Accessories` / "Finish your look today" — leading icon + label + supporting text + optional badge.

---

## §X. Accessibility (from spec "Accessibility" subsections, screen-cap 2026-06-04) — mostly [CODE]

### Use cases
With assistive tech, people must be able to: **navigate to / open / close a menu**, and **navigate between + select menu items.**

### Interaction & style — selection cues *(WCAG non-color redundancy)*
- **[CODE]** By default, selected items change **shape AND color.**
- **[CODE]** Default **color contrast ≥ 3:1** between selected and unselected items.
- **[CODE] ★ Include another non-color cue — a checkmark.** "Use multiple visual cues like color, shape, and icons." → Elwha selected item = `TERTIARY_CONTAINER` fill (§K) **+ leading ✓ checkmark** (don't rely on color alone). Confirms the §G/§H checkmark.

### Focus
- **[CODE]** **Initial focus → the first menu item** when the menu opens (lets keyboard/AT users navigate immediately). *(matches Material Web `defaultFocus=FIRST_ITEM`, §B.)*
- **[CODE]** Exit a menu by: **selecting an option**, **Escape**, **clicking outside**, or **system back**.
- **[DOC/CODE]** "Where focus is placed after closing depends on the app" — Elwha restores focus to the **trigger** (the dialog/FAB precedent; matches Material Web `skipRestoreFocus=false`).

### Keyboard navigation *(authoritative table — supersedes §B)*
| Keys | Action |
|---|---|
| **Tab** | Focus lands on menu (trigger) |
| **Space / Enter** | Closed: opens menu or **submenu**. Open: **selects** the focused item |
| **Up / Down** | Closed: opens menu. Open: moves focus **prev/next item** |
| **Left / Right** | **Opens / closes a submenu** |
| **Letters** | Type-ahead — focus to next item starting with that letter |
| **Escape** | Closes menu |
*(Material Web also wires **Home/End** = first/last item — keep as a sensible addition; not in M3's table but standard.)*
→ **[CODE]** This is the full ElwhaMenu key map. Note **Left/Right = submenu open/close** (lands with Phase-2 submenus, §S); flat Phase-1 menu wires Up/Down/Enter/Esc/type-ahead/Home/End.

### Interactability
- **[CODE] ★ Disabled menu items CAN receive focus but aren't selectable.** (Do.) → differs from default Swing (disabled = unfocusable); Elwha must keep disabled items in the focus order so AT users know they exist. Pairs with §O "disable, don't remove."
- **[CODE] Dividers and gaps CANNOT receive focus.** (Don't let a separator take focus.)

### Labeling elements
- **[CODE]** Accessible **name = the menu item text** (a11y label same as visible label).
- **[CODE] ★ The leading icon is DECORATIVE** when the item has text — mark it decorative so screen readers don't double-announce ("the icon's accessibility label should be marked as decorative to avoid redundant verbalizations").
- **[CODE]** **Role is platform-dependent** (Web `menuitem`; Android "generic actionable element"). **Swing mapping:** menu container → `AccessibleRole.POPUP_MENU`, item → `AccessibleRole.MENU_ITEM` (cf. §B; mirrors `ElwhaNavRailDestination`'s bespoke `AccessibleJComponent`, §Q′).

> **Elwha a11y checklist (derived):** `POPUP_MENU`/`MENU_ITEM` roles · initial focus = first item · focus trap while open · restore to trigger on close · disabled items focusable-but-inert · separators non-focusable · accessible name = item text · leading icon decorative · selected = 3:1 + ✓ checkmark · full key map above. **All standard Elwha a11y infra (push-model, bespoke AccessibleJComponent) — no new dependencies.**

---

## §W. Density (from spec "Density" page, screen-cap 2026-06-04) — web-only per M3; **optional/deferred for Elwha**

- **[DOC]** *"On web only, density levels control the spacing between elements. Increasing density decreases the top and bottom padding."*
- Four levels: **0, −1, −2, −3** ("Density of menus from 0 to −3"). Higher density → smaller **top/bottom** item padding (the §I 8dp top/bottom shrinks); leading/trailing/icon stay.

**Elwha stance:**
- M3 scopes density **web-only**; Elwha ships **density 0** by default (the §I numbers: 44dp item, 8dp top/bottom).
- BUT desktop is a "dense product" context (§U Motion calls out desktop) — a **compact menu** is a reasonable desktop want. So: an **optional `density(0..−3)` setter** that trims only top/bottom padding is a **plausible future affordance**, **not v1 scope** unless a consumer asks. #238 doesn't need it.
- Recorded so the skip is explicit (not an invented cut): **density deferred, default 0.**

---

## §V. Focus & shape morphing (from spec "Focus" page, screen-cap 2026-06-04) — **the corner detail**

- **[DOC]** "When a menu has multiple submenus, **focus follows the current hovered or focused submenu.**"
- **[CODE] ★ Shape morphing (the corners on the sub):** *"As a person moves from one submenu to the next, the corners of the **focused submenu become more rounded**, while the **unfocused submenu becomes less rounded**. This adds a dynamic quality to menu interactions."*
- **[DOC]** "On a custom menu, the **corner shape changes to indicate focus** as the cursor moves across submenus."

**Precise corner behavior (this is what §L #6 "active state" actually is):**
- The **focused** menu/submenu container **rounds MORE** (larger corner radius).
- The **unfocused** sibling(s) **square off** (smaller corner radius).
- As focus moves submenu→submenu, the two **animate** the swap → "dynamic quality."

**Elwha mapping:**
- This is a **container corner-radius morph between two shape states** (focused = more-rounded, unfocused = less-rounded) → drive with the shared **`ShapeMorphPainter`** ([[project_m3_morph_helper]], #176), the same engine used for Button/IconButton/FAB shape morphs. **Do not roll a bespoke animator.**
- Concretely (to pin in Phase 0): focused ≈ `ShapeScale.LG`/`XL`, unfocused ≈ `ShapeScale.SM`/`MD` — exact values from the §I `Menu *container shape` tokens (swatch-only there; confirm radii on the Specs page if needed).
- Honors reduced-motion (snap to end-state) via `MorphAnimator` / `ElwhaTheme.config().reducedMotion()`.
- **Only meaningful with submenus** → lands with the **submenu story (Phase 2, §S)**; the flat Phase-1 menu has a single container and no focus-morph between siblings.

→ Reinforces §L (active state) and §S (submenu phase). Ties the §I per-position corner tokens (`menu active/inactive container shape`) to a concrete behavior: **active = focused = more rounded; inactive = unfocused = less rounded.**

---

## §U. Behavior (from spec "Behavior" subsections, screen-cap 2026-06-04)

### Appearing
- **[DOC]** A menu appears when a person interacts with an element: **button, split button, text field, filter chip, or highlighted text / selected image.** (confirms §N open-from list; adds split-button + selected-image.)
- **[CODE]** "If opened at the top of the screen, it **expands downwards to avoid being cropped**." → confirms the §R flip/shift engine.

### Motion *(updates §L)*
- **[CODE]** Menus use an **enter and exit transition** that "creates a relationship between the menu and the element that generates it" (scale/fade from the trigger — the same `MorphAnimator` entrance pattern as the dialog/FAB).
- **[CODE]** **When the menu expands, the trigger element shows a pressed state** (see Selecting). **On item selection, a ripple appears** (reuse `RipplePainter`).
- **[CODE] ★ "In dense products, such as on desktop, menus can open *instantly* to reduce motion." / "Desktop menus can open instantly."** → **Elwha is desktop — instant (or minimal) open is explicitly sanctioned.** Default to a short entrance, but instant-open is on-spec and aligns with reduced-motion (`ElwhaTheme.config().reducedMotion()`). Lowers the motion bar for v1.

### Selecting *(trigger-control behavior — matters for #238)*
- **[CODE]** "When a menu is opened, the corresponding **button / icon button should remain the same visually, with the addition of a pressed state**." Holds **even when opened via keyboard shortcut.**
- **[DOC]** "Choosing a menu option **doesn't change the icon** generating the menu." → the trigger is stateless w.r.t. selection; the menu doesn't mutate its opener. Directly relevant to #238's overflow `ElwhaIconButton` (shows pressed while open, unchanged after).

### Filtering *(autocomplete — likely out of core §298 scope)*
- **[DOC/future]** A menu can include a **text field to filter options** ("also known as autocomplete"). As the person types, options filter to matches; items **ease into new positions** as the list filters.
- → This is the **exposed-dropdown / combobox** assembly (text field + filtering menu). `ElwhaList<T>` already has a filter contract, but the field+menu composition is a **separate assembly** — recommend **out of core ElwhaMenu v1**, candidate future "ElwhaExposedDropdown" (pairs with §J #9 "Label text", §P field-label note). **Data point for it:** the field/anchor row is **32dp** tall with `LABEL_LARGE` text (§M mark 1).

### Scrolling *(sharpens §N)*
- **[CODE]** Menus **scroll when items overflow**; in that state they show a **persistent scrollbar**.
- **[CODE] ★ "Don't use gaps if a menu scrolls; this is currently unsupported."** → stronger than §N's "prefer divider": **gaps are UNSUPPORTED in a scrollable menu** — Elwha must force/route to `Separator.DIVIDER` (or no separator) when the menu scrolls. Update applied to §N.

### Single- and multi-select menus *(new API axis)*
- **[CODE]** **Single-select** — one item selected at a time; selecting a new item **auto-unselects the previous**; menu typically closes on select.
- **[CODE]** **Multi-select** — many items selectable; the menu **stays open** until the person dismisses it (the `keepOpen`-on-select behavior, §B).
- "More on selection accessibility requirements" (sub-page, uncaptured).
- → **New enum: `SelectionMode { NONE (actions only), SINGLE, MULTI }`.** Mirrors the existing family pattern (`CardSelectionMode` / `ChipSelectionMode`) and the cross-cutting selection surface in epic #252. **Multi-select pins the "stays open on select" rule.** Added to §E.

> **Net new constraints for the design doc:** desktop **instant-open** is on-spec (§L softened); **gaps forbidden when scrolling** (§N hardened); **trigger shows pressed while open, unchanged after** (#238); **SelectionMode** is a first-class axis.

---

## §T. Adaptive design (from spec "Adaptive design" page, screen-cap 2026-06-04) — mostly **N/A for Elwha**

- **[DOC, N/A]** *Compact window sizes:* "Consider adapting menus into **bottom sheets** on small screens… A bottom sheet can replace a menu on smaller screens."
- **[DOC]** *Other window sizes:* "On **medium and expanded** windows, menus are most effective as they appear in context… On larger screens, menus can also display more items, and can use **submenus**… On large screens, a menu is often more appropriate than a bottom sheet."

→ **Elwha is desktop Swing — effectively always medium/expanded/large.** The bottom-sheet fallback is **out of scope**: ElwhaMenu always renders as a menu; there's no compact-window breakpoint to swap to a bottom sheet. Submenus on large screens are fine (§S). Breadcrumb: a **bottom sheet** is a separate M3 primitive (future epic, cf. the side-sheet epic #308) — *not* a responsive mode of ElwhaMenu. **Recorded so the skip is explicit, not silent.**

---

## §R. Placement (from spec "Placement" page, screen-cap 2026-06-04) — [CODE]

- **"A menu is positioned relative to the window edge. It typically appears below, next to, or in front of the element that generates it."**
- **"If a menu is in a position to be cut off, it should automatically reposition to appear to the left, right, or above the element that generates it."**
- "Menus can appear around or in front of the element that opened them." (6-cell grid: below-left, below-offset, below-right/top-anchor, above, side/in-front, etc.)

→ **Confirms §B anchored positioning + viewport flip.** The popup host needs:
- **Anchor**: open **below** the trigger by default, aligned to its leading edge (M3 `anchorCorner=END_START` / `menuCorner=START_START`, §B).
- **Flip/shift on clip**: if the menu would be cut off by the **window edge**, reposition **left / right / above**. Viewport = the host **frame / layered-pane bounds** (Swing analog of M3's window edge).
- This is the bespoke-host argument (§E Q1): the dialog host *centers*; the menu host *anchors + flips* — different positioning engine, shared dismiss/focus plumbing.

---

## §S. Submenus (from spec "Submenus" page, screen-cap 2026-06-04)

- **[CODE]** **"Submenus should open next to the parent menu item without overlapping it."** → side placement (M3 sub-menu `anchorCorner=START_END`, §B); no overlap with the parent.
- **[DOC]** **"Submenus are best used on large screens where there's space"** (mobile → adaptive alternatives). Elwha is **desktop Swing → always large-screen**, so this constraint is satisfied by platform; no mobile fallback needed.
- **[CODE]** **"Position submenu to the side of the parent item."** Active parent gets the shape-morph active state (§L). Sample: Google-Docs Format → Line & paragraph spacing → submenu opens right (Custom 1.2 selected).

→ **Submenu scope (RESOLVED §E Q3):** the spec treats submenus as core, but the **#238 consumer needs none** (flat overflow list). **Submenus split to V2 epic [#322](https://github.com/OWS-PFMS/elwha/issues/322)** — V1 (#298) ships the flat `ElwhaMenu` + `open(anchor)` + flip (unblocks #238); `ElwhaSubMenuItem` + side-placement + active-morph land in V2. Keeps the V1 critical path short.

---

## §Q′. Architecture insight — `ElwhaMenuItem` is a dedicated primitive (operator, 2026-06-04)

> Operator: *"reading this usage, I really see the need for a (many) menuitem prim similar to the nav destination."*

**Decision direction:** `ElwhaMenuItem` is its own **dedicated `JComponent` primitive** — *not* a styled `JMenuItem`, *not* a Button/IconButton in a mode — exactly mirroring **`ElwhaNavRailDestination extends JComponent implements IconBearing`** (the rail destination is a bespoke ~1000-line primitive with its own `AccessibleJComponent`, leading-icon + label + active-indicator anatomy, state painting). Precedent: [[project_navigation_rail_epic]] — *"rail destination = dedicated component, NOT Button/IconButton extended-mode."*

Why this fits the §Q slot model:
- A menu item = **container with a swappable slot** → a `JComponent` that owns its anatomy (leading icon / label / supporting text / trailing text / trailing icon / badge) and can host a **slot** component in place of the default label region.
- Gets its own `AccessibleRole.MENU_ITEM` (mirrors the destination's bespoke a11y), 48dp target, focus/hover/press/selected/disabled painting, and the active shape-morph (§L).

**"a (many)" — likely a small family, to firm up in Phase 0:**
- `ElwhaMenuItem` — standard (icon + label + supporting/trailing/badge).
- `ElwhaMenuItem` with **slot** content (image / progress / color-swatch) — same class with a slot setter, or a sibling.
- (deferred) `ElwhaSubMenuItem` — trailing caret + nested `ElwhaMenu` (§E Q3).
- Possibly a **divider** item type (anatomy #11) as a non-interactive `ElwhaMenuItem` variant or a separate lightweight separator.
- Connects to the open **aggregate-split question** [[project_aggregate_split_question]]: one item class with modes vs. a family of item primitives. **Phase-0 decision.**

→ Add to §E: **Q7 — item primitive family shape** (single `ElwhaMenuItem` w/ slot setter vs. a family). Lean: single primitive + swappable slot (matches "container with a swappable slot"), with submenu/divider as the only genuine siblings.

---

## §P. Terminology lock — M3 noun → ElwhaMenu API *(operator: "our API must reflect this terminology")*

The component API mirrors M3's exact nouns. Authoritative names from Anatomy (§J), Configurations (§H), Color (§K), States (§L):

| M3 term | ElwhaMenu API (proposed) | Kind |
|---|---|---|
| Menu (vertical) | `ElwhaMenu` | class |
| Menu item | `ElwhaMenuItem` | class |
| Container | the `ElwhaMenu` surface | — |
| Leading icon | `ElwhaMenuItem` leading-icon slot (`leadingIcon`) | slot |
| Menu item text / **Label text** (item) | item label (`text` / `label`) | slot |
| Supporting text | item supporting text (`supportingText`) | slot |
| **Slot** (swappable content) | item content slot (`setSlot(JComponent)`) — image / progress / swatch; §Q | slot |
| Trailing icon | trailing-icon slot (`trailingIcon`) — incl. submenu caret | slot |
| Trailing text / **keyboard command** | trailing text slot (`trailingText`, a.k.a. shortcut) | slot |
| Badge | item badge slot (`badge`) — reuse `ElwhaBadge`/`IconBearing` (#209) | slot |
| Gap (group separator) | `Separator.GAP` | enum value |
| Divider (group separator) | `Separator.DIVIDER` | enum value |
| Standard vertical menu | `Layout.STANDARD` | enum value |
| Grouped vertical menu | `Layout.GROUPED` | enum value |
| Standard (color) | `ColorStyle.STANDARD` | enum value |
| Vibrant (color) | `ColorStyle.VIBRANT` | enum value |
| Selected (item) | `setSelected(boolean)` / selected state | state |
| Single-select / Multi-select | `SelectionMode.SINGLE` / `.MULTI` (+ `.NONE` for action menus) | enum value |
| Keep-open-on-select (multi) | item/menu `keepOpen` behavior | behavior |
| Submenu | `ElwhaSubMenu` / `setSubMenu(...)` *(if in v1; §E Q3)* | — |
| States: Enabled / Disabled / Hovered / Focused / Pressed / Active | standard Swing + `setEnabled`; Active = submenu-open shape-morph (§L) | states |
| Label text (field, exposed dropdown) | **out of core** — belongs to a future exposed-dropdown assembly (§J #9) | — |

**Naming caveats to settle in Phase 0:**
- "Label text" is **overloaded** in M3: it's both the *menu item's* text (anatomy #3 calls it "Menu item text", but the Menu-items page says items include "label text") **and** the *exposed-dropdown field label* (anatomy #9 "Label text"). Elwha should use **`label`/`text` for the item** and reserve "field label" for the future dropdown — avoid the collision.
- Keep enum value names matching M3 exactly (`STANDARD`/`VIBRANT`, `STANDARD`/`GROUPED`, `GAP`/`DIVIDER`) per the operator directive.

---

## §N. Guidelines (from spec "Guidelines" page, screen-cap 2026-06-04)

Tagged **[CODE]** = enforceable/affordable in the ElwhaMenu API · **[DOC]** = usage guidance for the Javadoc/README, not enforced.

### Usage
- **[DOC]** Use a menu for a *temporary* set of actions; for always-on actions use a **toolbar** instead. *(echoes §A)*
- **[DOC]** A menu takes **less space than a set of radio buttons or chips** — positioning guidance vs `ElwhaChipList`/radio.
- **[DOC]** Color: Standard = surface, lower emphasis (default); Vibrant = tertiary, higher emphasis — **"use sparingly."** *(the enum itself is [CODE], §K; "sparingly" is [DOC].)*
- Sample shown: a text-editor formatting menu with **submenus** (Line spacing → `Custom: 1.2` selected, vibrant fill) — reinforces submenu + selected-state.

### Opening menus
- **[CODE]** **"Menus temporarily appear in front of all other UI elements."** → the popup host must **top all content** (z-band, coordinates with #221). Load-bearing for the host decision (§E Q1/Q2).
- **[CODE/DOC]** A menu opens when a person: **selects** an element (icon, button, text field) **or** performs a **trigger action** (right-click / press-and-hold). → Elwha provides the **`open(anchor)`** path [CODE]; the *choice* of trigger is the consumer's [DOC]. A **right-click/press-and-hold → context-menu** binding helper is an optional [CODE] affordance (see Context menus below).
- **[DOC]** Use menus for: **overflow menus** (← #238 consumer), text-field dropdowns, select menus, context menus.

### Menu groups
- **[CODE]** Vertical items can be **grouped via a divider or a small gap** — already modeled as Layout `GROUPED` + separator style (§H/§I). "More scannable when grouped."
- **[DOC]** Use groups to **bundle similar actions**; sample contrasts Standard (flat, 4 items) vs Grouped (2+3 with a gap).

### Gaps & dividers (optional) — the when-which rule *(captured 2026-06-04)*
Two separator treatments for the **Grouped** layout (§H):
- **Gap** — "visually divide menu items into distinct groups. Gaps are **more expressive** than dividers and make the relationship between items clear." Each group renders as its own rounded card ("Gaps separate menu items using **expressive shapes**").
  - **[DOC]** Avoid changing the size of the gap.
  - **[DOC]** Limit the number of gaps in a menu to **one or two**.
  - **[CODE]** **Gaps are UNSUPPORTED in scrollable menus** (§U Scrolling: *"Don't use gaps if a menu scrolls; this is currently unsupported"*). → when the menu scrolls, Elwha must force `Separator.DIVIDER` (or none); gap+scrollable is invalid, not merely discouraged.
- **Divider** — "create a **more subtle** separation." Use a divider for:
  - **[CODE/DOC]** **Scrollable menus** (the gap-forbidden case above).
  - **[DOC]** Text fields with a dropdown menu, where a grouped treatment isn't appropriate.
  - **[DOC]** "On web, use a divider to separate menu items" + "Dividers separate menu items in **baseline** menus and on web." *(platform note; Elwha is desktop Swing — gap is the expressive default, divider the scrollable/subtle fallback.)*

→ **Elwha rule:** Grouped layout defaults to **gap** (expressive); auto-switch to / require **divider** when the menu is **scrollable**. Cap gaps at 1–2 groups (DOC).

### Context menus
- **[DOC]** Context menus = additional actions for a specific item; opened by **secondary click** (right-click / two-finger trackpad tap). Sample: webpage right-click → Open in new window / Save link as / Copy address / Inspect.
- **[CODE — optional, scope Q]** Elwha *could* ship an `installContextMenu(JComponent, ElwhaMenu)` helper that binds the right-click/press-and-hold → anchored-open. **Not needed by #238** (rail overflow is a left-click on an icon button). → **flag for Phase 0:** is context-menu (right-click) binding in v1 scope, or deferred? Added to §E questions.

> **No conflicts with prior sections.** Guidelines reinforces: toolbar-vs-menu split (§A), color emphasis (§K), grouping (§H), and pins the **z-order "in front of all UI"** rule that informs the host/z-band decision.

---

## §M. Measurements redline — cross-check vs §I tokens (screen-cap 2026-06-04)

The "Measurements" diagram (vertical menu padding/size redlines). **Verified each legible redline against the §I token values:**

| Redline (dp) | Matches §I token | Verdict |
|---|---|---|
| **2** (between items / before group) | Menu gap 2dp · Menu group padding 2dp | ✅ consistent |
| **8** (item top, and at caret) | Menu item top/bottom space 8dp | ✅ consistent |
| **12** (icon↔label, bottom row) | Menu item between space 12dp | ✅ consistent |
| **16** (item leading/trailing) | Menu item leading/trailing space 16dp | ✅ consistent |
| **20** (left & right of Item 3) | Menu item leading + trailing icon size 20dp | ✅ consistent |

→ **The core item-padding/icon tokens from §I are corroborated by the redline.** No conflicts found among the legible values.

**Both initially-ambiguous marks now RESOLVED:**
1. **"32"** by the "Label" row → **RESOLVED (operator, 95%):** it's the **height of the "Label text" field component** — i.e. the **exposed-dropdown's anchor/field row** (anatomy #9), **32dp** tall; its text reuses the **menu-item label type** (`LABEL_LARGE`, §I). This is **not a core popup-menu metric** — it belongs to the future exposed-dropdown assembly. Data point for that future component: **field height 32dp.**
2. **"48"** near Item 2 → **RESOLVED (§Q):** the **48dp is the minimum touch target** ("Targets should be 48×48dp or larger"). The **44dp** (§I) is the *visual* item height. Both correct. **Elwha item rows: 44dp visual, 48dp min interactive target.**

> **§M fully verified.** Every core item metric (2/8/12/16/20/44/48) checks out against §I; the only non-§I mark (32) is the exposed-dropdown field height, out of core ElwhaMenu scope.

---

## §L. States (from spec "States" page, screen-cap 2026-06-04 — light + dark identical)

**Six interaction states** (numbered per the spec diagram):
| # | State | Treatment |
|---|---|---|
| 1 | **Enabled** | resting; no overlay |
| 2 | **Disabled** | item content dimmed (reduced opacity — M3 standard 38%) |
| 3 | **Hovered** | `ON_SURFACE` state layer over the item (+ pointer cursor) |
| 4 | **Focused** | the **3dp inset focus ring** (`SECONDARY` / −3dp offset, §I) on the item |
| 5 | **Pressed** | stronger `ON_SURFACE` state layer (ripple origin) |
| 6 | **Active (main menu reveals submenu)** | parent item marked active; **submenu opens to the side**; **focused menu rounds MORE / unfocused squares off** (§V) |

**Headline finding — shape-morph active state (direct Elwha reuse hook):**
> *"Shape morphing in vertical menus creates an expressive active state. As focus moves between submenus, the corner shape changes to highlight the active menu."*

- The **active** state (#6) is expressed by **corner-shape morphing**, not just a fill — when a submenu opens / focus enters a submenu, the parent menu's corner shape animates to highlight which menu is active.
- This maps **directly onto Elwha's shared `ShapeMorphPainter`** ([[project_m3_morph_helper]], extracted in FAB/Button-group work, #176) — the same engine that morphs Button/IconButton/FAB shapes. ElwhaMenu's active-state morph should reuse it, not roll its own. **Strong argument for landing the submenu/active-state story only after `ShapeMorphPainter` is available** (it is).
- Ties back to the §I per-position corner tokens (`first/last child shape`, `inner corner corner size`): those are the rest shape; the morph animates between rest and active.

**State-layer opacities:** not shown numerically here; M3 standard (hover 8% / focus 10% / press 10%) — Elwha's `StateLayer` already encodes these, so no new values.

**Reduced-motion:** the shape-morph active state must honor reduced motion (snap, no animation) — `MorphAnimator` / `ElwhaTheme.config().reducedMotion()` already auto-detect, same as FAB/dialog.

**Motion detail (§U):** enter/exit transition relates menu↔trigger; trigger shows **pressed** during expand; item-select shows a **ripple** (`RipplePainter`). **★ Desktop menus may open *instantly*** (on-spec, §U Motion) — Elwha can default to a minimal entrance and treat instant-open as a first-class, reduced-motion-aligned option.

---

## §K. Color (from spec "Color" page, screen-cap 2026-06-04) — **CORRECTS earlier secondary-container guesses**

**Two color mappings:**
- **Standard** → **Surface-based** (lower emphasis; the default).
- **Vibrant** → **Tertiary-based** (higher emphasis; *"more prominent, use sparingly"*).

> ⚠️ **CORRECTION:** earlier sections (§A, §B/§C, §D, §G, §H) called the selected fill *secondary-container*. **It is TERTIARY**, not secondary. The pink selected fill I read as "purple/secondary" is M3 **tertiary** (baseline tertiary container is the rose `0xFFD9E3`). All selection + vibrant roles below are **tertiary**.

### Standard color scheme — roles by anatomy part (light + dark identical roles)
| Part | Role |
|---|---|
| 1 Leading icon | `ON_SURFACE_VARIANT` |
| 2 Menu item text (label) | `ON_SURFACE` |
| 3 State layer (hover/press) | `ON_SURFACE` (state layer) |
| 4 **Container** | **`SURFACE_CONTAINER_LOW`** |
| 5 Trailing icon / badge | `ON_SURFACE_VARIANT` |
| 6 Trailing text | `ON_SURFACE_VARIANT` |
| 7 **Selected item container** | **`TERTIARY_CONTAINER`** |
| 8 Selected item content | **`ON_TERTIARY_CONTAINER`** |
| 9 Label text | `ON_SURFACE_VARIANT` |
| 10 Gap | `ON_SURFACE_VARIANT` |
| 11 Selected indicator (✓ checkmark) | `ON_TERTIARY_CONTAINER` |

→ **Standard selected = `TERTIARY_CONTAINER` fill + `ON_TERTIARY_CONTAINER` text/icon/checkmark.** Everything else surface/on-surface-variant. Container is `SURFACE_CONTAINER_LOW` (note: MDC-Android baseline said `surfaceContainer` — Expressive vertical menu drops one tier to **low**; use the Expressive value).

### Vibrant color scheme — roles by anatomy part
| Part | Role |
|---|---|
| 1 Leading icon | `ON_TERTIARY_CONTAINER` |
| 2 Menu item text | `ON_TERTIARY_CONTAINER` |
| 3 State layer | `ON_TERTIARY_CONTAINER` (state layer) |
| 4 **Container** | **`TERTIARY_CONTAINER`** |
| 5 Trailing icon / badge | `ON_TERTIARY_CONTAINER` |
| 6 Trailing text | `ON_TERTIARY_CONTAINER` |
| 7 **Selected item container** | **`TERTIARY`** |
| 8 Selected item content | **`ON_TERTIARY`** |
| 9 Label text | `ON_TERTIARY_CONTAINER` |
| 10 Gap | `ON_TERTIARY_CONTAINER` |
| 11 Selected indicator (✓) | `ON_TERTIARY` |

→ **Vibrant = the entire surface tints `TERTIARY_CONTAINER` with `ON_TERTIARY_CONTAINER` content; selected item is the bold `TERTIARY` fill + `ON_TERTIARY`.** That's why it "stands out" — selection jumps from container-tone to full-tone within an already-tinted menu.

**Elwha color model (derived):**
- `ElwhaMenu.ColorStyle.STANDARD` (default): container `SURFACE_CONTAINER_LOW`, label `ON_SURFACE`, icons/trailing/supporting `ON_SURFACE_VARIANT`, selected `TERTIARY_CONTAINER`/`ON_TERTIARY_CONTAINER`.
- `ElwhaMenu.ColorStyle.VIBRANT`: container `TERTIARY_CONTAINER`/`ON_TERTIARY_CONTAINER` throughout, selected `TERTIARY`/`ON_TERTIARY`.
- Both: light/dark use the **same roles** (the token system flips the resolved color) → Elwha gets dark mode free via `ElwhaTheme`.
- All four roles already exist in `ColorRole` — **still zero new tokens**.

---

## §I. M3 design tokens — "Menus – Common", Default/Light (screen-cap 2026-06-04, **verbatim**)

> Source: m3.material.io token table, token set **"Menus – Common"**, theme **"Default, Light"**. Values transcribed exactly from the swatch cards. These are the **M3 Expressive vertical-menu** tokens.

### Typography
| Token | Value | Elwha `TypeRole` |
|---|---|---|
| Menu item label text font | Roboto | (Inter in Elwha) |
| Menu item label text line height | **20pt** | — |
| Menu item label text size | **14pt** | — |
| Menu item label text tracking | **0.1pt** | — |
| Menu item label text weight | **500** | — |
| → label composite = **`LABEL_LARGE`** (14 / 20 / +0.1 / medium-500) | | `TypeRole.LABEL_LARGE` |
| Menu item supporting text font | Roboto | |
| Menu item supporting text line height | **16pt** | |
| Menu item supporting text size | **12pt** | |
| Menu item supporting text tracking | **0.4pt** | |
| Menu item supporting text weight | **400** | |
| → supporting composite = **`BODY_SMALL`** (12 / 16 / +0.4 / regular) | | `TypeRole.BODY_SMALL` |
| Menu item trailing supporting text font | Roboto | |
| Menu item trailing supporting text line height | **20pt** | |
| Menu item trailing supporting text size | **14pt** | |
| Menu item trailing supporting text tracking | **0.1pt** | |
| Menu item trailing supporting text weight | **500** | |
| → trailing (shortcut/`⌘C`) composite = **`LABEL_LARGE`** | | `TypeRole.LABEL_LARGE` |

**Correction to §D:** item label is **`LABEL_LARGE`**, *not* `BODY_LARGE` (MDC-Android's "bodyLarge default" is the older baseline value; the Expressive token table is authoritative → `LABEL_LARGE`).

### Shape (swatch-only — no numeric corner in the table; capture the token NAMES)
*Shape / Default:* `Menu container shape`, `Menu active container shape`, `Menu inactive container shape`, `Menu group shape`, `Menu item shape`, `Menu item first child shape`, `Menu item first child inner corner corner size`, `Menu item last child shape`, `Menu item last child inner corner corner size`.
*Shape / Selected:* `Menu item selected shape`.
*Shape / Horizontal, Default:* `Menu horizontal container shape` (**full circle**), `Menu horizontal menu item hovered shape`, `… focused shape`, `… pressed shape`.
*Shape / Horizontal, Selected:* `… selected hovered shape` (**circle**), `… selected focused shape` (**circle**), `… selected pressed shape` (**circle**).
*Shape / Horizontal icon-only, selected:* `Menu horizontal icon only menu item selected shape` (**circle**).

> **Reading of the shape system:** the vertical menu's items carry **per-position corner rounding** — `first child` and `last child` get the outer rounding while `inner corner corner size` keeps the touching edge tight, so a Grouped list reads as one rounded slab with crisp interior seams. The **selected** item has its own shape token (the rounded-fill highlight). Horizontal-menu item states round to a **full circle/stadium** (the chip-like horizontal toolbar variant). Numeric corner radii weren't in this view — grab the **Specs** page if exact dp is needed; Elwha's working assumption: container/item ≈ `ShapeScale.SM`(8)–`MD`(12), selected fill rounds to item shape.

### Layout / Default (the dp measurements — the important part)
| Token | Value |
|---|---|
| Menu gap | **2dp** |
| Menu group padding | **2dp** |
| Menu container elevation | swatch (Level 2–3; MDC-Android lists **3dp**) |
| **Menu item height** | **44dp** |
| Menu item top space | **8dp** |
| Menu item bottom space | **8dp** |
| Menu item leading space | **16dp** |
| Menu item trailing space | **16dp** |
| Menu item between space (icon↔label etc.) | **12dp** |
| **Menu item leading icon size** | **20dp** |
| **Menu item trailing icon size** | **20dp** |

### Layout / Horizontal *(horizontal menu variant — out of #298 scope, captured for completeness)*
| Token | Value |
|---|---|
| Menu horizontal container top space | 8dp |
| Menu horizontal container bottom space | 8dp |
| Menu horizontal menu item leading space | 12dp |
| Menu horizontal menu item trailing space | 12dp |
| Menu horizontal menu item between space | 12dp |
| Menu horizontal menu item top space | 6dp |
| Menu horizontal menu item bottom space | 6dp |

### Layout / Horizontal icon-only
| Token | Value |
|---|---|
| leading / trailing / top / bottom space | **16dp** each |
| Menu horizontal icon only gap | **4dp** |

### Focus ring
| Token | Value | Note |
|---|---|---|
| Menu item focus indicator color | **#625B71** | = M3 baseline **SECONDARY** → `ColorRole.SECONDARY` |
| Menu item focus indicator thickness | **3dp** | |
| Menu item focus indicator outline offset | **−3dp** | inset ring (draws inside the item edge) |

> **"Horizontal menu" tokens are a token-table ghost — IGNORE for ElwhaMenu.** The `Layout/Horizontal`, `Layout/Horizontal icon-only`, and `Shape/Horizontal*` blocks exist in the generated **"Menus – Common"** token set, but **NO horizontal example appears anywhere in the spec** (Anatomy page is titled *"Vertical menus"*; Overview / Variants / Configurations are all vertical) **and no implementation carries it** (Material Web `menu.md`, MDC-Android, Angular Material have zero "horizontal" / "orientation" references — verified 2026-06-04). Most likely these are either shared/generated template tokens or the backing for the **Toolbar** component (the M3 text-selection / floating action bar with circular icon-only items — the Overview's own *"use a toolbar instead"* pointer). **Decision:** ElwhaMenu (#298) ships the **vertical menu only**; the horizontal tokens are *not* a deferred menu variant. Breadcrumb: if/when a **Toolbar** epic is filed, revisit these tokens there.

### Elwha build numbers (derived from §I for Phase 0)
- Item row: **44dp tall**, **16dp** leading/trailing insets, **8dp** top/bottom, **12dp** icon↔label gap, **20dp** leading + trailing icon.
- Menu: **2dp** inter-item gap, **2dp** group padding, Level-3 shadow (`ShadowPainter`).
- Focus ring: 3dp `SECONDARY`, −3dp (inset) offset → maps onto the existing focus-ring painter pattern.
- Label `LABEL_LARGE` / supporting `BODY_SMALL` / trailing-shortcut `LABEL_LARGE`.

---

## §F. Captured screenshots log
- 2026-06-04 — Overview "usage rules" (menu vs toolbar; opens-from; context menus) → §A.
- 2026-06-04 — Overview "M3 Expressive update (Nov 2025)" (vertical menus, Standard/Vibrant, selection state) → §A.
- 2026-06-04 — Variants: "Vertical menus" (gap vs divider sub-layouts, anatomy, vibrant selected fill) → §G.
- 2026-06-04 — Variants: "Baseline variant" (square corners, supporting text, checkmark selection) → §G.
- 2026-06-04 — Variants: availability table (vertical = Expressive-only; baseline = both) → §G.
- 2026-06-04 — Configurations: "Vertical menus layout" Standard vs Grouped (dark samples) → §H.
- 2026-06-04 — Configurations: availability table (Color {Standard/Vibrant}, Layout {Standard/Grouped}) → §H.
- 2026-06-04 — Tokens "Menus – Common": Typography (label/supporting/trailing) → §I.
- 2026-06-04 — Tokens: Shape/Default + Shape/Selected (per-position corner system) → §I.
- 2026-06-04 — Tokens: Shape/Horizontal + Horizontal-Selected + Horizontal-icon-only (circle shapes) → §I.
- 2026-06-04 — Tokens: Layout/Default (item 44dp, icons 20dp, gaps/spaces) → §I.
- 2026-06-04 — Tokens: Layout/Horizontal + Horizontal-icon-only → §I.
- 2026-06-04 — Tokens: Focus ring (#625B71 / 3dp / −3dp) → §I.
- 2026-06-04 — Anatomy "Vertical menus" (11 parts; new: Badge slot #5; vertical-only) → §J.
- 2026-06-04 — Horizontal-menu verdict: token-table ghost, no examples/impl → §I note (confirmed via Material Web/MDC/Angular = zero "horizontal").
- 2026-06-04 — Color: Standard (surface) + Vibrant (tertiary) role maps, light+dark → §K (corrected secondary→tertiary).
- 2026-06-04 — States: 6 states + shape-morph active state → §L.
- 2026-06-04 — Measurements redline (cross-checked vs §I: 2/8/12/16/20 all ✅; "32" + "48?"/​"4+8" flagged unverified) → §M.
- 2026-06-04 — Guidelines: Usage / Opening menus / Menu groups / Context menus (CODE-vs-DOC tagged) → §N.
- 2026-06-04 — Anatomy (re-paste, confirms §J — same 11 parts).
- 2026-06-04 — Menu items page (disabled-not-removed; label/leading/trailing/keyboard-command) → §O.
- 2026-06-04 — Gaps & dividers guidelines sub-page (when-gap-vs-when-divider; scrollable→divider) → §N.
- 2026-06-04 — Terminology lock → §P (operator: API must reflect M3 nouns).
- 2026-06-04 — Flexibility & slots + Slot accessibility (container-with-slot; **48dp target resolves §M**) → §Q.
- 2026-06-04 — Architecture insight: ElwhaMenuItem = dedicated primitive like ElwhaNavRailDestination → §Q′ (+ §E Q7).
- 2026-06-04 — Placement (anchor below/next-to; flip left/right/above on clip) → §R.
- 2026-06-04 — Submenus (side placement, no overlap, large-screen) → §S (recommend Phase 2).
- 2026-06-04 — Adaptive design (bottom-sheet on compact; N/A for desktop Swing) → §T.
- 2026-06-04 — Behavior: Appearing / Motion / Selecting / Filtering / Scrolling / Single-multi-select → §U (hardens §N gaps, softens §L motion, adds SelectionMode).
- 2026-06-04 — Focus & shape morphing (focused submenu rounds MORE, unfocused squares off) → §V.
- 2026-06-04 — Density (0..−3, web-only, trims top/bottom padding; deferred for Elwha) → §W.
- 2026-06-04 — A11y: Use cases / Interaction&style (3:1 + checkmark) / Focus / Keyboard nav table / Interactability (disabled focusable; separators not) / Labeling (name=text, icon decorative, platform role) → §X.
- *(append as pasted)*
