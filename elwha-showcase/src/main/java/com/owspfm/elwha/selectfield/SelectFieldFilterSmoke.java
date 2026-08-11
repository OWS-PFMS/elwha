package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.util.List;

/**
 * Headless behavior smoke for epic #331 Phase 2 S2 (#392) — filter-as-you-type. Drives the filter
 * through the editor document (the same path real keystrokes take) and reads the cached menu's item
 * visibility: narrowing (case-insensitive prefix and substring matches), clearing back to the full
 * list, the disabled "No matches" placeholder on a no-match, the filter resetting on a pick, and
 * the filter composing with the rebuild-on-options-change lifecycle. The live open-menu relayout is
 * exercised by {@code SelectFieldFilterDemo}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldFilterSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldFilterSmoke {

  private SelectFieldFilterSmoke() {}

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

    final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Fruit");
    combo.setOptions(List.of("Apple", "Banana", "Cherry", "Blueberry"));
    combo.setEditable(true);
    final ElwhaTextField field = (ElwhaTextField) combo.getComponent(0);
    final ElwhaMenu menu = combo.optionsMenu();
    final List<ElwhaMenuItem> menuItems = menu.getItems();

    // --- editable menu carries the placeholder row, hidden at rest ---
    failures += check(menuItems.size() == 5, "editable menu = options + placeholder row");
    checks++;
    final ElwhaMenuItem placeholder = menuItems.get(4);
    failures +=
        check(
            "No matches".equals(placeholder.getLabel())
                && !placeholder.isEnabled()
                && !placeholder.isVisible(),
            "placeholder is disabled and hidden at rest");
    checks++;

    // --- substring narrowing (case-insensitive) ---
    field.getEditor().setText("an");
    failures +=
        check(
            !menuItems.get(0).isVisible()
                && menuItems.get(1).isVisible()
                && !menuItems.get(2).isVisible()
                && !menuItems.get(3).isVisible(),
            "\"an\" leaves only Banana (substring match)");
    checks++;
    failures += check(!placeholder.isVisible(), "placeholder stays hidden while matches exist");
    checks++;

    // --- prefix narrowing (case-insensitive) ---
    field.getEditor().setText("BL");
    failures +=
        check(
            menuItems.get(3).isVisible() && !menuItems.get(1).isVisible(),
            "\"BL\" matches Blueberry (case-insensitive prefix)");
    checks++;

    // --- clearing restores the full list ---
    field.getEditor().setText("");
    failures +=
        check(
            menuItems.get(0).isVisible()
                && menuItems.get(1).isVisible()
                && menuItems.get(2).isVisible()
                && menuItems.get(3).isVisible()
                && !placeholder.isVisible(),
            "empty filter shows all options, no placeholder");
    checks++;

    // --- no-match shows the placeholder, not a stale list ---
    field.getEditor().setText("zz");
    failures +=
        check(
            !menuItems.get(0).isVisible()
                && !menuItems.get(1).isVisible()
                && !menuItems.get(2).isVisible()
                && !menuItems.get(3).isVisible()
                && placeholder.isVisible(),
            "no-match hides every option and shows the placeholder");
    checks++;
    failures += check(combo.getSelectedValue() == null, "no-match never selects anything");
    checks++;

    // --- a pick clears the filter (write-back must not re-filter) ---
    combo.selectIndex(0);
    failures +=
        check(
            "Apple".equals(combo.getSelectedValue()) && "Apple".equals(combo.getText()),
            "pick writes back while filtered");
    checks++;
    failures +=
        check(
            menuItems.get(1).isVisible() && menuItems.get(3).isVisible(),
            "pick resets the filter — full list on next open");
    checks++;

    // --- filter composes with rebuild-on-options-change ---
    field.getEditor().setText("an");
    combo.setOptions(List.of("Anchovy", "Tuna", "Sardine"));
    final ElwhaMenu rebuilt = combo.optionsMenu();
    failures += check(rebuilt != menu, "options change still rebuilds the menu");
    checks++;
    final List<ElwhaMenuItem> rebuiltItems = rebuilt.getItems();
    failures +=
        check(
            rebuiltItems.get(0).isVisible()
                && !rebuiltItems.get(1).isVisible()
                && !rebuiltItems.get(2).isVisible(),
            "live filter re-applies to the rebuilt menu's items");
    checks++;

    // --- pure select carries no placeholder and no filtering ---
    final ElwhaSelectField<String> pure = ElwhaSelectField.filled("Fruit");
    pure.setOptions(List.of("Apple", "Banana"));
    failures +=
        check(pure.optionsMenu().getItems().size() == 2, "pure select has no placeholder row");
    checks++;

    System.out.println(
        "SelectFieldFilterSmoke: " + (checks - failures) + "/" + checks + " checks passed");
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
