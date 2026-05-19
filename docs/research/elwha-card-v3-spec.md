# ElwhaCard V3 — Spec

**Status:** LOCKED for v0.2 build. This doc fixes the V3 API surface,
layered architecture, slot vocabulary, token bindings, state paint,
actionability model, accessibility model, and migration approach.

**Drafted:** 2026-05-17

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — §9 "build only when raw Swing + tokens can't express the need."
- [`elwha-surface-design.md`](elwha-surface-design.md) — V3 composes / extends `ElwhaSurface` for token-bound paint.
- [`elwha-token-taxonomy.md`](elwha-token-taxonomy.md) — the 79-token surface V3 binds against.
- [`m3-card-spec-organized.md`](m3-card-spec-organized.md) — M3 doctrine, token bindings, accessibility (the spec content source).
- [`m3-card-lib-survey.md`](m3-card-lib-survey.md) — API-shape comparison across 5 libs supporting the chrome+composition decision.
- [`elwha-card-v3-sketch.md`](elwha-card-v3-sketch.md) — architectural narrative + GO decision.
- [`../development/component-api-conventions.md`](../development/component-api-conventions.md) — six locked doctrine rules every V3 surface must match (includes the new §6 leaf-vs-container rule).

**Epic:** TBD — file when Phase 2 starts.

**Origin:** The in-flight V2 rebuild (PRs #71–#77) was diagnosed as
under-specified against the actual M3 anatomy + adaptive design model
during a screenshot-led walkthrough of the M3 Cards spec on 2026-05-17.
A library survey of five M3 / M3-adjacent Card implementations
([`m3-card-lib-survey.md`](m3-card-lib-survey.md)) confirmed both
Google-shipped M3 references (MaterialCardView, Compose Material3) use
a chrome-only Card primitive with content composition delegated to
companion primitives. V3 adopts that shape; V2 is abandoned without
shipping (cherry-pick salvage in `chore/v2-salvage-and-icons`, then
close).

---

## TL;DR

1. **Chrome + composition split.** `ElwhaCard extends ElwhaSurface` is
   a chrome-only primitive (chassis, state, variants, actionability,
   ripple, collapse behavior, orientation). Content lives in companion
   primitives in the same package, added via `card.add(...)`.
2. **Variants:** `ELEVATED` (default) / `FILLED` / `OUTLINED`. Each
   carries the M3 surface role + per-state elevation + outline
   treatment (Outlined only). All values anchored to AndroidX Compose
   Material3 token sources.
3. **Companion primitives:** Layer 2 atoms (`ElwhaCardTitle`,
   `ElwhaCardSubtitle`, `ElwhaCardSupportingText`,
   `ElwhaCardLeadingIcon`, `ElwhaCardThumbnail`); Layer 3 layout
   primitives (`ElwhaCardHeader`, `ElwhaCardMedia`,
   `ElwhaCardActions`, `ElwhaCardDivider`); Layer 4 disclosure
   affordances (`ElwhaCardChevron`, `ElwhaCardExpandLink`); Layer 5
   adaptive wrapper (`ElwhaAdaptiveCard`, deferred post-1.0).
4. **Default LayoutManager:** custom `VerticalCardLayout` —
   `add()` order = stacking order, with M3-aware tweaks (media bleeds
   to chassis edges at first/last position, `ElwhaCardActions` as last
   child anchors to chassis bottom when there's slack, `ElwhaCardDivider`
   bleeds horizontally for `DividerStyle.FULL`). **HORIZONTAL orientation
   is deferred to v0.3.0** per spec §15.3 + #112; v0.2.0 ships
   VERTICAL only.
5. **Actionability is atomic.** `setActionable(boolean)` gates four
   signals together: cursor (hand), hover state-layer paint, ripple,
   tab stop + AccessibleRole. Not separable.
6. **Selection:** orthogonal to actionability — `setSelectable(boolean)`
   + `setSelected(boolean)` + M3 checked-icon overlay (top-trailing).
7. **Collapse:** Card owns the *behavior* (`setCollapsible` +
   `setCollapsed` + per-child `CollapseRule`); consumers place the
   *affordance* (chevron in header trailing, or text link in body) as
   any other component.
8. **Doctrine:** matches every rule in `#62` component-api-conventions,
   including the new §6 leaf-vs-container rule (ElwhaCard is the first
   container — chrome+composition shape).
9. **Carousel-readiness:** preserved from V2 §9. Cooperates with
   externally-imposed width; opt-in `setExpansionOverflow(GROW | SCROLL)`
   softens the "never installs internal scroll" stance to match M3
   doctrine.
10. **V1 lifecycle:** V1 moves to `card.v1.*` package at the start of
    Phase 1. V3 takes `card.*`. 0.2.0 ships both. 1.0.0 deletes V1.

---

## 1. The §9 bar

Design direction §9: *"Build a component only when raw Swing + tokens
can't express what you need."*

V3 is a rebuild, not a new build — V1 already ships and V2 was an
attempted rebuild that didn't merge. The §9 question for V3 is whether
the chrome + composition split *is* what V1 and V2 should have been —
and the library survey says yes:

1. **Both M3 references ship chrome-only Cards.** MaterialCardView
   exposes ~30 setters for the chassis (shape, stroke, fill, ripple,
   elevation, checked-icon) and **zero typed content slots**. Compose
   Material3 ships `Card { content }` — six chassis params + one
   content lambda. Slot vocabulary lives on `ListItem` (Compose) or
   nested child views (MaterialCardView), not on the Card itself.
   ([`m3-card-lib-survey.md`](m3-card-lib-survey.md) §1, §2.)
2. **V2's typed-slot Card was the most opinionated of any surveyed
   lib** — more so than Joy UI's hybrid, far more than the M3
   references. Every new M3 pattern surfaced during the spec walkthrough
   (leading thumbnail vs icon, header trailing polymorphism across chip
   / icon button / overflow, two-tier text, horizontal orientation,
   multiple vertical layouts, action row leading/trailing segments)
   would have required a new typed setter on V2. Chrome+composition
   absorbs them as companions without API churn on the root.
3. **V1's API was rushed.** It pre-dates Elwha's theme system entirely
   (extracted from the OWS-tool without the token-bound rewrite). Its
   surface is not a useful design reference for V3 — only a migration
   target.

V3 is the smallest Card that aligns with the M3-canonical chrome shape,
binds against Elwha's 79-token surface, and exposes a composable slot
vocabulary that matches M3's "core six + sanctioned additive" anatomy
model.

## 2. Architecture — the five layers

```
┌─ Layer 5 ── ElwhaAdaptiveCard ──────────────── opt-in breakpoint wrapper
│                                                  (deferred post-1.0)
├─ Layer 4 ── Disclosure affordances ─────────── ElwhaCardChevron, ElwhaCardExpandLink
│
├─ Layer 3 ── Layout primitives ───────────────── ElwhaCardHeader, ElwhaCardMedia,
│                                                  ElwhaCardActions, ElwhaCardDivider
│
├─ Layer 2 ── Atoms (typed text/icon/thumbnail) ─ ElwhaCardTitle, ElwhaCardSubtitle,
│                                                  ElwhaCardSupportingText,
│                                                  ElwhaCardLeadingIcon, ElwhaCardThumbnail
│
└─ Layer 1 ── ElwhaCard ─────────────────────── chrome only (chassis, state, variants,
                                                  actionability, ripple, collapse,
                                                  orientation, selection overlay)
```

**Composition rule.** Consumers add Layer 2 and Layer 3 components to
a Layer 1 Card via `add(...)`. `add()` order = layout order. Card
itself exposes zero typed content slots.

**Package layout.** All five layers live flat in
`com.owspfm.elwha.card` (the package vacated by V1 in Phase 1). No
sub-packages. Family relationship carried by the `ElwhaCard*` prefix.
Match Joy UI / shadcn structure. Per
[`component-api-conventions.md`](../development/component-api-conventions.md)
§6.

**Inheritance.** `ElwhaCard extends ElwhaSurface`. Surface paint
(rounded role-filled rect + optional outline) is inherited and never
re-implemented. Card adds: state-layer overlay, multi-layer drop
shadow, checked-icon overlay, focus ring, ripple, layout, collapse,
orientation.

## 3. Layer 1 — `ElwhaCard` (chrome)

### 3.1 API surface

```java
public class ElwhaCard extends ElwhaSurface {

  // Property change constants
  public static final String PROPERTY_SELECTED = "selected";
  public static final String PROPERTY_COLLAPSED = "collapsed";
  public static final String PROPERTY_ACTIONABLE = "actionable";

  // ---- Construction --------------------------------------------------------

  public ElwhaCard();

  // Per-variant static factories (#62 §2)
  public static ElwhaCard elevatedCard();
  public static ElwhaCard filledCard();
  public static ElwhaCard outlinedCard();

  // ---- Variant -------------------------------------------------------------

  public ElwhaCard setVariant(CardVariant variant);
  public CardVariant getVariant();

  // ---- Chassis (inherited from ElwhaSurface — listed for visibility) -------
  //   setSurfaceRole(ColorRole),  getSurfaceRole()    -- variant-derived default
  //   setShape(ShapeScale),       getShape()          -- default ShapeScale.MD (12dp)
  //   setBorderWidth(int),        getBorderWidth()
  // NOT exposed on Card (#62 §4): setBorderRole — variant-derived

  // ---- Elevation (variant-derived default, per-instance override) ----------

  public ElwhaCard setElevation(int elevationLevel);    // 0..MAX_ELEVATION
  public int getElevation();
  public static final int MAX_ELEVATION = 5;

  // ---- Padding (token-typed) -----------------------------------------------

  public ElwhaCard setPadding(SpaceScale horizontal, SpaceScale vertical);
  public Insets getPadding();
  // Default: SpaceScale.LG horizontal, SpaceScale.LG vertical
  //   (resolves to 16dp per the M3 measurement spec frame)

  // ---- Actionability (atomic gate — see §12) -------------------------------

  public ElwhaCard setActionable(boolean actionable);
  public boolean isActionable();
  public void addActionListener(ActionListener listener);
  public void removeActionListener(ActionListener listener);

  // ---- Selection (orthogonal to actionability — see §13) -------------------

  public ElwhaCard setSelectable(boolean selectable);
  public boolean isSelectable();
  public ElwhaCard setSelected(boolean selected);
  public boolean isSelected();
  public void addSelectionChangeListener(PropertyChangeListener listener);
  public void removeSelectionChangeListener(PropertyChangeListener listener);

  // ---- Collapse / disclosure (see §14) -------------------------------------

  public ElwhaCard setCollapsible(boolean collapsible);
  public boolean isCollapsible();
  public ElwhaCard setCollapsed(boolean collapsed);
  public boolean isCollapsed();
  public ElwhaCard setAnimateCollapse(boolean animate);
  public boolean isAnimateCollapse();
  public void setCollapseConstraint(Component child, CollapseRule rule);
  public CollapseRule getCollapseConstraint(Component child);
  public void addExpansionChangeListener(PropertyChangeListener listener);
  public void removeExpansionChangeListener(PropertyChangeListener listener);

  // ---- Expansion overflow (see §14.4) --------------------------------------

  public ElwhaCard setExpansionOverflow(ExpansionOverflow strategy);
  public ExpansionOverflow getExpansionOverflow();
  // Default: GROW (current behavior; sibling layout reacts).
  //   SCROLL = internal scroll, sibling cards stay put (M3 desktop pattern).

  // ---- Orientation (see §15) -----------------------------------------------
  // v0.2.0 ships VERTICAL only — chassis layout is VerticalCardLayout, add()
  //   order is stack order. HORIZONTAL is deferred to v0.3.0 per §15.3 / #112.
  //   The v0.3 design will reuse add(...) with typed partitioning
  //   (ElwhaCardMedia → leading column, everything else → trailing column
  //   under the same VerticalCardLayout rules) — no setLeadingColumn /
  //   setTrailingColumn API. CardOrientation enum is intentionally absent.

  // ---- Drag (carried forward from V1; used by ElwhaCardList<T>) -----------

  public ElwhaCard setDragged(boolean dragged);
  public boolean isDragged();
  public ElwhaCard cancelPendingClick();
}

public enum CardVariant { ELEVATED, FILLED, OUTLINED }
public enum CollapseRule { ALWAYS_VISIBLE, COLLAPSIBLE }
public enum ExpansionOverflow { GROW, SCROLL }
```

### 3.2 What V3 inherits vs overrides from `ElwhaSurface`

| Surface API | V3 disposition |
|---|---|
| `setSurfaceRole(ColorRole)` | **Inherited.** Default = variant-derived (§8). Per-instance override allowed. |
| `getSurfaceRole()` | Inherited. |
| `setShape(ShapeScale)` | Inherited. Default = `ShapeScale.MD` (12dp). Per-instance override allowed. |
| `getShape()` | Inherited. |
| `setBorderRole(ColorRole)` | **Inherited but NOT advertised** per #62 §4 (Card is variant-bearing). Variant supplies border role. |
| `getBorderRole()` | Same. |
| `setBorderWidth(int)` | **Inherited.** Variant supplies default; consumer may override. |
| `getBorderWidth()` | Inherited. |
| `paintComponent(Graphics)` | **Extended.** V3 calls `super.paintComponent` (surface paint) then layers: drop shadow → state-layer overlay → border (per variant) → focus ring (if focused) → ripple (if rippling). Selection-overlay badge paints in `paintChildren` to stay above media. |

### 3.3 Layout responsibility

| Orientation | LayoutManager | Composition |
|---|---|---|
| `VERTICAL` (default; only mode shipped in v0.2.0) | `VerticalCardLayout` | `card.add(child)` — `add()` order = stack order |

**HORIZONTAL deferred to v0.3.0** per spec §15.3 + #112. The v0.3
design will reuse `card.add(...)` with **typed partitioning**
(`ElwhaCardMedia` → leading column; everything else → trailing column
under the same `VerticalCardLayout` rules), so orientation becomes a
re-layout, not a re-construction. No `setLeadingColumn` /
`setTrailingColumn` setters in the future API.

### 3.4 Width-constraint behavior

Cards are responsive components. The chassis honors the width its
parent's LayoutManager assigns and never forces itself wider —
M3 doctrine, anchored to the spec page on visual presentation /
spacing:

> *"To adjust the presentation of content-focused components, begin with
> spacing. Allow components like lists, cards, and images to optimize
> space while filling the region of a screen that suits a device
> breakpoint's ergonomic needs."*

Prior art convergence (Compose Material 3 `Card`, MUI Joy UI `Card`,
Material Components Web `mdc-card`, and the CSS / `object-fit: cover`
foundation all agree on the same contract):

1. **Chassis honors parent-assigned width.** `ElwhaCard.getPreferredSize()`
   reports a sensible preferred width based on content, but the chassis
   accepts any width the parent gives it — never overflows the
   parent's allocation, never resists shrinking. `getMaximumSize()`
   returns `Integer.MAX_VALUE` (`JPanel` default, preserved) so
   `BoxLayout` / `GridLayout` / any parent layout can stretch *or
   compress* the chassis freely.

2. **Text reflows.** Layer 2 text atoms (`ElwhaCardTitle`,
   `ElwhaCardSubtitle`, `ElwhaCardSupportingText`) HTML-auto-wrap and
   compute their height for whatever width the chassis hands them.
   Per §4.x defaults: text atoms do not ellipsize. Narrow chassis →
   taller text block. To make `BoxLayout(Y_AXIS)` actually pass the
   narrowed width down, each atom must report
   `getMaximumSize() = (Integer.MAX_VALUE, preferredHeight)` so
   BoxLayout stretches it horizontally — `JLabel`'s default
   `getMaximumSize` (= preferred) would otherwise lock the atom at
   its natural text width.

3. **Media cover-fits.** `ElwhaCardMedia` follows CSS `object-fit:
   cover` semantics: the chassis layout sizes the media component to
   the chassis-content width × `(width / aspectRatio)` height; the
   image then scales to fill the slot, preserving its source aspect
   ratio. If the source aspect ratio differs from the slot's, the
   image is cover-cropped at the slot edges. Narrow chassis →
   proportionally narrower media at proportionally smaller height,
   re-cropped from the same source. The rule applies in any
   orientation when HORIZONTAL ships in v0.3.0; v0.2.0 exercises it
   in VERTICAL only.

4. **No hard minimum width.** M3 cites no specific minimum and defers
   to its window-size-class breakpoint system (Compact / Medium /
   Expanded / Large / X-Large). Elwha follows: no minimum is
   enforced. Cards remain legible down to whatever width still fits
   chassis padding (`SpaceScale.LG` × 2 = 32dp) plus at least one
   wrapped character of content. Below that the chassis still renders
   but content is effectively unreadable — same behavior any `JPanel`
   exhibits when over-constrained. Consumers wanting a hard minimum
   set `setMinimumSize(...)` or wrap the card in a constrained
   container.

**Universal invariant: no child paints past the chassis bounds, ever.**
Every reference library enforces this (Compose coerces, CSS clips by
default, MUI / mdc-card rely on overflow rules). Elwha enforces it
explicitly:

- `ElwhaSurface.paintChildren` clips child paint to the body's
  rounded shape — both for the M3 corner-clip aesthetic AND as a
  hard overflow boundary.
- The chassis's `VerticalCardLayout` sizes children to fit within the
  chassis-content bounds; nothing should ever need overflow clipping
  as a fallback. If a child's bounds exceed the chassis, that's a
  layout bug, not a clipping fallback to lean on.

## 4. Layer 2 — Atoms

Typed text / icon / thumbnail with M3-correct defaults baked in. All
extend `JLabel` (`JComponent` for `ElwhaCardThumbnail`) for free
accessibility, HTML wrap, and FlatLaf integration.

### 4.1 `ElwhaCardTitle`

```java
public final class ElwhaCardTitle extends JLabel {
  public ElwhaCardTitle();
  public ElwhaCardTitle(String text);

  public ElwhaCardTitle setTypeRole(TypeRole role);     // default TITLE_MEDIUM
  public TypeRole getTypeRole();

  public ElwhaCardTitle setColorRole(ColorRole role);   // default ON_SURFACE
  public ColorRole getColorRole();
}
```

**Defaults:** `TypeRole.TITLE_MEDIUM`, `ColorRole.ON_SURFACE`,
start-aligned, word-wraps (does not ellipsize at narrow widths).
HTML auto-wrapped via the same convention `setSupportingText` uses.

### 4.2 `ElwhaCardSubtitle`

```java
public final class ElwhaCardSubtitle extends JLabel {
  public ElwhaCardSubtitle();
  public ElwhaCardSubtitle(String text);

  public ElwhaCardSubtitle setTypeRole(TypeRole role);   // default LABEL_MEDIUM
  public TypeRole getTypeRole();

  public ElwhaCardSubtitle setColorRole(ColorRole role); // default ON_SURFACE_VARIANT
  public ColorRole getColorRole();
}
```

**Defaults:** `TypeRole.LABEL_MEDIUM`, `ColorRole.ON_SURFACE_VARIANT`,
start-aligned.

### 4.3 `ElwhaCardSupportingText`

```java
public final class ElwhaCardSupportingText extends JLabel {
  public ElwhaCardSupportingText();
  public ElwhaCardSupportingText(String text);

  public ElwhaCardSupportingText setTypeRole(TypeRole role);     // default BODY_MEDIUM
  public TypeRole getTypeRole();

  public ElwhaCardSupportingText setColorRole(ColorRole role);   // default ON_SURFACE_VARIANT
  public ColorRole getColorRole();
}
```

**Defaults:** `TypeRole.BODY_MEDIUM`, `ColorRole.ON_SURFACE_VARIANT`,
start-aligned, HTML wrap. Multi-line content uses `<br>` or natural
word wrap.

### 4.4 `ElwhaCardLeadingIcon`

```java
public final class ElwhaCardLeadingIcon extends JLabel {
  public ElwhaCardLeadingIcon();
  public ElwhaCardLeadingIcon(Icon icon);

  public ElwhaCardLeadingIcon setColorRole(ColorRole role);   // default PRIMARY
  public ColorRole getColorRole();
}
```

**Defaults:** size = 24dp (M3 canonical icon size for card leading),
color = `ColorRole.PRIMARY`. When the icon is a `FlatSVGIcon` from
`MaterialIcons`, the theme color filter applies automatically.

### 4.5 `ElwhaCardThumbnail`

Distinct from `ElwhaCardLeadingIcon` — accepts a photographic / object
image rather than a vector glyph. Recurring across multiple M3 frames
(Caminante, Daniel Maas, conversation cards).

```java
public final class ElwhaCardThumbnail extends JComponent {
  public ElwhaCardThumbnail(Image image);

  public ElwhaCardThumbnail setShape(ThumbnailShape shape);   // default CIRCULAR
  public ThumbnailShape getShape();

  public ElwhaCardThumbnail setSize(int dp);                   // default 40dp
  public int getSize();
}

public enum ThumbnailShape { CIRCULAR, SQUARE }
```

**Inert by construction:** `setFocusable(false)` in constructor. No
event listeners exposed beyond `JComponent` base.

## 5. Layer 3 — Layout primitives

### 5.1 `ElwhaCardHeader`

Composes atoms into the M3 header anatomy.

```java
public final class ElwhaCardHeader extends JComponent {

  public ElwhaCardHeader();

  // Leading — single slot (M3: leading is icon OR thumbnail, never multi)
  public ElwhaCardHeader setLeading(JComponent leading);
  public JComponent getLeading();
  public ElwhaCardHeader clearLeading();

  // Title — String shorthand OR typed Atom for typography override
  public ElwhaCardHeader setTitle(String text);
  public ElwhaCardHeader setTitle(ElwhaCardTitle title);
  public ElwhaCardTitle getTitle();

  // Subtitle — same two-overload pattern
  public ElwhaCardHeader setSubtitle(String text);
  public ElwhaCardHeader setSubtitle(ElwhaCardSubtitle subtitle);
  public ElwhaCardSubtitle getSubtitle();

  // Trailing — N items (M3: header trailing is polymorphic — chips,
  //   standard icon buttons, overflow trigger; can be 1-many)
  public ElwhaCardHeader addTrailing(JComponent affordance);
  public ElwhaCardHeader clearTrailing();
  public List<JComponent> getTrailingItems();
}
```

**Internal layout:** leading-column (if `leading != null`) +
title/subtitle stack (flex center, takes remaining width) +
trailing-row (flush right). Gap between segments: `SpaceScale.SM`.
Vertical alignment: title baseline shares row baseline with leading
and trailing items.

### 5.2 `ElwhaCardMedia`

Inert media slot. Factory-only construction; no public JComponent
escape hatch (M3 + actionability doctrine: media must be inert
decoration).

```java
public final class ElwhaCardMedia extends JComponent {

  // Factories
  public static ElwhaCardMedia image(Image image);
  public static ElwhaCardMedia painter(Consumer<Graphics2D> paint);

  // Sizing
  public ElwhaCardMedia setAspectRatio(double ratio);   // default 16:9
  public double getAspectRatio();

  public ElwhaCardMedia setPreferredHeight(int dp);     // overrides aspect-ratio sizing
  public int getPreferredHeight();

  // Accessibility (#109): informative vs decorative
  public ElwhaCardMedia setDecorative(boolean decorative); // default false (informative)
  public boolean isDecorative();

  public ElwhaCardMedia setAltText(String altText);     // null clears; default null
  public String getAltText();
}
```

**Inert by construction:** `setFocusable(false)` baked in; no public
`add(...)` overload exposed; no event listener overloads beyond
`JComponent` base. Media is decorative content, never interactive.

**Accessibility (#109).** M3 distinguishes informative media (the photo
of an article's hero, a chart, a screenshot — carries meaning, screen
reader verbalizes alt-text) from decorative media (a generic
hero-strip pattern, a faint divider thumbnail — purely visual, AT
should skip it). Per [`m3-card-spec-organized.md` §5.5.3](m3-card-spec-organized.md#553-decorative-image-rule):

- `setDecorative(false)` (default) + `setAltText("...")` → `AccessibleRole.ICON`
  with the alt-text exposed as the accessible description. Screen
  readers announce it during traversal.
- `setDecorative(true)` → `AccessibleRole.LABEL` with null name /
  description so AT skips the node entirely. Use for pure decoration.

The setters re-initialize the `AccessibleContext` so a toggle mid-
session takes effect on the next AT query.

**Corner clipping:** owned by the chassis, not the media. Per #106 and
spec §3.4, `ElwhaSurface.paintChildren` intersects every child's paint
with `SurfacePainter.bodyShape(w, h, arc)` — a single source of truth
for the chassis outer curve. Media painted at any chassis edge rounds
naturally to match the chassis corners; `ElwhaCardMedia` itself does
no local clipping.

### 5.3 `ElwhaCardActions`

```java
public final class ElwhaCardActions extends JComponent {

  public ElwhaCardActions();

  public ElwhaCardActions addLeading(JComponent action);
  public ElwhaCardActions clearLeading();
  public List<JComponent> getLeadingActions();

  public ElwhaCardActions addTrailing(JComponent action);
  public ElwhaCardActions clearTrailing();
  public List<JComponent> getTrailingActions();
}
```

**Internal layout:** leading-segment (left-anchored) + flex gap +
trailing-segment (right-anchored). Intra-segment spacing:
`SpaceScale.SM` (8dp). Inter-segment: flex (consumes remaining width).
Row vertical alignment: baseline.

Either segment may be empty. A card with only one segment of actions
should pick the right axis per M3 doctrine: lone promo action goes
leading; paired actions or overflow-bearing rows go trailing.

### 5.4 `ElwhaCardDivider`

```java
public final class ElwhaCardDivider extends JComponent {
  public ElwhaCardDivider();                          // default FULL
  public ElwhaCardDivider(DividerStyle style);
  public DividerStyle getStyle();
}

public enum DividerStyle { FULL, INSET }
```

`FULL` spans card edge-to-edge (ignores parent content padding).
`INSET` respects content padding. Both paint `ColorRole.OUTLINE_VARIANT`
at 1dp.

## 6. Layer 4 — Disclosure affordances

### 6.1 `ElwhaCardChevron`

M3 chevron icon button bound to a Card's collapsed state.

```java
public final class ElwhaCardChevron extends ElwhaIconButton {
  public ElwhaCardChevron(ElwhaCard card);
  public ElwhaCard getCard();
}
```

**Behavior:**
- On construction: registers an `addExpansionChangeListener` on the
  card; swaps glyph between `MaterialIcons.expandMore()` (collapsed)
  and `MaterialIcons.expandLess()` (expanded).
- On click: calls `card.setCollapsed(!card.isCollapsed())`.
- Default size: `IconButtonSize.S` (32dp) — sized to fit alongside
  header text without dominating the row.
- Inherits all `ElwhaIconButton` chrome (state layer, focus, ripple,
  a11y).

### 6.2 `ElwhaCardExpandLink`

M3 text-link variant of the disclosure affordance — primary-color
underlined link, body-bottom placement (consumer adds it after a full-
width divider).

```java
public final class ElwhaCardExpandLink extends JComponent {
  public ElwhaCardExpandLink(ElwhaCard card, String expandText, String collapseText);
  public ElwhaCard getCard();
  public String getExpandText();
  public String getCollapseText();
  public ElwhaCardExpandLink setColorRole(ColorRole role);   // default PRIMARY
}
```

**Behavior:** registers an `addExpansionChangeListener` on the card;
swaps text between `expandText` (collapsed) and `collapseText`
(expanded). Click toggles `card.setCollapsed(...)`. Tab-focusable;
Enter / Space activates.

## 7. Layer 5 — `ElwhaAdaptiveCard` (deferred)

```java
public final class ElwhaAdaptiveCard extends JComponent {
  public ElwhaAdaptiveCard at(BreakpointClass cls, Consumer<ElwhaCard> builder);
}

public enum BreakpointClass { COMPACT, MEDIUM, EXPANDED }
```

Opt-in wrapper. Listens to its own width; rebuilds the contained Card
per the matching breakpoint. **Deferred to post-1.0** — no concrete
OWS use case yet. Most consumers know their layout context at compose
time and just call `setOrientation(...)` directly. Specced for future
reference; not in V3's initial scope.

Default breakpoints: COMPACT (< 600dp), MEDIUM (< 840dp), EXPANDED
(≥ 840dp). M3 window-size classes.

## 8. Variant table + token bindings

Anchored to AndroidX Compose Material3 `ElevatedCardTokens.kt`,
`FilledCardTokens.kt`, `OutlinedCardTokens.kt`.

| Variant | Container role (rest) | Container role (disabled) | Outline | Default elevation (rest) |
|---|---|---|---|---|
| `ELEVATED` (default) | `SURFACE_CONTAINER_LOW` | `SURFACE` | none | Level 1 (1dp) |
| `FILLED` | `SURFACE_CONTAINER_HIGHEST` | `SURFACE_VARIANT` | none | Level 0 (0dp) |
| `OUTLINED` | `SURFACE` | `SURFACE` (no change) | `OUTLINE_VARIANT` at 1dp | Level 0 (0dp) |

**Disabled container role swaps** for Elevated and Filled — not just
an opacity fade. Outlined keeps its rest role (the disabled signal is
the faded outline at 0.12 opacity, not a fill change).

**Foreground:** resolved as the `on`-pair of the variant's surface
role — `ON_SURFACE` for ELEVATED + OUTLINED, `ON_SURFACE` for FILLED's
container-highest. Always correct by construction.

**Border-role override:** per #62 §4, V3 does not advertise
`setBorderRole(ColorRole)`. Border role is variant-derived. Inherited
from Surface but not part of V3's documented surface.

## 9. Elevation table

M3 elevation ramp (per `ElevationTokens.kt`): Level0=0dp, Level1=1dp,
Level2=3dp, Level3=6dp, Level4=8dp, Level5=12dp.

Per-variant per-state elevation:

| Variant | Rest | Hover | Focus | Pressed | Dragged | Disabled |
|---|---|---|---|---|---|---|
| Elevated | 1 (1dp) | 2 (3dp) | 1 (1dp) | 1 (1dp) | 4 (8dp) | 1 (1dp) — shape stays lifted, container fades |
| Filled | 0 (0dp) | 1 (1dp) | 0 (0dp) | 0 (0dp) | 3 (6dp) | 0 |
| Outlined | 0 (0dp) | 1 (1dp) | 0 (0dp) | 0 (0dp) | 3 (6dp) | 0 |

**Cross-variant rules:**
- **Hover lifts every variant** +1 level (Elevated 1→2, Filled 0→1,
  Outlined 0→1). Not Elevated-only.
- **Dragged lifts every variant**, with Elevated reaching the highest
  level (4 vs 3 for Filled/Outlined).
- **Focus and Pressed never change elevation** — state layer only.

**Implementation:** carry forward the multi-layer shadow paint
(`paintShadow`) from the V2 cherry-pick salvage. Shadow geometry
scales with the resolved elevation Level. Disabled-Elevated keeps
Level 1 shadow while applying 0.38 container opacity.

## 10. State paint

### 10.1 State-layer opacities

All variants, applied as `on-surface @ Xpct` overlay over the
resolved container token:

| State | Opacity | Token role |
|---|---|---|
| Hover | 0.08 | `ON_SURFACE` |
| Focus | 0.10 | `ON_SURFACE` |
| Pressed | 0.10 | `ON_SURFACE` + ripple |
| Dragged | 0.16 | `ON_SURFACE` |

State-layer painting is variant-agnostic for the chassis — same
formula over whatever surface-container token the variant resolved to.

### 10.2 Focus ring

Painted only when the chassis is the focused element AND `isActionable()`
is true.

| Variant | Focus-ring color |
|---|---|
| Elevated | `SECONDARY` |
| Filled | `SECONDARY` |
| Outlined | **outline role swaps to `ON_SURFACE`** (the existing outline IS the ring; painting an inset ring inside the outline would double-stroke) |

**Geometry:** 2dp stroke, painted as inset on the surface's outer
rounded shape. Half-pixel-grid centering for crisp AA.

Anchored to `ElevatedCardTokens.FocusIndicatorColor = Secondary`,
`FilledCardTokens.FocusIndicatorColor = Secondary`,
`OutlinedCardTokens.FocusOutlineColor = OnSurface`.

### 10.3 Ripple

Painted only when `isActionable()` is true. Custom Swing paint
(Material Components doesn't ship a Swing widget). Expanding-circle
alpha animation seeded at the click point, clipped to the card's
rounded shape.

Animation timing: 250ms expand to fill, fade out begins at 150ms,
clears at 400ms. Standard M3 ripple feel.

## 11. Disabled state

Two effects compose:

1. **Container role swap** per §8 (Elevated → Surface, Filled →
   SurfaceVariant, Outlined unchanged).
2. **Container opacity 0.38** applied to the resolved fill (post-swap).

Content opacity (text, icons, buttons inside the card) is handled by
the child components' own disabled tokens — they already resolve to
0.38 per M3, so the net visual is uniform 0.38 across the card.

**Outlined disabled outline:** color = `OUTLINE` (the stronger role,
not `OUTLINE_VARIANT`), opacity = 0.12. Stronger role token painted
faintly — a faded full-strength stroke, not a weaker role at full
strength.

## 12. Actionability quadrad

Direct M3 quote (per
[`m3-card-spec-organized.md`](m3-card-spec-organized.md) §5.1):

> A card can be a non-actionable container that holds actions like
> buttons and links, or it can be directly actionable without any
> buttons or links. **An action shouldn't be placed on an actionable
> surface.**

V3 takes the same stance Compose and MCV take: **doctrine, not
enforcement.** V3 does not refuse to add interactive children to an
actionable card. The README documents the rule; consumers respect it.

### 12.1 The atomic gate

`setActionable(boolean)` toggles four signals together. They cannot be
configured independently:

| Signal | Actionable=true | Actionable=false |
|---|---|---|
| Cursor on hover | `HAND_CURSOR` | default |
| State-layer hover paint | yes (`on-surface @ 8%`) | **no** |
| Ripple on click | yes (expanding circle, clipped to shape) | no |
| Chassis tab stop | yes (one stop = whole card) | no (only children are stops) |
| AccessibleRole | `PUSH_BUTTON` | `PANEL` |
| `addActionListener` fires | yes (mouse click, Space, Enter) | no |

Compose's `Card { ... }` vs `Card(onClick = ...) { ... }` overloads
model this distinction at the API level. In Swing's setter-based world,
the equivalent is a single `setActionable(boolean)` toggle.

### 12.2 Focus order within a non-actionable card

When `isActionable() == false`, the chassis is not a tab stop, but
every actionable child is. Focus traversal proceeds in **reading
order** — top-to-bottom, leading-to-trailing — through the children,
then advances to the next sibling card.

## 13. Selection model

**Orthogonal to actionability.** A card can be selectable without
being actionable (V1 had this conflated — `SELECTABLE` interaction
mode implied actionable). V3 separates the two axes:

| `setSelectable` | `setActionable` | Behavior |
|---|---|---|
| false | false | Static container; no interaction |
| false | true | Clickable card (fires `ActionEvent`); not selectable |
| true | false | Selection toggles on click via internal handler; no `ActionEvent` |
| true | true | Both — click toggles selection AND fires `ActionEvent` |

### 13.1 Checked-icon overlay

When `isSelected()` is true, paint a circular checked-icon badge in
the top-trailing corner of the card surface (carried forward from V2).

**Geometry:**
- Position: card top-right, inset by `SpaceScale.SM` from top and right
- Badge: 24dp circle filled `ColorRole.PRIMARY`
- Glyph: `MaterialIcons.check(16)` colored `ColorRole.ON_PRIMARY`
- Painted in `paintChildren` (above media), with antialiased edges
- Does not occupy layout space

**Accessibility:** card's accessible name gains `" (selected)"`
suffix when `selected == true`.

### 13.2 Selection list integration

`ElwhaCardList<T>` (§17) manages selection across multiple cards using
the V3 `CardSelectionModel`. Selection modes: `NONE`, `SINGLE`,
`SINGLE_MANDATORY`, `MULTI`. Modes inherited from the existing
`ChipSelectionMode` pattern.

## 14. Collapse / disclosure model

### 14.1 Card owns behavior, consumer places affordance

`ElwhaCard` owns the collapsed *state* and the visibility *behavior*.
Consumers choose *where* to put the affordance:

- Chevron icon button in header trailing: `header.addTrailing(new ElwhaCardChevron(card))`
- Text link at body bottom: `card.add(new ElwhaCardExpandLink(card, "Expand", "Collapse"))`

No M3-sanctioned position is privileged in the API. Both work.

### 14.2 Per-child `CollapseRule`

```java
card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);
card.setCollapseConstraint(media,  CollapseRule.ALWAYS_VISIBLE);
// body has no explicit constraint; defaults to COLLAPSIBLE
```

Two values:
- `ALWAYS_VISIBLE`: child stays visible whether the card is collapsed
  or expanded. Use for headers, identity media, etc.
- `COLLAPSIBLE` (default): child hides when card is collapsed.

Per-child constraint stored on the card (`Map<Component, CollapseRule>`),
queried by the card's LayoutManager during layout.

### 14.3 Animation

`setAnimateCollapse(boolean)` toggles the height-tween on
`setCollapsed`. Default: off in test/headless contexts (detected via
`GraphicsEnvironment.isHeadless()`), on in interactive UIs.

Animation: 250ms tween, M3 easing curve, on the card's preferred
height. LayoutManager invalidates and re-lays out parent on tween
completion.

### 14.4 `ExpansionOverflow` strategy

```java
card.setExpansionOverflow(ExpansionOverflow.GROW);    // default
card.setExpansionOverflow(ExpansionOverflow.SCROLL);  // for carousel / fixed-grid contexts
```

- **`GROW`** (default): card grows to its expanded preferred size.
  Sibling layout reacts as the parent's LayoutManager allows. Matches
  V1/V2 behavior.
- **`SCROLL`**: card grows to its configured `MaxExpandedHeight`
  (TBD setter) then installs an internal `JScrollPane` for the body.
  Siblings stay put. M3-sanctioned for desktop carousel / horizontal-row
  contexts per
  [`m3-card-spec-organized.md`](m3-card-spec-organized.md) §2.5.

Softens V2 spec §9's "never installs internal scroll" stance to match
M3 doctrine.

## 15. Orientation model

### 15.1 v0.2.0 — VERTICAL only

v0.2.0 ships VERTICAL as the sole orientation; HORIZONTAL is deferred
to v0.3.0 per #112. There is no `setOrientation` method, no
`CardOrientation` enum, and no `setLeadingColumn` / `setTrailingColumn`
setters on `ElwhaCard` in v0.2.0.

### 15.2 VERTICAL — `add()` order = layout order

```java
card.add(media);    // first → top
card.add(header);
card.add(body);
card.add(actions);  // last → bottom
```

Three M3-sanctioned vertical layouts buildable, no API change between
them:

- Media → Header → Body → Actions (media-on-top)
- Header → Body → Media → Actions (Display small)
- Header → Media → Body → Actions

### 15.3 HORIZONTAL — deferred to v0.3.0

**Deferred.** The Phase-2 shipped API (`setOrientation(HORIZONTAL)` +
`setLeadingColumn(JComponent)` + `setTrailingColumn(JComponent)`)
was withdrawn before v0.2.0 ship because it created an asymmetric
composition contract: VERTICAL uses `card.add(...)` with the chassis
owning layout, while HORIZONTAL forced the consumer to compose the
trailing column's internal layout themselves — and `add()` threw in
HORIZONTAL mode. The typed Layer-3 primitives (`ElwhaCardHeader`,
`ElwhaCardActions`, `ElwhaCardDivider`) lost their auto-positioning
behavior inside the trailing column because the chassis handed off
layout at the column boundary.

**v0.3.0 design intent — unified-`add()` typed partitioning.**
HORIZONTAL re-enters under a single composition contract: same
`card.add(child)` calls in both orientations; chassis partitions
children by type at layout time.

```java
// v0.3.0 — identical add() calls in either orientation
card.add(media);
card.add(header);
card.add(body);
card.add(actions);

card.setOrientation(VERTICAL);    // VerticalCardLayout: media at top, etc.
card.setOrientation(HORIZONTAL);  // ElwhaCardMedia → leading column;
                                  // everything else → trailing column,
                                  // VerticalCardLayout rules applied
                                  // inside the trailing column.
```

Partitioning rule (typed, not positional — sidesteps §22's anti-
heuristic stance):

| Child type | VERTICAL position | HORIZONTAL position (v0.3.0) |
|---|---|---|
| `ElwhaCardMedia` | stacks in `add()` order; bleeds at edges if first/last | leading column, bleeds to chassis edges |
| Everything else (`ElwhaCardHeader`, atoms, `ElwhaCardDivider`, `ElwhaCardActions`, custom `JComponent`) | stacks in `add()` order | trailing column, stacked in `add()` order under `VerticalCardLayout` rules |

Open design questions for v0.3.0 (file under the new epic when it lands):

- Multiple `ElwhaCardMedia` children in HORIZONTAL — first is leading,
  subsequent are trailing-stack? Or throw?
- No media in HORIZONTAL — trailing column takes the whole card
  (graceful degrade to VERTICAL-look) or throw?
- Non-media leading columns (e.g. a thick metadata panel) — drop
  entirely (consumers wrap in `ElwhaCardMedia.painter(...)`) or
  provide an escape-hatch setter?

### 15.4 Action alignment — deferred with HORIZONTAL

Action-alignment doctrine in horizontal cards (M3 organized doc §2.3:
both bottom-leading and bottom-trailing are sanctioned; horizontal
cards typically anchor bottom-trailing of the right column) re-enters
the spec when HORIZONTAL ships in v0.3.0. Under the unified-`add()`
design, the chassis's `VerticalCardLayout` running inside the trailing
column naturally applies the `anchorActionsToBottom` rule already
proven in VERTICAL.

## 16. Accessibility

### 16.1 Chassis AccessibleRole

Per `setActionable`:

| Card mode | Chassis role |
|---|---|
| `setActionable(true)` | `AccessibleRole.PUSH_BUTTON` |
| `setActionable(false)` | `AccessibleRole.PANEL` |

V3 does not distinguish button-acting vs link-acting cards in v0.2
(per organized doc §5.5.1 — visually identical, role distinction
handled by the consumer at the semantics layer). Possible v3+
addition.

### 16.2 Atom AccessibleRoles

| Atom | AccessibleRole | Notes |
|---|---|---|
| `ElwhaCardTitle` | `AccessibleRole.LABEL` + `AXRole = AXHeading` client property on macOS | Closest Swing analog to M3's "Heading" role |
| `ElwhaCardSubtitle` | `AccessibleRole.LABEL` | |
| `ElwhaCardSupportingText` | `AccessibleRole.LABEL` | |
| `ElwhaCardLeadingIcon` | `AccessibleRole.ICON` | `setAccessibleDescription` for alt-text |
| `ElwhaCardThumbnail` | `AccessibleRole.ICON` | Same. Default description: `null` (decorative); consumer sets per-instance via `setAccessibleDescription(altText)` for informative media |
| `ElwhaCardMedia` | `AccessibleRole.ICON` | Default decorative (`setFocusable(false)` + no accessible name); consumer can flip to informative |

### 16.3 Tab traversal

Per the actionability quadrad (§12.1):

| Card mode | Tab stops |
|---|---|
| Actionable card | One — the chassis itself |
| Non-actionable card | N — one per nested actionable child, in reading order, then onward |

Focus ring paints on the chassis only when the chassis itself is
focused (actionable mode). Non-actionable cards rely on child focus
rings.

### 16.4 Drag-reorder accessibility (likely v0.2 blocker)

Per M3 doctrine (organized doc §5.6.1):

> To meet Material's accessibility standards, any dragging or swiping
> interactions need a single-pointer alternative, like selecting the
> same actions from a menu.

`ElwhaCardList<T>` V3 must provide:

- Press-and-hold or focus-and-menu access to drag-equivalent actions
  (Move up / Move down / Delete)
- Keyboard binding alternatives (Cmd+↑ / Cmd+↓ for up/down at
  minimum)
- Menu placement that does not overlap the active row (bottom-sheet
  preferred; side-popover acceptable; overlapping avoid)

Without this, drag-reorder is not M3-compliant and blocks 1.0.

## 17. `ElwhaCardList<T>` V3 — scope

### 17.1 Lives in `card.list.*` (V1 list moves to `card.v1.list.*` in Phase 1)

V3 ElwhaCardList consumes V3 ElwhaCard. API shape:
- Implements `ElwhaList<T>` cross-cutting contract (orientation, gap,
  padding, empty, loading, filter, sort) per the existing
  `list.ElwhaList<T>` interface
- Owns the V3 `CardSelectionModel` for multi-card selection
- Owns drag-reorder with M3-compliant single-pointer alternatives
  (§16.4)
- Provides cell-renderer hook: `Function<T, ElwhaCard>` to map data
  items to V3 cards

### 17.2 Selection model

```java
public enum CardSelectionMode { NONE, SINGLE, SINGLE_MANDATORY, MULTI }
```

Mirrors `ChipSelectionMode` pattern. SINGLE_MANDATORY enforces at
least one selected card; useful for "filter strip" UIs that always
need an active filter.

### 17.3 Drag-reorder

Carried forward from V1 with the M3 a11y alternatives baked in
(§16.4). Drag-handle cursors (`grab` / `grabbing`, light/dark, 16/32px)
remain bundled in `card/list/cursors/`.

## 18. Carousel-readiness contract

Carried forward from V2 §9 with the §14.4 softening:

1. **Width is parent-imposed, not self-asserted.** Card returns
   sensible `preferredSize` from its layout but does not force a
   minimum or maximum width.
2. **Shape paint is mask-friendly.** Card's `paintComponent` paints
   the rounded shape into a clip the parent can read via `getShape()`.
3. **Expansion overflow is opt-in.** Default `GROW` (expansion grows
   the card; parent layout reacts). `SCROLL` (internal scroll, siblings
   stay put) is opt-in for carousel / horizontal-row / fixed-grid
   contexts.
4. **Expansion is local.** `setCollapsed(false)` grows the card.
   Sibling cards in a list / carousel do not react — the parent's
   `LayoutManager` decides whether to relayout or scroll. Same as V1/V2.
5. **No drag plumbing in Card.** Drag-to-reorder lives on
   `ElwhaCardList<T>`. Card only exposes the `cancelPendingClick()`
   cooperation hook and the `setDragged(boolean)` state.

## 19. Migration mapping (V1 → V3)

**This spec does not contain the V1 → V3 setter mapping.** V1's API is
pre-Elwha-theme legacy from the OWS-export; its shape is not useful
as a V3 design input, only as a migration target.

The V1 → V3 setter map is a separate inventory task to be produced
during Phase 5 (OWS migration planning) and lives at
`docs/migration/elwha-card-v1-to-v3.md` (or similar). It should
enumerate every V1 `ElwhaCard` setter/method and its V3 equivalent,
suitable for OWS implementors to follow when converting a card site.

That doc is not in scope for the V3 spec — it's part of OWS migration
planning, not V3 implementation.

## 20. Cross-reference to #62 doctrine

V3 matches every rule in
[`docs/development/component-api-conventions.md`](../development/component-api-conventions.md):

| Rule | V3 compliance |
|---|---|
| §1 Getter naming — `getX()` only | `getShape()`, `getSurfaceRole()`, `getVariant()`, `getElevation()`, `getOrientation()`, etc. No `getEffectiveX()`. |
| §2 Per-variant static factories | `elevatedCard()`, `filledCard()`, `outlinedCard()`. No-arg variants (Card has no single "primary content" concept under chrome+composition — primary content comes via `add()`). |
| §3 Single-arg convenience ctor | Not applicable — Card's primary content vocabulary is open. No-arg constructor is the only convenience. |
| §4 Border-role exposure on variant-bearing | V3 does not advertise `setBorderRole(ColorRole)`. Inherited from Surface but not part of V3's documented surface. |
| §5 Symmetric border-width | `setBorderWidth(int)` + `getBorderWidth()` inherited from Surface. |
| §6 Leaf vs container — chrome+composition for containers | V3 is the first Elwha container — chrome+composition shape, companion primitives flat in `card/` package. |

## 21. Out of scope for v0.2 / v1.0

- **Layer 5 `ElwhaAdaptiveCard`** — deferred to post-1.0 unless a
  concrete OWS use case surfaces.
- **Link-acting cards** (separate visual treatment from button-acting
  cards) — both render identically in v0.2; role distinction is a
  consumer concern at the semantics layer.
- **Card-list selection model extension on `ElwhaList<T>`** — separate
  epic ([`OWS-Local-Search-GUI#252`](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/252));
  V3 keeps the chip-list / card-list selection model patterns
  family-specific for now.
- **Per-instance focus-ring color customization** — variant-derived
  only.
- **Custom `MaxExpandedHeight` for `ExpansionOverflow.SCROLL`** —
  initial implementation uses a sensible default (e.g., 320dp); a
  setter ships if real usage demands it.
- **Two-tier text helper** — the Gmail-style pattern is expressible
  by atoms directly (header with title/subtitle, then a standalone
  `ElwhaCardTitle` in the body); no dedicated helper component
  ships.

## 22. Implementation guard-rails

Concrete things the V3 implementation must not do:

- **Don't reach for V1 as a design reference.** V1's API and paint
  approach are pre-theme-system legacy. The cherry-picked V2 work
  (paint pipeline, shadow math, icon assets) is the only salvageable
  prior art.
- **Don't add typed content setters to `ElwhaCard`.** No
  `setHeadline`, `setMedia`, `setActions`, `setLeadingIcon`,
  `setLeadingActions`, `setTrailingActions`, `setSummary`. Card is
  chrome only. Content lives in companion primitives.
- **Don't expose `JComponent` constructor on `ElwhaCardMedia`.** Two
  factories only: `image(Image)` and `painter(Consumer<Graphics2D>)`.
  Media is inert by construction.
- **Don't accept multiple leading items in `ElwhaCardHeader`.** M3
  evidence: leading is single (icon OR thumbnail). Use `setLeading`,
  not `addLeading`.
- **Don't paint a selection surface tint.** Checked-icon overlay is
  the only selection visual (per V2 spec §6, carried forward).
- **Don't fall back to OS dotted-line focus.** Per §10.2 the focus
  ring is 2dp inset, variant-correct color, half-pixel-centered AA.
- **Don't implement a surface-tint layer.** Per organized doc §3.6 +
  Compose tokens confirmation, surface-tint is not applied to cards
  in M3.
- **For the v0.3.0 HORIZONTAL rebuild (#112 follow-up): partition by
  type, not by position.** The earlier "don't bake
  first-child-is-left-column heuristic" guard-rail is retired. The
  unified-`add()` design uses `instanceof ElwhaCardMedia` to identify
  the leading column — role-in-type, not role-by-index. Positional
  heuristics ("first child is leading", "second child is trailing")
  remain banned: they reorder under RTL, break on child rearrangement,
  and don't survive `setOrientation()` flips.
- **Don't enforce the "no actions on actionable surface" doctrine in
  code.** Per §12 it's documentation, not type system. Both Compose
  and MCV take the same stance.
- **Don't introduce a `getEffectiveX()` getter on any V3 method.**
  Locked in #62 §1.
- **`MaxExpandedHeight` default for `ExpansionOverflow.SCROLL`** is
  internal — no setter in v0.2. Picks a sensible default; setter
  ships if a real use case demands it.
- **Don't preserve any V1 method as a deprecation shim.** V1 lives in
  `card.v1.*` during 0.2.0 for OWS migration. No bridging layer.
- **Playground may stay broken during the build-out.** Phase 3 step 13
  / 14 is when the V3 playground lands; until then, the V3 package
  imports won't build a full demo. Document in PR bodies.
- **Don't let any child paint past the chassis bounds.** Per §3.4, the
  chassis clip in `paintChildren` is a hard overflow boundary, not a
  cosmetic corner-clip. Children that don't fit clip at the chassis
  edge; they never overflow into sibling components. Media that
  doesn't fit cover-crops at its slot edge (CSS `object-fit: cover`
  semantics).
- **Atoms must report unbounded `getMaximumSize` X-axis.** Per §3.4
  rule 2, text atoms must override `getMaximumSize()` to return
  `(Integer.MAX_VALUE, preferredHeight)` so `BoxLayout(Y_AXIS)`
  stretches them to chassis-content width — letting HTML wrap take
  over at narrow widths. Don't rely on `JLabel`'s default
  `getMaximumSize() == preferred`.
