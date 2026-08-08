package com.owspfm.elwha.theme;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Answers "is the pointer still on this component?" when {@code mouseExited} cannot be trusted.
 *
 * <p>Swing delivers {@code mouseExited} reliably for a pointer that walks off a component. It does
 * not deliver one when the component stops being under a pointer that never moved — the window
 * scrolls, a layout runs, an overlay opens on top, the component itself is hidden. A component that
 * paints hover chrome and only listens for crossings therefore keeps painting a hover that is no
 * longer true, and because the hover state also gates the pressed state, a stale hover can strand a
 * pressed one with it.
 *
 * <p>The fix is a low-frequency poll of the real pointer position, armed while the component
 * believes it is hovered and disarmed the moment it learns otherwise. Polling is deliberately the
 * fallback and not the primary path: crossings are exact and free, the poll is a 100 ms correction
 * that only runs while a hover is actually being claimed.
 *
 * <p>The host keeps its own hover and press flags — they are what its paint reads — and hands this
 * tracker a callback to run when the pointer turns out to have left. The tracker owns only the
 * timer and the geometry test.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it, exactly as {@link
 * MorphAnimator} and {@link RipplePainter} are.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class HoverTracker {

  /** Correction interval. Long enough to be free, short enough that a stale hover is not seen. */
  private static final int POLL_INTERVAL_MS = 100;

  private final JComponent host;
  private final Runnable onPointerGone;
  private Timer pollTimer;

  /**
   * Creates a tracker for {@code host}.
   *
   * @param host the component whose bounds the pointer is tested against
   * @param onPointerGone run on the EDT when the poll finds the pointer is no longer over the host
   *     — the host clears its own hover and press flags here. Never run while the pointer is still
   *     inside, so it is safe for it to repaint unconditionally
   * @version v0.5.0
   * @since v0.5.0
   */
  public HoverTracker(final JComponent host, final Runnable onPointerGone) {
    this.host = host;
    this.onPointerGone = onPointerGone;
  }

  /**
   * Arms the poll, if it is not already running. Call when the host takes on a hover.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void start() {
    if (pollTimer != null && pollTimer.isRunning()) {
      return;
    }
    pollTimer = new Timer(POLL_INTERVAL_MS, e -> poll());
    pollTimer.setRepeats(true);
    pollTimer.start();
  }

  /**
   * Disarms the poll. Call whenever the host clears its hover — including from {@code
   * removeNotify}, where the poll is the only path that would otherwise have corrected the state.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void stop() {
    if (pollTimer != null) {
      pollTimer.stop();
    }
  }

  /**
   * Whether {@code p}, in the host's own coordinate space, is inside the host's bounds.
   *
   * @param p a point in host coordinates
   * @return {@code true} when the point is within the host's width and height
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean containsPoint(final Point p) {
    return p.x >= 0 && p.y >= 0 && p.x < host.getWidth() && p.y < host.getHeight();
  }

  /**
   * Whether the pointer is over the host right now, despite the {@code mouseExited} that prompted
   * the question.
   *
   * <p>Swing fires {@code mouseExited} when the pointer crosses onto a <em>child</em> of the host,
   * which is not a real exit as far as the host's own hover chrome is concerned. Asking the OS
   * where the pointer actually is distinguishes the two.
   *
   * @param event the crossing event, whose screen coordinates stand in when the OS will not answer
   * @return {@code true} when the pointer is still within the host
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean cursorStillInside(final MouseEvent event) {
    if (!host.isShowing()) {
      return false;
    }
    final PointerInfo info = MouseInfo.getPointerInfo();
    final Point screenPoint =
        info != null ? info.getLocation() : new Point(event.getXOnScreen(), event.getYOnScreen());
    final Point local = new Point(screenPoint);
    SwingUtilities.convertPointFromScreen(local, host);
    return containsPoint(local);
  }

  private void poll() {
    if (!host.isShowing()) {
      stop();
      onPointerGone.run();
      return;
    }
    final PointerInfo info = MouseInfo.getPointerInfo();
    if (info == null) {
      // A headless or locked-screen moment; the next tick asks again rather than guessing.
      return;
    }
    final Point local = new Point(info.getLocation());
    SwingUtilities.convertPointFromScreen(local, host);
    if (!containsPoint(local)) {
      stop();
      onPointerGone.run();
    }
  }
}
