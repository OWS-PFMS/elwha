package com.owspfm.elwha.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

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
 * @version v0.3.0
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
   * Returns the rounded-rect body shape every surface and surface-aware child must use as their
   * paint boundary — single source of truth for the chassis outer curve, so corner geometry doesn't
   * drift between e.g. {@code ElwhaSurface.paintChildren}'s clip and a child component's own
   * corner-aware paint. Java2D's {@link RoundRectangle2D.Float} internally uses cubic-Bezier circle
   * approximation (k ≈ 0.5523) for its arcs; routing every consumer through this helper ensures
   * pixel-perfect alignment without each call site re-deriving the math.
   *
   * @param width body width in pixels
   * @param height body height in pixels
   * @param arc corner radius in pixels (clamped internally to {@code min(width, height)})
   * @return the rounded-rect shape covering the body
   * @version v0.2.0
   * @since v0.2.0
   */
  public static RoundRectangle2D.Float bodyShape(final int width, final int height, final int arc) {
    final int clampedArc = Math.max(0, Math.min(arc, Math.min(width, height)));
    return new RoundRectangle2D.Float(0f, 0f, width, height, clampedArc, clampedArc);
  }

  /**
   * Per-corner counterpart to {@link #paint(Graphics2D, int, int, int, ColorRole, StateLayer,
   * ColorRole, float)} — paints the surface fill and optional border with four independent corner
   * radii instead of one uniform arc. The connected {@code ElwhaButtonGroup} variant uses this so a
   * segment's outward corners take the group shape while its butted inner corners stay nearly
   * square.
   *
   * <p>Compositing and stroke-inset rules are identical to the uniform overload; the only
   * difference is the body outline, which is the per-corner path from {@link #bodyShape(int, int,
   * CornerRadii)}.
   *
   * @param g the graphics context (not mutated; a copy is made for rendering-hint isolation)
   * @param width the surface width in pixels
   * @param height the surface height in pixels
   * @param radii the four corner radii (each clamped to half the shorter body axis internally)
   * @param surfaceRole the resting surface role; {@code null} for transparent resting fill
   * @param overlay the state layer to composite over the surface, or {@code null} for none
   * @param borderRole the border stroke role; {@code null} suppresses the border
   * @param borderWidthPx the border stroke width in pixels; {@code <= 0} suppresses the border
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void paint(
      Graphics2D g,
      int width,
      int height,
      CornerRadii radii,
      ColorRole surfaceRole,
      StateLayer overlay,
      ColorRole borderRole,
      float borderWidthPx) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

      Color fill = resolveFill(surfaceRole, overlay);
      if (fill != null) {
        g2.setColor(fill);
        g2.fill(roundedRectPath(0f, 0f, width, height, radii));
      }

      if (borderRole != null && borderWidthPx > 0f) {
        // Inset the stroke path by half its width — same rule as the uniform overload — and shrink
        // each corner radius by the same half-width so the stroke's outer edge tracks the fill's
        // corner.
        float inset = borderWidthPx / 2f;
        float strokeW = Math.max(0f, width - borderWidthPx);
        float strokeH = Math.max(0f, height - borderWidthPx);
        CornerRadii strokeRadii =
            CornerRadii.of(
                Math.round(radii.topLeftPx() - inset),
                Math.round(radii.topRightPx() - inset),
                Math.round(radii.bottomRightPx() - inset),
                Math.round(radii.bottomLeftPx() - inset));
        g2.setColor(borderRole.resolve());
        g2.setStroke(new BasicStroke(borderWidthPx));
        g2.draw(roundedRectPath(inset, inset, strokeW, strokeH, strokeRadii));
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Per-corner counterpart to {@link #bodyShape(int, int, int)} — the rounded-rect outline with
   * four independent corner radii. Each radius is clamped to half the shorter body axis, so an
   * over-large radius (a requested pill end-cap) degrades cleanly.
   *
   * @param width body width in pixels
   * @param height body height in pixels
   * @param radii the four corner radii
   * @return the rounded-rect shape covering the body
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Shape bodyShape(final int width, final int height, final CornerRadii radii) {
    return roundedRectPath(0f, 0f, width, height, radii);
  }

  // Cubic-Bezier circle approximation constant — the control-point offset factor for a quarter
  // circle, k = (4/3)·(√2 − 1). Matches the arc geometry RoundRectangle2D uses internally, so a
  // uniform-radii path renders consistently with the int-arc overloads.
  private static final float CORNER_K = 0.5522847498f;

  private static Path2D.Float roundedRectPath(
      final float x, final float y, final float w, final float h, final CornerRadii radii) {
    final float maxR = Math.min(w, h) / 2f;
    final float tl = Math.min(radii.topLeftPx(), maxR);
    final float tr = Math.min(radii.topRightPx(), maxR);
    final float br = Math.min(radii.bottomRightPx(), maxR);
    final float bl = Math.min(radii.bottomLeftPx(), maxR);
    final Path2D.Float path = new Path2D.Float();
    path.moveTo(x + tl, y);
    path.lineTo(x + w - tr, y);
    if (tr > 0f) {
      path.curveTo(x + w - tr + tr * CORNER_K, y, x + w, y + tr - tr * CORNER_K, x + w, y + tr);
    } else {
      path.lineTo(x + w, y);
    }
    path.lineTo(x + w, y + h - br);
    if (br > 0f) {
      path.curveTo(
          x + w, y + h - br + br * CORNER_K, x + w - br + br * CORNER_K, y + h, x + w - br, y + h);
    } else {
      path.lineTo(x + w, y + h);
    }
    path.lineTo(x + bl, y + h);
    if (bl > 0f) {
      path.curveTo(x + bl - bl * CORNER_K, y + h, x, y + h - bl + bl * CORNER_K, x, y + h - bl);
    } else {
      path.lineTo(x, y + h);
    }
    path.lineTo(x, y + tl);
    if (tl > 0f) {
      path.curveTo(x, y + tl - tl * CORNER_K, x + tl - tl * CORNER_K, y, x + tl, y);
    } else {
      path.lineTo(x, y);
    }
    path.closePath();
    return path;
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
