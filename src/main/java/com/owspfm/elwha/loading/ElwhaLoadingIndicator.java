package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ColorRole;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/**
 * The Elwha Material 3 Expressive <strong>loading indicator</strong> — a filled rounded-polygon
 * that continuously rotates while morphing through a sequence of M3 shape-library forms. For short
 * (&lt; ~5s) <em>indeterminate</em> waits — pull-to-refresh, inline-button loading — which M3 says
 * "should replace most uses of the indeterminate circular progress indicator." A separate component
 * from the {@linkplain com.owspfm.elwha.progress progress indicators}, not a variant of them.
 *
 * <p><strong>Spike skeleton (S1).</strong> This revision stands up the package and the
 * self-contained radius-profile shape-morph engine ({@link RoundedPolygonShape} + {@link
 * ShapeMorph}, design {@code elwha-loading-indicator-design.md} §2) and renders a single static
 * morph phase to prove the paint path. The indeterminate choreography (clock, spin, the full
 * 7-shape cycle), the contained configuration, determinate mode, sizing, and accessibility arrive
 * in S2–S5.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaLoadingIndicator extends JComponent {

  /** The M3 {@code ActiveSize} — the active indicator's box, px. */
  public static final int INDICATOR_SIZE_DEFAULT_PX = 38;

  private int indicatorSize = INDICATOR_SIZE_DEFAULT_PX;
  private ColorRole indicatorColorRole = ColorRole.PRIMARY;

  /**
   * A standard loading indicator.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaLoadingIndicator() {
    setOpaque(false);
    setFocusable(false);
  }

  /**
   * The active indicator's box size, px.
   *
   * @return the indicator size
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getIndicatorSize() {
    return indicatorSize;
  }

  /**
   * Sets the active indicator's box size.
   *
   * @param size the size, px (clamped to ≥ 8)
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setIndicatorSize(final int size) {
    this.indicatorSize = Math.max(8, size);
    revalidate();
    repaint();
  }

  /**
   * The active indicator's color role.
   *
   * @return the indicator color role
   * @version v0.5.0
   * @since v0.5.0
   */
  public ColorRole getIndicatorColorRole() {
    return indicatorColorRole;
  }

  /**
   * Sets the active indicator's color role.
   *
   * @param role the color role (never {@code null})
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setIndicatorColorRole(final ColorRole role) {
    if (role == null) {
      throw new NullPointerException("role");
    }
    this.indicatorColorRole = role;
    repaint();
  }

  /**
   * The radius profile to paint this frame. The spike renders the first indeterminate shape
   * statically; later stories make this time- or progress-driven.
   *
   * @return the current radius profile
   * @version v0.5.0
   * @since v0.5.0
   */
  protected float[] currentProfile() {
    return LoadingShapes.SOFT_BURST.radii();
  }

  /**
   * The rotation applied to the shape this frame, radians. The spike is static ({@code 0}).
   *
   * @return the rotation, radians
   * @version v0.5.0
   * @since v0.5.0
   */
  protected double currentRotationRad() {
    return 0.0;
  }

  /**
   * Preferred size — the indicator box plus insets, square.
   *
   * @return the preferred size
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final Insets in = getInsets();
    return new Dimension(indicatorSize + in.left + in.right, indicatorSize + in.top + in.bottom);
  }

  /**
   * Minimum size — same as preferred.
   *
   * @return the minimum size
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getMinimumSize() {
    return isMinimumSizeSet() ? super.getMinimumSize() : getPreferredSize();
  }

  /**
   * Maximum size — same as preferred (a fixed-size widget).
   *
   * @return the maximum size
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getMaximumSize() {
    return isMaximumSizeSet() ? super.getMaximumSize() : getPreferredSize();
  }

  /**
   * Paints the active indicator (design §2, §5).
   *
   * @param g the graphics
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      final Insets in = getInsets();
      final float availW = getWidth() - in.left - in.right;
      final float availH = getHeight() - in.top - in.bottom;
      if (availW < 6f || availH < 6f) {
        return;
      }
      final float box = Math.min(Math.min(availW, availH), indicatorSize);
      final float cx = in.left + availW / 2f;
      final float cy = in.top + availH / 2f;
      final float scale = box / 2f - 1f;
      if (scale <= 1f) {
        return;
      }
      paintIndicator(g2, cx, cy, scale);
    } finally {
      g2.dispose();
    }
  }

  /** Fills the current morph profile at the given center and scale. */
  private void paintIndicator(
      final Graphics2D g2, final float cx, final float cy, final float scale) {
    final Path2D.Float path =
        ShapeMorph.toPath(currentProfile(), cx, cy, scale, currentRotationRad());
    g2.setColor(indicatorColorRole.resolve());
    g2.fill(path);
  }
}
