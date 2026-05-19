package com.owspfm.elwha.button;

/**
 * The five M3 Expressive button-size tiers — {@link #XS}, {@link #S} (default), {@link #M}, {@link
 * #L}, {@link #XL}. Each tier carries its per-spec measurement set: container height, padding (both
 * with and without a leading icon), icon size, square-corner radius, and the WCAG-driven minimum
 * touch-target size.
 *
 * <p>All values are sourced from {@code docs/research/elwha-button-design.md} Appendix A. The 48 dp
 * minimum-target inflation for {@link #XS} and {@link #S} (WCAG 2.5.5 / M3 a11y guideline) is
 * exposed via {@link #minimumTargetPx()}; consumers see this in {@link
 * ElwhaButton#getMinimumSize()} but not in {@link ElwhaButton#getPreferredSize()} — the visible
 * body stays at the spec-mandated 32 / 40 dp and only the layout-known cross-axis grows.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ButtonSize {

  /**
   * Extra-small — 32 dp container, 20 dp icon, 12 dp square corner. The cross-axis layout target is
   * inflated to 48 dp by {@link #minimumTargetPx()}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  XS(32, 12, 12, 4, 12, 20, 12, 48),

  /**
   * Small — 40 dp container, 20 dp icon, 12 dp square corner. The default size; matches Base M3's
   * single common-button size. The cross-axis layout target is inflated to 48 dp by {@link
   * #minimumTargetPx()}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  S(40, 16, 16, 8, 16, 20, 12, 48),

  /**
   * Medium — 56 dp container, 24 dp icon, 16 dp square corner. The cross-axis layout target is the
   * container height (already above the 48 dp minimum).
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  M(56, 24, 24, 8, 24, 24, 16, 56),

  /**
   * Large — 96 dp container, 32 dp icon, 28 dp square corner. The cross-axis layout target is the
   * container height.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  L(96, 48, 48, 12, 48, 32, 28, 96),

  /**
   * Extra-large — 136 dp container, 40 dp icon, 28 dp square corner. The cross-axis layout target
   * is the container height.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  XL(136, 64, 64, 16, 64, 40, 28, 136);

  private final int containerHeightPx;
  private final int paddingNoIconPx;
  private final int paddingWithIconLeadingPx;
  private final int paddingWithIconGapPx;
  private final int paddingWithIconTrailingPx;
  private final int iconSizePx;
  private final int squareCornerPx;
  private final int minimumTargetPx;

  ButtonSize(
      final int containerHeightPx,
      final int paddingNoIconPx,
      final int paddingWithIconLeadingPx,
      final int paddingWithIconGapPx,
      final int paddingWithIconTrailingPx,
      final int iconSizePx,
      final int squareCornerPx,
      final int minimumTargetPx) {
    this.containerHeightPx = containerHeightPx;
    this.paddingNoIconPx = paddingNoIconPx;
    this.paddingWithIconLeadingPx = paddingWithIconLeadingPx;
    this.paddingWithIconGapPx = paddingWithIconGapPx;
    this.paddingWithIconTrailingPx = paddingWithIconTrailingPx;
    this.iconSizePx = iconSizePx;
    this.squareCornerPx = squareCornerPx;
    this.minimumTargetPx = minimumTargetPx;
  }

  /**
   * Container height in pixels — the visible body height per Appendix A.
   *
   * @return the container height
   * @version v0.2.0
   * @since v0.2.0
   */
  public int containerHeightPx() {
    return containerHeightPx;
  }

  /**
   * Horizontal padding (each side) when the button has no leading icon.
   *
   * @return the leading + trailing padding (symmetric)
   * @version v0.2.0
   * @since v0.2.0
   */
  public int paddingNoIconPx() {
    return paddingNoIconPx;
  }

  /**
   * Leading padding (left of the icon) when the button has a leading icon.
   *
   * @return the leading padding
   * @version v0.2.0
   * @since v0.2.0
   */
  public int paddingWithIconLeadingPx() {
    return paddingWithIconLeadingPx;
  }

  /**
   * Gap between the leading icon and the label.
   *
   * @return the icon→label gap
   * @version v0.2.0
   * @since v0.2.0
   */
  public int paddingWithIconGapPx() {
    return paddingWithIconGapPx;
  }

  /**
   * Trailing padding (right of the label) when the button has a leading icon.
   *
   * @return the trailing padding
   * @version v0.2.0
   * @since v0.2.0
   */
  public int paddingWithIconTrailingPx() {
    return paddingWithIconTrailingPx;
  }

  /**
   * Recommended icon size in pixels — the layout slot the button reserves for the leading icon.
   * Consumers should pass an icon rendered at this size; smaller icons paint at their intrinsic
   * size inside the slot but won't fill it. The button doesn't resize consumer-passed icons.
   *
   * @return the icon size
   * @version v0.2.0
   * @since v0.2.0
   */
  public int iconSizePx() {
    return iconSizePx;
  }

  /**
   * Square-shape corner radius in pixels for this size — passed as {@code arcWidth} (round-rect
   * full corner diameter) to the paint pipeline.
   *
   * @return the square corner arcWidth
   * @version v0.2.0
   * @since v0.2.0
   */
  public int squareCornerPx() {
    return squareCornerPx;
  }

  /**
   * Minimum touch-target size in pixels — 48 dp for {@link #XS} and {@link #S} per WCAG 2.5.5 / M3
   * a11y guideline; the container height for the larger sizes (already above 48 dp). {@link
   * ElwhaButton#getMinimumSize()} reports this on the cross axis; the visible body stays at {@link
   * #containerHeightPx()}.
   *
   * @return the minimum target size
   * @version v0.2.0
   * @since v0.2.0
   */
  public int minimumTargetPx() {
    return minimumTargetPx;
  }
}
