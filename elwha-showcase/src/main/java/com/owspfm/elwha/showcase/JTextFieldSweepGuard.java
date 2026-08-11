package com.owspfm.elwha.showcase;

import java.io.IOException;
import java.util.Map;

/**
 * Headless regression guard for #424: the storefront must not reintroduce a raw {@code JTextField}
 * where a single-line text input belongs. Fails on any non-allowlisted {@code new JTextField} in
 * the guarded packages.
 *
 * <p>Scope, mechanics, and why the scan is source-level: {@link RawSwingSweep}. Run from the module
 * root, where {@code mvn exec:java} puts the working directory.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class JTextFieldSweepGuard {

  // Justified survivors: file simple name -> why that file may keep raw sites.
  static final Map<String, String> ALLOWLIST =
      Map.of(
          "DialogAccessibilityDemo.java",
          "the focus trap's tab-order probe — an ElwhaTextField delegates focus to an embedded "
              + "editor, which is the very tab cycle under test",
          "FullScreenDialogA11yDemo.java",
          "the initial-focus probe (#280) — the proof is which component is the first focusable "
              + "descendant, which a composite field would redefine");

  private JTextFieldSweepGuard() {}

  /**
   * Runs the guard; exits non-zero on any stray raw construction.
   *
   * @param args unused
   * @throws IOException if the source tree cannot be read
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) throws IOException {
    RawSwingSweep.report("JTextField", "ElwhaTextField", ALLOWLIST);
  }
}
