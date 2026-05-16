# ElwhaCard

A token-native, M3-aligned card primitive for Swing. Composes the M3 formal Card slots (`headline` / `subhead` / `supportingText` / `media` / `actions`), four documented OWS extension slots (leading icon, leading actions, trailing actions, disclosure axis), and an M3 top-trailing checked-icon overlay on selection. Background, border, and corner radius come from the inherited [`ElwhaSurface`](../surface/) chassis — Card never paints raw colors.

Spec: [`docs/research/elwha-card-v2-spec.md`](../../../../../docs/research/elwha-card-v2-spec.md).

## Quick start

```java
ElwhaCard card = ElwhaCard.elevatedCard("Recent activity")
    .setSubhead("Last 30 days")
    .setSupportingText("12 cycles found across 4 factors.")
    .setActions(new JButton("Open"), new JButton("Dismiss"))
    .setInteractionMode(CardInteractionMode.HOVERABLE);
```

Defaults: `CardVariant.ELEVATED`, `CardInteractionMode.STATIC`, `elevation = 1` (variant-derived), `ShapeScale.MD` (inherited from Surface), `SpaceScale.LG` padding on both axes.

## Variants — `CardVariant`

Each variant carries a surface role + resting elevation + dragged elevation + default border treatment, applied automatically on `setVariant(...)`.

| Variant            | Surface role               | Elevation | Dragged elevation | Border                  |
| ------------------ | -------------------------- | --------- | ----------------- | ----------------------- |
| `ELEVATED` (default) | `SURFACE_CONTAINER_LOW`     | 1 dp      | 2 dp              | none                    |
| `FILLED`           | `SURFACE_CONTAINER_HIGHEST` | 0 dp      | 8 dp              | none                    |
| `OUTLINED`         | `SURFACE`                   | 0 dp      | 8 dp              | `OUTLINE_VARIANT`, 1 px |

Per-variant static factories — discoverability shorthand:

```java
ElwhaCard.elevatedCard("...");
ElwhaCard.filledCard("...");
ElwhaCard.outlinedCard("...");
```

## Interaction modes — `CardInteractionMode`

| Mode         | Cursor  | Focusable | Fires `ActionEvent`         | Holds selection |
| ------------ | ------- | --------- | --------------------------- | --------------- |
| `STATIC`     | default | no        | no                          | no              |
| `HOVERABLE`  | hand    | no        | no                          | no              |
| `CLICKABLE`  | hand    | yes       | yes (click + Space / Enter) | no              |
| `SELECTABLE` | hand    | yes       | yes (toggle)                | yes             |

## Slot vocabulary

### M3 formal slots

```java
card.setHeadline("Project alpha");
card.setSubhead("Updated 2 minutes ago");
card.setSupportingText("Long-form text body — HTML-wrapped automatically.");
card.setMedia(new ChartPanel(...));
card.setActions(new JButton("Open"), new JButton("Dismiss"));
```

### OWS header extensions

```java
card.setLeadingIcon(MaterialIcons.info());
card.setLeadingActions(pinButton, anchorButton);
card.setTrailingActions(menuButton);
```

Each extension is justified against a concrete OWS use case in [the spec doc](../../../../../docs/research/elwha-card-v2-spec.md) §4.

### Disclosure axis

```java
card.setCollapsible(true);
card.setCollapsed(true);
card.setSummary(new JLabel("3 options hidden"));        // shown per visibility policy
card.setSummaryVisibility(SummaryVisibility.ALWAYS);    // or COLLAPSED_ONLY (default)
card.setAnimateCollapse(true);
```

`SummaryVisibility` replaces V1's `setKeepSummaryWhenExpanded(boolean)` escape hatch with a first-class enum.

## Token-bound chassis

All paint axes are token-typed; raw `Color` / `int` setters from V1 are gone.

```java
card.setSurfaceRole(ColorRole.SURFACE_CONTAINER);       // inherited from ElwhaSurface
card.setShape(ShapeScale.LG);                           // inherited
card.setBorderWidth(2);                                  // inherited
card.setPadding(SpaceScale.LG, SpaceScale.MD);          // Card-level (token-typed)
card.setElevation(3);                                    // 0..MAX_ELEVATION
```

Card is **variant-bearing**, so per [#62 doctrine §4](../../../../../docs/development/component-api-conventions.md), it does not advertise `setBorderRole(ColorRole)` — the border role is variant-derived. To opt into a different border, change the variant.

## Selection — M3 checked-icon overlay

`setSelected(true)` paints a 24 dp `PRIMARY`-filled circle in the top-trailing corner with an `ON_PRIMARY` `check` glyph centered inside. Variant- and interaction-mode-agnostic — visible on `STATIC` cards too (V1's surface-tint approach was not).

```java
card.setSelected(true);
card.addSelectionChangeListener(evt -> updateModel(card.isSelected()));
```

## Listeners

```java
card.addActionListener(evt -> openProject());                // CLICKABLE / SELECTABLE only
card.addSelectionChangeListener(evt -> ...);                 // PROPERTY_SELECTED
card.addExpansionChangeListener(evt -> ...);                 // PROPERTY_COLLAPSED
```

`addSelectionChangeListener` / `addExpansionChangeListener` are scoped property-change listeners per the [#62 doctrine](../../../../../docs/development/component-api-conventions.md) — they replace V1's generic `onChange(String, PCL)`.

## Drag plumbing

`ElwhaCardList<T>` uses `setDragged(boolean)` to switch the card to its variant's dragged elevation during a reorder operation, and `cancelPendingClick()` to suppress an in-flight header toggle once a drag commits.

## Carousel-readiness contract

Card cooperates with externally-imposed widths, exposes its `getShape()` for parent masking, and **never** installs internal scroll. Expansion grows the card; sibling layout reacts as the parent's `LayoutManager` allows. Spec §9 for the full contract.

## Migration from V1

| V1 method                                       | V2 replacement                                              |
| ----------------------------------------------- | ----------------------------------------------------------- |
| `setHeader(String)`                             | `setHeadline(String)`                                       |
| `setHeader(String, String)`                     | `setHeadline(...).setSubhead(...)`                          |
| `setHeader(String, String, Icon)`               | three independent setters (no silent leading-icon clearing) |
| `setBody(JComponent)`                           | `setSupportingText(String)` or `add(Component)` directly    |
| `setFooter(JComponent)` / `setFooter(...)`      | `setActions(Component...)`                                  |
| `setCornerRadius(Integer)`                      | `setShape(ShapeScale)` (inherited)                          |
| `getEffectiveCornerRadius()`                    | `getShape()` (inherited)                                    |
| `setPadding(Insets)` / `setPadding(int)`        | `setPadding(SpaceScale, SpaceScale)`                        |
| `setBorderColor(Color)`                         | variant change (border role is variant-derived)             |
| `setSurfaceColor(Color)`                        | `setSurfaceRole(ColorRole)` (inherited)                     |
| `setCollapsedSummary(JComponent)`               | `setSummary(JComponent)`                                    |
| `setKeepSummaryWhenExpanded(boolean)`           | `setSummaryVisibility(SummaryVisibility.ALWAYS)`            |
| `getTitleLabel()` / `getSubtitleLabel()`        | **dropped** — no replacement                                |
| `onChange(String, PCL)`                         | `addSelectionChangeListener` / `addExpansionChangeListener` |
| `CardVariant.GHOST` / `WARM_ACCENT`             | **dropped** — use one of the three V2 variants              |

## Playground

```bash
mvn -q compile exec:java \
  -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
```

Three top-level tabs: **ElwhaCard** (gallery + live-config + rendered snippet), **ElwhaCardList** (showcase), **Cursors** (drag-handle cursor reference).
