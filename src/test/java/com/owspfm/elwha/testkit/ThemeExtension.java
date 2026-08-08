package com.owspfm.elwha.testkit;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Installs the deterministic test theme before every test: the baseline palette in {@link
 * Mode#LIGHT}, with {@link MorphAnimator#setReducedMotion(boolean) reduced motion} pinned on so no
 * assertion ever races a wall-clock animation ({@code MorphAnimator} otherwise auto-detects the
 * host OS accessibility setting on first use — an unpinned run can behave differently on a
 * developer's machine than on CI). Pinning it first also suppresses that probe outright, so no run
 * of the suite forks {@code gsettings}. Tests that need another mode call {@link #install(Mode)}
 * mid-test; the next test's {@code beforeEach} restores the baseline.
 *
 * <p>Register via {@code @ExtendWith({EdtInterceptor.class, ThemeExtension.class})} — after the EDT
 * interceptor, so the install runs on the dispatch thread.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ThemeExtension implements BeforeEachCallback {

  /**
   * Creates the extension; instantiated reflectively by JUnit.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public ThemeExtension() {}

  @Override
  public void beforeEach(final ExtensionContext context) {
    MorphAnimator.setReducedMotion(true);
    install(Mode.LIGHT);
  }

  /**
   * Installs the baseline palette in the given mode — the one sanctioned way for a test to switch
   * modes mid-flight (e.g. a light/dark parameterized probe).
   *
   * @param mode the theme mode to install
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void install(final Mode mode) {
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
  }
}
