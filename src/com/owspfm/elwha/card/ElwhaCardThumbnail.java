package com.owspfm.elwha.card;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * The card-thumbnail atom — a Layer 2 image holder for the leading slot of a card header. Accepts a
 * photographic / object {@link Image} rather than a vector glyph; for icon glyphs use {@link
 * ElwhaCardLeadingIcon} instead.
 *
 * <p>Defaults to {@link ThumbnailShape#CIRCULAR} at 40 dp. {@code CIRCULAR} clips the image to a
 * circle; {@code SQUARE} full-bleeds the image to the container's bounds.
 *
 * <p><strong>Inert by construction:</strong> {@code setFocusable(false)} in the constructor. The
 * thumbnail carries no event surface — it's decoration. See {@code
 * docs/research/elwha-card-v3-spec.md} §4.5.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardThumbnail extends JComponent {

  /** Default thumbnail size in dp. */
  public static final int DEFAULT_SIZE_DP = 40;

  private Image image;
  private ThumbnailShape shape = ThumbnailShape.CIRCULAR;
  private int sizeDp = DEFAULT_SIZE_DP;

  /**
   * Creates a thumbnail wrapping the given image.
   *
   * @param image the image to render (must not be {@code null})
   * @throws NullPointerException if {@code image} is {@code null}
   */
  public ElwhaCardThumbnail(final Image image) {
    this.image = Objects.requireNonNull(image, "image");
    setFocusable(false);
    setOpaque(false);
  }

  /**
   * Sets the thumbnail shape.
   *
   * @param newShape the shape (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code newShape} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardThumbnail setShape(final ThumbnailShape newShape) {
    this.shape = Objects.requireNonNull(newShape, "shape");
    repaint();
    return this;
  }

  /**
   * @return the active thumbnail shape
   * @version v0.2.0
   * @since v0.2.0
   */
  public ThumbnailShape getShape() {
    return shape;
  }

  /**
   * Sets the thumbnail size in dp.
   *
   * @param dp the size in dp (must be positive)
   * @return {@code this} for fluent chaining
   * @throws IllegalArgumentException if {@code dp} is non-positive
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardThumbnail setSize(final int dp) {
    if (dp <= 0) {
      throw new IllegalArgumentException("size must be positive, got " + dp);
    }
    this.sizeDp = dp;
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the thumbnail size in dp (named {@code getSizeDp} rather than {@code getSize} to avoid
   *     a clash with {@link java.awt.Component#getSize()}, which returns {@link Dimension})
   * @version v0.2.0
   * @since v0.2.0
   */
  public int getSizeDp() {
    return sizeDp;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(sizeDp, sizeDp);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    if (image == null) {
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      final int w = getWidth();
      final int h = getHeight();
      if (shape == ThumbnailShape.CIRCULAR) {
        g2.setClip(new Ellipse2D.Float(0, 0, w, h));
      }
      g2.drawImage(image, 0, 0, w, h, this);
    } finally {
      g2.dispose();
    }
  }
}
