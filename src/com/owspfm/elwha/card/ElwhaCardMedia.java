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

  /**
   * Intrinsic preferred-width fallback for painter-backed media or image-backed media whose source
   * hasn't loaded yet. Chosen as a sensible default; the chassis layout assigns whatever cell-width
   * is actually available at paint time per spec §3.4 rule 3.
   */
  private static final int DEFAULT_INTRINSIC_WIDTH = 320;

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

  /**
   * Returns the media's INTRINSIC preferred size — never the current laid-out width. Reading {@link
   * #getWidth()} here would create a positive-feedback layout loop in any container that respects
   * child preferred width (e.g., a plain {@link javax.swing.JScrollPane} viewport without {@link
   * javax.swing.Scrollable}-tracking): each layout pass would grow {@code getWidth()} → grow {@code
   * preferred} → grow the parent → grow {@code getWidth()} again, unbounded.
   *
   * <p>Cover-fit slot sizing per spec §3.4 rule 3 is the chassis layout's responsibility, not this
   * getter's. {@link ElwhaCard}'s {@code VerticalCardLayout} calls {@link #heightForSlotWidth(int)}
   * during {@code layoutContainer} to compute the media slot height from the actually-assigned cell
   * width — so the loop never happens and §3.4 still holds.
   *
   * <p>Intrinsic dimensions:
   *
   * <ul>
   *   <li>Image-backed: source image dimensions ({@link java.awt.Image#getWidth} / {@link
   *       java.awt.Image#getHeight}). Falls back to 320 px wide / aspect-ratio-derived height when
   *       the image hasn't loaded yet.
   *   <li>Painter-backed: 320 px wide / aspect-ratio-derived height. {@link #setPreferredHeight}
   *       overrides the derived height.
   * </ul>
   *
   * @return the intrinsic preferred size, never dependent on the current laid-out width
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public Dimension getPreferredSize() {
    final int intrinsicW;
    if (image != null) {
      final int iw = image.getWidth(this);
      intrinsicW = iw > 0 ? iw : DEFAULT_INTRINSIC_WIDTH;
    } else {
      intrinsicW = DEFAULT_INTRINSIC_WIDTH;
    }
    final int h;
    if (preferredHeightDp > 0) {
      h = preferredHeightDp;
    } else {
      h = (int) Math.round(intrinsicW / aspectRatio);
    }
    return new Dimension(intrinsicW, h);
  }

  /**
   * Computes the media's height for a given slot width per spec §3.4 rule 3 (CSS {@code object-fit:
   * cover} semantics). Called by {@code ElwhaCard.VerticalCardLayout.layoutContainer} so the
   * chassis assigns each media child the right (cellW × cellW/aspectRatio) bounds even though
   * preferred-size queries don't carry width context.
   *
   * <p>Resolution order: explicit {@link #setPreferredHeight} wins; otherwise {@code slotWidth /
   * aspectRatio}; otherwise (no slot width yet) {@link #getPreferredSize()}.height.
   *
   * @param slotWidth the chassis-assigned cell width in pixels (0 means "no width yet")
   * @return the height in pixels the chassis should reserve for this media at the given width
   * @version v0.2.0
   * @since v0.2.0
   */
  int heightForSlotWidth(final int slotWidth) {
    if (preferredHeightDp > 0) {
      return preferredHeightDp;
    }
    if (slotWidth > 0) {
      return (int) Math.round(slotWidth / aspectRatio);
    }
    return getPreferredSize().height;
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
