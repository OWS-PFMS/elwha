package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless behavior smoke for epic #331 Phase 2 S3 (#393) — the editable combo's value model and
 * commit semantics. Drives the commit/revert seams directly ({@code resolveFieldText()} is the
 * Enter / focus-loss path, {@code revertToCommitted()} the Esc path): case-insensitive exact-match
 * resolution canonicalizes to the option, the constrained policy reverts unknown text, the
 * free-text policy keeps it with a {@code null} selected value (and fires the change listener), the
 * committed value survives a later revert, and the public menu-highlight navigation no-ops safely
 * while closed. Keyboard routing and the a11y announcements are exercised live by {@code
 * SelectFieldCommitDemo}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldCommitSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldCommitSmoke {

  private SelectFieldCommitSmoke() {}

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

    final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Language");
    combo.setOptions(List.of("Java", "Kotlin", "Scala"));
    combo.setEditable(true);
    final ElwhaTextField field = (ElwhaTextField) combo.getComponent(0);
    final List<String> events = new ArrayList<>();
    combo.addSelectionChangeListener(v -> events.add(String.valueOf(v)));

    // --- policy default ---
    failures += check(!combo.isFreeTextAllowed(), "constrained to options by default");
    checks++;

    // --- exact match resolves + canonicalizes (case-insensitive) ---
    field.getEditor().setText("kotlin");
    combo.resolveFieldText();
    failures +=
        check(
            "Kotlin".equals(combo.getSelectedValue()) && "Kotlin".equals(combo.getText()),
            "exact match resolves and canonicalizes the display");
    checks++;
    failures += check(events.equals(List.of("Kotlin")), "resolution fires the change listener");
    checks++;

    // --- constrained: unknown text reverts to the committed value ---
    field.getEditor().setText("Kot");
    combo.resolveFieldText();
    failures +=
        check(
            "Kotlin".equals(combo.getText()) && "Kotlin".equals(combo.getSelectedValue()),
            "constrained commit reverts unknown text");
    checks++;
    failures += check(events.size() == 1, "a revert is not a change event");
    checks++;

    // --- free text allowed: unknown text commits with a null value ---
    combo.setFreeTextAllowed(true);
    field.getEditor().setText("Brainfuck");
    combo.resolveFieldText();
    failures +=
        check(
            combo.getSelectedValue() == null && "Brainfuck".equals(combo.getText()),
            "free text commits: text kept, selected value null");
    checks++;
    failures +=
        check(
            events.equals(List.of("Kotlin", "null")),
            "clearing the selection via free text fires the listener with null");
    checks++;
    failures +=
        check(
            combo.optionsMenu().getSelectedItems().isEmpty(),
            "free-text commit clears the menu's selected marks");
    checks++;

    // --- Esc reverts to the last committed value (the free text) ---
    field.getEditor().setText("xyz");
    combo.revertToCommitted();
    failures += check("Brainfuck".equals(combo.getText()), "Esc reverts to the committed text");
    checks++;

    // --- resolving back onto an option re-selects it ---
    field.getEditor().setText("SCALA");
    combo.resolveFieldText();
    failures +=
        check(
            "Scala".equals(combo.getSelectedValue()) && "Scala".equals(combo.getText()),
            "free-text mode still resolves an exact match to the option");
    checks++;

    // --- commit resets the live filter ---
    failures +=
        check(
            combo.optionsMenu().getItems().get(0).isVisible(),
            "commit clears the filter — full list on next open");
    checks++;

    // --- highlight navigation is closed-safe ---
    final ElwhaMenu menu = combo.optionsMenu();
    menu.moveHighlight(1);
    menu.activateHighlighted();
    menu.highlight(menu.getItems().get(0));
    failures +=
        check(menu.getHighlightedItem() == null, "highlight API no-ops while the menu is closed");
    checks++;

    // --- a11y: the editor carries the combobox description only while editable ---
    failures +=
        check(
            field.getEditor().getAccessibleContext().getAccessibleDescription() != null,
            "editable editor carries the combobox accessible description");
    checks++;
    combo.setEditable(false);
    failures +=
        check(
            field.getEditor().getAccessibleContext().getAccessibleDescription() == null,
            "pure select carries no combobox description");
    checks++;

    // --- the commit seams are editable-only ---
    field.setText("Java");
    combo.resolveFieldText();
    failures +=
        check(
            "Scala".equals(combo.getSelectedValue()),
            "resolveFieldText is a no-op on the pure select");
    checks++;

    System.out.println(
        "SelectFieldCommitSmoke: " + (checks - failures) + "/" + checks + " checks passed");
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
