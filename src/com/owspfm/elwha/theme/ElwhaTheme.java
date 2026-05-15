package com.owspfm.elwha.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Window;
import java.util.Objects;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * The static entry point that turns a {@link Config} into a live theme.
 *
 * <p>One operation — {@link #install(Config)} — serves both startup and runtime switching: there is
 * no separate "switch" API, switching <em>is</em> re-installing. {@code ElwhaTheme} is a static
 * facade rather than an instantiable object because Swing has exactly one look-and-feel per JVM, so
 * a global static API matches physical reality (see {@code elwha-theme-install-api.md} §8, Q5).
 *
 * <pre>{@code
 * // Startup
 * ElwhaTheme.install(
 *     ElwhaTheme.config()
 *         .theme(MaterialPalettes.baseline())
 *         .mode(Mode.SYSTEM)
 *         .build());
 *
 * // Runtime switch — same call, cheap derivation
 * ElwhaTheme.install(ElwhaTheme.current().withMode(Mode.DARK));
 * }</pre>
 *
 * <p>{@link #install(Config)} is idempotent and re-callable. It runs the ordered eight-step
 * sequence from the install-API doc §3: resolve mode, install the base LAF, select the palette,
 * write the {@code Elwha.*} keys, write the FlatLaf-native keys, compute-and-bake the
 * state-layer keys, apply typography, and repaint every live window.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class ElwhaTheme {

  private static volatile Config current;

  private ElwhaTheme() {}

  /**
   * Returns a fresh {@link Config.Builder}.
   *
   * @return a new config builder
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Config.Builder config() {
    return Config.builder();
  }

  /**
   * Returns the {@link Config} most recently passed to {@link #install(Config)}, or {@code null} if
   * no theme has been installed yet.
   *
   * <p>Callers derive runtime switches from this — {@code install(current().withMode(Mode.DARK))} —
   * without threading the config through their own state.
   *
   * @return the last-installed config, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Config current() {
    return current;
  }

  /**
   * Installs the given {@link Config} as the live theme, running the full eight-step sequence.
   *
   * <p>Idempotent and re-callable: calling it again with a different config is exactly how a
   * runtime theme or mode switch is performed. All live windows are re-skinned via {@link
   * SwingUtilities#updateComponentTreeUI} as the final step; if called off the Event Dispatch
   * Thread, that final repaint is dispatched to the EDT.
   *
   * @param config the config to install
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void install(Config config) {
    Objects.requireNonNull(config, "config");

    Mode resolvedMode = config.mode().resolved();
    installBaseLaf(resolvedMode);
    Palette palette = config.theme().paletteFor(resolvedMode);

    writeTokenKeys(palette);
    FlatLafKeyMapping.applyStaticKeys(palette);
    FlatLafKeyMapping.applyStateLayerKeys(palette);
    applyTypography(config.typography());

    current = config;
    repaintAllWindows();
  }

  private static void installBaseLaf(Mode resolvedMode) {
    if (resolvedMode == Mode.DARK) {
      FlatDarkLaf.setup();
    } else {
      FlatLightLaf.setup();
    }
  }

  // Step 4: write the Elwha.* keys — the namespace Elwha's own components resolve against.
  // Colors are stored as ColorUIResource so updateUI() will re-install any that also reach a
  // component through LookAndFeel.installColors; see FlatLafKeyMapping for why this matters.
  private static void writeTokenKeys(Palette palette) {
    for (ColorRole role : ColorRole.values()) {
      UIManager.put(role.uiKey(), new ColorUIResource(palette.get(role)));
    }
    for (ShapeScale shape : ShapeScale.values()) {
      UIManager.put(shape.uiKey(), shape.defaultPx());
    }
    for (SpaceScale space : SpaceScale.values()) {
      UIManager.put(space.uiKey(), space.defaultPx());
    }
    for (StateLayer layer : StateLayer.values()) {
      UIManager.put(layer.uiKey(), layer.defaultOpacity());
    }
    UIManager.put(StateLayer.DISABLED_CONTENT_KEY, StateLayer.DEFAULT_DISABLED_CONTENT);
    UIManager.put(StateLayer.DISABLED_CONTAINER_KEY, StateLayer.DEFAULT_DISABLED_CONTAINER);
  }

  // Step 7: register the fonts and write the Elwha.type.* keys plus the global defaultFont.
  // Fonts are stored as FontUIResource for the same updateUI() re-install reason as colors.
  private static void applyTypography(Typography typography) {
    for (TypeRole role : TypeRole.values()) {
      UIManager.put(role.uiKey(), new FontUIResource(typography.get(role)));
    }
    UIManager.put("defaultFont", new FontUIResource(typography.get(TypeRole.BODY_MEDIUM)));
  }

  // Step 8: the only step that touches live components — they re-resolve tokens per the binding
  // rule and re-skin.
  private static void repaintAllWindows() {
    Runnable repaint =
        () -> {
          for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
          }
        };
    if (SwingUtilities.isEventDispatchThread()) {
      repaint.run();
    } else {
      SwingUtilities.invokeLater(repaint);
    }
  }
}
