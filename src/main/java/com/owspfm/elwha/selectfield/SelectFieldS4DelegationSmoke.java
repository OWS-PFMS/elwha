package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.util.List;

/**
 * Headless behavior smoke for epic #331 S4 (#377) — variant delegation, state propagation, and the
 * owned trailing slot. Verifies that {@code filled()}/{@code outlined()} carry the embedded field's
 * variant, that label / supporting / placeholder / leading-icon / error pass through, that {@code
 * setEnabled} propagates to the whole control (field + arrow), that {@code setReadOnly} tracks, and
 * that the arrow owns the trailing slot.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldS4DelegationSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS4DelegationSmoke {

  private SelectFieldS4DelegationSmoke() {}

  /**
   * Runs the smoke and exits non-zero on any failure.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    int checks = 0;
    int failures = 0;

    // --- variant delegation ---
    final ElwhaSelectField<String> filled = ElwhaSelectField.filled("City");
    final ElwhaSelectField<String> outlined = ElwhaSelectField.outlined("City");
    failures +=
        check(
            fieldOf(filled).getVariant() == ElwhaTextField.Variant.FILLED,
            "filled() delegates the FILLED variant");
    checks++;
    failures +=
        check(
            fieldOf(outlined).getVariant() == ElwhaTextField.Variant.OUTLINED,
            "outlined() delegates the OUTLINED variant");
    checks++;

    final ElwhaSelectField<String> sf = ElwhaSelectField.filled("City");
    sf.setOptions(List.of("Oslo", "Lima"));
    final ElwhaTextField field = fieldOf(sf);

    // --- label / supporting / placeholder / leading icon passthrough ---
    sf.setLabel("Hometown");
    failures += check("Hometown".equals(sf.getLabel()), "setLabel passes through");
    checks++;
    sf.setSupportingText("Pick one");
    failures += check("Pick one".equals(sf.getSupportingText()), "supporting text passes through");
    checks++;
    sf.setPlaceholder("Choose…");
    failures += check("Choose…".equals(sf.getPlaceholder()), "placeholder passes through");
    checks++;
    sf.setLeadingIcon(MaterialIcons.home(20));
    failures += check(field.getLeadingIcon() != null, "leading icon passes through");
    checks++;

    // --- error state ---
    sf.setError(true);
    sf.setErrorText("Required");
    failures += check(sf.isError() && field.isError(), "setError propagates to the field");
    checks++;
    failures += check("Required".equals(sf.getErrorText()), "error text passes through");
    checks++;
    sf.setError(false);
    failures += check(!sf.isError(), "error clears");
    checks++;

    // --- enabled propagation ---
    sf.setEnabled(false);
    failures +=
        check(
            !sf.isEnabled() && !field.isEnabled() && !field.getTrailingIconButton().isEnabled(),
            "setEnabled propagates to field + arrow");
    checks++;
    sf.setEnabled(true);
    failures += check(sf.isEnabled() && field.isEnabled(), "re-enable propagates");
    checks++;

    // --- read-only ---
    failures += check(!sf.isReadOnly(), "not read-only by default");
    checks++;
    sf.setReadOnly(true);
    failures += check(sf.isReadOnly(), "setReadOnly tracks");
    checks++;

    // --- owned trailing slot (the arrow is the field's trailing button) ---
    failures +=
        check(
            field.getTrailingIconButton() != null, "arrow owns the trailing slot by construction");
    checks++;

    System.out.println(
        "SelectFieldS4DelegationSmoke: " + (checks - failures) + "/" + checks + " checks passed");
    if (failures > 0) {
      System.out.println("FAIL: " + failures + " check(s) failed");
      System.exit(1);
    }
    System.out.println("PASS");
  }

  private static ElwhaTextField fieldOf(final ElwhaSelectField<?> sf) {
    return (ElwhaTextField) sf.getComponent(0);
  }

  private static int check(final boolean condition, final String label) {
    System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    return condition ? 0 : 1;
  }
}
