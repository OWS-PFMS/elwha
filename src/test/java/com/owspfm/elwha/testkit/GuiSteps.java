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

  // The window must out-wait a stalled CI runner, not just event latency: a re-press after a
  // delivered-but-slow event corrupts state unrecoverably (a doubled keystroke poisons a typed
  // filter; an extra Down moves a highlight the test just baselined), while a longer window only
  // delays the retry of a genuinely lost event. 1500ms lost that bet on a loaded runner (#585).
  private static final int EFFECT_WINDOW_MS = 5000;

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
   * Types {@code text} one character at a time, confirming each character's arrival in the supplied
   * live text read before moving on. Delivery is judged content-agnostically — the text changed
   * <em>and</em> now ends with the typed character — so it holds whether the editor appends or
   * replaces a selection (a combo's select-on-focus makes the first keystroke replace). A character
   * is re-pressed only while the text is completely unchanged (nothing arrived — the X
   * lost-delivery race), so a slow-but-delivered character is never doubled. After the last
   * character the whole typed string is asserted to survive as the text's suffix, and every failure
   * message carries the actual text — a corrupted delivery diagnoses itself (#585).
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
    for (final char c : text.toCharArray()) {
      final String before = onEdtGet(read);
      final String suffix = String.valueOf(c);
      // WaitFor evaluates its condition ON the EDT — read directly there, never invokeAndWait.
      final BooleanSupplier delivered =
          () -> {
            final String now = read.get();
            return !now.equals(before) && now.endsWith(suffix);
          };
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
      WaitFor.waitFor(
          what + " — '" + c + "' delivered", delivered, () -> "text is '" + read.get() + "'");
    }
    WaitFor.waitFor(
        what + " — the full string survives intact",
        () -> read.get().endsWith(text),
        () -> "text is '" + read.get() + "'");
  }

  private static String onEdtGet(final java.util.function.Supplier<String> read) throws Exception {
    final java.util.concurrent.atomic.AtomicReference<String> value =
        new java.util.concurrent.atomic.AtomicReference<>();
    javax.swing.SwingUtilities.invokeAndWait(() -> value.set(read.get()));
    return value.get();
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

  /**
   * Returns the x for a per-test frame slot, wrapped so the frame always lands on the display.
   *
   * <p>Gui tests give each test method its own screen position because under bare Xvfb the previous
   * test's window can still be tearing down at the shared one, and events aimed there go to the
   * dying window. The wrap is the part that is easy to leave out and expensive to debug: a class
   * whose slots march past the display width places a frame off-screen, where {@code Robot}'s
   * clicks land on nothing. Nothing reports an error — the test simply never observes its effect
   * and times out, so the failure reads as a bug in whatever the test was measuring.
   *
   * <p>Found the hard way in #688: adding a fifth test to {@code ElwhaTextFieldGuiTest} shifted the
   * slots by one, put slot 4 at x=2340 on CI's 1920-wide Xvfb screen, and produced a focus test
   * that failed with no {@code MOUSE_PRESSED} ever reaching the application.
   *
   * @param slot the zero-based slot, typically from a static counter incremented per test
   * @param stride the horizontal spacing between slots, at least the frame's width
   * @return an x inside the display
   * @version v0.5.0
   * @since v0.5.0
   */
  public static int slotX(final int slot, final int stride) {
    final int margin = 100;
    final int usable = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width - margin;
    final int columns = Math.max(1, usable / stride);
    return margin + (slot % columns) * stride;
  }
}
