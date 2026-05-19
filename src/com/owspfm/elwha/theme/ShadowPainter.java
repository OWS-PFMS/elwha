package com.owspfm.elwha.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Library-internal helper that paints the M3-aligned drop shadow for an elevated round-rect surface
 * — the round-rect silhouette of the body convolution-blurred into a soft halo, with a directional
 * key-shadow downward offset. Extracted from {@code SurfacePainter.renderShadowImage} with a
 * **redesigned cache strategy**: the cache key is {@code (arc, elevation)} only, not body size; the
 * shadow image is generated once at a canonical body size and {@link Graphics2D#drawImage 9-sliced}
 * onto any host body at paint time. This decouples shadow cost from body resize — the same cached
 * image serves every {@link com.owspfm.elwha.surface.ElwhaSurface} instance and every {@link
 * com.owspfm.elwha.card.ElwhaCard} regardless of dimensions, eliminating the per-frame ConvolveOp
 * recompute that PR #110's {@code setSuspendShadowRecompute} workaround targeted.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it. Library consumers must not
 * depend on this type — its signature and semantics can change without a deprecation cycle.
 *
 * <p>Spike report {@code docs/research/shadow-spike-2026-05-19.md} documents the visual-gate
 * rejection of FlatLaf's {@code FlatDropShadowBorder} (rectangular halo doesn't follow round
 * corners) and the size-independent caching insight this painter adopts in its place.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ShadowPainter {

  /** Maximum elevation level supported; matches {@code ElwhaSurface.MAX_ELEVATION}. */
  public static final int MAX_ELEVATION = 5;

  /**
   * Straight-edge pixels in the canonical body between the two arc-corners on each axis — the
   * source pixels the 9-slice stretches across the center and edges of any larger destination body.
   * Kept small (the center slice carries no falloff, just uniform shadow tint) but non-zero so the
   * middle source rect is well-defined.
   */
  private static final int CANONICAL_STRAIGHT_EDGE = 4;

  private static final Map<CacheKey, SoftReference<BufferedImage>> CACHE =
      new ConcurrentHashMap<>();

  private ShadowPainter() {}

  /**
   * Paints the shadow for an elevated round-rect body of {@code width × height} px at body-local
   * coordinates {@code (0, 0)..(width, height)}. The shadow extends outward into the inset region
   * returned by {@link #shadowInsets(int)}; the caller is responsible for translating {@code g} to
   * the body origin and reserving that inset region around the body.
   *
   * <p>Sub-{@code elevation 1} or zero-sized bodies are no-ops.
   *
   * @param g the graphics context (the painter does {@code drawImage} calls only; rendering hints
   *     on {@code g} are preserved by drawImage's image-sampling semantics)
   * @param width the body width in pixels
   * @param height the body height in pixels
   * @param cornerRadiusPx the body's round-rect corner radius in pixels (clamped internally to
   *     {@code min(width, height) / 2})
   * @param elevationLevel the M3 elevation level (1..{@link #MAX_ELEVATION}); {@code <= 0} is a
   *     no-op
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void paint(
      Graphics2D g, int width, int height, int cornerRadiusPx, int elevationLevel) {
    if (elevationLevel <= 0 || width <= 0 || height <= 0) {
      return;
    }
    final int elevation = Math.min(MAX_ELEVATION, elevationLevel);
    final Insets insets = shadowInsets(elevation);
    final int arc = Math.max(0, Math.min(cornerRadiusPx, Math.min(width, height) / 2));

    // 9-slice requires a non-empty center slice; small bodies fall back to a direct render at exact
    // size. This path is uncached — small interactive primitives are unlikely to hit it on the hot
    // path, and re-rendering at the rare exact size avoids polluting the shared cache.
    final int sliceSide = arc + Math.max(insets.left, insets.top);
    if (width < 2 * sliceSide || height < 2 * sliceSide) {
      final BufferedImage exact = renderShadowAtBodySize(width, height, arc, elevation);
      g.drawImage(exact, -insets.left, -insets.top, null);
      return;
    }

    final BufferedImage canon = canonicalImage(arc, elevation);
    nineSlice(g, canon, width, height, arc, insets);
  }

  /**
   * Returns the inset reserve every elevated body needs around its visible bounds so the
   * convolution-blurred shadow doesn't clip against the host {@link java.awt.Component} bounds.
   * Sized to the larger of the key and ambient shadow extents on each side.
   *
   * @param elevationLevel the M3 elevation level (0..{@link #MAX_ELEVATION})
   * @return the inset reserve, never {@code null}; zero on {@code elevationLevel <= 0}
   * @version v0.2.0
   * @since v0.2.0
   */
  public static Insets shadowInsets(int elevationLevel) {
    if (elevationLevel <= 0) {
      return new Insets(0, 0, 0, 0);
    }
    final int e = Math.min(MAX_ELEVATION, elevationLevel);
    final int padX = Math.max(keyBlurRadius(e), ambientBlurRadius(e));
    final int padTop = padX;
    final int padBottom =
        Math.max(keyBlurRadius(e) + keyOffsetY(e), ambientBlurRadius(e) + ambientOffsetY(e));
    return new Insets(padTop, padX, padBottom, padX);
  }

  /**
   * Drops every cached shadow image from the shared cache. Test-only seam — the cache is otherwise
   * managed by JVM soft-reference eviction.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void clearCache() {
    CACHE.clear();
  }

  /**
   * @return the number of live entries in the shared shadow cache (entries whose {@link
   *     SoftReference} has not been cleared). Diagnostic / demo seam.
   * @version v0.2.0
   * @since v0.2.0
   */
  public static int cacheSize() {
    int live = 0;
    for (SoftReference<BufferedImage> ref : CACHE.values()) {
      if (ref.get() != null) {
        live++;
      }
    }
    return live;
  }

  private static BufferedImage canonicalImage(int arc, int elevation) {
    final CacheKey key = new CacheKey(arc, elevation);
    SoftReference<BufferedImage> ref = CACHE.get(key);
    BufferedImage img = ref != null ? ref.get() : null;
    if (img != null) {
      return img;
    }
    final int canonicalBody = Math.max(2 * arc + CANONICAL_STRAIGHT_EDGE, 16);
    img = renderShadowAtBodySize(canonicalBody, canonicalBody, arc, elevation);
    CACHE.put(key, new SoftReference<>(img));
    return img;
  }

  private static void nineSlice(
      Graphics2D g, BufferedImage canon, int width, int height, int arc, Insets insets) {
    final int canonW = canon.getWidth();
    final int canonH = canon.getHeight();
    final int sliceX = arc + insets.left;
    final int sliceTop = arc + insets.top;
    final int sliceBottom = arc + insets.bottom;

    // Source coordinates (in the canonical image)
    final int srcXMid0 = sliceX;
    final int srcXMid1 = canonW - sliceX;
    final int srcYMid0 = sliceTop;
    final int srcYMid1 = canonH - sliceBottom;

    // Destination coordinates (in body-relative units; the shadow halo extends out by the per-side
    // inset reserve returned by shadowInsets()).
    final int dstX0 = -insets.left;
    final int dstX1 = arc;
    final int dstX2 = width - arc;
    final int dstX3 = width + insets.right;
    final int dstY0 = -insets.top;
    final int dstY1 = arc;
    final int dstY2 = height - arc;
    final int dstY3 = height + insets.bottom;

    // 4 corners (no stretch)
    drawSlice(g, canon, dstX0, dstY0, dstX1, dstY1, 0, 0, srcXMid0, srcYMid0);
    drawSlice(g, canon, dstX2, dstY0, dstX3, dstY1, srcXMid1, 0, canonW, srcYMid0);
    drawSlice(g, canon, dstX0, dstY2, dstX1, dstY3, 0, srcYMid1, srcXMid0, canonH);
    drawSlice(g, canon, dstX2, dstY2, dstX3, dstY3, srcXMid1, srcYMid1, canonW, canonH);
    // 4 edges (stretch along one axis)
    drawSlice(g, canon, dstX1, dstY0, dstX2, dstY1, srcXMid0, 0, srcXMid1, srcYMid0);
    drawSlice(g, canon, dstX1, dstY2, dstX2, dstY3, srcXMid0, srcYMid1, srcXMid1, canonH);
    drawSlice(g, canon, dstX0, dstY1, dstX1, dstY2, 0, srcYMid0, srcXMid0, srcYMid1);
    drawSlice(g, canon, dstX2, dstY1, dstX3, dstY2, srcXMid1, srcYMid0, canonW, srcYMid1);
    // Center (stretch both)
    drawSlice(g, canon, dstX1, dstY1, dstX2, dstY2, srcXMid0, srcYMid0, srcXMid1, srcYMid1);
  }

  private static void drawSlice(
      Graphics2D g,
      BufferedImage src,
      int dx1,
      int dy1,
      int dx2,
      int dy2,
      int sx1,
      int sy1,
      int sx2,
      int sy2) {
    if (dx2 <= dx1 || dy2 <= dy1) {
      return;
    }
    g.drawImage(src, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
  }

  /**
   * Renders the M3-style key+ambient shadow stack for the given body dimensions at the given
   * elevation. Two passes are composited onto a single image:
   *
   * <ul>
   *   <li><strong>Ambient pass</strong> — wide soft halo, no Y offset, lower opacity. Provides the
   *       wider environmental glow that wraps the body silhouette evenly.
   *   <li><strong>Key pass</strong> — narrow sharp band, downward Y offset, higher opacity.
   *       Provides the defined edge below the body that reads as a directional drop from a single
   *       light source above.
   * </ul>
   *
   * <p>This is the per-canonical-cache-fill path and the small-body fallback path. Per-level blur /
   * offset / alpha values are tuned to approximate M3's CSS box-shadow tokens (key opacity 0.30,
   * ambient opacity 0.15) while compensating for box-blur's lower per-pixel density vs a true
   * Gaussian.
   */
  static BufferedImage renderShadowAtBodySize(int w, int h, int arc, int elevation) {
    final int e = Math.max(1, elevation);
    final Insets reserve = shadowInsets(e);
    final int imgW = w + reserve.left + reserve.right;
    final int imgH = h + reserve.top + reserve.bottom;

    final BufferedImage ambient =
        renderSingleShadowPass(
            imgW,
            imgH,
            reserve.left,
            reserve.top,
            w,
            h,
            arc,
            ambientBlurRadius(e),
            ambientOffsetY(e),
            ambientSourceAlpha(e));
    final BufferedImage key =
        renderSingleShadowPass(
            imgW,
            imgH,
            reserve.left,
            reserve.top,
            w,
            h,
            arc,
            keyBlurRadius(e),
            keyOffsetY(e),
            keySourceAlpha(e));

    final Graphics2D cg = ambient.createGraphics();
    try {
      cg.drawImage(key, 0, 0, null);
    } finally {
      cg.dispose();
    }
    return ambient;
  }

  private static BufferedImage renderSingleShadowPass(
      int imgW,
      int imgH,
      int bodyOriginX,
      int bodyOriginY,
      int bodyW,
      int bodyH,
      int arc,
      int blur,
      int offsetY,
      int sourceAlpha) {
    final BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(0, 0, 0, sourceAlpha));
      g.fill(
          new RoundRectangle2D.Float(bodyOriginX, bodyOriginY + offsetY, bodyW, bodyH, arc, arc));
    } finally {
      g.dispose();
    }
    BufferedImage blurred = boxBlur(img, blur);
    blurred = boxBlur(blurred, Math.max(1, blur / 2));
    return blurred;
  }

  private static BufferedImage boxBlur(BufferedImage src, int radius) {
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

  // Key shadow — sharp, narrow, directional drop. Approximates M3's first box-shadow value.
  private static int keyBlurRadius(int elevation) {
    return Math.max(1, Math.min(5, elevation));
  }

  private static int keyOffsetY(int elevation) {
    return Math.max(1, Math.min(5, (elevation + 1) / 2));
  }

  private static int keySourceAlpha(int elevation) {
    return Math.min(180, 110 + elevation * 12);
  }

  // Ambient shadow — wide, soft, no offset. Approximates M3's second box-shadow value (penumbra).
  private static int ambientBlurRadius(int elevation) {
    return Math.max(4, Math.min(16, 3 + elevation * 2));
  }

  private static int ambientOffsetY(int elevation) {
    return Math.max(0, Math.min(4, elevation / 2));
  }

  private static int ambientSourceAlpha(int elevation) {
    return Math.min(110, 60 + elevation * 8);
  }

  private static final class CacheKey {
    final int arc;
    final int elevation;

    CacheKey(int arc, int elevation) {
      this.arc = arc;
      this.elevation = elevation;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof CacheKey k)) {
        return false;
      }
      return arc == k.arc && elevation == k.elevation;
    }

    @Override
    public int hashCode() {
      return Objects.hash(arc, elevation);
    }
  }
}
