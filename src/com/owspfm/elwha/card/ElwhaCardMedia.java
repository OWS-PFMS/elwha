package com.owspfm.elwha.card;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.Objects;
import java.util.function.Consumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * The card-media layout primitive — an inert image / painter slot for a card. Factory-only
 * construction; no public constructor accepts a {@link JComponent} so consumers cannot embed
 * interactive content as media (per M3 + the V3 actionability doctrine: media is decoration).
 *
 * <p>Defaults to a 16:9 aspect ratio; {@link #setPreferredHeight(int)} overrides the aspect-ratio
 * sizing with an explicit height.
 *
 * <p><strong>Corner clipping.</strong> Owned by the chassis: {@code ElwhaSurface.paintChildren}
 * intersects every child's paint with the body's rounded rect via {@code SurfacePainter.bodyShape},
 * so media painted at the chassis edges naturally rounds to match the chassis corners. {@code
 * ElwhaCardMedia} itself does no local clipping — single source of truth for the corner curve
 * eliminates the cubic-Bezier vs elliptical-arc drift that the per-media clip introduced.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §5.2.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardMedia extends JComponent {

  private static final double DEFAULT_ASPECT_RATIO = 16.0 / 9.0;

  private final Image image;
  private final Consumer<Graphics2D> painter;
  private double aspectRatio = DEFAULT_ASPECT_RATIO;
  private int preferredHeightDp = -1;
  private boolean decorative;
  private String altText;

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

  // ----------------------------------------------------------- accessibility

  /**
   * Declares whether this media slot is purely decorative (hidden from assistive technology) or
   * informative (carries semantic content that screen readers should describe via {@link
   * #setAltText(String)}). Defaults to {@code false} (informative).
   *
   * <p>Per M3 accessibility doctrine (m3-card-spec-organized.md §5.5.3): decorative media is
   * skipped in the accessibility traversal entirely so a screen reader doesn't announce noisy
   * "image" placeholders between meaningful card content. Informative media carries an alt-text
   * description that AT verbalizes.
   *
   * @param newDecorative {@code true} to hide from AT, {@code false} to expose as an icon
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardMedia setDecorative(final boolean newDecorative) {
    this.decorative = newDecorative;
    // Force the AccessibleContext to re-resolve role / name on next AT query.
    accessibleContext = null;
    return this;
  }

  /**
   * @return whether the media is marked decorative (hidden from AT)
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isDecorative() {
    return decorative;
  }

  /**
   * Sets the alt-text description for informative media. AT verbalizes this via the {@link
   * AccessibleContext}'s accessible description. Ignored when {@link #isDecorative()} is {@code
   * true}. Pass {@code null} to clear.
   *
   * @param newAltText the alt-text description, or {@code null} to clear
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardMedia setAltText(final String newAltText) {
    this.altText = newAltText;
    accessibleContext = null;
    return this;
  }

  /**
   * @return the alt-text description, or {@code null} if none
   * @version v0.2.0
   * @since v0.2.0
   */
  public String getAltText() {
    return altText;
  }

  /**
   * Exposes the media's role and alt-text to assistive technology per spec §5.2 / §5.5.3 + #109.
   * Decorative media reports {@link AccessibleRole#LABEL} with no name / description (and AT
   * implementations skip null-name nodes); informative media reports {@link AccessibleRole#ICON}
   * with the alt-text as its accessible description so screen readers verbalize it.
   *
   * @return the accessible context
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext =
          new AccessibleJComponent() {
            @Override
            public AccessibleRole getAccessibleRole() {
              return decorative ? AccessibleRole.LABEL : AccessibleRole.ICON;
            }

            @Override
            public String getAccessibleName() {
              if (decorative) {
                return null;
              }
              return altText != null ? altText : super.getAccessibleName();
            }

            @Override
            public String getAccessibleDescription() {
              if (decorative) {
                return null;
              }
              return altText != null ? altText : super.getAccessibleDescription();
            }
          };
    }
    return accessibleContext;
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
      // Corner clipping is owned by ElwhaSurface.paintChildren — the chassis already intersects
      // every child's paint with the body's rounded rect (top + bottom corners). The previous
      // per-media cubic-Bezier setClip both duplicated that work AND overrode the inherited
      // chassis clip (Graphics2D.setClip REPLACES), letting media pixels leak at the bottom when
      // the bezier endpoints diverged from RoundRectangle2D's elliptical-arc internals. Rely on
      // the chassis clip — single source of truth per spec §5.2 + #106.
      if (image != null) {
        drawImageCoverFit(g2);
      } else if (painter != null) {
        painter.accept(g2);
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * CSS {@code object-fit: cover} semantics per spec §3.4 rule 3: scale the source image to fill
   * the slot while preserving the source aspect ratio; crop the overflow at the slot edges.
   * Narrower chassis → re-cropped from the same source rather than stretched.
   */
  private void drawImageCoverFit(final Graphics2D g2) {
    final int slotW = getWidth();
    final int slotH = getHeight();
    if (slotW <= 0 || slotH <= 0) {
      return;
    }
    final int iw = image.getWidth(this);
    final int ih = image.getHeight(this);
    if (iw <= 0 || ih <= 0) {
      // Image not yet loaded — fall back to stretch (ImageObserver will repaint when ready).
      g2.drawImage(image, 0, 0, slotW, slotH, this);
      return;
    }
    final double scale = Math.max((double) slotW / iw, (double) slotH / ih);
    final int drawW = (int) Math.round(iw * scale);
    final int drawH = (int) Math.round(ih * scale);
    final int dx = (slotW - drawW) / 2;
    final int dy = (slotH - drawH) / 2;
    g2.drawImage(image, dx, dy, drawW, drawH, this);
  }
}
