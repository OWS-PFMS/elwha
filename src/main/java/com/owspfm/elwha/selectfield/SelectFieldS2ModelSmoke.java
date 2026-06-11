package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless behavior smoke for epic #331 S2 (#375) — the typed option model and single-select
 * write-back as real API. Verifies {@code setSelectedValue} (programmatic select, menu-mark sync,
 * null-clear, value-not-in-options ignored), {@code addSelectionChangeListener} firing semantics
 * (on change, not on no-op, and on a menu-driven pick), the display function, and a non-{@code
 * String} value type.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldS2ModelSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS2ModelSmoke {

  private SelectFieldS2ModelSmoke() {}

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
    final ElwhaSelectField<String> sf = ElwhaSelectField.filled("Fruit");
    sf.setOptions(options);
    final ElwhaTextField field = (ElwhaTextField) sf.getComponent(0);

    final AtomicInteger fires = new AtomicInteger();
    final AtomicReference<String> last = new AtomicReference<>();
    sf.addSelectionChangeListener(
        v -> {
          fires.incrementAndGet();
          last.set(v);
        });

    // --- programmatic select ---
    sf.setSelectedValue("Banana");
    failures += check("Banana".equals(sf.getSelectedValue()), "setSelectedValue sets the value");
    checks++;
    failures += check("Banana".equals(field.getText()), "setSelectedValue writes the field text");
    checks++;
    final ElwhaMenu menu = sf.optionsMenu();
    failures +=
        check(
            menu.getItems().get(1).isSelected() && !menu.getItems().get(0).isSelected(),
            "setSelectedValue syncs the menu's selected mark");
    checks++;
    failures += check(fires.get() == 1 && "Banana".equals(last.get()), "listener fired with value");
    checks++;

    // --- no-op set does not fire ---
    sf.setSelectedValue("Banana");
    failures += check(fires.get() == 1, "no-op set to current value does not fire");
    checks++;

    // --- value not among options is ignored ---
    sf.setSelectedValue("Durian");
    failures +=
        check(
            "Banana".equals(sf.getSelectedValue()) && fires.get() == 1,
            "value not in options is ignored");
    checks++;

    // --- null clears ---
    sf.setSelectedValue(null);
    failures += check(sf.getSelectedValue() == null, "null clears the value");
    checks++;
    failures += check(field.getText().isEmpty(), "null clears the field text");
    checks++;
    failures +=
        check(
            !menu.getItems().get(1).isSelected(), "null clears the menu marks (no item selected)");
    checks++;
    failures += check(fires.get() == 2 && last.get() == null, "listener fired on clear");
    checks++;

    // --- a menu-driven pick also fires ---
    sf.selectIndex(2);
    failures +=
        check(
            "Cherry".equals(sf.getSelectedValue())
                && fires.get() == 3
                && "Cherry".equals(last.get()),
            "a menu pick fires the listener");
    checks++;

    // --- removed listener stops firing ---
    final java.util.function.Consumer<String> noop = v -> {};
    sf.addSelectionChangeListener(noop);
    sf.removeSelectionChangeListener(noop);
    final int before = fires.get();
    sf.setSelectedValue("Apple");
    failures += check(fires.get() == before + 1, "remaining listener still fires after a remove");
    checks++;

    // --- typed (non-String) value + display function ---
    final ElwhaSelectField<Integer> nums = ElwhaSelectField.outlined("Count");
    nums.setDisplayFunction(n -> "#" + n);
    nums.setOptions(List.of(10, 20, 30));
    final AtomicReference<Integer> picked = new AtomicReference<>();
    nums.addSelectionChangeListener(picked::set);
    nums.setSelectedValue(20);
    final ElwhaTextField numsField = (ElwhaTextField) nums.getComponent(0);
    failures += check(Integer.valueOf(20).equals(nums.getSelectedValue()), "typed value selected");
    checks++;
    failures += check("#20".equals(numsField.getText()), "display function applied to set value");
    checks++;
    failures += check(Integer.valueOf(20).equals(picked.get()), "typed listener fired");
    checks++;

    System.out.println(
        "SelectFieldS2ModelSmoke: " + (checks - failures) + "/" + checks + " checks passed");
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
