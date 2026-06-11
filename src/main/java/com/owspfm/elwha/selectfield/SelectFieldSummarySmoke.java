package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless smoke for Phase 3 S2 (#398): the multi-select summary display + change listeners.
 * Asserts the field joins display strings in option order up to the summary limit, collapses to the
 * count form past it (boundary included), {@code setSummaryLimit} clamps and re-renders
 * immediately, clearing rests the field, and the multi-selection-change listeners fire per toggle /
 * per effective set with the option-ordered snapshot — never for no-ops, never in single mode, and
 * the single-value listeners never fire in multi mode.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldSummarySmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldSummarySmoke {

  private SelectFieldSummarySmoke() {}

  private static int failures;

  /**
   * Runs the smoke; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    final ElwhaSelectField<String> combo = ElwhaSelectField.filled("Toppings");
    combo.setOptions(List.of("Mushroom", "Pepperoni", "Onion", "Olive", "Basil"));
    combo.setMultiSelect(true);

    final List<List<String>> multiEvents = new ArrayList<>();
    combo.addMultiSelectionChangeListener(multiEvents::add);
    final List<String> singleEvents = new ArrayList<>();
    combo.addSelectionChangeListener(singleEvents::add);

    check("default summary limit is 3", combo.getSummaryLimit() == 3);

    combo.toggleIndex(4); // Basil
    combo.toggleIndex(1); // Pepperoni
    check("two values join in option order", "Pepperoni, Basil".equals(combo.getText()));
    check(
        "listener fired per toggle with option-ordered snapshots",
        List.of(List.of("Basil"), List.of("Pepperoni", "Basil")).equals(multiEvents));

    combo.toggleIndex(0); // Mushroom — 3 values, at the limit
    check("at the limit still joins", "Mushroom, Pepperoni, Basil".equals(combo.getText()));

    combo.toggleIndex(2); // Onion — 4 values, past the limit
    check("past the limit collapses to the count form", "4 selected".equals(combo.getText()));
    check("four per-toggle events so far", multiEvents.size() == 4);

    combo.setSummaryLimit(4);
    check(
        "raising the limit re-renders immediately",
        "Mushroom, Pepperoni, Onion, Basil".equals(combo.getText()));
    combo.setSummaryLimit(-5);
    check("negative limit clamps to 0", combo.getSummaryLimit() == 0);
    check("limit 0 shows the count form for any selection", "4 selected".equals(combo.getText()));
    check("limit changes never fire listeners", multiEvents.size() == 4);
    combo.setSummaryLimit(3);

    combo.setSelectedValues(List.of("Olive", "Mushroom"));
    check("setSelectedValues summary follows", "Mushroom, Olive".equals(combo.getText()));
    check(
        "effective set fires once with the result",
        multiEvents.size() == 5 && List.of("Mushroom", "Olive").equals(multiEvents.get(4)));
    combo.setSelectedValues(List.of("Mushroom", "Olive"));
    check("no-op set (same option-ordered result) does not fire", multiEvents.size() == 5);

    combo.setSelectedValues(List.of());
    check("clearing empties the field (floating label rests)", combo.getText().isEmpty());
    check(
        "clearing fires with the empty snapshot",
        multiEvents.size() == 6 && multiEvents.get(5).isEmpty());

    check("single-value listeners never fired in multi mode", singleEvents.isEmpty());

    final ElwhaSelectField<String> single = ElwhaSelectField.outlined("Topping");
    single.setOptions(List.of("Mushroom", "Pepperoni"));
    final List<List<String>> singleModeMultiEvents = new ArrayList<>();
    single.addMultiSelectionChangeListener(singleModeMultiEvents::add);
    single.selectIndex(0);
    check("multi listeners never fire in single mode", singleModeMultiEvents.isEmpty());

    System.out.println(
        failures == 0 ? "PASS — all summary checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
