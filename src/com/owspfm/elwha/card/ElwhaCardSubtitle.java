package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Font;
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
    super(text == null ? "" : text);
    setHorizontalAlignment(SwingConstants.LEADING);
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
}
