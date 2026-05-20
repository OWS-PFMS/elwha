# ElwhaCard V1 → V3 migration map

**For:** OWS-PFMS/OWS-Local-Search-GUI implementors (and any other Elwha 0.2.0+ consumer with a V1 card site to convert).

V1 is pre-Elwha-theme legacy that ships in 0.2.0 at `com.owspfm.elwha.card.v1.*`. V3 ships in 0.2.0 at `com.owspfm.elwha.card.*` (the unprefixed namespace). 1.0.0 deletes V1 entirely (story #96, gated on this migration finishing). This doc is the per-setter recipe for converting one card site at a time.

The conversion is **not a drop-in rename**. V1's accumulated escape-hatches (raw label getters, `setSurfaceColor` bolt-on, `setKeepSummaryWhenExpanded`, the polymorphic `setHeader` overloads) are replaced in V3 by:

- A **layered API** — Layer 1 chassis (`ElwhaCard`), Layer 2 atoms (`ElwhaCardTitle`, `ElwhaCardSubtitle`, `ElwhaCardSupportingText`, `ElwhaCardLeadingIcon`, `ElwhaCardThumbnail`), Layer 3 primitives (`ElwhaCardHeader`, `ElwhaCardMedia`, `ElwhaCardActions`, `ElwhaCardDivider`), Layer 4 disclosure (`ElwhaCardChevron`, `ElwhaCardExpandLink`).
- **Theme-token-resolved chrome** — no more `setBorderColor`, `setSurfaceColor`, or `setCornerRadius`. Variant + theme decide.
- **Composition over slots** — V3 doesn't have a "body" slot. You `card.add(...)` whatever you want into the chassis in the order you want it.
- **Decomposed interaction** — `setInteractionMode(CardInteractionMode)` is gone; `setActionable(boolean)` and `setSelectable(boolean)` are independent.

---

## 1. Setter mapping (the core)

| V1 method | V3 equivalent | Notes |
|---|---|---|
| `new ElwhaCard()` | `new ElwhaCard()` or `ElwhaCard.elevatedCard()` / `.filledCard()` / `.outlinedCard()` | V3 ships per-variant static factories (recommended). |
| `setVariant(CardVariant)` | `setVariant(CardVariant)` | Enum values identical (`ELEVATED` / `FILLED` / `OUTLINED`). |
| `setInteractionMode(CardInteractionMode)` | `setActionable(boolean)` + `setSelectable(boolean)` | See §5 "Interaction mode decomposition". |
| `setElevation(int)` | `setElevation(int)` | Moved to `ElwhaSurface` parent; same signature. V3 transient bump on hover (+1) / drag (+3) handled automatically. |
| `setCornerRadius(Integer)` | **DROPPED** | Theme `ShapeScale` token decides. To override per-card: not supported. |
| `getEffectiveCornerRadius()` | **DROPPED** | Same. |
| `setPadding(int)` / `setPadding(Insets)` | `setPadding(SpaceScale, SpaceScale)` | Takes horizontal + vertical scales (`SpaceScale.MD` etc) instead of raw px. |
| `getPadding()` | `getPadding()` | Returns resolved `Insets`. |
| `setBorderColor(Color)` | **DROPPED** | Theme `ColorRole` decides per variant. |
| `setBorderWidth(int)` | **DROPPED** | V3 manages border per variant. |
| `setHeader(String title)` | `card.add(new ElwhaCardHeader().setTitle(title))` | See §2 "Constructor mapping". |
| `setHeader(String title, String subtitle)` | `card.add(new ElwhaCardHeader().setTitle(t).setSubtitle(s))` | |
| `setHeader(String, String, Icon)` | `card.add(new ElwhaCardHeader().setLeading(new ElwhaCardLeadingIcon(icon)).setTitle(t).setSubtitle(s))` | |
| `setLeadingIcon(Icon)` | header's `setLeading(new ElwhaCardLeadingIcon(icon))` | Lives on header, not card. |
| `setLeadingActions(Component...)` | **DROPPED** | V3 header has no leading-actions slot. Use `ElwhaCardActions.addLeading(...)` for action-row leading affordances. |
| `getTitleLabel()` | `ElwhaCardHeader.getTitle()` | Returns typed `ElwhaCardTitle` instead of raw `JLabel`. |
| `getSubtitleLabel()` | `ElwhaCardHeader.getSubtitle()` | Returns typed `ElwhaCardSubtitle`. |
| `setTrailingActions(Component...)` | header's `addTrailing(component)` per affordance | One call per affordance instead of a varargs. Header trailing slots accept any `JComponent`. |
| `setMedia(JComponent)` | `card.add(ElwhaCardMedia.image(...))` or `ElwhaCardMedia.painter(...)` | **Image / painter only by design** (spec §5.2). For an interactive widget where media goes, just `card.add(yourComponent)` as a regular child. |
| `setBody(JComponent)` | `card.add(yourComponent)` | V3 has no body slot — chassis is a layout container. Add atoms (`ElwhaCardSupportingText`), primitives, or arbitrary `JComponent`s in order. |
| `setFooter(JComponent)` / `setFooter(Component...)` | `card.add(new ElwhaCardActions().addLeading(...).addTrailing(...))` | Replaces footer with the dedicated actions row primitive. |
| `setCollapsible(boolean)` | `setCollapsible(boolean)` | Same. |
| `setCollapsed(boolean)` | `setCollapsed(boolean)` | Same. |
| `setCollapsedSummary(JComponent)` | `card.setCollapseConstraint(child, CollapseRule.ALWAYS_VISIBLE)` per child you want pinned | See §5 "Per-child collapse constraints". |
| `setKeepSummaryWhenExpanded(boolean)` | **DROPPED** | The `ALWAYS_VISIBLE` rule covers both states automatically. |
| `setAnimateCollapse(boolean)` | `setAnimateCollapse(boolean)` | Same. |
| `setSelected(boolean)` | `setSelected(boolean)` | Requires `setSelectable(true)`. |
| `setSurfaceColor(Color)` | **DROPPED** | Theme `ColorRole` decides per variant. |
| `setEnabled(boolean)` | `setEnabled(boolean)` | Atoms fade to 0.38 opacity automatically (spec §11). |

### New in V3 (no V1 equivalent)

| V3 method | Purpose |
|---|---|
| `setExpansionOverflow(ExpansionOverflow.GROW \| SCROLL)` | Body overflow strategy when expanded (`SCROLL` installs an internal `JScrollPane`). |
| `setCollapseConstraint(child, CollapseRule)` | Per-child collapse visibility (`ALWAYS_VISIBLE` / `COLLAPSIBLE`). |
| Layer 4 disclosure widgets — `ElwhaCardChevron`, `ElwhaCardExpandLink` | First-class collapse affordances bound to a card. |

---

## 2. Constructor mapping

V1's `new ElwhaCard()` plus chained setters is replaced by per-variant factories plus `add()` calls.

```java
// V1
ElwhaCard card = new ElwhaCard()
    .setVariant(CardVariant.OUTLINED)
    .setHeader("Project alpha", "Updated 2 minutes ago")
    .setLeadingIcon(myIcon)
    .setMedia(myMediaComponent)
    .setBody(myBodyPanel)
    .setFooter(new JButton("Open"), new JButton("Dismiss"))
    .setCollapsible(true)
    .setCollapsed(true)
    .setCollapsedSummary(new JLabel("3 options hidden"));

// V3
ElwhaCard card = ElwhaCard.outlinedCard().setCollapsible(true).setCollapsed(true);
ElwhaCardHeader header = new ElwhaCardHeader()
    .setLeading(new ElwhaCardLeadingIcon(myIcon))
    .setTitle("Project alpha")
    .setSubtitle("Updated 2 minutes ago");
header.addTrailing(new ElwhaCardChevron(card));
card.add(header);
card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);
card.add(ElwhaCardMedia.image(myImage));            // or .painter(g -> ...)
card.add(myBodyPanel);                              // anything — V3 has no "body" slot
card.add(new ElwhaCardActions()
    .addTrailing(new JButton("Open"))
    .addTrailing(new JButton("Dismiss")));
```

---

## 3. Listener mapping

| V1 | V3 |
|---|---|
| `addActionListener(ActionListener)` | `addActionListener(ActionListener)` — same. Requires `setActionable(true)`. |
| Property change on `selected` | `setSelected(boolean)` + `getSelected()`; programmatic toggle via `cancelPendingClick()` if you need to suppress the chrome click handler mid-flight. |
| Card list selection: `CardSelectionListener<T>` (event-based) | `CardSelectionModel.addChangeListener(Consumer<CardSelectionModel<T>>)` — simpler functional callback. |
| Card list reorder: `CardReorderListener<T>` + `CardReorderEvent<T>` | `CardListModel.addChangeListener(Consumer<CardListModel<T>>)` — model-level change covers reorders, inserts, removes. |
| Card list data: `CardListDataListener` + `CardListDataEvent` | Same model-level change callback. |

If you depended on V1's fine-grained event types (insert vs remove vs reorder), V3 sends a single "model changed" notification — callers diff if they need that detail. The most common usage (rebuild a status line / counter after any change) is unchanged.

---

## 4. Cross-package class mapping

| V1 class | V3 equivalent |
|---|---|
| `com.owspfm.elwha.card.v1.ElwhaCard` | `com.owspfm.elwha.card.ElwhaCard` |
| `com.owspfm.elwha.card.v1.CardVariant` | `com.owspfm.elwha.card.CardVariant` — same values |
| `com.owspfm.elwha.card.v1.CardInteractionMode` | **DROPPED** — see §5 |
| `com.owspfm.elwha.card.v1.list.ElwhaCardList` | `com.owspfm.elwha.card.list.ElwhaCardList` |
| `com.owspfm.elwha.card.v1.list.DefaultCardListModel` | `com.owspfm.elwha.card.list.DefaultCardListModel` |
| `com.owspfm.elwha.card.v1.list.CardSelectionMode` | `com.owspfm.elwha.card.list.CardSelectionMode` — same values, plus `SINGLE_MANDATORY` (new in V3) |
| `com.owspfm.elwha.card.v1.list.CardSelectionModel` | `com.owspfm.elwha.card.list.CardSelectionModel` |
| `com.owspfm.elwha.card.v1.list.ReorderHandle` | **DROPPED** — V3 list drag uses the chassis itself (or a dedicated handle component the consumer adds and wires) |
| `com.owspfm.elwha.card.v1.list.CardListDataListener` / `CardSelectionListener` / `CardReorderListener` | All replaced by the simpler model-level `Consumer<...>` change listeners. |

New in V3:
- `com.owspfm.elwha.card.{CollapseRule, ExpansionOverflow, DividerStyle, ThumbnailShape}`
- `com.owspfm.elwha.card.{ElwhaCardHeader, ElwhaCardMedia, ElwhaCardActions, ElwhaCardDivider, ElwhaCardChevron, ElwhaCardExpandLink, ElwhaCardTitle, ElwhaCardSubtitle, ElwhaCardSupportingText, ElwhaCardLeadingIcon, ElwhaCardThumbnail}`

---

## 5. Common migration patterns

### Interaction mode decomposition

V1's `CardInteractionMode` baked four overlapping behaviors into a single enum. V3 splits them:

| V1 | V3 |
|---|---|
| `STATIC` | default — both `setActionable(false)` and `setSelectable(false)` |
| `HOVERABLE` | always-on for actionable / selectable cards; standalone "hoverable only" isn't a V3 concept |
| `CLICKABLE` | `setActionable(true)` + `addActionListener(...)` |
| `SELECTABLE` | `setSelectable(true)` + (usually) `setActionable(true)` so the chassis is a click target |
| `CLICKABLE + SELECTABLE` | `setActionable(true).setSelectable(true)` — both fire on click |

### Per-child collapse constraints

V1's `setCollapsedSummary(JComponent)` installed a single component that swapped in when collapsed. V3 inverts: every child of the chassis is collapsible by default; you pin specific children with `setCollapseConstraint(child, CollapseRule.ALWAYS_VISIBLE)`. The header + a summary line stay visible; everything else hides.

```java
ElwhaCardHeader header = new ElwhaCardHeader().setTitle("Title");
card.add(header);
card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);   // header pinned

card.add(new ElwhaCardSupportingText("Summary line that stays visible"));
// no constraint = COLLAPSIBLE = hides when collapsed

card.add(new ElwhaCardSupportingText("Detail one"));
card.add(new ElwhaCardSupportingText("Detail two"));
```

### Cycle-style card with leading thumbnail

V1's common "leading icon + title + subtitle" pattern translates 1-for-1:

```java
// V1
new ElwhaCard().setHeader("REI", "Your order has shipped", myThumbnailIcon);

// V3
ElwhaCard card = ElwhaCard.outlinedCard();
card.add(new ElwhaCardHeader()
    .setLeading(new ElwhaCardThumbnail(myImage))   // ElwhaCardThumbnail for image; ElwhaCardLeadingIcon for Icon
    .setTitle("REI")
    .setSubtitle("Your order has shipped"));
```

---

## 6. What's gone with no replacement (and why)

| V1 surface | Why no V3 equivalent |
|---|---|
| `setCornerRadius(Integer)` / `getEffectiveCornerRadius()` | V3 corner radius is theme-token-driven (`ShapeScale.MD` for cards) so visual coherence with the rest of the design system is automatic. Per-card overrides would re-introduce the escape-hatch problem V3 is solving. If a card needs a different shape, change the token. |
| `setBorderColor(Color)` / `setBorderWidth(int)` | Same — theme decides border treatment per variant. |
| `setSurfaceColor(Color)` | Same — variant + theme decide. |
| `setKeepSummaryWhenExpanded(boolean)` | Subsumed by the per-child `ALWAYS_VISIBLE` collapse rule, which is more general. |
| `setLeadingActions(Component...)` | V3 header has only leading-icon + title-stack + trailing-affordances. Leading actions on the action row use `ElwhaCardActions.addLeading(...)`. |
| `getTitleLabel()` / `getSubtitleLabel()` returning raw `JLabel` | V3 exposes typed `ElwhaCardTitle` / `ElwhaCardSubtitle` (both extend `JLabel`) via `ElwhaCardHeader.getTitle()` / `.getSubtitle()`. Cast is unnecessary; the typed wrapper handles disabled-fade and HTML-wrap. |
| `ReorderHandle` enum | V3 list drag uses the chassis itself by default. If a consumer wants a dedicated handle, add it as a normal child and wire its mouse listeners. |

If a V1 capability you depend on isn't covered here, open an issue against [OWS-PFMS/elwha](https://github.com/OWS-PFMS/elwha) before working around it — V3 spec gaps get tracked and fixed, not papered over with escape-hatches.

---

## Reference

- V3 spec: [`docs/research/elwha-card-v3-spec.md`](../research/elwha-card-v3-spec.md)
- V3 playground (visual reference for every pattern in this doc): `mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"`
- V1 source (read-only reference; ships in 0.2.0, deleted in 1.0.0): `src/com/owspfm/elwha/card/v1/`
