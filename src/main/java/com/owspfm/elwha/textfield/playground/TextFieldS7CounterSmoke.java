package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.textfield.ElwhaTextField.SupportingTextVisibility;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S7 headless guard (#363): the character counter and supporting-text visibility mode. Asserts the
 * live {@code used/total} count feeds the accessible description ("character count, N of M
 * characters entered"), that the counter paints in the supporting row's right edge and turns the
 * {@code error} color when over the limit (while still accepting the input), that {@code ON_FOCUS}
 * keeps the supporting-row height reserved (zero layout shift) yet leaves the row blank on blur,
 * and that error text and an over-limit counter both override {@code ON_FOCUS} — across both
 * variants.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS7CounterSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS7CounterSmoke {

  private TextFieldS7CounterSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    // --- a11y: live "N of M" tracks the document ------------------------------
    final ElwhaTextField a = ElwhaTextField.outlined("Bio");
    a.setMaxLength(20);
    a.setText("hello");
    check(
        "counter feeds the a11y description (live count)",
        desc(a).contains("character count, 5 of 20 characters entered"));
    a.setText("hi");
    check("a11y count updates on text change", desc(a).contains("2 of 20"));

    check("default maxLength is -1 (no counter)", ElwhaTextField.filled("x").getMaxLength() == -1);
    check(
        "default visibility is ALWAYS",
        ElwhaTextField.filled("x").getSupportingTextVisibility()
            == SupportingTextVisibility.ALWAYS);

    // --- counter paints, right-aligned; over-limit turns error -----------------
    for (final Variant v : Variant.values()) {
      final ElwhaTextField under = new ElwhaTextField(v, "Name");
      under.setMaxLength(20);
      under.setText("abc");
      final BufferedImage uImg = render(under);
      check(v + " counter paints in the row's right edge", inkInCounterBand(uImg) > 0);
      check(v + " under-limit counter is not error-colored", !counterIsError(uImg));

      final ElwhaTextField over = new ElwhaTextField(v, "Name");
      over.setMaxLength(3);
      over.setText("abcdef"); // 6 > 3 — display only, input is NOT truncated
      check(v + " over-limit does not truncate input", "abcdef".equals(over.getText()));
      final BufferedImage oImg = render(over);
      check(v + " over-limit counter turns the error color", counterIsError(oImg));
    }

    // --- ON_FOCUS reserves height (zero layout shift) --------------------------
    for (final Variant v : Variant.values()) {
      final ElwhaTextField always = new ElwhaTextField(v, "Note");
      always.setSupportingText("Helper text");
      final ElwhaTextField onFocus = new ElwhaTextField(v, "Note");
      onFocus.setSupportingText("Helper text");
      onFocus.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
      check(
          v + " ON_FOCUS preserves the reserved row height",
          always.getPreferredSize().height == onFocus.getPreferredSize().height);
    }

    // --- ON_FOCUS hides advisory content on blur; ALWAYS shows it --------------
    final ElwhaTextField shown = ElwhaTextField.outlined("Note");
    shown.setSupportingText("Helper text");
    final ElwhaTextField hidden = ElwhaTextField.outlined("Note");
    hidden.setSupportingText("Helper text");
    hidden.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    check("ALWAYS paints supporting text (blurred)", inkInSupportingBand(render(shown)) > 0);
    check("ON_FOCUS leaves the row blank on blur", inkInSupportingBand(render(hidden)) == 0);

    // --- error text overrides ON_FOCUS (higher priority) ----------------------
    final ElwhaTextField err = ElwhaTextField.outlined("Note");
    err.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    err.setError(true);
    err.setErrorText("Required");
    check(
        "error text shows under ON_FOCUS even when blurred", inkInSupportingBand(render(err)) > 0);

    // --- over-limit counter overrides ON_FOCUS --------------------------------
    final ElwhaTextField overFocus = ElwhaTextField.outlined("Name");
    overFocus.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    overFocus.setMaxLength(3);
    overFocus.setText("abcdef");
    check(
        "over-limit counter shows under ON_FOCUS even when blurred",
        counterIsError(render(overFocus)));

    final ElwhaTextField underFocus = ElwhaTextField.outlined("Name");
    underFocus.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    underFocus.setMaxLength(20);
    underFocus.setText("abc");
    check(
        "in-limit counter is hidden under ON_FOCUS when blurred",
        inkInCounterBand(render(underFocus)) == 0);

    System.out.println(
        failures == 0 ? "PASS — all S7 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static String desc(final ElwhaTextField field) {
    final String d = field.getEditor().getAccessibleContext().getAccessibleDescription();
    return d == null ? "" : d;
  }

  private static BufferedImage render(final ElwhaTextField field) {
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
    return img;
  }

  /** The supporting row is the bottom strip; supporting text sits on its left half. */
  private static int inkInSupportingBand(final BufferedImage img) {
    return countInk(img, 0, (int) (img.getWidth() * 0.5), img.getHeight() - 15, img.getHeight());
  }

  /** The counter sits on the right edge of the bottom strip. */
  private static int inkInCounterBand(final BufferedImage img) {
    return countInk(
        img, (int) (img.getWidth() * 0.6), img.getWidth(), img.getHeight() - 15, img.getHeight());
  }

  private static int countInk(
      final BufferedImage img, final int x0, final int x1, final int y0, final int y1) {
    int n = 0;
    for (int y = Math.max(0, y0); y < y1; y++) {
      for (int x = Math.max(0, x0); x < x1; x++) {
        if ((img.getRGB(x, y) >>> 24) != 0) {
          n++;
        }
      }
    }
    return n;
  }

  /** Whether the counter ink reads closer to the {@code error} role than to its resting color. */
  private static boolean counterIsError(final BufferedImage img) {
    final Color avg =
        avgOpaque(
            img,
            (int) (img.getWidth() * 0.6),
            img.getWidth(),
            img.getHeight() - 15,
            img.getHeight());
    if (avg == null) {
      return false;
    }
    return dist(avg, ColorRole.ERROR.resolve()) < dist(avg, ColorRole.ON_SURFACE_VARIANT.resolve());
  }

  private static Color avgOpaque(
      final BufferedImage img, final int x0, final int x1, final int y0, final int y1) {
    long r = 0;
    long gg = 0;
    long b = 0;
    long count = 0;
    for (int y = Math.max(0, y0); y < y1; y++) {
      for (int x = Math.max(0, x0); x < x1; x++) {
        final int argb = img.getRGB(x, y);
        if ((argb >>> 24) == 0) {
          continue;
        }
        r += (argb >> 16) & 0xFF;
        gg += (argb >> 8) & 0xFF;
        b += argb & 0xFF;
        count++;
      }
    }
    return count == 0 ? null : new Color((int) (r / count), (int) (gg / count), (int) (b / count));
  }

  private static double dist(final Color a, final Color b) {
    final int dr = a.getRed() - b.getRed();
    final int dg = a.getGreen() - b.getGreen();
    final int db = a.getBlue() - b.getBlue();
    return Math.sqrt((double) dr * dr + dg * dg + db * db);
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
