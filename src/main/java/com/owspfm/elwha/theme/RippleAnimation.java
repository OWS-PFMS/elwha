package com.owspfm.elwha.theme;

import java.awt.Point;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The clock behind a touch ripple: where it started, how far it has spread, and when it is done.
 * {@link RipplePainter} draws a ripple at a given origin and progress; this drives that progress
 * from 0 to 1 and repaints the host as it goes.
 *
 * <p>Progress is read from the wall clock rather than accumulated per tick, so a dropped frame
 * shortens the animation instead of stretching it — the ripple always finishes on time.
 *
 * <p><strong>The disabled guard lives here.</strong> {@link #isActive()} answers {@code false} on a
 * disabled host, so a component that gates its ripple paint on this accessor cannot paint
 * interactive chrome on a control that is not accepting interaction — whatever sequence of state
 * changes got it there. A ripple already in flight when the host is disabled simply stops being
 * drawn.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it, exactly as {@link
 * MorphAnimator} and {@link RipplePainter} are.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class RippleAnimation {

  /** M3 ripple spread duration in milliseconds. */
  public static final int TOTAL_MS = 400;

  private static final int TICK_MS = 16;

  private final JComponent host;
  private Point origin;
  private float progress = 1f;
  private Timer timer;

  /**
   * Creates an idle ripple clock for {@code host}.
   *
   * @param host the component to repaint on every tick
   * @version v0.5.0
   * @since v0.5.0
   */
  public RippleAnimation(final JComponent host) {
    this.host = host;
  }

  /**
   * Restarts the ripple from {@code origin}, replacing any ripple in flight.
   *
   * @param origin the spread centre, in the host's coordinate space
   * @version v0.5.0
   * @since v0.5.0
   */
  public void start(final Point origin) {
    this.origin = origin;
    this.progress = 0f;
    if (timer != null && timer.isRunning()) {
      timer.stop();
    }
    final long startNanos = System.nanoTime();
    timer =
        new Timer(
            TICK_MS,
            e -> {
              progress = Math.min(1f, (System.nanoTime() - startNanos) / (TOTAL_MS * 1_000_000f));
              host.repaint();
              if (progress >= 1f) {
                timer.stop();
              }
            });
    timer.setRepeats(true);
    timer.start();
    host.repaint();
  }

  /**
   * Whether a ripple should be drawn this frame — one has been started, it has not finished, and
   * the host is enabled.
   *
   * @return {@code true} when the paint path should draw the ripple
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isActive() {
    return origin != null && progress < 1f && host.isEnabled();
  }

  /**
   * The spread centre of the ripple in flight, in the host's coordinate space.
   *
   * @return the origin, or {@code null} when no ripple has been started
   * @version v0.5.0
   * @since v0.5.0
   */
  public Point origin() {
    return origin;
  }

  /**
   * How far the ripple has spread, in {@code [0, 1]}.
   *
   * @return the current progress; {@code 1} when idle
   * @version v0.5.0
   * @since v0.5.0
   */
  public float progress() {
    return progress;
  }

  /**
   * Stops the timer and lands the ripple finished — the {@code removeNotify} cleanup a host owes
   * every timer it started.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void finish() {
    if (timer != null) {
      timer.stop();
    }
    progress = 1f;
  }
}
