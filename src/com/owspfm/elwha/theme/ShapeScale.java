package com.owspfm.elwha.theme;

import javax.swing.UIManager;

/**
 * The 7-step corner-radius scale of the Elwha token vocabulary, lifted from Material 3.
 *
 * <p>Each step resolves at paint time from {@link UIManager} under the key {@code
 * Elwha.shape.<key>}, which {@link ElwhaTheme#install} writes. The values are fixed in v1 —
 * they do not vary by theme or mode — but they are still resolved (not returned as compiled-in
 * constants) so that themeable geometry stays a cheap, component-free change later. See {@code
 * elwha-theme-install-api.md} §1.4.
 *
 * <p><strong>Binding rule.</strong> Callers must invoke {@link #px()} at paint time and must not
 * cache the result across paints.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ShapeScale {

  /**
   * Square — no rounding. Full-bleed surfaces.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  NONE("none", 0),
  /**
   * Subtle rounding (4px) — inputs and small affordances.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XS("xs", 4),
  /**
   * Small rounding (8px) — chips (the M3 chip spec value) and small buttons.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SM("sm", 8),
  /**
   * Medium rounding (12px) — the {@code ElwhaCard} default.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  MD("md", 12),
  /**
   * Large rounding (16px) — large cards and sheets.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LG("lg", 16),
  /**
   * Extra-large rounding (28px) — prominent containers and dialogs.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XL("xl", 28),
  /**
   * Fully rounded (9999px) — pill / capsule shapes and FABs.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FULL("full", 9999);

  /** The {@code UIManager} key namespace all shape steps resolve under. */
  private static final String KEY_PREFIX = "Elwha.shape.";

  private final String key;
  private final int defaultPx;

  ShapeScale(String key, int defaultPx) {
    this.key = key;
    this.defaultPx = defaultPx;
  }

  /**
   * Returns the {@code camelCase} token key for this step, e.g. {@code "md"}.
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
   * "Elwha.shape.md"}.
   *
   * @return the fully-qualified {@code UIManager} key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String uiKey() {
    return KEY_PREFIX + key;
  }

  /**
   * Resolves this step to a corner radius in pixels from {@link UIManager} at the moment of the
   * call.
   *
   * <p>If no theme has been installed (the {@code UIManager} key is absent or not an integer), this
   * degrades to the compiled-in v1 default rather than returning {@code 0}.
   *
   * @return the corner radius in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  public int px() {
    Object resolved = UIManager.get(uiKey());
    return resolved instanceof Integer ? (Integer) resolved : defaultPx;
  }

  /**
   * Returns the compiled-in v1 default radius for this step — the value {@link ElwhaTheme}
   * writes into {@link UIManager} at install time, and the value {@link #px()} falls back to when
   * no theme is installed.
   *
   * @return the default radius in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  int defaultPx() {
    return defaultPx;
  }
}
