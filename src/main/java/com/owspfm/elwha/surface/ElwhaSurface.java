package com.owspfm.elwha.surface;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShadowBearing;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * A token-native rounded surface primitive — a {@link JPanel} subclass that paints a role-filled,
 * round-rect, optionally outlined background through the shared {@link SurfacePainter} helper.
 *
 * <p><strong>What it is.</strong> {@code ElwhaSurface} is Elwha's equivalent of Material's
 * <em>Paper</em>: the bare round-rect container that the rest of the component set composes for its
 * background paint. It carries no interaction model — no hover / pressed / selected state layers,
 * no focus indicator, no padding API. Consumers needing an interactive surface should compose an
 * {@code ElwhaSurface} inside {@link com.owspfm.elwha.chip.ElwhaChip}, the upcoming {@code
 * ElwhaIconButton}, or {@code ElwhaCard} V2.
 *
 * <p><strong>Typed API only.</strong> The four setters — {@link #setSurfaceRole(ColorRole)}, {@link
 * #setShape(ShapeScale)}, {@link #setBorderRole(ColorRole)}, {@link #setBorderWidth(int)} — accept
 * tokens or pixel counts only. There is no {@code setSurfaceColor(Color)} / {@code
 * setCornerRadius(int)} / raw-typed escape hatch; the lesson from {@code ElwhaCard} V1 is that
 * every escape hatch becomes a long-term migration cost. Pre-1.0, the typed API is the only API.
 *
 * <p><strong>Defaults.</strong> {@link ColorRole#SURFACE} fill, {@link ShapeScale#MD} (12 px)
 * corner radius, no border. These match the locked {@code ElwhaCard} default so Card V2 composing a
 * Surface inherits the right look out of the box.
 *
 * <p><strong>Binding rule.</strong> The painter resolves every token at paint time, so a runtime
 * theme/mode switch re-skins the Surface on the next paint without any per-instance listener — see
 * {@code docs/research/elwha-token-taxonomy.md}.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaSurface card = new ElwhaSurface()
 *     .setSurfaceRole(ColorRole.SURFACE_CONTAINER)
 *     .setShape(ShapeScale.LG)
 *     .setBorderRole(ColorRole.OUTLINE_VARIANT);
 * card.add(new JLabel("Hello"));
 * }</pre>
 *
 * <p>Design decisions and Card V2 composition relationship: {@code
 * docs/research/elwha-surface-design.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.1.0
 */
public class ElwhaSurface extends JPanel implements ShadowBearing {

  /**
   * Maximum supported M3 elevation level, matching {@code ElevationTokens.Level5} (12 dp). Levels
   * 0..5 are accepted; higher values clamp.
   */
  public static final int MAX_ELEVATION = 5;

  private ColorRole surfaceRole = ColorRole.SURFACE;
  private ShapeScale shape = ShapeScale.MD;
  private ColorRole borderRole;
  private int borderWidth = 1;

  /**
   * The M3 elevation level used to size the chassis shadow reserve and drive the painted shadow.
   * Protected so subclasses with transient lift semantics (e.g. {@code ElwhaCard} during a drag)
   * can momentarily override the painted elevation via try/finally around super.paintComponent.
   */
  protected int elevation;

  /**
   * Creates a Surface with the default look — {@link ColorRole#SURFACE} fill, {@link ShapeScale#MD}
   * shape, no border. Use the fluent setters to override any of the four axes.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface() {
    setOpaque(false);
  }

  /**
   * Sets the surface fill role and triggers a repaint. Must not be {@code null} — Surface always
   * paints a fill; a transparent variant is intentionally not modeled here (compose the Surface
   * inside a parent that paints the desired background instead).
   *
   * @param role the fill role
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setSurfaceRole(final ColorRole role) {
    this.surfaceRole = Objects.requireNonNull(role, "surfaceRole");
    repaint();
    return this;
  }

  /**
   * Returns the current surface fill role.
   *
   * @return the surface role, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole getSurfaceRole() {
    return surfaceRole;
  }

  /**
   * Sets the corner-radius step and triggers a repaint. Must not be {@code null}.
   *
   * @param shape the shape step
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code shape} is {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setShape(final ShapeScale shape) {
    this.shape = Objects.requireNonNull(shape, "shape");
    repaint();
    return this;
  }

  /**
   * Returns the current corner-radius step.
   *
   * @return the shape step, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ShapeScale getShape() {
    return shape;
  }

  /**
   * Sets the border stroke role and triggers a repaint. Pass {@code null} to remove the border
   * entirely — the corollary is that an outlined Surface is just {@code
   * setBorderRole(ColorRole.OUTLINE)}.
   *
   * @param role the border role, or {@code null} to suppress the border
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setBorderRole(final ColorRole role) {
    this.borderRole = role;
    repaint();
    return this;
  }

  /**
   * Returns the current border stroke role, or {@code null} if no border is painted.
   *
   * @return the border role, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole getBorderRole() {
    return borderRole;
  }

  /**
   * Sets the border stroke width in pixels and triggers a repaint. Has no visible effect while
   * {@link #getBorderRole()} is {@code null}. A value of {@code 0} or less suppresses the border
   * just as {@code setBorderRole(null)} does.
   *
   * <p><strong>Geometry note.</strong> The stroke is centered on a path inset by {@code px / 2} so
   * the outer edge tracks the fill corner exactly at any width. The <em>inner</em> corner radius is
   * therefore {@code shape.px() / 2 - px}, which goes to zero (a visibly square inner edge) as the
   * stroke width approaches the corner radius. In practice the design system uses {@code 1} and
   * {@code 2} (focus ring) almost everywhere; thicker strokes are best paired with {@link
   * com.owspfm.elwha.theme.ShapeScale#LG} or {@link com.owspfm.elwha.theme.ShapeScale#XL} so the
   * inner corner retains visible rounding.
   *
   * @param px the stroke width in pixels
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setBorderWidth(final int px) {
    this.borderWidth = px;
    repaint();
    return this;
  }

  /**
   * Returns the current border stroke width in pixels.
   *
   * @return the stroke width
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getBorderWidth() {
    return borderWidth;
  }

  /**
   * Sets the M3 elevation level (0..{@link #MAX_ELEVATION}). Level 0 disables the shadow entirely;
   * levels 1..5 paint the M3 key+ambient shadow stack via {@link ShadowPainter#paint}. The chassis
   * reserves space around the visible body to accommodate the shadow halo — see {@link
   * #getInsets()} and {@link ShadowPainter#shadowInsets}.
   *
   * @param level the elevation level (clamped to {@code [0, MAX_ELEVATION]})
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaSurface setElevation(final int level) {
    final int clamped = Math.max(0, Math.min(MAX_ELEVATION, level));
    if (clamped == this.elevation) {
      return this;
    }
    this.elevation = clamped;
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the current elevation level (0..{@link #MAX_ELEVATION})
   * @version v0.2.0
   * @since v0.2.0
   */
  public int getElevation() {
    return elevation;
  }

  /**
   * Hook for subclasses to lift the painted elevation transiently — e.g. a card adds a hover or
   * dragged bump. Defaults to the resting {@link #elevation}; overrides should return values in
   * {@code [0, MAX_ELEVATION]}. The chassis shadow reserve in {@link #getInsets()} sizes from the
   * resting elevation, so transient lifts beyond the resting reserve may visually clip at the
   * chassis edge.
   *
   * @return the elevation to paint right now
   * @version v0.2.0
   * @since v0.2.0
   */
  protected int currentElevationForPaint() {
    return elevation;
  }

  /**
   * @return the chassis insets, including the shadow reserve required by {@link #elevation}. Layout
   *     managers reading this position children inside the visible card body, away from the
   *     reserved shadow halo.
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public Insets getInsets() {
    return ShadowPainter.shadowInsets(elevation);
  }

  /**
   * Returns this surface's shadow-halo reserve — the per-edge space it pads around its visible body
   * for the M3 elevation shadow. For the {@code ElwhaSurface} family the reserve <em>is</em> the
   * chassis insets ({@link #getInsets()} returns exactly {@code
   * ShadowPainter.shadowInsets(elevation)} — nothing else), so this delegates to the virtual {@link
   * #getInsets()} and returns a fresh copy. Subclasses that override {@code getInsets()} (e.g.
   * {@code ElwhaCard}, which reserves for {@link #MAX_ELEVATION} so transient hover / drag lifts
   * never clip) inherit the correct worst-case reserve through that delegation with no override of
   * their own. A defensive copy is returned so a placement consumer reading this through {@link
   * ShadowBearing} cannot mutate the surface's geometry.
   *
   * @return a fresh copy of the reserved halo insets (never {@code null}); mutating the returned
   *     instance does not affect the surface
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public Insets getShadowInsets() {
    final Insets insets = getInsets();
    return new Insets(insets.top, insets.left, insets.bottom, insets.right);
  }

  /**
   * Paints the surface — shadow into the reserved insets, then round-rect fill + optional border
   * stroke (delegated to {@link SurfacePainter}) over the visible body.
   *
   * @param g the graphics context
   * @version v0.2.0
   * @since v0.1.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final Insets s = getInsets();
    final int bodyX = s.left;
    final int bodyY = s.top;
    final int bodyW = Math.max(0, getWidth() - s.left - s.right);
    final int bodyH = Math.max(0, getHeight() - s.top - s.bottom);
    final int arc = shape.px();
    final int paintElevation = currentElevationForPaint();

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (paintElevation > 0 && bodyW > 0 && bodyH > 0) {
        final Graphics2D shadow = (Graphics2D) g2.create();
        try {
          shadow.translate(bodyX, bodyY);
          ShadowPainter.paint(shadow, bodyW, bodyH, arc, paintElevation);
        } finally {
          shadow.dispose();
        }
      }
      final Graphics2D body = (Graphics2D) g2.create(bodyX, bodyY, bodyW, bodyH);
      try {
        SurfacePainter.paint(
            body, bodyW, bodyH, arc, getSurfaceRole(), null, getBorderRole(), getBorderWidth());
      } finally {
        body.dispose();
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Renders the children through a soft (antialiased) rounded-corner clip so content that fills its
   * cell to the chassis bounds — an opaque {@code ElwhaCardMedia} cover especially — conforms to
   * the chassis's curved outer corners with a smooth edge.
   *
   * <p>Java2D never antialiases a {@link Graphics2D#clip(java.awt.Shape) clip} boundary, so a plain
   * round-rect clip leaves an opaque edge-to-edge child with jagged, stair-stepped corners against
   * the smooth chassis fill (#157). Instead the children are drawn into an offscreen buffer and
   * composited back through an antialiased rounded-rect alpha mask ({@link AlphaComposite#DstIn});
   * the mask carries the AA so every child's corner curve matches the chassis exactly. The buffer
   * is sized to the {@code Graphics}' device resolution so children stay crisp on a HiDPI display.
   *
   * @param g the graphics context
   * @version v0.3.0
   * @since v0.2.0
   */
  @Override
  protected void paintChildren(final Graphics g) {
    if (getComponentCount() == 0) {
      return;
    }
    final Insets s = getInsets();
    final int bodyX = s.left;
    final int bodyY = s.top;
    final int bodyW = Math.max(0, getWidth() - s.left - s.right);
    final int bodyH = Math.max(0, getHeight() - s.top - s.bottom);
    if (bodyW <= 0 || bodyH <= 0) {
      return;
    }
    final int arc = shape.px();

    // Device-resolution offscreen buffer: the current Graphics transform carries the HiDPI scale,
    // so painting at 1x then upscaling would blur the children (text especially).
    final Graphics2D g2 = (Graphics2D) g;
    final AffineTransform tx = g2.getTransform();
    final double scaleX = tx.getScaleX() > 0 ? tx.getScaleX() : 1.0;
    final double scaleY = tx.getScaleY() > 0 ? tx.getScaleY() : 1.0;
    final int deviceW = Math.max(1, (int) Math.ceil(bodyW * scaleX));
    final int deviceH = Math.max(1, (int) Math.ceil(bodyH * scaleY));

    final BufferedImage buffer = new BufferedImage(deviceW, deviceH, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D bg = buffer.createGraphics();
    try {
      bg.scale(scaleX, scaleY);
      bg.translate(-bodyX, -bodyY);
      super.paintChildren(bg);
      // Erase the four corner-overflow regions — the chassis rectangle minus its rounded body — so
      // an opaque edge-to-edge child is cut to the chassis curve. Clear-compositing an antialiased
      // fill soft-erases; a plain Graphics2D.clip would hard-erase and stair-step the corner
      // (#157). bodyShape is the single source of the curve geometry (chassis fill / stroke use it
      // too) so the cut aligns exactly — per #106.
      final RoundRectangle2D.Float body = SurfacePainter.bodyShape(bodyW, bodyH, arc);
      final Area overflow = new Area(new Rectangle2D.Float(bodyX, bodyY, bodyW, bodyH));
      overflow.subtract(
          new Area(
              new RoundRectangle2D.Float(
                  bodyX, bodyY, body.width, body.height, body.arcwidth, body.archeight)));
      bg.setComposite(AlphaComposite.Clear);
      bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      bg.fill(overflow);
    } finally {
      bg.dispose();
    }

    // Blit the device-resolution buffer back 1:1 onto the device pixel grid: translate to the body
    // origin, then undo the device scale so one buffer pixel maps to one device pixel.
    final Graphics2D blit = (Graphics2D) g.create();
    try {
      blit.translate(bodyX, bodyY);
      blit.scale(1.0 / scaleX, 1.0 / scaleY);
      blit.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      blit.drawImage(buffer, 0, 0, null);
    } finally {
      blit.dispose();
    }
  }
}
