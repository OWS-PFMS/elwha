package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * The card-title atom — a Layer 2 typed label with M3-correct defaults for the title slot of a card
 * header. Defaults to {@link TypeRole#TITLE_MEDIUM} type and {@link ColorRole#ON_SURFACE} color,
 * start-aligned, word-wraps at narrow widths (does not ellipsize). HTML content auto-wraps via
 * Swing's built-in HTML rendering.
 *
 * <p>Tokens resolve at paint time via {@link #getFont()} and {@link #getForeground()} overrides, so
 * a runtime theme switch re-skins the title on the next repaint without consumer code. See {@code
 * docs/research/elwha-card-v3-spec.md} §4.1.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardTitle extends JLabel {

  private TypeRole typeRole = TypeRole.TITLE_MEDIUM;
  private ColorRole colorRole = ColorRole.ON_SURFACE;

  /** Creates an empty title. Use {@link #setText(String)} to assign content. */
  public ElwhaCardTitle() {
    this("");
  }

  /**
   * Creates a title with the given text.
   *
   * @param text the title text (may be {@code null} or empty)
   */
  public ElwhaCardTitle(final String text) {
    super("");
    setHorizontalAlignment(SwingConstants.LEADING);
    setAlignmentX(LEFT_ALIGNMENT);
    setText(text);
  }

  /**
   * Auto-wraps plain text in {@code <html>} so the label word-wraps at the parent width. HTML
   * content the caller already provided is left as-is. See {@code
   * docs/research/elwha-card-v3-spec.md} §4.1 — "word-wraps (does not ellipsize at narrow widths).
   * HTML auto-wrapped via the same convention setSupportingText uses."
   *
   * @param text the new text (may be {@code null} or empty)
   * @version v0.2.0
   * @since v0.2.0
   */
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
   * per spec §3.4 rule 2 / §22 guard-rail. Without this override, {@link JLabel} falls back to its
   * preferred (natural single-line) width and refuses to shrink, so text would clip instead of wrap.
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
   * Sets the type role. Triggers a revalidate so the new font's metrics propagate to parent layout.
   *
   * @param role the type role (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardTitle setTypeRole(final TypeRole role) {
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
  public ElwhaCardTitle setColorRole(final ColorRole role) {
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
    if (isEffectivelyEnabled(this)) {
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

  /**
   * Walks up the ancestor chain — returns false if {@code component} or any ancestor reports {@link
   * java.awt.Component#isEnabled()} false. Enables the spec §11 "uniform 0.38 across the card"
   * treatment when only the {@link ElwhaCard} chassis was disabled.
   */
  static boolean isEffectivelyEnabled(final java.awt.Component component) {
    if (!component.isEnabled()) {
      return false;
    }
    Container p = component.getParent();
    while (p != null) {
      if (!p.isEnabled()) {
        return false;
      }
      p = p.getParent();
    }
    return true;
  }
}
