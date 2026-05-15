package com.owspfm.elwha.chip;

/**
 * Surface-style variants for {@link ElwhaChip}.
 *
 * <p>The variant is ergonomic sugar over the underlying three-layer styling system: each variant
 * resolves to a curated set of {@link javax.swing.UIManager} keys (background, border color, arc,
 * padding). Callers who need per-instance tweaks can still override via the {@code
 * "ElwhaChip.style"} client property without leaving the variant API.
 *
 * <p>The variant controls only the background and border treatment; it does not affect layout,
 * spacing, or interaction behavior — those are governed by separate setters on {@link ElwhaChip}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ChipVariant {

  /**
   * Filled background tinted from the panel surface. The workhorse default for chip / tag rows that
   * need to read as a distinct cluster against the surrounding surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FILLED,

  /**
   * Hairline border with a transparent fill. Best for dense rows where multiple FILLED chips would
   * crowd the visual field, or when the chip needs to look "lighter" than nearby surfaces.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINED,

  /**
   * No fill, no border. Renders as text-with-padding until hovered; the surface only appears on
   * hover/press/selected. Useful for tab-strip uses where the unselected chips should disappear
   * into the surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  GHOST,

  /**
   * Tinted with the application's warm accent (gold/amber range). Reserved for emphasizing a small
   * subset of chips — e.g., favorited factors, "you are here" view tabs.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  WARM_ACCENT
}
