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
}
