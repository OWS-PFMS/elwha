package com.owspfm.elwha.testkit;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Raises the {@code com.owspfm.elwha} logger threshold to {@link Level#SEVERE} for the test JVM, so
 * the library's runtime advisories — the missing-accessible-name warnings and the rail's 3–7
 * destination recommendation — cannot pollute suite output. Test fixtures rarely set accessible
 * names, and that is fine: the advisories target running apps, not offscreen paint probes.
 *
 * <p>Auto-registered via {@code META-INF/services} plus {@code
 * junit.jupiter.extensions.autodetection.enabled} in {@code junit-platform.properties}, so it
 * covers both surefire tiers with no per-fixture opt-in. A test that asserts an advisory fires
 * overrides the threshold on the specific component logger ({@code
 * Logger.setLevel(Level.WARNING)}), attaches its own handler, and restores the inherited level in a
 * {@code finally}.
 *
 * <p>The logger is held in a static field on purpose: {@link Logger#getLogger(String)} returns
 * weakly-referenced instances, and a level set on an unreferenced logger can be garbage-collected
 * away mid-session.
 *
 * @author Charles Bryan
 * @version v1.1.0
 * @since v1.1.0
 */
public final class SuiteLogSilencer implements BeforeAllCallback {

  private static final Logger ELWHA_ROOT = Logger.getLogger("com.owspfm.elwha");

  /**
   * Creates the extension; instantiated by the JUnit Platform via {@code ServiceLoader}.
   *
   * @version v1.1.0
   * @since v1.1.0
   */
  public SuiteLogSilencer() {}

  /**
   * Applies the {@link Level#SEVERE} threshold before each test class — idempotent, and early
   * enough that no component ever paints ahead of it.
   *
   * @param context the extension context supplied by the platform
   * @version v1.1.0
   * @since v1.1.0
   */
  @Override
  public void beforeAll(final ExtensionContext context) {
    ELWHA_ROOT.setLevel(Level.SEVERE);
  }
}
