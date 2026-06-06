# ElwhaMenu — M3 Expressive vertical menu

A temporary, light-dismissed **popover** anchored to a trigger that lists choices as dedicated
`ElwhaMenuItem` rows. The general-purpose menu Elwha was missing — distinct from the FAB Menu
(#185). Epic [#298](https://github.com/OWS-PFMS/elwha/issues/298), Phase 1.

Design decisions: [`docs/research/elwha-menu-design.md`](../../../../../../../docs/research/elwha-menu-design.md).
Full M3 spec capture: `docs/research/elwha-menu-research.md`.

## What ships (Phase 1)

Flat (non-nested) Expressive vertical menu · dedicated `ElwhaMenuItem` (+ swappable slot) · anchored
overlay host with flip + light-dismiss (outside-click / Esc / focus-loss) · full keyboard + a11y ·
`ColorStyle.STANDARD` · `Layout.STANDARD` **and** `GROUPED` (gap / divider) · default token theming +
dark mode · a Showcase leaf.

**Later phases / out of scope:** submenus (V2, #322), the baseline square menu (#323), the
`VIBRANT` color style (S5), `SelectionMode` SINGLE/MULTI (S6), exposed-dropdown / filtering, a
right-click context-menu binding helper, density levels.

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
| Enter / Space | activate the focused item (fires its action, closes) |
| Esc | close |

Initial focus is the first item; focus restores to the trigger on an intentional close
(Esc / selection / programmatic) but **not** on a focus-loss / outside-press close. Disabled items
can be focused but not activated.

## Color (`ColorStyle.STANDARD`)

Container `SURFACE_CONTAINER_LOW`, Level-3 shadow, `ShapeScale.MD` corners. Label `ON_SURFACE`;
leading icon / supporting / trailing `ON_SURFACE_VARIANT`; selected `TERTIARY_CONTAINER` /
`ON_TERTIARY_CONTAINER`. Light and dark use the same roles, so dark mode is free via `ElwhaTheme`.

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
| Standard color | `ColorStyle.STANDARD` |

## Showcase

The Elwha Showcase → Components → **Menu** leaf: a Workbench of live triggers configured by
Layout / Separator controls, and a Gallery of static `renderPreview()` snapshots.
```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
```
