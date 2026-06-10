package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.util.List;

/**
 * Headless behavior smoke for epic #331 Phase 2 S1 (#391) — the {@link ElwhaSelectField} editable
 * mode. Verifies the editable flag's read-only lift on the embedded editor (and its interaction
 * with the select-level read-only state), free text surviving in the field, the write-back
 * round-trip staying intact in editable mode, the cached menu invalidating on a mode flip, and the
 * pure select staying untouched by default. Window-dependent behavior (focus staying in the editor
 * while the menu is open, open-on-typing, the arrow toggle) is exercised by {@code
 * SelectFieldEditableSpikeDemo}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldEditableSpikeSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldEditableSpikeSmoke {

  private SelectFieldEditableSpikeSmoke() {}

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

    final List<String> options = List.of("Java", "Kotlin", "Scala");

    // --- default = pure select, unchanged ---
    final ElwhaSelectField<String> pure = ElwhaSelectField.filled("Language");
    pure.setOptions(options);
    final ElwhaTextField pureField = (ElwhaTextField) pure.getComponent(0);
    failures += check(!pure.isEditable(), "default is non-editable");
    checks++;
    failures += check(pureField.isReadOnly(), "pure select keeps the embedded field read-only");
    checks++;
    pure.selectIndex(0);
    failures +=
        check(
            "Java".equals(pure.getSelectedValue()) && "Java".equals(pure.getText()),
            "pure-select write-back unchanged");
    checks++;

    // --- editable lifts the embedded read-only ---
    final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Language");
    combo.setOptions(options);
    final ElwhaTextField comboField = (ElwhaTextField) combo.getComponent(0);
    final ElwhaMenu pureModeMenu = combo.optionsMenu();
    combo.setEditable(true);
    failures += check(combo.isEditable(), "setEditable(true) reflects");
    checks++;
    failures += check(!comboField.isReadOnly(), "editable lifts the embedded read-only");
    checks++;
    failures += check(combo.optionsMenu() != pureModeMenu, "mode flip invalidates the cached menu");
    checks++;

    // --- free text accepted, value model untouched by typing ---
    comboField.getEditor().setText("Type");
    failures += check("Type".equals(combo.getText()), "free text accepted into the field");
    checks++;
    failures += check(combo.getSelectedValue() == null, "free text does not set the typed value");
    checks++;

    // --- pick still writes back in editable mode ---
    combo.selectIndex(1);
    failures +=
        check(
            "Kotlin".equals(combo.getSelectedValue()) && "Kotlin".equals(combo.getText()),
            "menu pick writes back in editable mode");
    checks++;
    failures +=
        check(
            combo.optionsMenu().getItems().get(1).isSelected(),
            "picked item marked selected for reopen");
    checks++;

    // --- select-level read-only re-imposes the embedded read-only ---
    combo.setReadOnly(true);
    failures += check(comboField.isReadOnly(), "select-level read-only blocks typing");
    checks++;
    combo.setReadOnly(false);
    failures += check(!comboField.isReadOnly(), "lifting select-level read-only restores typing");
    checks++;

    // --- flipping editable off restores the pure select ---
    combo.setEditable(false);
    failures += check(comboField.isReadOnly(), "setEditable(false) restores the read-only field");
    checks++;
    failures +=
        check("Kotlin".equals(combo.getSelectedValue()), "selection survives the mode flip");
    checks++;

    System.out.println(
        "SelectFieldEditableSpikeSmoke: " + (checks - failures) + "/" + checks + " checks passed");
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
