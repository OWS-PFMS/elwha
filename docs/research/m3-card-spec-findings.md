# M3 Card spec — observations from the spec walkthrough

Working notes from a screenshot-led walk through
[m3.material.io/components/cards/specs](https://m3.material.io/components/cards/specs) and
adjacent in-the-wild references (Gmail). Used as input for
[`elwha-card-v2-spec.md`](elwha-card-v2-spec.md) revisions and follow-up
issues against `OWS-PFMS/elwha`.

Living doc — add as we go. Source screenshots live under
[`card-examples/`](card-examples/) where saved; most M3 spec frames were
viewed inline (TCC blocks save from `NSIRD_screencaptureui_*` temp paths).

## 1. Anatomy

M3 names **six** formal card elements:

1. Container
2. Image
3. Button
4. Supporting text
5. Subhead
6. Headline

That's it. **Leading icon, header-trailing icon buttons, overflow menu,
expand affordance** are all *patterns you compose with*, not parts of
the primitive. Our Elwha "OWS extension slots" (`setLeadingIcon`,
`setLeadingActions`, `setTrailingActions`, disclosure axis) are
extensions on top of M3, not deviations from it — keep that framing in
docs.

## 2. Layout / content blocks

Two named layouts surfaced:

- **Display small** (anatomy diagram): Headline → Subhead → Supporting
  text → **Media** → Actions. Media is *below* the text block.
- **Media-on-top** (the more familiar layout): Media → Headline →
  Subhead → Supporting text → Actions.

Implication: **media placement is layout-dependent, not a fixed slot
order.** Our current implementation only expresses media-on-top. Doc
should acknowledge media-below as a sanctioned variant, even if we don't
expose a layout enum yet.

## 3. Orientation — vertical vs horizontal

M3 explicitly shows the *same card content* rendered in two orientations:

- **Vertical** — media on top, content stacked below, action
  bottom-**leading**
- **Horizontal** — media on the left full card height, content stacked
  in a right column, action bottom-**trailing** of the right column

Caption from spec: *"Example of the same card with two different
orientations and element positioning."*

Action alignment is orientation-dependent:

| Orientation | Action alignment |
| ----------- | ---------------- |
| Vertical    | bottom-leading   |
| Horizontal  | bottom-trailing  |

Elwha Card today is vertical-only. This is a real expressivity gap.

## 4. Text vocabulary

Concrete usage definitions from the spec:

- **Headline** — subject of the card (photo album name, article title)
- **Subhead** — smaller (byline, tagged location, timestamp)
- **Supporting text** — body content (article summary, restaurant
  description)

**Two-tier text pattern** (observed in two Gmail-style spec frames):
the same card renders text in two distinct tiers:

- **Tier 1** — header row beside the leading thumbnail: identity
  (Daniel Maas) + timestamp (Yesterday). Headline = title-medium,
  subhead = label-medium / on-surface-variant.
- **Tier 2** — body row below the thumbnail, spanning full card width:
  subject (Clay pot fair on Saturday?) + supporting text preview
  ("I think it's time…"). Subject is styled stronger than supporting
  text — closer to a secondary headline (title-small / body-large with
  medium weight).

M3 doesn't formally name "Tier 2", but the spec renders this pattern
twice in conversation-style cards. Our current headline/subhead are
single-pass; consumers would nest a `JLabel` block to express the
Tier-2 subject. Worth documenting as a sanctioned composition.

**Headline reflow:** the visual-presentation spec shows the same
headline string rendering on 2 lines (narrower card) or 1 line (wider
card) without truncation. Confirms headline should **word-wrap**, not
ellipsize, at narrow widths. Worth a quick verify.

## 5. Media types

Three sub-kinds named:

- **Thumbnail** — avatar or logo (small, often leading)
- **Image** — photos, illustrations, weather icons (typically hero-sized)
- **Video** — image plate + play overlay + duration label

**Leading thumbnail is a first-class slot, not an escape hatch.**
Observed across multiple frames (Caminante card, Daniel Maas /
Ana Russo email cards) as a recurring layout primitive — the
headline/subhead column anchors to the thumbnail, and the body wraps
below it spanning full card width.

Today Elwha's `setLeadingIcon` is icon-typed (`MaterialIcons` /
`FlatSVGIcon`) and can't naturally accept a circular photographic
avatar. This is the highest-confidence expressivity gap in the V2 API.
Recommend a parallel `setLeadingThumbnail(Image | Icon)` setter (or a
unified `setLeading(Leading)` sealed variant) before 1.0.

Video overlay (play button + duration) is purely a consumer-paint
concern — no API change needed.

## 6. Actions area

Sanctioned action-area component types per spec text:

- **Buttons** — "Learn more", "Add to cart"
- **Icon buttons** — "Save", "Heart", "Leave a 4-star review"
- **Selection controls** — **chips, sliders, checkboxes**
- **Linked text** — inline link inside supporting text

### Button styles observed

All buttons in card examples are **full-shape pill**. Two M3 fill styles
appear, paired idiomatically:

- **Filled button** — primary fill, on-primary label. Deep saturated
  color. The dominant primary action.
- **Filled tonal button** — primary-container fill, on-primary-container
  label. Softer / muted color. Used when the action is important but
  not the single dominant CTA (e.g., paired actions like
  "Escucha / Ahorrar"), or in dense surfaces where a full-primary fill
  would be visually heavy.
- **Outlined button** — used as the secondary partner to a single
  filled-primary action ("Action / Action" anatomy frame).

A card never mixes more than two button styles in one action row.

### Action row is fully optional

The Daniel Maas / Ana Russo conversation cards carry **no action row
at all** — interaction is the card itself (click → open) plus the
trailing star icon button in the header. Confirms the action row is
fully optional, not just collapsible-empty. Our V2 supports this today
(don't call `setActions`), but worth documenting explicitly.

### Concrete patterns observed

- Row of 5 star icon buttons as a rating affordance
- Choice-chip group (showtimes) as the primary interaction, paired with
  a single outlined button
- Slider + transport icon buttons (audio player)
- **Split row, four component types** (the Caminante card — canonical
  example): two filled-tonal buttons leading-left
  ("Escucha / Ahorrar"), two standard icon buttons trailing-right
  (phone / message). Demonstrates leading/trailing segments inside
  a single action row.

### Implications

- Keeping `setActions(Component...)` open (not narrowed to button types)
  is correct — chips, sliders, custom controls are all valid here.
- **Actions row needs leading/trailing segments** to express the
  primary-leading + overflow/icon-buttons-trailing convention. The
  Caminante card is the canonical example: today our row is one flat
  sequence and can't reproduce it.
- **Action row alignment is pattern-dependent, not a single
  convention.** Both bottom-leading and bottom-trailing are sanctioned
  by M3:
  - **Bottom-leading** — lone promo/discovery action ("Buy tickets" /
    "Get tickets" / "Expand" text link)
  - **Bottom-trailing** — paired actions ("Action / Action" anatomy
    frame, padding-spec card with a single trailing action)
  Don't pick one as the default doctrinally — let the consumer set it.
- Document the **filled / filled-tonal / outlined** vocabulary in the
  Card README so consumers pick the right style for the slot.

## 7. Header trailing slot

The Caminante card carries **two outlined assist chips with leading
icons** (♡ Preferido / 📅 Ayudar) in the header trailing slot — not
icon buttons.

In other examples the header trailing slot carries an **overflow icon
button** (⋮, standard / no container). So the slot is polymorphic across
at least: **assist chip, standard icon button, overflow menu trigger.**

We typed `setTrailingActions(ElwhaIconButton...)` on Card V2. M3 says
chips are also valid there. Either widen back to `Component...` or
expose a parallel chip-typed setter. Decision pending — flag for
discussion before 1.0.

## 8. Overflow menu

Doctrine line: *"Overflow menus contain related actions. They are
typically placed in the upper-right or lower-right corner of a card."*

Two sanctioned positions:

- **Upper-right** — header trailing slot (Elwha supports today via
  `setTrailingActions`)
- **Lower-right** — trailing edge of the action row (Elwha does
  *not* express this distinctly; a consumer can stuff it into
  `setActions(...)` but it lays out alongside the primary buttons,
  not pinned trailing)

Reinforces the "actions row needs trailing segment" finding above.

## 9. Dividers

Spec shows two divider treatments inside a card:

- **Full-width divider** — spans the card edge-to-edge. Used to separate
  the body from an expand affordance. Example pairs the divider with a
  text-link "Expand" affordance below it.
- **Inset divider** — narrower (respects content padding). Used to
  separate related content blocks within the body (e.g., metadata /
  comments footer row).

Elwha Card has no first-class divider concept. Consumers could
`add(JSeparator)` manually. Worth considering as a follow-up — possibly
`setDivider(DividerStyle.FULL | INSET | NONE)` — but probably not
pre-1.0.

## 10. Expand affordance

Two sanctioned patterns:

- **Chevron icon button** — header trailing (Elwha's current
  implementation)
- **Text link** — body-bottom, paired with a full-width divider above
  it ("Expand" link styled as a primary-color link)

Elwha only exposes the chevron. Text-link variant is a real gap if we
want to express the divider+link pattern.

## 11. Adaptive / responsive design

Two transformations sanctioned by spec:

- **Grid reflow** at the parent level — 2-column grid (mobile) →
  single horizontal row (tablet/desktop). This is the container's
  responsibility, not the card's. Our carousel-readiness contract
  already covers this.
- **Internal orientation flip** — same card rendered vertical at
  narrow widths, horizontal at wide widths. This is a Card-level
  concern and ties back to §3 (orientation gap).

## 12. Scrolling on desktop — contradicts our current spec §9



Spec doctrine: *"On a desktop device, card content can expand and
scroll within a card."*

Spec example: a horizontal row of three coffee-shop cards. Expanding
one card grows its internal scroll region; sibling cards **do not
reflow or resize**.

Our `elwha-card-v2-spec.md` §9 currently says: *"Card never installs
internal scroll. Expansion grows the card; sibling layout reacts as
the parent's LayoutManager allows."*

M3 sanctions both behaviors; the choice is **contextual**:

| Container layout                  | Recommended expansion behavior |
| --------------------------------- | ------------------------------ |
| Vertical list / loose grid        | Grow + reflow (current Elwha)  |
| Carousel / horizontal row / fixed grid | Internal scroll (sibling stays put) |

Implication: **soften the §9 stance** and add an opt-in:
`setExpansionOverflow(GROW | SCROLL)`.

## 13. Hard measurements — the M3 spec table

The "Card padding and size measurements" spec frame gives canonical
numbers (first time we've had them written out, not inferred):

| Attribute              | Value           |
| ---------------------- | --------------- |
| Shape                  | 12dp corner radius |
| Left/right padding     | 16dp            |
| Padding between cards  | 8dp max         |
| Label text alignment   | Start-aligned   |

Implications for Elwha defaults — each one needs verification against
our current resolution:

1. **Shape default**: M3 = 12dp. Our `ShapeScale.MD` (Card's default)
   should resolve to 12dp. If `MD ≠ 12`, change the default or the
   token resolution.
2. **Content padding default**: M3 = 16dp horizontal. We default to
   `SpaceScale.LG` on both axes — verify `LG` = 16dp. If `LG` is
   larger (24dp etc.), we're padding more aggressively than M3 says,
   and the README should be corrected.
3. **Inter-card gap**: M3 = 8dp max. `ElwhaItemList<T>` gap default
   should be `SpaceScale.SM` (8dp).
4. **Text alignment**: start-aligned (LTR = left). Verify the
   headline / subhead / supporting-text labels don't center-align by
   accident.

These four verifications form a discrete checkable task before 1.0 —
likely a single PR.

**Update — direct view of the Measurements frame:** the diagram shows a
**horizontal-orientation card** with media on the left and text on the
right. The action button — a **filled-tonal pill labeled "Action"** —
sits at the **bottom-leading edge of the media column**, not the
bottom-trailing of the text column. This adds a third sanctioned
action-placement pattern beyond §3's vertical/horizontal split:
**action can live in the media column itself, anchored bottom-leading,
on horizontal cards.** Reinforces follow-up #5: action alignment is
genuinely pattern-dependent and the consumer needs to control it; we
shouldn't pick a single default for horizontal mode either.

The "12" callout in the diagram points at the corner-radius (matches
the table row); the "8" callout points at the inter-card gap below.
The diagram also shows the media subject as **three geometric shapes
(triangle / square / circle)** — M3's generic "media placeholder"
glyph, used throughout the spec when the actual photo content isn't
the point.

## 14. Visual presentation / spacing

Doctrine line: *"To adjust the presentation of content-focused
components, begin with spacing. Allow components like lists, cards,
and images to optimize space while filling the region of a screen
that suits a device breakpoint's ergonomic needs."*

Observation from the side-by-side wider/narrower example: **media size
stays constant** as the card narrows; only the text region reflows.
So width-responsiveness lives in the text wrappers, not in scaling
the media. Our `setSupportingText(String)` HTML-wrapping handles this
correctly.

## 15. Elevated card variant — spec frames

Walking the Elevated-card spec page directly. M3 documents anatomy,
color-role, and interaction-state for each variant separately; this
section captures Elevated. Filled and Outlined will get parallel
sections.

### 15a. Elevated — anatomy callout

Anatomy frame calls out **only one element by number**: `(1) Container`.
The example card content:

- Headline: "Play relaxing songs" (2-line wrap — confirms §4 wrap
  behavior on a primary frame)
- Subhead: "From your recent favorites"
- Action row: single **filled button** "Get started", bottom-trailing
  alignment

Components present but not numbered in this frame: headline, subhead,
button. M3's choice not to number them here implies the variant doc is
*only* about the container chassis — text + actions are covered by the
shared anatomy doc (§1).

**Action alignment note:** this is a bottom-trailing single-action card,
not bottom-leading. Reinforces §6's "alignment is pattern-dependent"
finding — even the canonical variant exemplar uses trailing for a single
filled-primary action. So "bottom-trailing for single primary CTA"
joins "bottom-trailing for paired actions" as a sanctioned pattern.

### 15b. Elevated — color roles

Single role called out for the whole variant: **(1) Surface container
low** — applies in both light and dark themes (same token, theme-aware
resolution).

The frame also shows the canonical card template M3 uses for all three
variant color frames:

- Leading **avatar/thumbnail** (small circle, labelled "A") in the
  upper-left of the card body
- Header trailing slot carries a small **icon button** (gear/settings
  icon visible) — standard / no container
- Below header: **Title** (headline) + **Subhead** (subhead) stacked
- **Supporting text** body block below
- No action row in this frame

Confirms again (§5, §7) that **leading thumbnail + trailing standard
icon button** are the canonical card composition M3 reaches for when
showing the chassis in isolation. Reinforces the leading-thumbnail-slot
gap.

**Token mapping for Elwha:** Elevated → `ColorRole.SURFACE_CONTAINER_LOW`
on the container fill. Verify our `ElwhaCard.Variant.ELEVATED` resolves
to this exact role, not `SURFACE` or `SURFACE_CONTAINER`. (Currently
under audit — flag in §13's measurement-verification PR or split out.)

### 15c. Elevated — interaction states

Five sanctioned states, shown in light + dark theme strips:

1. **Hovered** — container picks up `on-surface @ 8%` state layer over
   the surface-container-low fill. Elevation appears unchanged from rest
   in the frame.
2. **Focused** — **2dp outline ring** in `on-surface` (visible as a
   crisp dark/light border). State layer also present (`on-surface @
   10%` per M3 conventions). Ring is *inside* the card bounds, not an
   outer glow.
3. **Pressed** — `on-surface @ 10%` state layer; ripple visible as a
   soft radial highlight on the surface (partial-coverage circular
   gradient). No elevation change shown.
4. **Dragged** — **elevation lifts** (visibly larger drop shadow than
   rest), state layer `on-surface @ 16%`. Cursor shown as grab cursor.
   This is the only state where elevation changes.
5. **Disabled** — container fades: fill is `on-surface @ 12%`-derived
   tint, text/iconography fades further. No shadow / no state layer.
   The card looks flat and washed-out.

**State layer convention:** M3 standard percentages
(hover 8% / focus 10% / press 10% / drag 16%) painted in `on-surface`
over the variant's base fill. Disabled is a separate token, not a state
layer.

**Focus ring spec:** 2dp `on-surface` outline, inset. Distinct from the
Outlined variant's resting outline (which is `outline` role, not
`on-surface`). Don't confuse the two.

**Implications for Elwha:**

- We render hover/press/focus today via FlatLaf's defaults; verify the
  visual matches M3's state-layer percentages over our resolved
  container token. Likely close but worth a side-by-side.
- **Dragged-state elevation lift** is a real spec — Elwha's drag-reorder
  (in the card-list family) should lift the dragged card's shadow, not
  just move it. Check current behavior.
- **Focus ring at 2dp inset on-surface** is the M3-correct treatment.
  Verify our focus paint isn't relying on the OS-default focus dotted
  line or a 1px ring.
- **Disabled-state token** — confirm we have a disabled paint at all on
  the card chassis. Today Elwha cards are usually always-enabled; if
  a consumer sets `setEnabled(false)`, the chassis should fade per
  this spec, not just disable children.

## 16. Filled card variant — spec frames

Parallel to §15. Filled-card anatomy, color, states.

### 16a. Filled — anatomy callout

Same single callout pattern as Elevated: **(1) Container** is the only
numbered element. Same example content (Play relaxing songs / From
your recent favorites / "Get started" button).

**Action-button style differs from Elevated:**

- Elevated frame paired its chassis with a **filled button**
  (primary-fill "Get started", on-primary label).
- Filled frame pairs its chassis with an **outlined button**
  (transparent fill, outline ring, primary-color "Get started" label).

Reading: M3 deliberately swaps button style by variant to avoid stacked
filled surfaces — a filled chassis already carries fill weight, so the
primary action de-escalates to outlined. This is a *composition
pattern*, not a hard rule (consumers can still put a filled button on a
filled card), but worth surfacing as guidance.

**Implication for Elwha docs:** add a "pairing" note in the Card README
recommending button-variant choice per card variant:
- ELEVATED → filled button OK as primary CTA
- FILLED → outlined button preferred for primary CTA
- OUTLINED → TBD (next frame)

### 16b. Filled — color roles

Single role: **(1) Surface container highest** — both light and dark
themes, same theme-aware token.

Compare to Elevated:
| Variant  | Container role |
| -------- | -------------- |
| Elevated | `surface-container-low` |
| Filled   | `surface-container-highest` |

Filled is the **darkest/lightest surface tier in the surface-container
ramp**, intentionally — it leans on tonal contrast rather than elevation
shadow to read as a distinct surface. Elevated leans on shadow rather
than tone.

Same canonical card template as Elevated: leading avatar "A", trailing
gear icon button, Title + Subhead + Supporting text stacked. No action
row in the color frame.

**Token mapping for Elwha:** Filled → `ColorRole.SURFACE_CONTAINER_HIGHEST`.
Audit `ElwhaCard.Variant.FILLED` resolution; folds into follow-up #12.

### 16c. Filled — interaction states

Same five states as Elevated, same state-layer convention. Visual
differences from Elevated:

1. **Hovered** — `on-surface @ 8%` over filled chassis. Subtle (less
   contrast than over Elevated's lighter surface).
2. **Focused** — 2dp inset `on-surface` outline ring, same as Elevated.
3. **Pressed** — `on-surface @ 10%` state layer + ripple. Same paint
   over the darker fill.
4. **Dragged** — **elevation lift here too** — Filled cards also lift
   when dragged, even though their resting state has no shadow. M3
   reads dragged as elevation-bearing across all variants.
5. **Disabled** — chassis fades; `on-surface @ 12%`-derived tint over
   nothing (no rest-state shadow to lose).

**Cross-variant observation:** the state-layer percentages and focus-ring
treatment are **identical across Elevated and Filled**. Only the
base-fill token differs. So our Elwha state-layer code should not need
per-variant branching — same paint, applied over whatever
`SURFACE_CONTAINER_*` token the variant resolves to. Confirms the M3
state-layer model is variant-agnostic for the chassis.

## 17. Outlined card variant — spec frames

Parallel to §15 / §16. Outlined is the **first variant to break the
single-callout pattern** — it has two numbered chassis parts.

### 17a. Outlined — anatomy callout

**Two numbered elements**:

1. **Container** — the fill surface
2. **Outline** — the stroke around the container, called out as a
   distinct chassis part

Same example content (Play / relaxing songs / From your recent
favorites / "Get started" button).

**Action button style differs from both Elevated and Filled:**

- Elevated → filled button (primary fill, on-primary label)
- Filled → outlined button (transparent, outline ring, primary label)
- **Outlined → filled-tonal button** (primary-container fill,
  on-primary-container label) — the soft purple pill in this frame

This completes the variant ↔ button pairing pattern:

| Card variant | Paired primary-CTA button style |
| ------------ | ------------------------------- |
| Elevated     | Filled                          |
| Filled       | Outlined                        |
| Outlined     | Filled tonal                    |

Reading: M3 deliberately rotates button style across variants so the
button never *visually duplicates* the chassis treatment. Filled chassis
+ filled button would stack solid fills; outlined chassis + outlined
button would stack rings; elevated chassis + outlined/tonal would
under-claim. The pairings are tonal-contrast choices, not arbitrary.

Update follow-up #16 to capture the full table.

### 17b. Outlined — color roles

**Two roles** (matches the two anatomy callouts):

1. **(1) Surface** — container fill
2. **(2) Outline variant** — outline stroke

Same theme-aware tokens across light and dark.

Compare to the other variants:

| Variant  | Container role            | Outline role     |
| -------- | ------------------------- | ---------------- |
| Elevated | `surface-container-low`   | — (no outline)   |
| Filled   | `surface-container-highest` | — (no outline) |
| Outlined | `surface`                 | `outline-variant` |

Notes:
- Outlined uses **plain `surface`**, the lowest-tier surface token —
  it leans entirely on the outline stroke for surface separation, not
  tone. Elevated and Filled lean on `surface-container-*` tiers.
- The outline role is **`outline-variant`** (subdued), not `outline`
  (full-strength). Suggests the resting-state stroke is intentionally
  soft, with the brighter `outline` role reserved for focus/active.

Same canonical template (avatar A / trailing gear / Title + Subhead +
Supporting text).

**Token mapping for Elwha:** Outlined → `ColorRole.SURFACE` fill +
`ColorRole.OUTLINE_VARIANT` stroke at the resting state. Audit
`ElwhaCard.Variant.OUTLINED` resolution; folds into follow-up #12.

### 17c. Outlined — interaction states

Same five states. Visual differences specific to Outlined:

1. **Hovered** — `on-surface @ 8%` state layer over `surface` fill;
   outline-variant stroke unchanged.
2. **Focused** — focus ring reads **brighter and thicker** than
   Elevated/Filled focus rings. In this frame the focused stroke
   appears to be **`outline` (full strength)** or **`primary`** — a
   color shift from the resting `outline-variant`. M3 convention: focus
   on Outlined replaces the resting outline with a stronger stroke
   rather than adding an inset ring inside the existing outline (which
   would double-stroke). **Worth verifying the exact role** — could be
   `on-surface` at 2dp like the other variants, or it could be `primary`
   for visual prominence on the otherwise-bare surface.
3. **Pressed** — state layer over surface; outline-variant stroke
   remains visible underneath.
4. **Dragged** — **elevation lift + outline visually softens / gets
   overwhelmed by shadow.** The dragged Outlined card reads almost like
   a lifted Filled card — the resting stroke matters less when the
   shadow takes over the surface-separation job. Consistent
   cross-variant: dragged is the elevation-bearing state regardless of
   resting chassis.
5. **Disabled** — outline fades to a disabled-tinted stroke; surface
   fill fades to disabled-tinted fill. The chassis essentially mutes
   both parts (outline + fill), not just one.

**Cross-variant state-layer model holds:** same hover/press/drag
percentages, painted over each variant's resting paint. Outlined is the
only variant where the focus state visibly transforms the chassis
(stroke role swap) rather than overlaying onto it. That's an extra
paint concern for the Outlined variant specifically.

**Implications for Elwha:**

- Verify our Outlined-variant focused state swaps the outline role to a
  stronger token, doesn't just paint a second ring on top of the
  resting one. (Likely a regression risk — easy to over-paint.)
- Dragged-state elevation lift applies to Outlined too — same
  follow-up #14.
- Disabled state on Outlined must fade **both** outline and fill, not
  just one.

## 18. Canonical numeric / token table — from cross-LLM spec dump

The m3.material.io spec page is JS-rendered and isn't directly scrapable.
On 2026-05-17 the operator collected M3 Card spec dumps from four
different LLMs and pasted them inline. This section captures the values
that **three or more of the four sheets agree on** (treated as
high-confidence canonical), and flags the disagreements explicitly.
Source: conversation paste; reconstructed in those tools from Angular
Material token files + Material Components Android. Re-verify against
`material-components-android` source before any 1.0 release decision.

### 18a. Shared shape / spacing / typography

| Property                  | Value     | Token                          | Confidence |
| ------------------------- | --------- | ------------------------------ | ---------- |
| Corner radius             | 12dp      | `md.sys.shape.corner.medium`   | 4/4        |
| Internal padding (all sides) | 16dp   | —                              | 3/4 (one omits) |
| Inter-card stack spacing  | 8dp       | —                              | 3/4 |
| Min interactive target    | 48dp × 48dp | —                            | 2/4 (when clickable) |
| Header / Title            | —         | `title-medium` *or* `title-large` | split |
| Subhead                   | —         | `body-medium`                  | 2/4 |
| Supporting text           | —         | `body-medium`                  | 3/4 |

Typography split: one sheet maps Title→`title-large` (the older mapping);
another maps Title→`title-medium` (current Compose). Most likely current
M3 is `title-medium` for header / `body-medium` for subhead + supporting,
but verify before encoding into an Elwha type role mapping.

### 18b. Elevation scale (dp)

From the most token-explicit sheet:

| Level | dp   |
| ----- | ---- |
| 0     | 0dp  |
| 1     | 1dp  |
| 2     | 3dp  |
| 3     | 6dp  |
| 4     | 8dp  |
| 5     | 12dp |

This is the M3 elevation ramp Elwha should resolve `ShapeScale`-adjacent
elevation tokens against (or whatever equivalent we expose). Today Elwha
paints a hand-rolled multi-layer shadow approximating M3 — we should
explicitly anchor it to one of these levels per variant + per state.

### 18c. Per-variant elevation state table

Levels per state for each variant (3+/4 agreement unless flagged):

| Variant  | Rest | Hover | Focus | Pressed | Dragged | Disabled |
| -------- | ---- | ----- | ----- | ------- | ------- | -------- |
| Elevated | 1    | 2     | 1     | 1       | **4** ⚠ | 0 *or* 1 ⚠ |
| Filled   | 0    | 1     | 0     | 0       | 3       | 0        |
| Outlined | 0    | 1     | 0     | 0       | 3       | 0        |

**Disagreements / nuances:**

- **Elevated dragged**: 3 of 4 sheets say level 4 (8dp); 1 says level 3
  (6dp). Likely level 4 is correct (it's the *only* place level 4 is
  used in any sheet, suggesting it's specifically the Elevated-dragged
  carve-out).
- **Elevated disabled**: most sheets say level 0; one says level 1 with
  container_opacity 0.38. The token-explicit sheet keeps elevation but
  fades the surface — likely M3-correct behavior (the shape stays lifted
  while content fades).
- **Hover lifts Filled and Outlined too** (rest 0 → hover 1) — I missed
  this in §16c and §17c when I wrote "only dragged changes elevation."
  **Correction: hover bumps every variant up one level.** That's a
  cross-variant consistency in M3, not an Elevated-only behavior.

### 18d. State layer opacities (confirmed)

All four sheets agree exactly — confirms what I wrote in §15c:

| State    | Opacity | Token role  |
| -------- | ------- | ----------- |
| Hover    | 0.08    | `on-surface` |
| Focus    | 0.10    | `on-surface` |
| Pressed  | 0.10    | `on-surface` |
| Dragged  | 0.16    | `on-surface` |

State-layer paint is variant-agnostic. Confirms §16c finding that Elwha
state-layer code does not need per-variant branching.

### 18e. Disabled opacities

| Target | Opacity | Notes |
| ------ | ------- | ----- |
| Container | 0.12 | M3 spec doctrine line in one sheet: "Container drops to 12% total" |
| Content (text + icons) | 0.38 | Standard M3 disabled-content opacity |

One sheet collapses both to 0.38 (likely simplified). The two-tier
0.12/0.38 model is the M3-correct treatment — container fades more
aggressively than content text. Worth mirroring in Elwha's disabled
paint.

### 18f. Outlined-variant per-state outline color (resolves §17c open question)

The token-explicit sheet enumerates outline color per state. Confirms my
§17c hypothesis about focus role-swap:

| State    | Outline color           |
| -------- | ----------------------- |
| Rest     | `outline-variant`       |
| Hover    | `outline-variant` (unchanged) |
| **Focus**| **`on-surface`** ⬅ role swap |
| Pressed  | `outline-variant` (unchanged) |
| Dragged  | `outline-variant` (unchanged) |
| Disabled | `outline` (full strength) at 12% opacity |

So the **focus ring on Outlined is `on-surface`, not `primary`** — same
role as the focus ring on Elevated and Filled. The difference is that
Outlined *replaces* its existing stroke rather than overlaying an inset
ring (which would double-stroke). Settles §17c's "could be primary"
caveat.

Disabled note: the disabled stroke uses **`outline`** (the stronger
role) but at 0.12 opacity — net visual is a faded full-strength outline
rather than a stronger-tone one. Subtle but specific.

### 18g. Surface-tint layer

Two sheets mention `md.sys.color.surface-tint` as a per-variant token,
applied as a tint layer over the container fill at elevated states.
One sheet flags it as **phased out in M3 Expressive** (set to
null/transparent).

Stance for Elwha: **don't implement surface-tint.** It's a transitional
M3-2021 mechanism that the design system has moved away from. Our
multi-tier `surface-container-*` token already encodes the
tone-by-elevation relationship without needing a tint layer overlay.

### 18h. Icon defaults (when card carries an icon)

| Property      | Value     | Token                  |
| ------------- | --------- | ---------------------- |
| Default color | primary   | `md.sys.color.primary` |
| Default size  | 24dp      | —                      |

Note: this is M3's spec default; Elwha follows §CLAUDE.md's
"Material Symbols Rounded / 400 / fill 0 / 20px" house style, so we
deliberately diverge on size. Color stays `primary` as default.

### 18i. Compose API surface (for reference)

Three sheets enumerate the Compose Material3 components:

- `Card` (Filled — the default)
- `ElevatedCard`
- `OutlinedCard`

Per-variant `CardDefaults` helpers:
`cardColors()` / `elevatedCardColors()` / `outlinedCardColors()`, and
parallel `cardElevation()` / `elevatedCardElevation()` /
`outlinedCardElevation()`.

Reference only — Elwha is Swing, not Compose. But the
**three-companion-helper-per-variant** pattern is worth noting: Compose
separates color + elevation + (optionally) border into distinct helper
objects rather than overloading the constructor. Our Elwha API does the
latter (a single `ElwhaCard.elevated(...)` factory) — viable, but worth
checking whether per-axis defaults objects would help consumers express
"variant defaults except override X" without hand-threading.

## 19. Interaction & style — actionability doctrine

The Guidelines tab "Interaction & style" frame establishes a **hard
binary** for card actionability. Direct quote:

> A card can be a non-actionable container that holds actions like
> buttons and links, or it can be directly actionable without any
> buttons or links. This is to avoid stacking actionable elements.
> **An action shouldn't be placed on an actionable surface.**

Two sanctioned patterns, illustrated side by side:

1. **Non-actionable card with buttons.** The card chassis itself does
   not respond to clicks; interaction is entirely through nested
   controls. The example carries:
   - headline / media / supporting-text body
   - **action row** with three components in two segments:
     - leading: **filled button** "Get tickets" + **outlined button**
       "Learn more" (paired-actions idiom)
     - trailing: **standard overflow icon button** "⋮"
2. **Directly actionable card with no buttons.** The entire card
   surface is the click target. No buttons, no icon buttons, no links
   inside. The example shows only headline + media + supporting text.

### Why this matters for Elwha

Today `ElwhaCard` supports both modes — `setActions(...)` populates the
nested-controls case, and the card itself can be made clickable. **M3
doctrine says these are mutually exclusive.** We don't enforce that.

This affects two real Elwha patterns:

- **`ElwhaCardList<T>` selection** — when the parent list makes whole
  rows selectable (click-to-select), the cards become actionable
  surfaces. Any nested action button inside those cards violates M3
  doctrine.
- **Card V2 disclosure / expand chevron** — the chevron is itself a
  click target on what's typically also a clickable card.

**Stance to consider documenting:** flag this in the Card README and/or
ElwhaCardList README as a composition warning. Not an enforcement (M3
is convention, not a runtime rule), but consumers should know they're
deviating when they nest actions inside selectable rows.

### Additional layout observation

The non-actionable example uses a layout pattern §2 didn't enumerate:
**Headline (top) → Media (middle) → Supporting text → Actions**. So
media-placement is not just "top vs below text" — headline can also sit
*above* the media plate. Updates §2: at least three sanctioned vertical
layouts now:

- Media → Headline → Subhead → Supporting → Actions (media-on-top)
- Headline → Subhead → Supporting → Media → Actions (Display small)
- **Headline → Media → Supporting → Actions** (new — observed here)

Reinforces §2's underlying point: M3 doesn't lock a slot order;
media's vertical position is a layout choice, not a fixed anatomy
position.

### Overflow position confirmed

The non-actionable example's overflow icon button sits in the
**action row trailing edge**, not in the header. Concrete confirmation
of §8's "lower-right overflow position" — and another example for
follow-up #2's "actions row needs leading/trailing segments" (the row
literally has both).

## 20. Touch — ripple as the actionability signal

Direct quote from the Touch frame:

> When a user taps on a directly actionable card, a touch ripple
> appears across the card, indicating feedback.
> **Non-actionable cards don't ripple.**

So **ripple presence is the visible contract for §19's actionability
binary**. Tap a card → expanding circular ripple across the full
chassis → it's a click target. No ripple → it isn't.

Behavioral split:

| Card actionability | Tap behavior |
| ------------------ | ------------ |
| Directly actionable | Full-chassis ripple |
| Non-actionable container | No ripple on chassis (nested controls still ripple individually) |

### Example layout in the frame

A **Podcasts grid** of Filled-variant cards (4 visible: 2 + 2
partial). Each tile:

- Media on top (square hero image — podcast cover art)
- Headline below media ("Gamer Course", "The Breakdown")
- Subhead ("79 episodes", "58 episodes")
- No action row

This is the canonical **repetitive-content-grid use case** — exactly
what the variant guidance recommends Filled cards for. Each tile is
directly actionable (tap → open podcast detail), so each would ripple
on tap.

Outside the cards: a circular play/pause FAB sits bottom-right of the
screen — that's a screen-level control, not part of any card.

### Implications for Elwha

- We paint state-layer overlays (hover / press) on the chassis today,
  but **don't paint an expanding circular ripple**. Compose / Android
  M3 cards do. For Swing, this would be a custom paint:
  expanding-circle alpha animation seeded at the click point, clipped
  to the card shape.
- If we adopt ripple, it becomes the **enforcement mechanism** for
  §19's doctrine: setting the card clickable → wires the ripple paint;
  not clickable → no ripple. Consumers get the M3-correct visual
  contract for free.
- This is a real but bounded chunk of work — clip-to-shape + animation
  loop. Probably not pre-1.0 but worth filing.

## 21. Accessibility — dragging and dismissing

First frame from the Accessibility tab. Hard doctrine:

> To meet Material's accessibility standards, any dragging or swiping
> interactions need a single-pointer alternative, like selecting the
> same actions from a menu.
>
> For example, tapping a card, or pressing and holding, should open a
> menu to change its position in a list. That menu could also contain
> an action to delete the card.

### Canonical pattern in the example

"Edit card order" screen — list of four payment cards:

- Each row is a **horizontal Outlined card** containing:
  - Leading: small colored card thumbnail (square, ~40dp)
  - Two-line text block: name (`Nimbus Financial`) + masked number
    (`···· 4322`)
  - Optional inline supplementary text after the number
    (`Tap to set up`, `Suspended`)
  - Trailing: **drag handle icon** (two horizontal bars, std icon
    button sized)
- One row is **focused/selected** (Ascent Credit — picked up an
  outlined focus ring + state-layer-tinted fill)
- A **bottom-sheet menu** has slid up containing the drag-alternative
  actions:
  - `↑ Move up`
  - `↓ Move down`

The bottom-sheet appears after **tap or press-and-hold** on a focused
row. That sheet is the single-pointer alternative path; the drag handle
remains available for users who can drag.

### Implications for Elwha

This is the most consequential a11y finding so far. Maps directly to:

- **`ElwhaCardList<T>` drag-reorder** today: drag-handle-only.
  Single-pointer alternative is **not implemented**. That makes our
  drag-reorder non-conformant with M3 a11y doctrine.
- **`ElwhaChipList<T>` drag-reorder** (in `MovementMode`): same gap.
- **Epic OWS-Local-Search-GUI#252** (Extend `ElwhaList<T>` selection +
  drag-reorder surface) should make a single-pointer-alternative API
  part of the contract, not an afterthought. Probably some surface
  along the lines of `setReorderMenu(...)` exposing the same
  move-up / move-down actions as a menu, paired with keyboard
  bindings (Cmd/Ctrl+↑ / Cmd/Ctrl+↓ for keyboard reorder).
- **Delete-action piggyback**: the M3 doctrine specifically calls out
  that the same menu can carry destructive actions (delete). If Elwha
  exposes a drag-to-dismiss pattern later, the menu should expose
  delete too.

Filing-worthy: a single follow-up that says "drag-reorder requires a
single-pointer-alternative menu / keyboard path" applied to both card
and chip families. Probably blocks 1.0 on a11y grounds.

### Other observations

- The thumbnail in this layout is **a square (not a circle)** —
  recurring use case for the leading-thumbnail slot (§5) where the
  thumbnail represents an *object* (card art, app icon, document
  cover), not a *person* (avatar). Reinforces follow-up #3 — the slot
  needs to accept both square images and circular avatars, not just
  one shape.
- The trailing drag handle is rendered as a **standard icon button**
  (no container) — folds into §7's "header trailing slot is
  polymorphic" observation, except this is trailing in a *row*
  context, not a header. Same icon-button treatment.
- Inline supplementary text after the masked number (`Tap to set up`,
  `Suspended`) is a one-line metadata pattern not previously
  documented — sub-supporting-text at the trailing edge of the
  primary identifier line. Probably consumer-paint, not a primitive
  slot.

## 22. Accessibility — menu placement caution (don't occlude the card)

Companion to §21. Doctrine:

> It isn't recommended to place menus on top of the card on the
> draggable state. If doing so is necessary, ensure that the
> interaction can be completed.

Caption on the frame: **"Caution — Ensure that the menu doesn't cover
the card."**

### The anti-pattern (what the frame shows)

Same Edit-card-order list as §21. The Ascent Credit row is in
dragged/active state. The Move up / Move down menu has been rendered
as a **popover anchored to the right of the row**, partially
overlapping the row — covering the drag handle and bleeding over the
masked number. The user can no longer fully see the target of the menu
they're operating on.

### Implication for Elwha

When implementing the single-pointer-alternative menu from §21 /
follow-up #28, the menu's positioning matters:

- **Preferred:** bottom-sheet that slides up *below* the list (the
  §21 frame's "good" pattern). The active row stays fully visible.
- **Acceptable:** side-popover that anchors *outside* the row bounds.
- **Avoid:** popover overlapping the target row, even partially. If
  unavoidable (constrained screen space), at minimum ensure the
  primary identifier of the row (name / first line) remains visible.

This is a *positioning constraint on the menu*, not a new component
requirement — folds into follow-up #28's scope rather than adding a
new follow-up. Worth one inline note in the eventual API design.

The §21 + §22 pairing is the classic "do / don't" spec pattern —
together they bound the design space for the drag-alternative menu.

## 23. Accessibility — cursor + hover state (third leg of actionability triad)

A11y frame "Cursor". Two doctrine lines:

> When a directly actionable card is hovered, the hover state provides
> a visual cue to the person that the element is interactive.
> **Non-actionable cards don't have a hover state.**
>
> When a directly actionable card is clicked, a ripple appears,
> providing feedback.

Caption: **"Cursor: Hover, Click."**

### Actionability triad — now complete

Three independent visual signals all gated on the same actionability
binary from §19:

| Signal | Actionable card | Non-actionable card |
| ------ | --------------- | ------------------- |
| Hand cursor on enter | yes | no |
| State-layer hover paint (§18d) | yes (`on-surface @ 8%`) | **no** |
| Ripple on tap/click (§20) | yes (expanding circle) | no |

Reading: hover paint must be **gated on actionability**, not always-on.
Non-actionable cards in M3 don't reveal as interactive on hover — they
sit visually inert. That's part of the doctrine, not a paint
implementation detail.

### Concrete example in the frame

Gmail-style email list (Spanish UI strings — `Bandeja de entrada`,
`Borradores`, etc. in the nav rail). Two list rows visible, both
horizontal Filled cards:

- **Carmen Villanueva row** — currently hovered (pointer cursor visible
  at lower-right of the card). Picked up a subtle state-layer tint.
- **Ana Russo row** — at rest below.

Both rows demonstrate the **Gmail two-tier headline pattern** from §4
verbatim:

- Tier 1 (header row beside leading thumbnail): identity + timestamp
  (`Carmen Villanueva` / `Yesterday`)
- Tier 2 (body row below): subject (`Graduación de Inés`) + supporting
  text preview (`Hola hijo mío, aquí tienes unas fotos preciosas de
  Inés en su graduación la semana pasada. Estoy muy....`)

Each row carries a **trailing standard icon button** (☆ star, no
container) in the top-right of the header row. The pointer cursor is
the macOS hand cursor (`Cursor.HAND_CURSOR` equivalent).

### Implications for Elwha

- **Cursor change on actionable cards**: when `setClickable(true)` (or
  equivalent), card should install `Cursor.HAND_CURSOR` on enter and
  restore on exit. Verify current behavior — likely missing for the
  bare chassis, even if list-row clicks work.
- **Hover-paint gating**: the chassis state-layer hover paint must be
  *conditional on actionability*. If Elwha paints hover on
  always-non-clickable cards today, that's a doctrine violation.
  Compose's Card distinguishes the `Card { ... }` vs
  `Card(onClick = ...) { ... }` overloads exactly for this — hover and
  ripple only attach to the `onClick` form.
- The bare existence of `setClickable(false)` (or no click handler)
  should suppress all three signals: cursor / hover paint / ripple.
  Atomic gate, not three separate switches.

### Confirms earlier findings

- Two-tier headline pattern (§4) shows up *again* — that's now four
  distinct Gmail-style frames in this walkthrough using it. The
  "sanctioned composition" framing in §4 is well-supported.
- Trailing standard icon button in header row (§7) — same idiom yet
  again. The header trailing slot polymorphism (chip / icon button /
  overflow) is real and recurring.

## 24. Accessibility — focus / tab traversal

A11y frame "Focus." Doctrine:

> All interactive elements of cards need a tab stop so they can be
> focused. **Directly actionable cards are tab stops.**
>
> For non-actionable cards, the card itself is not a tab stop. However,
> every actionable element in the card is a tab stop so they're all
> visited before focus navigates to the next card.

Caption hint at bottom of frame: *"Use Tab to navigate through all
buttons in a card."*

### Tab-stop rule per actionability mode

| Card mode | Tab stops |
| --------- | --------- |
| Directly actionable | **One** — the card chassis itself |
| Non-actionable container | **N** — one per nested actionable child, traversed in order, then on to the next card |

So for a non-actionable card with action row `[Get tickets] [Learn
more] [⋮]`, tab order hits **three** stops within that card before
moving on. For a directly actionable card with the same visual
controls, the children would have to be focusable too (the M3 doctrine
is silent on this edge — but in practice, the "no actions on actionable
surfaces" rule from §19 means this combination shouldn't exist).

### Actionability quadrad — updating §23

The triad in §23 is actually a **quadrad** — four signals gated on the
same actionability binary:

| Signal | Actionable card | Non-actionable card |
| ------ | --------------- | ------------------- |
| Hand cursor on enter | yes | no |
| State-layer hover paint | yes (`on-surface @ 8%`) | no |
| Ripple on tap/click | yes (expanding circle) | no |
| **Chassis is a tab stop** | **yes (one stop = whole card)** | **no (only children are stops)** |

All four signals attach or detach together. This strengthens
follow-up #30 (atomic actionability gate) — the gate must also
control focus traversal.

### Example layout in the frame

Horizontal carousel of three cards titled "Live music coming soon to
The Hideout" (and a partial "Many Wond... Cryptocurrencies" to the
right). Each card is a **Filled variant**, media-on-top layout, with
the canonical filled+outlined+overflow action row:

- Leading: **filled button** "Get tickets"
- Next: **outlined button** "Learn more"
- Trailing: **standard overflow icon button** "⋮"

Middle card carries the focused-state target. A `[Tab]` chip floats
below with a (1) callout pointing at it, signaling that Tab steps
through the actionable elements.

### Implications for Elwha (Swing)

- **Focusability gating**: when a card is set actionable, the chassis
  must be focusable (`setFocusable(true)`) AND its actionable children
  must NOT be additional tab stops (`setFocusable(false)` on the row's
  nested buttons, or they should be unwrapped from focus traversal).
  When non-actionable, the chassis must NOT be focusable but every
  nested actionable child must be.
- **Focus order within a non-actionable card** is reading order
  (leading-to-trailing, top-to-bottom). Swing's default focus traversal
  policy usually gets this right, but worth verifying for the
  leading/trailing action segments (follow-up #2) — leading buttons
  should tab before trailing overflow.
- **Visible focus ring on the chassis** (§15c / §18f) is what users see
  for the tab-stop-on-chassis case. The 2dp inset `on-surface` ring
  (resolved per-variant) should appear when the chassis is the focused
  element.

### Confirms again

- The filled + outlined + overflow action row idiom (§19) — appears
  here too, now five frames in.
- Horizontal-carousel layout context (§12) — the example is explicitly
  a horizontal row of sibling cards.

## 25. Accessibility — labeling elements (screen-reader contract)

A11y frame "Labeling elements." This is the most specific a11y frame so
far — it walks a complete card and assigns every element a **Label**
and a **Role** for screen-reader exposure.

### Doctrine

> The informative contents of a card are verbalized when navigating to
> them using a screen reader. If an image in a card is purely
> decorative, hide it from screen readers. All actionable elements
> must receive both screen reader and keyboard focus.
>
> Directly actionable cards can have the **button** or **link** role,
> depending on how they're used.
>
> Non-actionable cards are purely containers, so they don't need a role.

So **the card chassis itself takes one of two roles**:

| Card mode | Chassis role |
| --------- | ------------ |
| Directly actionable (acts like a button) | `button` |
| Directly actionable (navigates to a URL/route) | `link` |
| Non-actionable container | *(no role — plain container)* |

### Per-element role table (from the fully-annotated example)

The same "Live music coming soon to The Hideout" card from §24 with
**every internal element labeled with both its accessible Label and
its Role**:

| Element | Label (verbalized) | Role |
| ------- | ------------------ | ---- |
| Headline text | "Live music coming soon to The Hideout" | **Heading** |
| Media (informative) | "Drummer singing into a microphone while playing the drums." *(alt text)* | **Image** |
| Supporting text | "Watch exclusive live performances at The Hideout every Saturday starting at 7pm." | **Text** |
| Primary action | "Get tickets" | **Button** |
| Secondary action | "Learn more" | **Button** |

### Traversal order (verbalized in this order)

A separate accompanying frame numbers the traversal sequence on a
non-actionable card explicitly:

1. Heading
2. Image
3. Body text
4. Primary button
5. Secondary button

Reading: **screen-reader traversal mirrors visual reading order**
(top-to-bottom, leading-to-trailing), and **non-actionable card
children are all visited individually** — consistent with §24's
keyboard tab-stop rule. Same N-stops-per-card model for AT users as
for keyboard users.

### Decorative-image rule

> If an image in a card is purely decorative, hide it from screen
> readers.

This means media has two sub-roles:

- **Informative media** → `Image` role + alt-text label
- **Decorative media** → hidden from AT entirely (not in the traversal
  list)

This is the standard `aria-hidden`-equivalent rule, but applied
specifically at the card level: a card's hero image might be either
depending on whether it's content or chrome.

### Implications for Elwha (Swing accessibility)

Swing exposes a11y through `AccessibleContext` + `AccessibleRole`.
Mapping the M3 role table to Swing:

| M3 role | Swing `AccessibleRole` (closest) | Notes |
| ------- | ------------------------------- | ----- |
| Heading | *(no direct match)* | Swing has no `HEADING`. Use `LABEL` + name hint; on macOS, can push `NSAccessibilityHeadingRoleAttribute` via java-access-bridge or `JComponent.putClientProperty("AXRole", ...)` |
| Image   | `AccessibleRole.ICON`           | Plus `setAccessibleDescription(altText)` |
| Text    | `AccessibleRole.LABEL`          | Default for `JLabel` |
| Button  | `AccessibleRole.PUSH_BUTTON`    | Default for `JButton` |
| Link    | `AccessibleRole.HYPERLINK`      | For actionable-as-link card |
| (container) | `AccessibleRole.PANEL`      | Default for `JPanel`; non-actionable cards |

**Card chassis role gating** — extends follow-up #30's atomic
actionability gate:

- Actionable mode: `card.getAccessibleContext().setAccessibleRole(PUSH_BUTTON)` (or `HYPERLINK` for link semantics)
- Non-actionable mode: leave as `PANEL` (default)

**Internal-slot role assignments** — Elwha should set roles for the
labels it owns:

- Headline label → push a heading-role hint (platform-dependent)
- Subhead label → `LABEL` (default ok)
- Supporting-text label → `LABEL` (default ok)
- Media component (when present) → `ICON` + accessible description

**Decorative media flag** — needs an API for the consumer to declare
intent:

- Default: media is informative; if no alt provided, log warning or
  use empty string with a note
- Opt-out: `setMediaDecorative(true)` → mark the media component not
  focusable + clear its accessible name → effectively hidden from AT

**Link-mode actionability** — today Elwha cards are either clickable
(button-semantic) or not. M3 distinguishes button-acting vs
link-acting cards. Probably overkill pre-1.0; flag as a possible V3
consideration.

### Confirms again

Same example card as §24 — fifth+ time the filled+outlined action row
shows up, and a fresh confirmation that **headline / image /
supporting / two buttons** is M3's canonical non-actionable card
content shape.

## Cross-cutting follow-up list (for Elwha)

Filing-worthy items surfaced during this walkthrough. Each should
become a GitHub issue against `OWS-PFMS/elwha`, scoped to a milestone
*after* the current Card V2 stack lands.

1. **Header trailing slot widening** — accept chips alongside icon
   buttons. Decision: widen `setTrailingActions` back to `Component...`
   or add a parallel chip-typed setter. (§7)
2. **Actions row leading/trailing segments** — express primary-actions
   leading-left + overflow/icon-buttons trailing-right idiomatically.
   (§6, §8)
3. **Leading thumbnail / avatar slot** — distinct from icon-typed
   `setLeadingIcon`. Accepts a circular photographic `Image`. Observed
   across multiple frames (Caminante, Daniel Maas, Ana Russo) as a
   recurring layout primitive — highest-confidence expressivity gap in
   the V2 API. Recommend `setLeadingThumbnail(...)` parallel to
   `setLeadingIcon`, or a unified leading-slot sealed type. (§5)
4. **Card orientation** — vertical (default) vs horizontal layout
   mode. Includes orientation-aware action alignment. Probably
   `CardOrientation` enum or `setOrientation(VERTICAL | HORIZONTAL)`.
   (§3)
5. **Action row alignment as a setter** — expose
   `setActionAlignment(LEADING | TRAILING)` (or default LEADING but
   allow override). M3 sanctions both, pattern-dependent. (§6)
6. **Headline word-wrap verification** — confirm headline doesn't
   truncate/ellipsize when card narrows. (§4)
7. **Optional divider slot** — full-width and inset variants. (§9)
8. **Expand affordance — text-link variant** — in addition to the
   chevron icon button, support a body-bottom text link paired with a
   full-width divider. (§10)
9. **Expansion overflow strategy** — `setExpansionOverflow(GROW |
   SCROLL)`. Default `GROW` (current). `SCROLL` for carousel /
   horizontal row contexts. Soften spec §9 to match. (§12)
10. **Double headline/subhead composition note** — document that
    nested label blocks are the supported way to express Gmail-style
    two-tier headers. (§4)
11. **Default measurement verification** — confirm the four canonical
    M3 numbers (shape 12dp, padding 16dp, inter-card 8dp, text
    start-aligned) match our token resolutions. Single PR. (§13)
12. **Variant ↔ container-role audit** — verify
    `Variant.ELEVATED` → `SURFACE_CONTAINER_LOW` (M3-correct), plus
    parallel checks for Filled / Outlined as those frames land. (§15b)
13. **Focus ring conformance** — 2dp inset `on-surface` outline on
    Elevated (matching ring per variant for Filled/Outlined TBD).
    Verify we're not falling back to the OS dotted-line focus paint or
    a 1px ring. (§15c)
14. **Dragged-state elevation lift** — confirm the card-list drag
    interaction lifts the dragged card's shadow (M3 dragged state is
    the only state with an elevation change). (§15c)
15. **Disabled-state chassis paint** — confirm `setEnabled(false)` on
    a card fades the chassis (fill + text + iconography), not just its
    children. (§15c)
16. **Card-variant ↔ button-variant pairing doc note** — add a README
    note recommending button styles per card variant. Full table:
    Elevated → Filled, Filled → Outlined, Outlined → Filled tonal.
    Composition guidance, not enforcement. (§16a, §17a)
17. **Outlined focus-ring role audit** — confirm the focused-state
    stroke on Outlined swaps the outline token (likely to `outline` or
    `primary`) rather than double-painting an inset ring over the
    resting `outline-variant`. (§17c)
18. **Outlined disabled fades both fill + stroke** — verify
    `setEnabled(false)` on an Outlined card mutes the outline alongside
    the fill, not just one. (§17c)
19. **Hover-state elevation lift across all variants** — Filled and
    Outlined also bump from level 0 → level 1 on hover. Today Elwha
    probably doesn't lift either of these on hover. Verify and add.
    Supersedes the §15c-implied "only dragged changes elevation"
    framing. (§18c)
20. **Two-tier disabled opacity** — disabled container at 12%, disabled
    text/icons at 38%. Verify our chassis-disable code applies both
    tiers, not a single 38% wash. (§18e)
21. **Outlined disabled stroke uses `outline` (not `outline-variant`)
    at 12% opacity** — a stronger role token painted faintly, not the
    weaker role at full strength. Verify the resolution. (§18f)
22. **Don't implement surface-tint layer** — it's a transitional M3
    mechanism being phased out in M3 Expressive; our
    `surface-container-*` token ramp already covers the
    elevation-tone relationship. (§18g)
23. **Elevation-level table verification** — confirm our hand-rolled
    multi-layer shadow at each card variant resolves to one of the M3
    canonical dp values (0/1/3/6/8/12) rather than a free-floating
    value. (§18b)
24. **Typography role mapping verification** — pick `title-medium` (or
    `title-large`) for headline, `body-medium` for subhead + supporting
    text; resolve the spec-source split before encoding into a Card
    type-role default. (§18a)
25. **Actionability doctrine — composition warning in README** —
    document M3's hard rule that a card is *either* directly actionable
    *or* contains actions, not both ("don't stack actionable
    elements"). Flag the implications for `ElwhaCardList<T>` selection
    + Card V2 disclosure chevron when the chassis is also clickable.
    Not enforcement, but consumer-facing guidance. (§19)
26. **Third sanctioned vertical layout** — Headline → Media →
    Supporting → Actions (headline above media plate). Update §2
    coverage and the README's layout examples. (§19)
27. **Tap ripple as actionability signal** — paint an M3-style
    expanding circular ripple, clipped to the card shape, on tap of
    a directly actionable card. Wired automatically when the card is
    set clickable; absent otherwise. Becomes the visible enforcement
    of §19's doctrine. Probably post-1.0. (§20)
28. **Single-pointer alternative for drag-reorder** — drag-handle-only
    is M3-noncompliant. Add a menu and/or keyboard path
    (move-up / move-down, optionally delete) to both
    `ElwhaCardList<T>` and `ElwhaChipList<T>`. Should be part of
    epic OWS-Local-Search-GUI#252's contract, not bolted on after.
    Likely a 1.0 blocker on a11y grounds. (§21)
29. **Leading-thumbnail slot must accept square shapes too** —
    not just circular avatars. Object-thumbnail use case (card art,
    app icon, doc cover) is recurring. Refines follow-up #3. (§21)
30. **Atomic actionability gate** — when a card is non-actionable, all
    **four** signals (hand cursor, hover state-layer paint, tap ripple,
    chassis-as-tab-stop) must be suppressed together. When actionable,
    all four attach (and nested children become non-tab-stops). Single
    setter or constructor distinction (à la Compose `Card { }` vs
    `Card(onClick = …) { }`) should govern all four. (§23, §24)
31. **Focus traversal contract** — verify focus order within a
    non-actionable card (leading-segment buttons tab before trailing
    overflow), and verify the focused-chassis paint (2dp inset
    `on-surface` ring per variant) actually appears when the card is
    the focused element. (§24)
32. **AccessibleRole on the card chassis** — gated on actionability
    (same atomic gate as #30). Actionable → `PUSH_BUTTON` (or
    `HYPERLINK` if link-acting); non-actionable → `PANEL` default. (§25)
33. **AccessibleRole + description for internal slots** — push roles
    on the labels/media Elwha owns: headline as Heading hint (push
    AXRole on macOS), supporting/subhead as `LABEL`, media as `ICON`
    with `setAccessibleDescription(altText)`. (§25)
34. **Decorative-media opt-out API** — `setMediaDecorative(true)` (or
    equivalent) marks the media component non-focusable and clears its
    accessible name so AT skips it. Default treats media as
    informative. (§25)

## What did *not* surface as a gap

Things our V2 already handles cleanly:

- Token-bound chassis (surface role, shape, elevation) — matches M3
  variant model
- Selection overlay (PRIMARY-filled circle + ON_PRIMARY check) — M3
  checked-icon convention
- Variant set (Elevated / Outlined / Filled) — direct match
- Cubic Bezier media corner clip — exact match to surface corner
- Carousel-readiness contract for grid reflow (parent layout) — covered
