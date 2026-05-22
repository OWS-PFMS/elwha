package com.owspfm.elwha.buttongroup;

/**
 * The two M3 Expressive button-group variants — {@link #STANDARD} and {@link #CONNECTED}.
 *
 * <p>The variant sets how an {@link ElwhaButtonGroup} lays its segments out and treats their shape:
 * {@code STANDARD} keeps visible gaps and lets each segment render its own rounded outline; {@code
 * CONNECTED} butts the segments edge-to-edge with a 2&nbsp;dp inner padding and applies a shared
 * per-segment corner treatment so the row reads as one connected control.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public enum ButtonGroupVariant {

  /**
   * The gapped toolbar / action cluster — segments sit with a size-dependent visible gap and each
   * keeps its own rounded shape. New in M3 Expressive; no baseline equivalent.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  STANDARD,

  /**
   * The butted selection control — segments share edges with a 2&nbsp;dp inner padding, take a
   * shared shape with rounded outer corners and nearly-square inner corners, and grow to fill the
   * container width. Replaces M3's deprecated Segmented Button.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  CONNECTED
}
