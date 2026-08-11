package com.owspfm.elwha.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
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
 * write the {@code Elwha.*} keys, write the FlatLaf-native keys, compute-and-bake the state-layer
 * keys, apply typography, and repaint every live window.
 *
 * @author Charles Bryan
 * @version v0.5.0
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
   * SwingUtilities#updateComponentTreeUI} as the final step.
   *
   * <p>Callable from any thread. Every component in the library resolves its tokens out of {@code
   * UIManager} from {@code paintComponent}, so the writes have to be ordered against painting or a
   * live window can render one frame from a half-swapped palette. Called off the Event Dispatch
   * Thread, this therefore dispatches the writes to the EDT and <em>blocks</em> until they land —
   * so when it returns, the theme is installed on whatever thread the caller was on. Only the mode
   * resolution stays on the calling thread, because {@link Mode#SYSTEM} shells out to the OS and
   * that is work the EDT should not do when the caller has already volunteered another thread.
   *
   * @param config the config to install
   * @throws IllegalStateException if the calling thread is interrupted while waiting for the EDT
   * @version v0.5.0
   * @since v0.1.0
   */
  public static void install(Config config) {
    Objects.requireNonNull(config, "config");

    Mode resolvedMode = config.mode().resolved();
    Palette palette = config.theme().paletteFor(resolvedMode);

    onEventDispatchThread(() -> applyToUiManager(config, resolvedMode, palette));
  }

  private static void applyToUiManager(Config config, Mode resolvedMode, Palette palette) {
    installBaseLaf(resolvedMode);

    writeTokenKeys(palette);
    FlatLafKeyMapping.applyStaticKeys(palette);
    FlatLafKeyMapping.applyStateLayerKeys(palette);
    applyTypography(config.typography());

    current = config;
    // #176 Phase 5 — apply the reduced-motion override if the config carries one. {@code null}
    // means "defer to the OS reduced-motion signal" (which MorphAnimator detects on first use
    // per design doc §9); a non-null Boolean forces the global toggle.
    if (config.reducedMotion() != null) {
      MorphAnimator.setReducedMotion(config.reducedMotion());
    }
    repaintAllWindows();
  }

  private static void onEventDispatchThread(Runnable work) {
    if (SwingUtilities.isEventDispatchThread()) {
      work.run();
      return;
    }
    try {
      SwingUtilities.invokeAndWait(work);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while installing the theme", interrupted);
    } catch (InvocationTargetException failed) {
      Throwable cause = failed.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException("Failed to install the theme", cause);
    }
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

  // Step 7: register the fonts and write the Elwha.type.* keys, the global defaultFont, and the
  // FlatLaf-native font keys. Fonts are stored as FontUIResource for the same updateUI()
  // re-install reason as colors. defaultFont goes first: the ladder rungs FlatLafKeyMapping
  // deliberately leaves un-bridged are derived from it.
  private static void applyTypography(Typography typography) {
    for (TypeRole role : TypeRole.values()) {
      UIManager.put(role.uiKey(), new FontUIResource(typography.get(role)));
    }
    UIManager.put("defaultFont", new FontUIResource(typography.get(TypeRole.BODY_MEDIUM)));
    FlatLafKeyMapping.applyFontKeys(typography);
  }

  // Step 8: the only step that touches live components — they re-resolve tokens per the binding
  // rule and re-skin. Already on the EDT: the whole write sequence is dispatched there.
  private static void repaintAllWindows() {
    for (Window window : Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window);
    }
  }
}
