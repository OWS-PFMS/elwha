package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleText;

/**
 * Headless smoke for Phase 3 S3 (#399): the multi-select keyboard + a11y wiring observable without
 * a display. Asserts the combo's MULTI menu items are checkbox-like through the combo (toggling
 * gains/loses {@code AccessibleState.CHECKED} + {@code SELECTED}; the single-select items never
 * report {@code CHECKED}), the arrow's expanded/collapsed accessible name + state announcement is
 * unchanged in multi mode, the live summary reaches the embedded editor's accessible-text path, and
 * the editable exclusion holds (multi forces non-editable — keyboard parity is the documented
 * deferral). The window-dependent half — Down/Enter/Space open, roving focus, Enter/Space
 * toggle-without-close, Esc focus-return — is exercised by {@code SelectFieldMultiKeyboardDemo}
 * (the menu-side bindings themselves are guarded by {@code menu.MenuSelectionSmoke}).
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldMultiA11ySmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldMultiA11ySmoke {

  private SelectFieldMultiA11ySmoke() {}

  private static int failures;

  /**
   * Runs the smoke; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    final ElwhaSelectField<String> combo = ElwhaSelectField.filled("Instruments");
    combo.setOptions(List.of("Violin", "Viola", "Cello"));
    combo.setMultiSelect(true);

    combo.toggleIndex(0);
    final ElwhaMenuItem checkedItem = combo.optionsMenu().getItems().get(0);
    final ElwhaMenuItem uncheckedItem = combo.optionsMenu().getItems().get(1);
    check(
        "toggled item reports CHECKED + SELECTED",
        hasState(checkedItem, AccessibleState.CHECKED)
            && hasState(checkedItem, AccessibleState.SELECTED));
    check(
        "untoggled item reports neither",
        !hasState(uncheckedItem, AccessibleState.CHECKED)
            && !hasState(uncheckedItem, AccessibleState.SELECTED));
    combo.toggleIndex(0);
    check("untoggling drops CHECKED", !hasState(checkedItem, AccessibleState.CHECKED));

    final ElwhaSelectField<String> single = ElwhaSelectField.outlined("Instrument");
    single.setOptions(List.of("Violin", "Viola"));
    single.selectIndex(0);
    final ElwhaMenuItem selectedSingle = single.optionsMenu().getItems().get(0);
    check(
        "single-select items are radio-like (SELECTED, never CHECKED)",
        hasState(selectedSingle, AccessibleState.SELECTED)
            && !hasState(selectedSingle, AccessibleState.CHECKED));

    final ElwhaTextField field = (ElwhaTextField) combo.getComponent(0);
    final ElwhaIconButton arrow = field.getTrailingIconButton();
    final AccessibleContext arrowCtx = arrow.getAccessibleContext();
    final AtomicReference<Object> lastState = new AtomicReference<>();
    arrowCtx.addPropertyChangeListener(
        evt -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(evt.getPropertyName())) {
            lastState.set(evt.getNewValue());
          }
        });
    check("arrow rests collapsed", "Open options".equals(arrowCtx.getAccessibleName()));
    combo.applyExpandedState(true);
    check(
        "expand flips the arrow name + announces EXPANDED in multi mode",
        "Close options".equals(arrowCtx.getAccessibleName())
            && lastState.get() == AccessibleState.EXPANDED);
    combo.applyExpandedState(false);
    check(
        "collapse flips back + announces COLLAPSED",
        "Open options".equals(arrowCtx.getAccessibleName())
            && lastState.get() == AccessibleState.COLLAPSED);

    combo.setSelectedValues(List.of("Violin", "Cello"));
    final AccessibleText accessibleText =
        field.getEditor().getAccessibleContext().getAccessibleText();
    check("editor exposes an accessible-text path", accessibleText != null);
    check(
        "summary reaches the accessible value path",
        "Violin, Cello".equals(field.getEditor().getText())
            && accessibleText != null
            && accessibleText.getCharCount() == "Violin, Cello".length());

    combo.setEditable(true);
    check(
        "editable+multi exclusion holds (multi forces non-editable; parity is the documented"
            + " deferral)",
        combo.isEditable() && !combo.isMultiSelect());

    System.out.println(
        failures == 0 ? "PASS — all multi a11y checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static boolean hasState(final ElwhaMenuItem item, final AccessibleState state) {
    return item.getAccessibleContext().getAccessibleStateSet().contains(state);
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
