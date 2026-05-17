package com.owspfm.elwha.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
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
   * multi-layer soft shadow doesn't get clipped by the {@link java.awt.Component} bounds. Top stays
   * at 1 px because the M3 shadow shape sits below + sides of the surface; lateral grows at {@code
   * e/2 + 1}; bottom grows at {@code e + 2}. {@code elevation <= 0} returns zero insets.
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
    int e = elevation;
    return new Insets(1, e / 2 + 1, e + 2, e / 2 + 1);
  }

  /**
   * Paints the M3-aligned drop shadow for an elevated surface. The shadow is a soft-blur stack of
   * low-alpha {@link RoundRectangle2D} layers approximating a Gaussian drop without a {@link
   * java.awt.image.ConvolveOp} pass, plus a slightly sharper key layer directly below the surface.
   * Every layer's top sits 1 px below {@code y} so the surface's top silhouette stays crisp at any
   * elevation.
   *
   * <p>Geometry scales with {@code elevation}; at low elevation the layers still fan out to give a
   * perceptible soft halo around the surface rather than collapsing to a hard 1 px line.
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
    if (elevation <= 0) {
      return;
    }
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // M3 shadow shape: ambient soft halo + key drop directly below. Scaling tuned so
      // elevation=1 still produces a visible (not hard-edged) shadow.
      int e = Math.max(1, elevation);
      int layers = 6;
      // Spread fans the shadow laterally — at e=1 we still want ~4 px halo, so the floor is 2.
      float maxSpread = Math.max(2f, (float) e * 0.85f);
      // Vertical offset of the deepest layer; grows with elevation.
      float maxOffsetY = Math.max(2f, (float) e * 1.2f);
      // Per-layer alpha — kept low so the stack accumulates into a soft falloff. Caps at ~22.
      int perLayerAlpha = Math.min(22, 8 + e * 2);
      for (int i = 1; i <= layers; i++) {
        float t = (float) i / (float) layers;
        float spread = maxSpread * t;
        float offsetY = maxOffsetY * t;
        g2.setColor(new Color(0, 0, 0, perLayerAlpha));
        g2.fill(
            new RoundRectangle2D.Float(
                x - spread,
                y + 1f + Math.max(0f, offsetY - spread),
                w + 2f * spread,
                h + offsetY,
                arc + spread,
                arc + spread));
      }
      // Key drop: tighter, directly below the surface — drives the "lifted" perception and reads
      // as a clear bottom-edge separator at every elevation.
      int keyOffset = Math.max(1, e / 2);
      int keyAlpha = Math.min(60, 26 + e * 4);
      g2.setColor(new Color(0, 0, 0, keyAlpha));
      g2.fill(new RoundRectangle2D.Float(x, y + keyOffset + 1f, w, h, arc, arc));
    } finally {
      g2.dispose();
    }
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
