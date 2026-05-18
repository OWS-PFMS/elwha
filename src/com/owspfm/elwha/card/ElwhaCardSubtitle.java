package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * The card-subtitle atom — a Layer 2 typed label for the secondary line of a card header. Defaults
 * to {@link TypeRole#LABEL_MEDIUM} type and {@link ColorRole#ON_SURFACE_VARIANT} color (the M3
 * de-emphasis color for secondary text), start-aligned.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §4.2.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardSubtitle extends JLabel {

  private TypeRole typeRole = TypeRole.LABEL_MEDIUM;
  private ColorRole colorRole = ColorRole.ON_SURFACE_VARIANT;

  /** Creates an empty subtitle. */
  public ElwhaCardSubtitle() {
    this("");
  }

  /**
   * Creates a subtitle with the given text.
   *
   * @param text the subtitle text (may be {@code null} or empty)
   */
  public ElwhaCardSubtitle(final String text) {
    super("");
    setHorizontalAlignment(SwingConstants.LEADING);
    setAlignmentX(LEFT_ALIGNMENT);
    setText(text);
  }

  @Override
  public void setText(final String text) {
    super.setText(WrappingLabels.htmlWrap(text));
  }

  @Override
  public Dimension getPreferredSize() {
    return WrappingLabels.preferredSizeForWidth(this, super.getPreferredSize());
  }

  /**
   * Reports unbounded X-axis so a parent {@code BoxLayout(Y_AXIS)} (or any width-respecting layout)
   * stretches this atom to the available width — letting HTML wrap kick in at narrow chassis widths
   * per spec §3.4 rule 2 / §22 guard-rail.
   *
   * @return a {@code Dimension} with unbounded width and preferred height
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
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
  public ElwhaCardSubtitle setTypeRole(final TypeRole role) {
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
  public ElwhaCardSubtitle setColorRole(final ColorRole role) {
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
