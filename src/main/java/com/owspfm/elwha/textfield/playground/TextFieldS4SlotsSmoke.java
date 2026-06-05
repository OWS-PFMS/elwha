package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S4 headless guard (#336): every slot configuration lays out and paints without throwing, the
 * editor stays inside the chrome with the icon-aware padding, the reserved supporting-text row
 * keeps the preferred height stable, the required asterisk reaches the accessible name, and the
 * leading/trailing slots mirror under a right-to-left orientation.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS4SlotsSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS4SlotsSmoke {

  private TextFieldS4SlotsSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    final ElwhaTextField leading = ElwhaTextField.outlined("L");
    leading.setLeadingIcon(MaterialIcons.info());
    paints("leading-icon", leading);
    // ICON_SLOT = 12 edge + 24 glyph + 16 gap = 52.
    check("editor starts past the leading icon slot (52px)", editorAt(leading).x >= 52);

    final ElwhaTextField trailingBtn = ElwhaTextField.filled("L");
    final ElwhaIconButton clear = new ElwhaIconButton(MaterialIcons.close());
    trailingBtn.setTrailingIconButton(clear);
    paints("trailing-button", trailingBtn);
    check("trailing button is a hosted child", clear.getParent() == trailingBtn);
    // PAD_LR_ICON = 12.
    check(
        "editor right edge clears the trailing slot",
        editorAt(trailingBtn).x + editorAt(trailingBtn).width <= trailingBtn.getWidth() - 12);

    final ElwhaTextField affixes = ElwhaTextField.filled("L");
    affixes.setText("9");
    affixes.setPrefixText("$");
    affixes.setSuffixText("USD");
    paints("prefix-suffix", affixes);

    final ElwhaTextField supporting = ElwhaTextField.outlined("L");
    final int bare = supporting.getPreferredSize().height;
    supporting.setSupportingText("helper");
    check(
        "supporting-text row is pre-reserved (height stable)",
        supporting.getPreferredSize().height == bare);
    paints("supporting", supporting);

    final ElwhaTextField required = ElwhaTextField.filled("Email");
    required.setRequired(true);
    check(
        "required appends asterisk to accessible name",
        required.getEditor().getAccessibleContext().getAccessibleName().contains("*"));
    required.setNoAsterisk(true);
    check(
        "no-asterisk suppresses it in the accessible name",
        !required.getEditor().getAccessibleContext().getAccessibleName().contains("*"));

    final ElwhaTextField rtl = ElwhaTextField.outlined("L");
    rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    rtl.setLeadingIcon(MaterialIcons.info());
    paints("rtl-leading", rtl);
    // PAD_LR_NO_ICON = 16: in RTL the leading slot moves right, so the editor keeps the bare left
    // pad.
    check(
        "RTL puts the leading slot on the right (editor keeps left padding)",
        editorAt(rtl).x == 16);

    System.out.println(
        failures == 0 ? "PASS — all S4 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static java.awt.Rectangle editorAt(final ElwhaTextField field) {
    field.setSize(field.getPreferredSize());
    field.doLayout();
    return field.getEditor().getBounds();
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
