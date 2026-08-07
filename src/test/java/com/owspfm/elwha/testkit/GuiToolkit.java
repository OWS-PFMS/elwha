package com.owspfm.elwha.testkit;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Per-platform toolkit switch for the {@code gui}-tagged tier. The gui tests themselves are
 * toolkit-agnostic (plain {@code Robot} + Swing APIs); this condition decides which display stack
 * hosts them, keyed off {@value #PROPERTY}:
 *
 * <ul>
 *   <li><b>{@code cacio}</b> (default) — lazily loads {@code CacioExtension}, whose static
 *       initializer injects the virtual toolkit: deterministic screen, no display theft, works on
 *       macOS/Windows dev machines with no setup. The class is only touched in this branch, so
 *       native mode never triggers the injection.
 *   <li><b>{@code native}</b> — leaves the platform toolkit alone; a real display must exist (Xvfb
 *       on CI, the desktop on a dev machine). Required on Linux: the JDK 21 X11 font natives depend
 *       on JNI state that only real X11 display initialization registers, so any font-manager
 *       construction under Cacio segfaults there (hs_err frame: {@code
 *       Java_sun_awt_FcFontManager_getFontPathNative}; full derivation in {@code
 *       docs/research/elwha-test-suite-research.md}). The Maven {@code linux-gui-native} profile
 *       selects this automatically.
 * </ul>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class GuiToolkit implements ExecutionCondition {

  /** System property selecting the gui-tier display stack: {@code cacio} or {@code native}. */
  public static final String PROPERTY = "elwha.guiTier.toolkit";

  /**
   * Creates the condition; instantiated reflectively by JUnit.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public GuiToolkit() {}

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
    if ("native".equals(System.getProperty(PROPERTY))) {
      return ConditionEvaluationResult.enabled(
          "gui tier on the native toolkit (display expected: Xvfb or desktop)");
    }
    try {
      final Class<?> cacio =
          Class.forName("com.github.caciocavallosilano.cacio.ctc.junit.CacioExtension");
      final ExecutionCondition delegate =
          (ExecutionCondition) cacio.getDeclaredConstructor().newInstance();
      return delegate.evaluateExecutionCondition(context);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Cacio is unavailable for the gui tier; pass -D"
              + PROPERTY
              + "=native to run against a real display instead.",
          e);
    }
  }
}
