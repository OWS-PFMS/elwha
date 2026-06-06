package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S3 headless guard (#335): paints the full variant&#215;state matrix without throwing and asserts
 * the externally observable color rules — input text stays {@code on-surface} in every state
 * (including error), and the caret follows {@code error}/{@code primary} (error beats focus).
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS3StateMatrixSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS3StateMatrixSmoke {

  private TextFieldS3StateMatrixSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    for (final ElwhaTextField.Variant variant : ElwhaTextField.Variant.values()) {
      for (final boolean populated : new boolean[] {false, true}) {
        for (final String state : new String[] {"enabled", "error", "disabled", "read-only"}) {
          final ElwhaTextField f = new ElwhaTextField(variant, "Label");
          if (populated) {
            f.setText("Value");
          }
          switch (state) {
            case "error":
              f.setError(true);
              break;
            case "disabled":
              f.setEnabled(false);
              break;
            case "read-only":
              f.setReadOnly(true);
              break;
            default:
              break;
          }
          paints(variant + "/" + state + "/pop=" + populated, f);
        }
      }
    }

    final ElwhaTextField normal = ElwhaTextField.filled("L");
    check(
        "input text is on-surface when enabled",
        normal.getEditor().getForeground().equals(ColorRole.ON_SURFACE.resolve()));
    check(
        "caret is primary when not errored",
        normal.getEditor().getCaretColor().equals(ColorRole.PRIMARY.resolve()));

    final ElwhaTextField errored = ElwhaTextField.outlined("L");
    errored.setError(true);
    check(
        "caret turns error (error beats focus)",
        errored.getEditor().getCaretColor().equals(ColorRole.ERROR.resolve()));
    check(
        "input text stays on-surface in error",
        errored.getEditor().getForeground().equals(ColorRole.ON_SURFACE.resolve()));

    System.out.println(
        failures == 0 ? "PASS — all S3 checks green" : "FAIL — " + failures + " check(s)");
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
