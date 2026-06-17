# ElwhaLoadingIndicator — Phase 0 Design

Epic: [#468](https://github.com/OWS-PFMS/elwha/issues/468). Research capture:
`elwha-loading-indicator-research.md` (cited as R§…). Sibling: ElwhaProgressIndicator #467 (separate
component — the loading indicator is the short-wait shape-morph spinner, not a progress variant). Milestone:
**v0.5.0** (R§M.7).

## TL;DR — the locked decisions

1. **One concrete component** — `ElwhaLoadingIndicator extends JComponent implements Accessible` in a new
   `loading/` package. No abstract base (unlike progress's linear/circular split): the loading indicator has a
   **single geometry** (the morphing polygon); *contained* is a backing-circle toggle and
   *indeterminate/determinate* is a mode — not separate primitives.
2. **New, self-contained shape-morph engine** (R§E, the load-bearing call). Elwha has **no** rounded-polygon
   machinery — `ShapeMorphPainter` only lerps corner radii on a rectangle. Because every M3 loading-indicator
   shape is **star-convex** (R§D), the engine represents each shape as a **radius profile `r(θ)`** and morphs
   by **per-angle lerp** of two profiles — seamless by construction, no feature-matching `DoubleMapper` port.
   Package-private in `loading/` for V1; extract to `theme/` only when a 2nd consumer appears.
3. **`BoundedRangeModel` for determinate progress** — the slider/progress precedent; lets the Showcase share
   one model between a slider and the indicator. Indeterminate ignores the model.
4. **Mode default = indeterminate** (R§J — the loading indicator's whole point is short *indeterminate* waits).
   Determinate is the secondary mode (R§I), in V1.
5. **Choreography = the Compose constants, deterministically interpreted** (R§F): 4666 ms linear full-spin +
   650 ms morph-step dwell + 90° kick per completed morph; the per-morph spring is approximated by an eased
   tween computed from elapsed time (reproducible for headless smoke — the progress-epic precedent).
6. **Zero new theme tokens** (R§K): `PRIMARY` (standard active) / `PRIMARY_CONTAINER` (container) /
   `ON_PRIMARY_CONTAINER` (contained active); all sizes/durations/shape data are component constants.
7. **Non-interactive**: no hover/focus/pressed, not focusable. A11y = `PROGRESS_BAR` + `AccessibleValue` (null
   while indeterminate) + `BUSY`, mirroring `AbstractElwhaProgressIndicator` (R§M.6).
8. Animation clock runs **only while showing and only in indeterminate mode** (hierarchy-gated, stopped on
   `removeNotify`); reduced-motion freezes it on a static settled shape. Determinate never runs the clock.

## §1. Scope — what V1 ships

Standard + contained · indeterminate (7-shape M3 loop) + determinate (Circle→SoftBurst progress morph) ·
indicator-size + container-size knobs · per-part color-role setters · reduced-motion freeze · a11y · Showcase
Workbench + Gallery. **Determinate is in V1** (M3 ships it; cheap once the engine exists — no silent cut). Out
of scope → §10.

## §2. Architecture [RECOMMENDED; locked by the S1 spike]

### The shape-morph engine (package-private, `loading/`)

- `RoundedPolygonShape` (or `LoadingShape`) — an immutable shape described by a **radius profile**: a
  `float[N]` of radii sampled at uniform angles `θ_k = 2πk/N` (N ≈ 144), centroid-relative, normalized so the
  profile fits the unit circle (max radius 1). Built once per named shape from a rounded-polygon spec:
  1. construct the sharp polygon (vertices, or star outer/inner radii);
  2. round corners (quad/cubic fillets per the vertex `cornerRadius`);
  3. flatten the outline to a dense polyline (`PathIterator`, small flatness);
  4. since star-convex, the polyline's vertex angles are monotonic around the centroid → resample to uniform
     `θ_k` by linear interpolation of `r`.
- `ShapeMorph.lerp(profileA, profileB, t)` → `float[N]` per-index `lerp`. Seamless because both profiles are
  closed periodic functions over the same `θ_k` grid; **no feature alignment needed**.
- `render(g2, cx, cy, scale, rotationRad, profile, color)` — map each `(θ_k + rotation, r_k·scale)` to a point,
  build a closed `Path2D`, fill. Rotation is a phase offset on the sampling angle.
- `LoadingShapes` — the static catalog: the 8 named profiles (§3) + the indeterminate sequence (7) + the
  determinate sequence (2).

This is the **fidelity trade** (R§E): the true `androidx.graphics.shapes.Morph` feature-matches corners; the
radial lerp matches by angle. For a shape that is also continuously rotating through 7 forms, radial morph is
visually faithful and a fraction of the code/risk. **The S1 spike builds the engine + 2 shapes + 1 morph and
validates by eye** before the rest of the catalog is committed.

### The component

- `ElwhaLoadingIndicator extends JComponent implements Accessible` — owns: `BoundedRangeModel`
  (`DefaultBoundedRangeModel(0,0,0,100)`), `indeterminate` (default `true`), `contained` (default `false`),
  color roles (indicator / container), `indicatorSize` (38), `containerSize` (48), and one `javax.swing.Timer`
  (~60 fps) gated by an animation-demand recompute on a `HierarchyListener` (the progress/slider/fab pattern).
- Constructors: `()`, `(BoundedRangeModel)`. Per-variant static factories: `contained()`, `determinate()`,
  `determinate(BoundedRangeModel)`, `containedDeterminate()`. Factories own the M3 color pairing (contained →
  container `PRIMARY_CONTAINER` + indicator `ON_PRIMARY_CONTAINER`); `setContained(boolean)` toggles only
  whether the container paints (no magic role-swap — the per-variant factory is the blessed path).
- Opaque `false`; `Graphics2D` antialiasing; pure `JComponent`, no FlatLaf UI delegate.

## §3. The shapes (reconstructed; tuned at S1/smoke)

Visually-faithful radial-profile reconstructions of the M3 shapes (R§D byte-exact vertex lists not captured —
documented faithful-interpretation):

| Shape | Reconstruction | Notes |
|---|---|---|
| **Circle** | regular 10-gon (≈ circle) | determinate start |
| **Sunny** | 8-point star, inner 0.8, corner round 0.15 | gentle 8-petal |
| **Cookie9Sided** | 9-point star, inner 0.8, corner round 0.5, rot −90° | scalloped |
| **Cookie4Sided** | 4-point star, inner ≈0.55, heavy round | deep 4-scallop |
| **Pentagon** | 5-gon, corner round ≈0.17 | rounded pentagon |
| **Pill** | rounded rect ≈1.0×0.66, full end-round | capsule |
| **Sunny/Oval** | circle scaled (1, 0.64), rot −45° | tilted oval |
| **SoftBurst** | 10-point star, inner ≈0.85, high round | soft 10-lobe burst |

Indeterminate sequence (R§C): `SoftBurst → Cookie9Sided → Pentagon → Pill → Sunny → Cookie4Sided → Oval →`
(wrap to SoftBurst). Determinate sequence: `Circle → SoftBurst`.

## §4. Tokens & color [zero new tokens — LOCKED]

- Standard active: `ColorRole.PRIMARY`. Contained: container `ColorRole.PRIMARY_CONTAINER` + active
  `ColorRole.ON_PRIMARY_CONTAINER` (R§H). Per-part setters `setIndicatorColorRole(...)`,
  `setContainerColorRole(...)` (component-API role-exposure doctrine).
- `Easing.EMPHASIZED` (morph settle approximation), `Easing.LINEAR` (global spin). Durations/sizes = private
  constants (R§F/§G).

## §5. Measurements & geometry (R§G)

- Standard preferred size = `indicatorSize` (38) square + insets. Contained preferred size = `containerSize`
  (48) square + insets; the 38 active shape centers in the 48 container → 5 px inset per side
  (`ActiveIndicatorScale ≈ 0.792`). The morph profile is scaled to fit `indicatorSize` (the rotating shape
  stays within its 38 box at all morph phases — profiles are normalized to max-radius 1, so
  `scale = indicatorSize/2`).
- Container = a filled circle, radius `min(w,h)/2`, painted **behind** the shape (contained only).
- `getMinimumSize` / `getMaximumSize` = `getPreferredSize` (fixed-size widget; no shadow halo).

## §6. Modes & motion (R§F/§I)

- **Indeterminate** (clock-driven, deterministic from elapsed ms):
  - `STEP_MS = 650`; `stepIndex = ⌊elapsed/STEP_MS⌋`; `morphIndex = stepIndex % 7` selects pair
    `(shapes[morphIndex] → shapes[(morphIndex+1) % 7])`.
  - `localT = EMPHASIZED.ease(clamp((elapsed mod STEP_MS)/MORPH_MS, 0, 1))`, `MORPH_MS ≈ 450` (settles inside
    the 650 step, then dwells — the Compose `delay(650); await(spring)` shape; tune at smoke).
  - `globalRotation = (elapsed/4666)·360°` (linear). `rotation = globalRotation + stepIndex·90° + localT·90°`
    — reproduces Compose `morphProgress·90 + morphRotationTargetAngle + globalRotation`, all clockwise.
  - Current profile = `ShapeMorph.lerp(shapes[morphIndex], shapes[next], localT)`.
- **Determinate** (no clock; repaint on model change): `p = fraction`; profile =
  `ShapeMorph.lerp(Circle, SoftBurst, p)`; `rotation = −p·180°` (R§I). Value-jump tween deferred (§10) — like
  progress, consumers driving increments get smooth motion for free.
- **Reduced motion** (`MorphAnimator.isReducedMotion()`): indeterminate freezes — no clock, render a single
  static settled shape (the first sequence shape, no rotation). `[DOC]` M3 doesn't prescribe this; Elwha
  honors its OS-detected flag.
- **Headless reproducibility**: when not showing, the clock is stopped and paint uses a deterministic settled
  phase; indeterminate smokes sample across the cycle with explicit elapsed stamps / sleeps (progress-epic
  precedent).

## §7. Accessibility (R§J/§M.6)

`AccessibleRole.PROGRESS_BAR`; `AccessibleValue` backed by the model — `getCurrentAccessibleValue()` returns
`null` while indeterminate; `AccessibleState.BUSY` while indeterminate. Not focusable. Javadoc instructs
naming the activity via `getAccessibleContext().setAccessibleName("Loading…")`. Contained: Javadoc notes the
≥3:1 active/container contrast guidance.

## §8. Showcase pattern (R§J)

One "Loading" catalog entry → Workbench + Gallery tabs (the tabs/progress pattern). **Workbench**: a live
indicator, controls dogfooding Elwha — `ElwhaSelectField` (mode: indeterminate / determinate), `ElwhaSwitch`
(contained), `ElwhaSlider` (determinate value 0–100; indicator size 24–64), `ElwhaButton` (simulate-load:
ramps a determinate fill then resets). **Gallery**: the matrix — standard/contained × indeterminate +
determinate-at-several-fills + a size row.

## §9. API surface (sketch)

```java
new ElwhaLoadingIndicator();                       // standard, indeterminate, PRIMARY
ElwhaLoadingIndicator.contained();                 // contained indeterminate (primaryContainer + onPC)
ElwhaLoadingIndicator.determinate();               // standard determinate over [0,100]
ElwhaLoadingIndicator.determinate(model);          // determinate over a shared model
ElwhaLoadingIndicator.containedDeterminate();
// setters: setIndeterminate, setContained, setIndicatorColorRole, setContainerColorRole,
//          setIndicatorSize, setContainerSize, setValue/getModel/addChangeListener
```

## §10. Out of scope (documented, not cut)

| Item | Why | Disposition |
|---|---|---|
| Consumer-overridable shape sequence (`polygons`) | the M3 sequence *is* the product; custom sets are niche | documented deferral; file on consumer demand |
| Animated determinate value-jump tween (spring) | separable polish; consumers can drive increments | V2 candidate |
| True feature-matched `Morph` (squircle smoothing fidelity) | radial lerp is visually faithful for a spinning morph | revisit only if a static shape consumer needs exactness |
| Color-change-across-sequence (multi-color morph, R§J) | M3 *optional*; single-color is the default look | file on demand |
| Pull-to-refresh / inline-button host integration | composition concern, consumer-side | consumer recipe, not lib |

## §11. Phasing → stories (Phase 1 = V1, single phase)

| Story | Scope | §refs |
|---|---|---|
| S1 *(spike)* | `loading/` package + radial shape engine (`RoundedPolygonShape` profile + `ShapeMorph.lerp` + render) + `ElwhaLoadingIndicator` skeleton + **standard indeterminate render of 2 shapes through 1 morph** (validate the engine by eye) | §2–§3 |
| S2 | full 8-shape catalog + the 7-shape indeterminate choreography (morph cycle + 4666 ms spin + 90° kicks) + clock lifecycle (demand/hierarchy-gated, reduced-motion freeze) | §3, §6 |
| S3 | contained configuration (48 container circle + 38 inset shape + `PRIMARY_CONTAINER`/`ON_PRIMARY_CONTAINER` pairing) | §4–§5 |
| S4 | determinate mode (Circle→SoftBurst progress morph, −180° rotation, `BoundedRangeModel`) | §6, §9 |
| S5 | sizing knobs + a11y (`PROGRESS_BAR`/`AccessibleValue`/`BUSY`) + reduced-motion + RTL-neutrality doc | §5, §7 |
| S6 | Showcase Workbench + Gallery + CHANGELOG (closes #468) | §8 |

Fresh demo per story + headless smoke per story (lib rule).

## §12. Open for the S1 spike

1. Radial profile resolution `N` (144 vs 180) — wave/scallop crispness vs path-build cost.
2. Corner-rounding construction: quadratic fillet vs cubic — which reads closest to M3 at 38 px.
3. Whether the rotating morph stays inside the 38 box at every phase, or needs a small safety inset
   (profiles are max-radius-1 normalized, so in principle yes — verify the scalloped shapes don't clip).
4. The `MORPH_MS` settle duration + easing that best matches the Compose spring's feel at 650 ms steps.

### S1 spike outcome (2026-06-17)

1. §12.1 → **N = 180** (2° steps). At 38–64 px the polyline chord is sub-pixel; static build cost
   (8 shapes × 180 rays × ~200 segments) is negligible — locked.
2. §12.2 → **quadratic fillet** (one `quadTo` per corner, trim = `roundness · ½·shorterEdge`). Reads
   cleanly at 38 px; rounding is captured into the radial profile, so the rendered polyline reproduces
   it. The `roundness` knob is a `[0,1]` fraction of the shorter adjacent edge — intuitive and bounded
   (M3's absolute `CornerRounding.radius` is reconstructed by eye, not ported).
3. §12.3 → no clipping. Profiles normalize to max-radius 1; render scale = `box/2 − 1` (1 px margin).
   Verified visually — all 8 shapes + the SoftBurst→Cookie9 morph filmstrip sit inside the box.
4. Engine **validated by `LoadingShapeEngineSmoke` + the contact-sheet PNG**: the radial-`r(θ)` lerp is
   seamless and every shape is faithful after one tuning pass (Cookie4Sided softened: inner-ratio
   0.52 → 0.68 — the deep-star reconstruction read too pointy vs M3's cushion). The two new engine
   classes (`RoundedPolygonShape` profile + ray-cast sampler, `ShapeMorph` lerp + `toPath`) held with
   no architecture fights — **locked for S2–S6.** `MORPH_MS`/easing (§12.4) deferred to S2 where the
   clock lands; the contact-sheet harness is the tuning instrument.
