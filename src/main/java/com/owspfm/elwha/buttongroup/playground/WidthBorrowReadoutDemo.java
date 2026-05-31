package com.owspfm.elwha.buttongroup.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;

/**
 * Headless verification harness for {@link ElwhaButton#currentWidthBorrow()} ([#184]). Not an
 * interactive playground — it drives the borrow morph under reduced motion (so progress snaps
 * deterministically) and exits non-zero on any mismatch, so it doubles as a smoke gate.
 *
 * <p>Mirrors what an {@code ElwhaButtonGroup} STANDARD group does when a middle segment is held: it
 * applies the §6 decay vector {@code [1.0, 0.3, 0.1, 0]} across five segments centered on index 2,
 * then asserts each segment's {@code currentWidthBorrow()} reports that segment's factor (reading
 * {@code 0.00} at rest and after release).
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class WidthBorrowReadoutDemo {

  private WidthBorrowReadoutDemo() {}

  private static final float[] DECAY = {1.0f, 0.3f, 0.1f};
  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(true); // snap progress so the readout is deterministic

    final ElwhaButton[] segments = new ElwhaButton[5];
    for (int i = 0; i < segments.length; i++) {
      segments[i] = new ElwhaButton("S" + (i + 1));
    }

    for (final ElwhaButton s : segments) {
      check("rest reads 0.00", s.currentWidthBorrow() == 0f);
    }

    // Press index 2: factor = DECAY[distance] (0 beyond the vector) — the group's exact wiring.
    final int pressed = 2;
    final float[] expected = new float[segments.length];
    for (int j = 0; j < segments.length; j++) {
      final int distance = Math.abs(j - pressed);
      final float factor = distance < DECAY.length ? DECAY[distance] : 0f;
      expected[j] = factor;
      segments[j].startWidthBorrow(factor);
    }

    final StringBuilder row = new StringBuilder();
    for (int j = 0; j < segments.length; j++) {
      final float got = segments[j].currentWidthBorrow();
      row.append(String.format("S%d=%.2f ", j + 1, got));
      check(
          "S" + (j + 1) + " borrow == " + expected[j] + " (got " + got + ")",
          Math.abs(got - expected[j]) < 1e-4);
    }
    System.out.println("  held idx2 → " + row.toString().trim());

    for (final ElwhaButton s : segments) {
      s.releaseWidthBorrow();
      check("release returns to 0.00", s.currentWidthBorrow() == 0f);
    }

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: currentWidthBorrow() tracks the §6 decay vector.");
  }

  private static void check(final String label, final boolean ok) {
    if (!ok) {
      System.out.println("  FAIL " + label);
      failures++;
    }
  }
}
