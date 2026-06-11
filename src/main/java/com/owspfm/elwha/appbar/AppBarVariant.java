package com.owspfm.elwha.appbar;

/**
 * The three M3 Expressive app-bar variants. {@code SMALL} is the single-row regular bar; the two
 * flexible variants add the taller expanded headline region and collapse back to the small bar's
 * 64&nbsp;px strip when scrolled. The deprecated baseline Medium/Large variants are not shipped,
 * and center-alignment is an option ({@link ElwhaAppBar#setTitleCentered(boolean)}), not a variant
 * (research {@code elwha-appbar-research.md} §A/§E).
 *
 * <p>Expanded heights are the verbatim v14.0.0 token values: the no-subtitle {@code
 * ContainerHeight} and the with-subtitle {@code LargeContainerHeight} pair per variant.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public enum AppBarVariant {

  /**
   * The single-row regular bar — 64&nbsp;px, title {@code TITLE_LARGE} — for dense layouts or
   * scrolled pages.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  SMALL(64, 64),

  /**
   * The medium collapsing bar — expands to 112&nbsp;px (136 with a subtitle) to display a larger
   * headline; collapses to the small bar on scroll.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  MEDIUM_FLEXIBLE(112, 136),

  /**
   * The large collapsing bar — expands to 120&nbsp;px (152 with a subtitle) to emphasize the page
   * headline; collapses to the small bar on scroll.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  LARGE_FLEXIBLE(120, 152);

  private final int expandedHeightPx;
  private final int expandedWithSubtitleHeightPx;

  AppBarVariant(final int expandedHeightPx, final int expandedWithSubtitleHeightPx) {
    this.expandedHeightPx = expandedHeightPx;
    this.expandedWithSubtitleHeightPx = expandedWithSubtitleHeightPx;
  }

  /**
   * The fully-expanded container height for this variant.
   *
   * @param withSubtitle whether the bar currently shows a subtitle (flexible variants are taller
   *     with one; {@code SMALL} is 64 either way)
   * @return the expanded height in px
   * @version v0.4.0
   * @since v0.4.0
   */
  public int expandedHeightPx(final boolean withSubtitle) {
    return withSubtitle ? expandedWithSubtitleHeightPx : expandedHeightPx;
  }

  /**
   * Whether this is a collapsing (two-row) variant.
   *
   * @return {@code true} for the flexible variants
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isFlexible() {
    return this != SMALL;
  }
}
