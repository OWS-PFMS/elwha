# Theming

Elwha's public API is a vocabulary of design tokens. Components — Elwha's and Swing's alike —
resolve those tokens out of `UIManager` at paint time, so changing the theme changes every screen
at once, and cohesion holds by construction rather than by discipline.

Everything on this page lives in
[`com.owspfm.elwha.theme`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/package-summary.html),
and every type named here links into the published
[API reference](https://ows-pfms.github.io/elwha/).

## The install API in five types

| Type | What it is |
|---|---|
| [`ElwhaTheme`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/ElwhaTheme.html) | The static facade. `config()` → a builder, `install(Config)` → apply, `current()` → the last-installed `Config`. |
| [`Config`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Config.html) | The immutable install request: a `Theme`, a `Mode`, a `Typography`, and an optional reduced-motion override. Has `with*` derivations for cheap runtime changes. |
| [`Theme`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Theme.html) | A named pair of palettes — `light()` and `dark()`. `paletteFor(Mode)` picks one. |
| [`Palette`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Palette.html) | One complete color scheme: all 49 `ColorRole` values. `Palette.builder()` requires every role, so an incomplete palette fails at build time rather than resolving to `null` at paint time. |
| [`Mode`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Mode.html) | `LIGHT`, `DARK`, or `SYSTEM`. `SYSTEM` resolves against the OS appearance at install time. |

```java
ElwhaTheme.install(
    ElwhaTheme.config()
        .theme(MaterialPalettes.baseline())
        .mode(Mode.SYSTEM)
        .typography(Typography.defaults())
        .build());
```

Only `theme` is meaningful to set explicitly for a first install; `mode` and `typography` have
sensible defaults.

`install` is idempotent and re-callable, runs an ordered sequence (resolve mode → install the base
LAF → select the palette → write the `Elwha.*` keys → write the FlatLaf-native keys → bake the
state-layer keys → apply typography → repaint every live window), and is callable from any thread.
Off the EDT it dispatches the writes to the EDT and blocks until they land, so when it returns the
theme is installed.

## The token families

Five families are written into `UIManager` under the `Elwha.*` namespace, and each has a typed
accessor you use from paint code:

| Family | Type | Accessor | Contents |
|---|---|---|---|
| Color roles | [`ColorRole`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/ColorRole.html) (49) | `resolve()` → `Color` | The full M3 scheme — `PRIMARY` / `ON_PRIMARY` / `PRIMARY_CONTAINER` / `SURFACE` / `SURFACE_CONTAINER_HIGH` / `OUTLINE` / `ERROR` / … with the mandatory `on`-pairings for foreground contrast. |
| Shape scale | [`ShapeScale`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/ShapeScale.html) (7) | `px()`, `arcPx()` | `NONE` 0 / `XS` 4 / `SM` 8 / `MD` 12 / `LG` 16 / `XL` 28 / `FULL`. |
| Space scale | [`SpaceScale`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/SpaceScale.html) (6) | `px()` | 4 dp ladder: `XS` 4 / `SM` 8 / `MD` 12 / `LG` 16 / `XL` 24 / `XXL` 32. |
| Type roles | [`TypeRole`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/TypeRole.html) (15) | `resolve()` → `Font`, `pt()` | The complete M3 scale, `DISPLAY_LARGE` through `LABEL_SMALL`. |
| State layers | [`StateLayer`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/StateLayer.html) (5) | `over(base, tint)` | Hover 8% / focus 10% / pressed 10% / dragged 16% / selected 12%, as opacity overlays on a role color rather than separate colors. |

```java
Color fill   = ColorRole.PRIMARY.resolve();
int   radius = ShapeScale.MD.px();
Font  label  = TypeRole.LABEL_MEDIUM.resolve();
int   pad    = SpaceScale.MD.px();
Color hover  = StateLayer.HOVER.over(fill, ColorRole.ON_PRIMARY);
```

**Resolve tokens at paint time; never cache them in fields.** That rule is what makes a runtime
theme or mode switch actually repaint correctly.

## Dark mode

A `Theme` always carries both palettes, so dark mode is a `Mode`, not a second theme:

```java
ElwhaTheme.install(ElwhaTheme.current().withMode(Mode.DARK));   // pin dark
ElwhaTheme.install(ElwhaTheme.current().withMode(Mode.SYSTEM)); // follow the OS
```

`Mode.SYSTEM` is resolved **at install time** — it reads the OS appearance once and picks a
palette. It does not subscribe to later OS changes; if you want to follow a mid-session switch,
re-install from your own listener. `Mode.resolved()` tells you which concrete mode a `SYSTEM`
install landed on, and `isConcrete()` distinguishes `LIGHT`/`DARK` from `SYSTEM`.

## Bundled palettes

[`MaterialPalettes`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/MaterialPalettes.html)
ships the demo palettes in two tiers, each discovered from its own resource directory at runtime
rather than from a hardcoded list:

| Accessor | Returns | Contents |
|---|---|---|
| `baseline()` | `Theme` | The M3 baseline scheme — what a theme builder produces before a source color is chosen. The right theme for validating your pipeline. |
| `primary()` | `List<Theme>` | The curated tier: the baseline plus the ROYGBIV set (red, orange, yellow, green, blue, indigo, deep purple). |
| `secondary()` | `List<Theme>` | The broader exploration tier — ten more Material Theme Builder palettes. |

The two tiers are disjoint, and each is returned in **spectral order** (sorted by the hue of its
`primary` color, neutrals last). Each accessor returns the *same* `Theme` instance for a given
bundled palette, so a picker can match the installed theme against a tier by identity —
`baseline()` is the very object `primary()` carries.

One known baseline quirk, and it is correct M3 output rather than a transcription error: in
**light** mode the baseline's `primaryContainer` and `secondaryContainer` are near-identical,
because M3's default Tonal Spot algorithm compresses the secondary hue rotation toward white at
the high-tone end. They are comfortably distinct in dark mode. If you need visibly distinct
containers in both modes, ship your own palette built with a more expressive scheme variant.

## Shipping your own palette

Two routes, depending on how much you want to hand-write.

**From JSON (recommended).**
[`PaletteLoader.loadTheme(resourcePath)`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/PaletteLoader.html)
reads an Elwha-normalized palette JSON off the classpath and returns a `Theme`. The schema is thin and maps 1:1 onto the 49
`ColorRole` keys — a top-level `name`, an optional `description`, and `light` / `dark` objects each
holding every role's `camelCase` key mapped to a `"#rrggbb"` string:

```java
Theme brand = PaletteLoader.loadTheme("/com/acme/theme/acme-palette.json");
ElwhaTheme.install(ElwhaTheme.config().theme(brand).mode(Mode.SYSTEM).build());
```

A resource that is missing, malformed, or short a single role throws `IllegalArgumentException`
with a message naming the problem — it never resolves silently to `null`.

**In code.** Build the two palettes and pair them:

```java
Palette light = Palette.builder()
    .set(ColorRole.PRIMARY, new Color(0x0F6FCB))
    // … all 49 roles
    .build();
Theme brand = new Theme("Acme", light, dark);
```

`Palette.Builder.build()` validates completeness, so this route means writing all 49 roles. The
JSON route exists because that is tedious by hand.

**Converting a Material Theme Builder export.** A raw MTB export is not the Elwha schema, but the
conversion is mechanical and scripted:

```bash
python3 scripts/convert_mtb_palette.py [INPUT_DIR] [OUTPUT_DIR]
```

It takes the standard-contrast `schemes.light` / `schemes.dark` verbatim — a modern MTB export
already carries all 49 role keys — and drops the medium/high-contrast variants and the source-color
metadata. Each `material-theme-<color>.json` becomes `<color>.json`.

Dropping a converted palette into a bundled tier directory is enough for it to appear in the
Showcase's palette picker, with no code change.

## Typography

[`Typography.defaults()`](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/Typography.html)
is the bundled Inter, shipped with real Regular (400) and Medium (500)
faces so the M3 weight distinction renders rather than being synthesized. The faces are registered
with the `GraphicsEnvironment` on first call and cached; if the bundled resources cannot be loaded
it degrades to a logical sans-serif rather than failing.

`Typography.ofFamily(name)` builds the same 15-role scale over any installed family, looking up a
`"<family> Medium"` face for the 500-weight roles and falling back to `WEIGHT_MEDIUM`, then
`Font.BOLD`, when the family has no Medium face.

```java
ElwhaTheme.install(
    ElwhaTheme.config()
        .theme(MaterialPalettes.baseline())
        .typography(Typography.ofFamily("Roboto"))
        .build());
```

## Overriding individual roles

For a one-off tweak rather than a full palette, write the `UIManager` key **after** `install`
returns, then repaint:

```java
ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());
UIManager.put("Elwha.color.primary", new ColorUIResource(0x0F6FCB));
SwingUtilities.updateComponentTreeUI(yourRootFrame);
```

Note the ordering: `install` writes the whole namespace, so an override applied first is lost. Also
note that overriding `primary` alone does not update `onPrimary`, `primaryContainer`, or the baked
state-layer keys derived from it — for anything beyond a spot fix, build a real palette.

## Why your existing Swing widgets change too

Elwha does not only write the `Elwha.*` namespace. As part of the same install it writes a curated
set of **FlatLaf-native `UIManager` keys** — the keys FlatLaf's own UI delegates read — mapping
each to the token that should drive it. That bridge is why a plain `JButton`, `JTextField`,
scrollbar, or dialog in an existing screen picks up the Elwha palette and typography without being
touched, and it is what makes incremental adoption practical: install the theme, ship, then replace
widgets with Elwha components where the richer behavior earns its keep. The mapping itself is
library-internal — you configure it by choosing a palette, not by calling into it.

## Using the bundled icons

`MaterialIcons` exposes the bundled Material Symbols through four small families, and picking the
right one matters because two component APIs are typed against them:

- **Named accessors** — `MaterialIcons.pushPin()`, `.delete(20)`, … — one glyph, no-arg at the
  house 24 px or sized.
- **`get(name)`** — the same lookup by bare Material Symbol name, for names chosen at runtime. A
  name outside the bundle throws `IllegalArgumentException` naming the glyph (as do `pair` and
  `symbol`), so a bad name fails at the call site instead of rendering a broken icon.
- **`pair(name)`** — an `IconPair` holding the outline (fill 0) and filled (fill 1) cuts, for
  hand-rolled selected/unselected icon swaps.
- **`symbol(name)`** — a strongly-typed `Symbol` handle that resolves both fill-axis cuts on
  demand. This is the type the icon overloads of `ElwhaTab.of(...)` and
  `ElwhaNavRailDestination.of(...)` take — those components flip the fill axis themselves when a
  tab or destination activates, so they want the handle, not a pre-resolved icon or pair:

```java
ElwhaTab home = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
ElwhaNavRailDestination settings =
    ElwhaNavRailDestination.of(MaterialIcons.symbol("settings"), "Settings");
```

The full glyph inventory lives in the
[`icons` package Javadoc](https://ows-pfms.github.io/elwha/com/owspfm/elwha/icons/package-summary.html).

## Using your own icons

The bundled Material Symbols (see [the component index](components.md) and the
[`MaterialIcons` Javadoc](https://ows-pfms.github.io/elwha/com/owspfm/elwha/icons/MaterialIcons.html))
are fixed at build time — client code cannot add to the bundle. To bring your own icons and have
them follow the installed theme exactly like the bundled set, keep the SVGs in **your own
resources** at a client-owned classpath location and wrap the loaded icon with
`MaterialIcons.themed(...)`:

```java
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.icons.MaterialIcons;

Icon rocket = MaterialIcons.themed(new FlatSVGIcon("com/acme/icons/rocket.svg", 24, 24));
```

`themed` attaches Elwha's theme-aware color filter: every painted color remaps to the current
`Label.foreground` at paint time, so the icon re-skins live on a light/dark switch with no
re-allocation. Monochrome artwork themes cleanly; multicolor artwork will flatten to the
foreground color — for icons that must keep their own colors, skip `themed` and manage color
yourself.

Two notes for a consistent result:

- **Match the house style axes** when sourcing from
  [fonts.google.com/icons](https://fonts.google.com/icons): Rounded, weight 400, fill 0, optical
  size 20px — fill 1 only for selected/active states. Mixed axes read as mismatched weight at
  small sizes.
- **Keep your icons in your own namespace.** Dropping SVGs into Elwha's
  `com/owspfm/icons/material/` path and calling `MaterialIcons.get(String)` appears to work, but
  your names race the bundle on the classpath with undefined resolution order.

## Reduced motion

`Config.reducedMotion()` is a `Boolean`, deliberately nullable. `null` (the default) means "defer
to the OS reduced-motion signal", which Elwha's animation layer detects on first use. A non-null
value forces the global toggle on or off regardless of the OS setting:

```java
ElwhaTheme.install(ElwhaTheme.current().withReducedMotion(Boolean.TRUE));
```

## Reference

Every type on this page is in the published API reference at
**[ows-pfms.github.io/elwha](https://ows-pfms.github.io/elwha/com/owspfm/elwha/theme/package-summary.html)**.

The full token taxonomy and the FlatLaf key bridge are documented for maintainers in
`docs/research/elwha-token-taxonomy.md` and `docs/research/elwha-theme-install-api.md`
(Appendix B of the latter specifies the palette JSON schema and the MTB conversion).
