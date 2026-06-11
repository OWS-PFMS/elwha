package com.owspfm.elwha.tabs;

/**
 * The two M3 tab-bar layout modes (MDC {@code tabMode}).
 *
 * <p><strong>{@link #FIXED}</strong> tabs display all tabs on one screen: every tab gets an equal
 * share of the bar's width. The visible tab set represents the only tabs available.
 *
 * <p><strong>{@link #SCROLLABLE}</strong> tabs are displayed at their content widths (clamped to
 * the M3 72–264&nbsp;px range) without fixed widths; tabs overflow the bar and scroll — mouse
 * wheel, {@link ElwhaTabs#scrollToTab(ElwhaTab)}, and automatic scroll-on-activation keep the
 * active tab in view with a 48&nbsp;px margin.
 *
 * <p>Design: {@code docs/research/elwha-tabs-design.md} §5/§7; research: {@code
 * elwha-tabs-research.md} §C/§I.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public enum TabMode {

  /**
   * Equal-width tabs filling the bar.
   *
   * @since v0.4.0
   */
  FIXED,

  /**
   * Content-width tabs (clamped 72–264&nbsp;px) in a scrolling strip.
   *
   * @since v0.4.0
   */
  SCROLLABLE
}
