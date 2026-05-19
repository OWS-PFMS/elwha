package com.owspfm.elwha.card;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import java.util.Objects;

/**
 * The card-chevron disclosure affordance — a Layer 4 icon button bound to an {@link ElwhaCard}'s
 * collapsed state. Glyph swaps between {@link MaterialIcons#expandMore()} (collapsed) and {@link
 * MaterialIcons#expandLess()} (expanded); click toggles the card's collapse.
 *
 * <p>Default size {@link IconButtonSize#S} (32 dp) — sized to fit alongside header text without
 * dominating the row. Inherits all {@link ElwhaIconButton} chrome (state layer, focus ring,
 * keyboard activation, accessibility).
 *
 * <p>Typical placement: as a trailing header affordance.
 *
 * <pre>{@code
 * card.setCollapsible(true);
 * header.addTrailing(new ElwhaCardChevron(card));
 * }</pre>
 *
 * <p><strong>Auto-anchors its host container as ALWAYS_VISIBLE.</strong> When the chevron is added
 * to the component tree, it walks up to find its direct-child-of-card ancestor (typically the
 * {@link ElwhaCardHeader} it sits in) and calls {@code card.setCollapseConstraint(host,
 * CollapseRule.ALWAYS_VISIBLE)} so the chevron survives a {@code card.setCollapsed(true)} — and the
 * user can therefore expand the card again. Without this self-anchor, every consumer would have to
 * remember the rule, and forgetting silently strands the collapsed card with no affordance to
 * expand (#23 footgun). Consumers who want the chevron's host to hide on collapse anyway can
 * override after adding: {@code card.setCollapseConstraint(host, CollapseRule.COLLAPSIBLE)}.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §6.1.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardChevron extends ElwhaIconButton {

  private final ElwhaCard card;

  /**
   * Creates a chevron bound to the given card. Registers an expansion-change listener and a click
   * listener that toggles the card's collapsed state.
   *
   * @param card the card to drive (must not be {@code null})
   * @throws NullPointerException if {@code card} is {@code null}
   */
  public ElwhaCardChevron(final ElwhaCard card) {
    super(MaterialIcons.expandMore());
    this.card = Objects.requireNonNull(card, "card");
    setButtonSize(IconButtonSize.S);
    syncGlyph();
    addActionListener(e -> card.setCollapsed(!card.isCollapsed()));
    card.addExpansionChangeListener(e -> syncGlyph());
  }

  /**
   * @return the card this chevron drives
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard getCard() {
    return card;
  }

  private void syncGlyph() {
    setIcon(card.isCollapsed() ? MaterialIcons.expandMore() : MaterialIcons.expandLess());
  }

  /**
   * Self-anchors the chevron's host container as {@link CollapseRule#ALWAYS_VISIBLE} on the driven
   * card the first time the chevron is added to the component tree underneath that card. See class
   * Javadoc for rationale (the #23 footgun). Defensive: silently does nothing if the chevron is
   * added outside the card's subtree (consumer error; chevron just doesn't drive survival).
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public void addNotify() {
    super.addNotify();
    // Walk up to find the direct child of `card` that contains us — anchor THAT child as
    // ALWAYS_VISIBLE. If we're directly added to the card (uncommon — chevron usually lives in
    // a header), `cursor` starts at `this` and the loop exits immediately, anchoring this. If
    // we're inside a header / panel / etc., cursor climbs until its parent is the card.
    java.awt.Component cursor = this;
    while (cursor != null && cursor.getParent() != card) {
      cursor = cursor.getParent();
    }
    if (cursor != null) {
      card.setCollapseConstraint(cursor, CollapseRule.ALWAYS_VISIBLE);
    }
  }
}
