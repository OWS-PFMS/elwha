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
}
