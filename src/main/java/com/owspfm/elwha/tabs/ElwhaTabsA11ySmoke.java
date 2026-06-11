package com.owspfm.elwha.tabs;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;

/**
 * S6 headless guard for {@link ElwhaTabs} accessibility, keyboard logic, and RTL (#431): the {@code
 * PAGE_TAB_LIST}/{@code PAGE_TAB} role pairing, SELECTED state + state-change events, {@link
 * AccessibleSelection} mandatory-one semantics, the "click" accessible action's user-gesture
 * behavior, accessible names (label / icon-only / override), the arrow/Home/End action wiring incl.
 * wrap-around + the RTL direction flip + the disabled guard (the #432 pattern), auto-activate, and
 * RTL geometry mirroring for both layout modes and inline icons.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsA11ySmoke {

  private static final int BAR_WIDTH = 480;

  private ElwhaTabsA11ySmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkRolesAndStates();
    checkAccessibleSelection();
    checkAccessibleAction();
    checkAccessibleNames();
    checkKeyboardActions();
    checkRtlGeometry();

    System.out.println(
        "ElwhaTabsA11ySmoke: OK (tablist/tab roles, SELECTED + state events, mandatory-one"
            + " AccessibleSelection, click action semantics, names, arrows/Home/End + wrap + RTL"
            + " flip + disabled guard, auto-activate, RTL mirror both modes)");
  }

  private static void checkRolesAndStates() {
    final ElwhaTabs bar = laidOutBar();
    check(
        "bar role is PAGE_TAB_LIST",
        bar.getAccessibleContext().getAccessibleRole() == AccessibleRole.PAGE_TAB_LIST);
    check(
        "tab role is PAGE_TAB",
        bar.getTabAt(0).getAccessibleContext().getAccessibleRole() == AccessibleRole.PAGE_TAB);

    check(
        "active tab carries SELECTED",
        bar.getTabAt(0)
            .getAccessibleContext()
            .getAccessibleStateSet()
            .contains(AccessibleState.SELECTED));
    check(
        "inactive tab lacks SELECTED",
        !bar.getTabAt(1)
            .getAccessibleContext()
            .getAccessibleStateSet()
            .contains(AccessibleState.SELECTED));

    final int[] stateEvents = {0};
    bar.getTabAt(1)
        .getAccessibleContext()
        .addPropertyChangeListener(
            e -> {
              if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(e.getPropertyName())) {
                stateEvents[0]++;
              }
            });
    bar.setActiveTabIndex(1);
    check("activation fires an accessible state-change event", stateEvents[0] == 1);
  }

  private static void checkAccessibleSelection() {
    final ElwhaTabs bar = laidOutBar();
    final AccessibleSelection selection = bar.getAccessibleContext().getAccessibleSelection();
    check("bar exposes AccessibleSelection", selection != null);
    check("selection count is 1", selection.getAccessibleSelectionCount() == 1);
    check(
        "selected child is the active tab",
        selection.getAccessibleSelection(0) == bar.getActiveTab());
    check(
        "isAccessibleChildSelected tracks the index",
        selection.isAccessibleChildSelected(0) && !selection.isAccessibleChildSelected(1));

    final int[] changes = {0};
    bar.addChangeListener(e -> changes[0]++);
    selection.addAccessibleSelection(2);
    check("addAccessibleSelection activates", bar.getActiveTabIndex() == 2 && changes[0] == 1);

    selection.removeAccessibleSelection(2);
    selection.clearAccessibleSelection();
    check(
        "removal/clearing are no-ops (mandatory selection)",
        bar.getActiveTabIndex() == 2 && selection.getAccessibleSelectionCount() == 1);
  }

  private static void checkAccessibleAction() {
    final ElwhaTabs bar = laidOutBar();
    final ElwhaTab tab = bar.getTabAt(1);
    final int[] actions = {0};
    tab.addActionListener(e -> actions[0]++);

    final var action = tab.getAccessibleContext().getAccessibleAction();
    check(
        "tab exposes one accessible action",
        action != null
            && action.getAccessibleActionCount() == 1
            && "click".equals(action.getAccessibleActionDescription(0)));

    check("doAccessibleAction activates", action.doAccessibleAction(0));
    check(
        "accessible action is a user gesture (fires ActionListener)",
        bar.getActiveTabIndex() == 1 && actions[0] == 1);

    check(
        "doAccessibleAction on the active tab still reports performed",
        action.doAccessibleAction(0) && actions[0] == 1);

    bar.setEnabled(false);
    check("disabled tab refuses the action", !action.doAccessibleAction(0));
  }

  private static void checkAccessibleNames() {
    final ElwhaTab labeled = ElwhaTab.of("Photos");
    check(
        "label tab is named by its label",
        "Photos".equals(labeled.getAccessibleContext().getAccessibleName()));

    final ElwhaTab iconOnly = ElwhaTab.iconOnly(MaterialIcons.symbol("home"), "Home");
    check(
        "icon-only tab is named by its accessible label",
        "Home".equals(iconOnly.getAccessibleContext().getAccessibleName()));

    labeled.setAccessibleLabel("Media library");
    check(
        "setAccessibleLabel overrides the label",
        "Media library".equals(labeled.getAccessibleContext().getAccessibleName()));
    labeled.setAccessibleLabel(null);
    check(
        "clearing the override restores the label",
        "Photos".equals(labeled.getAccessibleContext().getAccessibleName()));
  }

  private static void checkKeyboardActions() {
    final ElwhaTabs bar = laidOutBar();
    final int[] changes = {0};
    bar.addChangeListener(e -> changes[0]++);

    // Focus motion is headless-untestable (no focus owner); the index math and activation
    // side-effects are: moveFocus drives the same path the arrow actions use.
    bar.setAutoActivate(true);
    bar.moveFocus(bar.getTabAt(0), true);
    check("auto-activate selects on focus move", bar.getActiveTabIndex() == 1 && changes[0] == 1);
    bar.moveFocus(bar.getTabAt(0), false);
    check(
        "backward move wraps to the last tab",
        bar.getActiveTabIndex() == bar.getTabCount() - 1 && changes[0] == 2);
    bar.moveFocus(bar.getTabAt(bar.getTabCount() - 1), true);
    check("forward move wraps to the first tab", bar.getActiveTabIndex() == 0 && changes[0] == 3);

    bar.setAutoActivate(false);
    bar.moveFocus(bar.getTabAt(0), true);
    check(
        "without auto-activate, focus moves do not select",
        bar.getActiveTabIndex() == 0 && changes[0] == 3);

    final ElwhaTab first = bar.getTabAt(0);
    final var rightAction = first.getActionMap().get("elwhaTabs.focusRight");
    final var activateAction = first.getActionMap().get("elwhaTab.activate");
    check("arrow + activate bindings installed", rightAction != null && activateAction != null);

    bar.setAutoActivate(true);
    rightAction.actionPerformed(new ActionEvent(first, ActionEvent.ACTION_PERFORMED, "right"));
    check("LTR right-arrow moves forward", bar.getActiveTabIndex() == 1);

    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    rightAction.actionPerformed(
        new ActionEvent(bar.getTabAt(1), ActionEvent.ACTION_PERFORMED, "right"));
    check("RTL right-arrow moves backward", bar.getActiveTabIndex() == 0);
    bar.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

    bar.setEnabled(false);
    final int before = bar.getActiveTabIndex();
    rightAction.actionPerformed(
        new ActionEvent(bar.getTabAt(before), ActionEvent.ACTION_PERFORMED, "right"));
    activateAction.actionPerformed(
        new ActionEvent(bar.getTabAt(1), ActionEvent.ACTION_PERFORMED, "activate"));
    check(
        "disabled bar ignores keyboard actions (the #432 guard)",
        bar.getActiveTabIndex() == before);
  }

  private static void checkRtlGeometry() {
    final ElwhaTabs fixed = laidOutBar();
    fixed.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    fixed.doLayout();
    check(
        "RTL fixed layout puts tab 0 at the trailing (right) edge",
        fixed.getTabAt(0).getX() + fixed.getTabAt(0).getWidth() == BAR_WIDTH);
    check(
        "RTL fixed layout puts the last tab leftmost",
        fixed.getTabAt(3).getX() == 0 || fixed.getTabAt(3).getX() == 1);
    check(
        "indicator follows the RTL active tab",
        fixed.currentIndicatorRect().x >= fixed.getActiveTab().getX());

    final ElwhaTabs scrollable = new ElwhaTabs(TabsVariant.PRIMARY);
    scrollable.setTabMode(TabMode.SCROLLABLE);
    for (int i = 1; i <= 8; i++) {
      scrollable.addTab("Section " + i);
    }
    scrollable.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    scrollable.setSize(300, 48);
    scrollable.doLayout();
    final ElwhaTab firstTab = scrollable.getTabAt(0);
    check(
        "RTL scrollable offset 0 right-aligns the first tab",
        firstTab.getX() + firstTab.getWidth() == 300);

    final ElwhaTab last = scrollable.getTabAt(7);
    scrollable.scrollToTab(last);
    check(
        "RTL scroll-to brings the last tab into view from the left",
        last.getX() >= 0 && last.getX() + last.getWidth() <= 300);

    final ElwhaTab iconTab = ElwhaTab.of(MaterialIcons.symbol("home"), "Travel");
    final ElwhaTabs secondaryRtl = ElwhaTabs.secondary();
    secondaryRtl.addTab(iconTab);
    secondaryRtl.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    secondaryRtl.setSize(300, 48);
    secondaryRtl.doLayout();
    check(
        "RTL inline icon sits on the leading (right) side of its label",
        iconTab.getIconBounds().x > iconTab.getWidth() / 2);
  }

  // ----------------------------------------------------------------- helpers

  private static ElwhaTabs laidOutBar() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    bar.addTab("One");
    bar.addTab("Two");
    bar.addTab("Three");
    bar.addTab("Four");
    bar.setSize(BAR_WIDTH, 48);
    bar.doLayout();
    return bar;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
