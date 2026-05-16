# Elwha

[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![JDK 21](https://img.shields.io/badge/JDK-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![FlatLaf 3.2.5](https://img.shields.io/badge/FlatLaf-3.2.5-green.svg)](https://www.formdev.com/flatlaf/)

**Elwha is a Material 3-flavored design language for Swing.** Its public API is a vocabulary of design tokens — color roles, shape and type scales, spacing steps, state-layer overlays — that every screen in a consuming app paints against, so cohesion is enforced by construction instead of relying on developer discipline. Components (`ElwhaCard`, `ElwhaChip`, `ElwhaSurface`, `ElwhaIconButton`, their list containers) and raw Swing widgets (`JButton`, `JTextField`, scrollbars, dialogs, …) both resolve the same tokens at paint time.

> Elwha is a Pacific Northwest river restored after the largest dam removal in US history — the name puts the library on [Open Water Systems](https://openwatersystems.com)' clean-water mission.

## What this is — and isn't

- **A token-first design system, not a component grab-bag.** Tokens are the API; components are downstream of the design language. The cohesion problem ("our pages feel inconsistent") is a vocabulary problem, not a component problem — Elwha solves it by giving every component, raw or Elwha-built, the same fixed surface to paint against.
- **Material 3-flavored, not spec-compliant.** Elwha lifts the M3 *token taxonomy* as its semantic API. It does not chase spec compliance and does not claim the Material brand. (Drift is expected — e.g., the v1 surface treatment is intentionally flat; M3-style tonal-lift elevation is queued for v2.)
- **A Swing-native design system.** Elwha is implemented on top of [FlatLaf](https://www.formdev.com/flatlaf/) as its rendering substrate, but FlatLaf is plumbing — Elwha owns the design vocabulary, configures FlatLaf via `UIManager`, and ships the higher-level components Swing itself doesn't have (Chip, Card, their list containers).
- **Pre-1.0.** API is not yet stable; breaking changes between minor versions are expected until 1.0.0. Components are mid-migration onto the just-landed token foundation — see [Roadmap](#roadmap).

## Install

### Maven (GitHub Packages)

```xml
<repositories>
  <repository>
    <id>github-elwha</id>
    <url>https://maven.pkg.github.com/OWS-PFMS/elwha</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.owspfm</groupId>
    <artifactId>elwha</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

You'll need a GitHub Personal Access Token with `read:packages` scope in your `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>github-elwha</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_PAT_WITH_READ_PACKAGES</password>
  </server>
</servers>
```

## Quick start

Install the theme **before** building any UI, then build normally — raw Swing and Elwha components both inherit the design language from a single install call.

```java
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.chip.ElwhaChip;
import javax.swing.*;
import java.awt.*;

public class Demo {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      // One call wires up FlatLaf, the M3 baseline theme (light + dark palettes),
      // Inter typography, shape/space/state-layer scales, and the FlatLaf-native key bridge.
      ElwhaTheme.install(
          ElwhaTheme.config()
              .theme(MaterialPalettes.baseline())
              .mode(Mode.SYSTEM)
              .build());

      JFrame frame = new JFrame("Elwha Demo");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().setLayout(new FlowLayout());

      // Elwha components resolve tokens at paint time.
      frame.add(new ElwhaCard().setHeader("Recent activity", "Last 30 days"));
      frame.add(new ElwhaChip("Tag"));

      // Raw Swing inherits the design language via the FlatLaf-native key bridge.
      frame.add(new JButton("Save"));

      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    });
  }
}
```

Switch modes at runtime by re-installing — `ElwhaTheme.install` is idempotent, and every color/font is stored as a `ColorUIResource` / `FontUIResource` so `SwingUtilities.updateComponentTreeUI` re-skins live components correctly.

## The token foundation

`ElwhaTheme.install(Config)` writes five token families into `UIManager` under the `Elwha.*` namespace:

| Family | Type | What it is |
|---|---|---|
| **Color roles** | `ColorRole` (49 enums) | The full M3 color scheme — `PRIMARY` / `ON_PRIMARY` / `PRIMARY_CONTAINER` / `SURFACE` / `OUTLINE` / `ERROR` / `SURFACE_CONTAINER_HIGH` / … with mandatory `on`-pairings for foreground-on-container contrast. |
| **Shape scale** | `ShapeScale` (7 enums) | Corner radii: `NONE` / `XS` / `SM` / `MD` / `LG` / `XL` / `FULL`. |
| **Space scale** | `SpaceScale` (6 enums) | 4dp-based ladder: `XS` (4) / `SM` (8) / `MD` (12) / `LG` (16) / `XL` (24) / `XXL` (32). For padding, gaps, insets. |
| **Type roles** | `TypeRole` (12 enums) | M3 type roles (`TITLE_LARGE` / `BODY_MEDIUM` / `LABEL_SMALL` / …), rendered with the bundled Inter (Regular + Medium). |
| **State layers** | `StateLayer` (5 enums) | Hover / focus / pressed / dragged / selected as **opacity overlays** on a role color, not separate colors. M3 defaults: 8 % / 10 % / 10 % / 16 % / 12 %. |

Every token resolves at paint time from `UIManager`. **Components must not cache resolved values** in fields — the rule that makes runtime theme/mode switching work.

```java
// Typed access from inside a component's paint code:
Color fill   = ColorRole.PRIMARY.resolve();
int   radius = ShapeScale.MD.px();
Font  label  = TypeRole.LABEL_MEDIUM.resolve();
int   pad    = SpaceScale.MD.px();
Color hover  = StateLayer.HOVER.over(fill, ColorRole.ON_PRIMARY);   // tint-on-base composite at hover opacity
```

### Customizing the palette

`MaterialPalettes.baseline()` ships the M3 baseline scheme. For one-off tweaks, override specific roles via `UIManager` **after** `install` returns:

```java
ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());
UIManager.put("Elwha.color.primary",          new ColorUIResource(0x0F6FCB));
UIManager.put("Elwha.color.primaryContainer", new ColorUIResource(0xD8E9FF));
SwingUtilities.updateComponentTreeUI(yourRootFrame);
```

For a brand palette, build a full `Palette` (`Palette.builder().set(ColorRole.PRIMARY, …)…build()` — all 49 roles required), wrap it in a `Theme(name, light, dark)`, and pass that to `ElwhaTheme.install`. A `Palette.from(baseline)` derive-and-tweak affordance is queued for a future release.

The full token taxonomy and the FlatLaf-native key bridge (which keys FlatLaf reads, which token each is wired to) live in `docs/research/elwha-token-taxonomy.md` and `docs/research/elwha-theme-install-api.md`.

## Components

Built on top of the token foundation; depend only on Swing + FlatLaf.

| Component | What it is |
|---|---|
| **`ElwhaCard`** | A surface primitive: header, body, surface variants (`ELEVATED` / `OUTLINED` / `FILLED`), hover/pressed/selected states, optional collapse/expand, leading icon, trailing actions. V2 rebuild on the token foundation tracked in #253. |
| **`ElwhaCardList<T>`** | A list of `ElwhaCard` items with selection, drag-to-reorder, filter, sort, and orientation modes (vertical / horizontal / wrap / grid). |
| **`ElwhaChip`** | A token-native chip primitive: text + optional leading icon + optional trailing action button. Treatment via `ChipVariant` (`FILLED` / `OUTLINED` / `GHOST`); the surface role is independently overridable per instance; foreground is always derived as the `on`-pair of the effective surface role. Four M3 chip-type factory presets (`assistChip` / `filterChip` / `inputChip` / `suggestionChip`) sit over the orthogonal axes. |
| **`ElwhaChipList<T>`** | A list of `ElwhaChip` items with selection modes (`NONE` / `SINGLE` / `SINGLE_MANDATORY` / `MULTIPLE`), drag-to-reorder, pinned-partition + anchored modes, icon affordances. |
| **`ElwhaSurface`** | A token-native rounded surface primitive — a `JPanel` subclass that paints a role-filled, round-rect, optionally outlined background through the shared `SurfacePainter`. Elwha's equivalent of Material's *Paper*; the foundation `ElwhaCard` V2 (#253) will compose for its background paint. Four typed setters: `surfaceRole` / `shape` / `borderRole` / `borderWidth`. |
| **`ElwhaIconButton`** | A token-native M3 icon-button primitive with four emphasis variants (`FILLED` / `FILLED_TONAL` / `OUTLINED` / `STANDARD`), `CLICKABLE` / `SELECTABLE` interaction modes, a declarative `setIcons(resting, selected)` toggle pattern, and the five M3 sizes via `IconButtonSize` (`XS` / `S` / `M` / `L` / `XL`). Drops into `JToolBar` directly. |
| **`IconButtonGroup`** | Mutually-exclusive selection across a set of `ElwhaIconButton`s — the toolbar "radio" pattern Swing's `ButtonGroup` provides for `AbstractButton`s. Non-mandatory (deselect-allowed) and mandatory (one always selected) modes. |
| **`ElwhaList<T>`** | The cross-cutting list contract implemented by both list containers — orientation, gap, padding, empty / loading state, filter, sort. |
| **`MaterialIcons`** | Helper that loads bundled Material Symbols (Rounded / weight 400 / 20-dp optical-size axis, rendered at 24 px by default with sized overloads) via `FlatSVGIcon`, theme-coloured via the shared `Label.foreground` filter. Includes `MaterialIcons.pair(name)` returning an outline / fill `IconPair` for the canonical M3 toggle-icon-swap pattern. Bundled outline/fill pairs: `push_pin`, `anchor`, `favorite`, `star`, `info`, `help`, `delete`, `edit`, `visibility`. |

## Playgrounds

The library ships five interactive playground apps:

- **`com.owspfm.elwha.theme.playground.ThemePlayground`** — the central harness. Color-role swatches (all 49), the 12-role type scale, a raw-Swing components gallery, and top-level tabs for each of the token-native components (`Chip`, `Icon Button`, `Surface`). Live light / dark / system mode toggle. This is the place to verify a custom palette before wiring it into your app.
- **`com.owspfm.elwha.card.playground.ElwhaCardPlayground`** — exercises every variant, mode, and configurable property of `ElwhaCard` and `ElwhaCardList`.
- **`com.owspfm.elwha.chip.ElwhaChipPlayground`** — exercises every variant and selection mode of `ElwhaChip` and `ElwhaChipList`.
- **`com.owspfm.elwha.surface.playground.ElwhaSurfacePlayground`** — the 49 × 7 `ColorRole × ShapeScale` matrix for `ElwhaSurface` and a live combo-driven panel.
- **`com.owspfm.elwha.iconbutton.playground.ElwhaIconButtonPlayground`** — variant gallery (4 × 6 states), toggle examples (icon-swap pairs across all variants), the 5 × 4 size × variant matrix, a `JToolBar` mockup, and a live combo-driven panel.

Run them from a checkout:

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.surface.playground.ElwhaSurfacePlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.iconbutton.playground.ElwhaIconButtonPlayground"
```

## Requirements

- **JDK 21** or later (target bytecode 65)
- **FlatLaf 3.2.5+** (transitive — pulled in automatically)
- Pure Swing — no JavaFX, no Compose, no AWT-only fallback

## Roadmap

The token foundation (epic [#30](https://github.com/OWS-PFMS/elwha/issues/30)) landed. `ElwhaChip` is rebuilt onto it ([#31](https://github.com/OWS-PFMS/elwha/issues/31)), `ElwhaSurface` ([#43](https://github.com/OWS-PFMS/elwha/issues/43)) and `ElwhaIconButton` ([#45](https://github.com/OWS-PFMS/elwha/issues/45)) shipped on top. The remaining v1 component cut is still being finalized.

**1.0.0 ships after** these still-open epics complete:

- **`ElwhaCard` V2 rebuild** ([#253](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/253)) — retire Card V1's raw-typed escape hatches (`setSurfaceColor`, `setKeepSummaryWhenExpanded`, the `setHeader` overload pattern) by composing `ElwhaSurface` for the background paint and exposing only typed token setters. Card V1 still ships as-is in the meantime.
- **Shared list surface** ([#252](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/252)) — extend `ElwhaList<T>` to share selection and drag-reorder across both list families, replacing the family-specific `CardSelectionModel` / `ChipSelectionModel` split.
- **Tonal-lift elevation (v2)** — replace the current flat treatment with M3-style elevation as a tonal shift on `surface`, not drop shadows.

Expect breaking changes between minor versions until 1.0.0. Org-level project board tracking the work: [Project #5 — Material Flat Component Library](https://github.com/orgs/OWS-PFMS/projects/5).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE). Bundled third-party assets (Material Symbols, Inter font, Capitaine cursors) carry their own licenses; see [`NOTICE`](NOTICE).
