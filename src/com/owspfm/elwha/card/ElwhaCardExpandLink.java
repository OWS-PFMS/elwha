package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

/**
 * The card-expand-link disclosure affordance — a Layer 4 text link bound to an {@link ElwhaCard}'s
 * collapsed state. Text swaps between the supplied {@code expandText} (collapsed) and {@code
 * collapseText} (expanded); click / Enter / Space toggles the card's collapse.
 *
 * <p>M3 placement: card body bottom, after a full-width divider. Colored {@link ColorRole#PRIMARY}
 * by default and rendered underlined to read as a link.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §6.2.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardExpandLink extends JLabel {

  private final ElwhaCard card;
  private final String expandText;
  private final String collapseText;
  private ColorRole colorRole = ColorRole.PRIMARY;

  /**
   * Creates an expand-link bound to the given card. Registers an expansion-change listener and
   * mouse / keyboard activation toggling the card's collapsed state.
   *
   * @param card the card to drive (must not be {@code null})
   * @param expandText the label text shown when the card is collapsed
   * @param collapseText the label text shown when the card is expanded
   * @throws NullPointerException if any argument is {@code null}
   */
  public ElwhaCardExpandLink(
      final ElwhaCard card, final String expandText, final String collapseText) {
    this.card = Objects.requireNonNull(card, "card");
    this.expandText = Objects.requireNonNull(expandText, "expandText");
    this.collapseText = Objects.requireNonNull(collapseText, "collapseText");
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setFocusable(true);
    setText(card.isCollapsed() ? expandText : collapseText);
    card.addExpansionChangeListener(e -> syncText());
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent ev) {
            toggle();
          }
        });
    installKeyboardActivation();
  }

  /**
   * @return the card this link drives
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard getCard() {
    return card;
  }

  /**
   * @return the text shown when the card is collapsed
   * @version v0.2.0
   * @since v0.2.0
   */
  public String getExpandText() {
    return expandText;
  }

  /**
   * @return the text shown when the card is expanded
   * @version v0.2.0
   * @since v0.2.0
   */
  public String getCollapseText() {
    return collapseText;
  }

  /**
   * Sets the link color role. Defaults to {@link ColorRole#PRIMARY}.
   *
   * @param role the color role (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardExpandLink setColorRole(final ColorRole role) {
    this.colorRole = Objects.requireNonNull(role, "colorRole");
    repaint();
    return this;
  }

  /**
   * @return the active color role
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole getColorRole() {
    return colorRole;
  }

  @Override
  public Color getForeground() {
    return colorRole != null ? colorRole.resolve() : super.getForeground();
  }

  @Override
  public Font getFont() {
    final Font base = TypeRole.LABEL_LARGE.resolve();
    // Underline attribute for link affordance.
    return base.deriveFont(
        java.util.Map.of(
            java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON));
  }

  private void syncText() {
    setText(card.isCollapsed() ? expandText : collapseText);
  }

  private void toggle() {
    card.setCollapsed(!card.isCollapsed());
  }

  private void installKeyboardActivation() {
    final InputMap im = getInputMap(JComponent.WHEN_FOCUSED);
    final ActionMap am = getActionMap();
    im.put(KeyStroke.getKeyStroke("released SPACE"), "toggle");
    im.put(KeyStroke.getKeyStroke("released ENTER"), "toggle");
    am.put(
        "toggle",
        new AbstractAction() {
          @Override
          public void actionPerformed(final java.awt.event.ActionEvent e) {
            toggle();
          }
        });
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext =
          new AccessibleJLabel() {
            @Override
            public AccessibleRole getAccessibleRole() {
              return AccessibleRole.HYPERLINK;
            }
          };
    }
    return accessibleContext;
  }
}
