# ElwhaButton — Design Decisions

**Status:** LOCKED for v1 build. This doc fixes the `ElwhaButton` API, variant table, hybrid selection color model, paint-helper plan (`RipplePainter` + `ShadowPainter` extractions), state model, per-size measurements, and the M3 Expressive baseline posture for epic #103.

**Drafted:** 2026-05-19

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) §9 — doctrine bar (raw Swing + tokens can't express).
- [`elwha-v1-component-scope.md`](elwha-v1-component-scope.md) — catalogs `ElwhaButton` as filed (#103).
- [`elwha-icon-button-design.md`](elwha-icon-button-design.md) — the reference primitive. The toggle model, paint delegation, and §10 divergence-table format are borrowed verbatim where they apply.
- [`elwha-flatchip-rebuild.md`](elwha-flatchip-rebuild.md) §4.7 — locks `SurfacePainter`, the shared paint helper.
- [`elwha-surface-design.md`](elwha-surface-design.md) §6 — the composition pattern (round-rect paint lives in `SurfacePainter`, not in each component).
- [`elwha-card-v3-spec.md`](elwha-card-v3-spec.md) — the existing ripple paint that `RipplePainter` will source (extracted in story 2 of epic #103, refactored into the card via the follow-up issue).
- [M3 Common buttons spec](https://m3.material.io/components/buttons/specs) — the spec being adapted.
- [M3 Common buttons (Expressive)](https://m3.material.io/components/buttons/overview) — the **Expressive** variant of the spec, which this doc tracks as baseline (see §0).

**Epic:** [#103](https://github.com/OWS-PFMS/elwha/issues/103) — `epic: Add ElwhaButton M3 text-button primitive (blocks v0.2.0 release)`.

---

## TL;DR

1. **What it is:** a 5-variant M3 Expressive text-button primitive (`ELEVATED` / `FILLED` / `FILLED_TONAL` / `OUTLINED` / `TEXT`) with leading-icon support, a 5-size axis (`XS` / `S` / `M` / `L` / `XL`), two shape options (round capsule, square with per-size radius), and a `CLICKABLE` / `SELECTABLE` interaction axis.
2. **Why it earns v1:** Filled Tonal — M3's canonical CTA pairing with Outlined cards per V3 card spec §3.3 — has no path through the lib today. Raw `JButton` via `FlatLafKeyMapping` renders as Outlined (default) or Filled (with `Button.default.background = primary`); the other three variants and the toggle interaction have no expression. Action rows can't carry the full M3 vocabulary without this primitive.
3. **Posture:** tracks **M3 Expressive** as baseline, not base M3 (§0). Base M3 alone gives 1 size × 1 shape × 5 colors × no toggle — too narrow for a real action-row vocabulary. Where Expressive adds (sizes, square shape, 16dp padding, toggle interaction) we follow Expressive without flagging. Divergences in §10 are reserved for places we depart from **both** specs.
4. **Toggle color model:** variant-specific surface swap for `FILLED` + `OUTLINED`; uniform `SELECTED` overlay for `ELEVATED` + `TONAL`; `SELECTABLE` is rejected on `TEXT` (§7). This is the most consequential design decision in the doc and the most-load-bearing M3 divergence — see §7 and §10.
5. **Paint:** `SurfacePainter` (round-rect surface + state-layer overlays + border, shared with chip / icon-button / surface) + new `RipplePainter` (extracted from `ElwhaCard.paintRipple`, sourced from the same M3-style expanding-circle ripple the card already paints; clip-to-container behavior preserved) + new `ShadowPainter` (extracted from `SurfacePainter.renderShadowImage` with a redesigned cache strategy — key on `(arc, elevation)` only, not on body size; reuse across instances via 9-slice or scale). The `ShadowPainter` redesign is informed by [shadow-spike-2026-05-19.md](shadow-spike-2026-05-19.md): FlatLaf's `FlatDropShadowBorder` was rejected on visual grounds (rectangular halo doesn't follow round corners) but its size-independent caching strategy is adopted. As a side effect, the new cache eliminates PR #110's `setSuspendShadowRecompute` workaround at the root — the body-size key was the per-frame invalidation source.

---

## §0. Posture: M3 Expressive baseline

The M3 common-button spec has two columns — **Base M3** (Small only, Round only, 24dp legacy padding only, no toggle, 5 colors) and **M3 Expressive** (adds XS/M/L/XL sizes, Square shape, 16dp Small padding, Toggle interaction). M3 Expressive is M3's evolution, not a separate spec.

Base M3 alone gives Elwha 1 size × 1 shape × 5 colors × no toggle. Insufficient for OWS Loop's action rows; insufficient even to demo the variant table. Every interesting axis on `ElwhaButton` comes from the Expressive column.

This spec therefore tracks **M3 Expressive** as its baseline. Adopting Expressive's size axis, square-shape option, 16dp Small padding, and Toggle interaction is not flagged as divergence — it's the spec we're implementing. §10 is reserved for places where Elwha departs from **both** Base M3 and M3 Expressive (e.g. shape morph, ripple containment, focus indicator style).

**Features we adopt from the M3 Expressive column (not present in Base M3):**

- **5-size axis** — `XS`, `M`, `L`, `XL` join the Base M3 `S` default.
- **Square shape option** — `ButtonShape.SQUARE` with per-size corner radii (12 / 12 / 16 / 28 / 28 dp). Base M3 ships round only.
- **16dp Small padding** — Base M3 `S` uses 24dp legacy padding; Expressive recommends 16dp. We adopt 16dp; the 24dp legacy is not exposed.
- **Toggle interaction** — `ButtonInteractionMode.SELECTABLE`. Base M3 common-button has no toggle; toggle is Expressive's addition. Spec note "Toggle buttons don't use the text style" continues to bind — `TEXT + SELECTABLE` throws (§7).
- **"Variants are configurations" framing** — M3 Expressive reframes color styles, size, shape, and padding as orthogonal "configurations" that compose. Our enums (`ButtonVariant` / `ButtonSize` / `ButtonShape`) follow this orthogonal axis structure.

---

## §1. The §9 bar

Design direction §9: *"Build a component only when raw Swing + tokens can't express what you need."*

UIManager-styled `JButton` via `FlatLafKeyMapping` gives **two** treatments globally — base `JButton` = Outlined; with `JButton.putClientProperty(BUTTON_TYPE, BUTTON_TYPE_DEFAULT)` and `Button.default.background = primary` = Filled. The remaining three M3 button variants (`ELEVATED`, `FILLED_TONAL`, `TEXT`) have no path. The `SELECTABLE` interaction has no path. The 5-size axis has no path. Per-variant border-vs-fill compositing rules don't compose from theme keys.

Test B (reuse): `ElwhaIconButton` (epic #45) already established the token-native action-primitive pattern. `ElwhaButton` is the text-button-shaped analogue completing the action-row vocabulary. V3 Card (epic #80) action containers are the immediate consumer; V3 card spec §3.3 lists Filled-Tonal as the canonical CTA for Outlined cards and has no way to express it today.

---

## §2. Variant table

Treatment-only enum — same orthogonality lesson as `ChipVariant` and `IconButtonVariant`. The variant pins the treatment and carries the default surface/border/foreground roles; the surface role is overridable per instance.

| Variant | Default surface | Default border | Default foreground | M3 emphasis | Notes |
|---|---|---|---|---|---|
| `ELEVATED` | `SURFACE_CONTAINER_LOW` | `null` | `PRIMARY` | Low (with shadow) | dp1 elevation at rest, dp0 when disabled. The only variant with a real shadow stack. |
| `FILLED` | `PRIMARY` | `null` | `ON_PRIMARY` | Highest | Default variant. Primary affordance. |
| `FILLED_TONAL` | `SECONDARY_CONTAINER` | `null` | `ON_SECONDARY_CONTAINER` | Moderate | "Active but not primary" — the canonical M3 pairing with Outlined cards. |
| `OUTLINED` | `null` (transparent) | `OUTLINE_VARIANT` | `ON_SURFACE_VARIANT` | Medium | Border-only chrome; container fill invisible at rest. State layers paint on transparent base tinted against `SURFACE`. |
| `TEXT` | `null` (transparent) | `null` | `PRIMARY` | Lowest | No surface, no border. Foreground tints to `PRIMARY` directly — this is the one place Elwha's surface→on-pair foreground convention is broken (the M3 spec demands `PRIMARY`, not `ON_SURFACE_VARIANT`). |

**Per-instance surface override:** `setSurfaceRole(ColorRole)` reassigns the surface role for the active variant. Foreground re-pairs against the new surface's `on()` role (except `TEXT`, which retains its `PRIMARY` foreground regardless of override).

**Why these five exactly:** all five are first-class M3 button styles. ELEVATED is distinct from FILLED because its elevation (not its fill) is the affordance signal. FILLED_TONAL is distinct from FILLED because its emphasis sits between FILLED and OUTLINED — and it's the variant the V3 card spec explicitly pairs with Outlined cards. Collapsing any of these would lose meaningful emphasis distinctions.

---

## §3. Foreground resolution

```
if (variant == TEXT)                          → PRIMARY        (direct tint, not on-pair)
else if (effective surface role has .on())    → on()-pair      (FILLED → ON_PRIMARY, etc.)
else                                          → ON_SURFACE_VARIANT  (OUTLINED, ELEVATED default)
```

`ELEVATED` resolves to `PRIMARY` per its variant default row in §2 (not via this rule — its foreground is explicitly `PRIMARY` to read as a colored-icon-and-label-on-low-surface treatment, matching M3 reference).

`TEXT` breaks Elwha's standard rule because M3 spec demands the label paint in `PRIMARY` regardless of any consumer-set surface override. The override exists for the other four variants; `TEXT` is intentionally rigid here.

`ON_SURFACE_VARIANT` is the M3-spec foreground for outlined buttons sitting on transparent surfaces — slightly lower-emphasis than `ON_SURFACE`, matching the icon-button convention from `elwha-icon-button-design.md` §3.

**Per-state foreground swaps:** none. Foreground is determined solely by variant + surface role + selection (where selection triggers a variant-specific surface swap per §7, not a foreground swap). This differs from `ElwhaIconButton`, where `STANDARD + selected` is the one per-state foreground tint.

---

## §4. API shape

```java
public class ElwhaButton extends JComponent {

  // -- ctor
  public ElwhaButton();
  public ElwhaButton(String label);
  public ElwhaButton(String label, Icon icon);

  // -- variant + interaction
  public ElwhaButton setVariant(ButtonVariant variant);          // throws if SELECTABLE and TEXT
  public ButtonVariant getVariant();
  public ElwhaButton setInteractionMode(ButtonInteractionMode mode);  // throws if SELECTABLE on TEXT
  public ButtonInteractionMode getInteractionMode();

  // -- label + icon
  public ElwhaButton setText(String label);
  public String getText();
  public ElwhaButton setIcon(Icon icon);                         // leading position only; null clears
  public Icon getIcon();

  // -- size + shape
  public ElwhaButton setButtonSize(ButtonSize size);             // default S
  public ButtonSize getButtonSize();
  public ElwhaButton setShape(ButtonShape shape);                // ROUND (default, FULL capsule) or SQUARE (per-size dp)
  public ButtonShape getShape();

  // -- token overrides
  public ElwhaButton setSurfaceRole(ColorRole role);             // null = variant default; ignored on TEXT
  public ColorRole getEffectiveSurfaceRole();
  public ElwhaButton setBorderWidth(int px);                     // default 1; focus bumps to 2

  // -- selected state
  public ElwhaButton setSelected(boolean selected);              // throws if mode != SELECTABLE
  public boolean isSelected();
  public static final String PROPERTY_SELECTED = "selected";

  // -- listeners
  public void addActionListener(ActionListener listener);
  public void removeActionListener(ActionListener listener);
  public void addSelectionChangeListener(PropertyChangeListener listener);
}

public enum ButtonVariant { ELEVATED, FILLED, FILLED_TONAL, OUTLINED, TEXT }
public enum ButtonInteractionMode { CLICKABLE, SELECTABLE }
public enum ButtonSize { XS, S, M, L, XL }
public enum ButtonShape { ROUND, SQUARE }
```

Fluent setters return `this`. Same shape as `ElwhaChip` and `ElwhaIconButton`.

**No M3-factory presets** (no `filledButton(...)` / `tonalButton(...)` static helpers). The variant + label + icon trio is the entire constructor surface; a two-line builder reads clearly enough.

**No `setIcons(rest, selected)` pair.** Unlike `ElwhaIconButton` where icon-swap on toggle is the primary signal, text buttons carry their toggle signal through the variant-specific surface swap (§7) — icon-swap would be redundant and noisy. Single `setIcon(Icon)`.

**No trailing icon.** Deferred per §11; a future split-button primitive will cover the "Label ▾" affordance.

### Label authoring guidance (consumer-facing)

M3 spec is explicit: *"Keep labels concise and use sentence case."* `ElwhaButton` doesn't enforce either rule — case is purely the consumer's responsibility, and there's no length cap on `setText`. But consumers should:

- Use sentence case for labels: "Save changes" rather than "Save Changes" or "SAVE CHANGES".
- Keep labels short — one to three words. A button label is an action verb, not a sentence.
- Reserve longer label runs for `FILLED_TONAL` or `OUTLINED` action rows; long labels in `FILLED` buttons read as overweight in any size.

These are conventions, not constraints. The lib accepts any string.

---

## §5. Defaults

| Property | Default | Rationale |
|---|---|---|
| `variant` | `ButtonVariant.FILLED` | M3 spec — "filled (default)" in the configurations table. Primary action in its region. Diverges from `ElwhaIconButton`'s `STANDARD` default; icon buttons are typically the lowest-emphasis affordance, text buttons typically the highest. |
| `interactionMode` | `ButtonInteractionMode.CLICKABLE` | Push-button is the common case; toggle is opt-in. |
| `buttonSize` | `ButtonSize.S` | M3 spec — "Small (existing, default)" in the size column. Diverges from `ElwhaIconButton`'s `M` default; icon-button's `M` was tied to `MaterialIcons.DEFAULT_SIZE` which doesn't translate to text. |
| `shape` | `ButtonShape.ROUND` | M3 spec — "Round (default)" in the shape column. Capsule (`ShapeScale.FULL`) for all sizes. |
| `borderWidth` | `1` | Matches `OUTLINED` + chip + surface + icon-button. Focus bumps to `2`. |

---

## §6. State model

For non-toggleable variants (`CLICKABLE` mode), the state model is a **5-cell row**:

| State | Surface paint | Border paint | Foreground |
|---|---|---|---|
| Enabled | variant default | variant default | per §3 |
| Disabled | variant fill at 12% opacity | variant border at 12% opacity | content opacity 38% |
| Hovered | `HOVER` overlay (8%) on variant default | variant default | per §3 |
| Focused | (current overlay) | swap to `PRIMARY` at 2 px | per §3 |
| Pressed | `PRESSED` overlay (10%) + ripple animation (see §8) | variant default | per §3 |

For toggleable variants (`SELECTABLE` mode), the state model is a **2×5 grid** — selection axis (Unselected / Selected) crossed with the 5 base states. The Selected row uses the per-variant surface swap defined in §7.

**Per-variant elevation behavior (ELEVATED only):** dp1 shadow at rest, dp0 when disabled. All other variants have no shadow.

**Why uniform overlay vs M3's per-variant surface swap (for ELEVATED + TONAL):** M3 defines an explicit per-state container color for each variant. `ElwhaIconButton` §6 chose to use the M3 toggle-ON color as the variant default and apply uniform `SELECTED` overlay compositing as the selection indicator. That decision applies cleanly to `ELEVATED` and `TONAL` here — their M3 unselected/selected colors are close enough that an overlay reads correctly. It does **not** apply to `FILLED` and `OUTLINED`, where the M3 swap is the only viable selection signal — see §7 for the variant-specific exception.

---

## §7. Toggle pattern + selection color model

`SELECTABLE` triggers a **variant-specific** selection rendering. This is the most-load-bearing M3 divergence pattern in the doc — see §10 for the meta-divergence.

| Variant | Selected rendering | Mechanism |
|---|---|---|
| `ELEVATED` | `SELECTED` overlay (12%) on `surfaceContainerLow`; border swap to `PRIMARY` at 2 px; shadow stays dp1 | Uniform overlay |
| `FILLED` | **Surface swap**: variant default reads `surfaceContainer` / `onSurfaceVariant` in unselected; `PRIMARY` / `ON_PRIMARY` in selected | Variant-specific swap |
| `FILLED_TONAL` | `SELECTED` overlay (12%) on `secondaryContainer`; border swap to `PRIMARY` at 2 px | Uniform overlay |
| `OUTLINED` | **Surface swap**: variant default reads transparent + `outlineVariant` border in unselected; `INVERSE_SURFACE` fill + `INVERSE_ON_SURFACE` foreground in selected (border drops) | Variant-specific swap |
| `TEXT` | **N/A** — `setInteractionMode(SELECTABLE)` throws `IllegalStateException` when variant is `TEXT`; symmetric guard on `setVariant(TEXT)` throws if mode is already `SELECTABLE` | Rejected at runtime |

**Why this hybrid model:**

M3 Expressive uses **shape morph (round ↔ square) + color swap** as two co-equal selection indicators. Elwha v1 defers shape morph (§10) and is therefore left with color as the sole signal.

- For `FILLED`: uniform overlay alone leaves the unselected state visually identical to a regular FILLED push button — the toggle affordance disappears. M3's surfaceContainer → primary swap *is* the affordance. We adopt it verbatim.
- For `OUTLINED`: same reasoning — without the shape morph, the only signal that distinguishes unselected (border-only) from selected (filled) is the surface swap to `INVERSE_SURFACE`. We adopt it.
- For `ELEVATED` + `TONAL`: their M3 unselected and selected fills are close enough (e.g. `secondaryContainer` ↔ `secondary` for TONAL) that a uniform 12% `SELECTED` overlay over the existing fill reads correctly. Symmetric with `ElwhaChip` and `ElwhaIconButton` selection behavior — which matters because these can appear in the same UI region as those primitives.
- For `TEXT`: M3 spec is explicit ("There is no toggle text button"). TEXT has neither surface nor border to carry a selection signal, and consumers who reach for toggle-text-button are reaching for the wrong primitive (they probably want `FILLED_TONAL`).

**Throwing on `TEXT + SELECTABLE`:** fail-fast in the setter. Symmetric guard on both directions:
- `setVariant(TEXT)` throws `IllegalStateException` if `interactionMode == SELECTABLE`.
- `setInteractionMode(SELECTABLE)` throws `IllegalStateException` if `variant == TEXT`.

The exception message names the constraint: `"M3 prohibits toggle on the TEXT variant — choose FILLED_TONAL for a low-emphasis toggle, or FILLED for high emphasis."`

---

## §8. Paint

Same delegation pattern as `ElwhaSurface` / `ElwhaIconButton`, plus a new ripple painter.

```java
// 1. shadow (ELEVATED only)
if (variant == ButtonVariant.ELEVATED && isEnabled()) {
  ShadowPainter.paint(
      (Graphics2D) g,
      getWidth(),
      getHeight(),
      cornerRadiusPx(),
      elevationLevel());                // 1 at rest, 0 when disabled (matches V3 card §9)
}

// 2. round-rect surface + state-layer + border
SurfacePainter.paint(
    (Graphics2D) g,
    getWidth(),
    getHeight(),
    cornerRadiusPx(),                   // FULL for ROUND, table lookup for SQUARE (see Appendix B)
    effectiveSurfaceRole,               // null for OUTLINED/TEXT at rest, swapped per §7 when selected
    activeOverlay,                      // HOVER / PRESSED / SELECTED / null
    effectiveBorderRole,                // null / OUTLINE_VARIANT / PRIMARY (focus, selected ELEVATED/TONAL)
    effectiveBorderWidth);              // 1, bumped to 2 when focused or selected (non-FILLED/OUTLINED)

// 3. ripple — when pressed within the last 400ms
if (rippleProgress < 1f && rippleOrigin != null) {
  RipplePainter.paint(
      (Graphics2D) g,
      rippleOrigin,
      rippleProgress,
      cornerRadiusPx(),                 // ripple clips to the round-rect path — see ElwhaCard:1197
      resolveForegroundColor());        // ripple tint matches foreground at low opacity
}

// 4. icon + label
//    Icon paints at leading edge using FlatSVGIcon.ColorFilter keyed off resolveForegroundColor().
//    Label paints inline-after-icon with the per-size gap from Appendix A.
//    Vertical centering; horizontal centering inside (paddingL .. paddingR).
```

`setOpaque(false)` in the constructor — parent background shows through round-rect corners and (for OUTLINED/TEXT) the transparent body.

**`RipplePainter` extraction:** the current ripple paint lives in `ElwhaCard:1197-1209` (`paintRipple`) with timer wiring in `ElwhaCard:157-161, 1250` and `RIPPLE_TOTAL_MS = 400` (250 ms expand + 150 ms fade tail). Story 2 of #103 extracts these ~50 lines into `com.owspfm.elwha.theme.RipplePainter` as a static utility (parallel to `SurfacePainter`). The card refactor follow-up (filed against #80) replaces the inline `paintRipple` with a `RipplePainter.paint(...)` call. Clip-to-round-rect behavior preserved verbatim — the operator confirmed in spec session that ripple should NOT overflow the container.

**Shadow (`ELEVATED`) paint via `ShadowPainter`:** the spike ([shadow-spike-2026-05-19.md](shadow-spike-2026-05-19.md)) rejected FlatLaf's `FlatDropShadowBorder` on visual grounds — FDSB paints a rectangular halo that doesn't follow round corners (visible 90°-corner stubs at higher elevations, worst in dark mode). FDSB's perf advantage (30× on cold paint, 200× on resize loops) came from a size-independent cache; this spec adopts that insight without adopting FDSB itself.

`ShadowPainter` is a new static helper at `com.owspfm.elwha.theme.ShadowPainter`, parallel to `SurfacePainter` and `RipplePainter`. It extracts the round-rect Gaussian-blur logic currently inside `SurfacePainter.renderShadowImage` (two-pass `ConvolveOp` box blur) and redesigns the cache:

- **Old cache (in `ElwhaSurface`):** per-instance, key = `(bodyW, bodyH, arc, elevation)`. Any geometry change invalidates. Per-frame invalidation under animation tanks frame rate — PR #110 worked around this by freezing the cache during the collapse tween (`setSuspendShadowRecompute`).
- **New cache (in `ShadowPainter`):** shared static cache, key = `(arc, elevation)` only. Body size is decoupled — the cached shadow image is generated at a fixed canonical size (e.g. arc + 4 × shadow-blur-radius) and **9-sliced** at paint time to fit any host body. Soft-reference eviction bounds memory. No per-frame invalidation; PR #110's `setSuspendShadowRecompute` becomes dead code that the surface migration can delete.

Story flow:
- New paint helper story extracts both `RipplePainter` and `ShadowPainter` (paint-helper prerequisites) — see story plan update in the post-spike thread.
- `ElwhaButton` ELEVATED variant consumes `ShadowPainter` from day one.
- Follow-up issue against epic #80 migrates `ElwhaSurface` (and therefore `ElwhaCard`) onto `ShadowPainter` and deletes `setSuspendShadowRecompute`.

---

## §9. Accessibility

- `JComponent` subclass with a custom `AccessibleContext` (same approach as chip / icon-button).
- `AccessibleRole.PUSH_BUTTON` when `CLICKABLE`; `AccessibleRole.TOGGLE_BUTTON` when `SELECTABLE`.
- `getAccessibleName()` returns `getText()`, falling back to `getToolTipText()` if label is null/empty, falling back to `setName(String)` value, falling back to `"Button"`. Text buttons should always have a non-empty label in practice; the fallback chain matches `ElwhaIconButton` for consistency.
- Keyboard activation: `VK_SPACE` and `VK_ENTER` when focused. Same bindings as chip and icon-button.

**Target inflation for XS / S:** the M3 measurements panel notes *"Extra small and small icon buttons must have a target size of 48×48 dp or larger to be accessible."* The note specifies icon buttons but applies family-wide (WCAG 2.5.5 / M3 a11y guideline). XS button is 32 dp tall, S is 40 dp — both below the 48 dp minimum.

`ElwhaButton` inflates its `JComponent` bounds to a 48 dp **minimum** when `buttonSize` is `XS` or `S`. Implementation: override `getPreferredSize()` to return the visual size; override `getMinimumSize()` to ensure 48 dp on the cross axis. The visual button stays at the M3-spec dimensions (32 / 40); the inflated bounds increase the layout-known touch target. Layout managers see the real target, sibling components don't overlap the hit area, and consumers don't need to remember to pad XS/S buttons themselves.

A follow-up issue should backfill this rule to `ElwhaIconButton` XS / S (currently lacking the inflation). Filed against epic #45 as a cleanup task; noted in §10.

### 3:1 contrast constraint for `setSurfaceRole` overrides

M3 spec footnote: *"These color roles were chosen to create design coherence and familiarity. Other color roles can be used as long as the container and text have a 3:1 contrast ratio."*

The variant defaults in §2 are all 3:1-or-better against their resolved foreground. When a consumer calls `setSurfaceRole(ColorRole)` to override the variant default, **the burden of meeting 3:1 falls on the consumer**. ElwhaButton does not validate the override — the override may produce an inaccessible result (e.g. `setSurfaceRole(SURFACE_VARIANT)` on a `FILLED` button paints `OnPrimary` text on a near-`Surface` background, well under 3:1).

Consumers using `setSurfaceRole` should hand-verify the override pair (typically by inspecting `getEffectiveSurfaceRole()` + `resolveForegroundColor()` against a contrast checker) or test in a high-contrast scenario. Future a11y tooling work could add a runtime contrast assertion behind a dev flag; out of scope for v1.

---

## §10. Deliberate M3 divergences

| M3 spec says | Elwha does | Why |
|---|---|---|
| Shape morphs on press (more square) AND on selected (round ↔ square inversion) | No shape morph. `setShape` is static for the component's lifetime. | Shape morph is an animation requiring per-frame radius interpolation and (for selection) a state-driven shape swap. Elwha v1 is static-paint pre-v2 (same divergence as `ElwhaIconButton` §10). Defer to v2 animation epic. |
| Selection: per-variant surface swap (e.g. FILLED toggle-off uses `surfaceContainer`, toggle-on uses `primary`) | **Hybrid**: variant-specific swap for FILLED + OUTLINED, uniform 12% `SELECTED` overlay for ELEVATED + TONAL, throw for TEXT | Pure-uniform-overlay (the IconButton choice) leaves unselected FILLED/OUTLINED toggles visually identical to their regular push-button siblings — without shape morph backing them up, color is the only signal and uniform overlay isn't strong enough. ELEVATED and TONAL toggle colors are close enough that a uniform overlay reads correctly. See §7. |
| Focused state: outer ring with a gap around the container | Border-color swap to `PRIMARY` at 2 px width — no separate ring, no gap | Matches `ElwhaIconButton` §10. Elwha-internal focus indicator consistency beats spec fidelity here. A future v2 epic could move all primitives onto the M3 outer-ring style together. |
| Pressed state-layer extends past container (ripple overflow) | Ripple paints clipped to the round-rect path | Matches `ElwhaCard`'s existing behavior; the operator explicitly confirmed clipping is the desired posture. Ripple animation included (not deferred), just contained. |
| Buttons have 3 width variants (narrow / default / wide) | Single width = preferred-size driven | M3 Expressive territory beyond the core spec; defer to v2 if a real consumer need surfaces. Mirrors `ElwhaIconButton` §10. |
| Two-shadow stack per M3 elevation token (key + ambient pair) | Single Gaussian-blur shadow (the current `ElwhaSurface` approach, ported into `ShadowPainter`) | M3 elevation tokens specify a key + ambient shadow pair per level. Elwha v1 paints a single Gaussian-blurred drop shadow per level — the result reads correctly across all 5 levels in the existing `ElwhaCard` and the spike's visual comparison confirmed it remains acceptable. Adopting M3's key+ambient pair would double shadow compute and is a candidate future enhancement; the `ShadowPainter` API leaves room (an `elevation`-keyed lookup table per level) to swap the implementation without touching consumers. |

The divergences are documented here, not silently absorbed. Where the strict M3 spec is needed, `setSurfaceRole` (where applicable) is the consumer-side escape hatch.

**Cross-component follow-up (noted not enacted):** `ElwhaIconButton` should adopt the same XS/S 48dp target inflation as `ElwhaButton` (§9). Filed against epic #45 as a cleanup task post-#103.

---

## §11. Out of scope (v1)

| Deferred | Why deferred |
|---|---|
| **Trailing icon** (`Label ▾`) | M3 spec doesn't define a trailing-icon button — that's a split-button or menu-button composition. Separate primitive if a real need surfaces. Same precedent as `ElwhaIconButton` §11. |
| **Shape morph** (on press and on selection) | Animation, requires per-frame interpolation and a state-driven shape swap. v2 animation epic. |
| **Width variants** (narrow / wide) | M3 Expressive beyond core spec. Defer until a real consumer need surfaces. |
| **Ripple overflow** (M3-style extending past the container) | Operator confirmed clip-to-container is the desired posture. Not a "deferral" — a deliberate ongoing divergence. |
| **FAB / Extended FAB** | Different size + elevation system; separate component if OWS needs it. |
| **Split button** (button + trailing menu chevron with separate hit regions) | Composite — separate primitive if a real need surfaces. |
| **Selected icon swap** (`setIcons(rest, selected)` pattern from `ElwhaIconButton`) | Text buttons carry toggle signal through variant-specific surface swap (§7); icon swap would be redundant noise. Single `setIcon`. |
| **`ButtonSize` axis on `ElwhaIconButton`** for the 48dp target inflation gap | A backfill cleanup task filed against epic #45 (see §10). |

---

## §12. Validation surface (playground sub-issue)

Story 5 of epic #103 builds the playground surface. Three tabs exercise the full matrix:

- **Variant gallery tab.** 5 variants × 6 states (Enabled / Disabled / Hovered / Focused / Pressed + Selected for the four toggleable ones) at default size S, live-rerendered on theme/mode switch — the binding-rule contract end-to-end. For toggleable variants, also shows the 2×5 selection grid.
- **Sizes tab.** 5 sizes (XS / S / M / L / XL) × 2 shapes (Round / Square) × default variant (FILLED), demonstrating per-size measurements (height, padding, icon size, gap) and per-size corner radius for square. Shows the 48dp target inflation visually on XS / S.
- **Toggle examples tab.** Realistic toggle patterns — Filter ON/OFF rows (FILLED toggle), Pin/Unpin rows (FILLED_TONAL toggle), Active/Inactive rows (OUTLINED toggle), Subscribed/Unsubscribed rows (ELEVATED toggle). Each row also demonstrates `ButtonGroup` mutex-selection across 3 buttons.

All three tabs factored into `ButtonPlaygroundPanels` so the standalone `ElwhaButtonPlayground` and `ThemePlayground`'s new `Button` tab compose the same instances. Same factored-builder pattern `ChipPlaygroundPanels` / `IconButtonPlaygroundPanels` / `SurfacePlaygroundPanels` already establish.

---

## §13. CHANGELOG

`[Unreleased]` gets a new `### Added` entry for the epic, with sub-entries:

- `research(#103)` — this doc + shadow-spike-2026-05-19.md
- `feat(#103)` — paint helpers: `RipplePainter` (extracted from `ElwhaCard.paintRipple`) + `ShadowPainter` (extracted from `SurfacePainter.renderShadowImage` with redesigned size-independent cache that eliminates PR #110's `setSuspendShadowRecompute` workaround at the root)
- `feat(#103)` — `ElwhaButton` + `ButtonVariant` + `ButtonInteractionMode` + `ButtonShape`; consumes both new paint helpers
- `feat(#103)` — `ButtonSize` axis (XS / S / M / L / XL), per-size measurement table, square corner-radius lookup, XS/S 48dp target inflation
- `feat(#103)` — `ButtonGroup` mutex-selection container
- `feat(#103)` — `ButtonPlaygroundPanels` + standalone `ElwhaButtonPlayground` + `ThemePlayground` `Button` tab
- `feat(#103)` — V3 `ElwhaCardActions` integration: card playground swaps raw `JButton` for `ElwhaButton.<variant>` per the card↔button pairing table; V1→V3 migration doc updated with canonical action-button pattern

A separate `### Changed` entry covers the follow-up `ElwhaCard.paintRipple` refactor onto `RipplePainter`, filed against #80.

---

## Appendix A. Per-size measurements

| Size | Container H (dp) | Horiz padding, no icon (L / R) | Horiz padding, with icon (L / gap / R) | Icon size (dp) | Square corner (dp) | Pressed corner (dp, v2 only) |
|---|---|---|---|---|---|---|
| `XS` | 32 | 12 / 12 | 12 / 4 / 12 | 20 | 12 | 8 |
| `S` (default) | 40 | 16 / 16 | 16 / 8 / 16 | 20 | 12 | 8 |
| `M` | 56 | 24 / 24 | 24 / 8 / 24 | 24 | 16 | 12 |
| `L` | 96 | 48 / 48 | 48 / 12 / 48 | 32 | 28 | 16 |
| `XL` | 136 | 64 / 64 | 64 / 16 / 64 | 40 | 28 | 16 |

**Round shape:** all sizes use `ShapeScale.FULL` (capsule). No per-size value needed; `min(width, height) / 2` is clamped inside `SurfacePainter`.

**Pressed corner:** captured for completeness; not used in v1 (shape morph deferred).

**A11y target inflation:** for `XS` and `S`, the `JComponent` `minimumSize` cross-axis is 48 dp (the visible button stays 32 or 40 dp). See §9.

---

## Appendix B. Square corner radius lookup

Per-size square corner radii (Appendix A column) hardcoded inside `ElwhaButton`:

```java
private static final int[] BUTTON_SQUARE_CORNERS_DP = {
    12,  // XS
    12,  // S
    16,  // M
    28,  // L
    28   // XL
};

private int cornerRadiusPx() {
  return shape == ButtonShape.ROUND
      ? Integer.MAX_VALUE   // SurfacePainter clamps to min(w,h)/2 → capsule
      : BUTTON_SQUARE_CORNERS_DP[buttonSize.ordinal()];
}
```

This lookup lives inside `ElwhaButton`, not in `ShapeScale`. `ShapeScale` is a cross-component facade and shouldn't carry button-specific values; the per-size variation is a button concern owned by the button.

---

## Appendix C. Color tokens per state (M3 source-of-truth table)

| Variant | Default | Toggle unselected | Toggle selected |
|---|---|---|---|
| `ELEVATED` | `surfaceContainerLow` / `Primary` (+ dp1 shadow) | `surfaceContainerLow` / `Primary` | `surfaceContainerLow` (+ 12% SELECTED overlay) / `Primary`; border swap to `PRIMARY` at 2 px |
| `FILLED` | `Primary` / `OnPrimary` | **`surfaceContainer` / `OnSurfaceVariant`** | **`Primary` / `OnPrimary`** |
| `FILLED_TONAL` | `secondaryContainer` / `OnSecondaryContainer` | `secondaryContainer` / `OnSecondaryContainer` | `secondaryContainer` (+ 12% SELECTED overlay) / `OnSecondaryContainer`; border swap to `PRIMARY` at 2 px |
| `OUTLINED` | transparent + `outlineVariant` border / `OnSurfaceVariant` | transparent + `outlineVariant` border / `OnSurfaceVariant` | **`inverseSurface` (border drops) / `inverseOnSurface`** |
| `TEXT` | transparent / `Primary` | — (rejected) | — (rejected) |

**Bold cells** mark the variant-specific surface swaps Elwha implements verbatim (FILLED + OUTLINED). Non-bold cells in the Selected column use the uniform `SELECTED` overlay mechanism with border swap (ELEVATED + TONAL). TEXT rejects toggle.

All required color roles are already exposed in `ColorRole`:
- `SURFACE_CONTAINER_LOW` (line 206)
- `SURFACE_CONTAINER` (213)
- `SECONDARY_CONTAINER` + `ON_SECONDARY_CONTAINER` (81, 88)
- `OUTLINE_VARIANT` (244)
- `INVERSE_SURFACE` + `INVERSE_ON_SURFACE` (254, 261)
- `PRIMARY` + `ON_PRIMARY`, `SURFACE_VARIANT` + `ON_SURFACE_VARIANT` (existing)

No new tokens required.
