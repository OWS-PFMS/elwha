# Component index

Every component in Elwha, its package, and the leaf where you can see it in **The Elwha
Showcase**. Run the Showcase from a checkout:

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
```

The Showcase is organized into three areas — **Foundations**, **Components** (grouped by family),
and **Containers** — and the "Leaf" column below names the entry to click.

API reference ships as a `javadoc` classifier alongside the main artifact; add it in your IDE from
`com.owspfm:elwha:1.0.0:javadoc`. Design docs are linked where one exists; they are maintainer
documents that record *why* a component is shaped the way it is, including its deliberate
divergences from the Material 3 spec.

## Foundations

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Design tokens | `theme.ColorRole`, `ShapeScale`, `SpaceScale`, `TypeRole`, `StateLayer` | The five token families every component paints against — see [Theming](theming.md). | Foundations → Color Roles, Type Scale |
| Theme install | `theme.ElwhaTheme`, `Config`, `Theme`, `Palette`, `Mode`, `Typography`, `PaletteLoader`, `MaterialPalettes` | The static install facade and the palette/typography model. | — |
| Surface | `surface.ElwhaSurface` | A token-native rounded surface primitive: a `JPanel` that paints a role-filled, round-rect, optionally outlined background. Most other components extend it. ([design](../research/elwha-surface-design.md)) | Components → Surface |
| Icons | `icons.MaterialIcons` | Lookups for the bundled Material Symbols (Rounded, weight 400, fill 0, 20 px optical size), themed through a shared foreground filter. Roughly 65 named glyphs, each with a sized overload, plus `get(name)` and `pair(name)` for the outline/fill toggle idiom. | Foundations → Icons |
| Raw Swing | — | Not a component — the sanity check that `JButton`, `JTextField`, scrollbars and friends inherit the theme. | Foundations → Swing Comps |

## Actions

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Button | `button.ElwhaButton` | The M3 Expressive text button: five emphasis variants (elevated / filled / filled-tonal / outlined / text), the XS–XL size scale, round or square shape, a clickable/selectable interaction axis, shape morph and ripple. ([design](../research/elwha-button-design.md)) | Components → Button |
| Button selection group | `button.ElwhaButtonSelectionGroup` | Mutual exclusion across a set of `ElwhaButton`s. | Components → Button |
| Icon button | `iconbutton.ElwhaIconButton` | The icon-only button with the same variant and interaction-mode contract as Button, plus a declarative `setIcons(resting, selected)` toggle. Drops into a `JToolBar`. ([design](../research/elwha-icon-button-design.md)) | Components → Icon Button |
| Icon button selection group | `iconbutton.ElwhaIconButtonSelectionGroup` | Mutual exclusion across a set of `ElwhaIconButton`s — the toolbar "radio" pattern. | Containers → Icon Button Group (mutex) |
| Button group | `buttongroup.ElwhaButtonGroup` | The M3 Expressive button group — an invisible layout + selection container composing `ElwhaButton` and `ElwhaIconButton` segments with the connected/positional treatment. ([design](../research/elwha-button-group-design.md)) | Components → Button Group; Containers → Button Group (mutex) |
| FAB | `fab.ElwhaFab` | The Floating Action Button, standard (icon-only) and extended (icon + label), across three sizes and six color styles, with the standard ↔ extended shape morph. ([design](../research/elwha-fab-design.md)) | Components → FAB |
| FAB anchor | `fab.ElwhaFabAnchor` | Placement primitive that floats a FAB over content at a screen-edge corner, scroll-aware. | Components → FAB |

## Selection controls

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Checkbox | `checkbox.ElwhaCheckbox` | The M3 checkbox: tri-state with an indeterminate dash, hand-stroked marks, error palette, label, 40 px state-layer field and 48 px minimum touch target. ([design](../research/elwha-checkbox-design.md)) | Components → Checkbox |
| Radio button | `radio.ElwhaRadioButton` | The M3 ring-and-dot single-select, matching the checkbox label/geometry/focus contract. ([design](../research/elwha-radiobutton-design.md)) | Components → Radio button |
| Radio group | `radio.ElwhaRadioGroup` | Non-visual mutual-exclusion controller over radio buttons, with arrow-key navigation and a roving tab stop. | Components → Radio button |
| Switch | `switches.ElwhaSwitch` | The M3 switch: a corner-full track with a handle that morphs across selection states, drag-to-toggle, and optional on-handle icons. ([design](../research/elwha-switch-design.md)) | Components → Switch |
| Chip | `chip.ElwhaChip` | A single-row leading-icon + text + optional trailing icon-button surface. Filled / outlined / ghost treatments under the four M3 chip-type factories (assist / filter / input / suggestion). ([design](../research/elwha-flatchip-rebuild.md)) | Components → Chip |

## Fields

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Text field | `textfield.ElwhaTextField` | The M3 text field: filled and outlined chrome, floating label, supporting and error text, icon / prefix / suffix slots, character counter, multiline. ([design](../research/elwha-textfield-design.md)) | Components → Text Field |
| Select field | `selectfield.ElwhaSelectField<T>` | The M3 exposed dropdown — a typed combo over `ElwhaMenu` that writes the chosen option's display text back into the field. Editable and multi-select modes, mutually exclusive. ([design](../research/elwha-selectfield-design.md)) | Components → Select Field |
| Slider | `slider.ElwhaSlider` | The M3 Expressive slider over a `BoundedRangeModel`: standard / centered / range variants, the XS–XL size scale, horizontal and vertical, stops, inset icon and value bubble. ([design](../research/elwha-slider-design.md)) | Components → Slider |
| Color picker | `colorpicker.ElwhaColorPicker` | The M3 picker grammar applied to color — swatch tiers, spectrum, wheel, slider/hex modes, opt-in alpha, eyedropper. ([design](../research/elwha-color-picker-design.md)) | Components → Color picker |
| Color picker dialog / popover | `colorpicker.ElwhaColorPickerDialog`, `ElwhaColorPickerPopover` | The picker staged modally with pending-until-OK semantics, or as an anchored light-dismiss popover. | Components → Color picker |

## Containers and surfaces

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Card | `card.ElwhaCard` | Card chrome — elevated / filled / outlined variants over `ElwhaSurface`, with actionability, selection and collapse. It owns no typed content slots. ([spec](../research/elwha-card-v3-spec.md)) | Components → Card |
| Card content primitives | `card.ElwhaCardHeader`, `ElwhaCardTitle`, `ElwhaCardSubtitle`, `ElwhaCardSupportingText`, `ElwhaCardLeadingIcon`, `ElwhaCardThumbnail`, `ElwhaCardMedia`, `ElwhaCardActions`, `ElwhaCardDivider`, `ElwhaCardChevron`, `ElwhaCardExpandLink` | The composition vocabulary you add into a card with `card.add(...)`. | Components → Card |
| Item list | `list.ElwhaItemList<T>` | One model-driven generic container that renders any domain type through an `ElwhaItemAdapter<T>`, with selection, drag-reorder, filter, sort, orientation, empty and loading states. The single successor to the former card-list and chip-list families. ([spec](../research/elwha-list-generification-spec.md)) | Containers → Card List, Chip List |
| List contract | `list.ElwhaList<T>` | The interface `ElwhaItemList` implements — orientation, gap, padding, empty / loading / filter / sort. | Containers → Card List, Chip List |
| List models | `list.ElwhaListModel<T>`, `DefaultElwhaListModel<T>`, `ElwhaSelectionModel<T>`, `DefaultElwhaSelectionModel<T>` | The model and selection-model contracts plus their default implementations. | Containers → Card List, Chip List |
| Side sheet | `sidesheet.ElwhaSideSheet` | The M3 side sheet: header, content and action-footer slots; standard (docked, reflowing) or modal (scrim); docked or detached posture; opt-in drag-to-dismiss and drag-to-resize. ([design](../research/elwha-side-sheet-design.md), [V2](../research/elwha-side-sheet-v2-design.md)) | Components → Side Sheet |

## Overlays

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Dialog | `dialog.ElwhaDialog` | The M3 Basic Dialog: a modal surface with typed anatomy slots (icon → headline → supporting text → content → actions), mounted on the host frame's layered pane rather than a separate window. ([design](../research/elwha-dialog-design.md)) | Components → Dialog |
| Full-screen dialog | `dialog.ElwhaFullScreenDialog` | The edge-to-edge modal for longer-form input flows and narrow frames. ([design](../research/elwha-fullscreen-dialog-design.md)) | Components → Dialog |
| Menu | `menu.ElwhaMenu`, `ElwhaMenuItem`, `ElwhaSubMenuItem` | The M3 Expressive vertical menu — anchored, light-dismiss, with grouping, selection modes, keyboard navigation, nested submenus and hover-driven corner morph. ([design](../research/elwha-menu-design.md), [V2](../research/elwha-menu-v2-design.md)) | Components → Menu |
| Tooltip | `tooltip.ElwhaTooltip` | The M3 tooltip in both flavors — the plain inverse-surface label and the rich card with subhead, supporting text and actions. ([design](../research/elwha-tooltip-design.md)) | Components → Tooltip |
| Overlay host | `overlay.AbstractElwhaOverlay` | The shared layered-pane base every overlay builds on: placement, focus, dismissal, and parent–child chaining. Library-internal; listed because it is the reason overlays behave consistently. | — |

## Navigation

| Component | Class | What it is | Leaf |
|---|---|---|---|
| App bar | `appbar.ElwhaAppBar` | The M3 Expressive app bar: leading navigation button, title and optional subtitle, trailing actions; small / medium-flexible / large-flexible variants with tonal lift and scroll-driven collapse. ([design](../research/elwha-appbar-design.md)) | Components → App Bar |
| Navigation rail | `navrail.ElwhaNavigationRail` | The vertical rail that docks to a leading edge: header chrome (menu button + FAB), primary destinations, trailing utility actions, collapsed ↔ expanded morph. ([design](../research/elwha-navigation-rail-design.md)) | Components → Navigation Rail |
| Rail destination | `navrail.ElwhaNavRailDestination` | One slot of the rail — the "rail button", with collapsed and expanded layouts and a badge slot. | Components → Nav Rail Destination |
| Tabs | `tabs.ElwhaTabs`, `ElwhaTab` | The M3 tab bar: primary and secondary variants, fixed and scrollable modes, a sliding indicator overlay, icons and badges. ([design](../research/elwha-tabs-design.md)) | Components → Tabs |

## Feedback

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Badge | `badge.ElwhaBadge` | The M3 badge in both variants — the 6 dp dot and the 16 dp pill with a 1–4 character label. ([design](../research/elwha-badge-design.md)) | Components → Badge |
| Badge anchor | `badge.ElwhaBadgeAnchor` | Attaches a badge to a host component implementing `badge.IconBearing`, at M3 anchor geometry. | Components → Badge |
| Progress indicators | `progress.ElwhaLinearProgressIndicator`, `ElwhaCircularProgressIndicator` | The M3 Expressive progress anatomy — active indicator, visible track, the 4 px track-active gap and the stop-indicator dot; determinate and indeterminate, flat and wavy. ([design](../research/elwha-progress-indicator-design.md)) | Components → Progress |
| Loading indicator | `loading.ElwhaLoadingIndicator` | The M3 Expressive shape-morph spinner — a filled rounded polygon rotating while morphing through the M3 shape library. Distinct from the progress indicators. ([design](../research/elwha-loading-indicator-design.md)) | Components → Loading |
