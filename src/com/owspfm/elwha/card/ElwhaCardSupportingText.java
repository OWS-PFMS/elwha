package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * The card-supporting-text atom — a Layer 2 typed label for the body slot of a card. Defaults to
 * {@link TypeRole#BODY_MEDIUM} type and {@link ColorRole#ON_SURFACE_VARIANT} color, start-aligned,
 * HTML auto-wrapped. Multi-line content uses {@code <br>} or natural word wrap.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §4.3.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardSupportingText extends JLabel {

  private TypeRole typeRole = TypeRole.BODY_MEDIUM;
  private ColorRole colorRole = ColorRole.ON_SURFACE_VARIANT;

  /** Creates an empty supporting-text label. */
  public ElwhaCardSupportingText() {
    this("");
  }

  /**
   * Creates a supporting-text label with the given text.
   *
   * @param text the text (may be {@code null} or empty)
   */
  public ElwhaCardSupportingText(final String text) {
    super(text == null ? "" : text);
    setHorizontalAlignment(SwingConstants.LEADING);
    setVerticalAlignment(SwingConstants.TOP);
  }

  /**
   * Sets the type role.
   *
   * @param role the type role (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardSupportingText setTypeRole(final TypeRole role) {
    this.typeRole = Objects.requireNonNull(role, "typeRole");
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the active type role
   * @version v0.2.0
   * @since v0.2.0
   */
  public TypeRole getTypeRole() {
    return typeRole;
  }

  /**
   * Sets the foreground color role.
   *
   * @param role the color role (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardSupportingText setColorRole(final ColorRole role) {
    this.colorRole = Objects.requireNonNull(role, "colorRole");
    repaint();
    return this;
  }

  /**
   * @return the active foreground color role
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole getColorRole() {
    return colorRole;
  }

  @Override
  public Font getFont() {
    return typeRole != null ? typeRole.resolve() : super.getFont();
  }

  @Override
  public Color getForeground() {
    return colorRole != null ? colorRole.resolve() : super.getForeground();
  }

  @Override
  protected void paintComponent(final Graphics g) {
    if (ElwhaCardTitle.isEffectivelyEnabled(this)) {
      super.paintComponent(g);
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      super.paintComponent(g2);
    } finally {
      g2.dispose();
    }
  }
}
