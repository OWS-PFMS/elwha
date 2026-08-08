package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tier A coverage of which thread {@link ElwhaTheme#install} mutates the look-and-feel on (#657).
 *
 * <p>Every component in the library resolves its tokens out of {@code UIManager} from {@code
 * paintComponent}, so an install that writes those keys off the Event Dispatch Thread races live
 * painting and can hand a window one frame of a half-swapped palette. The install javadoc has
 * always advertised off-EDT calls, so the contract is repaired rather than narrowed: the writes are
 * dispatched to the EDT and the caller blocks until they land.
 *
 * <p>Deliberately <em>not</em> registered with {@code EdtInterceptor} — this fixture has to drive
 * the EDT from outside, which a test body running on the EDT cannot do. {@code ThemeExtension} is
 * likewise skipped so no install races the latches; each test installs what it needs.
 */
class ElwhaThemeInstallThreadingTest {

  private static final long GENEROUS_TIMEOUT_SECONDS = 10L;

  @BeforeEach
  void pinReducedMotion() {
    MorphAnimator.setReducedMotion(true);
  }

  private static Config config(final Mode mode) {
    return ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build();
  }

  @Test
  void installingOffTheEdtWritesTheKeysOnTheEdt() throws Exception {
    final AtomicReference<Boolean> wroteOnEdt = new AtomicReference<>();
    final Thread worker =
        new Thread(
            () -> {
              // The listener fires on whichever thread swapped the look-and-feel, so it reports
              // where the write actually happened rather than where install was called.
              final java.beans.PropertyChangeListener spy =
                  event -> wroteOnEdt.set(SwingUtilities.isEventDispatchThread());
              UIManager.addPropertyChangeListener(spy);
              try {
                ElwhaTheme.install(config(Mode.DARK));
              } finally {
                UIManager.removePropertyChangeListener(spy);
              }
            },
            "install-off-edt");

    worker.start();
    worker.join(TimeUnit.SECONDS.toMillis(GENEROUS_TIMEOUT_SECONDS));

    assertThat(worker.isAlive()).as("an off-EDT install must not deadlock").isFalse();
    assertThat(wroteOnEdt.get())
        .as("the look-and-feel swap is ordered against painting, so it happens on the EDT")
        .isTrue();
  }

  @Test
  void installingOffTheEdtBlocksUntilTheKeysAreLive() throws Exception {
    final CountDownLatch edtBusy = new CountDownLatch(1);
    final CountDownLatch releaseEdt = new CountDownLatch(1);
    final CountDownLatch installReturned = new CountDownLatch(1);

    SwingUtilities.invokeLater(
        () -> {
          edtBusy.countDown();
          try {
            releaseEdt.await(GENEROUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
          } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          }
        });
    assertThat(edtBusy.await(GENEROUS_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        .as("the fixture could not occupy the EDT, so the rest of this proves nothing")
        .isTrue();

    final Thread worker =
        new Thread(
            () -> {
              ElwhaTheme.install(config(Mode.DARK));
              installReturned.countDown();
            },
            "install-blocks");
    worker.start();

    assertThat(installReturned.await(250, TimeUnit.MILLISECONDS))
        .as("install cannot have finished while the EDT it dispatches to is occupied")
        .isFalse();

    releaseEdt.countDown();

    assertThat(installReturned.await(GENEROUS_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        .as("once the EDT is free the writes land and the caller is released")
        .isTrue();
    worker.join(TimeUnit.SECONDS.toMillis(GENEROUS_TIMEOUT_SECONDS));
  }

  @Test
  void aThemeIsFullyInstalledByTheTimeAnOffEdtInstallReturns() throws Exception {
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final Thread worker =
        new Thread(
            () -> {
              try {
                ElwhaTheme.install(config(Mode.DARK));
                // Read from the worker, immediately on return: the point of blocking is that a
                // caller needs no invokeLater of its own to see a live theme.
                assertThat(UIManager.getColor(ColorRole.SURFACE.uiKey()))
                    .as("every token key is written before install returns")
                    .isEqualTo(MaterialPalettes.baseline().dark().get(ColorRole.SURFACE));
                assertThat(UIManager.getLookAndFeel().getName())
                    .as("and so is the base look-and-feel for the resolved mode")
                    .containsIgnoringCase("dark");
                assertThat(ElwhaTheme.current().mode())
                    .as("current() reports the config that was just installed")
                    .isEqualTo(Mode.DARK);
              } catch (final Throwable thrown) {
                failure.set(thrown);
              }
            },
            "install-then-read");

    worker.start();
    worker.join(TimeUnit.SECONDS.toMillis(GENEROUS_TIMEOUT_SECONDS));

    assertThat(failure.get()).as("the worker's own assertions").isNull();
  }
}
