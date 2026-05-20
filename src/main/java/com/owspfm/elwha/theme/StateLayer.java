package com.owspfm.elwha.theme;

import java.awt.Color;
import javax.swing.UIManager;

/**
 * The 5 interaction-state layers of the Elwha token vocabulary, expressed the Material 3 way — as
 * <em>opacity overlays on a role color</em>, not as separate colors.
 *
 * <p>Each layer resolves its opacity at paint time from {@link UIManager} under the key {@code
 * Elwha.state.<key>}, which {@link ElwhaTheme#install} writes. The {@link #over} methods blend the
 * layer over a base color in sRGB — close enough to M3's perceptual compositing at these low
 * opacities without pulling in a color-science dependency.
 *
 * <p>Elwha's own components apply these overlays live at paint time. The raw-Swing bridge instead
 * bakes them into FlatLaf's discrete {@code *hoverBackground} / {@code *pressedBackground} keys at
 * install time — see {@code elwha-theme-install-api.md} §5.
 *
 * <p><strong>Disabled is not a state layer.</strong> M3 disabled is an opacity <em>treatment</em>
 * (content drops to 38%, container fill to 12%), applied as a compositing pass rather than a tinted
 * overlay. Those two constants live on this class as {@link #disabledContentOpacity()} / {@link
 * #disabledContainerOpacity()} but are deliberately outside the enum.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum StateLayer {

  /**
   * Hover overlay (8%) — pointer is over the component.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HOVER("hover", 0.08f),
  /**
   * Focus overlay (10%) — the component holds keyboard focus.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FOCUS("focus", 0.10f),
  /**
   * Pressed overlay (10%) — the component is being pressed.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  PRESSED("pressed", 0.10f),
  /**
   * Dragged overlay (16%) — the component is being dragged.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  DRAGGED("dragged", 0.16f),
  /**
   * Selected overlay (12%) — a Elwha-specific layer applied uniformly across chip and card lists.
   * M3 models selection as a container-color swap rather than an overlay; Elwha keeps one uniform
   * mechanism instead.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SELECTED("selected", 0.12f);

  /** The {@code UIManager} key namespace all state layers resolve under. */
  private static final String KEY_PREFIX = "Elwha.state.";

  /** The {@code UIManager} key for the disabled-content opacity treatment. */
  public static final String DISABLED_CONTENT_KEY = KEY_PREFIX + "disabledContent";

  /** The {@code UIManager} key for the disabled-container opacity treatment. */
  public static final String DISABLED_CONTAINER_KEY = KEY_PREFIX + "disabledContainer";

  /** The compiled-in v1 default for the disabled-content opacity treatment. */
  static final float DEFAULT_DISABLED_CONTENT = 0.38f;

  /** The compiled-in v1 default for the disabled-container opacity treatment. */
  static final float DEFAULT_DISABLED_CONTAINER = 0.12f;

  private final String key;
  private final float defaultOpacity;

  StateLayer(String key, float defaultOpacity) {
    this.key = key;
    this.defaultOpacity = defaultOpacity;
  }

  /**
   * Returns the {@code camelCase} token key for this layer, e.g. {@code "hover"}.
   *
   * @return the token key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String key() {
    return key;
  }

  /**
   * Returns the fully-qualified {@link UIManager} key this layer resolves under, e.g. {@code
   * "Elwha.state.hover"}.
   *
   * @return the fully-qualified {@code UIManager} key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String uiKey() {
    return KEY_PREFIX + key;
  }

  /**
   * Resolves this layer's opacity from {@link UIManager} at the moment of the call.
   *
   * <p>If no theme has been installed (the {@code UIManager} key is absent or not a number), this
   * degrades to the compiled-in M3 default.
   *
   * @return the overlay opacity in the range {@code [0, 1]}
   * @version v0.1.0
   * @since v0.1.0
   */
  public float opacity() {
    Object resolved = UIManager.get(uiKey());
    return resolved instanceof Number ? ((Number) resolved).floatValue() : defaultOpacity;
  }

  /**
   * Returns the compiled-in M3 default opacity for this layer — the value {@link ElwhaTheme} writes
   * into {@link UIManager} at install time, and the value {@link #opacity()} falls back to when no
   * theme is installed.
   *
   * @return the default opacity in the range {@code [0, 1]}
   * @version v0.1.0
   * @since v0.1.0
   */
  float defaultOpacity() {
    return defaultOpacity;
  }

  /**
   * Blends this layer over a base color, using {@code tintRole}'s resolved color as the overlay
   * tint.
   *
   * <p>Both the tint color and this layer's opacity are resolved at the moment of the call.
   *
   * @param base the color to overlay onto
   * @param tintRole the role supplying the overlay tint
   * @return the blended color
   * @version v0.1.0
   * @since v0.1.0
   */
  public Color over(Color base, ColorRole tintRole) {
    return over(base, tintRole.resolve());
  }

  /**
   * Blends this layer over a base color, using {@code tint} directly as the overlay color.
   *
   * <p>This layer's opacity is resolved at the moment of the call; the blend is a straight sRGB
   * interpolation. The returned color is fully opaque — the overlay opacity governs the mix, not
   * the result's alpha.
   *
   * @param base the color to overlay onto
   * @param tint the overlay color
   * @return the blended color, fully opaque
   * @version v0.1.0
   * @since v0.1.0
   */
  public Color over(Color base, Color tint) {
    float alpha = opacity();
    int r = Math.round(base.getRed() * (1f - alpha) + tint.getRed() * alpha);
    int g = Math.round(base.getGreen() * (1f - alpha) + tint.getGreen() * alpha);
    int b = Math.round(base.getBlue() * (1f - alpha) + tint.getBlue() * alpha);
    return new Color(clamp(r), clamp(g), clamp(b));
  }

  private static int clamp(int channel) {
    return Math.max(0, Math.min(255, channel));
  }

  /**
   * Resolves the disabled-content opacity treatment from {@link UIManager} — the factor M3 applies
   * to foreground content on a disabled component (default {@code 0.38}).
   *
   * @return the disabled-content opacity factor
   * @version v0.1.0
   * @since v0.1.0
   */
  public static float disabledContentOpacity() {
    Object resolved = UIManager.get(DISABLED_CONTENT_KEY);
    return resolved instanceof Number ? ((Number) resolved).floatValue() : DEFAULT_DISABLED_CONTENT;
  }

  /**
   * Resolves the disabled-container opacity treatment from {@link UIManager} — the factor M3
   * applies to the container fill of a disabled component (default {@code 0.12}).
   *
   * @return the disabled-container opacity factor
   * @version v0.1.0
   * @since v0.1.0
   */
  public static float disabledContainerOpacity() {
    Object resolved = UIManager.get(DISABLED_CONTAINER_KEY);
    return resolved instanceof Number
        ? ((Number) resolved).floatValue()
        : DEFAULT_DISABLED_CONTAINER;
  }
}
