package com.owspfm.elwha.menu;

/**
 * The color scheme of an {@link ElwhaMenu} and its items (M3 menu "Color", research §K). Mirrors
 * M3's exact nouns per the terminology lock (design §P).
 *
 * <p>{@link #STANDARD} is the surface-based default; {@link #VIBRANT} is the higher-emphasis,
 * tertiary-tinted scheme ("more prominent, use sparingly"). Both use the same color roles in light
 * and dark, so dark mode is free via {@code ElwhaTheme}.
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
  STANDARD,

  /**
   * Tertiary-based, higher-emphasis scheme (research §K): the whole surface tints {@code
   * TERTIARY_CONTAINER} with {@code ON_TERTIARY_CONTAINER} content (label / icons / trailing /
   * supporting / state layer), and the selected item jumps to the bold {@code TERTIARY} fill with
   * {@code ON_TERTIARY} content (including the ✓ checkmark). Selection therefore stands out by
   * moving from container-tone to full-tone within an already-tinted menu. Use sparingly.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  VIBRANT
}
