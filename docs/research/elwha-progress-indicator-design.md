# ElwhaProgressIndicator — Phase 0 Design

Epic: [#467](https://github.com/OWS-PFMS/elwha/issues/467). Research capture: `elwha-progress-indicator-research.md` (cited as R§…). Companion stub: ElwhaLoadingIndicator #468 (separate component).

## TL;DR — the locked decisions

1. **Two concrete primitives + one abstract base** in `progress/`: `AbstractElwhaProgressIndicator` (value model, mode, colors, thickness, wave knobs, animation clock) → `ElwhaLinearProgressIndicator`, `ElwhaCircularProgressIndicator`. MDC's class split; M3-exact nouns (R§P).
2. **`BoundedRangeModel` value model** — the ElwhaSlider precedent. `getValue/setValue/getMinimum/setMinimum/getMaximum/setMaximum/getModel/addChangeListener` mirror the slider; `setIndeterminate(boolean)` switches mode.
3. **Updated-M3 anatomy, not the web baseline**: active (`PRIMARY`) / track (`SECONDARY_CONTAINER`) / 4dp gap / 4dp stop dot (linear determinate only). Visible circular track with gaps at both arc ends (R§S, R§B).
4. **Wavy = Expressive shape knob**: `setWavy(boolean)`; amplitude 3px linear / 1.6px circular; wavelength 40px det / 20px indet (linear), 15px (circular); speed one wavelength/s; determinate amplitude ramps 0 outside [10%, 95%] (animated 500ms, `STANDARD` in / `EMPHASIZED_ACCELERATE` out). Only the **active indicator** waves; track stays flat (R§S).
5. **Indeterminate motion = the current Compose choreography**: linear 1750ms two-line cycle (R§C table), `EMPHASIZED_ACCELERATE`; circular 6000ms cycle + 1080° global rotation + 360° advance kick every 1500ms/300ms, `STANDARD`.
6. **Zero new theme tokens** (R§Tokens). Geometry = component constants. **Non-interactive**: no state layers, not focusable.
7. Animation timers run **only while showing** and only when something animates (indeterminate, wave speed > 0, amplitude transition) — hierarchy-listener gated, slider/fab precedent.

## §1. Scope — what V1 ships

Linear + circular · determinate + indeterminate · flat + wavy · thickness knob (4 default, 8 "thick" reference) · RTL (linear) · a11y (`PROGRESS_BAR` role + `AccessibleValue`) · Showcase Workbench + Gallery. Out of scope → §10.

## §2. Architecture [RECOMMENDED; locked via S1 spike]

- `progress/AbstractElwhaProgressIndicator extends JComponent` — owns: `BoundedRangeModel` (default `DefaultBoundedRangeModel(0, 0, 0, 100)`), `indeterminate` flag, color roles (indicator / track / stop), `trackThickness`, `indicatorTrackGapSize`, wave state (`wavy`, `waveAmplitude`, `wavelengthDeterminate`, `wavelengthIndeterminate`, `waveSpeed`, animated amplitude fraction + phase), one shared `javax.swing.Timer` (~60fps tick) started/stopped by an animation-demand recompute on mode/visibility changes (`HierarchyListener`).
- Concrete classes own geometry + painting only. Constructors mirror the slider: `()`, `(min, max, value)`, `(BoundedRangeModel)`. Per-variant static factories: `indeterminate()`, `wavy()`, `wavyIndeterminate()` on each concrete class.
- `getProgressFraction()` → clamped `[0,1]` from the model (extent-aware: `(value − min) / (max − min − extent)` denominator guard).
- Opaque `false`; paints with `Graphics2D` antialiasing; no FlatLaf UI delegate (pure `JComponent`, the slider/switch pattern).
- Model changes repaint via a `ChangeListener`; determinate **value animation is NOT in V1** (M3 animates progress jumps over 500ms linear — deferred §10; consumers driving frequent small increments get smooth motion for free).

## §3. Anatomy / primitives

Painted parts only — no child components: active indicator, track, stop indicator (linear). No labels (consumers compose their own; M3 shows none inside the component).

## §4. Tokens & color [zero new tokens — LOCKED]

`ColorRole.PRIMARY` (active + stop), `ColorRole.SECONDARY_CONTAINER` (track); per-part role setters (`setIndicatorColorRole`, `setTrackColorRole`, `setStopIndicatorColorRole`) per the component-API border-role-exposure doctrine. `Easing.STANDARD`, `Easing.EMPHASIZED_ACCELERATE`, `Easing.LINEAR`. Durations/geometry = private constants (R§Tokens).

## §5. Measurements & geometry

**Linear** (R§S table): thickness 4 (knob; 8 = thick reference). Preferred size `240 × max(thickness, thickness + 2·amplitudeReserve)` where `amplitudeReserve = wavy ? 3 : 0` → 4 / 10 / 8 / 14 ✓. Max width unbounded (fills layout), height capped at preferred. Outer ends full-round; **inner ends (gap-facing) 2px radius** — painted as asymmetric-corner capsule `Path2D` (own path building; `CornerRadii` is real-radius semantics, no shadow-silhouette coupling here). Stop dot Ø 4 centered in the last 4px of the track (`StopTrailingSpace` 0); hidden when the active head reaches it, hidden in indeterminate. Gap collapses gracefully near 0%/100% (track segment clamps to ≥ 0; no negative arcs).

**Circular**: Ø = `indicatorSize` base 40 **+ (thickness − 4) + (wavy ? 8 : 0)** (40/44/48/52 ✓ R§S); the knob sets the *flat-default* base. Arc starts 12 o'clock (−90°), clockwise. Gap in degrees = `(gapPx + thickness) / (π·Ømid)` · 360 at both active ends (cap-aware: round caps extend half a thickness). Track = remaining arc, round caps. No stop indicator.

## §6. Modes & motion

- **Determinate**: active spans `fraction` of the run; track spans the rest minus gap. Wavy amplitude target = `fraction ≤ 0.10 || fraction ≥ 0.95 ? 0 : 1`, animated 500ms (`STANDARD` up, `EMPHASIZED_ACCELERATE` down); wave phase advances `waveSpeed` px/s (default = active wavelength/s).
- **Linear indeterminate**: 1750ms loop, two [tail, head] line spans per R§C keyframe table, all channels `EMPHASIZED_ACCELERATE`; track fills the complementary spans with 4px gaps each side of each line; wavy uses wavelength 20 + full amplitude.
- **Circular indeterminate**: over a 6000ms cycle the sweep grows/shrinks (sweep oscillates ~[20°, 280°] via `STANDARD`-eased grow/shrink phases) while base rotation advances 1080°/cycle plus a 360° advance kick every 1500ms over 300ms. **Implementation note:** this is a faithful interpretation of the Compose constants (R§C) — tuned visually at smoke; record adjustments here.
- Reduced-motion: not a lib-wide facility yet; indeterminate motion is the component's meaning, no fallback variant (matches platform behavior). `[DOC]`

## §7. Wave rendering

Linear: sine `Path2D` sampled ≤ 2px steps across the active span, stroked `BasicStroke(thickness, CAP_ROUND, JOIN_ROUND)`; amplitude = animated fraction × 3px; the flat→wavy edge blends by amplitude, no path swap. Circular: radial sine `r(θ) = R + amp·sin(waveCount·θ + phase)` with `waveCount = round(π·Ømid / 15)` **rounded to an integer so the loop closes seamlessly**; phase spins for travel. Track always flat.

## §8. Accessibility

`AccessibleRole.PROGRESS_BAR`; `AccessibleValue` backed by the model — `getCurrentAccessibleValue()` returns `null` while indeterminate (JProgressBar/Compose semantics R§C/R§D); not focusable; consumers set `getAccessibleContext().setAccessibleName(…)` — Javadoc instructs naming the activity ("Download progress"). Linear honors `ComponentOrientation` (RTL mirrors direction, stop dot at the visual trailing end).

## §9. Showcase pattern

One "Progress" catalog entry → Workbench + Gallery tabs (tabs-epic pattern). Workbench: live indicator pair (linear above circular), controls dogfooding Elwha — `ElwhaSelectField` (variant), `ElwhaSwitch` (indeterminate, wavy), `ElwhaSlider` (value 0–100, thickness 4–8), `ElwhaButton` (simulate-load run). Gallery: the 8-cell matrix (linear/circular × flat/wavy × determinate/indeterminate) + thick row.

## §10. Out of scope (documented, not cut)

| Item | Why | Disposition |
|---|---|---|
| Loading indicator (shape-morph spinner) | separate M3 Expressive component | stub epic **#468** |
| `buffer`, `four-color` | web-legacy, superseded in current M3 (R§D) | documented here; revisit only on consumer demand |
| show/hide animation behaviors + `showDelay`/`minHideDelay` | MDC visibility transitions, separable polish | V2 candidate, file on demand |
| animated determinate value jumps (500ms linear) | separable polish; consumers can drive increments | V2 candidate |
| `contiguous` indeterminate type | needs ≥ 3 indicator colors — legacy multicolor | not planned |

## §11. Phasing → stories (Phase 1 = V1, single phase)

| Story | Scope | §refs |
|---|---|---|
| S1 *(spike)* | package + abstract base + linear determinate flat chrome (track/active/gap/stop, inner-corner-2, thickness, RTL) | §2–§5 |
| S2 | linear indeterminate motion + timer lifecycle | §2, §6 |
| S3 | linear wavy (det + indet, ramp, speed) | §6–§7 |
| S4 | circular determinate + indeterminate (flat) | §5–§6 |
| S5 | circular wavy | §7 |
| S6 | a11y + RTL verification + JProgressBar-parity sweep guard | §8 |
| S7 | Showcase Workbench + Gallery + CHANGELOG (closes #467) | §9 |

Fresh demo per story + headless smoke per story (lib rule).

### S1 spike outcome (2026-06-11)

1. §12.1 → **paint helpers stay variant-local**; the base (`AbstractElwhaProgressIndicator`) hosts model/mode/color/geometry state + the animation clock only. The asymmetric-corner capsule lives in `ElwhaLinearProgressIndicator` (package-visible for the smokes); circular needs arc math instead, no sharing to force.
2. §12.2 → degenerate-end clamp order locked: active capsule width = `clamp(fraction·run, thickness, run)` and is skipped under 0.5px; track = `[head + gap, end]`, skipped under 1px wide; stop dot needs a live track segment *and* `head + gap ≤ stopX`. 0% / 100% verified clean by `ElwhaLinearProgressChromeSmoke`.
3. §12.3 → clock cadence **16ms (~60fps)**, demand-gated; determinate-flat indicators never start it.
4. The `BoundedRangeModel` + capsule-path architecture held with no fights — locked for S2–S7.

## §12. Open for the S1 spike

1. Whether the base hosts shared paint helpers (gap-aware capsule path) or each concrete paints standalone.
2. Exact cap-aware gap math at the degenerate ends (0%, ~100%) — clamp order.
3. Timer cadence (16ms vs 33ms) for acceptable wave shimmer vs CPU.
