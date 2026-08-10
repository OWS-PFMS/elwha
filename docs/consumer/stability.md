# Stability policy

1.0.0 is the point at which Elwha's API becomes something you can build against without expecting
it to move under you. This page states exactly what that promise covers.

## Semantic versioning from 1.0.0 on

Elwha follows [Semantic Versioning 2.0.0](https://semver.org). From 1.0.0:

| Release | What may change |
|---|---|
| **Patch** (`1.0.x`) | Bug fixes and internal changes only. No new API, no signature changes, no behavior changes beyond correcting a defect. Visual output may change where it was wrong. |
| **Minor** (`1.x.0`) | Additive only — new components, new methods, new enum constants, new overloads. Existing signatures keep working. Deprecations may be introduced, but nothing is removed. |
| **Major** (`2.0.0`) | The only release that may remove or change existing API. |

Concretely, code that compiles against 1.0.0 will compile against every later 1.x without edits.

**New enum constants are additive but not always harmless.** Elwha's variant and mode enums are the
component vocabulary and will grow. If you `switch` over one exhaustively without a `default`, a
minor release can break your build. Add a `default` branch.

## What counts as public API

The promise covers types in `com.owspfm.elwha.*` that are declared `public` and are not in one of
the excluded areas below. The [API reference](https://ows-pfms.github.io/elwha/) lists every public
type, so anything absent from it is package-private and therefore not API — but note the reverse
does not hold: the Javadoc also documents the harness packages carved out below, and being
documented does not by itself make a type stable.

**Not API, and free to change or disappear in any release:**

- The **Showcase** (`com.owspfm.elwha.showcase`) — a visual harness, not a library surface.
- **Playgrounds** — every `playground` subpackage.
- Classes named `*Demo`, `*Smoke`, `*Guard`, and the `card.fixes` diagnostic harnesses. These are
  build-time artifacts left behind by the story that shipped a feature.
- Anything package-private, including `overlay.AbstractElwhaOverlay`'s internals and the
  FlatLaf key-mapping bridge.

**`UIManager` keys are a grey area.** The `Elwha.*` namespace is documented and stable enough to
override for a spot fix (see [Theming](theming.md#overriding-individual-roles)), but the specific
FlatLaf-native keys Elwha writes are an implementation detail of the bridge. Build on the typed
token accessors — `ColorRole.resolve()` and friends — wherever you can.

## Deprecation policy

Before 1.0.0 the rule was blunt: **no backwards-compatibility shims.** Breaking changes landed
directly, with no aliases or deprecation layers, because nothing depended on the published
artifact.

From 1.0.0 that rule is retired. Anything Elwha intends to remove is first **deprecated in a minor
release**, with `@Deprecated`, a `@deprecated` Javadoc tag naming the replacement, and a
`CHANGELOG.md` entry. It stays in place, working, for the remainder of the 1.x line, and is removed
only in the next major. You will never find a method simply gone between 1.2 and 1.3.

## Platform target

**JDK 21.** Elwha compiles to bytecode 65, so JDK 21 is a hard floor — a JDK 17 runtime cannot load
the jar. Newer JDKs work.

Raising the floor is a breaking change and will not happen inside 1.x.

## Threading

Elwha components follow **Swing's standard single-thread rule**, no more and no less: construct
components wherever you like before they are realized, but once a component is in a realized
hierarchy, every mutation — setters, model changes, `add`/`remove` — belongs on the event dispatch
thread, via `SwingUtilities.invokeLater(...)` from anywhere else. Elwha adds no locking of its own,
and its animation machinery (ripples, morphs, hover tracking) ticks on `javax.swing.Timer`, which
already dispatches on the EDT.

The **one deliberate exception** is theme install: `ElwhaTheme.install(...)` may be called from any
thread — off the EDT it dispatches its writes to the EDT and blocks until they land (see
[Theming](theming.md)). No component API makes that promise; assume EDT-only.

## Dependency stance

Elwha depends on **Swing and FlatLaf only**. There is no app framework, no dependency-injection
container, no logging facade, no JSON library beyond the parser FlatLaf already bundles, and no
domain types of any kind. The full compile-scope set a consumer inherits is:

```
com.formdev:flatlaf
com.formdev:flatlaf-extras
com.formdev:flatlaf-intellij-themes
com.github.weisj:jsvg            (runtime, via flatlaf-extras — SVG icon rendering)
```

This is a deliberate constraint, not an accident of the current state. Elwha was extracted from an
application specifically so that it would carry none of that application with it, and adding to
the list is treated as an architectural decision rather than a convenience. Practically, it means
dropping Elwha into an existing Swing app cannot conflict with your dependency graph.

## Relationship to Material 3

Elwha implements **Material 3 Expressive** as a design system for desktop Java. It adopts the M3
token taxonomy as its semantic API and follows the M3 component specs closely — but it is not
spec-compliant, does not claim the Material brand, and diverges where the desktop Swing context
demands it or where a spec behavior was judged not worth its cost.

Those divergences are deliberate and documented rather than silently absorbed: each component's
design doc under `docs/research/` carries an explicit divergence section — for example
`elwha-navigation-rail-design.md` §18 on rail hosting, and `elwha-button-design.md` §10 on the
toggle color model. Consult the relevant design doc when a component's behavior surprises you
against the M3 spec.

Divergences may be *narrowed* in a minor release when doing so is additive. Behavior changes that
would break a layout built against the current rendering wait for a major.

## Upgrading from 0.1.0

Short version: **there is no upgrade path, and you almost certainly do not need one.**

0.1.0 (2026-05-12) was the extraction snapshot — the components as they existed inside
OWS-Local-Search-GUI, published so the extraction could be verified. It shipped under the root
package `com.owspfm.ui.components.*` with classes named `FlatCard`, `FlatCardList`, `FlatPill` and
`FlatPillList`, and it had **no design-token foundation at all** — theming was per-component
`UIManager` keys.

Everything moved after that:

- The root package became `com.owspfm.elwha.*`.
- `FlatPill` → `ElwhaChip`; `FlatCard` → `ElwhaCard`, then rebuilt entirely (the V3 architecture,
  where the card is chrome and content is composed from companion primitives).
- The `theme` package — the token foundation this library is actually organized around — did not
  exist in 0.1.0.
- The parallel card-list and chip-list families collapsed into a single generic
  `list.ElwhaItemList<T>`.

No class in 0.1.0 survives to 1.0.0 under the same fully-qualified name. Migration is a rewrite of
your imports and your construction code, guided by the [Component index](components.md) — treat
1.0.0 as a first adoption rather than an upgrade. That is how it is being treated internally:
0.1.0 was never adopted by a downstream application, which is why the pre-1.0 no-shims rule was
sustainable.

## Reporting a break

If a 1.x release breaks source or binary compatibility, that is a bug, not a policy change. File it
at [OWS-PFMS/elwha/issues](https://github.com/OWS-PFMS/elwha/issues) with the two versions and the
failing signature.
