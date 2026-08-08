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

## 5a. Setter return type — fluent `this`, and a subclass re-types every setter it advertises

A component mutator returns `this`, typed as the **concrete component class**, so a construction chain reads as one expression. `void` is acceptable only where the component has no fluent chain to join (the progress indicators, `ElwhaLoadingIndicator`); a `withX()` prefix is not — the name is `setX` regardless of what it returns ([#636](https://github.com/OWS-PFMS/elwha/issues/636), which converted `ElwhaBadge`'s three `withX` mutators).

**The subclass rule.** When a component extends another component, **every inherited setter it advertises as its own API gets a covariant override narrowing the return type to the subclass.** Java resolves the return type statically, so one un-narrowed setter mid-chain silently ends the chain at the *parent* type and the next call fails to compile — an error the consumer reads as "that setter doesn't exist" rather than "you passed through a base-class setter."

**Apply when:** adding a component that extends `ElwhaSurface` (or any other component). Go through the parent's fluent setters, decide which ones your component advertises, and re-type all of them in the same change. The override body is `super.setX(...); return this;` — behavior stays on the parent.

**Deliberate exception — the un-advertised setter.** A setter the subclass does *not* advertise is left un-narrowed on purpose. Java cannot hide an inherited public method, so it stays callable; leaving the chain broken there is the closest the language gets to a "not part of this component's API" marker. `ElwhaCard` does exactly this with `setBorderRole` — §4 says a variant-bearing component does not expose a border-role override, and the V3 spec §3.2 records it as inherited-but-not-advertised.

**Precedent.** `ElwhaCard` shipped with only `setElevation` narrowed, so `ElwhaCard.filledCard().setShape(XL).setVariant(...)` did not compile even though the V3 spec advertised `setShape` as per-instance card API ([#570](https://github.com/OWS-PFMS/elwha/issues/570)). It now narrows `setSurfaceRole`, `setShape`, `setBorderWidth`, and `setClipChildrenToCorners` as well.

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

**The rule binds the primitive that *paints* the shadow, not every component that carries halo in its measurement.** [#660](https://github.com/OWS-PFMS/elwha/issues/660) asked whether a container inherits the trap through its children: `ElwhaButtonGroup` derives its preferred height from its segments' preferred heights, so an elevated segment puts halo into the group's own measurement, and the group clamps `max = preferred`. It does **not** reproduce — measured, not reasoned: a `STANDARD` group of elevated buttons in a `BoxLayout` renders each segment *pixel-identical* to the same button rendered standalone, halo included, and the halo stays left-right symmetric (pinned by `ElwhaButtonGroupShadowTest`). The group paints no shadow; each segment paints its own inside its own bounds, and no segment clamps its own maximum. A container's clamp governs how far *its* parent may stretch *it*, and never reaches a child's paint path. So the question to ask of a `max = preferred` override is "does this component paint a shadow it has reserved room for inside its own bounds?" — not "is there halo anywhere in the subtree?"

**Reserve elevation.** Size the reserve for the worst-case elevation the primitive can actually paint, not its resting level: `ElwhaCard` reserves for `MAX_ELEVATION` (transient hover/drag bumps never clip), `ElwhaFab` for its `HOVER_ELEVATION` bump, `ElwhaButton` for its variant's elevation (zero when the variant is flat). Document the chosen worst-case in the reserve accessor's javadoc.

**Apply when:** adding any new elevated/shadowed primitive. Implement `ShadowBearing`, pick the reserve home by role, honor the `ShadowPainter` translate convention, and obey the `getMaximumSize` rule. Contract-alignment of the existing primitives onto `ShadowBearing` is tracked in [#313](https://github.com/OWS-PFMS/elwha/issues/313).

## 8b. The visible body is a contract, not a calculation — `BodyBearing`

A primitive routinely paints smaller than the bounds it was granted: a fill layout hands it surplus space and the pill or track floats centered in it, a `ShadowBearing` halo lives inside the bounds, and an XS / S button inflates its height to the WCAG 48 dp target even at preferred size. Anything positioning against `getBounds()` therefore addresses an edge nobody can see, which is [#493](https://github.com/OWS-PFMS/elwha/issues/493) (anchoring) and [#505](https://github.com/OWS-PFMS/elwha/issues/505) (hit testing) — the same defect from two sides.

**One accessor — `getBodyBounds()`,** declared by `BodyBearing` in `theme/`. It returns the painted body in component coordinates, excluding halo, centering slack, and target-inflation padding. A sibling of `ShadowBearing` rather than an extension of it: most body-centering primitives carry no halo at all, and making them answer `getShadowInsets()` with zeros to get a body rect would be backwards.

**Consumers call the static resolver, never the method.** `BodyBearing.bodyBoundsOf(component)` degrades in three tiers — a `BodyBearing` answers for itself, a `ShadowBearing` has its halo backed out, anything else reports its bounds — so a placement helper handles the whole catalog *and* arbitrary consumer components with one call, and every consumer gets the same answer. Depending on the resolver rather than on concrete types is what kept `ElwhaTooltip`'s hand-rolled halo back-out from drifting away from the menu host's (which never had one).

**Implement it when body ≠ bounds; skip it when they agree.** `ElwhaIconButton` and `ElwhaChip` fill their bounds, so the default tier is already exact and an override would be noise. `ElwhaSurface` needs none either — backing its halo out of its bounds *is* its body. Implement it on a primitive that centers, insets, or pins its body: `ElwhaButton`, `ElwhaFab`, `ElwhaSwitch`, `ElwhaCheckbox`, `ElwhaRadioButton`, `ElwhaNavRailDestination`, `ElwhaMenuItem` today.

**Hit tests derive from the body too.** A hit rect is the body grown to the size's minimum touch target and centered on it — not the bounds. That keeps the WCAG target at preferred size and caps it under stretch. Hover arming uses the same rect, so pointer feedback and clickability cannot disagree.

**Apply when:** adding any primitive whose painted chrome is smaller than its bounds under a stretching layout, or any helper that anchors to, or hit-tests against, another component.

## 8a. Right-to-left is a lib-wide contract, and every design doc says which case it is

Any component that places content along the inline axis resolves *leading* and *trailing* through `getComponentOrientation().isLeftToRight()` — never through hardcoded left / right. This is not per-component opt-in; a consumer switching a window to an RTL locale expects the whole catalog to mirror, and one component that does not is the visible defect.

**The house shape.** Compute positions as if the container were left-to-right, then mirror on the way out: `x' = totalWidth - x - width`. `ElwhaItemList.flipX` is the reference implementation (it notes the mapping is its own inverse, so the same method serves layout going out and pointer coordinates coming in); `ElwhaCardHeader`, `ElwhaCardActions` and `ElwhaButtonGroup.doLayout` follow it, and `AbstractElwhaMenuOverlay.placeBeside` / `ElwhaSideSheet.isDockedRight` are the overlay-side equivalents. One flip covers both halves of the problem — the segments swap ends *and* the items inside a segment reverse.

**Cached geometry needs an orientation hook; live geometry does not.** A layout that reads the orientation each pass is correct for free. State *derived* from the orientation and stored — `ElwhaButtonGroup`'s per-segment corner radii are the case in point — goes stale, so the component overrides `setComponentOrientation` to re-derive it.

**Every component's design doc carries an RTL section, including when the answer is "nothing".** A symmetric component genuinely needs no mirroring — `ElwhaIconButton` is one centered glyph in a square, so mirroring maps it onto itself — but silence in the doc is indistinguishable from an oversight, which is what [#565](https://github.com/OWS-PFMS/elwha/issues/565) filed. Write the no-op case down, and say *why* it is a no-op, so the next reader does not have to re-derive it. If the component later grows a second slot, that section is the thing that has to change.

**Apply when:** adding any component with a leading or trailing slot, a row or column of children, or an anchored overlay. Mirror it, add the design-doc section either way, and add an RTL case to its Tier A suite.

## 9. "Requested configuration not available" — force when the request is satisfiable, throw when it is not

Two components appeared to disagree about what a setter does when the caller asks for something the component's current configuration does not allow: `ElwhaColorPicker.setMode` / `setSwatchSource` **throw** `IllegalArgumentException`, while `ElwhaSelectField`'s `setEditable` / `setMultiSelect` **force** the conflicting axis off. Ruled in [#573](https://github.com/OWS-PFMS/elwha/issues/573): they are answering different questions, and both are right.

**The test is whether the caller's request has a satisfying state at all.**

| Situation | Behavior | Why |
|---|---|---|
| The request *is* satisfiable; only a sibling setting has to yield | **Force.** Change the sibling, honor the request, return normally. | There is exactly one consistent state the caller can have meant. Making them clear the sibling first is ceremony that carries no information — and forces every call site to order its setters defensively. |
| The request cannot be satisfied in any state the component is allowed to reach | **Throw** `IllegalArgumentException`. | The alternatives both lose information: activating something else silently substitutes a different result for the one asked for, and widening the offered set silently overrides a restriction the consumer deliberately configured. |

`setEditable(true)` on a multi-select is the first row — the caller wants an editable combo, and multi-select is simply the thing that gives way (a locked [#331](https://github.com/OWS-PFMS/elwha/issues/331) decision). `setMode(WHEEL)` on a picker whose consumer called `setModes(SWATCHES)` is the second — there is no WHEEL to activate, and inventing one would defeat the consumer's own restriction.

**Never the third option: silently doing nothing.** A programmatic setter that no-ops leaves the caller reading a getter that disagrees with what they just wrote, with no signal either way. That is the defect class [#619](https://github.com/OWS-PFMS/elwha/issues/619) fixed in `ElwhaSelectField.setOptions`.

**Null is a separate question** and this rule does not cover it. A `null` argument conventionally means "no opinion" — reset to the default (`setDisplayFunction(null)`), clear (`setSelectedValue(null)`), or ignore (`ElwhaFab.setColorStyle(null)`) — and each setter documents which. Where `null` is genuinely invalid, reject it eagerly at the setter rather than deferring the failure to paint time ([#637](https://github.com/OWS-PFMS/elwha/issues/637)).

**Apply when:** adding a setter that names one member of a closed set the component was configured with, or a setting that conflicts with another. Pick the row, and say which in the javadoc.

## 10. Observable change — named state fires a property change, model-backed value fires a `ChangeListener`

Ruled in [#446](https://github.com/OWS-PFMS/elwha/issues/446). The library was split on how a component reports a change, and the split did not fall where it looked like it did: framed as "the checkbox is the odd one out among the selection controls" it is 3-to-1 for `ChangeListener`, but counted across every *toggle* in the catalog it is 3-to-2 the other way (`ElwhaButton`, `ElwhaIconButton` and `ElwhaCheckbox` fire property changes; `ElwhaSwitch` and `ElwhaRadioButton` expose `addChangeListener`). Numbers do not settle it, so the rule is about what the change *is*.

| The observable change is… | Surface | Components |
|---|---|---|
| A **discrete named state** — selected, checked, expanded, collapsed, the active item | `firePropertyChange(PROPERTY_X, old, new)`, observed with `addPropertyChangeListener(PROPERTY_X, l)` | `ElwhaButton`, `ElwhaIconButton`, `ElwhaCheckbox`, `ElwhaBadge`, `ElwhaButtonSelectionGroup`, `ElwhaNavigationRail`, `ElwhaSwitch`, `ElwhaRadioButton`, `ElwhaRadioGroup`, `ElwhaTabs` |
| A **value from a model** — a `BoundedRangeModel` position, progress | `addChangeListener(ChangeListener)` | `ElwhaSlider`, the progress indicators |

**Why named state gets the property change.** It carries typed old and new values, which a tri-state control genuinely needs (`ChangeListener` has no payload, so a checkbox consumer would have to cache the previous value to learn *which* transition happened — and the checkbox already needs old/new internally for its accessible-state firing). Subscription is key-scoped, so a listener watching selection is not woken when the component later grows a second observable property. And `JComponent.firePropertyChange` is inherited, so it needs no `listenerList` plumbing and composes with the `AccessibleContext` property events these components already fire.

**Why a model-backed value keeps `ChangeListener`.** `BoundedRangeModel` fires `ChangeEvent` natively; wrapping that in a property change would translate an event into a different shape for no gain, and `JSlider`'s own contract is the thing consumers expect to find.

**User gesture vs programmatic change stays orthogonal, and is already uniform:** `ActionListener` fires only for user-driven commits, on every interactive component. That axis is not what this rule is about.

**Applied.** The four components that sat on the wrong side converted in [#700](https://github.com/OWS-PFMS/elwha/issues/700): `ElwhaSwitch` and `ElwhaRadioButton` fire `PROPERTY_SELECTED` (`Boolean`), `ElwhaTabs` fires `PROPERTY_ACTIVE_TAB` (`ElwhaTab`), and `ElwhaRadioGroup` fires `PROPERTY_SELECTED` (`ElwhaRadioButton`). Their `addChangeListener` surfaces are gone — no pre-1.0 shims. Nothing in the catalog reports a named state through a `ChangeListener` any more, so a new component on that side would be the only one.

**Group-level surfaces take the same rule, in whichever shape the class can carry.** A group that is a `JComponent` (`ElwhaNavigationRail`, `ElwhaTabs`) fires the inherited `firePropertyChange` and needs no wrapper. A non-visual controller (`ElwhaButtonSelectionGroup`, `ElwhaRadioGroup`) holds its own `PropertyChangeSupport` and exposes exactly one scoped pair, `addSelectionChangeListener` / `removeSelectionChangeListener`, rather than re-publishing the generic keyed `addPropertyChangeListener` — the group has one observable state, so the narrow surface is the honest one. Either way the event carries the *member*, not an index: identity survives a member being removed and replaced at the same position, where an index would compare equal and be swallowed by `PropertyChangeSupport`.

**A `JComponent` gets no convenience wrapper — the inherited keyed pair is the whole surface.** Ruled in [#725](https://github.com/OWS-PFMS/elwha/issues/725). `ElwhaButton`, `ElwhaIconButton` and `ElwhaChip` each carried a one-line `addSelectionChangeListener` that forwarded to `addPropertyChangeListener(PROPERTY_SELECTED, l)` — and no `remove` counterpart, because there is no `removePropertyChangeListener` shape a one-arg wrapper can mirror without inventing a second name. The library's own code showed the cost before any consumer did: `ElwhaButtonGroup` subscribed through the wrapper and unsubscribed through the inherited keyed call, two idioms for the two halves of one subscription, sitting in adjacent methods. The wrappers are **removed** rather than completed. Adding the missing `remove` would have kept a surface that only restates one inherited method, on three of the nine components that fire a named state — and the other six, including the `ElwhaCheckbox` precedent and everything #700 converted, already ask consumers to call the inherited pair directly.

The distinction that decides it is **inheritance, not visibility**: a non-visual controller has no inherited subscription to point at, so its scoped pair is the only surface it can offer; a `JComponent` already has one, and a wrapper over it is a second way to say the same thing that can only drift. So: if the class extends `JComponent`, publish the `PROPERTY_X` constant and stop. If it does not, hold a `PropertyChangeSupport` and expose the symmetric pair.

**Apply when:** adding an observable named state to any component. Do not write an `addXxxListener` convenience over `addPropertyChangeListener` — and if you find one, it is missing its `remove`.

## 10a. The attached-label contract — and who is exempt from it

`ElwhaCheckbox.setLabel` and `ElwhaRadioButton.setLabel` attach a **visible, clickable** label: it widens the preferred size, extends the click target, and supplies the accessible name. Both also expose `setAccessibleLabel` for the label-less case, where only the accessible name is wanted.

**`ElwhaSwitch` is exempt, deliberately.** M3 places switches in list rows where the *row* owns the label, the tap target, and the arrangement; an attached label on the switch would duplicate that container instead of completing it (switch design doc §10 / §16). The checkbox and radio have no equivalent M3 container idiom — a bare box or dot with a caption beside it *is* the anatomy — so the label has to live on the component there.

**But an exempt component must not reuse the name.** `ElwhaSwitch.setLabel` and `ElwhaSlider.setLabel` set the accessible name only, which is exactly what the checkbox and radio call `setAccessibleLabel`. One method name meaning two different things across one family is the trap [#436](https://github.com/OWS-PFMS/elwha/issues/436) fixed for the radio; both are now `setAccessibleLabel` / `getAccessibleLabel`.

**Apply when:** adding a component that takes a caption. If it attaches a visible label, name it `setLabel` and add `setAccessibleLabel` for the label-less case. If it only names itself for assistive tech, `setAccessibleLabel` is the only accessor it gets — `setLabel` is reserved.

## 11. A sizing hook stands down when the caller sets a size — and "fixed geometry" is not an exemption

Every override of `getPreferredSize()` / `getMinimumSize()` / `getMaximumSize()` opens with the escape, per hook:

```java
@Override
public Dimension getPreferredSize() {
  if (isPreferredSizeSet()) {
    return super.getPreferredSize();
  }
  return /* the component's own M3 geometry */;
}
```

**Why it is not optional.** `setPreferredSize` records the value on `JComponent` and flips `isPreferredSizeSet()`, so the call *appears* to succeed. An override that never reads the flag then answers the layout manager with its own number. No exception, no warning — the consumer reads a getter that disagrees with what they just wrote, which is the same silent-no-op class §9 rules out for setters. [#567](https://github.com/OWS-PFMS/elwha/issues/567) swept nine sites; [#712](https://github.com/OWS-PFMS/elwha/issues/712) ruled on and swept the remaining thirty-six.

**"But the geometry is fixed" is not a reason — that was ruled and rejected.** It is the most natural objection (an M3 switch really is 52×32; a FAB's halo really does have to fit) and #567 already settled it against, on the two hardest cases: `ElwhaSwitch` has fixed M3 track geometry *and* bakes `2 × HALO_OVERHANG_PX` of shadow halo into its preferred size, and it took the escape anyway, with the test that pinned the old behavior flipped. So **§8's halo-in-preferred does not license ignoring an explicit size.** Honoring a caller who leaves no room for the halo clips the halo — a visible consequence of their own instruction, not a broken component — and every leaf degrades that way gracefully. The alternative is a component that cannot be placed in a layout the consumer controls.

**Scope: the declaring class's reachability, not the geometry.** The rule binds a **top-level** class, whatever its visibility — a caller who can name the type can set a size on it, which is why #567 swept the package-private `ColorTrackSlider` alongside the public eight, and why #712 swept `ColorPickerHeader` and `TooltipSurface`. A **private nested** composition child is exempt: it is laid out solely by the component that declares it, nothing outside can name its type, and the enclosing component computes its geometry rather than setting it. Adding an escape there is dead code — verified, not assumed: no library code calls `setXxxSize` on any of them. The twenty-four exempt sites are the colour picker's panes (`SvBox`, `HueGrid`, `ShadeStrip`, `RecentRow`, `ThemeGrid`, `FavoritesGrid`, `WheelDisc`, `ChannelRow`), the dialogs' inner surfaces and dividers, the chip's slot buttons, and the side sheet's footer divider.

**Do not confuse this with §8's `getMaximumSize` rule.** They govern the same methods and answer different questions: this one is *may the caller override the answer*, §8's is *may a halo-in-preferred leaf clamp `max = preferred`* (it may not — [#199](https://github.com/OWS-PFMS/elwha/issues/199)). Both hold at once. #712 audited the second across the catalog and found no violations: the three `ShadowBearing` primitives (`ElwhaButton`, `ElwhaFab`, `ElwhaSurface`) override no maximum at all, `ElwhaChip` and `ElwhaIconButton` carry `max = preferred` legitimately because they paint no shadow, and `ElwhaButtonGroup`'s clamp is the [#660](https://github.com/OWS-PFMS/elwha/issues/660) case §8 already refuted.

**Apply when:** overriding any sizing hook on a top-level component. Add the escape to *every* hook you override, not just preferred — an explicit minimum is as much an instruction as an explicit preferred — and add the component to `SizingHookEscapeTest`'s parameterized sweep, which tests the doctrine rather than any one geometry.

## 12. The focusable flag states whether the component itself operates the keyboard — and every component states it

`setFocusable` is declared explicitly on every component, `true` or `false`, and it means one thing: *does this component, and not a child of it, respond to keystrokes.* A component that binds keys says `true`; painted chrome and wrappers that delegate to an embedded focusable say `false`. Ruled in [#688](https://github.com/OWS-PFMS/elwha/issues/688), which found `ElwhaSurface` and the `ElwhaTextField` / `ElwhaSelectField` / `ElwhaColorPicker` / `ElwhaSideSheet` decorators declaring nothing at all.

**The flag bites in one direction, which is why silence is not good enough.** `LayoutFocusTraversalPolicy.accept` takes a component into the Tab order if it has a non-empty `WHEN_FOCUSED` `InputMap` **or**, failing that, if `Component.isFocusTraversableOverridden()` — i.e. if anyone ever called `setFocusable`. Measured on all five plus controls:

| | never called | `setFocusable(true)` | `setFocusable(false)` |
|---|---|---|---|
| No `WHEN_FOCUSED` bindings (the five, `JLabel`, `JPanel`) | skipped | **tab stop** | skipped |
| Bindings present (`JButton`) | tab stop | tab stop | skipped |

So the default was already correct for the five — none of them was a live stray tab stop — but `setFocusable(true)` on a component that binds no keys **bypasses the binding test and manufactures an inert stop**. That is the same defect [#578](https://github.com/OWS-PFMS/elwha/issues/578) fixed inside `firstFocusable`, arriving through the ordinary Tab path instead. Declaring `false` is therefore not decoration: it converts a correct-by-accident default into a stated contract that a later edit cannot silently flip, and that `FocusStopDoctrineTest` pins.

**A wrapper that declines the stop must forward the focus request.** `ElwhaTextField` and `ElwhaSelectField` override `requestFocusInWindow()` / `requestFocus()` onto the embedded editor. Without that, `field.requestFocusInWindow()` returns `false` and nothing happens — the silent no-op §9 rules out, in the one place a consumer is most likely to reach for. A wrapper with no single obvious inner target (`ElwhaColorPicker`, `ElwhaSideSheet`, `ElwhaSurface`) forwards nothing; there is no honest answer to forward to, and the consumer focuses the child they actually mean.

**Dynamic is fine when the bindings are dynamic.** `ElwhaCard` calls `setFocusable(actionable)` because its `WHEN_FOCUSED` bindings only exist while it is actionable. The flag tracking the bindings *is* the rule, not an exception to it.

**Container focusability does not cascade**, so declaring `false` on a host never costs its children their stops — pinned since `ElwhaAppBarAccessibilityTest`.

**Apply when:** adding any component. Declare the flag in the constructor next to `setOpaque`, match it to whether you install `WHEN_FOCUSED` bindings, add a `requestFocus*` forward if you are a decorator over an embedded focusable, and add the component to `FocusStopDoctrineTest`'s parameterized sweep.

---

## Cross-reference

- `CLAUDE.md` — Conventions section links here for the canonical doctrine.
- `docs/development/code-style.md` — formatting + identifier rules (Spotless + Checkstyle).
- `docs/development/versioning.md` + `versioning-playbook.md` — javadoc tag rules + bump cadence.
