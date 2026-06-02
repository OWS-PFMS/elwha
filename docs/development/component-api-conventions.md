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

## 6. Leaf vs container — different API shapes are sanctioned

Components split into two roles, and the role determines the API shape:

| Role | Description | API shape | Examples |
|---|---|---|---|
| **Leaf widget** | IS the content. Small, fixed slot set (a label and at most 2 icons, an icon, etc.). | Single class, typed setters, per-variant static factories. | `ElwhaIconButton`, `ElwhaChip` |
| **Container widget** | HOLDS variable content. M3 sanctions an open-ended composition vocabulary (anatomy + additive patterns). | Chrome-only root primitive + family of companion primitives carrying the slot vocabulary. Consumer composes via `add()`. | `ElwhaCard` (V3 onward) |

**Rationale.** Leaf widgets have a known finite surface; typed setters fit cleanly and stay short. Containers have to express patterns M3 sanctions but doesn't fully enumerate (header trailing slot polymorphism across icon button / chip / overflow; multiple vertical layouts; two orientations; etc.). A typed setter API on the root accumulates bloat as new M3 patterns surface; a chrome + companion split absorbs them as additional companions without API churn on the root.

**Precedent.** Compose Material3 ships both patterns in the same package — `AssistChip(label = {...}, leadingIcon = {...}, trailingIcon = {...})` is typed-slot (leaf); `Card { content }` is chrome-only with composition delegated to siblings like `ListItem` (container). The split is per-component-role, not lib-wide doctrine.

**Apply when:** designing a new component. If the slot vocabulary is small + fixed, follow leaf shape. If the slot vocabulary is open-ended or M3 shows multiple sanctioned layouts for the same content, follow container shape. When in doubt, leaf — promote to container only if real expressivity gaps surface.

**Package layout note.** Leaf vs container does NOT drive package structure. Both shapes live alongside each other in their respective `<componentname>/` packages; container companion primitives live flat in the same package as the chrome root with an `Elwha<Name>*` prefix carrying the family relationship. Match Joy UI / shadcn structure here; reach for sub-packages only when a single component family exceeds ~15 classes.

## 7. Symmetric slot affordances — leading and trailing offer the same interaction vocabulary

When a leaf widget exposes a clickable, two-state affordance on one slot, the opposite slot exposes a symmetric one. `ElwhaChip` pairs `setLeadingAffordance(...)` and `setTrailingAffordance(...)` — identical six-arg signature (`idleIcon`, `activeIcon`, `active`, `hoverRevealIdle`, `tooltip`, `onClick`) and identical idle / active / hover-reveal semantics. A slot's single-state action setters (`setTrailingAction` / `setTrailingIcon`) remain as the lighter-weight option; the two-state affordance and the single-state action share the slot, last-call-wins.

**Rationale.** A consumer who has learned a slot's affordance API should not have to learn a different shape for the opposite slot. M3 conventionally uses the trailing slot for a single-action remove / dismiss, so a single-state setter there is the *common* case — but common is not *only*, and an asymmetric API forces a host container (the way `ElwhaChipList`'s pin / anchor buttons are built on the leading affordance) to special-case which slot it targets. Symmetry keeps the interaction vocabulary one thing to learn. The asymmetry that prompted this rule was caught in [#152](https://github.com/OWS-PFMS/elwha/issues/152).

**Apply when:** a leaf widget gains a two-state affordance on any slot. Add the mirror on the opposite slot in the same change — or, if the slot is single-state by deliberate design, record that decision (and why) here.

**Trailing-slot vocabulary (`ElwhaChip`).** The trailing slot resolves to one of four mutually-exclusive modes — last-call-wins, one setter per mode:

| Mode | Setter | Interactive? |
|---|---|---|
| None | — | — |
| Indicator | `setTrailingIndicator(Icon)` | No — display-only glyph; the chip body owns the click |
| Button | `setTrailingAction(Action)` / `setTrailingIcon(Icon, String, Runnable)` | Yes — single-state, own hover / press states |
| Affordance | `setTrailingAffordance(...)` | Yes — two-state, own hover / press states |

The display-only mode is named `setTrailingIndicator`, **not** `setTrailingIcon` — `setTrailingIcon` already denotes the single-state button, and its three-arg `(Icon, tooltip, onClick)` signature makes that interactivity explicit. The leading slot has no single-state button, so its display-only setter keeps the plain name `setLeadingIcon`. The resulting `setLeadingIcon` vs `setTrailingIndicator` naming asymmetry is deliberate: the trailing slot carries one extra mode and needs three distinct nouns (*indicator* / *button* / *affordance*) where the leading slot needs two. Decision recorded under [#164](https://github.com/OWS-PFMS/elwha/issues/164).

## 8. Shadow reserve — one contract, container/leaf mechanism may differ

Every shadowed (elevated) primitive reserves space around its visible body for the M3 key+ambient shadow halo, so the shadow never clips against the component bounds. The **contract** for that reserve is uniform across all of them; the **mechanism** is allowed to differ by role (§6 leaf vs container).

**One accessor — `getShadowInsets()`.** Every shadowed primitive exposes its halo reserve through a single public getter (`Insets getShadowInsets()`), declared by the `ShadowBearing` interface. Placement helpers (e.g. `ElwhaFabAnchor`) back the halo out of bounds by depending on `ShadowBearing`, never on a concrete primitive type. Do **not** invent a per-component name (`shadowReserve()`, a private accessor, etc.) — that is the drift this rule exists to prevent.

**Where the reserve lives — by role:**

| Role | Reserve home | Body placement |
|---|---|---|
| **Container** (`ElwhaSurface` family, e.g. `ElwhaCard`) | `getInsets()` returns the shadow reserve; preferred size includes it via `super` | layout flows children inside the insets |
| **Leaf** (`JComponent` widgets, e.g. `ElwhaButton`, `ElwhaFab`) | reserve baked into `getPreferredSize()` (the parent reserves room for the halo) | `paintComponent` translates the body by the reserve and centers it manually |

Both roles honor the **same paint convention**: translate the graphics origin by the reserve, then call `ShadowPainter.paint` against the body rect. This divergence is sanctioned for the same reason as §6 — a container that lays out real children gets insets-flow for free; a self-painting leaf does not, and forcing the container's `JPanel`/insets machinery onto a leaf would drag in baggage it doesn't need.

**`getMaximumSize` rule (the hard one).** A leaf that bakes its halo into `getPreferredSize()` must **never** override `getMaximumSize()` to equal `getPreferredSize()`. Doing so disrupts the shadow render — a dark concentration at one corner ([#199](https://github.com/OWS-PFMS/elwha/issues/199); the empirical fix was removing the override, mechanism investigated in [#200](https://github.com/OWS-PFMS/elwha/issues/200)). If a real stretch constraint is ever needed, return `(Integer.MAX_VALUE, Integer.MAX_VALUE)` the way `ElwhaCard` does — never `= preferred`. A leaf with **no** halo (e.g. `ElwhaIconButton`) may carry `max = preferred` safely; the trap is specific to halo-in-preferred primitives.

**Reserve elevation.** Size the reserve for the worst-case elevation the primitive can actually paint, not its resting level: `ElwhaCard` reserves for `MAX_ELEVATION` (transient hover/drag bumps never clip), `ElwhaFab` for its `HOVER_ELEVATION` bump, `ElwhaButton` for its variant's elevation (zero when the variant is flat). Document the chosen worst-case in the reserve accessor's javadoc.

**Apply when:** adding any new elevated/shadowed primitive. Implement `ShadowBearing`, pick the reserve home by role, honor the `ShadowPainter` translate convention, and obey the `getMaximumSize` rule. Contract-alignment of the existing primitives onto `ShadowBearing` is tracked in [#313](https://github.com/OWS-PFMS/elwha/issues/313).

---

## Cross-reference

- `CLAUDE.md` — Conventions section links here for the canonical doctrine.
- `docs/development/code-style.md` — formatting + identifier rules (Spotless + Checkstyle).
- `docs/development/versioning.md` + `versioning-playbook.md` — javadoc tag rules + bump cadence.
