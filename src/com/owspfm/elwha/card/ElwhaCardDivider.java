package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * A 1 dp horizontal divider painted in {@link ColorRole#OUTLINE_VARIANT}. {@link DividerStyle}
 * selects between {@link DividerStyle#FULL} (intended to bleed past the card's content padding to
 * the card edge) and {@link DividerStyle#INSET} (respects the card padding).
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §5.4.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardDivider extends JComponent {

  private static final int DIVIDER_HEIGHT_PX = 1;
  private final DividerStyle style;

  /** Creates a divider with the default {@link DividerStyle#FULL} treatment. */
  public ElwhaCardDivider() {
    this(DividerStyle.FULL);
  }

  /**
   * Creates a divider with the given style.
   *
   * @param style the divider style (must not be {@code null})
   * @throws NullPointerException if {@code style} is {@code null}
   */
  public ElwhaCardDivider(final DividerStyle style) {
    this.style = Objects.requireNonNull(style, "style");
    setOpaque(false);
  }

  /**
   * @return the divider style
   * @version v0.2.0
   * @since v0.2.0
   */
  public DividerStyle getStyle() {
    return style;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(1, DIVIDER_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, DIVIDER_HEIGHT_PX);
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    g.setColor(ColorRole.OUTLINE_VARIANT.resolve());
    g.fillRect(0, 0, getWidth(), DIVIDER_HEIGHT_PX);
  }
}
