package com.owspfm.elwha.card;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * The card-media layout primitive — an inert image / painter slot for a card. Factory-only
 * construction; no public constructor accepts a {@link JComponent} so consumers cannot embed
 * interactive content as media (per M3 + the V3 actionability doctrine: media is decoration).
 *
 * <p>Defaults to a 16:9 aspect ratio; {@link #setPreferredHeight(int)} overrides the aspect-ratio
 * sizing with an explicit height.
 *
 * <p><strong>Corner clipping.</strong> When the media's parent is an {@link ElwhaCard} and the
 * media is the first child in {@code VERTICAL} orientation (or the leading column in {@code
 * HORIZONTAL}), the media auto-clips its paint to the card's outer rounded shape using a
 * cubic-Bezier circle approximation matching {@code SurfacePainter}'s elliptical arc — so the
 * media's top corners meet the card's top corners pixel-perfectly.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §5.2.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardMedia extends JComponent {

  private static final double DEFAULT_ASPECT_RATIO = 16.0 / 9.0;
  // Cubic Bezier control distance for a circle approximation: k = (4/3) * tan(pi/8).
  private static final double BEZIER_K = 0.5522847498;

  private final Image image;
  private final Consumer<Graphics2D> painter;
  private double aspectRatio = DEFAULT_ASPECT_RATIO;
  private int preferredHeightDp = -1;

  private ElwhaCardMedia(final Image image, final Consumer<Graphics2D> painter) {
    this.image = image;
    this.painter = painter;
    setFocusable(false);
    setOpaque(false);
  }

  /**
   * Factory: a media slot rendering the given {@link Image}, scaled to fill the slot bounds with
   * bilinear interpolation.
   *
   * @param image the image (must not be {@code null})
   * @return a new media slot
   * @throws NullPointerException if {@code image} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaCardMedia image(final Image image) {
    Objects.requireNonNull(image, "image");
    return new ElwhaCardMedia(image, null);
  }

  /**
   * Factory: a media slot delegating paint to a {@link Consumer} of {@link Graphics2D} — for
   * procedurally-rendered media (charts, gradients, generative art). The painter is called inside
   * the corner-clip if the media is at the card's top.
   *
   * @param paint the paint callback (must not be {@code null})
   * @return a new media slot
   * @throws NullPointerException if {@code paint} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaCardMedia painter(final Consumer<Graphics2D> paint) {
    Objects.requireNonNull(paint, "paint");
    return new ElwhaCardMedia(null, paint);
  }

  /**
   * Sets the aspect ratio (width / height). Used for preferred-height sizing when {@link
   * #setPreferredHeight(int)} has not been set.
   *
   * @param ratio the aspect ratio (must be positive)
   * @return {@code this} for fluent chaining
   * @throws IllegalArgumentException if {@code ratio} is non-positive
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardMedia setAspectRatio(final double ratio) {
    if (ratio <= 0) {
      throw new IllegalArgumentException("ratio must be positive, got " + ratio);
    }
    this.aspectRatio = ratio;
    revalidate();
    return this;
  }

  /**
   * @return the active aspect ratio
   * @version v0.2.0
   * @since v0.2.0
   */
  public double getAspectRatio() {
    return aspectRatio;
  }

  /**
   * Overrides the aspect-ratio-derived preferred height with an explicit height in dp. Pass {@code
   * -1} to revert to aspect-ratio sizing.
   *
   * @param dp the explicit height in dp, or {@code -1} to revert
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardMedia setPreferredHeight(final int dp) {
    this.preferredHeightDp = dp;
    revalidate();
    return this;
  }

  /**
   * @return the explicit preferred height in dp, or {@code -1} if aspect-ratio sizing is in effect
   * @version v0.2.0
   * @since v0.2.0
   */
  public int getPreferredHeight() {
    return preferredHeightDp;
  }

  @Override
  public Dimension getPreferredSize() {
    final int width = getWidth() > 0 ? getWidth() : 0;
    final int h;
    if (preferredHeightDp > 0) {
      h = preferredHeightDp;
    } else if (width > 0) {
      h = (int) Math.round(width / aspectRatio);
    } else {
      h = 160;
    }
    return new Dimension(width > 0 ? width : 320, h);
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      final Shape clip = topCornerClip();
      if (clip != null) {
        g2.setClip(clip);
      }
      if (image != null) {
        g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
      } else if (painter != null) {
        painter.accept(g2);
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * If this media is the first child of an {@link ElwhaCard} in {@code VERTICAL} orientation,
   * return a Shape clipping the paint to the card's outer top corners. Otherwise return {@code
   * null} (no clipping — paint the full bounds).
   */
  private Shape topCornerClip() {
    final Container parent = getParent();
    if (!(parent instanceof ElwhaCard card)) {
      return null;
    }
    if (card.getOrientation() != CardOrientation.VERTICAL) {
      return null;
    }
    if (parent.getComponentCount() == 0 || parent.getComponent(0) != this) {
      return null;
    }
    final int arc = card.getShape().px();
    return cubicBezierTopRoundedClip(getWidth(), getHeight(), arc);
  }

  /**
   * Builds a top-rounded clip shape (top-left + top-right corners rounded to {@code arc} via
   * cubic-Bezier; bottom corners square). Matches {@code SurfacePainter}'s elliptical arc so the
   * media's top edges align pixel-perfectly with the card's chassis.
   */
  private static Shape cubicBezierTopRoundedClip(final int w, final int h, final int arc) {
    if (arc <= 0) {
      return new RoundRectangle2D.Float(0, 0, w, h, 0, 0);
    }
    final double k = arc * BEZIER_K;
    final Path2D.Double path = new Path2D.Double();
    path.moveTo(arc, 0);
    path.lineTo(w - arc, 0);
    path.curveTo(w - arc + k, 0, w, arc - k, w, arc);
    path.lineTo(w, h);
    path.lineTo(0, h);
    path.lineTo(0, arc);
    path.curveTo(0, arc - k, arc - k, 0, arc, 0);
    path.closePath();
    return path;
  }
}
