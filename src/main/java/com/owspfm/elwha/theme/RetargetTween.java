package com.owspfm.elwha.theme;

import javax.swing.JComponent;

/**
 * A one-float tween over a backing {@link MorphAnimator} that can be <strong>retargeted
 * mid-flight</strong> without jumping.
 *
 * <p>A raw {@code MorphAnimator} runs {@code 0 → 1} and reverses; that is the right shape for a
 * press that returns to rest, and the wrong one for a value chasing a moving destination. Flip a
 * switch and flip it back before the handle lands, and reversing the animator would rewind the
 * <em>whole</em> travel rather than turn around from where the handle currently is. {@link
 * #retarget} instead captures the current interpolated value as the new start, sets the new
 * destination, and re-runs the animator from {@code 0}, so a direction change continues from
 * wherever the motion had reached.
 *
 * <p>Each instance owns one animator and one value; a component with several independently-moving
 * quantities holds one tween per quantity. The {@link Easing} is per-retarget rather than
 * per-tween, because the same quantity often eases differently on the way out than on the way back
 * (M3's emphasized-decelerate / emphasized-accelerate pairing).
 *
 * <p>{@code animate = false} is the reduced-motion and initialization path: the value lands on the
 * target immediately and no timer is scheduled. Repeating a {@code retarget} to the destination
 * already in force is a no-op while animating, so a redundant state push cannot restart a running
 * tween — but it still snaps when {@code animate} is false, which is how a mid-flight tween is
 * forced to its destination.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it, exactly as {@link
 * MorphAnimator} and {@link RipplePainter} are.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class RetargetTween {

  private final MorphAnimator animator;
  private float from;
  private float to;
  private Easing easing = Easing.LINEAR;

  /**
   * Creates a tween resting at {@code initial}, repainting {@code host} while it runs.
   *
   * @param host the component to repaint on every tick; held weakly by the backing animator
   * @param durationMs the initial duration in milliseconds; {@link #retarget} may change it
   * @param initial the value the tween rests at until first retargeted
   * @version v0.5.0
   * @since v0.5.0
   */
  public RetargetTween(final JComponent host, final int durationMs, final float initial) {
    this.animator = new MorphAnimator(host, durationMs);
    this.from = initial;
    this.to = initial;
    this.animator.snapTo(1f);
  }

  /**
   * The current interpolated value — the eased blend between the start captured at the last
   * retarget and the destination.
   *
   * @return the value at this instant
   * @version v0.5.0
   * @since v0.5.0
   */
  public float value() {
    return from + (to - from) * easing.ease(animator.progress());
  }

  /**
   * The destination the tween is travelling toward, which equals {@link #value()} once it lands.
   *
   * @return the current destination
   * @version v0.5.0
   * @since v0.5.0
   */
  public float target() {
    return to;
  }

  /**
   * Sends the value toward {@code target}, continuing from wherever it currently is rather than
   * restarting from the previous origin.
   *
   * <p>A retarget to the destination already in force does nothing while animating — a redundant
   * state push must not restart a tween that is already going the right way. When {@code animate}
   * is {@code false} it still snaps, which is how a mid-flight tween is forced to land.
   *
   * @param target the new destination
   * @param durationMs the duration for this leg in milliseconds
   * @param easing the curve to read the animator's progress through for this leg
   * @param animate {@code false} to land immediately with no timer (reduced motion, or seeding
   *     initial state)
   * @version v0.5.0
   * @since v0.5.0
   */
  public void retarget(
      final float target, final int durationMs, final Easing easing, final boolean animate) {
    if (this.to == target) {
      if (!animate) {
        this.from = target;
        animator.snapTo(1f);
      }
      return;
    }
    this.from = value();
    this.to = target;
    this.easing = easing;
    animator.setDurationMs(durationMs);
    if (animate) {
      animator.snapTo(0f);
      animator.start();
    } else {
      this.from = target;
      animator.snapTo(1f);
    }
  }

  /**
   * Re-seats the tween at an externally-driven value without animating — the drag handoff, where
   * the pointer owns the value until it lets go.
   *
   * @param value the value to rest at
   * @version v0.5.0
   * @since v0.5.0
   */
  public void seed(final float value) {
    this.from = value;
    this.to = value;
    animator.snapTo(1f);
  }

  /**
   * Stops the timer and lands on the target — the {@code removeNotify} cleanup a host owes every
   * animator it started.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void finish() {
    animator.immediateFinish();
  }
}
