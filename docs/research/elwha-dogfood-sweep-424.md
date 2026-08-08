# The Showcase dogfood sweep — per-site mapping (#424, folding in #321)

**Status:** executed. **Milestone:** v0.5.0.
**Supersedes** the mapping table in the epic body, which was filed before `ElwhaCheckbox` (#410),
`ElwhaRadioButton` (#416), and `ElwhaSlider` (#340) shipped and therefore routed several sites to
the wrong primitive (most visibly: every `JCheckBox` to `ElwhaSwitch`, and every `JRadioButton` to a
documented keep).

## Why

The Showcase is the storefront. A consumer learning Elwha reads it, and a rail built from raw
`JComboBox` teaches that Elwha has no select. The rule — *the storefront never demonstrates raw
Swing where an Elwha primitive exists* — was enforceable only for buttons when #317 ran, because
`ElwhaTextField`, `ElwhaSelectField`, `ElwhaSwitch`, `ElwhaCheckbox`, and `ElwhaRadioButton` had not
shipped yet. They all have now.

## The numbers

| Widget | Sites before | Converted | Kept (with reason) |
|---|---:|---:|---:|
| `JComboBox` | 115 | 113 | 2 |
| `JCheckBox` | 40 | 37 | 3 |
| `JToggleButton` (#321) | 48 | 37 | 11 |
| `JRadioButton` | 6 | 2 | 4 |
| `JTextField` | 18 | 11 | 7 |
| `JButton` (#317 residue) | 9 | 4 | 5 |
| `JSlider` | 2 | 1 | 1 |
| `JSpinner` | 33 | 0 | 33 |
| **Total** | **271** | **205** | **66** |

Counts are construction sites (`new JFoo(...)` lines), not instances — a site inside a loop or an
array initialiser can produce several controls. The 37 converted `JToggleButton` sites collapse into
**20 `ElwhaButtonGroup`s and 4 SELECTABLE `ElwhaButton`s**, because a `ButtonGroup` of N toggles is
*one* M3 segmented control, not N loose buttons.

66 of the 271 survive, and 54 of those are for two structural reasons that have nothing to do with
the sweep's judgement: 33 spinners have no Elwha counterpart, and 21 sites sit in two files this
branch deliberately did not touch. The genuinely interesting keeps are the remaining 12.

## The mapping, by widget

### `JComboBox` → `ElwhaSelectField<T>`

Mechanical once the shape is fixed: `setOptions(List.of(E.values()))`, then `setSelectedValue(seed)`,
then `addSelectionChangeListener`. The listener is registered last so construction does not run the
workbench's `apply` before the rest of the rail exists — which the old
`setSelectedItem`-before-`addActionListener` ordering already guaranteed.

**The caption moves into the control.** `WorkbenchControls.addControl(label, control)` lays out
`label | control`; an `ElwhaSelectField` paints its own floating label, so keeping both duplicated
the caption. Every converted rail row now reads `addControl("", select)` with the caption inside the
select. `JSpinner` rows are untouched and keep their row label, because a spinner has no label of
its own. This is the idiom `SwitchShowcasePanels` (#407) established and the sweep generalised.

**Two seeds changed meaning, not value.** `JComboBox` implicitly selects index 0; `ElwhaSelectField`
starts empty. Eleven sites relied on that implicit selection and never called `setSelectedItem`, so
each got an explicit `setSelectedValue(<the value the combo was already showing>)`. That is parity
made visible, not a new default.

| Package | Sites |
|---|---:|
| `showcase/` (48 in `ElwhaShowcase` alone) | 68 |
| `chip/playground`, `iconbutton/playground`, `button/playground` | 21 |
| `navrail/playground`, `surface/playground` | 7 |
| `card/playground`, `dialog/playground` | 6 |
| `theme/playground` (11 in the two once-deferred files, swept by #718), `badge/playground`, `fab/playground` | 31 |

### `JCheckBox` → `ElwhaCheckbox`

**Not `ElwhaSwitch`**, which is what the epic body said. `ElwhaCheckbox` did not exist when #424 was
filed (it shipped in #410), and it is the right control on both axes:

- **Semantics.** A workbench rail toggle is "check these options", not "flip this setting". M3's
  switch is for a setting that takes effect immediately on a real preference; the rails are
  configuring a demonstration.
- **Density.** A checkbox is a glyph plus a label. A switch is a 52×32 track that never paints its
  own label, so every site would also need an adjacent `JLabel` — in a rail that is already
  ~2.5× taller after the select-field conversion.

`SurfaceControlPanel` and `SwitchShowcasePanels` already used `ElwhaCheckbox`; the sweep matched
them rather than introducing a third idiom. `isSelected()` → `isChecked()`, `getText()` →
`getLabel()`, and the two `addItemListener` sites became `addActionListener` (there is no
`ItemListener` on `ElwhaCheckbox`).

No site was found where switch semantics read better. If one appears later, the split is per-site,
not per-package.

### `JToggleButton` → `ElwhaButtonGroup` / SELECTABLE `ElwhaButton` — this is #321

#321 filed this separately because the swap is not mechanical, and that was right: **33 of the 37
converted sites were inside a `javax.swing.ButtonGroup`**, which means they were never N toggles at
all — they were one mutually-exclusive selector wearing N components. Each such row collapsed into a
single `ElwhaButtonGroup`:

```java
ElwhaButtonGroup.connected()
    .setSelectionMode(SelectionMode.REQUIRED)
    .setButtonSize(ButtonSize.XS)
    .setResizeMode(ResizeMode.FIXED)
    .setColorStyle(ButtonGroupColorStyle.OUTLINED)
```

Seventeen of the twenty groups are the light/dark/system **mode bar** every playground carries, which
is now one component instead of three toggles plus a `ButtonGroup` — and is the same
`ElwhaButtonGroup` preset the Showcase's own header bar uses, so the storefront demonstrates the
segmented control ~20 more times than it did.

The remaining 4 were standalone binary toggles with no group and no sibling
(`ElwhaFabAnchorPlayground`'s RTL flip, `ElwhaNavRailDestination*`'s "Toggle selected" /
"Detach all badges" / "Clear all badges"). Those became `ElwhaButton` in
`ButtonInteractionMode.SELECTABLE`. No site read better as an `ElwhaChip` — the chip's filter
affordance implies a set being narrowed, and none of these narrow anything.

**One behavioral difference, accepted deliberately.** `javax.swing.ButtonGroup` re-fires
`actionPerformed` when you click the already-selected toggle, so a redundant click used to re-run
`ElwhaTheme.install` + `updateComponentTreeUI` (and, on the RTL playgrounds, a full matrix rebuild).
`ElwhaButtonGroup` in `REQUIRED` mode refuses the deselect and dedupes the notification, so a
redundant click is now a no-op. Every state *transition* the demos exercise is unchanged; only the
wasted repeat work is gone.

### `JRadioButton` → `ElwhaRadioButton` + `ElwhaRadioGroup`

The epic body listed this as "no equivalent — documented keep". `ElwhaRadioButton` shipped in #416,
so both reachable sites converted (`ElwhaNavigationRailExpandedPlayground`'s Collapsed/Expanded
pair). `ElwhaRadioGroup` expresses everything `javax.swing.ButtonGroup` was doing there: `add()` on
a selection-less group adopts the incoming selected radio, and a programmatic `setSelected(true)`
fires `ChangeListener`s but not `ActionListener`s — the same programmatic/user split `JRadioButton`
had, which is what stops the rail's own variant-tracking listener re-entering `morphTo`.

Two additions come with the M3 group contract and are worth knowing at smoke time: the pair is now
**one** roving tab stop rather than two, and arrow keys navigate with selection-following-focus.

### `JTextField` → `ElwhaTextField`

Eleven converted, seven kept. The keeps are the interesting part — see below.

### `JSpinner` → kept, all 33

No Elwha stepper exists. Every spinner keeps its row label, since it has no floating label to carry
the caption. A stepper recipe was considered and is **not** drafted here: the sites are 0–4 border
widths, 1–10 column counts, and millisecond delays, all of which an `ElwhaTextField` + two
`ElwhaIconButton`s would serve worse than the raw spinner until the library has a real numeric-input
primitive with M3 semantics. That is a component decision, not a sweep decision.

## The keeps that are judgement calls

Twelve sites survive for reasons other than "no primitive exists" or "deferred file". Each one is a
place where converting would have destroyed what the code proves.

| Site | Kept | Why |
|---|---|---|
| `dialog/playground/DialogAccessibilityDemo` | `JTextField` | The demo's stated proof is *"Tab walks Save → Cancel → the text field → back to Save and never escapes"*. `ElwhaTextField` is a wrapper whose embedded editor becomes the real focus stop, so the swap redefines the very cycle under test. |
| `dialog/playground/FullScreenDialogA11yDemo` (×4) | `JTextField` | Proves *"initial focus lands on the first content field"* (#280). The assertion is about which component is the first focusable descendant — a composite field changes the answer. |
| `tooltip/TooltipTriggerSmoke` (×2 `JButton`, ×1 `JTextField`) | raw Swing | The smoke asserts `a.getMouseListeners().length <= 2` — one trigger listener against a bare `JButton`'s single UI listener. An `ElwhaButton` installs hover/press/ripple listeners and the assertion becomes meaningless. The field is a focus parking lot whose `requestFocusInWindow(...)` return value gates a whole block. |
| `tooltip/TooltipPlainChromeSmoke` (×1 `JButton`, ×1 `JTextField`) | raw Swing | The `JButton` is deliberately the **non**-`ShadowBearing` anchor, contrasted two lines later against an `ElwhaButton` anchor. Converting collapses both halves of the halo test into one case. |
| `navrail/playground/ElwhaNavigationRailExpandedPlayground` | `JSlider` | `ElwhaSlider` exists and the value plumbing is a drop-in, but the site paints tick marks (`setMajorTickSpacing` + `setPaintTicks`) and `ElwhaSlider` has no painted-tick API. Its nearest analogue, `setStops(int)`, *snaps the value* rather than decorating the track — converting would silently turn a continuous slider into a 3-position one. See the gaps below. |
| `theme/playground/ThemePlayground` | `JButton` | Handed to `JRootPane.setDefaultButton(JButton)`. `ElwhaButton` extends `JComponent`, not `JButton`. This is #317's original allowlisted survivor and still the only one of its kind. |

Two adjacent sites were **converted** despite sitting in the same category, and are called out so the
line is visible: `DialogModalityDemo`'s field proves *input inertness* (the scrim blocks an
`ElwhaTextField` identically, so the proof survives and now shows an Elwha control going inert), and
`FullScreenDialogContentDemo`'s 16-row form proves *scroll behavior*, not focus order.

## Deferred: two files, 21 sites — resolved by #718

`theme/playground/ThemePlayground.java` and `theme/playground/FoundationsPanels.java` kept all their
raw controls through the #424 pass. Both were edited by the then-open ShapeScale PR
[#706](https://github.com/OWS-PFMS/elwha/pull/706), and a sweep rewriting the same regions would have
guaranteed a conflict on a PR already awaiting operator smoke. They sat on every guard's allowlist
with that reason, so the exemption was visible rather than silent.

[#718](https://github.com/OWS-PFMS/elwha/issues/718) swept them once #706 landed: 20 of the 21 sites
converted, and the five deferred allowlist entries came off `JCheckBox` / `JComboBox` /
`JRadioButton` / `JTextField` / `JToggleButton`. Four of those allowlists are now empty.

The 6 `JToggleButton`s per file did not become 6 Elwha toggles. The three view-mode buttons in each
were a `javax.swing.ButtonGroup` + `FlowLayout` hand-assembly of exactly what `ElwhaButtonGroup`'s
connected treatment is, so each trio collapsed into one component — the same reduction #424 applied
to the other 33. `ThemePlayground`'s mode bar took the `REQUIRED` / `XS` / `FIXED` / `TONAL` preset
every other playground's mode bar now uses.

**One conversion overrode a standing in-code comment**, and it is called out because the comment
said the opposite. `buildTextRow` in both files carried *"Intentionally raw Swing (not
ElwhaTextField): this row demonstrates how the token foundation themes native JTextField /
JTextArea"*, written in #286 long before the dogfood doctrine settled. #424's keeps table — which
enumerates its judgement calls exhaustively, and does list `ThemePlayground`'s `JButton` — does not
list this row, and #424's own follow-up text names the `JTextField` as convert-scope. It converted.
The row's proof survives intact in the raw `JTextArea` beside it, which no guard covers and which
Elwha still has no primitive for; the comment now says that.

**The one site that stayed:** `ThemePlayground`'s `JButton defaultButton`, for the
`JRootPane.setDefaultButton(JButton)` reason already in the keeps table. Its `JButtonSweepGuard`
entry is unchanged.

`card/fixes/` is out of scope, as it was for #317 — frozen diagnostic harnesses for historical card
bugs, advisory-only in reviews.

## The guards

Six headless guards, one per swept widget class, on the pattern #317 established:

    JButtonSweepGuard  JComboBoxSweepGuard  JCheckBoxSweepGuard
    JToggleButtonSweepGuard  JRadioButtonSweepGuard  JTextFieldSweepGuard

They share one scan engine (`RawSwingSweep`) rather than duplicating it six times, and
`JButtonSweepGuard` was refactored onto it.

**The guarded surface is now structural.** #317 listed six directories by hand; the engine walks the
tree and guards `showcase/` plus *every* `playground/` package, so a new component's playground is
covered the day it lands. Story-time `*Demo` / `*Smoke` / `*Diag` mains sitting directly in a
component package stay outside it — as the keeps table shows, a raw control is frequently the
deliberate substrate of what such a proof measures, and guarding those directories would ossify
their internals.

**The guards now actually gate.** They were `main`s that nothing ran together, which is how a sweep
rots. `RawSwingSweepGuardTest` (13 invocations) drives all six on the same engine, so CI fails on a
regression. It also asserts each allowlist entry is *still live* — an entry kept after its file was
swept silently un-guards that file, so a stale exemption is a test failure rather than a slow leak.

## What the sweep changed in the test suite

`WorkbenchControlApplyTest` walked every workbench's `JComboBox` choices, `ElwhaCheckbox` toggles,
and `JSpinner` steps. After the conversion the combo sweep would have found **zero** controls and
passed vacuously — the exact failure mode where a green suite tests nothing. It now drives
`ElwhaSelectField` through `getOptions()` + `setSelectedValue`, and gained two tests that make that
failure mode loud:

- `theSweepsHaveControlsToSweep` — floors on what the sweeps find (71 selects, 417 individual
  choices, 82 checkboxes, 20 spinners today). A tripwire, not a target.
- `everyRailControlIsReachableByItsCaption` — every rail select resolves back to itself by the
  caption it displays.

The second one earned its keep immediately: it caught that a caption equal to a **section title**
("State" on the Checkbox leaf, "Tabs" on the Tabs leaf) resolved to the section header's neighbour
instead of the control. Section headers and row labels are both bare `JLabel`s in the same grid, so
a text match alone was ambiguous — a latent bug the old row-label-only lookup had always had, which
only became reachable once captions moved into the controls. `WorkbenchControls` now marks its
section headers and the fixture skips them.

Invocations: **145 → 169** on `WorkbenchControlApplyTest`, plus 13 new guard invocations.
`SurfaceControlPanelTest` holds at 17 — its one index-driven site
(`setSelectedIndex(1)` on a private-typed option) now reads `getOptions().get(1)`.

## Component gaps found

Three, none fixed here beyond the one the suite could not survive without.

1. **`ElwhaSelectField` had no `getOptions()`** — `setOptions` with no getter, which
   `docs/development/component-api-conventions.md` §5 already names as drift to fix in the next
   pass. **Added** (read-only, returns the existing immutable list). Without it the 417-choice sweep
   has no way to enumerate what a select offers, and constraint "the sweep must end at least as
   strong as it started" is unsatisfiable. This is the sweep's one component-source edit;
   `ElwhaSelectField` is not touched by PR #706, so it carries no conflict risk.
2. **`ElwhaSelectField` has no index API** (`getSelectedIndex` / `setSelectedIndex` /
   `getItemCount`). Three sites needed it and were rewritten to dispatch on value, which reads
   better; worth a deliberate ruling on whether a select should expose position at all, given that
   `ElwhaButtonGroup` does.
3. **`ElwhaSlider` has no painted-tick API.** Blocks the one `JSlider` keep above. `setStops(int)`
   is not the same thing — it snaps values rather than decorating the track.

## Density: the honest cost

An `ElwhaSelectField` is 245×~80 dp (a 56 dp container, a floating-label band, a supporting-text
row) where a `JComboBox` was ~200×30. A workbench rail with eight selects is roughly 2.5× taller
than it was. The rails live in a `JScrollPane`, so nothing clips — but this is a real ergonomic cost
of dogfooding, paid knowingly.

It is not a bug to fix at the call site: M3 has no dense text field, so a compact variant would be a
library-level divergence decision, not a Showcase workaround. Three sites needed an explicit
`setPreferredSize` where a `FlowLayout` row would otherwise have wrapped into height its `BoxLayout`
parent never allocated (`SurfacePlaygroundPanels`, `ShapeMorphPlayground`), and two hard-pinned rail
heights computed for 30 px combos became natural heights (`ElwhaCardListShowcase`,
`LiveConfigPanel`).
