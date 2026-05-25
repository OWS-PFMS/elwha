package com.owspfm.elwha.theme;

import java.awt.Toolkit;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Tiny {@link javax.swing.Timer} wrapper that drives a single morph animation in {@code [0, 1]}.
 * One {@code MorphAnimator} owns one animation slot — an {@code ElwhaButton} carrying concurrent
 * press / select / width morphs holds three of them, each with its own duration. Consumers query
 * {@link #progress()} from {@code paintComponent} and read it back through whatever {@link Easing}
 * the caller wants.
 *
 * <p>State lives here; rendering is the consumer's job (typically delegated to {@link
 * ShapeMorphPainter}). The animator drives {@link JComponent#repaint() repaint} on its host every
 * tick via a weak reference, so a host that forgets to call {@link #stop()} in {@code removeNotify}
 * is cleaned up by GC rather than leaking a Timer thread.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it.
 *
 * <p><strong>Reduced motion.</strong> When {@link #setReducedMotion(boolean) reduced motion} is on,
 * {@link #start()} and {@link #reverse()} snap {@link #progress()} to the destination value and the
 * Timer never schedules ticks — visually identical to the static v1 behavior. The flag is static
 * and global per the design doc §10; the {@code ElwhaTheme.config(...).reducedMotion(...)} wiring
 * is Phase 5.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class MorphAnimator {

  /** {@code motion.duration.short3} — 150 ms; the press-morph duration per design doc §3. */
  public static final int SHORT3_MS = 150;

  /** {@code motion.duration.medium2} — 300 ms; the select-flip duration per design doc §3. */
  public static final int MEDIUM2_MS = 300;

  private static final int TICK_INTERVAL_MS = 16;

  private static volatile boolean reducedMotion;
  // #176 Phase 5 — workbench-driven global duration multiplier. Default {@code 1.0} (the §3
  // pinned durations apply verbatim); the Showcase's Animation control group raises this to
  // {@code 2.0} / {@code 5.0} so an operator can watch a single morph cycle without chasing a
  // fast click. Not part of the consumer-facing API (the §3 durations are pinned for a reason);
  // the multiplier is workbench-only.
  private static volatile float durationMultiplier = 1f;

  // #176 Phase 2 / Phase 5 — auto-detect OS reduced-motion at class-load (design doc §9). Each
  // platform check is wrapped independently so a failure on one (a JVM without an AWT Toolkit, a
  // Linux box without {@code gsettings}, a Windows box with the property unset) doesn't shadow
  // the others. Consumers can still flip the global toggle via {@link
  // #setReducedMotion(boolean)} regardless of platform — or via {@code
  // ElwhaTheme.config(...).reducedMotion(...)} for the public surface.
  static {
    if (detectMacReducedMotion() || detectWindowsReducedMotion() || detectLinuxReducedMotion()) {
      reducedMotion = true;
    }
  }

  private static boolean detectMacReducedMotion() {
    try {
      final Object value = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.reduceMotion");
      return Boolean.TRUE.equals(value);
    } catch (final RuntimeException ignored) {
      return false;
    }
  }

  private static boolean detectWindowsReducedMotion() {
    // The JDK exposes the "Show animations in Windows" Ease-of-Access toggle through this
    // desktop property — its value is the closest signal Windows offers; when animations are
    // disabled, motion-heavy effects across Swing already collapse. Inverted on read because
    // the property returns "are animations enabled," not "is motion reduced."
    try {
      final Object value =
          Toolkit.getDefaultToolkit().getDesktopProperty("win.text.animationsEnabled");
      return Boolean.FALSE.equals(value);
    } catch (final RuntimeException ignored) {
      return false;
    }
  }

  private static boolean detectLinuxReducedMotion() {
    // GNOME exposes the toggle through {@code gsettings get org.gnome.desktop.interface
    // enable-animations}. Wrapped in process-level safety: short timeout, swallow every
    // exception, OS-name guard so other JVMs don't spawn the process. KDE / others don't have
    // a portable equivalent — users on those desktops fall back to the
    // {@code ElwhaTheme.config(...).reducedMotion(...)} explicit override.
    final String osName = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
    if (!osName.contains("linux")) {
      return false;
    }
    try {
      final Process p =
          new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "enable-animations")
              .redirectErrorStream(true)
              .start();
      if (!p.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
        p.destroyForcibly();
        return false;
      }
      final String output = new String(p.getInputStream().readAllBytes()).trim();
      // gsettings prints "true" or "false" — animations enabled means motion isn't reduced.
      return "false".equalsIgnoreCase(output);
    } catch (final Exception ignored) {
      return false;
    }
  }

  private final WeakReference<JComponent> hostRef;
  private final Timer timer;

  private int durationMs;
  private float progress;
  private float target;
  private long lastTickNanos;

  /**
   * Creates an animator hosted by {@code host} (repainted on every tick) with the given duration.
   *
   * @param host the component to repaint as the animation ticks; held weakly
   * @param durationMs the animation duration in milliseconds; clamped to {@code >= 1}
   * @throws NullPointerException if {@code host} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public MorphAnimator(final JComponent host, final int durationMs) {
    if (host == null) {
      throw new NullPointerException("host");
    }
    this.hostRef = new WeakReference<>(host);
    this.durationMs = Math.max(1, durationMs);
    this.timer = new Timer(TICK_INTERVAL_MS, e -> tick());
    this.timer.setRepeats(true);
  }

  /**
   * Globally toggles reduced-motion mode for every {@code MorphAnimator} in the JVM. When on,
   * {@link #start()} and {@link #reverse()} snap to the destination without scheduling ticks. The
   * flag does not retroactively cancel a running animation — {@link #stop()} is the explicit kill
   * path.
   *
   * @param reduced whether reduced-motion is on
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void setReducedMotion(final boolean reduced) {
    reducedMotion = reduced;
  }

  /**
   * Returns whether reduced-motion mode is on.
   *
   * @return {@code true} if morph animations should snap to the destination
   * @version v0.3.0
   * @since v0.3.0
   */
  public static boolean isReducedMotion() {
    return reducedMotion;
  }

  /**
   * Sets the global animation-duration multiplier — every subsequent {@link #start()} / {@link
   * #reverse()} call uses {@code duration * multiplier} as its effective duration. The default is
   * {@code 1.0}. Used by The Elwha Showcase's Animation control group to slow morphs down for
   * observation (1× / 2× / 5×). Not part of the consumer-facing API — the §3 durations are pinned
   * values, not a tuning knob.
   *
   * @param multiplier the global multiplier; clamped to {@code >= 0.1} to defend against a
   *     zero-or-negative tick step
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void setDurationMultiplier(final float multiplier) {
    durationMultiplier = Math.max(0.1f, multiplier);
  }

  /**
   * Returns the current global animation-duration multiplier.
   *
   * @return the multiplier; {@code 1.0} by default
   * @version v0.3.0
   * @since v0.3.0
   */
  public static float durationMultiplier() {
    return durationMultiplier;
  }

  /**
   * Returns the current animation phase in {@code [0, 1]}. Stable between ticks; safe to call from
   * {@code paintComponent}.
   *
   * @return the current progress
   * @version v0.3.0
   * @since v0.3.0
   */
  public float progress() {
    return progress;
  }

  /**
   * Returns the destination value the animator is animating toward — {@code 1.0} after the most
   * recent {@link #start()} call, {@code 0.0} after the most recent {@link #reverse()} or {@link
   * #stop()} call. Useful to pick a per-direction {@link Easing} (e.g. M3 press uses {@code
   * emphasized.decelerate} going in and {@code emphasized.accelerate} going out).
   *
   * @return the current target value
   * @version v0.3.0
   * @since v0.3.0
   */
  public float target() {
    return target;
  }

  /**
   * Returns whether the Timer is currently scheduled.
   *
   * @return {@code true} if the animation is running
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isRunning() {
    return timer.isRunning();
  }

  /**
   * Changes the animation duration. Affects subsequent {@link #start()} / {@link #reverse()} calls;
   * a running animation continues at its current rate to avoid a visible jump.
   *
   * @param durationMs the new duration in milliseconds; clamped to {@code >= 1}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setDurationMs(final int durationMs) {
    this.durationMs = Math.max(1, durationMs);
  }

  /**
   * Animates {@link #progress()} from its current value toward {@code 1.0}. If reduced-motion is
   * on, snaps to {@code 1.0} without scheduling ticks.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void start() {
    animateTo(1f);
  }

  /**
   * Animates {@link #progress()} from its current value toward {@code 0.0}. If reduced-motion is
   * on, snaps to {@code 0.0} without scheduling ticks.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void reverse() {
    animateTo(0f);
  }

  /**
   * Stops the animation in place and resets {@link #progress()} to {@code 0.0}. Suitable for {@code
   * removeNotify} cleanup.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void stop() {
    timer.stop();
    progress = 0f;
    target = 0f;
  }

  /**
   * Stops the animation and snaps {@link #progress()} to the current {@code target} — useful when
   * the host wants to short-circuit the in-flight tween without resetting.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void immediateFinish() {
    timer.stop();
    progress = target;
    repaintHost();
  }

  /**
   * Stops the animation and snaps {@link #progress()} to the given value — useful for setting an
   * initial state at construction or first {@code addNotify}, and for the §15.7 disabled-button
   * path where a programmatic {@code setSelected(...)} should land at the new state without
   * animating. The value is clamped to {@code [0, 1]}; both {@link #progress()} and {@link
   * #target()} are set so a subsequent {@link #start()} or {@link #reverse()} animates from the new
   * resting point.
   *
   * @param value the value to snap to; clamped to {@code [0, 1]}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void snapTo(final float value) {
    final float clamped = Math.max(0f, Math.min(1f, value));
    timer.stop();
    progress = clamped;
    target = clamped;
    repaintHost();
  }

  private void animateTo(final float to) {
    this.target = to;
    if (reducedMotion) {
      progress = to;
      timer.stop();
      repaintHost();
      return;
    }
    if (progress == to) {
      timer.stop();
      return;
    }
    lastTickNanos = System.nanoTime();
    if (!timer.isRunning()) {
      timer.start();
    }
  }

  private void tick() {
    final long now = System.nanoTime();
    final float deltaSeconds = (now - lastTickNanos) / 1_000_000_000f;
    lastTickNanos = now;
    // #176 Phase 5 — the global durationMultiplier stretches effective duration so the Showcase
    // can slow morphs down for observation without touching the per-animator durationMs.
    final float effectiveDurationMs = durationMs * durationMultiplier;
    final float rate = 1000f / effectiveDurationMs;
    final float step = deltaSeconds * rate;
    if (target > progress) {
      progress = Math.min(target, progress + step);
    } else {
      progress = Math.max(target, progress - step);
    }
    if (progress == target) {
      timer.stop();
    }
    repaintHost();
  }

  private void repaintHost() {
    final JComponent host = hostRef.get();
    if (host == null) {
      timer.stop();
      return;
    }
    host.repaint();
  }
}
