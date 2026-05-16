package com.owspfm.elwha.card;

/**
 * Policy for when an {@link ElwhaCard}'s summary slot is rendered, relative to the disclosure
 * (collapsed / expanded) state.
 *
 * <p>The summary is the compact band installed via {@link
 * ElwhaCard#setSummary(javax.swing.JComponent)}. In the M3-standard {@link #COLLAPSED_ONLY} mode
 * the summary stands in for the body while the card is collapsed and disappears on expansion; in
 * the {@link #ALWAYS} mode the summary is a persistent header band that remains visible in both
 * states. Replaces V1's {@code setKeepSummaryWhenExpanded(boolean)} escape hatch, which modeled
 * "always" as a boolean override on top of an unstated default.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum SummaryVisibility {

  /**
   * Summary renders only while the card is collapsed; expanding the card hides the summary and
   * reveals the body slots. The default.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  COLLAPSED_ONLY,

  /**
   * Summary renders in both collapsed and expanded states — visible as a persistent header band
   * regardless of disclosure. Use when the summary surfaces metrics or affordances the user wants
   * always-reachable.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ALWAYS
}
