package com.owspfm.elwha.menu;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Headless behavior-matrix smoke for epic #298 S6 — the {@link SelectionMode} axis. Drives item
 * activation directly (no overlay mount) and asserts the NONE / SINGLE / MULTI contract from design
 * §9, plus the accessibility and layout-stability rules:
 *
 * <ul>
 *   <li><strong>NONE</strong> (the default) — activating an item selects nothing; no selection
 *       change fires.
 *   <li><strong>SINGLE</strong> — activating selects exactly one and auto-deselects the prior; a
 *       change fires per activation; the selected item reports {@code SELECTED} but not {@code
 *       CHECKED} (radio-like).
 *   <li><strong>MULTI</strong> — activating toggles; selections accumulate and a re-activation
 *       deselects; a selected item reports both {@code SELECTED} and {@code CHECKED}
 *       (checkbox-like).
 *   <li><strong>Layout stability</strong> — in a selection mode every item reserves the leading
 *       check-column, so a no-icon item is the same width selected or not (toggling never reflows).
 * </ul>
 *
 * Runs fully headless. Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSelectionSmoke {

  private MenuSelectionSmoke() {}

  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    MorphAnimator.setReducedMotion(true);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    int checks = 0;
    int failures = 0;

    // --- enum surface ---
    failures += check(SelectionMode.values().length == 3, "SelectionMode has 3 values");
    checks++;

    // --- NONE: default, no persistent selection ---
    final AtomicInteger noneChanges = new AtomicInteger();
    final ElwhaMenu none =
        menu(SelectionMode.NONE, i -> noneChanges.incrementAndGet(), "Rename", "Duplicate");
    failures += check(none.getSelectionMode() == SelectionMode.NONE, "NONE is the default mode");
    checks++;
    none.getItems().get(0).activate(0);
    failures += check(none.getSelectedItems().isEmpty(), "NONE: activation selects nothing");
    checks++;
    failures += check(noneChanges.get() == 0, "NONE: no selection-change fires");
    checks++;

    // --- SINGLE: one at a time, auto-deselect, change fires ---
    final AtomicInteger singleChanges = new AtomicInteger();
    final ElwhaMenu single =
        menu(SelectionMode.SINGLE, i -> singleChanges.incrementAndGet(), "List", "Grid", "Gallery");
    single.getItems().get(0).activate(0);
    failures +=
        check(
            single.getSelectedItems().size() == 1 && single.getItems().get(0).isSelected(),
            "SINGLE: first activation selects exactly one");
    checks++;
    single.getItems().get(1).activate(0);
    failures +=
        check(
            single.getSelectedItems().size() == 1
                && single.getItems().get(1).isSelected()
                && !single.getItems().get(0).isSelected(),
            "SINGLE: selecting another auto-deselects the prior");
    checks++;
    failures += check(singleChanges.get() == 2, "SINGLE: a change fires per activation");
    checks++;
    failures +=
        check(
            stateSet(single.getItems().get(1)).contains(AccessibleState.SELECTED)
                && !stateSet(single.getItems().get(1)).contains(AccessibleState.CHECKED),
            "SINGLE: selected reports SELECTED, not CHECKED (radio-like)");
    checks++;

    // --- MULTI: toggle, accumulate, change fires; CHECKED reported ---
    final AtomicInteger multiChanges = new AtomicInteger();
    final ElwhaMenu multi =
        menu(SelectionMode.MULTI, i -> multiChanges.incrementAndGet(), "Name", "Modified", "Size");
    multi.getItems().get(0).activate(0);
    multi.getItems().get(1).activate(0);
    failures += check(multi.getSelectedItems().size() == 2, "MULTI: selections accumulate");
    checks++;
    multi.getItems().get(0).activate(0);
    failures +=
        check(
            multi.getSelectedItems().size() == 1 && !multi.getItems().get(0).isSelected(),
            "MULTI: re-activating toggles off");
    checks++;
    failures += check(multiChanges.get() == 3, "MULTI: a change fires per toggle");
    checks++;
    // After the toggles above, item 1 is the one left selected.
    final ElwhaMenuItem checkedItem = multi.getItems().get(1);
    failures +=
        check(
            checkedItem.isSelected()
                && stateSet(checkedItem).contains(AccessibleState.SELECTED)
                && stateSet(checkedItem).contains(AccessibleState.CHECKED),
            "MULTI: selected reports both SELECTED and CHECKED (checkbox-like)");
    checks++;

    // --- layout stability: selection mode reserves the check-column ---
    // A bare item (NONE / no reserve) has no leading column; the same item in a selection mode
    // reserves it, so its label sits where the ✓ will land and toggling never reflows.
    final int widthNoneMode = ElwhaMenuItem.of("Plain").getPreferredSize().width;
    final ElwhaMenuItem plainSingle = ElwhaMenuItem.of("Plain");
    ElwhaMenu.builder().selectionMode(SelectionMode.SINGLE).addItem(plainSingle).build();
    final int widthSelMode = plainSingle.getPreferredSize().width;
    failures +=
        check(
            widthSelMode > widthNoneMode,
            "selection mode reserves the leading check-column (wider, layout-stable)");
    checks++;
    final int widthUnselected = plainSingle.getPreferredSize().width;
    plainSingle.setSelected(true);
    failures +=
        check(
            plainSingle.getPreferredSize().width == widthUnselected,
            "toggling selection does not change item width (no reflow)");
    checks++;

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static ElwhaMenu menu(
      final SelectionMode mode,
      final java.util.function.Consumer<ElwhaMenuItem> onChange,
      final String... labels) {
    final ElwhaMenu.Builder b = ElwhaMenu.builder().selectionMode(mode);
    if (onChange != null) {
      b.onSelectionChange(onChange);
    }
    for (final String label : labels) {
      b.addItem(ElwhaMenuItem.of(MaterialIcons.star(20), label));
    }
    return b.build();
  }

  private static AccessibleStateSet stateSet(final ElwhaMenuItem item) {
    return item.getAccessibleContext().getAccessibleStateSet();
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
