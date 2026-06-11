package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;

/**
 * The Elwha Material 3 Expressive <strong>linear progress indicator</strong> — a horizontal bar
 * painting the updated-M3 anatomy: a {@code primary} active indicator, a {@code
 * secondaryContainer} track separated by the 4px track-active gap, and the 4px {@code primary}
 * stop-indicator dot at the track's trailing end (determinate only; it hides once the active head
 * reaches it). Outer ends are full-round; the gap-facing inner ends hold the spec's 2px radius.
 *
 * <p>Determinate fills toward {@link #getProgressFraction()}; {@linkplain #setIndeterminate
 * indeterminate} loops the current-M3 two-line cycle (S2 — story #470). The Expressive {@linkplain
 * #setWavy wavy} shape lands in S3 (story #471). Direction follows {@link
 * java.awt.ComponentOrientation} — RTL mirrors the fill and the stop dot.
 *
 * <p>The bar stretches to its layout width (preferred 240px); height is the chrome height (track
 * thickness, plus the wave band when wavy). See {@code elwha-progress-indicator-design.md} §5.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaLinearProgressIndicator extends AbstractElwhaProgressIndicator {

  /** The M3 default layout width, px (the Compose default). */
  public static final int PREFERRED_WIDTH_PX = 240;

  /** The M3 {@code StopSize} — the stop-indicator dot diameter, px. */
  public static final int STOP_SIZE_DEFAULT_PX = 4;

  /** The M3 determinate wave wavelength, px. */
  public static final int WAVELENGTH_DETERMINATE_PX = 40;

  /** The M3 indeterminate wave wavelength, px. */
  public static final int WAVELENGTH_INDETERMINATE_PX = 20;

  /** The M3 linear wave amplitude, px. */
  public static final float WAVE_AMPLITUDE_PX = 3f;

  private static final int MIN_WIDTH_PX = 48;

  private int trackStopIndicatorSize = STOP_SIZE_DEFAULT_PX;
  private ColorRole stopIndicatorColorRole = ColorRole.PRIMARY;

  /**
   * A determinate linear indicator over the default {@code [0, 100]} model at zero.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaLinearProgressIndicator() {
    this(new DefaultBoundedRangeModel(0, 0, 0, 100));
  }

  /**
   * A determinate linear indicator over a fresh model.
   *
   * @param min the model minimum
   * @param max the model maximum
   * @param value the starting value
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaLinearProgressIndicator(final int min, final int max, final int value) {
    this(new DefaultBoundedRangeModel(value, 0, min, max));
  }

  /**
   * A determinate linear indicator over a caller-supplied model (shareable with other
   * range-driven components, e.g. an {@code ElwhaSlider}).
   *
   * @param model the value model (never {@code null})
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaLinearProgressIndicator(final BoundedRangeModel model) {
    super(model, WAVELENGTH_DETERMINATE_PX, WAVELENGTH_INDETERMINATE_PX, WAVE_AMPLITUDE_PX);
  }

  /**
   * Factory — an indeterminate linear indicator.
   *
   * @return the indicator
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaLinearProgressIndicator indeterminate() {
    final ElwhaLinearProgressIndicator indicator = new ElwhaLinearProgressIndicator();
    indicator.setIndeterminate(true);
    return indicator;
  }

  /**
   * Factory — a determinate linear indicator with the Expressive wavy shape.
   *
   * @return the indicator
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaLinearProgressIndicator wavy() {
    final ElwhaLinearProgressIndicator indicator = new ElwhaLinearProgressIndicator();
    indicator.setWavy(true);
    return indicator;
  }

  /**
   * Factory — an indeterminate linear indicator with the Expressive wavy shape.
   *
   * @return the indicator
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaLinearProgressIndicator wavyIndeterminate() {
    final ElwhaLinearProgressIndicator indicator = wavy();
    indicator.setIndeterminate(true);
    return indicator;
  }

  /**
   * The stop-indicator dot diameter, px (M3 {@code StopSize} 4; {@code 0} hides it).
   *
   * @return the dot diameter
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getTrackStopIndicatorSize() {
    return trackStopIndicatorSize;
  }

  /**
   * Sets the stop-indicator dot diameter; {@code 0} hides the dot.
   *
   * @param size the diameter, px (clamped to ≥ 0)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTrackStopIndicatorSize(final int size) {
    this.trackStopIndicatorSize = Math.max(0, size);
    repaint();
  }

  /**
   * The color role of the stop-indicator dot (M3: {@code primary}).
   *
   * @return the stop role
   * @version v0.4.0
   * @since v0.4.0
   */
  public ColorRole getStopIndicatorColorRole() {
    return stopIndicatorColorRole;
  }

  /**
   * Re-roles the stop-indicator dot.
   *
   * @param role the new role (never {@code null})
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setStopIndicatorColorRole(final ColorRole role) {
    this.stopIndicatorColorRole = role;
    repaint();
  }

  /**
   * Preferred size — 240px wide by the chrome height (thickness, plus the reserved wave band when
   * wavy), plus insets.
   *
   * @return the preferred size
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final Insets in = getInsets();
    return new Dimension(
        PREFERRED_WIDTH_PX + in.left + in.right, chromeHeight() + in.top + in.bottom);
  }

  /**
   * Minimum size — a short run at full chrome height.
   *
   * @return the minimum size
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public Dimension getMinimumSize() {
    if (isMinimumSizeSet()) {
      return super.getMinimumSize();
    }
    final Insets in = getInsets();
    return new Dimension(MIN_WIDTH_PX + in.left + in.right, chromeHeight() + in.top + in.bottom);
  }

  /**
   * Maximum size — unbounded width (the bar stretches to its layout), height capped at the chrome
   * height.
   *
   * @return the maximum size
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public Dimension getMaximumSize() {
    if (isMaximumSizeSet()) {
      return super.getMaximumSize();
    }
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }

  private int chromeHeight() {
    final int waveBand = isWavy() ? 2 * Math.round(getWaveAmplitude()) : 0;
    return getTrackThickness() + waveBand;
  }

  /**
   * Paints the linear chrome (design §5): active capsule, gap, track capsule, stop dot — mirrored
   * under RTL.
   *
   * @param g the graphics
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      final Insets in = getInsets();
      final boolean ltr = getComponentOrientation().isLeftToRight();
      if (!ltr) {
        g2.translate(getWidth(), 0);
        g2.scale(-1, 1);
      }
      final float runX = ltr ? in.left : in.right;
      final float runW = getWidth() - in.left - in.right;
      final float midY = in.top + (getHeight() - in.top - in.bottom) / 2f;
      if (runW <= 0f) {
        return;
      }
      if (isIndeterminate()) {
        paintIndeterminate(g2, runX, runW, midY);
      } else {
        paintDeterminate(g2, runX, runW, midY);
      }
    } finally {
      g2.dispose();
    }
  }

  private void paintDeterminate(
      final Graphics2D g2, final float runX, final float runW, final float midY) {
    final float thickness = getTrackThickness();
    final float halfT = thickness / 2f;
    final float top = midY - halfT;
    final float innerR = Math.min(INNER_CORNER_RADIUS_PX, halfT);
    final float gap = getIndicatorTrackGapSize();
    final float fraction = getProgressFraction();
    final float activeW = fraction * runW;
    final boolean hasActive = activeW >= 0.5f;

    float head = runX;
    if (hasActive) {
      final float capsuleW = Math.min(Math.max(activeW, thickness), runW);
      head = runX + capsuleW;
      g2.setColor(getIndicatorColorRole().resolve());
      g2.fill(capsule(runX, top, capsuleW, thickness, halfT, innerR));
    }

    final float trackStart = hasActive ? head + gap : runX;
    final float trackEnd = runX + runW;
    final float trackW = trackEnd - trackStart;
    if (trackW >= 1f) {
      g2.setColor(getTrackColorRole().resolve());
      g2.fill(capsule(trackStart, top, trackW, thickness, hasActive ? innerR : halfT, halfT));
    }

    final float stopD = Math.min(trackStopIndicatorSize, thickness);
    if (stopD > 0f && trackW >= 1f) {
      final float stopX = trackEnd - stopD;
      if (head + gap <= stopX) {
        g2.setColor(stopIndicatorColorRole.resolve());
        g2.fill(new Ellipse2D.Float(stopX, midY - stopD / 2f, stopD, stopD));
      }
    }
  }

  private void paintIndeterminate(
      final Graphics2D g2, final float runX, final float runW, final float midY) {
    final float thickness = getTrackThickness();
    final float halfT = thickness / 2f;
    g2.setColor(getTrackColorRole().resolve());
    g2.fill(capsule(runX, midY - halfT, runW, thickness, halfT, halfT));
  }

  static Path2D.Float capsule(
      final float x,
      final float y,
      final float w,
      final float h,
      final float leftRadius,
      final float rightRadius) {
    final float rl = Math.min(leftRadius, Math.min(w / 2f, h / 2f));
    final float rr = Math.min(rightRadius, Math.min(w / 2f, h / 2f));
    final Path2D.Float p = new Path2D.Float();
    p.moveTo(x + rl, y);
    p.lineTo(x + w - rr, y);
    p.append(new Arc2D.Float(x + w - 2 * rr, y, 2 * rr, 2 * rr, 90, -90, Arc2D.OPEN), true);
    p.lineTo(x + w, y + h - rr);
    p.append(
        new Arc2D.Float(x + w - 2 * rr, y + h - 2 * rr, 2 * rr, 2 * rr, 0, -90, Arc2D.OPEN), true);
    p.lineTo(x + rl, y + h);
    p.append(new Arc2D.Float(x, y + h - 2 * rl, 2 * rl, 2 * rl, 270, -90, Arc2D.OPEN), true);
    p.lineTo(x, y + rl);
    p.append(new Arc2D.Float(x, y, 2 * rl, 2 * rl, 180, -90, Arc2D.OPEN), true);
    p.closePath();
    return p;
  }
}
