# ElwhaMenu — M3 Expressive vertical menu

A temporary, light-dismissed **popover** anchored to a trigger that lists choices as dedicated
`ElwhaMenuItem` rows. The general-purpose menu Elwha was missing — distinct from the FAB Menu
(#185). Epic [#298](https://github.com/OWS-PFMS/elwha/issues/298), Phase 1.

Design decisions: [`docs/research/elwha-menu-design.md`](../../../../../../../docs/research/elwha-menu-design.md).
Full M3 spec capture: `docs/research/elwha-menu-research.md`.

## What ships (V1)

Flat (non-nested) Expressive vertical menu · dedicated `ElwhaMenuItem` (+ swappable slot) · anchored
overlay host with flip + light-dismiss (outside-click / Esc / focus-loss) · full keyboard + a11y ·
`ColorStyle.STANDARD` **and** `VIBRANT` · `Layout.STANDARD` **and** `GROUPED` (gap / divider) ·
`SelectionMode.NONE` / `SINGLE` / `MULTI` · default token theming + dark mode · a Showcase leaf.

**Out of scope (V2 / later epics):** submenus (V2, #322), the baseline square menu (#323),
exposed-dropdown / filtering, a right-click context-menu binding helper, density levels.

## Quick start

```java
ElwhaMenu menu = ElwhaMenu.builder()
    .addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename"))
    .addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"))
    .onClose(cause -> System.out.println("closed: " + cause))
    .build();

ElwhaIconButton overflow = new ElwhaIconButton(MaterialIcons.moreVert(24));
overflow.addActionListener(e -> menu.open(overflow));
```

Opening a menu by clicking another trigger light-dismisses the current one on that same press, so
only one menu is ever open — no bookkeeping needed.

`open(anchor)` anchors the menu below the trigger, leading-aligned, and flips above / shifts
horizontally to stay inside the host window. The menu mounts at `JLayeredPane.POPUP_LAYER` (300), so a
menu opened from inside an `ElwhaDialog` tops it.

## Grouping

```java
ElwhaMenu.builder()
    .addItem(ElwhaMenuItem.of(MaterialIcons.home(20), "Home"))
    .addItem(ElwhaMenuItem.of(MaterialIcons.star(20), "Starred"))
    .addGroup()                                   // a group break
    .addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"))
    .separator(Separator.GAP)                     // GAP (rounded cards) | DIVIDER (subtle line)
    .build();
```

`addGroup()` switches the layout to `GROUPED`. `Separator.GAP` is the expressive default (each group
is a rounded card); `Separator.DIVIDER` draws a subtle line. A menu taller than the window scrolls
with a **persistent scrollbar** and is forced to `DIVIDER` — M3 forbids gaps in a scrollable menu.

## `ElwhaMenuItem` anatomy

`[leading icon?] · label (+ supporting text?) · [badge?] · [trailing text?] · [trailing icon?]`

```java
ElwhaMenuItem copy = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
copy.setTrailingText("⌘⌫");            // keyboard command
copy.setSupportingText("Move to trash"); // optional second line
copy.setBadge(ElwhaBadge.large("New")); // inline trailing badge
copy.setSlot(myImagePanel);             // swap the label region for display content
```

Tokens (research §I, **zero new**): 44 dp visual / **48 dp** interactive target, 16 / 8 / 12 dp
insets, 20 dp icons, label `LABEL_LARGE`, supporting `BODY_SMALL`, trailing `LABEL_LARGE`. A slot is
**display-only** — per the M3 slot-accessibility rule it must not host a second interactive control.

## States

Enabled · Disabled (38 % dim, **focusable-but-inert** — kept in the focus order, not removed) ·
Hovered (`ON_SURFACE` state layer) · Focused (3 dp inset `SECONDARY` ring) · Pressed (ripple) ·
Selected (`TERTIARY_CONTAINER` fill + a `ON_TERTIARY_CONTAINER` ✓ checkmark — a 3:1 + non-color cue).

## Keyboard & focus

The **menu surface owns a single keyboard focus** (`AccessibleRole.POPUP_MENU`); items are
non-focusable and carry a roving "focused" ring.

| Key | Action |
|---|---|
| Up / Down | move focus (wraps) |
| Home / End | first / last item |
| letters / digits | type-ahead to the next matching item |
| Enter / Space | activate the focused item (same as a click — fires / selects / toggles per the mode) |
| Esc | close |

Initial focus is the first item; focus restores to the trigger on an intentional close
(Esc / selection / programmatic) but **not** on a focus-loss / outside-press close. Disabled items
can be focused but not activated.

## Color (`ColorStyle`)

Two schemes, both with a Level-3 shadow and `ShapeScale.MD` corners; light and dark use the same
roles, so dark mode is free via `ElwhaTheme`.

- **`STANDARD`** (default, surface-based, lower emphasis): container `SURFACE_CONTAINER_LOW`; label
  `ON_SURFACE`; leading icon / supporting / trailing `ON_SURFACE_VARIANT`; selected
  `TERTIARY_CONTAINER` / `ON_TERTIARY_CONTAINER`.
- **`VIBRANT`** (tertiary-tinted, higher emphasis — *use sparingly*): the whole surface tints
  `TERTIARY_CONTAINER` with `ON_TERTIARY_CONTAINER` content (label / icons / trailing / supporting /
  state layer); the selected item jumps to the bold `TERTIARY` fill with `ON_TERTIARY` content
  (including the ✓ checkmark), so selection stands out from container-tone to full-tone.

## Selection (`SelectionMode`)

By default a menu is an **action menu** — picking an item fires it and the menu closes, with no
persistent selection. Set a `SelectionMode` to make it a list:

```java
ElwhaMenu view = ElwhaMenu.builder()
    .selectionMode(SelectionMode.SINGLE)               // NONE (default) | SINGLE | MULTI
    .onSelectionChange(item -> applyView(item.getLabel()))
    .addItem(listItem)                                 // listItem.setSelected(true) for the initial pick
    .addItem(gridItem)
    .build();

List<ElwhaMenuItem> picked = view.getSelectedItems();  // read selection back
```

- **`NONE`** (default) — action menu; fires and closes, no selection held.
- **`SINGLE`** — one item at a time; selecting auto-deselects the prior and **closes** the menu
  (a `SELECTION` close that restores focus to the trigger). Set the initial pick with
  `item.setSelected(true)` before `build()`. Radio-like: the selected item reports
  `AccessibleState.SELECTED`.
- **`MULTI`** — toggles items; the menu **stays open** until dismissed (light-dismiss / Esc), so
  several can be checked in one pass. Checkbox-like: each selected item reports `CHECKED` *and*
  `SELECTED`.

Mouse and keyboard (Enter / Space) drive selection identically. The selected visual is the
`TERTIARY_CONTAINER` / Vibrant bold-`TERTIARY` fill + ✓ checkmark — so in a selection mode every item
reserves the leading check-column and toggling never reflows the row. Read selection back with
`getSelectedItems()` / `ElwhaMenuItem.isSelected()`, or observe it via `onSelectionChange`.

## Trigger

The menu **never mutates its trigger** — it opens and closes without touching the trigger's state.
M3's "trigger shows a pressed state while the menu is open" affordance is intentionally not faked via
the trigger's `selected` state (that corrupts a `SELECTABLE` toggle button, which already flips its
own selection on click). A faithful transient held-visual needs a dedicated button API the lib
doesn't have yet — a known gap, deferred to a future enhancement.

## Host & z-band

`ElwhaMenu` is built on `com.owspfm.elwha.overlay.AbstractElwhaOverlay` (extracted from the dialog
family, #325) in light-dismiss + anchored mode. The menu has no scrim; it tops dialogs
(`MODAL_LAYER`, 200) and Elwha overlays (`ElwhaLayers.OVERLAY_LAYER`, 190) at `POPUP_LAYER` (300).

## Terminology (M3 noun → API)

| M3 | API |
|---|---|
| Vertical menu | `ElwhaMenu` |
| Menu item | `ElwhaMenuItem` |
| Leading icon / label / supporting text / trailing text / trailing icon / badge / slot | `ElwhaMenuItem` slots |
| Standard / Grouped | `Layout.STANDARD` / `Layout.GROUPED` |
| Gap / Divider | `Separator.GAP` / `Separator.DIVIDER` |
| Standard / Vibrant color | `ColorStyle.STANDARD` / `ColorStyle.VIBRANT` |

## Showcase

The Elwha Showcase → Components → **Menu** leaf: a Workbench of live triggers configured by
Layout / Separator / Color controls, and a Gallery of static `renderPreview()` snapshots (Standard
and Vibrant).
```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
```
