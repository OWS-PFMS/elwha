# Quick start

This page assumes the dependency resolves — if not, start with [Install](install.md).

## The one rule

**Install the theme before you create any UI.** Elwha components resolve their colors, fonts,
shapes and spacing out of `UIManager` at paint time, and `ElwhaTheme.install` is what puts them
there. It also installs the FlatLaf look-and-feel underneath, so a component built before the
install would be created against the platform LAF.

## A complete program

Copy this into `QuickStart.java` and run it. It is compiled against the shipped jar as part of the
release checks — see [Compile proof](#compile-proof) below.

```java
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class QuickStart {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          // One call installs FlatLaf, the M3 baseline palettes (light + dark), the bundled
          // Inter typography, and the shape / space / state-layer scales. Do this before you
          // create any UI.
          ElwhaTheme.install(
              ElwhaTheme.config()
                  .theme(MaterialPalettes.baseline())
                  .mode(Mode.SYSTEM)
                  .build());

          // A card is chrome only. Content is composed by adding companion primitives.
          ElwhaCard card = ElwhaCard.elevatedCard();
          card.add(new ElwhaCardHeader().setTitle("Recent activity").setSubtitle("Last 30 days"));
          card.add(new ElwhaCardSupportingText("12 cycles found across 4 factors."));

          ElwhaButton refresh = ElwhaButton.filledButton("Refresh");
          refresh.addActionListener(e -> System.out.println("refresh"));

          ElwhaSwitch liveUpdates = new ElwhaSwitch(true);
          liveUpdates.addActionListener(e -> System.out.println(liveUpdates.isSelected()));

          JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING));
          controls.add(refresh);
          controls.add(liveUpdates);
          // Raw Swing picks up the same theme — Elwha writes the FlatLaf-native UIManager keys.
          controls.add(new JButton("Export"));

          JFrame frame = new JFrame("Elwha quick start");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.setLayout(new BorderLayout());
          frame.add(card, BorderLayout.CENTER);
          frame.add(controls, BorderLayout.SOUTH);
          frame.setSize(420, 260);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
```

## What each piece is doing

**`ElwhaTheme.install(config)`** writes the `Elwha.*` token keys and the FlatLaf-native keys into
`UIManager`, applies the typography, and repaints every live window. It is idempotent and
re-callable — that is how you switch theme or mode at runtime. It is safe to call from any thread;
off the EDT it dispatches the writes to the EDT and blocks until they land.

**`MaterialPalettes.baseline()`** is the Material 3 baseline scheme, carrying both a light and a
dark `Palette`. See [Theming](theming.md) for the other bundled palettes and for shipping your own.

**`Mode.SYSTEM`** resolves to `LIGHT` or `DARK` from the operating system's appearance at install
time. Pass `Mode.LIGHT` or `Mode.DARK` to pin it.

**`ElwhaCard.elevatedCard()`** returns card *chrome* — the rounded, elevated, token-resolved
surface. `ElwhaCard` owns no typed content slots; you compose content by adding companion
primitives from the same package (`ElwhaCardHeader`, `ElwhaCardSupportingText`,
`ElwhaCardMedia`, `ElwhaCardActions`, and the rest) through the ordinary `add(...)` call.

**The raw `JButton`** renders in the Elwha design language too. Elwha does not only style its own
components — it also writes the FlatLaf-native `UIManager` keys, so the Swing widgets you already
have inherit the palette and typography without being rewritten. That is the intended migration
path: install the theme, keep your existing screens working, and adopt Elwha components where they
buy you something.

## Switching mode at runtime

`ElwhaTheme.current()` returns the last-installed `Config`, and `Config` has `with*` derivations,
so a mode toggle needs no state of your own:

```java
ElwhaTheme.install(ElwhaTheme.current().withMode(Mode.DARK));
```

Every color and font is stored as a `ColorUIResource` / `FontUIResource`, so live components
re-skin correctly. `install` already repaints all open windows.

## Where to go next

- **[API reference](https://ows-pfms.github.io/elwha/)** — the full Javadoc for every class named
  on this page, redeployed on every push to `main`.
- **[Theming](theming.md)** — palettes, dark mode, typography, and shipping your own colors.
- **[Component index](components.md)** — the full catalog with its Showcase leaf.
- **[Stability policy](stability.md)** — what 1.0 promises.

## Seeing it all at once

Everything Elwha ships has a leaf in **The Elwha Showcase**, the visual harness in this
repository. From a checkout:

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
```

That is the fastest way to see a component's states, and to try a bundled palette against your own
content before wiring it in.

## Compile proof

The program above is not a sketch — it is compiled verbatim against the built artifact:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # or your JDK 21 path
mvn -B clean install -DskipTests                    # in an elwha checkout
# then, in a scratch project depending on com.owspfm:elwha
mvn -B clean compile
```

If you change the snippet on this page, re-run that check. A quick start that does not compile is
the failure mode this page exists to prevent.
