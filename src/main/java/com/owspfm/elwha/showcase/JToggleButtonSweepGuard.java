package com.owspfm.elwha.showcase;

import java.io.IOException;
import java.util.Map;

/**
 * Headless regression guard for #321: the storefront must not reintroduce a raw {@code
 * JToggleButton} where a binary or mutually-exclusive toggle belongs. Fails on any non-allowlisted
 * {@code new JToggleButton} in the guarded packages.
 *
 * <p>Scope, mechanics, and why the scan is source-level: {@link RawSwingSweep}. Run from the module
 * root, where {@code mvn exec:java} puts the working directory.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class JToggleButtonSweepGuard {

  // Justified survivors: file simple name -> why that file may keep raw sites.
  static final Map<String, String> ALLOWLIST =
      Map.of(
          "ThemePlayground.java",
              "deferred — the in-flight ShapeScale PR #706 edits this file; sweeping it here would"
                  + " collide",
          "FoundationsPanels.java",
              "deferred — the in-flight ShapeScale PR #706 edits this file; sweeping it here would"
                  + " collide");

  private JToggleButtonSweepGuard() {}

  /**
   * Runs the guard; exits non-zero on any stray raw construction.
   *
   * @param args unused
   * @throws IOException if the source tree cannot be read
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) throws IOException {
    RawSwingSweep.report(
        "JToggleButton",
        "ElwhaButtonGroup for a segmented selector, ElwhaButton SELECTABLE or ElwhaChip for a"
            + " standalone toggle",
        ALLOWLIST);
  }
}
