package com.owspfm.elwha.card;

import java.awt.Component;

/**
 * Per-child visibility rule for {@link ElwhaCard}'s collapse model. Assigned via {@link
 * ElwhaCard#setCollapseConstraint(Component, CollapseRule)}; defaults to {@link #COLLAPSIBLE} for
 * any child without an explicit rule. See {@code docs/research/elwha-card-v3-spec.md} §14.2.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum CollapseRule {
  /** Always rendered, even when the card is collapsed (e.g. the persistent header). */
  ALWAYS_VISIBLE,

  /** Hidden when the card is collapsed; shown when expanded. The default for added children. */
  COLLAPSIBLE
}
