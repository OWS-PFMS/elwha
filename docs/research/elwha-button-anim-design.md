# ElwhaButton Shape-Morph Animation — Design Decisions

**Status:** LOCKED — all 10 §15 decisions resolved 2026-05-24. Ready for the Phase 1 build. Epic
[#176](https://github.com/OWS-PFMS/elwha/issues/176).

**Drafted:** 2026-05-24

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-button-design.md`](elwha-button-design.md) §10 — defers the pressed shape+width morph and
  the animated selection round↔square flip to "a future animation epic"; this is that epic.
- [`elwha-button-group-design.md`](elwha-button-group-design.md) §10 + §12 — defers the standard
  group's "ripples to adjacent buttons' widths" press choreography and the connected variant's
  animated pill-pop on selection.
- [`elwha-icon-button-design.md`](elwha-icon-button-design.md) §10 — also defers the morph for
  symmetry with the button primitive; **not built in this epic** (see §1).
- [M3 Common buttons (Expressive)](https://m3.material.io/components/buttons/overview) — the
  button-press / select morph reference.
- [M3 Button groups (Expressive)](https://m3.material.io/components/button-groups/overview) — the
  group-ripple reference.
- [M3 Motion — easing & duration tokens](https://m3.material.io/styles/motion/easing-and-duration/tokens-specs)
  — the motion vocabulary we map onto morph values.
- [M3 Motion — overview](https://m3.material.io/styles/motion/overview) — Material's motion
  principles (expressive, springy, purposeful).
- `com.owspfm.elwha.theme.RipplePainter` (already in the lib) — the **paint-helper pattern this
  design mirrors**: a stateless static painter; the Timer + animation state lives in the
  consuming component.

**Epic:** [#176](https://github.com/OWS-PFMS/elwha/issues/176) — `epic: button shape-morph
animation (soft spec)`.

**Target milestone:** v0.3.0.

---

## TL;DR

1. **What it is:** the M3 Expressive **shape-morph motion language** wired onto `ElwhaButton` and
   `ElwhaButtonGroup` — three button-local behaviors (pressed shape, pressed width, selected
   round↔square flip) and one group-level behavior (standard-group press width-ripple to
   neighbors). The animated version of the connected-group selected pill pop is a fifth behavior
   that drops cleanly onto the same machinery once the per-button morph lands.
2. **Why now:** the static selection signal (`SQUARE` selected segments / pill pop on connected
   groups) is the v1 affordance and ships in `0.3.0`. The animated transitions are the
   "Expressive" signature — without them the controls read as Material **baseline**, not
   Expressive. This epic closes that gap and resolves the §10 deferrals on both the button and
   the button-group docs.
3. **Scope:** **ElwhaButton + ElwhaButtonGroup only.** IconButton (#132-adjacent), Chip, and any
   future morphing component (FAB, Switch, Slider, Carousel) are explicit non-goals **here**, but
   the shared paint/animation helper this epic introduces (§4) is designed to take them later
   without re-architecture. The pattern mirrors `RipplePainter`, which `ElwhaCard` shipped first
   and `ElwhaButton` / `ElwhaIconButton` / `ElwhaChip` later adopted.
4. **Engine:** a stateless **`ShapeMorphPainter`** in `com.owspfm.elwha.theme` (sibling to
   `RipplePainter` / `SurfacePainter` / `ShadowPainter`), paired with a tiny `MorphAnimator`
   timer helper in the same package — Timer + state lives in the consuming component, exactly as
   today's ripple does. **No new transitive dep** — `javax.swing.Timer` over FlatLaf's
   `com.formdev.flatlaf.util.Animator` (rationale in §4).
5. **Motion values:** locked in §3 as Elwha-pinned, not M3-mandated — M3's motion tokens give us
   curves and duration tiers but don't spec morph durations. We commit to the values and call
   them out as such.
6. **Reduced motion:** detected via `Toolkit.getDefaultToolkit().getDesktopProperty("awt.dynamicLayoutSupported")`-
   adjacent OS hooks per platform (§9); when on, the morph collapses to an instant flip — paint
   identical to the v1 static behavior. Globally togglable via `ElwhaTheme.config(...)` for
   testing and for consumers that want to opt out unconditionally.

---

## Source

Two pre-existing deferral commitments anchor this epic:

- `elwha-button-design.md` §10 — "Shape morphs on press (more square) AND on selected (round ↔
  square inversion) — No shape morph. `setShape` is static for the component's lifetime. Shape
  morph is an animation requiring per-frame radius interpolation and (for selection) a
  state-driven shape swap. Elwha v1 is static-paint pre-v2. **Defer to v2 animation epic.**"
- `elwha-button-group-design.md` §10 — "**Pressed:** a pressed button changes **width and shape**.
  Standard → the change **ripples to neighbors**. Connected → no ripple. The *animated* morph
  transition + the standard width-ripple is a separate polish epic (#176)."

M3 itself does **not** spec morph durations or easing curves outside the static end-state shape
tables. The motion-token page gives us a vocabulary (emphasized vs. standard easing, short / medium
/ long duration tiers, spring-spatial presets) but doesn't say "press morph runs N ms with curve
X." We make those calls in §3.

---

## §1. Scope decisions — Elwha adaptation

| Behavior | In scope? | Notes |
|---|---|---|
| ElwhaButton — pressed shape morph (round → less-round on press, then back) | ✅ | The headline behavior. |
| ElwhaButton — pressed width morph (narrows on press, then back) | ✅ | Springy, paired with shape. |
| ElwhaButton — selected round↔square animated flip (replaces the v1 instant swap) | ✅ | Resolves button §10's selection-side deferral. |
| ElwhaButtonGroup — STANDARD: press width-ripple to neighbors (springy choreography) | ✅ | Resolves group §10. |
| ElwhaButtonGroup — CONNECTED: animated pill-pop on selection (replaces the v1 instant) | ✅ | Drops onto the same per-button machinery — see §7. |
| ElwhaIconButton — pressed shape morph + selected flip | 🚫 | Same motion language; deferred to its own epic so this one doesn't sprawl. The shared `ShapeMorphPainter` is the wire-up path when it lands. |
| ElwhaChip — pressed/selected morph | 🚫 | M3 chips do not morph; static-paint only. |
| Future morphing primitives (FAB / Switch / Slider / Carousel) | 🚫 | Filed as the helpers are extracted; this epic builds the helper, doesn't pre-wire consumers. |

---

## §2. The four behaviors

| # | Name | Trigger | Geometry | Duration tier | Curve |
|---|---|---|---|---|---|
| 1 | **Press shape morph** | mouse-down / VK_SPACE-VK_ENTER hold | corner radius round → "more square" on press, snap back on release | short | emphasized-decel in, emphasized-accel out |
| 2 | **Press width morph** | same as #1 | width narrows ~6 % on press, snaps back on release | short | spring-spatial-default |
| 3 | **Select shape flip** | `setSelected(true)` / `setSelected(false)` | corner radius round ↔ square interpolated | medium | emphasized |
| 4 | **Group width-ripple (STANDARD only)** | any segment press inside the group | adjacent segments shed width to the pressed segment, snap back on release. Decay 1.0 at the pressed segment → ~0.3 at index ±1 → ~0.1 at ±2 → 0 at ±3+. End-of-row neighbors borrow nothing (no out-of-bounds). | short (matched to #2) | spring-spatial-default — borrowing this segment's curve so the press feels coherent |

Behaviors **#1 and #2 always co-occur** (one trigger, two morph axes). Behavior **#3** runs
on the selected segment only. Behavior **#4** runs alongside **#1+#2** when the host is a standard
ElwhaButtonGroup; the *pressed* segment runs its own normal press morph as if it were standalone,
and the group additionally borrows width from its neighbors.

For the **connected** group's animated selected-pill pop (#5 in TL;DR), the *selected* segment's
per-corner `connectedRadii(…, selected=true)` is the END state and `connectedRadii(…,
selected=false)` is the START state of behavior #3, interpolated per-frame. Same machinery,
different start/end values — no new behavior to spec.

---

## §3. Motion tokens — durations + curves

M3 motion vocabulary, with the duration/curve we pin for each behavior:

**Duration tiers** (M3 motion-tokens page):

- `motion.duration.short1` = 50 ms — micro-interactions, presses
- `motion.duration.short2` = 100 ms
- `motion.duration.short3` = 150 ms — **press morph in/out**
- `motion.duration.short4` = 200 ms
- `motion.duration.medium1` = 250 ms
- `motion.duration.medium2` = 300 ms — **select flip**
- `motion.duration.medium3` = 350 ms
- `motion.duration.medium4` = 400 ms

**Easing curves** (cubic-bezier):

- `motion.easing.standard` = (0.2, 0, 0, 1) — most state transitions
- `motion.easing.standard.decelerate` = (0, 0, 0, 1) — incoming
- `motion.easing.standard.accelerate` = (0.3, 0, 1, 1) — outgoing
- `motion.easing.emphasized` = (0.2, 0, 0, 1) (same equation as standard but with a longer
  duration budget — the difference is the **300 ms+** dwell, not the curve shape; M3 lists them
  separately because the duration token differs)
- `motion.easing.emphasized.decelerate` = (0.05, 0.7, 0.1, 1)
- `motion.easing.emphasized.accelerate` = (0.3, 0, 0.8, 0.15)

**Spring presets** (M3 doesn't expose damping ratios as tokens, but the spring vocabulary maps to
critically-damped spatial springs with response times of:

- `spring.spatial.fast` ~ 150 ms response, 0.85 damping
- `spring.spatial.default` ~ 250 ms response, 0.85 damping
- `spring.spatial.slow` ~ 450 ms response, 0.85 damping

(Damping fixed at 0.85 = M3's "no-overshoot" posture for shape morph. Underdamped springs read as
**playful**; the buttons should read as **responsive**, not playful.)

**Pinned values** for the four behaviors:

| Behavior | Duration | Curve | Rationale |
|---|---|---|---|
| Press shape morph (in) | 150 ms (`short3`) | `emphasized.decelerate` | Press needs to read as immediate; deceleration in matches the "snap to pressed state." |
| Press shape morph (out / release) | 150 ms (`short3`) | `emphasized.accelerate` | Releases feel snappier with acceleration out. |
| Press width morph | 150 ms (`short3`) | `spring.spatial.default` (250 ms response capped to the 150 ms window) | Spring gives the springy choreography M3 calls out without overshoot. |
| Select shape flip | 300 ms (`medium2`) | `ease-in-out` (Elwha-pinned, see below) | Slower than press — selection is a **state change**, not an interaction tic. |
| Group width-ripple (STANDARD) | 150 ms (`short3`) | `spring.spatial.default` | Matches the press to read coherently. |

These are the **Elwha-locked values**, not M3-mandated. Comments in `ShapeMorphPainter` cite the M3
token names so the rationale travels with the code.

**Why the select-flip uses a non-M3 curve.** M3's motion tokens are exclusively decelerate or
accelerate variants; the select flip is a *symmetric toggle* — forward (unselected → selected) and
reverse (selected → unselected) must read as visually identical, or the toggle looks buggy.
Asymmetric curves like `emphasized` (which we originally pinned here) trace different visual
paths in each direction: forward shows a long dwell at the start state, while reverse snaps off
the start state and then crawls. We diverge from M3 with `EASE_IN_OUT = cubic-bezier(0.42, 0,
0.58, 1)` (the classic CSS ease-in-out, which satisfies `ease(t) + ease(1-t) = 1`) so a select-on
and a select-off look like mirror images of the same motion. The Phase 1 playground
(`ShapeMorphPlayground`) was the validation surface — see §15.2 for the empirical decision.

---

## §4. The morph helper — paint vs animator split

Mirror the `RipplePainter` pattern verbatim, with one addition. The lib already has:

```
com.owspfm.elwha.theme/
  RipplePainter      — stateless static; paint(g, w, h, origin, progress, radius, tint)
  SurfacePainter     — stateless static; paint(g, w, h, radius, surface, border, ...)
  ShadowPainter      — stateless static + an LRU cache keyed on (arc, elevation)
```

Add two siblings:

```
  ShapeMorphPainter  — stateless static; paint(g, w, h, fromRadii, toRadii, progress, easing, ...)
  MorphAnimator      — tiny Timer wrapper; one-line wire-up for consumers
```

**`ShapeMorphPainter.paint(...)`** takes start and end per-corner `CornerRadii` (from #170), a
`progress` in [0, 1], an easing function, and the SurfacePainter inputs (surface, border,
foreground), and renders the surface body at the interpolated geometry. Stateless. Caller drives
`progress` from its own clock.

**`MorphAnimator`** is the part `RipplePainter` doesn't have a sibling for today — each ripple
consumer holds its own `javax.swing.Timer` and computes its own `rippleProgress`. That's fine for
one animation per component, but a button has *three* concurrent animations possible (press, select,
width) and a group adds a fourth (ripple from a neighbor). Building a tiny Timer wrapper makes the
consumer-side wiring a single line per animation:

```java
private final MorphAnimator pressMorph = new MorphAnimator(this, MorphAnimator.SHORT3);
private final MorphAnimator selectMorph = new MorphAnimator(this, MorphAnimator.MEDIUM2);
private final MorphAnimator widthMorph = new MorphAnimator(this, MorphAnimator.SHORT3);
```

— each one owns its own `javax.swing.Timer`, exposes `progress()` (clamped [0, 1]), `start()`,
`reverse()`, and `stop()`, and calls `host.repaint()` per tick. Cleanup is automatic in
`host.removeNotify()` via a WeakReference / one-line teardown helper.

**Why `javax.swing.Timer`, not FlatLaf's `com.formdev.flatlaf.util.Animator`:**

- **No new transitive surface.** FlatLaf is already a direct dep; using its internal `Animator`
  would couple Elwha to a util whose stability isn't guaranteed across FlatLaf versions
  (`com.formdev.flatlaf.util` is documented as "for FlatLaf internal use").
- **Existing pattern.** Every existing Elwha animation (ripple in card / button / iconbutton /
  chip; hover poll in button; card collapse tween) is `javax.swing.Timer` based. `MorphAnimator`
  keeps the library's animation surface uniform.
- **Easing isn't a value-add.** M3 easing curves are 4 cubic-bezier coefficients each — a
  ~20-line `Easing` helper class beats taking on the FlatLaf coupling. Spring evaluation is
  similarly trivial (critically-damped ODE step, ~30 lines).

A note in §15 marks this **OPEN** for the operator — there's a real argument the other way
(FlatLaf's Animator does the easing-table arithmetic for free).

---

## §5. ElwhaButton wiring

`ElwhaButton` gains four pieces of state:

```java
private final MorphAnimator pressMorph;   // 0 = rest, 1 = pressed
private final MorphAnimator selectMorph;  // 0 = unselected shape, 1 = selected shape
private final MorphAnimator widthMorph;   // 0 = natural width, 1 = group-ripple borrowed
private float widthBorrowFactor;          // -1..1, set by the group during a neighbor press
```

`paintComponent` reads the three progresses and asks `ShapeMorphPainter` to render the surface at:

- **radii** = interpolate(restRadii, pressedRadii, ease(pressMorph.progress()))
              ⨯ interpolate(unselectedRadii, selectedRadii, ease(selectMorph.progress()))
              — composed in the order **select first, then press** (press modifies the
              currently-selected geometry).
- **width offset** = interpolate(0, -PRESSED_WIDTH_DELTA_PX, widthMorph.progress())
                   + interpolate(0, BORROWED_WIDTH_PX × widthBorrowFactor, widthMorph.progress())

`mousePressed` / `mouseReleased` (and the keyboard activation paths in §8 of the button doc)
trigger `pressMorph.start()` / `pressMorph.reverse()`. `setSelected(...)` triggers
`selectMorph.start()` / `reverse()`. The width Timer is driven by the group (see §6); standalone
buttons keep it at 0.

The existing ripple Timer is **untouched**. Press fires both the ripple and the morph; they layer
in the paint stack (surface ← morph-shape ← ripple ← icon/label).

---

## §6. ElwhaButtonGroup width-ripple — the standard-group neighbor choreography

When a segment of a STANDARD ElwhaButtonGroup is pressed, the group:

1. Reads the segment's natural press-width delta (~6 % of its width).
2. Computes a **borrow vector** indexed by segment: `borrow[pressedIdx] = +6 %`,
   `borrow[pressedIdx ± 1] = -1.8 %` (= 30 % of the press delta), `borrow[pressedIdx ± 2] =
   -0.6 %` (10 %), `borrow[pressedIdx ± 3+] = 0`. Out-of-bounds neighbors contribute nothing — the
   pressed segment at the row's edge borrows less, which is the M3 reference behavior.
3. Calls `segment.startWidthBorrow(borrowFactor)` on each affected segment, which kicks each
   segment's `widthMorph` via the existing `MorphAnimator.start()` path.
4. On release, calls `segment.releaseWidthBorrow()` on each — the morph reverses back to 0.

Sum-of-borrow is **not** required to be zero. The row's total natural width is fixed by layout
(see §12 of the group doc — STANDARD lays out hugged segments with a fixed gap), and the morph
runs **within** each segment's bounds rather than re-laying-out the row. Visual width is purely a
**paint-layer** change driven by `ShapeMorphPainter`'s width offset — `doLayout()` is **not**
re-run during the morph (this is the critical performance call; see §11).

CONNECTED groups have no width-ripple — M3 explicitly excludes connected from this choreography
(group doc §10). The segments are visually butted; ripple-width math would break the connection.

---

## §7. Connected variant — animated pill-pop on selection

When a CONNECTED ElwhaButtonGroup's selection changes from index `i` → index `j`:

- Segment `i` gets `selectMorph.reverse()` — its radii animate from `connectedRadii(i, …,
  selected=true)` (uniform pill) back to `connectedRadii(i, …, selected=false)` (segmented-bar
  treatment).
- Segment `j` gets `selectMorph.start()` — radii animate the other way.

The two animations run **concurrently**, both on `medium2` (300 ms) `emphasized` curve. The visual
effect: the pill "slides" from one segment to the next via a brief moment where both segments
hold intermediate radii. This is the M3 Expressive signature on segmented controls.

No new behavior — purely a different start/end value plugged into `selectMorph`. The static v1
behavior falls out of this naturally when `MorphAnimator.IMMEDIATE_FINISH` is set (reduced motion,
or the global config toggle in §10).

---

## §8. Interaction with existing systems

| System | Interaction |
|---|---|
| `RipplePainter` Timer | Untouched. Press fires both the ripple Timer and the press-morph Timer; both paint in `paintComponent`, layered (morph-shape under ripple). |
| `setShape(ButtonShape)` | The static `shape` field becomes the **target** of `selectMorph` rather than the painted value. Reading `getShape()` returns the target (the post-morph value), not the in-flight interpolation. |
| `setCornerRadii(CornerRadii)` (#171) | Same — becomes the morph target. Connected segments compute their target via the group's `connectedRadii(…)`. |
| `setSelected(boolean)` | Triggers `selectMorph.start()` / `.reverse()`. `isSelected()` returns the target, not the in-flight value (matches `setShape`). |
| Focus / `AccessibleState.SELECTED` | Unchanged. Accessibility surface always reports the target state — assistive tech doesn't see the in-flight morph. |
| `ElwhaButtonGroup.refreshSegments()` (#170) | Calls into each segment's `applyShape(…)` / `applyCornerRadii(…)` — these now feed the morph target rather than instant-painting. Idempotent calls (which `refreshSegments` does many of) are no-ops when the target hasn't actually changed. |
| `ElwhaCard.paintRipple` / shadow cache | Unrelated. Card animation is its own concern; the morph helper is button-scoped. |

---

## §9. Reduced motion

Per JDK 21 and the platforms Elwha targets:

- **macOS:** `Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.reduceMotion")` returns a
  `Boolean` reflecting `System Preferences → Accessibility → Display → Reduce motion`.
- **Windows:** `Toolkit.getDefaultToolkit().getDesktopProperty("win.text.animationsEnabled")` — the
  closest available signal; flips when "Show animations in Windows" is off in Ease of Access.
- **Linux (X11 / GNOME):** no standard Java path. GNOME exposes the preference via the
  `gtk-enable-animations` GTK setting; reachable from Java only via JNI or shell-out to `gsettings
  get org.gnome.desktop.interface enable-animations`. **OPEN** in §15 — we can shell-out, or just
  default to "animations on" on Linux and ship a config toggle.

When reduced motion is detected (or the consumer sets `ElwhaTheme.config(...).reducedMotion(true)`):

- All `MorphAnimator` instances run with `IMMEDIATE_FINISH` — `progress` snaps to the end value on
  `start()`, the Timer never schedules ticks.
- Visual outcome: identical to v1 static behavior. The button still paints the selected-square /
  unselected-round shapes, just without the transition.

The detection is read **once at first morph trigger** and cached; if the user flips the system
setting mid-session, the lib does not pick that up live (matches FlatLaf's posture for other
desktop-property reads).

---

## §10. API surface

**Per-instance:** none. The morph is internal — consumers don't opt individual buttons in or out.

**Global:** one config toggle on `ElwhaTheme.config(...)`:

```java
ElwhaTheme.config()
    .reducedMotion(true)   // forces IMMEDIATE_FINISH on every MorphAnimator
    .apply();
```

Default: `false` (animations on), overridden by the OS reduced-motion signal.

**Why not per-instance:** the morph is the M3 Expressive signature; turning it off per-button is
the v1 (static) behavior, which is what the OS signal and the global toggle already provide. A
per-instance opt-out would create a third axis of "is this button animated?" that consumers don't
need.

**Why a config toggle at all:** test infra and snapshot tooling want determinism. A global "no
animations" flip is the standard accommodation.

---

## §11. Performance budget

- **Frame target:** 60 fps (16.67 ms / tick). `MorphAnimator` uses a single shared `Timer` per
  consuming component (one Timer per animation slot, so an `ElwhaButton` peaks at three Timers
  during a fast press-on-selected — within budget).
- **No layout invalidation during morph.** §6 spells this out for the group ripple: the visual
  width change is paint-layer only, `doLayout()` is not re-run. Same for the per-button press
  width — `getPreferredSize()` continues to return the resting width.
- **Idempotent calls are free.** `MorphAnimator.start()` on an already-running animation toward
  the same target is a no-op. `refreshSegments(...)` cycles that don't change targets are free.
- **Cleanup.** Each consuming component calls `morph.stop()` in `removeNotify()`. The animator
  holds a WeakReference to its host to defend against leaks if a consumer forgets — but the doc
  spells out the explicit teardown as the canonical pattern.

---

## §12. Existing-code reuse audit

| Existing | Reuse path |
|---|---|
| `com.owspfm.elwha.theme.RipplePainter` | Pattern model for `ShapeMorphPainter` — stateless paint, Timer + state in consumer. No code reuse; structural sibling. |
| `com.owspfm.elwha.theme.SurfacePainter` (per-corner overload from #171) | **Direct dependency.** `ShapeMorphPainter` calls `SurfacePainter.paint(...)` with the interpolated `CornerRadii`. The morph painter is a thin wrapper that picks the per-frame radii; rendering is delegated. |
| `com.owspfm.elwha.theme.CornerRadii` (from #171) | The interpolation target type. Add `CornerRadii.interpolate(CornerRadii a, CornerRadii b, float t)`. |
| `javax.swing.Timer` | `MorphAnimator`'s engine. |
| `ElwhaButton`'s ripple Timer wiring (lines ~1042-1080) | Pattern model for the morph Timer wiring. Mostly mechanical copy with new field names. |
| `ElwhaCard`'s collapse-tween Timer | Confirms the `javax.swing.Timer` posture across the lib's existing animations. |
| `ElwhaButtonGroup.refreshSegments(…)` (#170) | Hook for the connected pill-pop. The existing call site already pushes per-segment radii on selection changes; we feed the **target** rather than painting instantly. |

Nothing to extract from existing code into a new helper for this epic — the helpers (`ShapeMorphPainter`,
`MorphAnimator`, `Easing`) are net-new files.

---

## §13. Showcase wiring

Two surfaces:

- **Button Workbench** — a new control group **Animation**, with:
  - `reducedMotion` toggle (drives `ElwhaTheme.config(...).reducedMotion(...)` for the duration of
    the workbench session)
  - press / select buttons to trigger the morph deliberately (so the operator can watch a single
    morph cycle without having to chase a fast click)
  - duration multiplier slider (1× / 2× / 5× — purely for visualization; multiplies all the §3
    pinned durations during the workbench session)
- **Button Group Workbench** — the duration multiplier carries through; additionally exposes the
  STANDARD width-ripple borrow factors as readouts (per-segment decimal next to each segment).

Gallery panels are untouched — the static M3 reference matrix already shows the end states.

---

## §14. Phase roadmap

Proposed story breakdown, mirroring the #170 phase structure:

- **Phase 1 — animation helpers.** `ShapeMorphPainter` (stateless), `MorphAnimator` (Timer wrapper),
  `Easing` (cubic-bezier + critically-damped spring evaluator), `CornerRadii.interpolate(…)`. No
  consumer wiring. Validates the helpers compile and the Easing math is correct.
- **Phase 2 — ElwhaButton wiring.** Press shape + press width morph on standalone buttons. Select
  flip on SELECTABLE buttons. Reduced-motion detection (macOS first; Linux/Windows in Phase 5).
- **Phase 3 — ElwhaButtonGroup STANDARD width-ripple.** Wire the borrow-vector from §6; the
  pressed segment's own press morph already works from Phase 2.
- **Phase 4 — ElwhaButtonGroup CONNECTED animated pill-pop.** §7 — different start/end values,
  same machinery; should be a small story.
- **Phase 5 — reduced-motion + Showcase wiring + cross-platform polish.** Per-platform reduced-motion
  detection (§9), the `ElwhaTheme.config(...).reducedMotion(...)` toggle, the Button Workbench's
  Animation control group, Button Group Workbench's borrow-factor readouts.

Five stories, all on `feat/176-button-shape-morph`. CHANGELOG entry lands in Phase 5.

---

## §15. Decisions — all resolved

_All 10 resolved 2026-05-24. Numbering is stable so existing references (§15.1, §15.2, …) hold._

1. **`javax.swing.Timer` vs `com.formdev.flatlaf.util.Animator` for `MorphAnimator`'s engine** →
   **`javax.swing.Timer`.** Keeps Elwha's animation surface uniform with the existing ripple /
   hover-poll / card-collapse Timers; avoids coupling to `com.formdev.flatlaf.util` (FlatLaf
   marks it "for internal use"). We write the easing table ourselves (§4 rationale).

2. **The four pinned motion values in §3 (150 ms press, 300 ms select, decay vector
   `[1.0, 0.3, 0.1, 0]` for the group ripple)** → **ship with §3's values; tune live via the
   Workbench duration-multiplier during smoke-testing.** The §3 numbers are derived from M3's
   motion-token tiers and are the most defensible starting point; the Workbench is the iteration
   surface, not the spec.

   **Phase 1 playground update (2026-05-24):** the select-flip curve was originally pinned to
   `EMPHASIZED`. Smoke-testing in `ShapeMorphPlayground` showed `EMPHASIZED` reads as visibly
   asymmetric on a symmetric toggle (forward and reverse trace different visual paths because the
   curve is heavily decelerating). **Curve re-pinned to `EASE_IN_OUT`** — the classic
   symmetric-across-midpoint cubic-bezier (0.42, 0, 0.58, 1) — added to {@code Easing} as an
   explicit Elwha divergence from M3 tokens (none of M3's variants are symmetric). The duration
   stays at 300 ms (`medium2`); the operator's playground read suggested 150 ms felt better in
   isolation, but a bare rectangle has no semantic weight, and a real button needs the longer
   dwell to register as a deliberate state change. **Re-validate the 300 ms duration in Phase 2
   on a real button.**

3. **Spring overshoot / damping ratio** → **0.85 ("responsive, not playful") for v1; revisit
   after smoke-testing.** Underdamping (0.7-0.75) is an Expressive-ier option; we adopt it later
   if the no-overshoot read feels flat. Single damping value across all spring uses to keep the
   helper simple.

4. **Press width delta floor** → **6 % of resting width with a 1 px paint-layer floor.** The
   floor protects the smallest XS groups from sub-pixel borrow widths; paint-layer only
   (`getPreferredSize()` reports the resting width, layout doesn't re-run).

5. **Linux reduced-motion detection** → **default-on; global `ElwhaTheme.config(...)
   .reducedMotion(...)` toggle is the user-facing escape hatch.** No `gsettings` shell-out,
   no platform-sniffing — the global toggle covers the use case at zero cost.

6. **Reduced-motion re-read cadence** → **read-once at first morph trigger, cached for the
   session.** Matches FlatLaf's posture for similar desktop-property reads. Consumers needing
   live tracking wire their own listener and flip the global toggle.

7. **Animation when `setEnabled(false)`** → **no morph plays on a disabled button.** A disabled
   button is visually frozen; programmatic `setSelected(…)` snaps to the new state without
   transition. Re-enabling does not retroactively animate; the new state is already painted.

8. **Showcase Workbench duration multiplier** → **discrete 1× / 2× / 5× / 10×.** Snapping
   easier than a continuous slider for side-by-side comparisons.

9. **Naming of the paint helper** → **`ShapeMorphPainter`.** "Morph" alone is ambiguous (M3
   uses the word for cross-fades too); the painter is shape-specific and the name carries that.

10. **Phase 1 deliverable scope** → **helpers + a `ShapeMorphPlayground` `main(...)` smoketest.**
    The helpers are hard to validate in isolation without a harness; the playground is a single
    `main(...)` class that animates a rectangle so Phase 1 is independently testable. NOT wired
    into the Showcase — per [[fresh-demo-per-story]], each phase gets its own artifact, and the
    Showcase wiring is Phase 5.

---

## §16. References

- M3 — Common buttons: https://m3.material.io/components/buttons/overview
- M3 — Button groups: https://m3.material.io/components/button-groups/overview
- M3 — Motion overview: https://m3.material.io/styles/motion/overview
- M3 — Easing & duration tokens: https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
- M3 — Transitions: https://m3.material.io/styles/motion/transitions/transition-patterns
- FlatLaf — `com.formdev.flatlaf.util.Animator` source:
  https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/util/Animator.java
- `elwha-button-design.md` §10 (deferred behaviors)
- `elwha-button-group-design.md` §10 + §12 (deferred behaviors)
- `RipplePainter.java` (the paint-helper pattern this design mirrors)
