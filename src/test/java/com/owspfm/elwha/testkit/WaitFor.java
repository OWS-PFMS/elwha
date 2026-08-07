package com.owspfm.elwha.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;

/**
 * Poll-with-deadline for the {@code gui} tier — the only sanctioned way to wait on real asynchrony
 * (window focus, Robot-driven pipelines, animation settling). Conditions are read on the EDT every
 * {@value #POLL_MS}ms; on timeout the assertion fails with the supplied label, never silently.
 * Sleep-and-hope is banned; so is wall-clock sampling of live animations (that exact pattern
 * produced the incumbent's one reproducible flake).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class WaitFor {

  /** Poll interval. */
  private static final int POLL_MS = 20;

  /** Default deadline. */
  private static final int DEFAULT_TIMEOUT_MS = 4000;

  private WaitFor() {}

  /**
   * Polls the condition on the EDT until true or the default {@value #DEFAULT_TIMEOUT_MS}ms
   * deadline elapses, failing with {@code what} on timeout.
   *
   * @param what plain-English description of the awaited condition (the assertion label)
   * @param condition the condition, evaluated on the EDT
   * @throws Exception if the EDT round-trip is interrupted
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void waitFor(final String what, final BooleanSupplier condition) throws Exception {
    waitFor(what, condition, DEFAULT_TIMEOUT_MS);
  }

  /**
   * Polls the condition on the EDT until true or the deadline elapses, failing with {@code what} on
   * timeout.
   *
   * @param what plain-English description of the awaited condition (the assertion label)
   * @param condition the condition, evaluated on the EDT
   * @param timeoutMs the deadline in milliseconds
   * @throws Exception if the EDT round-trip is interrupted
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void waitFor(
      final String what, final BooleanSupplier condition, final int timeoutMs) throws Exception {
    final long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (onEdt(condition)) {
        return;
      }
      Thread.sleep(POLL_MS);
    }
    assertThat(onEdt(condition)).as(what).isTrue();
  }

  /**
   * Evaluates a boolean read on the EDT and returns it — for one-shot assertions against live
   * component state from a non-EDT test thread.
   *
   * @param read the state read to run on the EDT
   * @return the read's result
   * @throws Exception if the EDT round-trip is interrupted
   * @version v0.5.0
   * @since v0.5.0
   */
  public static boolean onEdt(final BooleanSupplier read) throws Exception {
    final AtomicBoolean value = new AtomicBoolean();
    SwingUtilities.invokeAndWait(() -> value.set(read.getAsBoolean()));
    return value.get();
  }
}
