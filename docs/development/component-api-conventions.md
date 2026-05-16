# Component API conventions

Cross-component API doctrine for Elwha primitives (`ElwhaSurface`, `ElwhaIconButton`, `ElwhaChip`, and downstream components). These rules are the authoritative source for naming and shape decisions every new component is expected to match.

Locked in **#62 — cross-component API consistency sweep**. Future drift should be resolved by updating this doc and aligning components to it, not by letting the drift compound.

## 1. Getter naming: `getX()` only

Bare `getX()` always returns the **effective (resolved) value** — the per-instance override if set, otherwise the variant's default. There is no parallel `getEffectiveX()` getter.

**Rationale.** No current consumer asks for "what did the caller literally pass, ignoring variant defaults." Surfacing the resolved value through a single getter keeps the API narrow and matches IconButton's pre-existing convention.

**Apply when:** designing any token-bound getter (`getSurfaceRole`, `getShape`, `getBorderRole`, etc.).

## 2. Static factories — one per variant

Every component with a `Variant` enum exposes one `static` factory per variant, taking the convenience-constructor argument (text / icon / headline) of that component. Factories whose variant carries mandatory behavioral parameters take those as additional args (e.g., `inputChip(String, Runnable onRemove)`). Components without variants (`ElwhaSurface`) have no factories.

**Rationale.** M3 docs name components by variant ("an assist chip", "a filled icon button", "an elevated card"). Factories match spec vocabulary at the call site and reduce constructor+setter chaining for the common case.

**Apply when:** introducing or extending a `Variant` enum. Add one factory per variant in the same change.

**Examples.**
- `ElwhaChip.assistChip(String)`, `filterChip(String)`, `inputChip(String, Runnable)`, `suggestionChip(String)`
- `ElwhaIconButton.filledIconButton(Icon)`, `filledTonalIconButton(Icon)`, `outlinedIconButton(Icon)`, `standardIconButton(Icon)`

This rule **subsumes** the earlier "variant-as-behavior vs variant-as-treatment" framing, which leaked: 3 of Chip's 4 factories are pure discoverability shorthand, not behavior-forcing. The doctrine is now uniform — every variant gets a factory, full stop.

## 3. Convenience constructors — single primary-content arg

Components with a single "primary content" concept get one convenience constructor taking that content. Components without one stay no-arg only.

- `ElwhaChip(String text)`
- `ElwhaIconButton(Icon icon)`
- `ElwhaSurface()` — no convenience ctor (no primary content)

**Apply when:** designing a new component's constructor surface. Pick the single most-common content arg; do not multiply convenience constructors.

## 4. Border-role exposure rule — variant-bearing components do not expose `setBorderRole`

`ElwhaSurface` (no variants) exposes `setBorderRole(ColorRole)` because the border is the only border-color signal. `ElwhaIconButton` and `ElwhaChip` (variant-bearing) do **not** expose a border-role override — the border role is variant-derived. To "opt out" of a border, set the variant to one that has no border.

**Rationale.** A per-instance border-role override on top of a variant-derived border-role decision creates two sources of truth for the same visual surface. The variant should win for variant-bearing components; the override is meaningful only when there is no variant doctrine to defer to.

**Apply when:** adding a new variant-bearing component. Do not expose `setBorderRole` / `getBorderRole`. If the consumer needs a border with no surface fill, the variant taxonomy is where that gets encoded.

## 5. Border-width is symmetric — `setBorderWidth(int)` + `getBorderWidth()`

Both the setter and the getter are exposed on every component that has a paintable border (Surface, IconButton, Chip, future variant-bearing primitives). Asymmetric setter-without-getter is drift; fix it in the next pass.

---

## Cross-reference

- `CLAUDE.md` — Conventions section links here for the canonical doctrine.
- `docs/development/code-style.md` — formatting + identifier rules (Spotless + Checkstyle).
- `docs/development/versioning.md` + `versioning-playbook.md` — javadoc tag rules + bump cadence.
