package com.owspfm.elwha.testkit;

import java.awt.Robot;
import java.util.function.BooleanSupplier;

/**
 * Delivery-hardened Robot steps for the {@code gui} tier. Under X/Xvfb a synthetic key or button
 * event can be lost against a freshly-mapped window even after event pacing and {@code waitForIdle}
 * (observed on CI as a keypress with no observable effect — the same race class the click-count fix
 * in {@code ElwhaSwitchGuiTest} closed). Each step here presses, waits for the expected observable
 * effect, and re-presses <em>only while nothing observable happened</em> — so a lost event retries
 * but a slow-yet-delivered one is never doubled by the guard.
 *
 * <p>The condition must describe the step's own observable effect (focus arrived, state toggled).
 * Steps whose repetition is not idempotent are exactly why the guard exists: never loop a raw
 * {@code keyPress} without an effect condition.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class GuiSteps {

  private static final int ATTEMPTS = 3;
  private static final int EFFECT_WINDOW_MS = 1500;

  private GuiSteps() {}

  /**
   * Presses and releases {@code keyCode} until {@code effect} reports true, re-pressing at most
   * {@value #ATTEMPTS} times and only while no effect is observable; fails with {@code what} if the
   * effect never materializes.
   *
   * @param robot the tier's robot
   * @param keyCode the {@link java.awt.event.KeyEvent} key code
   * @param what plain-English description of the expected effect (the assertion label)
   * @param effect the step's observable effect, evaluated on the EDT
   * @throws Exception if the EDT round-trip is interrupted
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void keyUntil(
      final Robot robot, final int keyCode, final String what, final BooleanSupplier effect)
      throws Exception {
    for (int attempt = 0; attempt < ATTEMPTS && !WaitFor.onEdt(effect); attempt++) {
      robot.keyPress(keyCode);
      robot.keyRelease(keyCode);
      robot.waitForIdle();
      final long deadline = System.currentTimeMillis() + EFFECT_WINDOW_MS;
      while (System.currentTimeMillis() < deadline && !WaitFor.onEdt(effect)) {
        Thread.sleep(20);
      }
    }
    WaitFor.waitFor(what, effect);
  }

  /**
   * Clicks at the supplied point until {@code effect} reports true, with the same lost-delivery
   * guard as {@link #keyUntil} plus two X-specific hardenings: the host window's focus is restored
   * before each attempt (under a WM-less Xvfb the window can silently lose focus between steps, and
   * focus-on-click semantics need a focused window), and the target point is re-derived per attempt
   * so a late window move cannot stale it.
   *
   * @param robot the tier's robot
   * @param window the window hosting the click target
   * @param target supplier of the click point in screen coordinates, re-read per attempt
   * @param what plain-English description of the expected effect (the assertion label)
   * @param effect the step's observable effect, evaluated on the EDT
   * @throws Exception if the EDT round-trip is interrupted
   * @version v0.5.0
   * @since v0.5.0
   */
  /**
   * Types {@code text} one character at a time, confirming each character's arrival in the supplied
   * live text read before moving on. A character is re-pressed only while the text still shows the
   * pre-press prefix (nothing arrived — the X lost-delivery race); if the text moves to anything
   * else the step fails immediately rather than blind-retyping, since a slow-but- delivered
   * character must never be doubled.
   *
   * @param robot the tier's robot
   * @param text the characters to type
   * @param read live text read, evaluated on the EDT (e.g. the editor document's text)
   * @param what plain-English description of the typing step (the assertion label)
   * @throws Exception if the EDT round-trip is interrupted
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void typeUntil(
      final Robot robot,
      final String text,
      final java.util.function.Supplier<String> read,
      final String what)
      throws Exception {
    String expected = onEdtGet(read);
    for (final char c : text.toCharArray()) {
      final String before = expected;
      expected = expected + c;
      final String want = expected;
      for (int attempt = 0; attempt < ATTEMPTS && before.equals(onEdtGet(read)); attempt++) {
        final int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(c);
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        robot.waitForIdle();
        final long deadline = System.currentTimeMillis() + EFFECT_WINDOW_MS;
        while (System.currentTimeMillis() < deadline && before.equals(onEdtGet(read))) {
          Thread.sleep(20);
        }
      }
      final String finalWant = want;
      WaitFor.waitFor(
          what + " — '" + c + "' delivered",
          () -> {
            try {
              return finalWant.equals(onEdtGetUnchecked(read));
            } catch (final RuntimeException e) {
              return false;
            }
          });
    }
  }

  private static String onEdtGet(final java.util.function.Supplier<String> read) throws Exception {
    final java.util.concurrent.atomic.AtomicReference<String> value =
        new java.util.concurrent.atomic.AtomicReference<>();
    javax.swing.SwingUtilities.invokeAndWait(() -> value.set(read.get()));
    return value.get();
  }

  private static String onEdtGetUnchecked(final java.util.function.Supplier<String> read) {
    try {
      return onEdtGet(read);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static void clickUntil(
      final Robot robot,
      final java.awt.Window window,
      final java.util.function.Supplier<java.awt.Point> target,
      final String what,
      final BooleanSupplier effect)
      throws Exception {
    for (int attempt = 0; attempt < ATTEMPTS && !WaitFor.onEdt(effect); attempt++) {
      if (!WaitFor.onEdt(window::isFocused)) {
        javax.swing.SwingUtilities.invokeAndWait(
            () -> {
              window.toFront();
              window.requestFocus();
            });
        robot.waitForIdle();
      }
      final java.awt.Point point = target.get();
      robot.mouseMove(point.x, point.y);
      robot.waitForIdle();
      robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
      robot.waitForIdle();
      final long deadline = System.currentTimeMillis() + EFFECT_WINDOW_MS;
      while (System.currentTimeMillis() < deadline && !WaitFor.onEdt(effect)) {
        Thread.sleep(20);
      }
    }
    WaitFor.waitFor(what, effect);
  }
}
