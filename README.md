# FlatComp

[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![JDK 21](https://img.shields.io/badge/JDK-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![FlatLaf 3.2.5](https://img.shields.io/badge/FlatLaf-3.2.5-green.svg)](https://www.formdev.com/flatlaf/)

A Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/) — `FlatCard`, `FlatChip`, and related primitives.

## Components

| Component | What it is |
|---|---|
| **`FlatCard`** | A theme-aware card primitive: header, body, surface variants, hover/pressed/selected states, optional collapse/expand, leading icon, trailing actions. |
| **`FlatCardList<T>`** | A list of `FlatCard` items with selection, drag-to-reorder, filter, sort, orientation modes (vertical / horizontal / wrap / grid). |
| **`FlatChip`** | A compact chip primitive: text + optional leading icon + optional trailing action button, themeable via UIManager keys, auto-contrast foreground. |
| **`FlatChipList<T>`** | A list of `FlatChip` items with selection (`NONE` / `SINGLE` / `SINGLE_MANDATORY` / `MULTIPLE`), drag-to-reorder, pinned-partition + anchored modes, icon affordances. |
| **`FlatList<T>`** | The shared cross-cutting contract implemented by both `FlatCardList` and `FlatChipList` — orientation, gap, padding, empty / loading state, filter, sort. |
| **`MaterialIcons`** | Helper that loads Material Symbols SVGs (Rounded / 400 / fill0 / 20px) via `FlatSVGIcon`, with a theme-aware color filter so icons follow `Label.foreground`. |

## Install

> ⚠️ **0.1.0 — pre-release.** API is not yet stable; breaking changes between minor versions are expected until 1.0.0.

### Maven (GitHub Packages)

Add the repository and dependency to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github-flatcomp</id>
    <url>https://maven.pkg.github.com/OWS-PFMS/flatcomp</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.owspfm</groupId>
    <artifactId>flatcomp</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

You'll need a GitHub Personal Access Token with `read:packages` scope in your `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>github-flatcomp</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_PAT_WITH_READ_PACKAGES</password>
  </server>
</servers>
```

## Quick start

```java
import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.ui.components.card.FlatCard;
import com.owspfm.ui.components.chip.FlatChip;
import javax.swing.*;
import java.awt.*;

public class Demo {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      FlatLightLaf.setup();

      JFrame frame = new JFrame("FlatComp Demo");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().setLayout(new FlowLayout());

      // A simple card
      FlatCard card = new FlatCard()
          .setHeader("Recent activity", "Last 30 days");
      frame.add(card);

      // A simple chip
      FlatChip chip = new FlatChip("Tag");
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

- `com.owspfm.ui.components.card.playground.FlatCardListShowcase`
- `com.owspfm.ui.components.chip.FlatChipPlayground`

Run them directly from a checkout: `mvn compile exec:java -Dexec.mainClass="com.owspfm.ui.components.chip.FlatChipPlayground"`.

## Theming

Every component reads theme defaults via `UIManager` under a component-specific namespace (`FlatChip.*`, etc.). Override at app startup before any component is created:

```java
UIManager.put("FlatChip.arc", 20);
UIManager.put("FlatChip.padding", new Insets(4, 12, 4, 12));
UIManager.put("FlatChip.background", new Color(0xEBF5FF));
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
  - `FlatChip` → `FlatChip` rename, aligning with Material's chip taxonomy
  - `FlatList<T>` extended to share selection + drag-reorder surface across both list families

API will stabilize at **1.0.0** after those land.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE).
