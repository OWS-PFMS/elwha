package com.owspfm.elwha.surface;

import com.owspfm.elwha.theme.ColorRole;
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
public class ElwhaSurface extends JPanel {

  /**
   * Maximum supported M3 elevation level, matching {@code ElevationTokens.Level5} (12 dp). Levels
   * 0..5 are accepted; higher values clamp.
   */
  public static final int MAX_ELEVATION = 5;

  private ColorRole surfaceRole = ColorRole.SURFACE;
  private ShapeScale shape = ShapeScale.MD;
  private ColorRole borderRole;
  private int borderWidth = 1;
  private boolean clipChildrenToCorners;

  // Reused across paints so the rounded-corner child clip doesn't reallocate a device-resolution
  // buffer (or rebuild the corner-overflow Area) every frame — only the genuinely-clipping path
  // touches these, and only when the device size or body geometry actually changes (#272).
  private java.lang.ref.SoftReference<BufferedImage> clipBufferCache;
  private Area cornerOverflowCache;
  private int cornerOverflowX = Integer.MIN_VALUE;
  private int cornerOverflowY = Integer.MIN_VALUE;
  private int cornerOverflowW = -1;
  private int cornerOverflowH = -1;
  private int cornerOverflowArc = -1;

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
   * Opts this surface into the antialiased rounded-corner child clip. When {@code true}, children
   * are composited through the chassis's curved corner so an opaque edge-to-edge child (e.g. an
   * {@code ElwhaCardMedia} cover) conforms to the round-rect outline instead of overhanging it with
   * a square corner (#157). When {@code false} (the default) children paint directly with no
   * offscreen buffer — the right, cheap choice for the common case where every child is inset from
   * the corners (plain surfaces, dialogs, non-media cards), since the clip then has nothing to do
   * and would only burn an allocation per paint (#272).
   *
   * <p>Subclasses that intrinsically host a corner-reaching opaque child may force the clip on
   * regardless of this flag — {@code ElwhaCard} enables it automatically whenever an edge-bleed
   * {@code ElwhaCardMedia} is its first or last visible child, so card consumers never set this.
   *
   * @param clip {@code true} to clip children to the rounded corners; {@code false} to paint them
   *     directly
   * @return {@code this} for fluent chaining
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSurface setClipChildrenToCorners(final boolean clip) {
    if (clip == this.clipChildrenToCorners) {
      return this;
    }
    this.clipChildrenToCorners = clip;
    repaint();
    return this;
  }

  /**
   * Returns whether this surface clips its children to the rounded corners — the consumer-set flag
   * only; a subclass that forces the clip on for intrinsic corner-reaching content does not change
   * this value.
   *
   * @return {@code true} if the child corner clip is enabled
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean getClipChildrenToCorners() {
    return clipChildrenToCorners;
  }

  /**
   * Paint-time resolver for whether the rounded-corner child clip runs — mirrors {@link
   * #currentElevationForPaint()}. Defaults to the consumer-set {@link #getClipChildrenToCorners()}
   * flag; subclasses with intrinsic corner-reaching content (an {@code ElwhaCard} hosting
   * edge-bleed media) override to force it on without disturbing the public flag.
   *
   * @return {@code true} if children should be composited through the rounded-corner clip this
   *     paint
   * @version v0.4.0
   * @since v0.4.0
   */
  protected boolean clipsChildrenToCorners() {
    return clipChildrenToCorners;
  }

  /**
   * Whether this paint folds the body fill / overlay / border into the child clip buffer rather
   * than painting them directly in {@link #paintComponent}. True exactly when the clip runs — it
   * needs a corner-reaching child to be worthwhile — so a childless clip-enabled surface still gets
   * its fill from {@link #paintComponent}, never silently dropping it.
   */
  private boolean foldsBodyIntoClipBuffer() {
    return clipsChildrenToCorners() && getComponentCount() > 0;
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
   * Paints the surface — shadow into the reserved insets, then round-rect fill + optional border
   * stroke (delegated to {@link SurfacePainter}) over the visible body.
   *
   * <p>When {@link #clipsChildrenToCorners()} resolves {@code true} the fill, state-layer overlay,
   * and border are <em>not</em> painted here — they are folded into the child clip buffer in {@link
   * #paintChildren} so the chassis fill shares the children's single antialiased corner cut,
   * instead of the fill and an opaque edge-to-edge child antialiasing their corners in two separate
   * passes (the #163 corner fringe). Only the shadow, which sits behind everything, is painted here
   * in that case.
   *
   * @param g the graphics context
   * @version v0.4.0
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
      if (!foldsBodyIntoClipBuffer() && bodyW > 0 && bodyH > 0) {
        final Graphics2D body = (Graphics2D) g2.create(bodyX, bodyY, bodyW, bodyH);
        try {
          SurfacePainter.paint(
              body, bodyW, bodyH, arc, getSurfaceRole(), null, getBorderRole(), getBorderWidth());
          paintSurfaceOverlay(body, bodyW, bodyH, arc);
        } finally {
          body.dispose();
        }
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Hook for subclasses to paint a surface-level overlay between the chassis fill and the children
   * — the M3 hover / pressed / dragged state-layer tint belongs here. The graphics is positioned at
   * the body origin (paint in body-local coordinates {@code (0,0)..(bodyW,bodyH)}), and the call is
   * invoked from whichever path painted the fill: directly over the fill in {@link #paintComponent}
   * for the common case, or inside the child clip buffer (under the children, sharing their corner
   * cut) when {@link #clipsChildrenToCorners()} is active. Default does nothing.
   *
   * @param g the body-local graphics context (already antialiased)
   * @param bodyW the visible body width in pixels
   * @param bodyH the visible body height in pixels
   * @param arc the corner radius in pixels
   * @version v0.4.0
   * @since v0.4.0
   */
  protected void paintSurfaceOverlay(
      final Graphics2D g, final int bodyW, final int bodyH, final int arc) {}

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
   * <p>The offscreen buffer runs only when {@link #clipsChildrenToCorners()} resolves {@code true}
   * — i.e. the surface actually hosts a corner-reaching opaque child. Every other surface (inset
   * children, dialogs, non-media cards) takes the direct {@code super.paintChildren} path with no
   * allocation, since the clip would have nothing to cut there (#272). When the buffer does run,
   * the device-resolution image and the corner-overflow {@link Area} are cached and reused across
   * paints — re-created only when the device size or body geometry changes — so an animating child
   * (a card ripple) doesn't churn an allocation per frame.
   *
   * @param g the graphics context
   * @version v0.4.0
   * @since v0.2.0
   */
  @Override
  protected void paintChildren(final Graphics g) {
    if (!foldsBodyIntoClipBuffer()) {
      super.paintChildren(g);
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

    final BufferedImage buffer = acquireClipBuffer(deviceW, deviceH);
    final Graphics2D bg = buffer.createGraphics();
    try {
      bg.scale(scaleX, scaleY);
      bg.translate(-bodyX, -bodyY);

      // Fold the chassis fill + state-layer overlay into this buffer, beneath the children, so they
      // share the single antialiased corner cut applied below — an opaque edge-to-edge child no
      // longer antialiases its corner against a separately-AA'd fill (the #163 fringe). The fill is
      // a plain rectangle here; the corner curve comes solely from the one Clear mask, so fill and
      // children get the identical AA edge.
      final Graphics2D bodyG = (Graphics2D) bg.create();
      try {
        bodyG.translate(bodyX, bodyY);
        bodyG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final ColorRole fillRole = getSurfaceRole();
        if (fillRole != null) {
          bodyG.setColor(fillRole.resolve());
          bodyG.fillRect(0, 0, bodyW, bodyH);
        }
        paintSurfaceOverlay(bodyG, bodyW, bodyH, arc);
      } finally {
        bodyG.dispose();
      }

      super.paintChildren(bg);
      // Erase the four corner-overflow regions — the chassis rectangle minus its rounded body — so
      // the folded fill and an opaque edge-to-edge child are cut to the chassis curve together.
      // Clear-compositing an antialiased fill soft-erases; a plain Graphics2D.clip would hard-erase
      // and stair-step the corner (#157). bodyShape is the single source of the curve geometry
      // (chassis fill / stroke use it too) so the cut aligns exactly — per #106.
      bg.setComposite(AlphaComposite.Clear);
      bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      bg.fill(cornerOverflow(bodyX, bodyY, bodyW, bodyH, arc));

      // Border last — inside the kept region, after the corner cut. Its inset stroke tracks the
      // same
      // curve, so it rides the single AA edge too. Suppressed (null role) for an OUTLINED card,
      // which repaints its outline above the children.
      final ColorRole borderRoleNow = getBorderRole();
      if (borderRoleNow != null && getBorderWidth() > 0) {
        bg.setComposite(AlphaComposite.SrcOver);
        final Graphics2D borderG = (Graphics2D) bg.create();
        try {
          borderG.translate(bodyX, bodyY);
          SurfacePainter.paint(
              borderG, bodyW, bodyH, arc, null, null, borderRoleNow, getBorderWidth());
        } finally {
          borderG.dispose();
        }
      }
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

  /**
   * Returns a cleared device-resolution ARGB buffer of the requested size, reusing the cached image
   * when its dimensions are unchanged (the common animating-child case) so the clip path doesn't
   * allocate per paint. A reused buffer is cleared to fully transparent first, since it still holds
   * the previous frame's pixels (#272).
   */
  private BufferedImage acquireClipBuffer(final int deviceW, final int deviceH) {
    BufferedImage buffer = clipBufferCache != null ? clipBufferCache.get() : null;
    if (buffer == null || buffer.getWidth() != deviceW || buffer.getHeight() != deviceH) {
      buffer = new BufferedImage(deviceW, deviceH, BufferedImage.TYPE_INT_ARGB);
      clipBufferCache = new java.lang.ref.SoftReference<>(buffer);
      return buffer;
    }
    final Graphics2D clear = buffer.createGraphics();
    try {
      clear.setComposite(AlphaComposite.Clear);
      clear.fillRect(0, 0, deviceW, deviceH);
    } finally {
      clear.dispose();
    }
    return buffer;
  }

  /**
   * Returns the corner-overflow region — the body rectangle minus its rounded body shape — reusing
   * the cached {@link Area} when the body geometry is unchanged, so the {@link Area} subtract math
   * isn't rebuilt per paint (#272). {@link SurfacePainter#bodyShape} is the single source of the
   * curve geometry so the cut aligns with the chassis fill and stroke exactly.
   */
  private Area cornerOverflow(
      final int bodyX, final int bodyY, final int bodyW, final int bodyH, final int arc) {
    if (cornerOverflowCache != null
        && bodyX == cornerOverflowX
        && bodyY == cornerOverflowY
        && bodyW == cornerOverflowW
        && bodyH == cornerOverflowH
        && arc == cornerOverflowArc) {
      return cornerOverflowCache;
    }
    final RoundRectangle2D.Float body = SurfacePainter.bodyShape(bodyW, bodyH, arc);
    final Area overflow = new Area(new Rectangle2D.Float(bodyX, bodyY, bodyW, bodyH));
    overflow.subtract(
        new Area(
            new RoundRectangle2D.Float(
                bodyX, bodyY, body.width, body.height, body.arcwidth, body.archeight)));
    cornerOverflowCache = overflow;
    cornerOverflowX = bodyX;
    cornerOverflowY = bodyY;
    cornerOverflowW = bodyW;
    cornerOverflowH = bodyH;
    cornerOverflowArc = arc;
    return overflow;
  }
}
