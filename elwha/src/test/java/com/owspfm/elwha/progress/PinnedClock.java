package com.owspfm.elwha.progress;

import com.owspfm.elwha.testkit.Pixels;
import java.awt.image.BufferedImage;

/**
 * Indicators whose animation timeline is pinned to an injected clock value.
 *
 * <p>The indeterminate choreography in both indicators is a pure function of elapsed milliseconds,
 * but it reads that elapsed time inline from {@code nowNanos()}. Overriding that one
 * package-private seam turns "what does the bar look like 400 ms into the cycle" into an exact
 * question with an exact answer — no timers, no sleeps, no sampling a live animation.
 *
 * <p>This is what replaces the wall-clock sampling in the retired {@code
 * ElwhaLinearProgressIndeterminateSmoke}, whose "a line is visible in nearly every frame" assertion
 * raced an 84 ms blank window against a 60 ms sample interval and failed roughly three runs in
 * five.
 *
 * <p>The clock starts at a non-zero base so the lazily-established cycle anchor is a real value
 * rather than the {@code 0} sentinel the indicator uses to mean "not yet anchored".
 */
final class PinnedClock {

  /** Clock origin — any non-zero value; the timeline is measured relative to it. */
  private static final long BASE_NANOS = 1_000_000_000L;

  private PinnedClock() {}

  /** A linear indicator reading a pinned clock. */
  static final class Linear extends ElwhaLinearProgressIndicator {

    private long nanos = BASE_NANOS;

    @Override
    long nowNanos() {
      return nanos;
    }

    /** Re-anchors the timeline and advances the clock to {@code elapsedMs} into it. */
    void seekTo(final long elapsedMs) {
      nanos = BASE_NANOS;
      indeterminateElapsedMs();
      nanos = BASE_NANOS + elapsedMs * 1_000_000L;
    }

    /** Renders the frame at {@code elapsedMs} into the indeterminate timeline. */
    BufferedImage frameAt(final long elapsedMs) {
      seekTo(elapsedMs);
      return Pixels.render(this, PREFERRED_WIDTH_PX, getPreferredSize().height);
    }
  }

  /** A circular indicator reading a pinned clock. */
  static final class Circular extends ElwhaCircularProgressIndicator {

    private long nanos = BASE_NANOS;

    @Override
    long nowNanos() {
      return nanos;
    }

    /** Re-anchors the timeline and advances the clock to {@code elapsedMs} into it. */
    void seekTo(final long elapsedMs) {
      nanos = BASE_NANOS;
      indeterminateElapsedMs();
      nanos = BASE_NANOS + elapsedMs * 1_000_000L;
    }

    /** Renders the frame at {@code elapsedMs} into the indeterminate timeline. */
    BufferedImage frameAt(final long elapsedMs) {
      seekTo(elapsedMs);
      final int d = getPreferredSize().width;
      return Pixels.render(this, d, d);
    }

    /**
     * Renders the frame at {@code elapsedMs} over an explicit ground.
     *
     * <p>Needed for absence probes. Anti-aliased edges of the {@code PRIMARY} arc over the default
     * {@code SURFACE} ground pass through blends that sit within the ±10/channel tolerance of
     * {@code SECONDARY_CONTAINER} — so "is any track painted" reads as a false positive on the
     * arc's own cap fringe. Compositing over a dark ground moves every blend away from the light
     * track color instead of through it.
     */
    BufferedImage frameAt(final long elapsedMs, final java.awt.Color ground) {
      seekTo(elapsedMs);
      final int d = getPreferredSize().width;
      return Pixels.render(this, d, d, ground);
    }
  }
}
