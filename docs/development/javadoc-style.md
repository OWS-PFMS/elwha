# Javadoc house style

**Owner epic:** #529 · **Applies to:** every public/protected entity in the shipped component
packages + the theme foundation. Demos, smokes, guards, playgrounds, and Showcase internals follow
the spirit but are not audited.

The canonical exemplars are **`switches/ElwhaSwitch`** (a component class doc, end-to-end) and
**`surface/ElwhaSurface`** (a primitive with subclass hooks — and the first doclint-clean class in
the library). Write new docs by imitating them, not by inventing structure.

## The component class doc

One `<p><strong>…</strong>` paragraph per concern, in this order, dropping paragraphs that don't
apply. The first sentence names the M3 component and what it paints — a consumer skimming an IDE
tooltip gets the whole story from it.

1. **Summary sentence.** "The Elwha Material 3 *switch* — …" with the key geometry inline via
   `{@value}` where the constants are real compile-time constants.
2. **Architecture** — the load-bearing structural decision and *why* (what it is **not** built on,
   when that's the surprise), citing the design doc section (`design §2`).
3. **Color** — the token bindings per state, as `{@link ColorRole#…}` references, and the
   paint-time-resolution promise (runtime re-skin).
4. **Geometry** — preferred-size semantics, halo/shadow reserves, centering behavior.
5. **Interaction** — pointer, drag, keyboard; which listeners fire on what (user gesture vs
   programmatic — state it explicitly, it is the API's most common surprise).
6. **Motion** — durations/easings by name, what reduced motion does, what snaps vs tweens.
7. **Icons / content** — configuration axes and their precedence.
8. **Labelling & accessibility** — the accessible role/state/action/value shape, and what the
   consumer must do (`setLabel` / `setLabelFor`).
9. **Quick start** — a compact `<pre>{@code …}</pre>` for entry-point classes (containers,
   anchors, theme API). Optional for leaf controls whose constructor is the whole story.
10. The design/research doc pointer: `{@code docs/research/elwha-<component>-design.md}`.

## Member docs

- **Every doc comment starts with a main description sentence.** A comment that opens with
  `@return` is a doclint warning (`no main description`) and reads as an empty tooltip. Getters
  get "Returns …" prose plus the `@return` tag; yes, both.
- `@param` / `@return` / `@throws` complete on everything public/protected. Setters state their
  side effects (repaint, revalidate, event fire) and their null policy.
- `@version` / `@since` per `versioning.md` — on every public class *and* member, bumped only when
  the entity itself changes.
- **State the event contract at the listener-registration site** (which gestures fire it, what
  programmatic writes do), not only in the class doc.

## Rules that keep doclint at zero

- **`@serial exclude` on every Swing component class.** Swing components are `Serializable`, so
  javadoc otherwise demands docs for every private field as serialized-form (518 of the original
  925-warning baseline was exactly this). Elwha does not support Java serialization; the tag goes
  directly above `@author` in the class doc.
- **`@value` only on compile-time constants.** A token-derived field
  (`public static final int X = SpaceScale.LG.px()`) is not a constant expression — reference it
  with `{@link #X}` instead.
- **Escape raw HTML:** `&` is `&amp;` in prose ("Interaction &amp; motion"). Named entities
  (`&ndash;`, `&rarr;`, `&times;`) are fine.
- **Every `{@link}` must resolve.** In files that don't import the target, fully qualify. Never
  link private members from public docs — describe them in prose.
- **Give `Accessible*` inner classes an explicit documented constructor** — the implicit default
  constructor is a doclint warning on a protected inner class.

## Enforcement

As of #529 Phase 0 the javadoc plugin runs `<doclint>all</doclint>` with
`<failOnError>true</failOnError>`: doclint *errors* fail `mvn package` (and therefore the `build`
check). Warnings print but do not fail — until the epic's final story flips `failOnWarnings` once
the tracked debt reaches zero. Do not reintroduce suppression.
