package com.owspfm.elwha.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;

/**
 * Library-internal helper that paints the round-rect token-resolved surface — fill, optional
 * state-layer overlay, and optional border stroke — used by {@link com.owspfm.elwha.chip.ElwhaChip}
 * today and by {@code ElwhaSurface} (#43) / {@code ElwhaCard} V2 (#253) when they land. Living in
 * one place keeps the round-rect surface paint definitionally consistent across the component set.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only because Java's
 * package-private visibility doesn't cross the {@code .theme} package boundary into the component
 * packages that consume it. Library consumers must not depend on this type — its signature and
 * semantics can change without a deprecation cycle.
 *
 * <p>All inputs are resolved at call time — callers pass {@link ColorRole}s, not pre-resolved
 * {@link Color}s — so the painter respects the token binding rule by construction.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.1.0
 */
public final class SurfacePainter {

  private SurfacePainter() {}

  /**
   * Paints the surface background and (optionally) border for a token-resolved component into
   * {@code g}.
   *
   * <p>The painter centers the round-rect on the half-pixel grid so a 1 px AA stroke renders crisp
   * without straddling integer columns, and uses the same geometry for fill and stroke so the
   * surface edge and border edge stay co-located. Sets {@link RenderingHints#KEY_ANTIALIASING} and
   * {@link RenderingHints#KEY_STROKE_CONTROL} on a defensive copy of {@code g} — callers don't need
   * to.
   *
   * <p>State-layer compositing follows the Material 3 model: the overlay color is the {@code
   * on}-pair of {@code surfaceRole}, blended at the layer's resolved opacity over the resolved
   * surface. When {@code surfaceRole} is {@code null} (transparent variants such as {@link
   * com.owspfm.elwha.chip.ChipVariant#GHOST}) the overlay still paints — tinted against {@link
   * ColorRole#SURFACE} — so an idle ghost surface still reveals on hover / press / select.
   *
   * @param g the graphics context (not mutated; a copy is made for rendering-hint isolation)
   * @param width the surface width in pixels
   * @param height the surface height in pixels
   * @param arc the corner radius in pixels (clamped to {@code min(width, height)} internally)
   * @param surfaceRole the resting surface role; {@code null} for transparent resting fill
   * @param overlay the state layer to composite over the surface, or {@code null} for none
   * @param borderRole the border stroke role; {@code null} suppresses the border
   * @param borderWidthPx the border stroke width in pixels; {@code <= 0} suppresses the border
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void paint(
      Graphics2D g,
      int width,
      int height,
      int arc,
      ColorRole surfaceRole,
      StateLayer overlay,
      ColorRole borderRole,
      float borderWidthPx) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

      int clampedArc = Math.min(arc, Math.min(width, height));

      Color fill = resolveFill(surfaceRole, overlay);
      if (fill != null) {
        // Fill extends to the full component bounds so anti-aliased corners match the stroke's
        // outer edge — and so a fill-only call (no border) doesn't leave a 1 px transparent gutter.
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0f, 0f, width, height, clampedArc, clampedArc));
      }

      if (borderRole != null && borderWidthPx > 0f) {
        // Center the stroke on a path inset by half its width so the entire stroke fits inside the
        // component bounds at any width — without this the outer half of the stroke is clipped
        // away and a wide border reads as a fuzzy inner band. Inset corner diameter shrinks by the
        // stroke width so the stroke's outer edge tracks the fill's corner exactly.
        float inset = borderWidthPx / 2f;
        float strokeW = Math.max(0f, width - borderWidthPx);
        float strokeH = Math.max(0f, height - borderWidthPx);
        float strokeArc = Math.max(0f, clampedArc - borderWidthPx);
        g2.setColor(borderRole.resolve());
        g2.setStroke(new BasicStroke(borderWidthPx));
        g2.draw(new RoundRectangle2D.Float(inset, inset, strokeW, strokeH, strokeArc, strokeArc));
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Returns the inset reserve every elevated surface needs around its visible body so the
   * convolution-blurred shadow doesn't get clipped by the {@link java.awt.Component} bounds.
   * Lateral + top reserves equal the blur radius (the halo feathers symmetrically); bottom reserves
   * blur + the key downward offset. {@code elevation <= 0} returns zero insets.
   *
   * @param elevation the M3 elevation level (0..{@code MAX_ELEVATION})
   * @return the inset reserve, never {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public static Insets shadowInsets(int elevation) {
    if (elevation <= 0) {
      return new Insets(0, 0, 0, 0);
    }
    final int blur = blurRadius(elevation);
    final int offsetY = keyOffsetY(elevation);
    // Lateral reserve = the blur halo. Vertical reserve = blur halo + key offset (downward).
    // Top can stay at the blur halo since the convolution feathers symmetrically (we don't
    // shift the source up, so the top blur is just the natural blur radius).
    return new Insets(blur, blur, blur + offsetY, blur);
  }

  /** Blur radius in pixels per elevation level — tuned for a soft falloff that scales smoothly. */
  private static int blurRadius(final int elevation) {
    return Math.max(2, Math.min(12, 2 + elevation * 2));
  }

  /** Downward offset of the key shadow per elevation level — biases weight below the surface. */
  private static int keyOffsetY(final int elevation) {
    return Math.max(1, Math.min(6, elevation + 1));
  }

  /**
   * Paints the M3-aligned drop shadow for an elevated surface. Renders the body silhouette into an
   * offscreen {@link BufferedImage} with margin for the blur halo, then runs a two-pass box blur
   * via {@link ConvolveOp} for a smooth Gaussian-like falloff at the corners — no visible stepping
   * from stacked round-rect layers. The source rect is shifted downward by the key offset so the
   * visible weight lands below the surface (directional drop, "lifted from above").
   *
   * <p>Geometry + alpha scale with {@code elevation}: low elevations produce a tight subtle halo;
   * high elevations produce a substantial drop. Per-paint allocation of the offscreen image is
   * acceptable for v0.2 — revisit with a size-keyed cache if hot-path profiling surfaces it.
   *
   * @param g the graphics context (not mutated; a copy is made for rendering-hint isolation)
   * @param x left of the visible surface body
   * @param y top of the visible surface body
   * @param w width of the visible surface body
   * @param h height of the visible surface body
   * @param arc corner radius in pixels
   * @param elevation the M3 elevation level (no-op if {@code <= 0})
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void paintShadow(Graphics2D g, int x, int y, int w, int h, int arc, int elevation) {
    if (elevation <= 0 || w <= 0 || h <= 0) {
      return;
    }
    final int e = Math.max(1, elevation);
    final int blur = blurRadius(e);
    final int offsetY = keyOffsetY(e);
    final int padX = blur + 1;
    final int padY = blur + 1;
    final int imgW = w + padX * 2;
    final int imgH = h + padY * 2 + offsetY;

    // Render the body silhouette at full opacity into an ARGB image with margin padX/padY for
    // the blur halo to spread into (plus offsetY worth of extra vertical room so the key
    // downward drop has somewhere to feather without clipping at the image's bottom edge).
    final BufferedImage shadow = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D sg = shadow.createGraphics();
    try {
      sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      // Source alpha tuned so cumulative shadow AFTER blur reads as M3 "lifted from above" —
      // strong enough to feel directional, low enough not to swamp the surface fill at low
      // elevation. The blur disperses this alpha over the halo, so the per-pixel rendered alpha
      // is well below this value.
      final int sourceAlpha = Math.min(180, 60 + e * 12);
      sg.setColor(new Color(0, 0, 0, sourceAlpha));
      sg.fill(new RoundRectangle2D.Float(padX, padY + offsetY, w, h, arc, arc));
    } finally {
      sg.dispose();
    }

    // Two-pass box blur — approximates a Gaussian without the cost of a real Gaussian kernel.
    // First pass at full blur, second at half — gives a smooth, directional falloff with no
    // visible layer-stepping at the corners (which the previous stacked-RoundRect approach had).
    BufferedImage blurred = boxBlur(shadow, blur);
    blurred = boxBlur(blurred, Math.max(1, blur / 2));

    g.drawImage(blurred, x - padX, y - padY, null);
  }

  /**
   * Box-blur the source image with a square kernel of the given radius. {@code radius=1} gives a
   * 3×3 kernel; radius=N gives (2N+1)×(2N+1). {@code EDGE_NO_OP} preserves edge pixels rather than
   * darkening them with the kernel's zero-fill default — important so the shadow image's outer rim
   * doesn't visibly darken at its bounds.
   */
  private static BufferedImage boxBlur(final BufferedImage src, final int radius) {
    if (radius <= 0) {
      return src;
    }
    final int size = radius * 2 + 1;
    final float weight = 1f / (size * size);
    final float[] data = new float[size * size];
    Arrays.fill(data, weight);
    final ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
    return op.filter(src, null);
  }

  private static Color resolveFill(ColorRole surfaceRole, StateLayer overlay) {
    if (surfaceRole == null && overlay == null) {
      return null;
    }
    // GHOST (null surface) pairs against SURFACE for both the overlay base and the on-tint —
    // the chip is assumed to sit on a SURFACE-painted parent.
    ColorRole basis = surfaceRole != null ? surfaceRole : ColorRole.SURFACE;
    Color base = basis.resolve();
    if (overlay == null) {
      return base;
    }
    ColorRole tintRole = basis.on().orElse(ColorRole.ON_SURFACE);
    return overlay.over(base, tintRole);
  }
}
