package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.menu.SelectionMode;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.util.List;

/**
 * Headless smoke for Phase 3 S1 (#397): the multi-select spike + value model. Asserts the opt-in
 * ({@code setMultiSelect}) builds a {@link SelectionMode#MULTI} menu, toggles accumulate an
 * option-ordered {@code getSelectedValues()} with {@code getSelectedValue()} on the first, {@code
 * setSelectedValues} filters/dedupes/orders, the single-value API delegates in multi mode, mode
 * flips seed/collapse the selection, editable and multi are mutually exclusive, and the
 * single-select + editable combo are untouched while multi is off. Drives toggles through {@code
 * toggleIndex} — the same path a real MULTI menu pick takes (the popup needs a window).
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldMultiSpikeSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldMultiSpikeSmoke {

  private SelectFieldMultiSpikeSmoke() {}

  private static int failures;

  /**
   * Runs the smoke; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    final List<String> planets = List.of("Mercury", "Venus", "Earth", "Mars");

    final ElwhaSelectField<String> combo = ElwhaSelectField.filled("Planets");
    combo.setOptions(planets);
    check("multi defaults off", !combo.isMultiSelect());
    check(
        "single menu is SelectionMode.SINGLE",
        combo.optionsMenu().getSelectionMode() == SelectionMode.SINGLE);

    combo.setMultiSelect(true);
    check("multi turns on", combo.isMultiSelect());
    check(
        "multi menu is SelectionMode.MULTI",
        combo.optionsMenu().getSelectionMode() == SelectionMode.MULTI);
    check("empty selection -> empty values", combo.getSelectedValues().isEmpty());
    check("empty selection -> null single value", combo.getSelectedValue() == null);

    combo.toggleIndex(3); // Mars
    combo.toggleIndex(1); // Venus — toggled after Mars, but option order wins
    check("values are option-ordered", List.of("Venus", "Mars").equals(combo.getSelectedValues()));
    check("single value is the first in option order", "Venus".equals(combo.getSelectedValue()));
    check("provisional summary joins in option order", "Venus, Mars".equals(combo.getText()));
    check(
        "menu marks mirror the selection",
        combo.optionsMenu().getItems().get(1).isSelected()
            && combo.optionsMenu().getItems().get(3).isSelected()
            && !combo.optionsMenu().getItems().get(0).isSelected());

    combo.toggleIndex(1); // Venus off
    check("untoggle removes", List.of("Mars").equals(combo.getSelectedValues()));
    check("single value follows", "Mars".equals(combo.getSelectedValue()));

    combo.setSelectedValues(List.of("Pluto", "Earth", "Mercury", "Earth"));
    check(
        "setSelectedValues filters unknowns, dedupes, and orders",
        List.of("Mercury", "Earth").equals(combo.getSelectedValues()));
    check("summary follows setSelectedValues", "Mercury, Earth".equals(combo.getText()));

    combo.setSelectedValue("Mars");
    check(
        "setSelectedValue in multi mode becomes the whole selection",
        List.of("Mars").equals(combo.getSelectedValues()));
    combo.setSelectedValue(null);
    check("setSelectedValue(null) clears in multi mode", combo.getSelectedValues().isEmpty());
    check("cleared selection empties the field", combo.getText().isEmpty());

    combo.setSelectedValues(List.of("Venus", "Mars"));
    combo.setMultiSelect(false);
    check("multi off collapses to the first value", "Venus".equals(combo.getSelectedValue()));
    check("collapsed values are the singleton", List.of("Venus").equals(combo.getSelectedValues()));
    check("collapsed field shows the single display", "Venus".equals(combo.getText()));
    check(
        "collapsed menu is SINGLE again",
        combo.optionsMenu().getSelectionMode() == SelectionMode.SINGLE);

    combo.setMultiSelect(true);
    check(
        "multi on seeds from the single value", List.of("Venus").equals(combo.getSelectedValues()));

    combo.setEditable(true);
    check("editable forces multi off", !combo.isMultiSelect() && combo.isEditable());
    combo.setMultiSelect(true);
    check("multi forces editable off", combo.isMultiSelect() && !combo.isEditable());
    combo.setMultiSelect(false);

    final ElwhaSelectField<String> single = ElwhaSelectField.outlined("Planet");
    single.setOptions(planets);
    single.selectIndex(2);
    check("single-select regression: pick writes back", "Earth".equals(single.getText()));
    check(
        "single-select regression: getSelectedValues mirrors the single value",
        List.of("Earth").equals(single.getSelectedValues()));
    single.setSelectedValues(List.of("Pluto", "Mars"));
    check(
        "single mode setSelectedValues applies the first recognized value",
        "Mars".equals(single.getSelectedValue()));

    System.out.println(
        failures == 0 ? "PASS — all multi-spike checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
