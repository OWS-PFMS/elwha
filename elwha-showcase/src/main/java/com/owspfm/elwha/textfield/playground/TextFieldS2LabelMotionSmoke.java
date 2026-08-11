package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S2 headless guard (#334): the risk is the label-float motion and the placeholder visibility
 * rules. Exercises the animated path under both reduced and non-reduced motion (paints at the
 * endpoints without throwing) and asserts FlatLaf's native placeholder toggles for the label-less
 * and populated branches.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS2LabelMotionSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS2LabelMotionSmoke {

  private TextFieldS2LabelMotionSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    final ElwhaTextField labelless = new ElwhaTextField("");
    labelless.setPlaceholder("Search");
    check(
        "label-less + empty shows placeholder",
        "Search".equals(labelless.getEditor().getClientProperty("JTextField.placeholderText")));

    final ElwhaTextField populated = ElwhaTextField.filled("Email");
    populated.setPlaceholder("you@example.com");
    populated.setText("ada@x.io");
    check(
        "populated field hides placeholder",
        populated.getEditor().getClientProperty("JTextField.placeholderText") == null);

    final ElwhaTextField restingLabelled = ElwhaTextField.outlined("Name");
    restingLabelled.setPlaceholder("hint");
    check(
        "resting labelled (unfocused, empty) hides placeholder",
        restingLabelled.getEditor().getClientProperty("JTextField.placeholderText") == null);

    check("getPlaceholder round-trips", "hint".equals(restingLabelled.getPlaceholder()));

    // Animated path: populate a labelled field under both motion modes; paint must not throw.
    for (final boolean reduced : new boolean[] {true, false}) {
      MorphAnimator.setReducedMotion(reduced);
      final ElwhaTextField f = ElwhaTextField.filled("Animated");
      f.setText("value");
      paints("float/reduced=" + reduced, f);
      f.setText("");
      paints("unfloat/reduced=" + reduced, f);
    }
    MorphAnimator.setReducedMotion(false);

    System.out.println(
        failures == 0 ? "PASS — all S2 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void paints(final String label, final ElwhaTextField field) {
    try {
      field.setSize(field.getPreferredSize());
      field.doLayout();
      final BufferedImage img =
          new BufferedImage(
              Math.max(1, field.getWidth()),
              Math.max(1, field.getHeight()),
              BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g = img.createGraphics();
      field.paint(g);
      g.dispose();
      check("paints " + label, true);
    } catch (final RuntimeException ex) {
      check("paints " + label + " (threw " + ex + ")", false);
    }
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
