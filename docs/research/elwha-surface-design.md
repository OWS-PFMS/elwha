# ElwhaSurface — Design Decisions

**Status:** LOCKED for v1 build. This doc fixes the `ElwhaSurface` API, defaults, and composition relationship with `ElwhaCard` V2.

**Drafted:** 2026-05-15

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — §9 "build only when raw Swing + tokens can't express the need."
- [`elwha-v1-component-scope.md`](elwha-v1-component-scope.md) — §3.3 catalogs `ElwhaSurface` as filed (#43).
- [`elwha-flatchip-rebuild.md`](elwha-flatchip-rebuild.md) §4.7 — locks the `SurfacePainter` extraction this component composes.

**Epic:** [#43](https://github.com/OWS-PFMS/elwha/issues/43) — `epic: ElwhaSurface — token-native rounded surface primitive`.

---

## TL;DR

1. **What it is:** a `JPanel` subclass that paints a rounded, role-filled, optionally outlined background through `SurfacePainter`. No state layers, no elevation, no padding API.
2. **API:** four typed setters — `setSurfaceRole(ColorRole)`, `setShape(ShapeScale)`, `setBorderRole(ColorRole)`, `setBorderWidth(int)`. No raw `Color` / `Insets` / pixel setter ever ships.
3. **Defaults:** `SURFACE` + `MD` + no border. Matches the future `ElwhaCard` V2 default, so Card V2 composes a Surface and inherits the right look out of the box.
4. **Why it earns v1:** Swing's `JPanel` is rectangular. Rounded panel paint is per-consumer `paintComponent` boilerplate today and is the missing piece for retiring Card V1's escape hatches in V2.

---

## 1. The §9 bar

Design direction §9: *"Build a component only when raw Swing + tokens can't express what you need."*

Raw `JPanel` is rectangular. A rounded, token-filled panel is `paintComponent` boilerplate — every consumer that wants one writes its own anti-aliased `RoundRectangle2D`, picks a corner radius, picks a fill color, and re-implements the binding rule by hand. `SurfacePainter` already centralizes the geometry / token resolution for `ElwhaChip`; `ElwhaSurface` is the `JPanel` wrapper that exposes that machinery to consumers as a container they can `add()` children to.

`ElwhaCard` V2 (#253) composes a Surface. That's the single most important reason this exists — V2 retires V1's `setSurfaceColor` / `setCornerRadius` escape hatches by delegating the background paint to a `Surface` whose role + shape are token-typed by construction.

## 2. API shape

```java
public class ElwhaSurface extends JPanel {
  public ElwhaSurface();

  public ElwhaSurface setSurfaceRole(ColorRole role);
  public ColorRole getSurfaceRole();

  public ElwhaSurface setShape(ShapeScale shape);
  public ShapeScale getShape();

  public ElwhaSurface setBorderRole(ColorRole role);   // null = no border
  public ColorRole getBorderRole();

  public ElwhaSurface setBorderWidth(int px);          // default 1
  public int getBorderWidth();
}
```

Fluent setters return `this` so a Surface can be configured inline at construction. Mirrors `ElwhaChip`'s fluent style.

**No raw setters.** No `setSurfaceColor(Color)`, no `setBackground(Color)`-respecting fallback, no `setCornerRadius(int)`. This is the lesson learned from `ElwhaCard` V1 — every raw escape hatch becomes a long-term migration cost when the token system catches up. Pre-1.0, we don't ship the escape hatches in the first place.

**No padding API.** Padding belongs to whoever composes the surface — Card V2 owns its content padding via its slot layout; bare consumers can wrap children in their own `EmptyBorder`. Bundling a `setPadding(SpaceScale, SpaceScale)` into Surface here would (a) duplicate what Card V2 needs anyway, and (b) make Surface load-bearing for a concern it doesn't paint.

**No interaction.** Surface is not a button, not a chip, not selectable. It has no hover / pressed / selected states and `SurfacePainter` is always called with `overlay = null`. Consumers needing an interactive surface should compose a Surface inside an `ElwhaChip` / `ElwhaIconButton` (#45) / `ElwhaCard` V2.

## 3. Defaults

| Property | Default | Rationale |
|---|---|---|
| `surfaceRole` | `ColorRole.SURFACE` | The workhorse background role. M3 Paper-equivalent default. |
| `shape` | `ShapeScale.MD` (12 px) | The locked `ElwhaCard` default (`ShapeScale.MD` Javadoc explicitly says so). Card V2 composing a Surface gets the right look with zero configuration. |
| `borderRole` | `null` | No border by default. Setting a non-null role turns the border on; `setBorderRole(null)` turns it back off. |
| `borderWidth` | `1` | The standard M3 outline width (matches `ElwhaChip.OUTLINED`). Has no visual effect while `borderRole` is `null`. |

## 4. Paint

`paintComponent(Graphics)` delegates to `SurfacePainter.paint(...)`:

```java
SurfacePainter.paint(
    (Graphics2D) g,
    getWidth(),
    getHeight(),
    shape.px(),
    surfaceRole,
    null,                // no state-layer overlay — Surface is non-interactive
    borderRole,          // null suppresses the border inside SurfacePainter
    borderWidth);
```

The painter's existing semantics carry: clamped arc, half-pixel-grid centering for crisp 1 px AA strokes, defensive `Graphics2D` copy for rendering-hint isolation, and (critically) paint-time token resolution. The binding rule is satisfied by construction — the only `Color` values touched live inside `SurfacePainter` after a `role.resolve()` call.

`setOpaque(false)` is set in the constructor — Surface paints its own (rounded) background; leaving `opaque = true` would have Swing pre-fill the bounding rectangle with the parent's color, visible as square corners showing through the round-rect fill.

## 5. Re-skin on theme switch

No explicit listener required. `ElwhaTheme.install` re-writes the `UIManager` keys and dispatches `SwingUtilities.updateComponentTreeUI` through every window; that triggers a repaint, which calls `SurfacePainter.paint`, which calls `role.resolve()` fresh, which reads the new `UIManager` value. End-to-end binding-rule compliance comes for free from delegating to the painter.

The playground tab (#53) exercises this end-to-end across all 49 roles × 7 shapes on every mode switch.

## 6. `ElwhaCard` V2 composition (forward-looking, non-binding)

Card V2 (#253) is queued behind this epic. The intended composition:

```java
class ElwhaCard extends ElwhaSurface {        // or composes one, TBD in #253
  // Card adds: header / body / footer slots, hover/pressed/selected interaction
  // (via internal state-layer overlay), padding, optional drag handle.
  // Card does NOT re-implement: round-rect paint, token resolution, border.
}
```

Whether Card V2 *extends* `ElwhaSurface` or *composes* one is a #253 decision — both work; the composition variant gives V2 freedom to swap the surface implementation for an interactive variant later. The contract this doc fixes is just: **Surface owns the round-rect paint; Card V2 does not duplicate it.**

## 7. Out of scope (v1)

| Deferred | Why deferred |
|---|---|
| **Elevation / tonal lift / shadow** | M3's elevation system is a tonal ladder (`surfaceContainerLow` … `surfaceContainerHighest`) + an optional shadow. The ladder is already a `ColorRole` axis the caller can pick. The shadow is a v2 system (`elwha-design-direction.md` §10). |
| **State layers** | Surface is non-interactive. Interactive surfaces live in components that compose Surface (Chip, IconButton, Card V2). |
| **`surfaceTint` overlay** | M3's "elevation tint" overlay belongs to the elevation system — deferred with it. |
| **Padding API** | Composers own their padding (see §2). |
| **Sizing presets** | A Surface doesn't have semantic sizes — it's whatever the layout sizes it to. Composers (Card, IconButton) own their sizing. |
| **Accessibility hooks** | Surface is a non-interactive container; it inherits `JPanel`'s default a11y. Consumers that compose interactive children own those children's a11y. |

## 8. Validation surface (#53)

The playground sub-issue exercises the full matrix:

- **Matrix tab:** `ColorRole × ShapeScale` (49 × 7 = 343 cells), each cell a real `ElwhaSurface` instance. Re-skins on mode switch verify the binding rule end-to-end.
- **Live tab:** a single Surface driven by combo boxes — role, shape, border role, border width — for visual exploration.

Both panels are factored into `SurfacePlaygroundPanels` so the standalone `ElwhaSurfacePlayground` and `ThemePlayground`'s new `Surface` tab compose the same instances. Same factored-builder pattern `ChipPlaygroundPanels` (#39) established.

## 9. CHANGELOG

`[Unreleased]` gets a new `### Added` entry for the epic, with sub-entries for the three sub-issues:
- `research(#51)` — this doc
- `feat(#52)` — the `ElwhaSurface` class
- `task(#53)` — `SurfacePlaygroundPanels` + standalone playground + `ThemePlayground` `Surface` tab
