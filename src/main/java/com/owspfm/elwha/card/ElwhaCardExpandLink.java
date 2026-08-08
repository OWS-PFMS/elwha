package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
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
 * <p><strong>Auto-anchors itself as ALWAYS_VISIBLE.</strong> When the link is added to the
 * component tree, it walks up to find its direct-child-of-card ancestor and calls {@code
 * card.setCollapseConstraint(host, CollapseRule.ALWAYS_VISIBLE)} so the link survives a {@code
 * card.setCollapsed(true)} — otherwise the link disappears with the rest of the body and the user
 * has no way to re-expand the card. Same self-anchor rule as {@link ElwhaCardChevron}; consumers
 * can override after construction by re-setting the constraint to COLLAPSIBLE.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §6.2.
 *
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.2.0
 */
public final class ElwhaCardExpandLink extends JLabel {

  private final ElwhaCard card;
  private final String expandText;
  private final String collapseText;
  private ColorRole colorRole = ColorRole.PRIMARY;

  /** Held as a field, not a bare lambda, so {@link #removeNotify()} can unregister it. */
  private final PropertyChangeListener expansionSync = e -> syncText();

  private boolean subscribed;

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
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent ev) {
            toggle();
          }
        });
    installKeyboardActivation();
    subscribe();
  }

  // Idempotent so the constructor and addNotify can both ask for it: a link tracks its card from
  // birth (an unparented one still reports the right text), and re-add after a detach has to
  // restore the subscription without stacking a second copy.
  private void subscribe() {
    if (!subscribed) {
      card.addExpansionChangeListener(expansionSync);
      subscribed = true;
    }
  }

  private void unsubscribe() {
    if (subscribed) {
      card.removeExpansionChangeListener(expansionSync);
      subscribed = false;
    }
  }

  /**
   * Returns the card this link drives.
   *
   * @return the card this link drives
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard getCard() {
    return card;
  }

  /**
   * Returns the text shown when the card is collapsed.
   *
   * @return the text shown when the card is collapsed
   * @version v0.2.0
   * @since v0.2.0
   */
  public String getExpandText() {
    return expandText;
  }

  /**
   * Returns the text shown when the card is expanded.
   *
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
   * Returns the active color role.
   *
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

  /**
   * Self-anchors the link's host container as {@link CollapseRule#ALWAYS_VISIBLE} on the driven
   * card the first time the link is added to the component tree underneath that card. See class
   * Javadoc for rationale (the #23 footgun applies to ExpandLink for the same reason as
   * ElwhaCardChevron). Defensive: silently does nothing if the link is added outside the card's
   * subtree.
   *
   * <p>Also subscribes the label text to the card's expansion state and resyncs it, since the card
   * can have been toggled while the link was detached.
   *
   * @version v0.5.0
   * @since v0.2.0
   */
  @Override
  public void addNotify() {
    super.addNotify();
    subscribe();
    syncText();
    // Walk up to find the direct child of `card` that contains us — anchor THAT child as
    // ALWAYS_VISIBLE. ExpandLink is typically added directly to the card (M3 placement: body
    // bottom, after divider), so cursor starts at `this` and the loop usually exits immediately
    // anchoring this. Same defensive fallthrough as ElwhaCardChevron — if the link is added
    // outside the card's subtree, no anchor.
    java.awt.Component cursor = this;
    while (cursor != null && cursor.getParent() != card) {
      cursor = cursor.getParent();
    }
    if (cursor != null) {
      card.setCollapseConstraint(cursor, CollapseRule.ALWAYS_VISIBLE);
    }
  }

  /**
   * Unsubscribes from the card's expansion state. The card's {@code PropertyChangeSupport} holds
   * its listeners strongly, so a link swapped out of a card's body would otherwise stay alive — and
   * keep reacting — for the card's whole lifetime.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public void removeNotify() {
    unsubscribe();
    super.removeNotify();
  }
}
