# M3 Card spec — organized reference

Reorganized form of [`m3-card-spec-findings.md`](m3-card-spec-findings.md).
That doc is the raw chronological walkthrough (one section per spec
frame, in the order we read them). This doc collects the same material
by topic — what M3 *says* about cards, with Elwha implications pulled
to a single follow-up section at the end.

When in conflict, this doc supersedes the chronological doc — earlier
sections in the findings file were partially superseded by later ones
(e.g., the §23 "actionability triad" became a quadrad in §24; the
§16c claim that "only dragged changes elevation" was corrected by §18c).
This file folds those corrections in.

**Source citation key:** every section cites the originating findings
section in parentheses (e.g., `(findings §15)`) so claims trace back to
the spec frame that produced them.

---

## Table of contents

1. [Anatomy and vocabulary](#1-anatomy-and-vocabulary)
2. [Layout and orientation](#2-layout-and-orientation)
3. [Variants (chassis)](#3-variants-chassis)
4. [Measurements and tokens](#4-measurements-and-tokens)
5. [Interaction and accessibility](#5-interaction-and-accessibility)
6. [Cross-cutting follow-ups for Elwha](#6-cross-cutting-follow-ups-for-elwha)
7. [Appendices](#7-appendices)

---

## 1. Anatomy and vocabulary

### 1.1 The six formal elements

M3 names exactly **six** formal card elements (findings §1):

1. Container
2. Image
3. Button
4. Supporting text
5. Subhead
6. Headline

Everything else — **leading icons, header-trailing icon buttons,
overflow menus, expand affordances, dividers, thumbnails as avatars** —
is a *pattern you compose with*, not a part of the primitive. Elwha's
"OWS extension slots" (`setLeadingIcon`, `setLeadingActions`,
`setTrailingActions`, disclosure axis) are extensions on top of M3, not
deviations from it.

### 1.2 Text vocabulary

Concrete role definitions from the spec (findings §4):

| Role            | Definition                                | Typical content                       |
| --------------- | ----------------------------------------- | ------------------------------------- |
| Headline        | Subject of the card                       | Photo album name, article title       |
| Subhead         | Smaller secondary text under headline     | Byline, location, timestamp           |
| Supporting text | Body content paragraph                    | Article summary, restaurant description |

**Headline reflow:** the visual-presentation spec shows the same
headline string rendering on 2 lines (narrow card) and 1 line (wide
card) without truncation. Headline word-wraps; does not ellipsize.

**Two-tier text pattern** (Gmail-style cards, findings §4, §23): the
same card can carry text in two tiers, observed in multiple frames:

- **Tier 1** — header row beside leading thumbnail. Identity +
  timestamp ("Carmen Villanueva" / "Yesterday"). Headline =
  `title-medium`, subhead = `label-medium` / `on-surface-variant`.
- **Tier 2** — body row below the thumbnail spanning full card width.
  Subject ("Graduación de Inés") + supporting-text preview. Subject
  styled stronger than supporting text — closer to a secondary
  headline.

M3 doesn't formally name "Tier 2"; the pattern recurs across four+
Gmail/conversation frames in the walkthrough, so we treat it as a
sanctioned composition.

### 1.3 Media types and roles

Three sub-kinds (findings §5):

| Kind      | Use                                       |
| --------- | ----------------------------------------- |
| Thumbnail | Avatar or logo (small, often leading)     |
| Image     | Photos, illustrations, weather icons (typically hero-sized) |
| Video     | Image plate + play overlay + duration label |

**Leading thumbnail is a first-class slot.** Recurring across multiple
frames (Caminante, Daniel Maas, Ana Russo, the variant color templates
that use the canonical "A" avatar, the Edit-card-order list with square
card-art thumbnails).

Two shape modes both sanctioned:
- **Circular thumbnail** — person/avatar use case
- **Square thumbnail** — object use case (card art, app icon, doc cover)

For screen-reader exposure, media has two sub-roles (findings §25):
- **Informative media** → `Image` role + alt-text label
- **Decorative media** → hidden from AT (not in traversal)

### 1.4 Action area

Sanctioned component types in the action area (findings §6):

- **Buttons** — "Learn more", "Add to cart"
- **Icon buttons** — "Save", "Heart", rating stars
- **Selection controls** — chips, sliders, checkboxes
- **Linked text** — inline link inside supporting text

A single action row may carry mixed types — e.g., choice-chip group +
outlined button; slider + transport icon buttons.

#### 1.4.1 Button styles

Three M3 button fill styles appear in card examples (findings §6, §16a,
§17a). All are full-shape pill:

| Style          | Fill                  | Label                       | When                                        |
| -------------- | --------------------- | --------------------------- | ------------------------------------------- |
| Filled         | `primary`             | `on-primary`                | Dominant primary CTA                        |
| Filled tonal   | `primary-container`   | `on-primary-container`      | Important paired actions; less heavy than filled |
| Outlined       | transparent + outline | `primary`                   | Secondary partner to a single filled CTA    |

A card never mixes more than two button styles in one action row.

#### 1.4.2 Action row segments

Action rows can have **leading and trailing segments** (findings §6,
§19). Canonical: primary buttons leading-left + overflow / icon
buttons trailing-right. Examples:

- Caminante card: filled-tonal × 2 leading + standard icon button × 2 trailing
- "Live music coming soon" cards: filled + outlined leading + overflow ⋮ trailing
- Padding-spec card: single trailing action

#### 1.4.3 Action row is fully optional

Some cards carry no action row at all (Daniel Maas / Ana Russo
conversation cards). Interaction is the card itself (click → open)
plus a trailing icon button in the header.

### 1.5 Header trailing slot

Polymorphic across at least three component types (findings §7):

- **Assist chip** (outlined, with leading icon) — Caminante card has two
- **Standard icon button** (no container) — gear, star, overflow ⋮
- **Overflow menu trigger** — see 1.6

Elwha currently types `setTrailingActions(ElwhaIconButton...)`. M3 says
chips are also valid. Decision pending (see [§6 follow-up](#61-api--surface-gaps)).

### 1.6 Overflow menu

Doctrine: *"Overflow menus contain related actions. They are typically
placed in the upper-right or lower-right corner of a card."* (findings §8)

Two sanctioned positions:

- **Upper-right** — header trailing slot
- **Lower-right** — trailing edge of the action row

Lower-right position confirmed by the "Get tickets / Learn more / ⋮"
action row recurring across multiple frames (§19, §24).

### 1.7 Dividers

Two divider treatments inside a card (findings §9):

- **Full-width divider** — spans the card edge-to-edge. Pairs with an
  expand text-link below.
- **Inset divider** — narrower (respects content padding). Separates
  related blocks within the body (metadata / comments footer row).

### 1.8 Expand affordance

Two sanctioned patterns (findings §10):

- **Chevron icon button** — header trailing (Elwha's current)
- **Text link** — body-bottom, paired with a full-width divider above
  ("Expand" link styled as a primary-color link)

---

## 2. Layout and orientation

### 2.1 Sanctioned vertical layouts

At least **three** vertical layout orders are sanctioned (findings §2,
§19):

1. **Media → Headline → Subhead → Supporting → Actions** (media-on-top — most familiar)
2. **Headline → Subhead → Supporting → Media → Actions** (Display small — media below text)
3. **Headline → Media → Supporting → Actions** (headline above media plate)

M3 does not lock a slot order; media's vertical position is a layout
choice, not a fixed anatomy position.

### 2.2 Horizontal orientation

M3 explicitly shows the same card content rendered in two orientations
(findings §3):

- **Vertical** — media on top, content stacked below
- **Horizontal** — media on the left full-card-height, content stacked
  in a right column

Caption from spec: *"Example of the same card with two different
orientations and element positioning."*

Elwha Card today is vertical-only — real expressivity gap.

### 2.3 Action alignment is pattern-dependent

Both `bottom-leading` and `bottom-trailing` are sanctioned by M3 — the
choice depends on the pattern, not the orientation alone (findings §3,
§6, §13, §15a):

| Pattern                                         | Alignment                  |
| ----------------------------------------------- | -------------------------- |
| Lone promo/discovery action ("Buy tickets")     | bottom-leading             |
| Paired actions ("Action / Action")              | bottom-trailing            |
| Single primary CTA on variant exemplar          | bottom-trailing            |
| Horizontal-card action in media column          | bottom-leading (of media column) |

Don't pick a single doctrinal default — let the consumer set it.

### 2.4 Adaptive / responsive design

Two transformations sanctioned (findings §11):

- **Grid reflow** at the parent level — 2-column (mobile) → single
  horizontal row (tablet/desktop). Container's responsibility, not the
  card's.
- **Internal orientation flip** — same card rendered vertical at narrow
  widths, horizontal at wide widths. Card-level concern (ties to 2.2).

Also (findings §14): **media size stays constant** as a card narrows;
only the text region reflows. Width-responsiveness lives in text
wrappers, not in scaling the media.

### 2.5 Internal scrolling on desktop

M3 sanctions two expansion behaviors, contextual (findings §12):

| Container layout                       | Recommended expansion behavior |
| -------------------------------------- | ------------------------------ |
| Vertical list / loose grid             | Grow + sibling reflow          |
| Carousel / horizontal row / fixed grid | Internal scroll (siblings stay put) |

Spec doctrine: *"On a desktop device, card content can expand and
scroll within a card."* — Elwha's current spec §9 says "never installs
internal scroll" and should be softened to an opt-in.

---

## 3. Variants (chassis)

### 3.1 Variant ↔ surface-role mapping

Per the canonical variant frames (findings §15b, §16b, §17b):

| Variant  | Container role               | Outline role         |
| -------- | ---------------------------- | -------------------- |
| Elevated | `surface-container-low`      | — (no outline)       |
| Filled   | `surface-container-highest`  | — (no outline)       |
| Outlined | `surface`                    | `outline-variant`    |

**Reading:**

- Elevated leans on **shadow** for surface separation.
- Filled leans on **tonal contrast** (`surface-container-highest` is
  the darkest/lightest tier of the surface-container ramp).
- Outlined leans on the **outline stroke**, so its fill drops to plain
  `surface`.

### 3.2 Per-variant anatomy callouts

Elevated and Filled call out **only Container** (a single numbered
chassis part). Outlined calls out **two parts** — Container + Outline —
because the stroke is structural for that variant (findings §15a, §16a,
§17a).

### 3.3 Variant ↔ primary-button pairing

M3 deliberately rotates button style across variants so the button
never visually duplicates the chassis treatment (findings §16a, §17a):

| Card variant | Paired primary-CTA button style |
| ------------ | ------------------------------- |
| Elevated     | Filled                          |
| Filled       | Outlined                        |
| Outlined     | Filled tonal                    |

Composition guidance, not enforcement — but worth documenting so
consumers pick the right pairing.

### 3.4 Interaction states (shared model)

Five sanctioned states across all variants. The **state-layer percentages
are identical across all three variants**; the **focus-ring color
differs** between Elevated/Filled vs Outlined (findings §15c, §16c, §17c,
§18d; corrected against AndroidX Compose `*CardTokens.kt`):

| State    | State layer (over container) | Focus indicator (when focused) | Elevation effect |
| -------- | ---------------------------- | ------------------------------ | ---------------- |
| Hovered  | `on-surface @ 8%`            | —                              | Lifts +1 level (all variants) |
| Focused  | `on-surface @ 10%`           | 2dp inset outline ring — color = `secondary` (Elevated/Filled) **or** outline-role-swap to `on-surface` (Outlined, see 3.5) | No elevation change |
| Pressed  | `on-surface @ 10%` + ripple  | —                              | No change        |
| Dragged  | `on-surface @ 16%`           | —                              | Largest lift; cursor = grab |
| Disabled | (no state layer)             | —                              | See [4.5](#45-disabled-opacities) |

**Correction (against AndroidX source):** the focus-ring color is
`secondary` for Elevated and Filled, *not* `on-surface`. Only the
Outlined variant uses `on-surface` for the focus indicator (because
there the outline itself acts as the ring — see 3.5).

```
ElevatedCardTokens.FocusIndicatorColor = ColorSchemeKeyTokens.Secondary
FilledCardTokens.FocusIndicatorColor   = ColorSchemeKeyTokens.Secondary
OutlinedCardTokens.FocusOutlineColor   = ColorSchemeKeyTokens.OnSurface
```

State layer paint is **variant-agnostic** for the chassis — applied over
whatever surface-container token the variant resolves to. No
per-variant branching needed for the state layer, but **focus-ring
color paint is per-variant** (secondary vs on-surface).

### 3.5 Outlined-only — per-state outline color

The Outlined variant is the only one where the *focus* state visibly
transforms the chassis (stroke role swap) rather than overlaying onto
it. Outline color per state (findings §17c, §18f):

| State    | Outline color                                     |
| -------- | ------------------------------------------------- |
| Rest     | `outline-variant`                                 |
| Hover    | `outline-variant` (unchanged)                     |
| **Focus**| **`on-surface`** — role swap from rest            |
| Pressed  | `outline-variant` (unchanged)                     |
| Dragged  | `outline-variant` (unchanged; visually softened by shadow lift) |
| Disabled | `outline` (full strength role) at 12% opacity     |

Focus on Outlined **replaces** the resting outline with `on-surface`
rather than painting a second inset ring inside the existing outline
(which would double-stroke). Disabled uses the *stronger* `outline`
role but at faint opacity — a faded full-strength stroke, not a weaker
role at full strength.

### 3.6 Surface-tint layer (confirmed deprecated for cards)

Earlier LLM sources mentioned `md.sys.color.surface-tint` as a
per-variant token applied as a tint overlay at elevated states (findings
§18g). **Confirmed against AndroidX source: no `SurfaceTintColor` token
exists in any of `ElevatedCardTokens.kt`, `FilledCardTokens.kt`, or
`OutlinedCardTokens.kt`.** Compose Material3 cards do not apply a
surface-tint layer.

**Stance: don't implement surface-tint.** The multi-tier
`surface-container-*` token ramp already encodes the elevation-tone
relationship.

---

## 4. Measurements and tokens

Cross-validated from four LLM-collected spec dumps + the canonical M3
Measurements frame. Values with 3+ of 4 sources agreeing treated as
high-confidence; disagreements flagged inline.

### 4.1 Shape / padding / spacing / typography

| Property                  | Value     | Token                          | Confidence |
| ------------------------- | --------- | ------------------------------ | ---------- |
| Corner radius             | 12dp      | `md.sys.shape.corner.medium`   | 4/4 + spec frame |
| Internal padding (all sides) | 16dp   | —                              | 3/4 + spec frame |
| Inter-card stack spacing  | 8dp max   | —                              | 3/4 + spec frame |
| Min interactive target    | 48dp × 48dp | —                            | 2/4 (when clickable) |
| Text alignment            | Start-aligned (LTR = left) | — | spec frame |
| Headline                  | —         | `title-medium` *or* `title-large` | split — see below |
| Subhead                   | —         | `body-medium`                  | 2/4 |
| Supporting text           | —         | `body-medium`                  | 3/4 |

Typography split: one source maps Headline → `title-large` (older); one
maps → `title-medium` (current Compose). Most likely current M3 is
`title-medium` for header / `body-medium` for subhead + supporting.
Verify before encoding into an Elwha type-role mapping.

### 4.2 Elevation scale (dp ramp)

| Level | dp   |
| ----- | ---- |
| 0     | 0dp  |
| 1     | 1dp  |
| 2     | 3dp  |
| 3     | 6dp  |
| 4     | 8dp  |
| 5     | 12dp |

Elwha paints a hand-rolled multi-layer shadow approximating M3 — should
explicitly anchor each variant + state to one of these levels.

### 4.3 Per-variant elevation state table

Resolved against AndroidX Compose `ElevatedCardTokens.kt`,
`FilledCardTokens.kt`, `OutlinedCardTokens.kt`:

| Variant  | Rest | Hover | Focus | Pressed | Dragged | Disabled |
| -------- | ---- | ----- | ----- | ------- | ------- | -------- |
| Elevated | 1    | 2     | 1     | 1       | **4**   | **1** (shape stays lifted, content fades) |
| Filled   | 0    | 1     | 0     | 0       | 3       | 0        |
| Outlined | 0    | 1     | 0     | 0       | 3       | 0        |

**Resolved earlier disagreements:**

- **Elevated dragged = Level 4 (8dp)** — confirmed against
  `ElevatedCardTokens.DraggedContainerElevation = ElevationTokens.Level4`.
  Only place Level 4 appears in any card token; deliberate Elevated-dragged
  carve-out.
- **Elevated disabled = Level 1 (1dp)** — confirmed against
  `ElevatedCardTokens.DisabledContainerElevation = ElevationTokens.Level1`.
  Shape stays lifted, opacity drops to 0.38.

**Cross-variant rules:**

- **Hover lifts every variant** +1 level (Elevated 1→2, Filled 0→1,
  Outlined 0→1). Not Elevated-only.
- **Dragged lifts every variant**, with Elevated reaching the highest
  level (4 vs 3 for Filled/Outlined).
- **Focus and Pressed never change elevation** for any variant — they
  paint state layer only.

### 4.4 State-layer opacities

All four sources agree exactly (findings §18d):

| State    | Opacity | Token role  |
| -------- | ------- | ----------- |
| Hover    | 0.08    | `on-surface` |
| Focus    | 0.10    | `on-surface` |
| Pressed  | 0.10    | `on-surface` |
| Dragged  | 0.16    | `on-surface` |

### 4.5 Disabled opacities and container-role swap

**Container opacity** (resolved against AndroidX `*CardTokens.kt`):

| Variant  | DisabledContainerOpacity |
| -------- | ------------------------ |
| Elevated | 0.38                     |
| Filled   | 0.38                     |
| Outlined | (no explicit value — Outlined uses outline-opacity 0.12 instead) |

**Correction:** the earlier "two-tier 0.12 container / 0.38 content"
model is not in the card tokens. **All three variants ship a single
container opacity of 0.38.** Content opacity (text, icons, buttons)
is handled by the disabled child components' own disabled tokens
(which also resolve to 0.38), so the net visual effect is uniform 0.38.

**Container role swap on disable** — disabled isn't just an opacity
overlay; the container token *changes role*:

| Variant  | Rest container             | Disabled container          |
| -------- | -------------------------- | --------------------------- |
| Elevated | `surface-container-low`    | **`surface`**               |
| Filled   | `surface-container-highest`| **`surface-variant`**       |
| Outlined | `surface`                  | `surface` (no role change)  |

Elwha implication: `setEnabled(false)` should swap the surface role
*and* apply 0.38 opacity. A single-value opacity wash isn't enough —
the underlying color also changes.

**Outlined disabled** (special case, from `OutlinedCardTokens`):

| Property | Value |
| -------- | ----- |
| `DisabledOutlineColor` | `Outline` (full-strength role) |
| `DisabledOutlineOpacity` | 0.12 |

Stronger outline role at faint opacity — a faded full-strength stroke,
not a weaker role at full strength.

### 4.6 Icon defaults (M3-canonical)

| Property      | Value     | Token                  |
| ------------- | --------- | ---------------------- |
| Default color | primary   | `md.sys.color.primary` |
| Default size  | 24dp      | —                      |

Elwha deliberately diverges on size per `CLAUDE.md`'s "Material Symbols
Rounded / 400 / fill 0 / **20px**" house style. Color stays `primary` as
default.

---

## 5. Interaction and accessibility

### 5.1 Actionability doctrine — hard binary

Direct quote (findings §19):

> A card can be a non-actionable container that holds actions like
> buttons and links, or it can be directly actionable without any
> buttons or links. This is to avoid stacking actionable elements.
> **An action shouldn't be placed on an actionable surface.**

Two sanctioned modes, mutually exclusive:

1. **Non-actionable card with buttons.** Chassis is inert; interaction
   is entirely through nested controls.
2. **Directly actionable card with no buttons.** Entire chassis is the
   click target; no interactive children.

Today `ElwhaCard` supports both modes but does not enforce the
exclusion. Two real composition patterns currently violate the
doctrine: `ElwhaCardList<T>` row selection (chassis becomes actionable
while still holding action buttons), and the Card V2 disclosure
chevron (a click target on an otherwise-clickable card). Worth a
README composition warning, not runtime enforcement.

### 5.2 The actionability quadrad

**Four** visual/behavioral signals all gated on the same actionability
binary (findings §23, §24):

| Signal                            | Actionable card           | Non-actionable card   |
| --------------------------------- | ------------------------- | --------------------- |
| Hand cursor on enter              | yes                       | no                    |
| State-layer hover paint           | yes (`on-surface @ 8%`)   | **no**                |
| Ripple on tap/click               | yes (expanding circle)    | no                    |
| Chassis is a tab stop             | yes (one stop = whole card) | no (only children are stops) |

All four attach or detach together — a single atomic gate, not four
separate switches. Compose's `Card { ... }` vs `Card(onClick = ...) { ... }`
overloads model this distinction at the API level.

### 5.3 Tap ripple

Doctrine (findings §20): *"When a user taps on a directly actionable
card, a touch ripple appears across the card, indicating feedback.
Non-actionable cards don't ripple."*

Ripple is the visible contract for the actionability binary. Compose /
Android M3 cards paint this natively; Swing requires a custom paint —
expanding-circle alpha animation seeded at the click point, clipped to
the card shape.

### 5.4 Keyboard focus / tab traversal

Doctrine (findings §24):

> All interactive elements of cards need a tab stop so they can be
> focused. **Directly actionable cards are tab stops.**
>
> For non-actionable cards, the card itself is not a tab stop. However,
> every actionable element in the card is a tab stop so they're all
> visited before focus navigates to the next card.

| Card mode                  | Tab stops                                       |
| -------------------------- | ----------------------------------------------- |
| Directly actionable        | **One** — the card chassis itself               |
| Non-actionable container   | **N** — one per nested actionable child, in order, then on |

**Focus ring:** 2dp inset `on-surface` outline appears when the chassis
is the focused element (per 3.4; Outlined-variant role swap per 3.5).

**Focus order within a non-actionable card** is reading order —
top-to-bottom, leading-to-trailing.

### 5.5 Screen-reader labels and roles

Doctrine (findings §25):

> The informative contents of a card are verbalized when navigating to
> them using a screen reader. If an image in a card is purely
> decorative, hide it from screen readers. All actionable elements
> must receive both screen reader and keyboard focus.
>
> Directly actionable cards can have the **button** or **link** role,
> depending on how they're used.
>
> Non-actionable cards are purely containers, so they don't need a role.

#### 5.5.1 Card chassis role

| Card mode                                          | Chassis role             |
| -------------------------------------------------- | ------------------------ |
| Directly actionable (acts like a button)           | `button`                 |
| Directly actionable (navigates to a URL/route)     | `link`                   |
| Non-actionable container                           | *(no role — container)*  |

#### 5.5.2 Per-element role table

From the fully-annotated example (findings §25):

| Element             | Label (verbalized)                                    | Role     |
| ------------------- | ----------------------------------------------------- | -------- |
| Headline text       | full headline string                                  | Heading  |
| Media (informative) | alt-text describing the image                         | Image    |
| Supporting text     | full supporting paragraph                             | Text     |
| Primary action      | button label                                          | Button   |
| Secondary action    | button label                                          | Button   |

Traversal order: same as visual reading order (heading → image → body
text → primary button → secondary button).

#### 5.5.3 Decorative-image rule

Media has two sub-roles:

- **Informative media** → `Image` role + alt-text label
- **Decorative media** → hidden from AT entirely (not in traversal)

Consumer needs an API to declare intent — Elwha should expose a
`setMediaDecorative(boolean)` or equivalent.

#### 5.5.4 Swing mapping

| M3 role | Swing `AccessibleRole` (closest) | Notes |
| ------- | -------------------------------- | ----- |
| Heading | *(no direct match)* | Use `LABEL` + name hint; on macOS push `AXRole` via `JComponent.putClientProperty("AXRole", "AXHeading")` |
| Image   | `AccessibleRole.ICON`           | Plus `setAccessibleDescription(altText)` |
| Text    | `AccessibleRole.LABEL`          | Default for `JLabel` |
| Button  | `AccessibleRole.PUSH_BUTTON`    | Default for `JButton` |
| Link    | `AccessibleRole.HYPERLINK`      | For link-acting cards |
| (container) | `AccessibleRole.PANEL`      | Default for non-actionable cards |

### 5.6 Drag-reorder accessibility

Hard doctrine (findings §21):

> To meet Material's accessibility standards, any dragging or swiping
> interactions need a single-pointer alternative, like selecting the
> same actions from a menu.

The canonical pattern: **tap or press-and-hold** on a focused row →
opens a menu (typically bottom-sheet) with the drag-alternative
actions exposed as discrete commands (`↑ Move up`, `↓ Move down`,
optionally Delete).

#### 5.6.1 Menu placement caution

Companion doctrine (findings §22): *"It isn't recommended to place
menus on top of the card on the draggable state. If doing so is
necessary, ensure that the interaction can be completed."*

| Placement                                                | Verdict     |
| -------------------------------------------------------- | ----------- |
| Bottom-sheet sliding up *below* the list                 | Preferred   |
| Side-popover anchored *outside* the row bounds           | Acceptable  |
| Popover overlapping the target row                       | Avoid       |

The active row must remain visible while the menu is open.

#### 5.6.2 Elwha implications

`ElwhaCardList<T>` and `ElwhaChipList<T>` both have drag-handle-only
reorder today — **M3-noncompliant** until a single-pointer alternative
(menu and/or keyboard bindings like Cmd+↑ / Cmd+↓) is added. Likely a
1.0 blocker on a11y grounds. Folds into epic
[OWS-Local-Search-GUI#252](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/252)
(Extend `ElwhaList<T>` selection + drag-reorder surface).

---

## 6. Cross-cutting follow-ups for Elwha

Filing-worthy items, grouped by category. Each should become a GitHub
issue against `OWS-PFMS/elwha`, scoped to a milestone *after* the
current Card V2 stack lands.

### 6.1 API / surface gaps

| # | Item | Source |
| - | ---- | ------ |
| 1 | **Header trailing slot widening** — accept chips alongside icon buttons. Widen `setTrailingActions` to `Component...` or add a parallel chip-typed setter. | 1.5 |
| 2 | **Actions row leading/trailing segments** — express primary leading-left + overflow/icon-buttons trailing-right idiomatically. | 1.4.2, 1.6 |
| 3 | **Leading thumbnail / avatar slot** — distinct from icon-typed `setLeadingIcon`. Accepts both circular avatars and square object thumbnails. Highest-confidence expressivity gap. | 1.3 |
| 4 | **Card orientation** — `CardOrientation.{VERTICAL, HORIZONTAL}` enum or equivalent. Includes orientation-aware action alignment. | 2.2 |
| 5 | **Action row alignment as a setter** — `setActionAlignment(LEADING \| TRAILING)`. M3 sanctions both, pattern-dependent. | 2.3 |
| 6 | **Optional divider slot** — full-width and inset variants. `setDivider(DividerStyle.{FULL, INSET, NONE})`. Probably not pre-1.0. | 1.7 |
| 7 | **Expand affordance — text-link variant** — body-bottom text link paired with full-width divider, in addition to the chevron. | 1.8 |
| 8 | **Expansion overflow strategy** — `setExpansionOverflow(GROW \| SCROLL)`. Default `GROW`; `SCROLL` for carousel / horizontal-row contexts. Soften the current Elwha spec §9 to match. | 2.5 |
| 9 | **Single-pointer alternative for drag-reorder** — menu + keyboard path (move-up / move-down, optionally delete) for both `ElwhaCardList<T>` and `ElwhaChipList<T>`. **Likely 1.0 blocker on a11y grounds.** | 5.6 |
| 10 | **Decorative-media opt-out API** — `setMediaDecorative(true)` marks the media component non-focusable + clears its accessible name. | 5.5.3 |
| 11 | **Atomic actionability gate** — single setter/constructor governs all four signals (cursor / hover paint / ripple / tab stop). Compose-style `onClick`-presence-or-absence model. | 5.2 |
| 12 | **Link-mode actionability** — distinguish button-acting vs link-acting cards. Probably post-1.0 (V3 consideration). | 5.5.1 |
| 13 | **Tap ripple paint** — expanding circular ripple clipped to card shape, wired through the atomic actionability gate. Custom Swing paint. Probably post-1.0. | 5.3 |

### 6.2 Token / default verification

| # | Item | Source |
| - | ---- | ------ |
| 14 | **Default measurement verification** — confirm shape 12dp, padding 16dp, inter-card 8dp, text start-aligned. Single PR covering all four. | 4.1 |
| 15 | **Variant ↔ container-role audit** — verify `Variant.ELEVATED` → `surface-container-low`, `FILLED` → `surface-container-highest`, `OUTLINED` → `surface` + `outline-variant`. | 3.1 |
| 16 | **Elevation-level table verification** — confirm hand-rolled multi-layer shadow per variant + state resolves to one of the M3 canonical dp values (0/1/3/6/8/12). | 4.2, 4.3 |
| 17 | **Headline/subhead/supporting-text role setters** — the card token files do NOT include typography constants (resolved Q3, Appendix B). Don't lock a default. Expose `setHeadlineRole(TypeRole)`, `setSubheadRole(TypeRole)`, `setSupportingTextRole(TypeRole)` so consumers pick the layout-appropriate role (compact card → `title-medium`; hero card → `display-small` or `title-large`). Apply a sensible default for backward compatibility but document that it's just a default. | 4.1 |
| 18 | **Don't implement surface-tint layer** — `surface-container-*` token ramp already covers it; surface-tint is being phased out. | 3.6 |

### 6.3 State paint conformance

| # | Item | Source |
| - | ---- | ------ |
| 19 | **Focus ring conformance** — 2dp inset focus ring per variant. **Color is variant-dependent: `secondary` for Elevated and Filled; `on-surface` (outline role swap) for Outlined.** Verify we're not falling back to OS dotted-line focus or a 1px ring. | 3.4, 3.5 |
| 20 | **Outlined focus-ring role swap** — confirm the focused-state stroke swaps to `on-surface` rather than double-painting an inset ring over the resting `outline-variant`. | 3.5 |
| 21 | **Hover-state elevation lift across all variants** — Filled and Outlined also bump 0 → 1 on hover. Today Elwha probably doesn't lift either. | 4.3 |
| 22 | **Dragged-state elevation lift** — card-list drag interaction must lift the dragged card's shadow per the M3 dragged elevation tier (level 4 for Elevated, level 3 for Filled/Outlined). | 3.4, 4.3 |
| 23 | **Disabled-state chassis paint** — `setEnabled(false)` swaps the surface role AND applies 0.38 opacity. Role swap: Elevated → Surface, Filled → SurfaceVariant, Outlined → no role change. Single-value opacity (NOT two-tier as previously stated). | 4.5 |
| 24 | **Disabled container role swap** — implement the role swap from #23 as a single setter side effect: when `setEnabled(false)`, internally swap the container token, then apply 0.38 opacity wash. Don't expose a separate `setDisabledSurfaceRole` API; doctrine ties the swap to the disabled state. | 4.5 |
| 25 | **Outlined disabled stroke uses `outline` (not `outline-variant`) at 12% opacity** — stronger role token painted faintly. | 3.5 |
| 26 | **Outlined disabled fades both fill + stroke** — verify both, not just one. | 3.5 |

### 6.4 Accessibility

| # | Item | Source |
| - | ---- | ------ |
| 27 | **AccessibleRole on the card chassis** — gated by the atomic actionability gate (#11). Actionable → `PUSH_BUTTON` (or `HYPERLINK`); non-actionable → `PANEL`. | 5.5.1, 5.5.4 |
| 28 | **AccessibleRole + description for internal slots** — push roles on Elwha-owned labels/media: headline as Heading hint (push `AXRole` on macOS), supporting/subhead as `LABEL`, media as `ICON` with `setAccessibleDescription(altText)`. | 5.5.4 |
| 29 | **Focus traversal contract within non-actionable card** — verify leading-segment buttons tab before trailing overflow. | 5.4 |
| 30 | **Headline word-wrap verification** — confirm headline word-wraps and does not ellipsize at narrow widths. | 1.2 |

### 6.5 Documentation

| # | Item | Source |
| - | ---- | ------ |
| 31 | **Actionability doctrine — composition warning in README** — document the hard binary (a card is *either* directly actionable *or* contains actions, not both). Flag implications for `ElwhaCardList<T>` selection + Card V2 disclosure chevron. | 5.1 |
| 32 | **Card-variant ↔ button-variant pairing doc note** — full table: Elevated → Filled, Filled → Outlined, Outlined → Filled tonal. Composition guidance, not enforcement. | 3.3 |
| 33 | **Three sanctioned vertical layouts** — document Media→…, Headline→Subhead→…→Media→Actions (Display small), and Headline→Media→Supporting→Actions. | 2.1 |
| 34 | **Two-tier headline composition note** — Gmail-style nested label blocks are the supported way to express Tier-2 subjects. | 1.2 |
| 35 | **Filled/Outlined/Elevated use-case guidance** — Filled for repetitive content grids (photo galleries / podcast lists); Elevated for breaking up busy layouts; Outlined for understated separation. | 3.1, 5.3 |

---

## 7. Appendices

### Appendix A — What V2 already handles cleanly

Things our V2 already matches M3 on (findings, closing section):

- Token-bound chassis (surface role, shape, elevation) — matches M3
  variant model
- Selection overlay (PRIMARY-filled circle + ON_PRIMARY check) — M3
  checked-icon convention
- Variant set (Elevated / Outlined / Filled) — direct match
- Cubic Bezier media corner clip — exact match to surface corner
- Carousel-readiness contract for grid reflow (parent layout)

### Appendix B — Source confidence and open questions

**High-confidence** (multiple independent corroborating sources +
ground-truthed against AndroidX Compose Material3 token sources at
`androidx-main/compose/material3/material3/src/commonMain/kotlin/
androidx/compose/material3/tokens/{Elevated,Filled,Outlined}CardTokens.kt`):

- The four canonical measurements (12dp / 16dp / 8dp / start-aligned)
- State-layer opacities (4/4 sources + spec frame)
- Surface-role-per-variant rest mapping (3.1)
- **Surface-role-per-variant DISABLED mapping** (4.5) — disabled
  swaps the container role (Elevated → Surface; Filled → SurfaceVariant)
- Per-variant elevation state table (4.3) — every cell anchored to a
  Compose token constant
- Outlined focus-ring role swap (3.5) — token-explicit confirmation
- **Elevated focus ring color = `secondary`** (3.4 correction) — was
  previously claimed as `on-surface`
- Surface-tint NOT applied to cards (3.6) — confirmed by absence
- Actionability quadrad (four independent spec frames in the
  Accessibility tab all gate on the same binary)

**Resolved earlier open questions:**

- Elevated dragged elevation = **Level 4 (8dp)** ✅ — confirmed
- Elevated disabled elevation = **Level 1 (1dp), opacity 0.38** ✅ —
  shape stays lifted, content fades
- Surface-tint deprecated for cards ✅ — confirmed by absence in tokens

**Genuinely under-specified / out of scope of tokens:**

- **Headline typography role** — card tokens do NOT include
  typography constants. M3 Card is a container; text-role is set by
  consumer composables, layout-dependent. No canonical default
  exists. Elwha should expose a `setHeadlineRole(TypeRole)` setter
  rather than locking a default.
- **Link vs button actionability visual treatment** — Compose models
  this at the API level (`Card { }` vs `Card(onClick = ...) { }`)
  and the button/link distinction is purely an accessibility-role
  concern set by the consumer at the semantics layer. Visually
  identical chassis.

**Substantive corrections folded in (this revision):**

- 3.4 — focus indicator color is `secondary` for Elevated/Filled
  (not `on-surface` as previously claimed)
- 4.3 — every elevation cell now anchored to a Compose token
- 4.5 — disabled is single-value 0.38 (not two-tier 0.12/0.38) AND
  the container role swaps (not just opacity fade)
- 3.6 — surface-tint deprecation upgraded from "lower-confidence" to
  "confirmed"

### Appendix C — Relationship to other Elwha docs

- [`elwha-card-v2-spec.md`](elwha-card-v2-spec.md) — the in-progress
  Card V2 spec. Items in §6 above feed back as either spec revisions
  (e.g., 2.5 softens current spec §9) or post-V2 follow-ups.
- [`elwha-extraction-decisions.md`](elwha-extraction-decisions.md) /
  [`elwha-coupling-audit.md`](elwha-coupling-audit.md) — extraction
  rationale; don't re-litigate.
- [`m3-card-spec-findings.md`](m3-card-spec-findings.md) — raw
  chronological walkthrough notes. This doc is the topical reorganization.
