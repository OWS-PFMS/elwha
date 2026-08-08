package com.owspfm.elwha.showcase;

import java.io.IOException;
import java.util.Map;

/**
 * Headless regression guard for #317: the storefront must not reintroduce a raw {@code new
 * JButton(...)} where an {@link com.owspfm.elwha.button.ElwhaButton} belongs. Fails on any
 * non-allowlisted {@code new JButton} in the guarded packages.
 *
 * <p>Scope, mechanics, and why the scan is source-level: {@link RawSwingSweep}. The dogfood sweep
 * (#424) widened that surface from #317's six hardcoded directories to every playground package,
 * and moved the scan itself into the shared engine the five sibling guards run on. Run from the
 * module root, where {@code mvn exec:java} puts the working directory.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.4.0
 */
public final class JButtonSweepGuard {

  // Justified survivors: file simple name -> why that file may keep raw sites.
  static final Map<String, String> ALLOWLIST =
      Map.of(
          "ThemePlayground.java",
          "default button passed to JRootPane.setDefaultButton(JButton) — ElwhaButton extends "
              + "JComponent, not JButton");

  private JButtonSweepGuard() {}

  /**
   * Runs the guard; exits non-zero on any stray raw construction.
   *
   * @param args unused
   * @throws IOException if the source tree cannot be read
   * @version v0.5.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws IOException {
    RawSwingSweep.report("JButton", "ElwhaButton", ALLOWLIST);
  }
}
