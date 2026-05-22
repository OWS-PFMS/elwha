package com.owspfm.elwha.buttongroup;

/**
 * How an {@link ElwhaButtonGroup} resolves its width — {@link #FIXED} (hug content) or {@link
 * #FLEXIBLE} (auto-grow).
 *
 * <p>The mode only changes the {@link ButtonGroupVariant#CONNECTED} variant: a connected group is
 * {@code FLEXIBLE} by default and fills its container width with uniform-width segments, or {@code
 * FIXED} and hugs the widest segment's content. The {@link ButtonGroupVariant#STANDARD} variant
 * always hugs its content regardless of this mode.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public enum ResizeMode {

  /**
   * The group hugs its content — segment widths come from each segment's own preferred size.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  FIXED,

  /**
   * The group auto-grows — a connected group fills the container width (optionally clamped by
   * {@link ElwhaButtonGroup#setMaxWidth(int)}), dividing it into uniform-width segments.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  FLEXIBLE
}
