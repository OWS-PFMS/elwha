package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.Easing;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;

/**
 * The Elwha Material 3 Expressive <strong>circular progress indicator</strong> — a fixed-size
 * ring painting the updated-M3 anatomy: a {@code primary} active arc sweeping clockwise from 12
 * o'clock over a visible {@code secondaryContainer} track, separated by the 4px track-active gap
 * at <em>both</em> arc ends (cap-aware — the round caps don't eat the gap). At 0% the track is a
 * seamless full ring; at 100% the active arc is.
 *
 * <p>{@linkplain #setIndeterminate Indeterminate} drops the track and loops the current-M3
 * choreography: over a 6000ms cycle the arc grows and shrinks (standard easing) while the figure
 * rotates 1080° per cycle, plus a 360° advance kick every 1500ms over 300ms. The Expressive
 * {@linkplain #setWavy wavy} shape lands in S5 (story #473).
 *
 * <p>Diameter follows the spec redlines: the {@linkplain #setIndicatorSize indicator size} (40px)
 * is the flat-default at 4px thickness; thickness past 4 grows it 1:1 (44 at 8px) and the wavy
 * shape adds 8 (48 / 52). See {@code elwha-progress-indicator-design.md} §5.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaCircularProgressIndicator extends AbstractElwhaProgressIndicator {

  /** The M3 {@code Size} — the flat-default diameter at 4px thickness. */
  public static final int SIZE_DEFAULT_PX = 40;

  /** The M3 {@code WaveSize} growth — the wavy shape adds this to the diameter. */
  public static final int WAVE_DIAMETER_GROWTH_PX = 8;

  /** The M3 circular wave wavelength, px. */
  public static final int WAVELENGTH_PX = 15;

  /** The M3 circular wave amplitude, px. */
  public static final float WAVE_AMPLITUDE_PX = 1.6f;

  private static final int INDETERMINATE_CYCLE_MS = 6000;
  private static final float GLOBAL_ROTATION_PER_CYCLE_DEG = 1080f;
  private static final int ADDITIONAL_ROTATION_DELAY_MS = 1500;
  private static final int ADDITIONAL_ROTATION_MS = 300;
  private static final float ADDITIONAL_ROTATION_DEG = 360f;
  private static final float INDETERMINATE_MAX_TURN_FRACTION = 0.78f;
  private static final float MIN_VISIBLE_SWEEP_DEG = 6f;

  private int indicatorSize = SIZE_DEFAULT_PX;

  /**
   * A determinate circular indicator over the default {@code [0, 100]} model at zero.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaCircularProgressIndicator() {
    this(new DefaultBoundedRangeModel(0, 0, 0, 100));
  }

  /**
   * A determinate circular indicator over a fresh model.
   *
   * @param min the model minimum
   * @param max the model maximum
   * @param value the starting value
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaCircularProgressIndicator(final int min, final int max, final int value) {
    this(new DefaultBoundedRangeModel(value, 0, min, max));
  }

  /**
   * A determinate circular indicator over a caller-supplied model.
   *
   * @param model the value model (never {@code null})
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaCircularProgressIndicator(final BoundedRangeModel model) {
    super(model, WAVELENGTH_PX, WAVELENGTH_PX, WAVE_AMPLITUDE_PX);
  }

  /**
   * Factory — an indeterminate circular indicator.
   *
   * @return the indicator
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaCircularProgressIndicator indeterminate() {
    final ElwhaCircularProgressIndicator indicator = new ElwhaCircularProgressIndicator();
    indicator.setIndeterminate(true);
    return indicator;
  }

  /**
   * Factory — a determinate circular indicator with the Expressive wavy shape.
   *
   * @return the indicator
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaCircularProgressIndicator wavy() {
    final ElwhaCircularProgressIndicator indicator = new ElwhaCircularProgressIndicator();
    indicator.setWavy(true);
    return indicator;
  }

  /**
   * Factory — an indeterminate circular indicator with the Expressive wavy shape.
   *
   * @return the indicator
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaCircularProgressIndicator wavyIndeterminate() {
    final ElwhaCircularProgressIndicator indicator = wavy();
    indicator.setIndeterminate(true);
    return indicator;
  }

  /**
   * The flat-default diameter at 4px thickness (M3 {@code indicatorSize}, 40px) — thickness and
   * the wavy shape grow the painted diameter from here (design §5).
   *
   * @return the base diameter, px
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getIndicatorSize() {
    return indicatorSize;
  }

  /**
   * Sets the flat-default base diameter.
   *
   * @param size the diameter, px (clamped to ≥ 8)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setIndicatorSize(final int size) {
    this.indicatorSize = Math.max(8, size);
    revalidate();
    repaint();
  }

  /**
   * The painted diameter: base size + 1:1 thickness growth past 4px + 8px when wavy.
   *
   * @return the diameter, px
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getDiameter() {
    final int thicknessGrowth = Math.max(0, getTrackThickness() - TRACK_THICKNESS_DEFAULT_PX);
    final int waveGrowth = isWavy() ? WAVE_DIAMETER_GROWTH_PX : 0;
    return indicatorSize + thicknessGrowth + waveGrowth;
  }

  /**
   * Preferred size — the painted diameter plus insets, square.
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
    final int d = getDiameter();
    return new Dimension(d + in.left + in.right, d + in.top + in.bottom);
  }

  /**
   * Minimum size — same as preferred (the ring doesn't shrink).
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
    return getPreferredSize();
  }

  /**
   * Maximum size — same as preferred (a fixed-size widget; no shadow halo in play).
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
    return getPreferredSize();
  }

  /**
   * Paints the ring (design §5–§6): determinate active arc + cap-aware double-gapped track, or
   * the indeterminate grow/shrink rotation.
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
      final float availW = getWidth() - in.left - in.right;
      final float availH = getHeight() - in.top - in.bottom;
      final float d = getDiameter();
      if (availW < 4 || availH < 4) {
        return;
      }
      final float cx = in.left + availW / 2f;
      final float cy = in.top + availH / 2f;
      final float thickness = getTrackThickness();
      final float waveReserve = isWavy() ? getWaveAmplitude() : 0f;
      final float radius = (Math.min(d, Math.min(availW, availH)) - thickness) / 2f - waveReserve;
      if (radius <= 1f) {
        return;
      }
      if (isIndeterminate()) {
        paintIndeterminate(g2, cx, cy, radius, thickness);
      } else {
        paintDeterminate(g2, cx, cy, radius, thickness);
      }
    } finally {
      g2.dispose();
    }
  }

  private void paintDeterminate(
      final Graphics2D g2, final float cx, final float cy, final float radius, final float thickness) {
    final float fraction = getProgressFraction();
    final float activeSweep = fraction * 360f;
    final float capDeg = (float) Math.toDegrees((thickness / 2f) / radius);
    final float gapDeg =
        (float) Math.toDegrees((getIndicatorTrackGapSize() + thickness) / radius);

    if (activeSweep >= 359.5f) {
      g2.setColor(getIndicatorColorRole().resolve());
      strokeFullRing(g2, cx, cy, radius, thickness);
      return;
    }
    if (activeSweep < 0.5f) {
      g2.setColor(getTrackColorRole().resolve());
      strokeFullRing(g2, cx, cy, radius, thickness);
      return;
    }

    g2.setColor(getIndicatorColorRole().resolve());
    paintArcSpan(g2, cx, cy, radius, thickness, 90f, activeSweep, capDeg);

    final float trackSweep = 360f - activeSweep - 2f * gapDeg;
    if (trackSweep > 1f) {
      g2.setColor(getTrackColorRole().resolve());
      paintArcSpan(g2, cx, cy, radius, thickness, 90f - activeSweep - gapDeg, trackSweep, capDeg);
    }
  }

  private void paintIndeterminate(
      final Graphics2D g2, final float cx, final float cy, final float radius, final float thickness) {
    final long elapsed = indeterminateElapsedMs();
    final float cycleT = (elapsed % INDETERMINATE_CYCLE_MS) / (float) INDETERMINATE_CYCLE_MS;
    final float head = Easing.STANDARD.ease(Math.min(1f, cycleT * 2f));
    final float tail = Easing.STANDARD.ease(Math.max(0f, cycleT * 2f - 1f));
    final float sweep =
        Math.max(MIN_VISIBLE_SWEEP_DEG, (head - tail) * INDETERMINATE_MAX_TURN_FRACTION * 360f);

    final float globalRotation =
        (elapsed / (float) INDETERMINATE_CYCLE_MS) * GLOBAL_ROTATION_PER_CYCLE_DEG;
    final float startAngle =
        90f
            - tail * INDETERMINATE_MAX_TURN_FRACTION * 360f
            - globalRotation
            - additionalRotationDeg(elapsed);

    final float capDeg = (float) Math.toDegrees((thickness / 2f) / radius);
    g2.setColor(getIndicatorColorRole().resolve());
    paintArcSpan(g2, cx, cy, radius, thickness, startAngle, sweep, capDeg);
  }

  private static float additionalRotationDeg(final long elapsedMs) {
    final long kicks = elapsedMs / ADDITIONAL_ROTATION_DELAY_MS;
    float total = 0f;
    for (long i = 1; i <= kicks; i++) {
      final float t =
          Math.min(1f, (elapsedMs - i * ADDITIONAL_ROTATION_DELAY_MS) / (float) ADDITIONAL_ROTATION_MS);
      total += Easing.STANDARD.ease(t) * ADDITIONAL_ROTATION_DEG;
    }
    return total;
  }

  /**
   * Strokes a visual arc span with round caps, insetting the drawn endpoints by half a cap so the
   * caps land exactly on the visual ends; degenerate spans collapse to a dot.
   */
  private void paintArcSpan(
      final Graphics2D g2,
      final float cx,
      final float cy,
      final float radius,
      final float thickness,
      final float visualStartDeg,
      final float visualSweepDeg,
      final float capDeg) {
    final float drawnSweep = visualSweepDeg - 2f * capDeg;
    if (drawnSweep <= 0f) {
      final double midDeg = Math.toRadians(visualStartDeg - visualSweepDeg / 2f);
      final float px = cx + (float) (radius * Math.cos(midDeg));
      final float py = cy - (float) (radius * Math.sin(midDeg));
      g2.fill(
          new Ellipse2D.Float(px - thickness / 2f, py - thickness / 2f, thickness, thickness));
      return;
    }
    final Stroke previous = g2.getStroke();
    g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(
        new Arc2D.Float(
            cx - radius,
            cy - radius,
            radius * 2f,
            radius * 2f,
            visualStartDeg - capDeg,
            -drawnSweep,
            Arc2D.OPEN));
    g2.setStroke(previous);
  }

  private void strokeFullRing(
      final Graphics2D g2, final float cx, final float cy, final float radius, final float thickness) {
    final Stroke previous = g2.getStroke();
    g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    g2.setStroke(previous);
  }
}
