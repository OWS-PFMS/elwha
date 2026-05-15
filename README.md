# Elwha

[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![JDK 21](https://img.shields.io/badge/JDK-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![FlatLaf 3.2.5](https://img.shields.io/badge/FlatLaf-3.2.5-green.svg)](https://www.formdev.com/flatlaf/)

A Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/) — `ElwhaCard`, `ElwhaChip`, and related primitives.

## Components

| Component | What it is |
|---|---|
| **`ElwhaCard`** | A theme-aware card primitive: header, body, surface variants, hover/pressed/selected states, optional collapse/expand, leading icon, trailing actions. |
| **`ElwhaCardList<T>`** | A list of `ElwhaCard` items with selection, drag-to-reorder, filter, sort, orientation modes (vertical / horizontal / wrap / grid). |
| **`ElwhaChip`** | A compact chip primitive: text + optional leading icon + optional trailing action button, themeable via UIManager keys, auto-contrast foreground. |
| **`ElwhaChipList<T>`** | A list of `ElwhaChip` items with selection (`NONE` / `SINGLE` / `SINGLE_MANDATORY` / `MULTIPLE`), drag-to-reorder, pinned-partition + anchored modes, icon affordances. |
| **`ElwhaList<T>`** | The shared cross-cutting contract implemented by both `ElwhaCardList` and `ElwhaChipList` — orientation, gap, padding, empty / loading state, filter, sort. |
| **`MaterialIcons`** | Helper that loads Material Symbols SVGs (Rounded / 400 / fill 0; 20-dp optical-size axis, rendered at 24px by default with sized overloads) via `FlatSVGIcon`, with a theme-aware color filter so icons follow `Label.foreground`. |
| **`ElwhaTheme`** | The design-token install API: a `Palette` / `Theme` / `Mode` / `Typography` / `Config` value-object chain plus a single static `install(Config)` that writes the full `Elwha.*` token namespace and a curated FlatLaf-native key bridge so raw Swing inherits the design language. Ships `MaterialPalettes.baseline()` (M3 baseline) and bundles Inter (Regular + Medium) for `Typography.defaults()`. |

## Install

> ⚠️ **0.1.0 — pre-release.** API is not yet stable; breaking changes between minor versions are expected until 1.0.0.

### Maven (GitHub Packages)

Add the repository and dependency to your `pom.xml`:

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

```java
import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.chip.ElwhaChip;
import javax.swing.*;
import java.awt.*;

public class Demo {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      FlatLightLaf.setup();

      JFrame frame = new JFrame("Elwha Demo");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().setLayout(new FlowLayout());

      // A simple card
      ElwhaCard card = new ElwhaCard()
          .setHeader("Recent activity", "Last 30 days");
      frame.add(card);

      // A simple chip
      ElwhaChip chip = new ElwhaChip("Tag");
      frame.add(chip);

      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    });
  }
}
```

## Playgrounds & demos

The library ships interactive playground apps that exercise the full surface of each component family:

- `com.owspfm.elwha.card.playground.ElwhaCardPlayground`
- `com.owspfm.elwha.chip.ElwhaChipPlayground`
- `com.owspfm.elwha.theme.playground.ThemePlayground` — visual harness for the token foundation (color swatches, type scale, raw-Swing components gallery, live light/dark/system mode toggle).

Run them directly from a checkout: `mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"`.

## Theming

Every component reads theme defaults via `UIManager` under a component-specific namespace (`ElwhaChip.*`, etc.). Override at app startup before any component is created:

```java
UIManager.put("ElwhaChip.arc", 20);
UIManager.put("ElwhaChip.padding", new Insets(4, 12, 4, 12));
UIManager.put("ElwhaChip.background", new Color(0xEBF5FF));
```

See the playground apps for the full list of available keys and their effects.

## Requirements

- **JDK 21** or later (target bytecode version 65)
- **FlatLaf 3.2.5+** (transitive dependency — pulled in automatically)
- Pure Swing — no JavaFX, no Compose, no AWT-only fallback

## Status

This library was extracted from the [OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) project (its [epic #231](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/231)) on 2026-05-12. It is **pre-1.0**:

- API may change between minor versions (`0.x.y`)
- Two breaking-change epics are queued post-extraction:
  - `ElwhaChip` → `ElwhaChip` rename, aligning with Material's chip taxonomy
  - `ElwhaList<T>` extended to share selection + drag-reorder surface across both list families

API will stabilize at **1.0.0** after those land.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE).
