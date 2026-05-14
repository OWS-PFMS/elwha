# FlatComp Design Direction — Design-System Stance

**Status:** Working direction, not yet locked. Captures a philosophical design conversation about what FlatComp should *be* before more components ship.

**Date:** 2026-05-13

**Author:** Charles Bryan (`cfb3@uw.edu`), recorded via design conversation with Claude.

**Related:**
- [`flatcomp-extraction-decisions.md`](flatcomp-extraction-decisions.md) — strategic decisions made at extraction time.
- [`flatcomp-coupling-audit.md`](flatcomp-coupling-audit.md) — the audit that confirmed extraction-readiness.
- Open epics: [#27](https://github.com/OWS-PFMS/flatcomp/issues/27) (`FlatPill` → `FlatChip` — complete in lib), OWS-Local-Search-GUI#252 (`FlatList<T>` selection/drag unification), OWS-Local-Search-GUI#253 (`FlatCard` V2 API).

This document is **direction**, not locked decisions. Treat it as the source of intent for the next phase of work; promote items to a "locked decisions" doc as they solidify.

---

## TL;DR — the position

1. **FlatComp is a design system, not a component grab-bag.** The pages-feel-non-cohesive smell in OWS-Local-Search-GUI is the forcing function. Components are downstream of the design language.
2. **The centerpiece is a design-token layer** modeled on Material 3's vocabulary (color roles, shape scale, type roles, spacing scale, state layers).
3. **FlatLaf is the substrate, not the design system.** FlatComp configures FlatLaf via `UIManager` keys and ships the components Swing doesn't have (Chip, Card, their list containers). FlatLaf paints `JButton` et al.; FlatComp tokens give them a coherent look.
4. **Components are `final` and composition-driven**, in the spirit of MUI's slot/variant/theme model. Inheritance is not part of the public API.
5. **Component scope stays small and deliberate.** Build a component only when raw Swing + tokens can't express the need.
6. **Tokens ship inside FlatComp as a single artifact.** No separate tokens repo. Consumer palettes (e.g. OWS's brand colors) live in the consumer codebase; FlatComp owns the vocabulary, not the values.
7. **Swing stays the substrate.** JavaFX is not a serious alternative for the OWS context (see footnote).

---

## 1. Framing — what problem is FlatComp solving?

Two intertwined problems, in priority order:

1. **Visual / interaction cohesion across the consumer app.** OWS-Local-Search-GUI has accumulated multiple ad-hoc chip and card implementations; pages are starting to feel non-cohesive. Extraction was driven by this duplication, not by any plan to ship a general-purpose lib.
2. **A reusable design language for future OWS-PFMS software.** If the library is well-designed, more consumers may follow. Not a current requirement; should not drive API decisions.

Implication: FlatComp's primary forcing function is OWS's real needs. The API must not encode OWS-specific assumptions (coupling stance already enforces this), but it also must not pre-build for hypothetical consumers.

## 2. Why "design system" rather than "component library"

The user-stated symptom — "pages starting to feel non-cohesive" — is not usually a component problem. You can ship perfect chips and cards and still get incoherent pages, because cohesion lives in the *shared vocabulary the components draw from*: the same handful of colors, corner radii, spacing steps, type sizes, and state-layer opacities, used consistently.

When that vocabulary is implicit — lives in the developer's head, scattered across literal hex codes and `setBorder(new EmptyBorder(8,12,8,12))` calls — every new screen drifts. When it's explicit and every component reads from it, cohesion is enforced *by construction*.

Two corollaries:

- **The token layer is more important than the components themselves.** Get the tokens right and the components are a relatively mechanical exercise; get the components right with no token discipline and the lib ships the same incoherence in a tidier package.
- **The lib has an opinion about how pages are composed.** Variants, slots, and a spacing scale should make it *hard* to build a visually incoherent screen, not just possible to build a cohesive one.

## 3. Anchoring on Material 3's vocabulary

FlatComp should lift Material 3's token vocabulary almost wholesale.

**Why M3 specifically:**

- It is the most carefully designed public token system going. Re-inventing it would burn time for no upside.
- It gives a stable, well-debated, well-documented language to design against.
- The user already likes the Material look. Confirmed: FlatComp aims to be Material-*flavored*, in the same way MUI is now Material-flavored rather than spec-compliant. Drift from spec is expected and acceptable.

**Not the same as adopting Material Design as a brand.** FlatComp does not claim spec compliance; it does not need to. It claims to use the M3 *token taxonomy* as its semantic API.

## 4. The token API — five categories

Trimmed from M3 to what a Swing lib actually needs:

| Category | What it is | Approx. size for v1 |
|---|---|---|
| **Color roles** | Semantic names: `primary` / `onPrimary` / `primaryContainer` / `onPrimaryContainer` / `surface` / `surfaceVariant` / `outline` / etc. Pairs are mandatory — every container has a matching "on-container" foreground. | ~16 roles (skip tertiary + inverse for v1) |
| **Type roles** | `titleLarge` / `bodyMedium` / `labelSmall` / etc. Each resolves to a `Font`. Drop the M3 `display` tier — Swing apps rarely need it. | ~9–12 |
| **Shape scale** | Corner radii by role: `none` / `xs` / `sm` / `md` / `lg` / `xl` / `full`. | 7 levels |
| **Spacing scale** | 4dp-based step ladder: `xs`(4) / `sm`(8) / `md`(12) / `lg`(16) / `xl`(24) / `xxl`(32). For paddings, gaps, insets. | 6–7 levels |
| **State layers** | Hover / focus / pressed / dragged / selected as *opacity overlays on a role color*, not separate colors. M3: 8% / 10% / 10% / 16% / 12%. | 5 states |

Deferred to v2:

- **Elevation.** M3 expresses elevation as *tonal lift* (surface gets bluer/lighter at higher elevation) rather than shadow. Doable in Swing but adds a tonal-palette computation. Ship flat for v1.
- **Motion tokens.** Swing animation is a tar pit. Skip until there is a real need.

## 5. Storage vs. access — the key design call

**One source of truth: `UIManager`.** Every token lives there under a `FlatComp.*` namespace (`FlatComp.color.primary`, `FlatComp.shape.md`, `FlatComp.type.bodyMedium`, etc.).

**A typed facade on top.** Enums (or records) that know how to resolve themselves from `UIManager`. Components never call `UIManager.getColor("FlatComp.color.primary")` directly — they go through the facade.

**Why the hybrid:**

1. FlatLaf already plumbs `UIManager` through every paint call, so a raw `JButton` whose `Button.background` you've pointed at your `primary` token Just Works. No special path for raw Swing.
2. The typed facade gives FlatComp components type safety, IDE autocomplete, and refactor-safety.
3. Theme swapping (light → dark, or custom palette) is a single re-population of `UIManager`. No object graph to rebuild.

## 6. Sketch of the core types

Direction only — not final API.

```java
public enum ColorRole {
  PRIMARY, ON_PRIMARY, PRIMARY_CONTAINER, ON_PRIMARY_CONTAINER,
  SECONDARY, ON_SECONDARY, SECONDARY_CONTAINER, ON_SECONDARY_CONTAINER,
  ERROR, ON_ERROR, ERROR_CONTAINER, ON_ERROR_CONTAINER,
  SURFACE, ON_SURFACE, SURFACE_VARIANT, ON_SURFACE_VARIANT,
  BACKGROUND, ON_BACKGROUND,
  OUTLINE, OUTLINE_VARIANT;

  public Color resolve() {
    return UIManager.getColor("FlatComp.color." + key());
  }
}

public enum ShapeScale {
  NONE(0), XS(4), SM(8), MD(12), LG(16), XL(28), FULL(9_999);
  // .px()
}

public enum SpaceScale {
  XS(4), SM(8), MD(12), LG(16), XL(24), XXL(32);
  // .px(), .insets(...)
}

public enum TypeRole {
  TITLE_LARGE, TITLE_MEDIUM, TITLE_SMALL,
  BODY_LARGE,  BODY_MEDIUM,  BODY_SMALL,
  LABEL_LARGE, LABEL_MEDIUM, LABEL_SMALL;
  public Font resolve() { /* UIManager lookup */ }
}

public enum StateLayer {
  HOVER(0.08f), FOCUS(0.10f), PRESSED(0.10f), DRAGGED(0.16f), SELECTED(0.12f);
  public Color over(Color base, ColorRole tintRole) { /* alpha-blend */ }
}
```

Theme install (direction):

```java
FlatCompTheme.builder(FlatLightLaf.class)
    .palette(MaterialPalettes.indigo())
    .typography(Typography.system())
    .install();
```

The builder writes both `FlatComp.*` keys *and* the FlatLaf-native keys (`Button.background`, `Component.arc`, etc.) so raw Swing inherits the design language.

## 7. Variants as token expressions

The payoff of the token approach: component variants stop being hardcoded color sets and become *declarations in the token language*.

```java
public enum FlatChipVariant {
  FILLED     (ColorRole.PRIMARY_CONTAINER,   ColorRole.ON_PRIMARY_CONTAINER,   null),
  OUTLINED   (ColorRole.SURFACE,             ColorRole.ON_SURFACE,             ColorRole.OUTLINE),
  GHOST      (null,                          ColorRole.ON_SURFACE,             null),
  WARM_ACCENT(ColorRole.SECONDARY_CONTAINER, ColorRole.ON_SECONDARY_CONTAINER, null);
}
```

Direct consequences:

- **Most of the V2 `FlatCard` escape-hatches dissolve.** `setSurfaceColor` becomes "pick a different variant, or override the *role* at theme level." `setKeepSummaryWhenExpanded` was never a color thing — stays.
- **Reskins are free.** Swap palette → every component repaints with new semantics intact.
- **Light/dark mode is mechanical.** A `Palette` is a `Map<ColorRole, Color>`; ship `MaterialPalettes.lightIndigo()` / `MaterialPalettes.darkIndigo()` and consumers flip.
- **`final` on components becomes natural.** There's nothing left worth subclassing — paint logic is just token resolution.

## 8. The state-layer / FlatLaf impedance mismatch

Worth flagging because it'll bite if ignored.

**FlatLaf models hover/pressed/focus as *separate colors*** (`Button.hoverBackground`, `Button.pressedBackground`).

**M3 models them as *opacity overlays on the role color***.

Two honest choices:

1. **Compute and bake.** At theme install, alpha-blend the state-layer opacity over the role color and write the *result* into FlatLaf's `*hoverBackground` / `*pressedBackground` keys. Math runs once per install. Components painted by FlatLaf look right. FlatComp's own components (FlatChip, FlatCard) paint the overlay live at runtime since they own their paint.
2. **Fork the state model.** Build a `FlatButton` that does overlay painting at runtime. Worse: raw `JButton` no longer picks up state layers the M3 way, which breaks the "raw Swing inherits the design language" promise — the whole point of going token-first.

**Direction: option 1, decisively.** Blend math is ~5 lines of `Color` interpolation; runs at theme-install, not per-paint.

## 9. Component scope — what to build vs. what to lean on Swing for

The rule: **build a component only when raw Swing + tokens can't express what you need.**

| Need | Verdict |
|---|---|
| Button, TextField, CheckBox, Radio, ComboBox, Label, ScrollPane, ProgressBar, Slider, Tooltip | Raw Swing + tokens. Done. |
| TabbedPane | Raw Swing + tokens *probably*; wrap if M3 tab indicator / state layers are wanted. |
| Dialog | Raw `JDialog` + tokens; ship a builder/factory if patterns repeat. |
| Menu / Menubar / Popover | Raw Swing + tokens, unless M3 menu surfaces become a need. |
| **Chip** | Doesn't exist in Swing → build (have `FlatChip`). |
| **Card** | Doesn't exist in Swing → build (have `FlatCard`). |
| **Chip list / Card list** | Selection + drag-reorder semantics → build (have these). |
| Snackbar / Toast | Doesn't exist → build *if and when* OWS needs it. |

This is the same logic MUI uses: they don't ship a `<Div>`; they style raw `<div>` via theme + `sx`. They ship `<Button>` because HTML's `<button>` isn't enough.

The library's component layer therefore stays small and deliberate — Chip, Card, their lists, maybe later Snackbar / Tabs / Dialog patterns when a real consumer need surfaces. The token layer is universal.

## 10. Composition over inheritance — `final` as a design statement

Direction: **make FlatComp components `final`** (or otherwise sealed against public extension), and lean on MUI-style composition primitives:

- **Variants** (already present) — small enum of pre-baked configurations.
- **Slots** — named injection points for sub-content (leading icon, trailing icon, label, delete affordance, header, footer). The `FlatCard.setHeader` overload pattern in V1 is a slot API in disguise; making this explicit cleans up much of the V2 epic (#253).
- **Theming** — centralized token resolution via `FlatCompTheme` / `UIManager`.
- **Interaction modes** (already present on cards).

The V2 `FlatCard` API escape-hatches (`setSurfaceColor`, `setKeepSummaryWhenExpanded`, raw label getters, `setHeader` overloads) are the *symptom* of an effectively-final-ish component with no coherent composition story. Each new consumer need became a new setter. Explicit slot + variant + theme model is the cure.

**The user has accepted consumer-side pain in OWS-Local-Search-GUI** as the cost of cleaning this up before more components ship. OWS-tool dev is paused for this reason.

## 11. The one open design question — public vs. sealed tokens

The fork worth deciding before code:

- **Public tokens.** Consumers can write `panel.setBackground(ColorRole.SURFACE_VARIANT.resolve())` for one-off cases. Maximum flexibility. **Risk:** consumers (including future-you) reach for tokens instead of building proper variants, and drift returns — slightly less bad than hardcoded hex codes but the same shape of problem.
- **Sealed tokens.** Consumers must go through `FlatChipVariant`, `FlatCardVariant`, etc. Cohesion enforced by *structural* constraint, not by discipline. **Risk:** every new consumer need becomes a new variant, recreating the V2-API escape-hatch problem at the variant level.

MUI exposes both; MUI codebases drift partly because of this. Purer design systems (Radix Themes, Carbon) hide tokens.

**Tentative direction: public-but-discouraged.** Expose tokens, document them as escape valves only, prefer variants, treat direct token use in consumer code as a code-review smell. Revisit once there's real consumer usage data.

## 12. FlatLaf is the substrate, FlatComp is the design system

Mental model — four layers, each owning one job:

```
┌────────────────────────────────────────────────┐
│ FlatComp components (FlatChip, FlatCard, ...) │  ← we build, read tokens
├────────────────────────────────────────────────┤
│ FlatComp tokens / theme (color roles, shape,   │  ← we build, the design language
│ type, elevation, state layers)                 │
├────────────────────────────────────────────────┤
│ FlatLaf (paints JButton/JTextField/etc.,       │  ← already exists, we configure it
│ exposes UIManager keys & .properties)          │
├────────────────────────────────────────────────┤
│ Swing (JComponent, painting, event model)     │  ← JDK, untouched
└────────────────────────────────────────────────┘
```

We do **not** fork FlatLaf or subclass `FlatLightLaf` / `FlatDarkLaf`. Theme application is a side-effect of installing the LAF *plus* writing tokens to `UIManager`.

Analogies:

- **FlatLaf : Swing :: Tailwind : CSS.** A coherent system on top of a primitive substrate without replacing it.
- **FlatComp : FlatLaf :: a design-system config : Tailwind.** Not replacing Tailwind; configuring it semantically and adding the bespoke components Tailwind doesn't ship.

**Portability is theoretical, not practical.** Tokens are semantic and could in principle target another Swing LAF. The components are built against FlatLaf's painting assumptions (corner-arc keys, focus-width keys, etc.). If FlatLaf ever died, work would be needed — but not rebuilding from zero. Acceptable risk for a maintained, mature library that JetBrains products depend on.

## 13. Where the tokens live — one repo, one artifact

**Tokens ship inside FlatComp.** OWS-Local-Search-GUI depends on FlatComp; that pulls components *and* tokens together. No separate `flatcomp-tokens` repo.

**Why not a separate tokens repo:**

- **Tokens and components are intrinsically coupled in visual language.** New shape steps, renamed color roles, type-scale changes need the components to adapt in lockstep. Two repos turn one version-skew problem into a triangular one for zero benefit.
- **FlatComp's tokens are not generic design tokens.** Adobe Spectrum / Salesforce Lightning / GitHub Primer publish token repos because they target web + iOS + Android + Figma simultaneously and need one source of truth across substrates. FlatComp's tokens are tightly bound to Swing + FlatLaf's `UIManager`. No cross-substrate pressure justifying the split.
- **One consumer today.** Extract when duplication is *real* and *painful*, not for hypothetical reuse — same rule that justified extracting FlatComp from OWS-tool in the first place.

**Where OWS's own brand palette lives:** in OWS-Local-Search-GUI, not in FlatComp.

A palette (concrete colors mapped to roles) is just *data* — a `Map<ColorRole, Color>` or a small builder. OWS defines `OwsPalette.light()` / `OwsPalette.dark()` in its own codebase and passes it to `FlatCompTheme.builder(...).palette(owsPalette).install()`. **The token vocabulary is FlatComp's; the palette values are the consumer's.** No third repo.

**Cheap future-proofing — enforce a clean internal package boundary:**

```
com.owspfm.ui.components.theme.*     ← tokens, palette interface, theme installer
com.owspfm.ui.components.card.*      ← uses theme; never the other way
com.owspfm.ui.components.chip.*      ← uses theme; never the other way
com.owspfm.ui.components.flatlist.*  ← uses theme; never the other way
```

Components depend on `theme`; `theme` depends on nothing in components. A one-line ArchUnit / Checkstyle rule away from being mechanically enforced. If a future world ever demands extracting tokens to a separate artifact, the `theme` package pulls out cleanly because it has zero inbound coupling to component code.

**When to flip and ship a separate tokens artifact:**

- A real second consumer surfaces that wants just the tokens, not the components.
- Multiple sibling Swing component libs (e.g. `flatcomp-charts`, `flatcomp-forms`) start sharing tokens.
- Multi-substrate token consumption becomes real (Swing + JavaFX + web style sheet) — not happening, per the JavaFX footnote.

Until one of those is real: one repo, one artifact, clean internal boundary.

## 14. Adjacent ecosystem — the gap FlatComp fills

Honest landscape scan (May 2026):

| Project | What it is | Useful as |
|---|---|---|
| **FlatLaf** | The modern flat LAF. Active maintainer (FormDev), used by JetBrains IDEs. | Substrate — what we depend on. |
| **FlatLaf IntelliJ Themes** | Property bundles that re-skin FlatLaf with JetBrains-community palettes. | Palette reference, not a semantic system. |
| **material-ui-swing** (vincenzopalazzo) | Material-on-Swing attempt. | Alive-ish, partial, uneven quality. Not bettable. |
| **Radiance** (Substance successor, Grouchnikov) | Sophisticated LAF + animation framework, Material-flavored skins. | Its own world; heavier than FlatLaf; not M3-aligned. |
| **WebLaF / JTattoo / Synthetica / Darklaf** | Older or commercial LAFs. | Not relevant for a 2026 Material direction. |
| **Nimbus** | JDK built-in. | Ugly. |

For **design systems** specifically — token-driven, semantic-color-roles, MUI/Radix/Carbon-style — **nothing in Swing.** The closest in spirit is **AtlantaFX**, which does design-system-on-top-of-a-modern-LAF on JavaFX. Worth a glance for API-shape ideas; not directly usable.

The gap exists because most "new Java desktop" activity migrated to JavaFX in the 2010s, and from there much migrated to web/Electron. Surviving Swing apps (IntelliJ, NetBeans, big finance shops) roll their own ad-hoc styling rather than ship reusable libs.

**Implication for FlatComp:**

1. Not duplicating prior art. A token-driven Material-flavored design-system layer on FlatLaf is a real gap.
2. Audience is small but specific — Swing-app teams who've outgrown ad-hoc FlatLaf styling. Healthy narrow target.
3. No in-language reference to crib from. Port ideas from MUI / Radix / Material Web / AtlantaFX. The M3 spec itself is the real reference.

## 15. Next concrete steps (not commitments)

Roughly in order:

1. ~~Lock the **token taxonomy**~~ — **DONE (2026-05-14).** See [`flatcomp-token-taxonomy.md`](flatcomp-token-taxonomy.md), now a locked-decisions doc: 46 tokens across color / type / shape / spacing / state.
2. ~~Define the **`FlatCompTheme` install API**~~ — **DONE (2026-05-14).** See [`flatcomp-theme-install-api.md`](flatcomp-theme-install-api.md), now a locked-decisions doc.
3. Build **one reference palette** (light + dark) — likely Material 3-aligned indigo or purple, to validate the end-to-end pipeline.
4. ~~Retrofit `FlatChip` variants~~ — **plan LOCKED (2026-05-14):** see [`flatcomp-flatchip-rebuild.md`](flatcomp-flatchip-rebuild.md). Grew from a narrow retrofit into a token-native rebuild of FlatChip's styling API (treatment-only variants, M3 types as factory presets, typed role/scale setters, `WARM_ACCENT` removed, auto-contrast deleted). Implementation waits on steps 1–3 landing in code. Serves as the pattern for `FlatCard`.
5. Use the token layer as the foundation of the **`FlatCard` V2 API** (#253), retiring the accumulated escape-hatches.
6. Revisit **`FlatList<T>` selection/drag unification** (#252) once selection state has a token-based visual story.
7. **1.0.0** after #27 (rename — done in lib), #252, #253, and the token layer all land.

---

## Footnote — why not switch to JavaFX

Considered and rejected for the current OWS context.

**Where JavaFX would genuinely win:**

- CSS-based styling (closer to MUI's `sx` model than `UIManager`).
- Scene graph + observable properties/bindings.
- First-class effects (drop shadows, blurs, opacity, transforms) — would make elevation trivial.
- First-class animation (timelines, transitions) — would make M3 motion tokens implementable.
- Slightly better-looking baseline; better existing design-system libs (AtlantaFX, JMetro, MaterialFX).

**Where Swing still wins (and why we stay):**

- Maturity of complex widgets (`JTable`, `JTree`, accessibility). Relevant for OWS-tool's data-heavy panels.
- JetBrains validation — IntelliJ et al. run on Swing + FlatLaf in 2026. Demonstrates Swing can look modern.
- JavaFX's strategic posture is shaky. Oracle handed maintenance to Gluon years ago; community smaller than 2017; "is JavaFX dying" is not a dead conversation.
- JavaFX threading (Application Thread) has the same single-thread-confinement constraints as EDT. Migration does not simplify concurrency bugs.
- **Migration cost for OWS-Local-Search-GUI is enormous** — and that, not FlatComp, is the actual decision point. FlatComp is the dependent. A consumer migration to JavaFX would require a FlatComp rewrite, not a port.

**When to flip the position:**

- OWS-tool needs charts/timelines/heavy animation Swing fights.
- Maintainer count rises and the lone-developer constraint loosens.
- A consumer beyond OWS-tool asks for JavaFX and matters.
- JetBrains itself migrates off Swing (canary signal).

**The hybrid trap:** `JFXPanel` / `SwingNode` interop exists. **Don't.** Works in demos, falls apart in production — focus traversal, drag-and-drop, look-and-feel mismatch, dual event loops. Pick a substrate and commit.

Until one of the flip conditions is true: stay on Swing, push the token work, and treat "is the design system any good?" as a separable question from "is the substrate any good?". The substrate is fine.
