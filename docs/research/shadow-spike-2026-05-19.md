# Shadow Spike — `FlatDropShadowBorder` vs Elwha custom shadow

**Date:** 2026-05-19
**Spike harness:** `target/spike-src/ShadowSpike.java` (out-of-tree; not under `src/`)
**Screenshots:** `docs/research/shadow-spike-images/level-{1..5}-{light,dark}.png`

## TL;DR

**FDSB is inadequate as a drop-in replacement.** Its shadow halo is rectangular and ignores the card's rounded corners, producing visible square-corner stubs around every elevated card at every level in both light and dark modes. Phase A gate: **FAIL.** Recommendation: extract a shared `ShadowPainter` using the current custom logic (round-rect silhouette + convolve-blur) and address the perf problem at the painter level via image-pool reuse and tween-aware caching — not by adopting FDSB.

The perf delta is large (FDSB ~30× faster at cold paint, ~200× faster at the resize-loop workload, ~10× smaller heap), so once the painter is shared, **layering an FDSB-style "scale a single cached image" fast path on top of round-rect renders** is the obvious follow-up — but the immediate win is correctness, not speed.

## Phase A — Visual gate: **FAIL**

Side-by-side panels: (a) current `ElwhaCard` with `setElevation(N)`; (b) `ElwhaCard` with elevation 0 wrapped in a `JPanel` carrying a single `FlatDropShadowBorder`; (c) elevation 0 wrapped in two stacked `FlatDropShadowBorder`s (key + ambient, M3 dual-shadow model).

| Level | Elwha (current) | FDSB single | FDSB key+ambient |
|---|---|---|---|
| 1 | rounded halo follows card corners | square-cornered halo, visible at corner stubs | square-cornered halo, visible |
| 3 | rounded halo, M3-aligned drop | rectangular shadow extending past rounded corners | rectangular shadow, two layers |
| 5 | rounded halo, deep but soft | obvious 90°-corner shadow stubs around card | obvious 90°-corner shadow stubs, doubled |

**Root cause.** `FlatDropShadowBorder` paints a shadow image sized to the host component's *rectangular* border bounds. There is no constructor argument or property that lets it follow a rounded outline. The card's body is a round-rect; its FDSB-rendered shadow halo is a soft-edged rectangle. The mismatch is most visible:

- at the four corners, where the shadow extends out past the rounded card edge in 90° wedges,
- on dark mode, where the high-contrast shadow makes the rectangular silhouette unmistakable,
- at levels 4–5, where larger blur radius amplifies the mismatch.

Stacking two FDSBs (key + ambient) does nothing for the corner-geometry problem — it just doubles the rectangular shadow. M3's two-shadow visual model is a separable concern; without round-rect awareness it doesn't help.

**Gate call: FAIL.** No FDSB configuration produces an acceptable match to either M3 reference or the current Elwha look. See `docs/research/shadow-spike-images/level-{3,5}-{light,dark}.png` for the most diagnostic cases.

## Phase B — Perf numbers (kept for record; gate already failed)

Numbers from `ShadowSpike` on the spike machine, JDK 21, `-Xmx512m`. Per-card geometry 280×160, arc 12, elevation 3 unless noted. **These are off-screen `Component.paint(Graphics2D)` calls into a `BufferedImage`** — not real EDT frame measurements, but the relative cost ordering is informative.

| Workload | Elwha | FDSB single | FDSB key+ambient |
|---|---|---|---|
| Cold paint, 1 card (mean / p95) | **13.00 / 14.05 ms** | 0.52 / 0.76 ms | 0.40 / 0.56 ms |
| Steady paint, 50 cards/frame (mean / p95) | 6.35 / 6.90 ms | 6.79 / 7.04 ms | 7.15 / 8.05 ms |
| Resize-loop, 60 frames (mean / p95) | **18.02 / 21.71 ms** | 0.09 / 0.12 ms | 0.11 / 0.15 ms |
| Heap delta, 50 cards | 8.72 MB (170 KB/card) | 0.82 MB (16 KB/card) | 0.90 MB (18 KB/card) |

Takeaways:

- **Cold paint** is where Elwha pays its `ConvolveOp` two-pass blur — ~13 ms per card. FDSB amortizes its single image once per (color, size, opacity) tuple across all instances, so the per-card cost is essentially `drawImage`.
- **Steady paint** is roughly equal once Elwha's per-instance cache is hot — FDSB has no edge for a hot list of cards that aren't resizing.
- **Resize-loop** is the PR #110 pain point. Elwha invalidates its cache on every height change and pays the full ConvolveOp again; FDSB ignores body size for its shadow image and just re-positions a cached one. This is the ~200× delta. PR #110's workaround (freeze the cache during the tween) gets Elwha to roughly FDSB-class behavior at the cost of a slightly stretched shadow during the 250 ms animation.
- **Heap** — Elwha's `BufferedImage` cache is per-instance; FDSB caches in the border instance itself, but identical-style FDSB borders share style and can reuse images at the impl level. 10× smaller per-card heap matters at list scale (50 cards × 170 KB ≈ 8 MB; with cards inside scroll lists numbering in the hundreds, this can dominate).

## Architectural recommendation

**Extract a shared `ShadowPainter` using the current custom logic (round-rect-aware), not FDSB.** Three reasons:

1. **Visual correctness is non-negotiable.** FDSB doesn't and can't follow a round-rect outline. The library's whole point is matching M3 — square shadow corners around round-rect cards is a recognizable wrongness.
2. **Perf gap is closeable inside the painter.** FDSB's speed comes from caching a single shadow image keyed on `(size, color, opacity)` rather than `(bodyW, bodyH, arc, elevation)`. Elwha can adopt the same trick: render the shadow image once per `(arc, elevation)` tuple at a max size, then `drawImage` it scaled / 9-sliced for each instance. That gets the resize-loop case to FDSB-class performance *without* the corner geometry compromise. The current per-instance cache is the conservative implementation; a process-wide cache keyed on `(arc, elevation)` only is the V2.
3. **Code-simplification motive (the third path in the brief) doesn't apply** because FDSB doesn't simplify the consumer side — wrapping every elevated component in a `JPanel`-with-border has its own layout costs (extra `paintBorder` wiring, extra container in the tree, the `getInsets` reserve has to be re-modeled by the wrapper) and the round-rect mismatch still has to be solved before any of that buys anything.

The painter should expose:
- `paintShadow(Graphics2D, Shape outline, int elevation)` — paints a round-rect-silhouette shadow under any outline, not just `ElwhaSurface`'s round-rect.
- An internal cache keyed on `(arc, elevation)` *only*, not on body size. Body size is handled via image stretching or 9-slice. This is the perf change.
- An optional M3 dual-pass mode (key + ambient) for the V2 if visual review wants closer parity with M3 reference. The current single-pass blur is close enough, but the two-pass option is cheap once the painter is in one place.

## Honest unknowns

- **Off-screen vs on-screen paint cost.** All Phase B numbers are `BufferedImage`-targeted paints, not EDT frame timing through Swing's repaint manager. The relative ordering should hold but absolute ms will be different on-screen (Active rendering / DirtyRegion compositing changes the picture). Real frame-time numbers would need a `glassPane`-based instrumented `JFrame` and a `Toolkit.sync` after each paint — out of scope for a spike.
- **Hi-DPI behavior.** FDSB scales its shadow image via `Image` resize; the Elwha custom path renders at logical px. Neither was tested on a 2x scale factor. The square-corner problem will persist regardless of DPI.
- **M3 reference fidelity.** The harness uses my best-known M3 elevation parameters (`offsetY`, `blur`, `opacity` for key and ambient). Pixel-exact comparison to a Compose-rendered reference would require a Compose-on-JVM render of the same geometry; the spike used a side-by-side visual gut-check instead. The gate call doesn't depend on M3 parity — it depends on FDSB's geometric inadequacy, which is independent of token values.
- **`SurfacePainter`'s blur tuning** (alpha = `min(180, 60 + 12·elev)`, blur radius = `min(12, 2 + 2·elev)`, key offsetY = `min(6, elev + 1)`) was chosen by eye in PR #43; I did not re-evaluate it against M3 reference here. The painter extraction should revisit those constants against a Compose render at the same time.
