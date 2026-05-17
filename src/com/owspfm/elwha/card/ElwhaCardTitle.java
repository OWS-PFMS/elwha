package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Font;
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
    super(text == null ? "" : text);
    setHorizontalAlignment(SwingConstants.LEADING);
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
}
