package com.owspfm.elwha.theme;

import java.awt.Insets;
import javax.swing.UIManager;

/**
 * The 6-step spacing scale of the Elwha token vocabulary — a 4px-based ladder for paddings,
 * gaps, and insets.
 *
 * <p>Material 3 has no formal spacing <em>token</em> scale (it uses an informal 4px/8px grid), so
 * this is Elwha's own ladder on that grid. Each step resolves at paint time from {@link
 * UIManager} under the key {@code Elwha.space.<key>}, which {@link ElwhaTheme#install}
 * writes. As with {@link ShapeScale}, the v1 values are fixed but still resolved — see {@code
 * elwha-theme-install-api.md} §1.4.
 *
 * <p><strong>Binding rule.</strong> Callers must invoke {@link #px()} / {@link #insets()} at paint
 * time and must not cache the result across paints.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum SpaceScale {

  /**
   * Tight gaps (4px) — icon-to-label spacing inside a chip.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XS("xs", 4),
  /**
   * Default intra-component padding (8px).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SM("sm", 8),
  /**
   * Chip padding and compact card padding (12px).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  MD("md", 12),
  /**
   * Default card padding and comfortable gaps (16px).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LG("lg", 16),
  /**
   * Section gaps within a panel (24px).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XL("xl", 24),
  /**
   * Major section separation (32px).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XXL("xxl", 32);

  /** The {@code UIManager} key namespace all spacing steps resolve under. */
  private static final String KEY_PREFIX = "Elwha.space.";

  private final String key;
  private final int defaultPx;

  SpaceScale(String key, int defaultPx) {
    this.key = key;
    this.defaultPx = defaultPx;
  }

  /**
   * Returns the {@code camelCase} token key for this step, e.g. {@code "lg"}.
   *
   * @return the token key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String key() {
    return key;
  }

  /**
   * Returns the fully-qualified {@link UIManager} key this step resolves under, e.g. {@code
   * "Elwha.space.lg"}.
   *
   * @return the fully-qualified {@code UIManager} key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String uiKey() {
    return KEY_PREFIX + key;
  }

  /**
   * Resolves this step to a length in pixels from {@link UIManager} at the moment of the call.
   *
   * <p>If no theme has been installed (the {@code UIManager} key is absent or not an integer), this
   * degrades to the compiled-in v1 default rather than returning {@code 0}.
   *
   * @return the spacing length in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  public int px() {
    Object resolved = UIManager.get(uiKey());
    return resolved instanceof Integer ? (Integer) resolved : defaultPx;
  }

  /**
   * Resolves this step to a uniform {@link Insets} — the same length on all four sides.
   *
   * @return a uniform {@code Insets} at this step's resolved length
   * @version v0.1.0
   * @since v0.1.0
   */
  public Insets insets() {
    int length = px();
    return new Insets(length, length, length, length);
  }

  /**
   * Resolves a symmetric {@link Insets} from two steps — {@code vertical} on the top and bottom,
   * {@code horizontal} on the left and right.
   *
   * @param vertical the step for the top and bottom insets
   * @param horizontal the step for the left and right insets
   * @return a symmetric {@code Insets} at the two resolved lengths
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Insets insets(SpaceScale vertical, SpaceScale horizontal) {
    int v = vertical.px();
    int h = horizontal.px();
    return new Insets(v, h, v, h);
  }

  /**
   * Returns the compiled-in v1 default length for this step — the value {@link ElwhaTheme}
   * writes into {@link UIManager} at install time, and the value {@link #px()} falls back to when
   * no theme is installed.
   *
   * @return the default length in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  int defaultPx() {
    return defaultPx;
  }
}
