package com.owspfm.elwha.button;

import com.owspfm.elwha.theme.TypeRole;

/**
 * The five M3 Expressive button-size tiers — {@link #XS}, {@link #S} (default), {@link #M}, {@link
 * #L}, {@link #XL}. Each tier carries its per-spec measurement set: container height, padding (both
 * with and without a leading icon), icon size, square-corner radius, and the WCAG-driven minimum
 * touch-target size.
 *
 * <p>All values are sourced from {@code docs/research/elwha-button-design.md} Appendix A. The 48 dp
 * minimum-target inflation for {@link #XS} and {@link #S} (WCAG 2.5.5 / M3 a11y guideline) is
 * exposed via {@link #minimumTargetPx()}; both {@link ElwhaButton#getPreferredSize()} and {@link
 * ElwhaButton#getMinimumSize()} report this on the cross axis so the click hit area matches the
 * touch target. The visible chrome stays at the spec-mandated 32 / 40 dp, centered inside the
 * inflated component bounds.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ButtonSize {

  /**
   * Extra-small — 32 dp container, 20 dp icon, 12 dp square corner. The cross-axis layout target is
   * inflated to 48 dp by {@link #minimumTargetPx()}. Label type role: {@link TypeRole#LABEL_LARGE}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  XS(32, 12, 12, 4, 12, 20, 12, 48, TypeRole.LABEL_LARGE),

  /**
   * Small — 40 dp container, 20 dp icon, 12 dp square corner. The default size; matches Base M3's
   * single common-button size. The cross-axis layout target is inflated to 48 dp by {@link
   * #minimumTargetPx()}. Label type role: {@link TypeRole#LABEL_LARGE}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  S(40, 16, 16, 8, 16, 20, 12, 48, TypeRole.LABEL_LARGE),

  /**
   * Medium — 56 dp container, 24 dp icon, 16 dp square corner. The cross-axis layout target is the
   * container height (already above the 48 dp minimum). Label type role: {@link
   * TypeRole#TITLE_MEDIUM}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  M(56, 24, 24, 8, 24, 24, 16, 56, TypeRole.TITLE_MEDIUM),

  /**
   * Large — 96 dp container, 32 dp icon, 28 dp square corner. The cross-axis layout target is the
   * container height. Label type role: {@link TypeRole#HEADLINE_SMALL} — M3 Expressive scales the
   * label dramatically at L to fill the larger chrome.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  L(96, 48, 48, 12, 48, 32, 28, 96, TypeRole.HEADLINE_SMALL),

  /**
   * Extra-large — 136 dp container, 40 dp icon, 28 dp square corner. The cross-axis layout target
   * is the container height. Label type role: {@link TypeRole#HEADLINE_LARGE} — XL is the
   * marquee-style M3 button; label fills the chrome.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  XL(136, 64, 64, 16, 64, 40, 28, 136, TypeRole.HEADLINE_LARGE);

  private final int containerHeightPx;
  private final int paddingNoIconPx;
  private final int paddingWithIconLeadingPx;
  private final int paddingWithIconGapPx;
  private final int paddingWithIconTrailingPx;
  private final int iconSizePx;
  private final int squareCornerPx;
  private final int minimumTargetPx;
  private final TypeRole typeRole;

  ButtonSize(
      final int containerHeightPx,
      final int paddingNoIconPx,
      final int paddingWithIconLeadingPx,
      final int paddingWithIconGapPx,
      final int paddingWithIconTrailingPx,
      final int iconSizePx,
      final int squareCornerPx,
      final int minimumTargetPx,
      final TypeRole typeRole) {
    this.containerHeightPx = containerHeightPx;
    this.paddingNoIconPx = paddingNoIconPx;
    this.paddingWithIconLeadingPx = paddingWithIconLeadingPx;
    this.paddingWithIconGapPx = paddingWithIconGapPx;
    this.paddingWithIconTrailingPx = paddingWithIconTrailingPx;
    this.iconSizePx = iconSizePx;
    this.squareCornerPx = squareCornerPx;
    this.minimumTargetPx = minimumTargetPx;
    this.typeRole = typeRole;
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
   * a11y guideline; the container height for the larger sizes (already above 48 dp). The {@link
   * ElwhaButton} component grows on the cross axis to this value (so click dispatch covers the full
   * touch target), with the visible body centered inside.
   *
   * @return the minimum target size
   * @version v0.2.0
   * @since v0.2.0
   */
  public int minimumTargetPx() {
    return minimumTargetPx;
  }

  /**
   * The M3 type role used for the label at this size — labelLarge for {@link #XS} / {@link #S},
   * titleMedium for {@link #M}, headlineSmall for {@link #L}, headlineLarge for {@link #XL}.
   * Matches M3 Expressive's scaling-type behavior across the size axis (the label fills more of the
   * chrome at larger sizes rather than sitting as a small caption inside a large container).
   *
   * @return the type role for this size's label (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public TypeRole typeRole() {
    return typeRole;
  }
}
