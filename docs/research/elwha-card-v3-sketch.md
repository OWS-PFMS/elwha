# Elwha Card V3 — design sketch

Working sketch for a Card primitive rebuild that aligns with the
M3-canonical "chrome + composition" pattern (per
[`m3-card-lib-survey.md`](m3-card-lib-survey.md)).

**Status:** sketch — not a spec. Captured from an iterative discussion
so we don't lose the model. Open questions in §8.

> **2026-05-19 addendum — HORIZONTAL orientation deferred to v0.3.0
> (#112).** This sketch was written assuming a single milestone shipped
> VERTICAL + HORIZONTAL together with an asymmetric API
> (`setLeadingColumn` / `setTrailingColumn` for HORIZONTAL,
> `card.add(...)` for VERTICAL). On review the asymmetry was judged a
> bad consumer contract: orientation should be a re-layout, not a
> re-construction. v0.2.0 ships VERTICAL only. v0.3.0 re-enters
> HORIZONTAL under a unified `card.add(...)` API with **typed
> partitioning** — `ElwhaCardMedia` → leading column, everything else
> → trailing column running `VerticalCardLayout` rules. See spec §15.3
> for the v0.3 design intent. Sections below that discuss the
> withdrawn HORIZONTAL API are kept as historical context for the v0.3
> follow-up; ignore them for v0.2.0 implementation.

## 1. Why V3 — the reframe

M3 defines a card as **six canonical anatomy elements** (Container,
Image, Button, Supporting text, Subhead, Headline) — but the spec then
shows examples with leading icons, leading thumbnails, header trailing
icon buttons, assist chips in the header, overflow menus, dividers,
two-tier text, etc. **The anatomy is a vocabulary, not a recipe.**

V2 baked many of those extensions as typed root-level setters
(`setLeadingIcon`, `setLeadingActions`, `setTrailingActions`,
`setMedia`, `setActions`, etc.). The lib survey shows that both
Google-shipped M3 references (MaterialCardView and Compose Material3)
take the opposite approach: **the Card is chrome only, content
composition lives in companion primitives or in `add(child)` directly.**

V3 adopts the chrome + composition split. The Card primitive holds the
M3 chassis story (look, feel, state, variants, actionability, ripple).
A small family of companion components holds the slot vocabulary
(headline/subhead/supporting/media/actions). Layout is `add()`-order
freedom. Adaptive orientation is opt-in.

Companion references:
- [`m3-card-spec-findings.md`](m3-card-spec-findings.md) — raw spec walkthrough
- [`m3-card-spec-organized.md`](m3-card-spec-organized.md) — topical M3 reference
- [`m3-card-lib-survey.md`](m3-card-lib-survey.md) — API comparison across MaterialCardView / Compose / Flutter / shadcn / Joy UI

## 2. Architecture — the layered model

Five layers, top to bottom:

```
┌─ Layer 5 ── ElwhaAdaptiveCard ──────────────── opt-in breakpoint wrapper
│
├─ Layer 4 ── Disclosure affordances ─────────── ElwhaCardChevron, ElwhaCardExpandLink
│
├─ Layer 3 ── Layout primitives ───────────────── ElwhaCardHeader, ElwhaCardMedia,
│                                                  ElwhaCardActions, ElwhaCardDivider
│
├─ Layer 2 ── Atoms (typed text/icon/thumbnail) ─ ElwhaCardTitle, ElwhaCardSubtitle,
│                                                  ElwhaCardSupportingText,
│                                                  ElwhaCardLeadingIcon, ElwhaCardThumbnail
│
└─ Layer 1 ── ElwhaCard ─────────────────────── chrome only (chassis, state,
                                                  variants, actionability, ripple,
                                                  collapse behavior, orientation)
```

**Composition rule:** consumers add Layer 2 (atoms) and Layer 3 (layout
primitives) to a Layer 1 Card via `add(...)`. `add()` order = layout
order. Card itself has zero typed content slots.

## 3. Component catalog

Signatures only — implementation deferred.

### Layer 1 — `ElwhaCard`

```java
public class ElwhaCard extends ElwhaSurface {
    // Variant factories
    public static ElwhaCard elevatedCard();
    public static ElwhaCard filledCard();
    public static ElwhaCard outlinedCard();

    // Chassis (inherited from ElwhaSurface)
    //   setSurfaceRole, setShape, setElevation, setBorderWidth, setPadding

    // Actionability — atomic gate (cursor + hover paint + ripple + tab stop + a11y role)
    public ElwhaCard setActionable(boolean);
    public void addActionListener(ActionListener);
    public AccessibleRole getChassisRole();   // PUSH_BUTTON | PANEL based on setActionable

    // Selection
    public ElwhaCard setSelectable(boolean);
    public ElwhaCard setSelected(boolean);
    public void addSelectionChangeListener(PropertyChangeListener);

    // Collapse — Card owns the BEHAVIOR; consumer places the affordance
    public ElwhaCard setCollapsible(boolean);
    public ElwhaCard setCollapsed(boolean);
    public void setCollapseConstraint(Component child, CollapseRule rule);
    public void addExpansionChangeListener(PropertyChangeListener);

    // Drag (CardList integration)
    public ElwhaCard setDragged(boolean);

    // Default LayoutManager: VerticalCardLayout — add()-order = stacking order,
    // with M3-aware tweaks (media bleeds at edges, actions anchor to bottom,
    // FULL dividers bleed horizontally). v0.2.0 ships VERTICAL only;
    // HORIZONTAL deferred to v0.3.0 per #112 — see spec §15.3.
}

public enum CollapseRule { ALWAYS_VISIBLE, COLLAPSIBLE }
```

### Layer 2 — Atoms

```java
public final class ElwhaCardTitle extends JLabel {
    public ElwhaCardTitle(String text);
    // Defaults: title-medium, on-surface, start-aligned, word-wraps
    public ElwhaCardTitle setTypeRole(TypeRole role);   // override default
}

public final class ElwhaCardSubtitle extends JLabel {
    public ElwhaCardSubtitle(String text);
    // Defaults: label-medium, on-surface-variant, start-aligned
    public ElwhaCardSubtitle setTypeRole(TypeRole role);
}

public final class ElwhaCardSupportingText extends JLabel {
    public ElwhaCardSupportingText(String htmlOrPlainText);
    // Defaults: body-medium, on-surface-variant, HTML wrap
    public ElwhaCardSupportingText setTypeRole(TypeRole role);
}

public final class ElwhaCardLeadingIcon extends JLabel {
    public ElwhaCardLeadingIcon(Icon icon);                 // typed adapter
    // Defaults: 24dp, primary color, follows MaterialIcons theme filter
}

public final class ElwhaCardThumbnail extends JComponent {
    public ElwhaCardThumbnail(Image image);
    public ElwhaCardThumbnail setShape(ThumbnailShape s);   // CIRCULAR | SQUARE
    public ElwhaCardThumbnail setSize(int dp);              // default 40dp
}

public enum ThumbnailShape { CIRCULAR, SQUARE }
```

### Layer 3 — Layout primitives

```java
public final class ElwhaCardHeader extends JComponent {
    // Slots
    public ElwhaCardHeader setLeading(JComponent leading);  // single (icon OR thumbnail)
    public ElwhaCardHeader setTitle(String text);           // shorthand
    public ElwhaCardHeader setTitle(ElwhaCardTitle title);  // for typography override
    public ElwhaCardHeader setSubtitle(String text);
    public ElwhaCardHeader setSubtitle(ElwhaCardSubtitle s);
    public ElwhaCardHeader addTrailing(JComponent affordance);  // N items: chips, icon buttons, overflow
    public ElwhaCardHeader clearTrailing();

    // Internal layout: leading column (if present) + title/subtitle column + trailing row
}

public final class ElwhaCardMedia extends JComponent {
    public static ElwhaCardMedia image(Image image);                     // bitmap
    public static ElwhaCardMedia painter(Consumer<Graphics2D> paint);    // direct Graphics2D
    public ElwhaCardMedia setAspectRatio(double ratio);                  // default 16:9
    public ElwhaCardMedia setPreferredHeight(int dp);                    // skip aspect, fix height

    // Inert by construction: setFocusable(false) baked in;
    //   no public add(...); no event listener overloads beyond JComponent base.
    // Clips to card shape (corner radius) automatically.
}

public final class ElwhaCardActions extends JComponent {
    public ElwhaCardActions addLeading(JComponent action);   // primary buttons
    public ElwhaCardActions addTrailing(JComponent action);  // icon buttons, overflow

    // Internal layout: leading segment left-anchored, trailing segment right-anchored
    // Spacing per M3 (8dp between siblings within segment, flex gap between segments)
}

public final class ElwhaCardDivider extends JComponent {
    public ElwhaCardDivider();                          // default FULL
    public ElwhaCardDivider(DividerStyle style);
}

public enum DividerStyle { FULL, INSET }
```

### Layer 4 — Disclosure affordances

```java
public final class ElwhaCardChevron extends ElwhaIconButton {
    public ElwhaCardChevron(ElwhaCard card);
    // Wires itself: click → card.setCollapsed(!card.isCollapsed())
    // Listens to card collapsed-change → swaps glyph (expand_more ↔ expand_less)
    // Sized to IconButtonSize.S (32dp) by default
}

public final class ElwhaCardExpandLink extends JComponent {
    public ElwhaCardExpandLink(ElwhaCard card, String expandText, String collapseText);
    // M3 text-link variant — primary-color underlined text
    // Click → card.setCollapsed(!card.isCollapsed())
    // Text swaps based on card collapsed-change
}
```

### Layer 5 — `ElwhaAdaptiveCard`

```java
public final class ElwhaAdaptiveCard extends JComponent {
    public ElwhaAdaptiveCard at(BreakpointClass cls, Consumer<ElwhaCard> builder);
    // Listens to its own width; rebuilds the contained Card per the matching breakpoint
    // Default breakpoints: COMPACT (< 600dp), MEDIUM (< 840dp), EXPANDED (>= 840dp)
}

public enum BreakpointClass { COMPACT, MEDIUM, EXPANDED }
```

Opt-in. Most consumers will not use this — they know their layout
context at compose time and just call `setOrientation(...)` directly.

## 4. Usage scenarios

### 4.1 Media-top promo card

```java
ElwhaCard card = ElwhaCard.elevatedCard();
card.add(ElwhaCardMedia.image(hero));
card.add(new ElwhaCardHeader().setTitle("Glass Souls' World Tour"));
card.add(new ElwhaCardSupportingText("From your recent favorites"));
card.add(new ElwhaCardActions().addLeading(filledButton("Buy tickets")));
```

```
┌──────────────────────────────────────┐
│██████████████████████████████████████│
│██████      (hero image)        ██████│
│██████████████████████████████████████│
│                                      │
│ Glass Souls' World Tour              │
│ From your recent favorites           │
│                                      │
│ ╭──────────────╮                     │
│ │ Buy tickets  │                     │
│ ╰──────────────╯                     │
└──────────────────────────────────────┘
```

### 4.2 Display small (media below text)

```java
ElwhaCard card = ElwhaCard.outlinedCard();
card.add(new ElwhaCardHeader().setTitle("Display small").setSubtitle("Subhead"));
card.add(new ElwhaCardSupportingText("Explain more about the topic..."));
card.add(ElwhaCardMedia.painter(g2 -> /* paint plate */));
card.add(new ElwhaCardActions().addTrailing(filledTonalButton("Action")));
```

```
┌──────────────────────────────────────┐
│ Display small                        │
│ Subhead                              │
│                                      │
│ Explain more about the topic in      │
│ the display and subhead through      │
│ supporting text.                     │
│                                      │
│ ╭──────────────────────────────────╮ │
│ │      (painted plate)             │ │
│ ╰──────────────────────────────────╯ │
│                                      │
│                       ╭───────────╮  │
│                       │  Action   │  │
│                       ╰───────────╯  │
└──────────────────────────────────────┘
```

### 4.3 Two-tier conversation card (Gmail-style)

```java
ElwhaCard card = ElwhaCard.elevatedCard();
card.add(new ElwhaCardHeader()
    .setLeading(new ElwhaCardThumbnail(avatar))
    .setTitle("Daniel Maas")
    .setSubtitle("Yesterday")
    .addTrailing(starIconButton));
card.add(new ElwhaCardTitle("Clay pot fair on Saturday?"));   // tier-2 atom standalone
card.add(new ElwhaCardSupportingText("I think it's time..."));
```

```
┌──────────────────────────────────────┐
│  ╭───╮  Daniel Maas             ☆    │
│  │ ● │  Yesterday                    │
│  ╰───╯                               │
│                                      │
│  Clay pot fair on Saturday?          │  ← tier-2 ElwhaCardTitle
│                                      │
│  I think it's time for us to finally │
│  try that new noodle shop downtown   │
│  that doesn't use menus. Anyone els..│
└──────────────────────────────────────┘
```

### 4.4 Full combined (media + full header + collapsible body, both orientations)

```java
ElwhaCard card = ElwhaCard.elevatedCard().setCollapsible(true);

ElwhaCardMedia media = ElwhaCardMedia.image(hero);
ElwhaCardHeader header = new ElwhaCardHeader()
    .setLeading(new ElwhaCardThumbnail(avatar))
    .setTitle("Daniel Maas")
    .setSubtitle("Yesterday")
    .addTrailing(starIconButton)
    .addTrailing(new ElwhaCardChevron(card));
ElwhaCardSupportingText body = new ElwhaCardSupportingText(
    "I think it's time for us to finally try that new noodle shop "
    + "downtown that doesn't use menus. Anyone else?");

card.add(media);
card.add(header);
card.add(body);

card.setCollapseConstraint(media,  CollapseRule.ALWAYS_VISIBLE);
card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);
// body is COLLAPSIBLE by default

card.setOrientation(VERTICAL);   // OR HORIZONTAL — only line that differs
```

**Vertical — expanded:**

```
┌──────────────────────────────────────┐
│██████████████████████████████████████│
│██████      (hero image)        ██████│
│██████████████████████████████████████│
│                                      │
│  ╭───╮  Daniel Maas         ☆   ⌃    │
│  │ ● │  Yesterday                    │
│  ╰───╯                               │
│                                      │
│  I think it's time for us to finally │  ← collapses
│  try that new noodle shop downtown   │
│  that doesn't use menus. Anyone else │
└──────────────────────────────────────┘
```

**Vertical — collapsed:**

```
┌──────────────────────────────────────┐
│██████████████████████████████████████│
│██████      (hero image)        ██████│
│██████████████████████████████████████│
│                                      │
│  ╭───╮  Daniel Maas         ☆   ⌄    │
│  │ ● │  Yesterday                    │
│  ╰───╯                               │
└──────────────────────────────────────┘
```

**Horizontal — expanded:**

In horizontal mode the consumer explicitly populates the two columns:

```java
card.setOrientation(HORIZONTAL);
card.setLeadingColumn(media);
JComponent rightCol = /* Box(Y_AXIS) containing header, body */;
card.setTrailingColumn(rightCol);
```

```
┌───────────────────────────────────────────────────────────┐
│█████████████│                                             │
│█████████████│  ╭───╮ Daniel Maas              ☆    ⌃      │
│██  media  ██│  │ ● │ Yesterday                            │
│██  (full  ██│  ╰───╯                                      │
│██ height) ██│                                             │
│█████████████│  I think it's time for us to finally try    │  ← collapses
│█████████████│  that new noodle shop downtown that doesn't │
│█████████████│  use menus. Anyone else                     │
└───────────────────────────────────────────────────────────┘
```

**Horizontal — collapsed:**

```
┌──────────────────────────────────────────────┐
│█████████████│                                │
│██  media  ██│  ╭───╮ Daniel Maas    ☆   ⌄    │
│██ (shrinks ██│  │ ● │ Yesterday              │
│█████████████│  ╰───╯                         │
└──────────────────────────────────────────────┘
```

## 5. Migration mapping (V1 → V3)

The actually-relevant migration is **V1 → V3**: V1 is what OWS ships
with today; V3 is what 0.2.0 will land alongside it. V2 never ships
and is not in the picture.

**This sketch does not contain the V1 → V3 setter mapping.** V1 is
pre-Elwha-theme legacy from the OWS-export — its API shape is not
useful as a V3 design input, only as a migration target. The V1 → V3
setter map is a separate inventory task that should be produced
during Phase 5 (OWS migration planning), modeled on V1's actual
public API rather than on this sketch's design.

Mapping output target: a sub-doc under `docs/migration/` (or similar)
that lists every V1 `ElwhaCard` setter/method and its V3 equivalent,
suitable for OWS implementors to follow when converting a card site.

## 6. Actionable next steps

Phased plan. All decisions in §8 are locked. Sequencing per Path B
(V1+V3 dual-package in 0.2.0; V1 deleted in 1.0.0).

### Phase 0 — V2 sweep (V2 never ships)

1. **Cherry-pick salvageable bits** from #72–#77 into
   `chore/v2-salvage-and-icons`:
   - Paint pipeline rewrite (multi-layer shadow, badge above children,
     supporting-text gap, precise media corner clipping)
   - Token-anchored elevation table wiring
   - Icon SVG assets (`expand_more.svg`, `expand_less.svg`,
     `more_vert.svg`)
   - `MaterialIcons.expandMore/expandLess/moreVert` accessors + sized
     overloads
   - `ElwhaIconButton.setRequestFocusEnabled(false)` default
   - Any other generally-applicable improvements (not card-V2-API
     specific)
2. **Close #72, #73, #74, #75, #76, #77 as superseded.** V2 never
   merges to main.

### Phase 1 — V1 package rename (one-time bulk update)

3. **PR — `chore/rename-v1-to-card-v1-package`** — move existing
   `card.ElwhaCard` (and its companions: `CardVariant`,
   `CardInteractionMode`, `card.list.*`, `card.playground.*`) to
   `card.v1.*` package. V1 functionality unchanged, only the import
   path moves. OWS picks this up in a single bulk import rename when
   it bumps Elwha — paired naturally with the existing
   FlatCard→ElwhaCard rename it's already doing.

### Phase 2 — V3 build-out (V1 stays in `card.v1.*`, untouched)

4. **Write V3 spec** — `docs/research/elwha-card-v3-spec.md` upgrading
   this sketch to a real spec. Modeled on `elwha-card-v2-spec.md`'s
   structure (the team-familiar spec shape).

   **Spec content sources — where to reach for each kind of content:**

   | Content area | Source |
   | ------------ | ------ |
   | Token bindings (surface roles, shape, elevation, type roles, state layers) | Elwha theme docs (`elwha-token-taxonomy.md`, `elwha-theme-install-api.md`) + `m3-card-spec-organized.md` §3–§4 |
   | Paint pipeline (shadow math, corner clip, state-layer overlay) | Cherry-picked V2 work from #74 (Phase 0 step 1) |
   | Component anatomy / API surface | This sketch (§3 catalog + §4 usage scenarios) |
   | Variant ↔ surface-role mapping | `m3-card-spec-organized.md` §3.1 + AndroidX token pins in §4.3, §4.5 |
   | M3 doctrine (actionability quadrad, accessibility, drag-reorder a11y, anatomy) | `m3-card-spec-organized.md` §5 |
   | API shape philosophy (chrome + composition rationale) | `m3-card-lib-survey.md` |
   | Leaf-vs-container API convention | Update `docs/development/component-api-conventions.md` first (see step 4a) |

   **DO NOT reach for V1.** V1 is pre-Elwha-theme legacy from the
   OWS-export; its API shape is the migration target, not a design
   reference. The only V1-related task in V3 design is producing the
   V1 → V3 migration map during Phase 5.

4a. **Update `docs/development/component-api-conventions.md`** — add
    the leaf-vs-container API doctrine: leaf widgets (Chip,
    IconButton) get single-class setter APIs; container widgets
    (Card) get chrome + composition primitives. New component
    authors pick based on role. This update is a prerequisite for
    step 4 because the spec will reference the convention.
5. **Stub Layer 1 `ElwhaCard` V3** — fresh class at `card.ElwhaCard`
   (the now-vacated package). Chassis + state machinery only.
6. **Build Layer 2 atoms** — `ElwhaCardTitle`, `ElwhaCardSubtitle`,
   `ElwhaCardSupportingText`, `ElwhaCardLeadingIcon`,
   `ElwhaCardThumbnail`. Single batched PR.
7. **Build Layer 3 layout primitives** — `ElwhaCardHeader`,
   `ElwhaCardMedia`, `ElwhaCardActions`, `ElwhaCardDivider`. One PR
   each (or two batched PRs: header+media, actions+divider).
8. **Wire collapse behavior** into Card with `CollapseRule` per-child
   constraints.
9. **Build Layer 4 disclosure** — `ElwhaCardChevron`,
   `ElwhaCardExpandLink`.
10. **Add orientation support** — `setOrientation(HORIZONTAL)` +
    `setLeadingColumn`/`setTrailingColumn` + custom 2-column
    LayoutManager.
11. **Wire actionability + accessibility** — atomic gate, ripple
    paint, focus ring (variant-correct color), AccessibleRole, tab
    traversal.
12. **Card-list integration** — V3 ElwhaCardList (in `card.list.*`,
    the now-vacated package) consumes V3 cards. Drag-reorder a11y per
    `m3-card-spec-organized.md` §5.6 — likely 1.0 blocker.

### Phase 3 — Playgrounds + theme integration

13. **Standalone V3 playground** — `ElwhaCardPlayground` (at
    `card.playground.*`, the now-vacated package) with gallery +
    live-config + snippet panel exercising every primitive and every
    usage scenario from §4.
14. **Theme playground update** — remove V1 Card tab from
    `ThemePlayground`, add V3 Card tab. (V2 never reached the theme
    playground.)

### Phase 4 — Ship 0.2.0 (dual-package)

15. **Release 0.2.0** — Elwha publishes with both `card.v1.*` (V1
    intact) and `card.*` (V3). OWS bumps to 0.2.0 and starts
    migrating card sites incrementally. Each OWS PR converts a
    chunk; OWS stays compilable throughout.

### Phase 5 — OWS migration (driven from OWS-Local-Search-GUI)

16. **OWS V1→V3 migration PRs** — incremental in OWS, one chunk per
    PR. Not in this lib.

### Phase 6 — Ship 1.0.0 (V3 only)

17. **(Defer) Layer 5 `ElwhaAdaptiveCard`** — opt-in. Probably ships
    post-1.0 unless a concrete OWS use surfaces before.
18. **PR — `chore/delete-card-v1`** — once OWS confirms migration
    complete, delete `card.v1.*` package entirely. No deprecation
    layer (pre-1.0 doctrine).
19. **Release 1.0.0** — Elwha V3-only. Stability milestone.
20. **OWS bumps to 1.0.0** — final cleanup.

## 7. Out of scope (explicitly NOT in V3)

- **Backwards-compatibility shims** — per `CLAUDE.md`'s "No
  backwards-compat shims pre-1.0" doctrine. V2 → V3 is a clean break.
- **`setLeadingActions(...)`** — M3 doesn't sanction multiple leading
  items. Single leading slot (icon OR thumbnail) is the rule.
- **`setMedia(JComponent)`** — media is `image()` or `painter(...)`
  only. No arbitrary JComponent escape hatch (misuse vector: consumer
  drops interactive children into media, breaking M3 + actionability
  doctrine).
- **Surface-tint layer** — confirmed deprecated per
  `m3-card-spec-organized.md` §3.6.
- **`setActionable` auto-detection from listener presence** — explicit
  setter is clearer in a setter-based API (Compose's onClick-presence
  detection works because composition is declarative; in Swing it'd be
  surprising).
- **Auto-breakpoint adaptation in `ElwhaCard`** — that's the opt-in
  `ElwhaAdaptiveCard` wrapper's job, not the Card primitive's.

## 8. Resolved decisions

All previously-open questions resolved.

| # | Question | Decision |
| - | -------- | -------- |
| 1 | Horizontal LayoutManager | **Explicit** `setLeadingColumn(JComponent)` / `setTrailingColumn(JComponent)` (RTL-aware, no first-child heuristic) |
| 2 | `setTitle` shorthand vs typed | **Keep both** overloads — `setTitle(String)` and `setTitle(ElwhaCardTitle)` |
| 3 | Atoms as `JLabel` or `JComponent` | **`JLabel` subclass** — free a11y, free HTML wrap, free icon support |
| 4 | `ElwhaAdaptiveCard` API | **Declarative** `at(breakpoint, builder)` — deferred to post-1.0 anyway |
| 5 | Actionability gate | **Setter** — `setActionable(boolean)` (matches setter-based Swing model) |
| 6 | Collapse constraint model | **Per-child constants** — `setCollapseConstraint(child, rule)` |
| 7 | V1 lifecycle | **Path B — dual-package in 0.2.0**, V1 deleted in 1.0.0. V2 (the in-flight rebuild) never ships — cherry-pick salvage + close PRs |
| 8 | Playground placement | **Standalone V3 playground** during build-out + remove V1 Card tab from `ThemePlayground` and add V3 Card tab |
| 9 | In-flight V2 PR stack | **Cherry-pick** valuable bits (paint pipeline, shadow math, icon assets, IconButton focus default), close #72–#77 as superseded |

**Package layout (Path B, option Y):**

- **Phase 1** (one-time rename): V1 moves from `card.*` to `card.v1.*`
- **Phase 2+** (V3 build-out): V3 takes over `card.*` (the just-vacated
  package). V3 lives at its final import path from day one.
- **Phase 6** (V1 deletion at 1.0.0): `card.v1.*` package deletes
  entirely.

**Release plan:**

| Release | Contents | Audience |
| ------- | -------- | -------- |
| 0.2.0 | V1 at `card.v1.*` + V3 at `card.*` (dual-package) | OWS migrates incrementally on this |
| 1.0.0 | V3 only at `card.*` (V1 deleted) | Stability milestone — Elwha's first 1.0 |

## 9. Status

**GO given.** All §8 decisions locked. Implementation kicks off at
§6 step 1 (cherry-pick salvage + close V2 PRs).

This sketch is the input to the V3 spec doc that gets written at §6
step 4. After that spec lands, this sketch becomes a historical
artifact and the spec governs.
