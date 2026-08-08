# Component index

Every component in Elwha, its package, and the leaf where you can see it in **The Elwha
Showcase**. Run the Showcase from a checkout:

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
```

The Showcase is organized into three areas — **Foundations**, **Components** (grouped by family),
and **Containers** — and the "Leaf" column below names the entry to click.

**API reference: [ows-pfms.github.io/elwha](https://ows-pfms.github.io/elwha/)**, redeployed on
every push to `main`. Every class name in the tables below links into it. The same Javadoc also
ships as a classifier on the artifact (`com.owspfm:elwha:1.0.0:javadoc`) if you would rather attach
it in your IDE.

Design docs are linked where one exists; they are maintainer documents that record *why* a
component is shaped the way it is, including its deliberate divergences from the Material 3 spec.

## Foundations

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Design tokens | [`ColorRole`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/ColorRole.html), [`ShapeScale`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/ShapeScale.html), [`SpaceScale`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/SpaceScale.html), [`TypeRole`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/TypeRole.html), [`StateLayer`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/StateLayer.html) | The five token families every component paints against — see [Theming](theming.md). | Foundations → Color Roles, Type Scale |
| Theme install | [`ElwhaTheme`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/ElwhaTheme.html), [`Config`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Config.html), [`Theme`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Theme.html), [`Palette`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Palette.html), [`Mode`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Mode.html), [`Typography`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Typography.html), [`PaletteLoader`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/PaletteLoader.html), [`MaterialPalettes`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/MaterialPalettes.html) | The static install facade and the palette/typography model. | — |
| Surface | [`ElwhaSurface`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/surface/ElwhaSurface.html) | A token-native rounded surface primitive: a `JPanel` that paints a role-filled, round-rect, optionally outlined background. Most other components extend it. ([design](../research/elwha-surface-design.md)) | Components → Surface |
| Icons | [`MaterialIcons`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/icons/MaterialIcons.html) | Lookups for the bundled Material Symbols (Rounded, weight 400, fill 0, 20 px optical size), themed through a shared foreground filter. Roughly 65 named glyphs, each with a sized overload, plus `get(name)` and `pair(name)` for the outline/fill toggle idiom. | Foundations → Icons |
| Raw Swing | — | Not a component — the sanity check that `JButton`, `JTextField`, scrollbars and friends inherit the theme. | Foundations → Swing Comps |

## Actions

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Button | [`button.ElwhaButton`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/button/ElwhaButton.html) | The M3 Expressive text button: five emphasis variants (elevated / filled / filled-tonal / outlined / text), the XS–XL size scale, round or square shape, a clickable/selectable interaction axis, shape morph and ripple. ([design](../research/elwha-button-design.md)) | Components → Button |
| Button selection group | [`button.ElwhaButtonSelectionGroup`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/button/ElwhaButtonSelectionGroup.html) | Mutual exclusion across a set of `ElwhaButton`s. | Components → Button |
| Icon button | [`iconbutton.ElwhaIconButton`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/iconbutton/ElwhaIconButton.html) | The icon-only button with the same variant and interaction-mode contract as Button, plus a declarative `setIcons(resting, selected)` toggle. Drops into a `JToolBar`. ([design](../research/elwha-icon-button-design.md)) | Components → Icon Button |
| Icon button selection group | [`iconbutton.ElwhaIconButtonSelectionGroup`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/iconbutton/ElwhaIconButtonSelectionGroup.html) | Mutual exclusion across a set of `ElwhaIconButton`s — the toolbar "radio" pattern. | Containers → Icon Button Group (mutex) |
| Button group | [`buttongroup.ElwhaButtonGroup`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/buttongroup/ElwhaButtonGroup.html) | The M3 Expressive button group — an invisible layout + selection container composing `ElwhaButton` and `ElwhaIconButton` segments with the connected/positional treatment. ([design](../research/elwha-button-group-design.md)) | Components → Button Group; Containers → Button Group (mutex) |
| FAB | [`fab.ElwhaFab`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/fab/ElwhaFab.html) | The Floating Action Button, standard (icon-only) and extended (icon + label), across three sizes and six color styles, with the standard ↔ extended shape morph. ([design](../research/elwha-fab-design.md)) | Components → FAB |
| FAB anchor | [`fab.ElwhaFabAnchor`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/fab/ElwhaFabAnchor.html) | Placement primitive that floats a FAB over content at a screen-edge corner, scroll-aware. | Components → FAB |

## Selection controls

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Checkbox | [`checkbox.ElwhaCheckbox`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/checkbox/ElwhaCheckbox.html) | The M3 checkbox: tri-state with an indeterminate dash, hand-stroked marks, error palette, label, 40 px state-layer field and 48 px minimum touch target. ([design](../research/elwha-checkbox-design.md)) | Components → Checkbox |
| Radio button | [`radio.ElwhaRadioButton`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/radio/ElwhaRadioButton.html) | The M3 ring-and-dot single-select, matching the checkbox label/geometry/focus contract. ([design](../research/elwha-radiobutton-design.md)) | Components → Radio button |
| Radio group | [`radio.ElwhaRadioGroup`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/radio/ElwhaRadioGroup.html) | Non-visual mutual-exclusion controller over radio buttons, with arrow-key navigation and a roving tab stop. | Components → Radio button |
| Switch | [`switches.ElwhaSwitch`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/switches/ElwhaSwitch.html) | The M3 switch: a corner-full track with a handle that morphs across selection states, drag-to-toggle, and optional on-handle icons. ([design](../research/elwha-switch-design.md)) | Components → Switch |
| Chip | [`chip.ElwhaChip`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/chip/ElwhaChip.html) | A single-row leading-icon + text + optional trailing icon-button surface. Filled / outlined / ghost treatments under the four M3 chip-type factories (assist / filter / input / suggestion). ([design](../research/elwha-flatchip-rebuild.md)) | Components → Chip |

## Fields

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Text field | [`textfield.ElwhaTextField`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/textfield/ElwhaTextField.html) | The M3 text field: filled and outlined chrome, floating label, supporting and error text, icon / prefix / suffix slots, character counter, multiline. ([design](../research/elwha-textfield-design.md)) | Components → Text Field |
| Select field | [`selectfield.ElwhaSelectField<T>`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/selectfield/ElwhaSelectField.html) | The M3 exposed dropdown — a typed combo over `ElwhaMenu` that writes the chosen option's display text back into the field. Editable and multi-select modes, mutually exclusive. ([design](../research/elwha-selectfield-design.md)) | Components → Select Field |
| Slider | [`slider.ElwhaSlider`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/slider/ElwhaSlider.html) | The M3 Expressive slider over a `BoundedRangeModel`: standard / centered / range variants, the XS–XL size scale, horizontal and vertical, stops, inset icon and value bubble. ([design](../research/elwha-slider-design.md)) | Components → Slider |
| Color picker | [`colorpicker.ElwhaColorPicker`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/colorpicker/ElwhaColorPicker.html) | The M3 picker grammar applied to color — swatch tiers, spectrum, wheel, slider/hex modes, opt-in alpha, eyedropper. ([design](../research/elwha-color-picker-design.md)) | Components → Color picker |
| Color picker dialog / popover | [`colorpicker.ElwhaColorPickerDialog`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/colorpicker/ElwhaColorPickerDialog.html), [`ElwhaColorPickerPopover`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/colorpicker/ElwhaColorPickerPopover.html) | The picker staged modally with pending-until-OK semantics, or as an anchored light-dismiss popover. | Components → Color picker |

## Containers and surfaces

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Card | [`card.ElwhaCard`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/card/ElwhaCard.html) | Card chrome — elevated / filled / outlined variants over `ElwhaSurface`, with actionability, selection and collapse. It owns no typed content slots. ([spec](../research/elwha-card-v3-spec.md)) | Components → Card |
| Card content primitives | [`card.ElwhaCardHeader`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/card/ElwhaCardHeader.html) and its siblings — `ElwhaCardTitle`, `ElwhaCardSubtitle`, `ElwhaCardSupportingText`, `ElwhaCardLeadingIcon`, `ElwhaCardThumbnail`, `ElwhaCardMedia`, `ElwhaCardActions`, `ElwhaCardDivider`, `ElwhaCardChevron`, `ElwhaCardExpandLink` ([all](https://ows-pfms.github.io/elwha/com/owspfm/elwha/card/package-summary.html)) | The composition vocabulary you add into a card with `card.add(...)`. | Components → Card |
| Item list | [`list.ElwhaItemList<T>`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/list/ElwhaItemList.html) | One model-driven generic container that renders any domain type through an [`ElwhaItemAdapter<T>`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/list/ElwhaItemAdapter.html), with selection, drag-reorder, filter, sort, orientation, empty and loading states. The single successor to the former card-list and chip-list families. ([spec](../research/elwha-list-generification-spec.md)) | Containers → Card List, Chip List |
| List contract | [`list.ElwhaList<T>`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/list/ElwhaList.html) | The interface `ElwhaItemList` implements — orientation, gap, padding, empty / loading / filter / sort. | Containers → Card List, Chip List |
| List models | [`list.ElwhaListModel<T>`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/list/ElwhaListModel.html), [`ElwhaSelectionModel<T>`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/list/ElwhaSelectionModel.html), and their `Default…` implementations | The model and selection-model contracts plus their default implementations. | Containers → Card List, Chip List |
| Side sheet | [`sidesheet.ElwhaSideSheet`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/sidesheet/ElwhaSideSheet.html) | The M3 side sheet: header, content and action-footer slots; standard (docked, reflowing) or modal (scrim); docked or detached posture; opt-in drag-to-dismiss and drag-to-resize. ([design](../research/elwha-side-sheet-design.md), [V2](../research/elwha-side-sheet-v2-design.md)) | Components → Side Sheet |

## Overlays

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Dialog | [`dialog.ElwhaDialog`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/dialog/ElwhaDialog.html) | The M3 Basic Dialog: a modal surface with typed anatomy slots (icon → headline → supporting text → content → actions), mounted on the host frame's layered pane rather than a separate window. ([design](../research/elwha-dialog-design.md)) | Components → Dialog |
| Full-screen dialog | [`dialog.ElwhaFullScreenDialog`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/dialog/ElwhaFullScreenDialog.html) | The edge-to-edge modal for longer-form input flows and narrow frames. ([design](../research/elwha-fullscreen-dialog-design.md)) | Components → Dialog |
| Menu | [`menu.ElwhaMenu`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/menu/ElwhaMenu.html), [`ElwhaMenuItem`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/menu/ElwhaMenuItem.html), `ElwhaSubMenuItem` | The M3 Expressive vertical menu — anchored, light-dismiss, with grouping, selection modes, keyboard navigation, nested submenus and hover-driven corner morph. ([design](../research/elwha-menu-design.md), [V2](../research/elwha-menu-v2-design.md)) | Components → Menu |
| Tooltip | [`tooltip.ElwhaTooltip`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/tooltip/ElwhaTooltip.html) | The M3 tooltip in both flavors — the plain inverse-surface label and the rich card with subhead, supporting text and actions. ([design](../research/elwha-tooltip-design.md)) | Components → Tooltip |
| Overlay host | [`overlay.AbstractElwhaOverlay`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/overlay/AbstractElwhaOverlay.html) | The shared layered-pane base every overlay builds on: placement, focus, dismissal, and parent–child chaining. Library-internal; listed because it is the reason overlays behave consistently. | — |

## Navigation

| Component | Class | What it is | Leaf |
|---|---|---|---|
| App bar | [`appbar.ElwhaAppBar`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/appbar/ElwhaAppBar.html) | The M3 Expressive app bar: leading navigation button, title and optional subtitle, trailing actions; small / medium-flexible / large-flexible variants with tonal lift and scroll-driven collapse. ([design](../research/elwha-appbar-design.md)) | Components → App Bar |
| Navigation rail | [`navrail.ElwhaNavigationRail`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/navrail/ElwhaNavigationRail.html) | The vertical rail that docks to a leading edge: header chrome (menu button + FAB), primary destinations, trailing utility actions, collapsed ↔ expanded morph. ([design](../research/elwha-navigation-rail-design.md)) | Components → Navigation Rail |
| Rail destination | [`navrail.ElwhaNavRailDestination`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/navrail/ElwhaNavRailDestination.html) | One slot of the rail — the "rail button", with collapsed and expanded layouts and a badge slot. | Components → Nav Rail Destination |
| Tabs | [`tabs.ElwhaTabs`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/tabs/ElwhaTabs.html), [`ElwhaTab`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/tabs/ElwhaTab.html) | The M3 tab bar: primary and secondary variants, fixed and scrollable modes, a sliding indicator overlay, icons and badges. ([design](../research/elwha-tabs-design.md)) | Components → Tabs |

## Feedback

| Component | Class | What it is | Leaf |
|---|---|---|---|
| Badge | [`badge.ElwhaBadge`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/badge/ElwhaBadge.html) | The M3 badge in both variants — the 6 dp dot and the 16 dp pill with a 1–4 character label. ([design](../research/elwha-badge-design.md)) | Components → Badge |
| Badge anchor | [`badge.ElwhaBadgeAnchor`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/badge/ElwhaBadgeAnchor.html) | Attaches a badge to a host component implementing [`badge.IconBearing`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/badge/IconBearing.html), at M3 anchor geometry. | Components → Badge |
| Progress indicators | [`progress.ElwhaLinearProgressIndicator`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/progress/ElwhaLinearProgressIndicator.html), [`ElwhaCircularProgressIndicator`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/progress/ElwhaCircularProgressIndicator.html) | The M3 Expressive progress anatomy — active indicator, visible track, the 4 px track-active gap and the stop-indicator dot; determinate and indeterminate, flat and wavy. ([design](../research/elwha-progress-indicator-design.md)) | Components → Progress |
| Loading indicator | [`loading.ElwhaLoadingIndicator`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/loading/ElwhaLoadingIndicator.html) | The M3 Expressive shape-morph spinner — a filled rounded polygon rotating while morphing through the M3 shape library. Distinct from the progress indicators. ([design](../research/elwha-loading-indicator-design.md)) | Components → Loading |
