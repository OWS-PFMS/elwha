package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.menu.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.util.List;

/**
 * Headless behavior smoke for epic #331 S1 (#374) — the {@link ElwhaSelectField} composition
 * skeleton and write-back round-trip. The popup needs a window, so selection is driven without a
 * display through the package-private {@code selectIndex(int)} seam (the shared selection path a
 * real menu pick also runs); this verifies the composition invariants, the typed write-back, and
 * the {@code selected}-on-reopen mark. Window-dependent behavior (anchor / flip / light-dismiss /
 * focus-return / arrow flip) is exercised by {@code SelectFieldS1SpikeDemo}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldS1SpikeSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS1SpikeSmoke {

  private SelectFieldS1SpikeSmoke() {}

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

    final List<String> options = List.of("Apple", "Banana", "Cherry");

    // --- composition invariants (filled) ---
    final ElwhaSelectField<String> filled = ElwhaSelectField.filled("Fruit");
    filled.setOptions(options);
    final ElwhaTextField filledField = (ElwhaTextField) filled.getComponent(0);

    failures += check(filledField.isReadOnly(), "embedded field is read-only");
    checks++;
    failures += check(filledField.getTrailingIconButton() != null, "trailing arrow button present");
    checks++;
    failures +=
        check(
            filled.getPreferredSize().equals(filledField.getPreferredSize()),
            "preferred size delegates to the embedded field");
    checks++;
    failures += check(!filled.isExpanded(), "collapsed at rest");
    checks++;
    failures += check(filled.getSelectedValue() == null, "no selection initially");
    checks++;

    // --- menu surface ---
    final ElwhaMenu menu = filled.optionsMenu();
    failures +=
        check(menu.getSelectionMode() == SelectionMode.SINGLE, "menu built SelectionMode.SINGLE");
    checks++;
    failures += check(menu.getItems().size() == 3, "menu has one item per option");
    checks++;
    failures += check("Banana".equals(menu.getItems().get(1).getLabel()), "item label = display");
    checks++;

    // --- write-back round-trip ---
    filled.selectIndex(1);
    failures += check("Banana".equals(filled.getSelectedValue()), "pick writes the typed value");
    checks++;
    failures += check("Banana".equals(filledField.getText()), "pick writes the field text");
    checks++;
    failures +=
        check(
            menu.getItems().get(1).isSelected() && !menu.getItems().get(0).isSelected(),
            "reopen shows the picked item selected (and only it)");
    checks++;

    // --- switching selection auto-deselects (single-select) ---
    filled.selectIndex(2);
    failures +=
        check(
            "Cherry".equals(filled.getSelectedValue())
                && menu.getItems().get(2).isSelected()
                && !menu.getItems().get(1).isSelected(),
            "switching selection writes the new value and deselects the old");
    checks++;

    // --- options change rebuilds the menu ---
    filled.setOptions(List.of("Xenon", "Yttrium"));
    final ElwhaMenu rebuilt = filled.optionsMenu();
    failures += check(rebuilt != menu, "setOptions rebuilds the menu");
    checks++;
    failures +=
        check(
            rebuilt.getItems().size() == 2 && "Xenon".equals(rebuilt.getItems().get(0).getLabel()),
            "rebuilt menu reflects the new options");
    checks++;

    // --- display function applied ---
    final ElwhaSelectField<Integer> numbers = ElwhaSelectField.filled("Count");
    numbers.setDisplayFunction(n -> "#" + n);
    numbers.setOptions(List.of(1, 2, 3));
    numbers.selectIndex(2);
    final ElwhaTextField numbersField = (ElwhaTextField) numbers.getComponent(0);
    failures +=
        check(Integer.valueOf(3).equals(numbers.getSelectedValue()), "typed value round-trips");
    checks++;
    failures += check("#3".equals(numbersField.getText()), "display function applied");
    checks++;

    // --- outlined variant composes ---
    final ElwhaSelectField<String> outlined = ElwhaSelectField.outlined("Fruit");
    outlined.setOptions(options);
    final ElwhaTextField outlinedField = (ElwhaTextField) outlined.getComponent(0);
    failures +=
        check(
            outlinedField.isReadOnly() && outlinedField.getTrailingIconButton() != null,
            "outlined variant composes the same way");
    checks++;

    System.out.println(
        "SelectFieldS1SpikeSmoke: " + (checks - failures) + "/" + checks + " checks passed");
    if (failures > 0) {
      System.out.println("FAIL: " + failures + " check(s) failed");
      System.exit(1);
    }
    System.out.println("PASS");
  }

  private static int check(final boolean condition, final String label) {
    System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    return condition ? 0 : 1;
  }
}
