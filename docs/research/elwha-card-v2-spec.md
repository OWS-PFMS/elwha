# ElwhaCard V2 — Spec

**Status:** LOCKED for v0.1 build. This doc fixes the V2 API surface, slot vocabulary, variant table, disclosure semantics, and migration map. Source-code implementation is story #65; playground migration is story #66.

**Drafted:** 2026-05-16

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — §9 "build only when raw Swing + tokens can't express the need."
- [`elwha-surface-design.md`](elwha-surface-design.md) — Card V2 composes / extends `ElwhaSurface` for token-bound paint.
- [`elwha-flatchip-rebuild.md`](elwha-flatchip-rebuild.md) — established the token-foundation rebuild pattern Card V2 follows.
- [`../development/component-api-conventions.md`](../development/component-api-conventions.md) — five locked doctrine rules (#62) every V2 surface must match.

**Epic:** [#63](https://github.com/OWS-PFMS/elwha/issues/63) — `ElwhaCard V2 — M3-aligned rebuild + disclosure axis + extensions`.

**Origin:** `OWS-PFMS/OWS-Local-Search-GUI#253` (pre-extraction). Card V1 accumulated five escape-hatches (`setSurfaceColor(Color)`, `setKeepSummaryWhenExpanded`, the three-arg `setHeader(...)` overload that quietly clears `leadingIcon`, raw `JLabel` getters `getTitleLabel()` / `getSubtitleLabel()`, and the absence of token-typed setters); V2 retires all five by starting clean rather than patching.

---

## TL;DR

1. **Base:** `ElwhaCard extends ElwhaSurface` — surface paint, border paint, shape, and the four token-typed setters (`setSurfaceRole` / `setShape` / `setBorderRole` / `setBorderWidth`) are inherited, never re-implemented.
2. **Variants:** `ELEVATED` (default) / `FILLED` / `OUTLINED`. `GHOST` and `WARM_ACCENT` are gone. Each variant carries its M3 surface role + default elevation + dragged elevation + default border treatment.
3. **Slot vocabulary:** M3 formal anatomy (`headline` / `subhead` / `supportingText` / `media` / `actions`) + four documented OWS extensions (`leadingIcon`, `leadingActions`, `trailingActions`, disclosure axis).
4. **Disclosure:** `setCollapsible(boolean)` / `setCollapsed(boolean)` / `setSummary(JComponent)` / `setSummaryVisibility(SummaryVisibility)` / `setAnimateCollapse(boolean)`. `SummaryVisibility = { COLLAPSED_ONLY (default), ALWAYS }`.
5. **Selection:** keep `setSelected`; render M3 "checked" icon overlay in the top-trailing corner (not tint-only).
6. **Doctrine:** matches all five #62 rules — `getX()`-only getters, per-variant static factories (`elevatedCard(String headline)` etc.), single-arg convenience ctor `(String headline)`, no `setBorderRole` override (Card is variant-bearing — inherits one but does not advertise it), symmetric border-width getter/setter (inherited).
7. **Carousel-readiness:** cooperates with externally-imposed width, exposes a mask-friendly shape paint, **never** installs internal scroll. Expansion grows the card; sibling layout reacts as the parent's `LayoutManager` allows.

---

## 1. The §9 bar

Design direction §9: *"Build a component only when raw Swing + tokens can't express what you need."*

Card V1 already shipped; V2 is a rebuild, not a "build". But §9 still applies — the question is whether the **disclosure axis + OWS extension slots** justify keeping Card as a primitive once the M3 formal anatomy is also stripped to its essentials.

Answer: yes. Three concrete reasons:

1. **Disclosure isn't a separate primitive.** M3 Cards spec treats expansion as a layout pattern on the Card itself, not a separable `Disclosure` widget. Path A (extract Disclosure as a sibling primitive sharing Surface) was ruled out in #63 planning — it would invent a non-M3 split, and the real duplication between a hypothetical Card + Disclosure (both extending Surface, sharing header geometry, both carrying variants and interaction modes) confirms the split would be invented complexity.
2. **The OWS extension slots are load-bearing.** `leadingIcon`, `leadingActions`, `trailingActions`, and the summary axis are how OWS-Local-Search-GUI's cycle / mode cards present themselves (multi-action toolbars adjacent to a headline, summary text that survives both expanded and collapsed states). Hand-rolling these per consumer would re-introduce the V1-era boilerplate this repo exists to retire.
3. **Token-bound chassis can't be derived from Surface alone.** Surface paints a rounded role-filled rectangle. The Card-specific concerns (slot geometry, elevation→shadow, dragged-elevation handling on interaction, the checked-icon overlay) are paint-layer + layout-layer additions that belong above Surface, not in every consumer.

V2 is the smallest Card that retires V1's escape hatches and exposes the M3 slot vocabulary as typed setters.

## 2. Header geometry

Header strip — single row, flexible center, fixed leading/trailing edges. The chevron (when `collapsible == true`) is rendered as a `MaterialIcons` glyph in the rightmost slot; its rotation tracks `isCollapsed()`.

```
┌────────────────────────────────────────────────────────────────────────┐
│  ┌────┐  ┌───┐ ┌───┐   ┌──────────────────┐   ┌───┐ ┌───┐  ┌─────────┐ │
│  │ LI │  │ A1│ │ A2│   │  HEADLINE        │   │ T1│ │ T2│  │   ▼/▶   │ │
│  └────┘  └───┘ └───┘   │  Subhead         │   └───┘ └───┘  └─────────┘ │
│                        └──────────────────┘                            │
│   ↑           ↑              ↑                  ↑           ↑          │
│   leading     leading        headline +         trailing    chevron    │
│   icon        actions        subhead block      actions     (collapse) │
└────────────────────────────────────────────────────────────────────────┘
```

**Rules.**
- `leadingIcon` and the chevron are single-glyph slots — width = icon container, no internal growth.
- `leadingActions` and `trailingActions` are `Component...` slots, laid out flush with the icon row. Empty arrays render nothing (no reserved gutter).
- `headline + subhead` is the only flexible slot; consumes remaining horizontal space.
- Vertical centering applies within the row; all glyphs and action components share a baseline.
- Padding is the Card's `setPadding(SpaceScale h, SpaceScale v)` value, applied as the row's outer inset; gaps between siblings are a fixed `SpaceScale.SM`.

Below the header, optional in this order: `media` (full-bleed component, sized by parent), `supportingText` (multi-line text), `actions` row (button group, right-aligned by default), then the disclosure body (visible when `!isCollapsed()`).

## 3. Variant table

| Variant | Surface role | Default elevation | Dragged elevation | Default border |
|---|---|---|---|---|
| `ELEVATED` (default) | `SURFACE_CONTAINER_LOW` | 1 dp | 2 dp | none |
| `FILLED` | `SURFACE_CONTAINER_HIGHEST` | 0 dp | 8 dp | none |
| `OUTLINED` | `SURFACE` | 0 dp | 8 dp | `OUTLINE_VARIANT`, 1 px |

**Elevation.** Stored as an `int` 0–5 (the V1 `MAX_ELEVATION` range carries forward). Paint composites a soft drop-shadow whose alpha + spread scale with the value. The `setElevation(int)` API is kept; default is variant-derived per the table above.

**Dragged elevation.** While the card is participating in a drag operation (signaled by `ElwhaCardList`'s reorder machinery — V2 keeps the same `setBeingDragged(boolean)` hook V1 has), elevation temporarily bumps to the dragged value, then restores on drop. No new public API; the existing internal toggle is preserved.

**Foreground.** Resolved as the `on`-pair of the variant's surface role (`ON_SURFACE` for ELEVATED + OUTLINED, `ON_SURFACE` for FILLED's highest container). Always correct by construction; never stored on the variant.

**Border-role override.** Per #62 §4, Card does not expose `setBorderRole(ColorRole)` even though `ElwhaSurface` does. Card is variant-bearing — the border role is variant-derived. To opt into a different border, change the variant. (The setter is inherited from Surface but is **not advertised** in Card's javadoc as part of the public surface; it remains callable for now because pre-1.0 we don't add visibility-hiding shims.)

## 4. Slot table

Every public slot setter, marked **M3 formal** (drawn from Material 3 Cards spec) or **OWS extension** (Elwha-specific, justified per row).

| Setter | Type | M3? | Justification (extensions only) |
|---|---|---|---|
| `setHeadline(String)` | M3 formal | ✓ | — |
| `setSubhead(String)` | M3 formal | ✓ | — |
| `setSupportingText(String)` | M3 formal | ✓ | — |
| `setMedia(JComponent)` | M3 formal | ✓ | — |
| `setActions(Component...)` | M3 formal | ✓ | M3 "actions" footer row — buttons right-aligned by default. |
| `setLeadingIcon(Icon)` | **OWS extension** | — | Cycle / mode cards in OWS-Local-Search-GUI carry a single-glyph type indicator left of the headline (e.g., reinforcing-cycle glyph). Could be hand-rolled in `setMedia` but `media` is a full-bleed block above the header, not an inline header glyph. Single-glyph slot keeps the header row intact when media is also present. |
| `setLeadingActions(Component...)` | **OWS extension** | — | OWS pin / anchor / favorite toolbars sit on the header's leading edge, adjacent to the leading icon. M3 only specifies a trailing actions row; OWS layouts split actions by purpose (state toggles leading, navigation trailing). Solves V1's "third hack" — V1 had no leading-actions slot at all. |
| `setTrailingActions(Component...)` | **OWS extension** | — | OWS navigation / overflow buttons sit right of the headline, distinct from the bottom-row M3 `actions`. Without this, consumers stuff trailing controls into the headline string or invent footer rows. |
| `setCollapsible(boolean)` | **OWS extension** | — | M3 doesn't formalize a disclosure axis on Card. OWS lists frequently render long-form cards that need to fold to a summary line for scanability. Path B-extended (#63) chose to keep this on Card rather than extract a Disclosure primitive. |
| `setCollapsed(boolean)` | **OWS extension** | — | Companion to `setCollapsible`. |
| `setSummary(JComponent)` | **OWS extension** | — | The component rendered in place of the body when collapsed (or always, per `SummaryVisibility`). A custom component, not a String, because OWS summaries are often chip rows / metric rollups, not plain text. |
| `setSummaryVisibility(SummaryVisibility)` | **OWS extension** | — | Two values: `COLLAPSED_ONLY` (M3-style — collapse hides body, shows summary) and `ALWAYS` (summary is a persistent header band, visible whether expanded or collapsed). Resolves V1's `setKeepSummaryWhenExpanded(boolean)` escape hatch — V1 modeled this as a boolean override on top of a default; V2 makes the visibility model a first-class enum. |
| `setAnimateCollapse(boolean)` | **OWS extension** | — | Toggles the height-tween on `setCollapsed`. Off by default in test contexts; on by default in interactive UIs. Carries forward from V1. |
| `setSelected(boolean)` | M3 formal | ✓ | M3 Card spec defines a selected state. |
| `isSelected()` | M3 formal | ✓ | — |
| `setVariant(CardVariant)` | M3 formal | ✓ | — |
| `setInteractionMode(CardInteractionMode)` | **OWS extension** | — | M3 doesn't axis-separate Card interaction; the V1 axis (`STATIC` / `HOVERABLE` / `CLICKABLE` / `SELECTABLE`) is OWS sugar over Swing's mouse listener boilerplate. Keep — it's the same pattern Chip and IconButton use. |
| `setElevation(int)` | **OWS extension** | — | M3 ties elevation to variant; OWS consumers occasionally need to override (e.g., highlight a "focus" card in a list). Kept as numeric override; variant supplies the default. |
| `setPadding(SpaceScale h, SpaceScale v)` | **OWS extension** | — | M3 doesn't expose per-card padding overrides; default applies. OWS uses denser cards in list contexts and roomier cards in detail contexts. Token-typed per #62. |

**Dropped from V1.** `setBody(JComponent)`, `setFooter(JComponent)`, `setFooter(Component...)`, `setHeader(String)` + the two overloads, `setCornerRadius(Integer)` + `getEffectiveCornerRadius()`, `setPadding(Insets)`, `setPadding(int)`, `setBorderColor(Color)`, `setSurfaceColor(Color)`, `setCollapsedSummary(JComponent)`, `setKeepSummaryWhenExpanded(boolean)` + `isKeepSummaryWhenExpanded()`, `getTitleLabel()`, `getSubtitleLabel()`. Migration map in §8.

## 5. `SummaryVisibility` enum

```java
public enum SummaryVisibility {
  /**
   * Summary renders only while the card is collapsed; expanding hides the summary and shows
   * the body slots (default).
   */
  COLLAPSED_ONLY,

  /**
   * Summary renders in both states — visible as a persistent header band whether the card is
   * collapsed or expanded. Use when the summary surfaces metrics / state the user wants
   * always-visible.
   */
  ALWAYS
}
```

**Defaults:** `COLLAPSED_ONLY`. **Add later on demand:** `EXPANDED_ONLY` (collapse hides everything; summary only shows when expanded) — not in v0.1; no current OWS consumer asks for it.

**Replaces V1's** `setKeepSummaryWhenExpanded(boolean)`. The boolean modeled "always" as an override on top of an unstated default; the enum makes the visibility policy first-class and self-documenting at the call site (`setSummaryVisibility(SummaryVisibility.ALWAYS)` reads better than `setKeepSummaryWhenExpanded(true)`).

## 6. M3 "checked" icon overlay

When `isSelected()` is true, paint a circular checked-icon badge in the **top-trailing corner** of the card surface (anchored to the card's outer rect, not the header strip — so the overlay stays anchored regardless of slot composition).

**Geometry.**
- Position: card top-right, inset by `SpaceScale.SM` from the top edge and `SpaceScale.SM` from the right edge.
- Badge size: 24 dp circle (matches `MaterialIcons.DEFAULT_SIZE = 24`).
- Icon glyph: `MaterialIcons.check(16)` (M3 check, 16 dp inside a 24 dp container, centered).
- Background fill: `ColorRole.PRIMARY` at 100 % opacity.
- Glyph color: `ColorRole.ON_PRIMARY`.

**Contrast.** Always correct by construction — paired via the `ColorRole.on()` pair lookup on `PRIMARY`, so any palette substitution carries the matched glyph color.

**Z-order.** Painted last (above the card body), with antialiased edges; does not occupy layout. The badge can overlap a trailing action — accepted, the badge is meant to read as "this card is selected" even in dense layouts, and obscuring a trailing pin / favorite is fine while the card is in a selected state.

**Accessibility.** Card's accessible-name machinery (carried forward from V1's `CardInteractionMode.SELECTABLE` path) gains a `" (selected)"` suffix when `selected == true`. Screen readers announce the selection state; the badge is purely visual.

**Replaces V1's** tint-only treatment (V1 darkened the surface fill when selected). The tint approach was load-bearing in `SELECTABLE` cards but invisible in `STATIC` + manually-toggled cards because the M3 state-layer machinery isn't applied to non-interactive surfaces. The badge is variant- and interaction-mode-agnostic.

## 7. V2 API surface

Method signatures + javadoc summary lines for every public method. Inherited Surface API (4 typed setters + 4 getters) is not repeated here.

```java
public class ElwhaCard extends ElwhaSurface {

  // Property change constants.
  /** Fired on every {@link #setSelected(boolean)} transition. */
  public static final String PROPERTY_SELECTED = "selected";
  /** Fired on every {@link #setCollapsed(boolean)} transition. */
  public static final String PROPERTY_COLLAPSED = "collapsed";

  // Elevation cap, carried forward from V1.
  public static final int MAX_ELEVATION = 5;

  // ---- Construction --------------------------------------------------------

  /** No-arg constructor; equivalent to {@code new ElwhaCard(null)}. */
  public ElwhaCard();

  /** Convenience constructor — primary content is the headline. */
  public ElwhaCard(String headline);

  /** {@code ELEVATED} variant preset. */
  public static ElwhaCard elevatedCard(String headline);

  /** {@code FILLED} variant preset. */
  public static ElwhaCard filledCard(String headline);

  /** {@code OUTLINED} variant preset. */
  public static ElwhaCard outlinedCard(String headline);

  // ---- Variant + interaction ----------------------------------------------

  public ElwhaCard setVariant(CardVariant variant);
  public CardVariant getVariant();

  public ElwhaCard setInteractionMode(CardInteractionMode mode);
  public CardInteractionMode getInteractionMode();

  // ---- M3 formal slots ----------------------------------------------------

  public ElwhaCard setHeadline(String headline);
  public String getHeadline();

  public ElwhaCard setSubhead(String subhead);
  public String getSubhead();

  public ElwhaCard setSupportingText(String text);
  public String getSupportingText();

  public ElwhaCard setMedia(JComponent media);
  public JComponent getMedia();

  public ElwhaCard setActions(Component... actions);

  // ---- OWS header extensions ----------------------------------------------

  public ElwhaCard setLeadingIcon(Icon icon);
  public Icon getLeadingIcon();

  public ElwhaCard setLeadingActions(Component... actions);
  public ElwhaCard setTrailingActions(Component... actions);

  // ---- Disclosure axis ----------------------------------------------------

  public ElwhaCard setCollapsible(boolean collapsible);
  public boolean isCollapsible();

  public ElwhaCard setCollapsed(boolean collapsed);
  public boolean isCollapsed();

  public ElwhaCard setSummary(JComponent summary);
  public JComponent getSummary();

  public ElwhaCard setSummaryVisibility(SummaryVisibility visibility);
  public SummaryVisibility getSummaryVisibility();

  public ElwhaCard setAnimateCollapse(boolean animate);
  public boolean isAnimateCollapse();

  // ---- Selection ----------------------------------------------------------

  public ElwhaCard setSelected(boolean selected);
  public boolean isSelected();

  // ---- Token-bound chassis (OWS extensions on top of Surface) -------------

  public ElwhaCard setElevation(int elevation);  // 0..MAX_ELEVATION
  public int getElevation();

  public ElwhaCard setPadding(SpaceScale horizontal, SpaceScale vertical);
  public Insets getPadding();

  // ---- Listeners ----------------------------------------------------------

  /** Named per #62 doctrine — listener targets the SELECTED property. */
  public void addSelectionChangeListener(PropertyChangeListener listener);
  public void removeSelectionChangeListener(PropertyChangeListener listener);

  /** Named per #62 doctrine — listener targets the COLLAPSED property. */
  public void addExpansionChangeListener(PropertyChangeListener listener);
  public void removeExpansionChangeListener(PropertyChangeListener listener);

  /** ActionListener carried forward for {@code CLICKABLE} + {@code SELECTABLE} modes. */
  public void addActionListener(ActionListener listener);
  public void removeActionListener(ActionListener listener);

  // ---- Drag / cancel (carried forward from V1, used by ElwhaCardList) ----

  public ElwhaCard cancelPendingClick();
}
```

**No raw setters ship.** `setSurfaceColor(Color)`, `setCornerRadius(int)`, `setBorderColor(Color)`, `setPadding(Insets)`, `setPadding(int)` — all dropped without replacement (token-typed setters cover the same surface).

**No raw label getters.** `getTitleLabel()` / `getSubtitleLabel()` are gone. Consumers wanting to style the headline reach for `setHeadline(String)` and trust the token-foundation defaults; bespoke text styling on the card title was never a clean pattern.

## 8. Migration table

Maps every V1 public method (touched by current consumers in this repo and the OWS-tool consumer) to its V2 replacement or its deletion rationale.

| V1 method | V2 replacement | Notes |
|---|---|---|
| `setHeader(String)` | `setHeadline(String)` | Direct rename. |
| `setHeader(String, String)` | `setHeadline(String).setSubhead(String)` | Split into two setters. |
| `setHeader(String, String, Icon)` | `setHeadline(...)` + `setSubhead(...)` + `setLeadingIcon(Icon)` | V1's three-arg overload silently *cleared* `leadingIcon` when called via the two-arg form afterward (V1 escape hatch #5). V2 makes each slot independent. |
| `setLeadingIcon(Icon)` | `setLeadingIcon(Icon)` | Unchanged. |
| `setLeadingActions(Component...)` | `setLeadingActions(Component...)` | Unchanged. |
| `setTrailingActions(Component...)` | `setTrailingActions(Component...)` | Unchanged. |
| `setMedia(JComponent)` | `setMedia(JComponent)` | Unchanged. |
| `setBody(JComponent)` | **Dropped.** Use `setSupportingText(String)` for text, or compose your own panel via `add(Component)` for arbitrary body content. | M3 doesn't have a generic "body" slot; the V1 slot was a placeholder for anything-goes content. Consumers needing arbitrary children should add them directly — Card is still a `JPanel`. |
| `setFooter(JComponent)` | `setActions(Component...)` for buttons; `add(Component)` for arbitrary | The two V1 footer overloads collapsed under one M3 vocabulary. |
| `setFooter(Component...)` | `setActions(Component...)` | Direct rename — M3 names the bottom-row button group "actions". |
| `setCollapsible(boolean)` | `setCollapsible(boolean)` | Unchanged. |
| `setCollapsed(boolean)` | `setCollapsed(boolean)` | Unchanged. |
| `setCollapsedSummary(JComponent)` | `setSummary(JComponent)` | Renamed — "summary" is the M3-friendlier term and matches `SummaryVisibility`. |
| `setKeepSummaryWhenExpanded(boolean)` | `setSummaryVisibility(SummaryVisibility.ALWAYS)` | V1 escape hatch #4 — boolean override on top of unstated default. V2 makes the visibility model first-class. |
| `isKeepSummaryWhenExpanded()` | `getSummaryVisibility()` | Read the enum. |
| `setAnimateCollapse(boolean)` | `setAnimateCollapse(boolean)` | Unchanged. |
| `setSelected(boolean)` | `setSelected(boolean)` | Renders via M3 checked-icon overlay, not surface tint. |
| `isSelected()` | `isSelected()` | Unchanged. |
| `setVariant(CardVariant)` | `setVariant(CardVariant)` | Variant enum is now `ELEVATED / FILLED / OUTLINED` — `GHOST` and `WARM_ACCENT` are gone. |
| `setInteractionMode(CardInteractionMode)` | `setInteractionMode(CardInteractionMode)` | Unchanged. |
| `setElevation(int)` | `setElevation(int)` | Default now variant-derived (see §3). |
| `setCornerRadius(Integer)` | `setShape(ShapeScale)` (inherited from Surface) | Token-typed; pre-1.0 break. |
| `getEffectiveCornerRadius()` | `getShape()` (inherited) | Renames + retypes (returns `ShapeScale`, not `int`). |
| `setPadding(Insets)` | `setPadding(SpaceScale, SpaceScale)` | Token-typed; pre-1.0 break. |
| `setPadding(int)` | `setPadding(SpaceScale, SpaceScale)` | Token-typed; pre-1.0 break. |
| `setBorderColor(Color)` | `setBorderRole(ColorRole)` (inherited from Surface, not advertised on Card) | Border role is variant-derived; consumers needing a non-variant border should change the variant. |
| `setBorderWidth(int)` | `setBorderWidth(int)` (inherited from Surface) | Unchanged. |
| `setSurfaceColor(Color)` | `setSurfaceRole(ColorRole)` (inherited from Surface) | V1 escape hatch #2 retired by Surface's token-typed API. |
| `getTitleLabel()` | **Dropped.** No replacement. | V1 escape hatch #1 — raw `JLabel` getter. Card is the contract; bespoke text styling on its internal labels was never a clean pattern. |
| `getSubtitleLabel()` | **Dropped.** No replacement. | V1 escape hatch #1. |
| `cancelPendingClick()` | `cancelPendingClick()` | Unchanged; used by `ElwhaCardList` drag plumbing. |
| `addActionListener(ActionListener)` | `addActionListener(ActionListener)` | Unchanged. |
| `onChange(String, PropertyChangeListener)` | `addSelectionChangeListener(PCL)` / `addExpansionChangeListener(PCL)` | V1's generic "by property name" subscription is gone — V2 exposes named listeners per #62 doctrine (Chip + IconButton already do this). Backed by the same `PropertyChangeSupport`. |

## 9. Carousel-readiness contract

Card V2 is the building block for an eventual `ElwhaCarousel` (epic not yet filed; out of scope for v0.1). The contract V2 must honor so the future carousel can compose it without re-fighting the API:

1. **Width is parent-imposed, not self-asserted.** Card returns sensible `preferredSize` from its layout but does not force a minimum or maximum width. A carousel sets card width by `setBounds(...)` or by `preferredSize` override on a wrapper; Card cooperates.
2. **Shape paint is mask-friendly.** The card's `paintComponent` paints the rounded shape into a clip the parent can read via `getShape()` (or by inspecting the Surface's painted region). This lets a carousel apply a mask (e.g., for the M3 "edge-stretching" effect during a swipe) without the card painting outside its own clip.
3. **Never install internal scroll.** Card does not wrap its body in a `JScrollPane`. If content overflows, the card grows; the parent decides how to handle scroll (carousel by horizontal page; list by vertical scroll on the container, not the card).
4. **Expansion is local, not signaling.** `setCollapsed(false)` grows the card. Sibling cards in a list / carousel do not react — the parent's `LayoutManager` decides whether to relayout or scroll. V1 already behaves this way; V2 preserves it.
5. **No drag plumbing in Card.** Drag-to-reorder lives on `ElwhaCardList<T>` (#69 will unify this with chip-list drag). Card only exposes the `cancelPendingClick()` cooperation hook the existing reorder model uses.

## 10. Cross-reference to #62 doctrine

Card V2 matches every rule in [`docs/development/component-api-conventions.md`](../development/component-api-conventions.md):

| Rule | Card V2 |
|---|---|
| §1 Getter naming — `getX()` only | `getShape()` (inherited), `getSurfaceRole()` (inherited), `getVariant()`, `getInteractionMode()`, `getHeadline()` etc. No `getEffectiveX()` on the surface. |
| §2 Per-variant static factories | `elevatedCard(String)`, `filledCard(String)`, `outlinedCard(String)`. |
| §3 Single-arg convenience ctor | `new ElwhaCard(String headline)`. |
| §4 Border-role exposure on variant-bearing | Card does not advertise `setBorderRole(ColorRole)` in its public API. The setter remains inherited from Surface but is not part of Card's documented surface. |
| §5 Symmetric border-width | `setBorderWidth(int)` + `getBorderWidth()` inherited from Surface. |

---

## 11. Out of scope for v0.1

- `ElwhaCardList<T>` selection / drag unification — that's epic [#67](https://github.com/OWS-PFMS/elwha/issues/67), stories #68 / #69 / #70.
- `ElwhaCarousel` primitive — out of scope; the carousel-readiness contract here (§9) is the spec the future epic would consume.
- Aggregate split question (separate primitives for M3 Carousel / List / Grid vs unified container class) — open architectural question, revisit post-V2.
- `SummaryVisibility.EXPANDED_ONLY` — add later when an OWS consumer asks.
- Per-instance selection-overlay customization (different color, different glyph) — not in v0.1; if needed, consumers can paint on top of the card.

## 12. Implementation guard-rails (story #65 reads these)

Concrete things story #65 must not do, captured here to keep the implementer from sliding off-spec:

- **Don't keep `GHOST` or `WARM_ACCENT`.** Delete the enum constants entirely; any reference to them anywhere in `src/` must move to one of the three V2 variants or be deleted.
- **Don't ship `setBody(JComponent)` or `setFooter(...)`.** They have V2 replacements (`setSupportingText`, `setActions`, raw `add(...)`).
- **Don't preserve `setKeepSummaryWhenExpanded(boolean)` as a shim.** It's escape-hatch #4; the enum is the replacement.
- **Don't preserve `getTitleLabel()` / `getSubtitleLabel()`.** They're escape-hatch #1.
- **Don't paint a selected-state surface tint.** The checked-icon overlay is the only selection visual.
- **Don't introduce a `getEffectiveX()` getter on any V2 method.** Locked in #62.
- **Don't add a `Carousel`-readiness setter** (e.g., `setMaxWidth`). The contract is "cooperate," not "expose hooks."
- **`ElwhaCardList<T>` lives.** Story #65 does not touch the list class unless the V2 Card API forces a signature change — if it does, document why in the PR body.
- **Playgrounds will break.** That's #66's job, not #65's. The #65 PR body should call this out so reviewers don't expect green playgrounds yet.
