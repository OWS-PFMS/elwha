package com.owspfm.elwha.menu;

/**
 * The color scheme of an {@link ElwhaMenu} and its items (M3 menu "Color", research §K). Mirrors
 * M3's exact nouns per the terminology lock (design §P).
 *
 * <p><strong>Phase 1 ships {@link #STANDARD} only.</strong> The higher-emphasis {@code VIBRANT}
 * (tertiary-tinted surface) is a later phase (design §12 S5); the constant is added when that phase
 * lands. Adding an enum constant is source- and binary-compatible pre-1.0.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum ColorStyle {

  /**
   * Surface-based, lower-emphasis scheme (the default): container {@code SURFACE_CONTAINER_LOW},
   * label {@code ON_SURFACE}, icons/trailing/supporting {@code ON_SURFACE_VARIANT}, selected item
   * {@code TERTIARY_CONTAINER} / {@code ON_TERTIARY_CONTAINER}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  STANDARD
}
