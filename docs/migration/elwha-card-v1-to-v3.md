# ElwhaCard V1 → V3 migration map

**For:** OWS-PFMS/OWS-Local-Search-GUI implementors (and any other consumer with a V1 card site to convert).

V1 is the pre-Elwha-theme card: `FlatCard` in the `0.1.0` artifact, renamed to `com.owspfm.elwha.card.v1.*` on `main` during the 0.x window and **deleted before 1.0.0**. It was never published under the `v1` package name — the planned `0.2.0` release was cancelled — so no resolvable artifact carries both shapes, and V3 is simply what `com.owspfm.elwha.card.*` has meant since 1.0.0.

Read this as a translation table for source you still have, not for a dependency you can still resolve against. It is the per-setter recipe for converting one card site at a time.

> **Card half vs list half.** §1–§2 and §5–§6 are about the card and stand as written. §3 and §4 also cover the *card list*, which took a second, larger move after this doc was written: the parallel `card/list/` and `chip/list/` families were collapsed into one generic `com.owspfm.elwha.list` package. A V1 card **list** therefore migrates onto `ElwhaItemList<T>`, not onto anything under `card/` — see §4a.

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

| V1 | Now |
|---|---|
| `addActionListener(ActionListener)` | `addActionListener(ActionListener)` — same. Requires `setActionable(true)`. |
| Property change on `selected` | `card.addPropertyChangeListener(ElwhaCard.PROPERTY_SELECTED, listener)`; `setSelected(boolean)` + `isSelected()`. Programmatic toggle via `cancelPendingClick()` if you need to suppress the chrome click handler mid-flight. |
| Card list selection: `CardSelectionListener<T>` | `ElwhaItemList.addSelectionListener(ElwhaSelectionListener<T>)`, delivering an `ElwhaSelectionEvent<T>` whose `getSelected()` is the whole selection. |
| Card list reorder: `CardReorderListener<T>` + `CardReorderEvent<T>` | `ElwhaItemList.addReorderListener(ElwhaReorderListener<T>)` + `ElwhaReorderEvent<T>` — `getItem()`, `getFromIndex()`, `getToIndex()`. |
| Card list data: `CardListDataListener` + `CardListDataEvent` | `ElwhaListModel.addListDataListener(ElwhaListDataListener<T>)` + `ElwhaListDataEvent<T>`, typed `ADDED` / `REMOVED` / `CHANGED` / `MOVED` with `getIndex0()` / `getIndex1()`. |

The fine-grained event vocabulary survived the move. If you depended on telling an insert from a remove from a reorder, you still can — reorder has its own listener and event type, and list-data changes carry a `Type` discriminator. Subscribing to the *list* gets you selection and reorder; subscribing to the *model* gets you data changes.

---

## 4. Cross-package class mapping

| V1 class | Shipped equivalent |
|---|---|
| `com.owspfm.elwha.card.v1.ElwhaCard` | `com.owspfm.elwha.card.ElwhaCard` |
| `com.owspfm.elwha.card.v1.CardVariant` | `com.owspfm.elwha.card.CardVariant` — same values |
| `com.owspfm.elwha.card.v1.CardInteractionMode` | **DROPPED** — see §5 |
| `com.owspfm.elwha.card.v1.list.ElwhaCardList` | `com.owspfm.elwha.list.ElwhaItemList<T>` — one generic list for every item type; see §4a |
| `com.owspfm.elwha.card.v1.list.CardListModel` / `DefaultCardListModel` | `com.owspfm.elwha.list.ElwhaListModel<T>` / `DefaultElwhaListModel<T>` |
| `com.owspfm.elwha.card.v1.list.CardSelectionMode` | `com.owspfm.elwha.list.SelectionMode` — `NONE` / `SINGLE` / `SINGLE_MANDATORY` / `MULTIPLE` |
| `com.owspfm.elwha.card.v1.list.CardSelectionModel` | `com.owspfm.elwha.list.ElwhaSelectionModel<T>` / `DefaultElwhaSelectionModel<T>` |
| `com.owspfm.elwha.card.v1.list.ReorderHandle` | **DROPPED** — drag is configured on the list via `MovementMode` + `ReorderAffordance`, not by a handle enum on the card |
| `com.owspfm.elwha.card.v1.list.CardListDataListener` / `CardSelectionListener` / `CardReorderListener` | `com.owspfm.elwha.list.ElwhaListDataListener<T>` / `ElwhaSelectionListener<T>` / `ElwhaReorderListener<T>` — see §3 |

Note the package: **every list type moved out of `card/` entirely.** There is no `com.owspfm.elwha.card.list` package. The parallel card-list and chip-list families were collapsed into one generic `com.owspfm.elwha.list`, so the same container hosts cards, chips, or any `JComponent` you render.

New alongside V3:
- `com.owspfm.elwha.card.{CollapseRule, ExpansionOverflow, DividerStyle, ThumbnailShape}`
- `com.owspfm.elwha.card.{ElwhaCardHeader, ElwhaCardMedia, ElwhaCardActions, ElwhaCardDivider, ElwhaCardChevron, ElwhaCardExpandLink, ElwhaCardTitle, ElwhaCardSubtitle, ElwhaCardSupportingText, ElwhaCardLeadingIcon, ElwhaCardThumbnail}`
- `com.owspfm.elwha.list.{ElwhaItemList, ElwhaItemAdapter, ElwhaList, ElwhaListModel, DefaultElwhaListModel, ElwhaSelectionModel, DefaultElwhaSelectionModel, SelectionMode, MovementMode, ReorderAffordance, IconAffordance, ElwhaListItemView, ElwhaCursors}`

---

## 4a. Card list → `ElwhaItemList<T>`

The structural change is bigger than a rename, and it is worth understanding before converting a list site: **the list no longer holds cards.** It holds your domain items, and an adapter turns each one into a view on demand.

```java
// V1 — the list held ElwhaCards, and you built them up front
ElwhaCardList<Report> list = new ElwhaCardList<>();
list.setModel(new DefaultCardListModel<>(reports));
list.setSelectionMode(CardSelectionMode.SINGLE);

// Now — the list holds Reports; the adapter makes a view for each
ElwhaItemList<Report> list = new ElwhaItemList<>(
    new DefaultElwhaListModel<>(reports),
    (report, visibleIndex) -> {
      ElwhaCard card = ElwhaCard.outlinedCard();
      card.add(new ElwhaCardHeader()
          .setTitle(report.title())
          .setSubtitle(report.updatedAt()));
      return card;
    });
list.setSelectionMode(SelectionMode.SINGLE);
```

`ElwhaItemAdapter<T>` is a single-method interface — `JComponent componentFor(T item, int visibleIndex)` — so a lambda is the usual form. The `T` parameter on the list is the **item** type, not the view type; that is the sharpest difference from V1, where the list was typed by the card.

Selection is by value, not by index or by card: `getSelectionModel().getSelected()` hands back `List<T>` of your items.

The view you return may implement `com.owspfm.elwha.list.ElwhaListItemView` to receive selection state and drag chrome. `ElwhaCard` and `ElwhaChip` both do, so a card view gets selection styling for free; a plain `JComponent` still renders and lays out correctly, it just doesn't restyle itself on selection.

Drag-reorder is two settings on the list rather than a per-card handle:

```java
list.setMovementMode(MovementMode.MOVABLE);              // STATIC / MOVABLE / PINNED
list.setReorderAffordance(ReorderAffordance.CURSOR_SWAP); // CURSOR_SWAP / HOVER_ICON / BOTH
list.addReorderListener(event ->
    persistOrder(event.getItem(), event.getFromIndex(), event.getToIndex()));
```

If you are hand-building a draggable surface *outside* a list and want the same pointer feedback, `ElwhaCursors.grab()` / `grabbing()` are the cursors the list itself wears.

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

### Action-button pairing

Card action rows take their buttons via `ElwhaCardActions.addLeading` / `addTrailing`. `ElwhaCardActions` accepts any `JComponent`, so a raw `JButton` still works — but `ElwhaButton` is the token-native, M3-correct choice (and the only path to the Filled-tonal variant). M3 spec §3.3 pairs each card variant with a specific secondary + primary button variant; match the card to its pairing so the action row reads at the right emphasis:

| Card variant | Secondary CTA | Primary CTA |
|---|---|---|
| Elevated | `ElwhaButton.outlinedButton(...)` | `ElwhaButton.filledButton(...)` |
| Filled | `ElwhaButton.textButton(...)` | `ElwhaButton.outlinedButton(...)` |
| Outlined | `ElwhaButton.textButton(...)` | `ElwhaButton.filledTonalButton(...)` |

```java
ElwhaCard card = ElwhaCard.elevatedCard();
card.add(new ElwhaCardHeader().setTitle("Title"));
card.add(new ElwhaCardSupportingText("Body text."));
card.add(new ElwhaCardDivider());
card.add(new ElwhaCardActions()
    .addTrailing(ElwhaButton.outlinedButton("Cancel"))   // secondary — left of primary
    .addTrailing(ElwhaButton.filledButton("Confirm")));   // primary — rightmost
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
| `ReorderHandle` enum | Drag became a property of the list, not of the card: `ElwhaItemList.setMovementMode(...)` says whether an item moves, `setReorderAffordance(...)` says how that is advertised (pointer swap, hover icon, or both). A per-card handle enum has nothing left to configure. |

If a V1 capability you depend on isn't covered here, open an issue against [OWS-PFMS/elwha](https://github.com/OWS-PFMS/elwha) before working around it — V3 spec gaps get tracked and fixed, not papered over with escape-hatches.

---

## Reference

- Card V3 spec: [`docs/research/elwha-card-v3-spec.md`](../research/elwha-card-v3-spec.md)
- List unification spec (why the card list became `ElwhaItemList<T>`): [`docs/research/elwha-list-generification-spec.md`](../research/elwha-list-generification-spec.md)
- Card playground (visual reference for every pattern in this doc): `mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"`
- The shipped API, class by class: the [published javadoc](https://ows-pfms.github.io/elwha/), and `docs/consumer/components.md` for the catalogue
- V1 source: not in the tree and not in any 1.x artifact. It is still in history — `git log --all -- '*card/v1/ElwhaCard.java'` finds it, and the `v0.1.0` tag carries the same shape under its original name at `src/com/owspfm/ui/components/card/FlatCard.java`.
