# ElwhaLoadingIndicator — M3 Research Capture

> **Status: RAW CAPTURE → SYNTHESIZED.** Source spec for epic [#468](https://github.com/OWS-PFMS/elwha/issues/468)
> (ElwhaLoadingIndicator — M3 Expressive loading indicator, the shape-morph spinner). Companion to the
> ElwhaProgressIndicator epic [#467](https://github.com/OWS-PFMS/elwha/issues/467) — the loading indicator is a
> **separate M3 component**, not a progress-indicator variant. Decisions live in
> `elwha-loading-indicator-design.md`; this file is the captured evidence behind them.

The M3 loading indicator is **Android/Compose-first** — there is **no Material Web implementation** and the
spec page on `m3.material.io` is JS-only (un-fetchable). The authoritative source is therefore the Jetpack
Compose Material3 source + the material-components-android docs. Every numeric constant below is quoted from
those files.

## §TL;DR — synthesis

The Loading indicator is a **shape-morphing spinner**: a filled rounded-polygon that continuously rotates while
morphing through a fixed sequence of shapes from the M3 shape library. It exists in two configurations
(**standard** = bare shape on `primary`; **contained** = the shape in `onPrimaryContainer` on a 48dp
`primaryContainer` circle) and two modes (**indeterminate** = time-driven 7-shape loop; **determinate** =
progress-driven 2-shape Circle→SoftBurst morph). M3 positions it for **short (< ~5s) indeterminate waits** —
pull-to-refresh, inline-button loading — and says it "should replace most uses of the indeterminate circular
progress indicator."

The load-bearing build fact: **Elwha has no rounded-polygon shape engine.** `ShapeMorphPainter` only lerps
**corner radii on a rounded rectangle** (the FAB/button morph) and `MorphAnimator` runs a one-shot 0→1 tween —
neither produces polygon-to-polygon morphing. This component needs **new, self-contained shape + morph
machinery**. That is the architecture question the design doc resolves (and the S1 spike validates).

**Reading order:** §A sources · §B variants/API · §C shape sequence · §D the shapes · §E morph algorithm ·
§F timing/choreography · §G measurements · §H color · §I determinate · §J usage/a11y · §K Elwha token mapping ·
§L terminology lock · §M open questions.

---

## §A. Sources

Compose Material3 (`androidx-main`), raw GitHub:

- `compose/material3/material3/.../LoadingIndicator.kt` — the composables + all timing constants + the
  `LoadingIndicatorDefaults` shape sequences.
- `compose/material3/material3/.../tokens/LoadingIndicatorTokens.kt` — sizes + color tokens.
- `compose/material3/material3/.../MaterialShapes.kt` — the named shape definitions.
- `graphics/graphics-shapes/.../{RoundedPolygon,Shapes,CornerRounding,Morph}.kt` — the polygon + morph engine.
- `material-components-android/docs/components/LoadingIndicator.md` — usage + a11y guidance.

Base raw URL: `https://raw.githubusercontent.com/androidx/androidx/androidx-main/...` and
`https://raw.githubusercontent.com/material-components/material-components-android/master/...`.

---

## §B. Variants & public API (Compose, verbatim signatures)

Four public composables — {standard, contained} × {indeterminate, determinate}:

```kotlin
// Indeterminate, standard (uncontained)
fun LoadingIndicator(modifier, color = indicatorColor,
    polygons = IndeterminateIndicatorPolygons)
// Determinate, standard
fun LoadingIndicator(progress: () -> Float, modifier, color = indicatorColor,
    polygons = DeterminateIndicatorPolygons)
// Indeterminate, contained
fun ContainedLoadingIndicator(modifier, containerColor = containedContainerColor,
    indicatorColor = containedIndicatorColor, containerShape = containerShape,
    polygons = IndeterminateIndicatorPolygons)
// Determinate, contained
fun ContainedLoadingIndicator(progress: () -> Float, modifier, containerColor, indicatorColor,
    containerShape, polygons = DeterminateIndicatorPolygons)
```

- `[CODE]` Two configurations (**standard / contained**) and two modes (**indeterminate / determinate**).
- `[CODE]` `polygons` (the shape sequence) is a parameter — overridable, with sensible defaults.
- `[CODE]` `containerShape` is overridable (default fully-rounded / `CornerFull`).
- `[DOC]` Contained = standard indicator centered on a filled, fully-rounded container circle.

---

## §C. The shape sequence (`LoadingIndicatorDefaults`, verbatim)

**Indeterminate** — `IndeterminateIndicatorPolygons`, 7 shapes cycled in order with a wrap-around link
last→first:

```
SoftBurst → Cookie9Sided → Pentagon → Pill → Sunny → Cookie4Sided → Oval → (loops to SoftBurst)
```

**Determinate** — `DeterminateIndicatorPolygons`, 2 shapes:

```
Circle.transformed(rotateZ(360f / 20))   →   SoftBurst
```

(The circle is pre-rotated 18° so its vertices line up with SoftBurst's features.)

- `[CODE]` The engine builds `Morph(polygons[i].normalized(), polygons[i+1].normalized())` per adjacent pair
  (plus the wrap pair for indeterminate). The two modes use **different** sequences.

---

## §D. The shapes (`MaterialShapes.kt` + graphics-shapes)

**Construction primitives** (verbatim defaults):

```kotlin
RoundedPolygon.circle(numVertices = 8, radius = 1f, centerX = 0f, centerY = 0f)
RoundedPolygon.star(numVerticesPerRadius, radius = 1f, innerRadius = .5f,
    rounding = CornerRounding.Unrounded, innerRounding = null, perVertexRounding = null, ...)
class CornerRounding(val radius: Float = 0f, val smoothing: Float = 0f)   // Unrounded = (0,0)
```

- A shape = a closed ring of vertices; each vertex carries a `CornerRounding(radius, smoothing)`. `radius` is in
  the shape's coordinate space (≈ 0..1 on a unit shape); `smoothing` 0 → circular-arc corner, 1 → squircle.
  Stars alternate an outer-radius and inner-radius vertex. Every shape ends `.normalized()` → fit to the unit
  square (0,0)–(1,1).

**The sequence shapes** (verbatim where captured; `cornerRound15 = CornerRounding(.15f)`,
`cornerRound50 = CornerRounding(.5f)`):

```kotlin
Sunny        = star(numVerticesPerRadius = 8, innerRadius = .8f, rounding = cornerRound15)
Cookie9Sided = star(numVerticesPerRadius = 9, innerRadius = .8f, rounding = cornerRound50).rotate(-90)
Circle       = circle(numVertices = 10)
Oval         = circle().scale(1f, 0.64f).rotate(-45)
SoftBurst    = customPolygon(2 base pts, reps = 10)            // 10-lobed gentle burst
Cookie4Sided = customPolygon(2 base pts, reps = 4)             // 4-lobe scallop
Pentagon     = customPolygon(3 base pts, reps = 1, mirror)     // 5 sides
Pill         = customPolygon(3 base pts, reps = 2, mirror)     // rounded capsule
```

- `[CODE]` Every loading-indicator shape is **star-convex** (a ray from the centroid crosses the boundary once)
  — Sunny/Cookie stars, the lobed bursts, Pentagon, Pill, Oval, Circle all qualify. This is the key
  simplification the Elwha port leans on (design §2): a star-convex shape is fully described by a radius
  profile `r(θ)`, so morphing is a per-angle lerp of two profiles — no feature-matching needed.
- `[DOC]` Exact `customPolygon` vertex float-lists for SoftBurst/Pill/Pentagon/Cookie4Sided were not captured
  byte-for-byte (long literal arrays). The Elwha port reconstructs visually-faithful equivalents as
  rounded polygons and validates by eye at the S1 spike + smoke — the design doc owns the reconstructed
  definitions. ⚠️ This is a documented faithful-interpretation, the progress-epic precedent.

---

## §E. Morph algorithm (`Morph.kt`)

`Morph(start, end)` precomputes a matched cubic list; `asCubics(progress)` lerps it:

1. **Measure** each polygon into cubics with arc-length progress; expose `features` (corners + edges) by
   normalized progress 0..1.
2. **Feature-match** (`featureMapper`) → a `DoubleMapper` correspondence; `cutAndShift` rotates polygon2 so its
   start feature aligns with polygon1's.
3. **Pair cubics**: walk both lists, splitting a cubic where breakpoints don't coincide, so both end with a 1:1
   `List<Pair<Cubic, Cubic>>`.
4. **Interpolate** at `t`: lerp all 8 floats (4 points × x,y) of each cubic pair.

- `[CODE]` For a faithful general morph this feature-matching (`DoubleMapper`) is required. **The Elwha port
  does not reimplement it** — because all loading-indicator shapes are star-convex (§D), it uses the radial
  `r(θ)` lerp instead, which is seamless-by-construction (both profiles are closed periodic functions) and a
  fraction of the code. Fidelity trade documented in design §2.

---

## §F. Timing & choreography (`LoadingIndicator.kt`, verbatim constants)

```kotlin
private const val GlobalRotationDurationMillis = 4666   // continuous full-spin period
private const val MorphIntervalMillis = 650L            // dwell between morph steps
private const val FullRotation = 360f
private const val QuarterRotation = 90f                 // FullRotation / 4
// per-morph progress spring:
spring(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.1f)
```

**Indeterminate stepping loop** (verbatim):

```kotlin
while (true) {
  val deferred = async {
    val r = morphProgress.animateTo(1f, morphAnimationSpec)   // spring 0→1
    if (r.endReason == Finished) {
      currentMorphIndex = (currentMorphIndex + 1) % morphSequence.size
      morphProgress.snapTo(0f)
      morphRotationTargetAngle = (morphRotationTargetAngle + QuarterRotation) % FullRotation
    }
  }
  delay(MorphIntervalMillis)   // 650ms dwell
  deferred.await()
}
```

- Global rotation: `infiniteRepeatable(tween(4666, LinearEasing), Restart)` — constant ≈ **77.1°/s**,
  **LinearEasing** (not an emphasized token).
- Combined rotation: `rotation = morphProgress*90 + morphRotationTargetAngle + globalRotation`
  — a continuous linear spin + a discrete **+90° kick committed per completed morph** + a smooth 0→90°
  contribution from the in-flight morph (clockwise).

- `[CODE]` Choreography per step ≈ 650ms dwell + a spring-driven shape morph + 90° rotational advance, riding on
  a 4.666s linear full-spin.
- `[CODE]` The shape spring **is** the morph easing — no separate emphasized/standard easing token applies to
  the morph. ⚠️ Elwha has no spring-physics animator (`MorphAnimator` is a linear-rate tween, `Easing.spring`
  is a static curve). The port approximates the spring's settle with an eased deterministic tween, computed
  from elapsed time (reproducible for headless smoke — the progress-epic precedent). Design §6 owns the
  interpretation; tune at smoke.

---

## §G. Measurements (`LoadingIndicatorTokens.kt`, verbatim)

```
ActiveSize      = 38.0.dp     // the morphing active indicator
ContainerWidth  = 48.0.dp
ContainerHeight = 48.0.dp
ContainerShape  = CornerFull  // fully rounded
ActiveIndicatorScale = 38 / min(48,48) = 0.7917   // active ≈ 79.2% of container
```

- `[CODE]` Standard indicator: **38×38 dp**. Contained: **48×48 dp** fully-rounded container, **38 dp** active
  shape inside → **5 dp inset** per side ((48−38)/2). 1 dp = 1 px in Elwha.

---

## §H. Color (`LoadingIndicatorTokens.kt`, verbatim)

```
ActiveIndicatorColor    = Primary             // standard active shape
ContainedActiveColor    = OnPrimaryContainer  // active shape inside container
ContainedContainerColor = PrimaryContainer    // the container fill
```

- `[CODE]` Standard active = `primary`. Contained: container = `primaryContainer`, active = `onPrimaryContainer`.
- `[DOC]` No state layers, no other color slots. Contained needs ≥ **3:1** active/container contrast (a11y).
  The view-system uncontained `containerColor` defaults transparent — Elwha standard paints no container.

---

## §I. Determinate loading indicator (`LoadingIndicator.kt`, verbatim fragments)

```kotlin
val activeMorphIndex = (morphSequence.size * progress).toInt().coerceAtMost(morphSequence.size - 1)
val rotation = -progress * 180     // progress-driven, counter-clockwise
```

- `[CODE]` Exists. Uses `DeterminateIndicatorPolygons` (Circle→SoftBurst). Morph is **progress-driven**, not
  time-driven; no continuous global spin — a single progress-mapped **−180°** sweep. Progress's fractional part
  drives the active morph's interpolation.
- `[DOC]` API: a `progress: () -> Float` overload on both `LoadingIndicator` and `ContainedLoadingIndicator`.

---

## §J. Usage & accessibility (material-components-android docs)

- `[DOC]` **When to use:** "designed to show progress that loads in under five seconds. It should replace most
  uses of the indeterminate circular progress indicator." Short indeterminate waits, pull-to-refresh,
  inline-button loading. Use a **progress indicator** ([#467]) instead for longer/quantifiable progress or when
  the process transitions indeterminate→determinate.
- `[DOC]` **Motion:** "captures attention through motion … morphs the shape in a sequence with potential color
  change, if multiple colors are specified."
- `[DOC]` **Accessibility:** set `contentDescription` (animated, non-text element — recommended for screen
  readers). Contained: ensure active/container contrast ≥ 3:1.
- `[DOC]` Reduced-motion: **not** addressed in the M3 source — no explicit reduced-motion branch in
  `LoadingIndicator.kt`. Treat reduced-motion handling as an Elwha design decision (design §6), not an M3
  prescription. Elwha already has `MorphAnimator.isReducedMotion()` (OS-detected) to honor.

---

## §K. Elwha token mapping — **zero new theme tokens** (goal: LOCKED)

| M3 need | M3 value | Elwha mapping | New token? |
|---|---|---|---|
| Standard active color | `Primary` | `ColorRole.PRIMARY` | no |
| Contained container fill | `PrimaryContainer` | `ColorRole.PRIMARY_CONTAINER` | no |
| Contained active color | `OnPrimaryContainer` | `ColorRole.ON_PRIMARY_CONTAINER` | no |
| Active size | 38 dp | `INDICATOR_SIZE_DEFAULT_PX = 38` (component const) | no |
| Container size | 48 dp | `CONTAINER_SIZE_DEFAULT_PX = 48` (component const) | no |
| Container shape | CornerFull | painted circle / `min(w,h)/2` radius | no |
| Global spin | 4666 ms linear | `GLOBAL_ROTATION_MS = 4666` (const) | no |
| Morph dwell | 650 ms | `MORPH_INTERVAL_MS = 650` (const) | no |
| Per-step kick | +90° | `QUARTER_ROTATION_DEG = 90` (const) | no |
| Morph easing (spring 0.6/200) | spring | approximated by `Easing.EMPHASIZED`/tuned tween (const duration) | no |
| Determinate rotation | −progress·180 | const | no |
| Shape library | RoundedPolygon set | package-private radial shape defs (design §2) | no |

All geometry, timing, and shape data are **component constants / package-private data**, exactly like the
progress indicator. Color resolves through existing `ColorRole`s. **No `theme/` token additions.**

---

## §L. Terminology → API lock (M3 nouns mirror)

| M3 noun | Elwha API |
|---|---|
| Loading indicator | `ElwhaLoadingIndicator` |
| Standard / contained | `setContained(boolean)` / `isContained()` (default standard) |
| Indeterminate / determinate | `setIndeterminate(boolean)` (default **true** — the primary mode) |
| Active indicator (the morphing shape) | "indicator" — `setIndicatorColorRole(...)` |
| Container | `setContainerColorRole(...)`, `setContainerSize(...)` |
| Active size | `setIndicatorSize(int)` |
| Shape sequence (polygons) | the built-in M3 sequence; not consumer-overridable in V1 (§M) |
| progress (determinate) | `BoundedRangeModel` (slider/progress precedent) — `setValue` / `getModel` |

---

## §M. Open design questions (resolved in design doc)

1. **Shape-morph engine fidelity** — full `androidx.graphics.shapes` port (RoundedPolygon + feature-matched
   Morph) vs the radial `r(θ)` lerp the star-convex property allows. → design §2 picks **radial**, S1 spike
   validates.
2. **Engine location** — package-private in `loading/` vs shared in `theme/` (the ShapeMorphPainter precedent).
   → design §2: **`loading/` package-private for V1**; extract to `theme/` only when a 2nd consumer appears.
3. **Determinate in V1?** M3 ships it; cheap once the engine exists (2 shapes + progress map). → design §1:
   **in V1, later phase** (no silent cut). Operator may defer at smoke.
4. **Consumer-overridable `polygons`?** Compose exposes it. → design §10: **out of V1** (the M3 sequence is the
   product; custom shape sets are a niche escape hatch) — documented deferral, not a cut.
5. **Spring morph** — Elwha has no spring animator. → design §6: deterministic eased tween approximating the
   settle, tuned at smoke.
6. **Accessibility role** — no Swing "busy spinner" role. → design §8: `PROGRESS_BAR` + `AccessibleValue`
   (null while indeterminate) + `BUSY` state, mirroring `AbstractElwhaProgressIndicator`.
7. **Milestone** — stub filed v0.4.0; active pre-1.0 milestone is **v0.5.0**. → built now → **v0.5.0**.
