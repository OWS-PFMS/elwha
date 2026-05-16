# ElwhaIconButton — Design Decisions

**Status:** LOCKED for v1 build. This doc fixes the `ElwhaIconButton` API, variant table, toggle model, and ThemePlayground integration.

**Drafted:** 2026-05-15

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — §9 doctrine bar (raw Swing + tokens can't express).
- [`elwha-v1-component-scope.md`](elwha-v1-component-scope.md) — §3.1 catalogs `ElwhaIconButton` as filed (#45).
- [`elwha-flatchip-rebuild.md`](elwha-flatchip-rebuild.md) §4.7 — locks `SurfacePainter`, the shared paint helper.
- [`elwha-surface-design.md`](elwha-surface-design.md) §6 — the composition pattern (round-rect paint lives in `SurfacePainter`, not in each component).
- [M3 Icon button spec](https://m3.material.io/components/icon-buttons/specs) — the spec being adapted (see §10 for the deliberate divergences).

**Epic:** [#45](https://github.com/OWS-PFMS/elwha/issues/45) — `epic: ElwhaIconButton — token-native M3 icon button (with toggle behavior)`.

---

## TL;DR

1. **What it is:** a square, icon-only button primitive with 4 M3 variants (`FILLED` / `FILLED_TONAL` / `OUTLINED` / `STANDARD`), a `CLICKABLE` / `SELECTABLE` interaction axis, and a declarative toggle pattern via `setIcons(resting, selected)`.
2. **Why it earns v1:** UIManager-styled `JButton` gives one look globally; M3 wants per-instance variant + toggle icon-swap. The OWS-tool playground already hand-rolls the pattern (`pushPin ↔ pushPinFilled`); this subsumes it.
3. **Paint:** delegates the round-rect surface and border to `SurfacePainter` (shared with `ElwhaChip` and `ElwhaSurface`). No round-rect code lives in `ElwhaIconButton`.
4. **Toggle model:** Elwha-uniform — `SELECTED` state-layer overlay (12%), icon swap via `setIcons`, focus ring swap to `PRIMARY` (2 px). Deliberate divergence from M3's per-variant surface-color swap (see §10).
5. **Five sizes via `IconButtonSize`:** XS (24/16) / S (32/20) / M (40/24, default) / L (48/28) / XL (56/32) — the full M3 size axis, exposed as an enum because OWS has icon buttons at multiple sizes (added during PR #58 smoke-test per #59). Size and shape stay orthogonal — `setButtonSize` does not touch the corner radius.

---

## 1. The §9 bar

Design direction §9: *"Build a component only when raw Swing + tokens can't express what you need."*

UIManager-styled `JButton` gives **one** look across the whole UI. M3 icon button needs **four** treatments selectable per instance, plus a toggle pattern with icon swap on select. Neither composes from raw Swing + theme keys.

Test B (reuse): OWS uses icon buttons "almost exclusively." The chip rebuild playground already hand-rolls `pushPin ↔ pushPinFilled` borderless-toggle and that pattern becomes the `STANDARD` variant of this component verbatim.

## 2. Variant table

Treatment-only enum — same orthogonality lesson as `ChipVariant`. The variant pins the treatment and carries the default surface/border roles; the surface role is overridable per instance.

| Variant | Default surface role | Default border role | M3 emphasis | Notes |
|---|---|---|---|---|
| `FILLED` | `PRIMARY` | `null` | Highest emphasis. Primary affordance. | Foreground resolves to `ON_PRIMARY`. |
| `FILLED_TONAL` | `SECONDARY_CONTAINER` | `null` | Moderate. "Active but not primary" — the common OWS case. | Foreground resolves to `ON_SECONDARY_CONTAINER`. |
| `OUTLINED` | `null` (transparent) | `OUTLINE` | Medium. | Foreground resolves to `ON_SURFACE_VARIANT` (the icon-button-specific foreground; see §3). |
| `STANDARD` | `null` (transparent) | `null` | Lowest. The borderless toggle pattern the playground hand-rolls today. | Foreground resolves to `ON_SURFACE_VARIANT`. **Toggle-on tints icon to `PRIMARY`** (the only per-state foreground swap; see §6). |

**Per-instance surface override:** `setSurfaceRole(ColorRole)` reassigns the surface role for the active variant, same mechanism as `ElwhaChip.setSurfaceRole`. The foreground re-pairs automatically against the new surface's `on()` role.

**Why these four exactly:** M3 distinguishes filled from filled-tonal because the emphasis difference is meaningful — `FILLED` reads as "the primary action in this region," `FILLED_TONAL` as "an active control, not the headline." Collapsing them would lose that distinction and force consumers to manually swap surface roles for the common case.

## 3. Foreground resolution

Same rule as chip with one icon-button-specific tweak:

```
if (selected && variant == STANDARD)         → PRIMARY        (M3 toggle-on indicator)
else if (effective surface role has .on())   → on()-pair      (FILLED → ON_PRIMARY, etc.)
else                                          → ON_SURFACE_VARIANT  (icon-button default)
```

`ON_SURFACE_VARIANT` is the M3-spec foreground for icon buttons sitting on transparent surfaces — slightly lower-emphasis than `ON_SURFACE` (which the chip uses), reflecting that icon buttons are smaller affordances and don't need to read at the same prominence as text-bearing chips.

`STANDARD + selected` is the **only** per-state foreground swap. It exists because the STANDARD variant has neither a surface fill nor a border to carry the selected indicator — without an icon color swap there's no visual signal at all when a toggle flips. (Consumers who supply `setIcons(rest, sel)` get the icon-glyph swap on top of this; the tint applies to both icons since both flow through the chip-internal `FlatSVGIcon.ColorFilter` keyed off `resolveForegroundColor()`.)

## 4. API shape

```java
public class ElwhaIconButton extends JComponent {

  // -- ctor
  public ElwhaIconButton();
  public ElwhaIconButton(Icon icon);

  // -- variant + interaction
  public ElwhaIconButton setVariant(IconButtonVariant variant);
  public IconButtonVariant getVariant();
  public ElwhaIconButton setInteractionMode(IconButtonInteractionMode mode);
  public IconButtonInteractionMode getInteractionMode();

  // -- icon
  public ElwhaIconButton setIcon(Icon icon);          // resting + selected (selected omitted)
  public ElwhaIconButton setIcons(Icon resting, Icon selected); // toggle pair
  public Icon getIcon();
  public Icon getSelectedIcon();

  // -- token overrides
  public ElwhaIconButton setSurfaceRole(ColorRole role);   // null = variant default
  public ColorRole getEffectiveSurfaceRole();
  public ElwhaIconButton setShape(ShapeScale shape);       // default FULL (capsule)
  public ShapeScale getShape();
  public ElwhaIconButton setBorderWidth(int px);           // default 1; 2 for focus ring

  // -- sizing (single size for v1, but configurable so the playground can demo it)
  public ElwhaIconButton setContainerSize(int px);         // default 40
  public ElwhaIconButton setIconSize(int px);              // default 24

  // -- selected state
  public ElwhaIconButton setSelected(boolean selected);
  public boolean isSelected();
  public static final String PROPERTY_SELECTED = "selected";

  // -- listeners
  public void addActionListener(ActionListener listener);
  public void removeActionListener(ActionListener listener);
  public void addSelectionChangeListener(PropertyChangeListener listener);
}
```

Fluent setters return `this`. Same shape as `ElwhaChip`.

**No M3-factory presets** (no `toggleButton(...)` / `pinButton(...)` static factories). Unlike Chip's M3 chip-type sugar (`assistChip` / `filterChip` / `inputChip` / `suggestionChip`), M3 doesn't define discrete "icon button types" beyond the variant + toggle axis already exposed. A two-line `new ElwhaIconButton(MaterialIcons.pushPin()).setIcons(pushPin, pushPinFilled).setInteractionMode(SELECTABLE)` is enough.

**No leading/trailing slot.** Icon buttons hold one icon. The chip's leading/trailing surface (and the menu/affordance machinery it brings) is the wrong precedent here.

## 5. Defaults

| Property | Default | Rationale |
|---|---|---|
| `variant` | `STANDARD` | Lowest emphasis. The "I just want an icon button" case — the OWS-tool's existing borderless pattern. Bumping to FILLED is one setter call. |
| `interactionMode` | `CLICKABLE` | Most icon buttons are push buttons; toggle is opt-in via `SELECTABLE`. |
| `shape` | `ShapeScale.FULL` | M3 spec — capsule corners on a 40×40 container produce the round icon-button look. Overrideable to `MD` / `LG` etc. for square treatments per consumer taste. |
| `buttonSize` | `IconButtonSize.M` (40 px container, 24 px icon) | M3 medium icon button. Matches `MaterialIcons.DEFAULT_SIZE` for the icon dimension. |
| `borderWidth` | `1` | Matches OUTLINED + chip + Surface. Focus ring bumps to 2 px. |

## 6. State model

| State | Surface paint | Border paint | Foreground |
|---|---|---|---|
| idle | variant default | variant default | per §3 |
| hovered | `HOVER` overlay (8%) | variant default | per §3 |
| pressed | `PRESSED` overlay (10%) | variant default | per §3 |
| focused | (current overlay) | swap to `PRIMARY` at 2 px | per §3 |
| selected (non-STANDARD) | `SELECTED` overlay (12%) | swap to `PRIMARY` | per §3 (variant on-pair) |
| selected (STANDARD) | (no surface — overlay paints on transparent base tinted against `SURFACE`) | `null` | `PRIMARY` |
| disabled | variant fill at 12% opacity | variant border at 12% opacity | content opacity 38% |

This is the same state model as `ElwhaChip` with two adjustments: (1) the STANDARD foreground-swap rule from §3, and (2) one icon-only state (no text baseline / leading slot to worry about).

**Why uniform overlay vs M3 surface-swap:** M3 spec defines an explicit `selectedContainerColor` per variant — e.g. FILLED toggle-off uses `surfaceContainerHighest` and toggle-on uses `primary`. Elwha picks the M3 toggle-ON color as the variant default (§2) and uses uniform `SELECTED` overlay compositing as the selection indicator. This is the same call `StateLayer.SELECTED`'s Javadoc documents for chip ("M3 models selection as a container-color swap rather than an overlay; Elwha keeps one uniform mechanism instead"). Repeating it here keeps icon-button selection behavior visually consistent with chip selection — which matters because both can appear in the same UI region.

## 7. Toggle pattern

Two layers, both opt-in:

1. **Icon swap** via `setIcons(resting, selected)`. The selected icon paints in place of the resting one when `isSelected()` is true. If `selected` is `null`, the resting icon paints in both states.
2. **State-layer overlay** via the SELECTED state (12% tint of foreground over surface), applied uniformly whether `setIcons` was called or not.

Material symbol pairs already bundled in `MaterialIcons` (suitable defaults):
- `pushPin` ↔ `pushPinFilled`
- `anchor` ↔ `anchorFilled`
- `favorite` ↔ (filled variant — currently not bundled; renders the same outline icon, state-layer carries the signal)
- `star` ↔ (filled variant — currently not bundled; same)

The "filled" pairs for `favorite` / `star` are out of scope for this epic. If OWS wants them later, dropping the SVG into `resources/com/owspfm/icons/material/` and adding a one-line lookup to `MaterialIcons` is enough.

## 8. Paint

Same delegation pattern as `ElwhaSurface`:

```java
SurfacePainter.paint(
    (Graphics2D) g,
    getWidth(),                    // == containerSize (square)
    getHeight(),                   // == containerSize
    shape.px(),                    // FULL → 9999, clamped to min(w,h)/2 → capsule
    effectiveSurfaceRole,          // null for STANDARD/OUTLINED at rest
    activeOverlay,                 // HOVER / PRESSED / SELECTED / null
    effectiveBorderRole,           // null / OUTLINE / PRIMARY (focus, selected non-STANDARD)
    effectiveBorderWidth);         // 1, bumped to 2 when focused
```

After the round-rect surface paints, the icon paints centered on `(width/2, height/2)` using the `FlatSVGIcon.ColorFilter` keyed off `resolveForegroundColor()` — same icon-tint mechanism the chip uses.

`setOpaque(false)` in the constructor so the parent's background shows through the round-rect corners.

## 9. Accessibility

- `JComponent` subclass with a custom `AccessibleContext` (same approach as chip).
- `AccessibleRole.PUSH_BUTTON` when `CLICKABLE`; `AccessibleRole.TOGGLE_BUTTON` when `SELECTABLE`.
- `getAccessibleName()` returns `getToolTipText()` if set, else the icon button's `setName(String)` value, else `"Icon button"` as a last-resort fallback. **Consumers should always supply a name or tooltip** — an icon-only button is invisible to screen readers without one.
- Keyboard activation: `VK_SPACE` and `VK_ENTER` when focused, same bindings as chip.

## 10. Deliberate M3 divergences

| M3 spec says | Elwha does | Why |
|---|---|---|
| FILLED toggle-off uses `surfaceContainerHighest`, toggle-on uses `primary` | FILLED always uses `primary`; selection signalled by 12% overlay + (optionally) icon swap | Elwha-uniform selection mechanism (see §6). A toggle-off FILLED that wants the M3 look uses `setSurfaceRole(SURFACE_CONTAINER_HIGHEST)`. |
| OUTLINED toggle-on swaps surface to `inverseSurface`, foreground to `inverseOnSurface` | OUTLINED selected gets 12% overlay over the (transparent) base + PRIMARY border swap | Same reason. The `inverseSurface` swap reads as "different component" in dense rows; the overlay reads as "this one is selected." |
| STANDARD toggle-on colors icon `primary` | STANDARD + selected colors icon `PRIMARY` (the one per-state foreground swap) | Honored — STANDARD has nothing else to carry the signal (§3). |
| 5 size variants (XS / S / M / L / XL) | Honored as `IconButtonSize` enum (#59) — added during the #58 smoke-test once the OWS need was confirmed. Size and shape stay orthogonal. | OWS has icon buttons at multiple sizes; the v1 one-size posture was reversed. |
| Shape paired to size (S/M → FULL, L/XL → LG) | Size and shape stay independent | `setButtonSize` doesn't touch the corner radius; consumers pair `setShape` explicitly when the M3-spec coupling is wanted. |
| 3 width variants (narrow / default / wide) | Single width = container size | M3 Expressive territory; defer to v2 if needed. |
| Shape morphing on selected (FULL → MD or vice versa) | No shape morphing — `setShape` is static for the component's lifetime | Shape morphing is an animation; Elwha is static-paint pre-v2. |

The divergences are documented here, not silently absorbed. If OWS hits a case where the strict M3 spec is needed (e.g. inserting an icon button into a third-party M3-themed surface that expects the surface-swap), the `setSurfaceRole` override is the escape hatch.

## 11. Out of scope (v1)

| Deferred | Why deferred |
|---|---|
| **Width variants (narrow / wide)** | M3 Expressive; not in base spec. Defer to v2 if OWS needs them. |
| **Shape morphing on toggle** | Animation system deferred to v2. |
| **Split icon button** | Composite — separate component if a real need surfaces. |
| **Icon button with menu** | Same — likely composes IconButton with a popup, not a new primitive. |
| **FAB / Extended FAB** | Different size + elevation; separate component if OWS needs it. |
| **In-list icon-button-list container** | The leading/trailing button slots inside `ElwhaChip` already cover the "icon button inside another component" case. A list of standalone icon buttons is just a `JPanel` with `FlowLayout`. |

## 12. Validation surface (#57 — playground sub-issue)

The playground sub-issue exercises the full matrix:

- **Variant gallery tab:** 4 variants × 6 states (idle / hover / pressed / selected / focused / disabled), re-rendered live on theme/mode switch — the binding-rule contract end-to-end.
- **Toggle examples tab:** real `setIcons` pairs (`pushPin ↔ pushPinFilled`, `anchor ↔ anchorFilled`, single-icon star/favorite with state-layer-only indication), one row per variant. Subsumes the hand-rolled pin toggle currently in the playground.

Both panels factored into `IconButtonPlaygroundPanels` so the standalone `ElwhaIconButtonPlayground` and `ThemePlayground`'s new `Icon Button` tab compose the same instances. Same factored-builder pattern `ChipPlaygroundPanels` / `SurfacePlaygroundPanels` already establish.

## 13. CHANGELOG

`[Unreleased]` gets a new `### Added` entry for the epic, with sub-entries:
- `research(#55)` — this doc
- `feat(#56)` — `ElwhaIconButton` + `IconButtonVariant` + `IconButtonInteractionMode`
- `feat(#57)` — `IconButtonPlaygroundPanels` + standalone playground + `ThemePlayground` `Icon Button` tab
- `feat(#59)` — `IconButtonSize` axis (XS/S/M/L/XL), `Sizes` sub-tab, `JToolBar` mockup
