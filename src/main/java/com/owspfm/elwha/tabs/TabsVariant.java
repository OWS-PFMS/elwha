package com.owspfm.elwha.tabs;

/**
 * The two M3 tab-bar variants. The variant belongs to the {@link ElwhaTabs} bar — M3: "Use the same
 * type of tab in a tab bar" — and is stamped onto every {@link ElwhaTab} as it is added.
 *
 * <p><strong>{@link #PRIMARY}</strong> tabs are placed at the top of the content pane under a top
 * app bar and display the main content destinations. Their active indicator is 3&nbsp;px tall with
 * rounded top corners and hugs the tab's content width; active content renders in {@link
 * com.owspfm.elwha.theme.ColorRole#PRIMARY}.
 *
 * <p><strong>{@link #SECONDARY}</strong> tabs are used within a content area to further separate
 * related content and establish hierarchy. Their active indicator is 2&nbsp;px tall, square, and
 * spans the full tab width; active content renders in {@link
 * com.owspfm.elwha.theme.ColorRole#ON_SURFACE}.
 *
 * <p>Design: {@code docs/research/elwha-tabs-design.md} §3–§5; research: {@code
 * elwha-tabs-research.md} §T.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public enum TabsVariant {

  /**
   * Primary tabs — main content destinations; content-hugging 3&nbsp;px rounded indicator, active
   * content in {@code PRIMARY}.
   *
   * @since v0.4.0
   */
  PRIMARY,

  /**
   * Secondary tabs — hierarchy within a content area; full-width 2&nbsp;px square indicator, active
   * content in {@code ON_SURFACE}.
   *
   * @since v0.4.0
   */
  SECONDARY
}
