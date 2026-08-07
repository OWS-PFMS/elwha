package com.owspfm.elwha.tabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import java.awt.FocusTraversalPolicy;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of tab keyboard navigation and accessibility — design doc §8: the arrow/Home/End
 * InputMap wiring, the wrap-around roving-focus policy, the single-stop traversal cycle, and the
 * ARIA-shaped {@code PAGE_TAB_LIST} / {@code PAGE_TAB} pairing with a mandatory single selection.
 *
 * <p>Real focus <em>ownership</em> is display-bound and belongs to the gui tier. What is checked
 * here is everything that does not need a focus owner: which keystroke is bound to which action,
 * what the traversal policy offers, and where the roving-focus move lands — observable headlessly
 * through auto-activation and through the scroll it triggers.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTabsKeyboardAccessibilityTest {

  /** A bar in follow-focus mode — the opt-in the arrow-navigation cases exercise. */
  private static ElwhaTabs autoActivatingBarOf(final int count) {
    final ElwhaTabs bar = barOf(count);
    bar.setAutoActivate(true);
    return bar;
  }

  private static ElwhaTabs barOf(final int count) {
    final ElwhaTabs bar = new ElwhaTabs();
    for (int i = 0; i < count; i++) {
      bar.addTab("Tab " + i);
    }
    bar.setSize(400, bar.getPreferredSize().height);
    bar.doLayout();
    return bar;
  }

  private static Object binding(final JComponent tab, final String keystroke) {
    return tab.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(keystroke));
  }

  // ------------------------------------------------------------ InputMap

  @ParameterizedTest
  @CsvSource({
    "pressed RIGHT, elwhaTabs.focusRight",
    "pressed LEFT, elwhaTabs.focusLeft",
    "pressed HOME, elwhaTabs.focusFirst",
    "pressed END, elwhaTabs.focusLast"
  })
  void everyNavigationKeyIsBoundOnEveryTab(final String keystroke, final String action) {
    final ElwhaTabs bar = barOf(4);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(binding(bar.getTabAt(i), keystroke))
          .as("§8 — '%s' navigates from any tab, not just the active one", keystroke)
          .isEqualTo(action);
    }
  }

  @Test
  void aRemovedTabLosesItsNavigationBindings() {
    final ElwhaTabs bar = barOf(3);
    final ElwhaTab removed = bar.getTabAt(1);

    bar.removeTab(removed);

    assertThat(binding(removed, "pressed RIGHT"))
        .as("§8 — a detached tab must not keep driving the bar it left")
        .isNull();
    assertThat(removed.getActionMap().get("elwhaTabs.focusRight")).isNull();
  }

  // -------------------------------------------------------- roving focus

  @Test
  void rightMovesTheRovingFocusForward() {
    final ElwhaTabs bar = autoActivatingBarOf(4);

    Input.pressBoundKey(bar.getTabAt(1), "pressed RIGHT", "elwhaTabs.focusRight");

    assertThat(bar.getActiveTabIndex())
        .as("§8 — with auto-activation on, moving focus selects as it goes")
        .isEqualTo(2);
  }

  @Test
  void leftMovesTheRovingFocusBackward() {
    final ElwhaTabs bar = autoActivatingBarOf(4);
    bar.setActiveTabIndex(2);

    Input.pressBoundKey(bar.getTabAt(2), "pressed LEFT", "elwhaTabs.focusLeft");

    assertThat(bar.getActiveTabIndex()).isEqualTo(1);
  }

  @Test
  void focusWrapsAroundAtTheEnds() {
    final ElwhaTabs bar = autoActivatingBarOf(4);

    Input.pressBoundKey(bar.getTabAt(0), "pressed LEFT", "elwhaTabs.focusLeft");
    assertThat(bar.getActiveTabIndex())
        .as("§8 — arrow navigation is a ring, so back from the first lands on the last")
        .isEqualTo(3);

    Input.pressBoundKey(bar.getTabAt(3), "pressed RIGHT", "elwhaTabs.focusRight");
    assertThat(bar.getActiveTabIndex())
        .as("and forward from the last comes round to the first")
        .isZero();
  }

  @Test
  void homeAndEndJumpToTheEdges() {
    final ElwhaTabs bar = autoActivatingBarOf(5);
    bar.setActiveTabIndex(2);

    Input.pressBoundKey(bar.getTabAt(2), "pressed END", "elwhaTabs.focusLast");
    assertThat(bar.getActiveTabIndex()).isEqualTo(4);

    Input.pressBoundKey(bar.getTabAt(4), "pressed HOME", "elwhaTabs.focusFirst");
    assertThat(bar.getActiveTabIndex()).isZero();
  }

  @Test
  void rightToLeftSwapsWhichArrowGoesForward() {
    final ElwhaTabs bar = autoActivatingBarOf(4);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    bar.setActiveTabIndex(1);

    Input.pressBoundKey(bar.getTabAt(1), "pressed RIGHT", "elwhaTabs.focusRight");

    assertThat(bar.getActiveTabIndex())
        .as("§8 — under RTL the right arrow moves toward the visually-right, earlier tab")
        .isZero();
  }

  @Test
  void aDisabledBarIgnoresArrowNavigation() {
    final ElwhaTabs bar = autoActivatingBarOf(4);
    bar.setEnabled(false);

    Input.pressBoundKey(bar.getTabAt(0), "pressed RIGHT", "elwhaTabs.focusRight");

    assertThat(bar.getActiveTabIndex())
        .as("#432 — the action checks the bar's own enabled state, not the Action's")
        .isZero();
  }

  @Test
  void aSingleTabBarHasNowhereToMove() {
    final ElwhaTabs bar = autoActivatingBarOf(1);

    Input.pressBoundKey(bar.getTabAt(0), "pressed RIGHT", "elwhaTabs.focusRight");

    assertThat(bar.getActiveTabIndex()).as("wrapping onto itself is a no-op, not a loop").isZero();
  }

  @Test
  void movingFocusWithoutAutoActivationLeavesTheSelectionAlone() {
    final ElwhaTabs bar = barOf(4);
    Input.pressBoundKey(bar.getTabAt(0), "pressed RIGHT", "elwhaTabs.focusRight");

    assertThat(bar.getActiveTabIndex())
        .as("§8 — manual activation means arrows browse; Space or Enter commits")
        .isZero();
  }

  @Test
  void browsingWithoutAutoActivationStillScrollsTheTargetIntoView() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 0; i < 10; i++) {
      bar.addTab("Section " + i);
    }
    bar.setSize(300, bar.getPreferredSize().height);
    bar.doLayout();

    Input.pressBoundKey(bar.getTabAt(0), "pressed END", "elwhaTabs.focusLast");
    bar.doLayout();

    final ElwhaTab last = bar.getTabAt(9);
    assertThat(last.getX() + last.getWidth())
        .as("§7 — the tab being browsed to is brought into view even when not activated")
        .isLessThanOrEqualTo(300);
  }

  @Test
  void autoActivationIsOffByDefault() {
    assertThat(new ElwhaTabs().isAutoActivate())
        .as("§8 — arrows browse and Space/Enter commits, matching material-web's default")
        .isFalse();
  }

  @Test
  void autoActivationIsOptIn() {
    final ElwhaTabs bar = barOf(3);

    bar.setAutoActivate(true);
    Input.pressBoundKey(bar.getTabAt(0), "pressed RIGHT", "elwhaTabs.focusRight");

    assertThat(bar.getActiveTabIndex())
        .as("§8 — the material-web auto-activate attribute makes focus moves select")
        .isEqualTo(1);
  }

  // --------------------------------------------------- traversal policy

  @Test
  void barOffersExactlyOneTabStop() {
    final ElwhaTabs bar = barOf(4);
    final FocusTraversalPolicy policy = bar.getFocusTraversalPolicy();

    assertThat(policy.getComponentAfter(bar, bar.getTabAt(0)))
        .as("§8 — Tab leaves the bar rather than walking every tab; arrows move within")
        .isNull();
    assertThat(policy.getComponentBefore(bar, bar.getTabAt(1))).isNull();
  }

  @Test
  void tabStopIsTheActiveTab() {
    final ElwhaTabs bar = barOf(4);
    bar.setActiveTabIndex(2);
    final FocusTraversalPolicy policy = bar.getFocusTraversalPolicy();

    assertThat(policy.getDefaultComponent(bar))
        .as("§8 — Tab enters the bar on the current selection, the roving-stop rule")
        .isSameAs(bar.getTabAt(2));
    assertThat(policy.getFirstComponent(bar)).isSameAs(bar.getTabAt(2));
    assertThat(policy.getLastComponent(bar)).isSameAs(bar.getTabAt(2));
  }

  @Test
  void anEmptyBarOffersNoTabStop() {
    final ElwhaTabs bar = new ElwhaTabs();

    assertThat(bar.getFocusTraversalPolicy().getDefaultComponent(bar))
        .as("nothing to focus, and no exception on the way to finding that out")
        .isNull();
  }

  @Test
  void barProvidesItsOwnTraversalPolicy() {
    final ElwhaTabs bar = new ElwhaTabs();

    assertThat(bar.isFocusTraversalPolicyProvider())
        .as("§8 — the single-stop policy only takes effect if the bar provides it")
        .isTrue();
    assertThat(bar.isFocusable()).as("the bar itself is never a tab stop; its tabs are").isFalse();
  }

  // ------------------------------------------------------- accessibility

  @Test
  void barIsATabList() {
    assertThat(barOf(3).getAccessibleContext().getAccessibleRole())
        .as("§8 — the ARIA tablist mapping")
        .isEqualTo(AccessibleRole.PAGE_TAB_LIST);
  }

  @Test
  void eachTabIsAPageTab() {
    final ElwhaTabs bar = barOf(3);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).getAccessibleContext().getAccessibleRole())
          .as("§8 — the tablist/tab pairing AT relies on, tab %d", i)
          .isEqualTo(AccessibleRole.PAGE_TAB);
    }
  }

  @Test
  void onlyTheActiveTabReportsSelected() {
    final ElwhaTabs bar = barOf(3);
    bar.setActiveTabIndex(1);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(
              bar.getTabAt(i)
                  .getAccessibleContext()
                  .getAccessibleStateSet()
                  .contains(AccessibleState.SELECTED))
          .as("§8 — the selected state tracks the active tab, tab %d", i)
          .isEqualTo(i == 1);
    }
  }

  @Test
  void barReportsExactlyOneSelectedChild() {
    final ElwhaTabs bar = barOf(3);
    bar.setActiveTabIndex(2);
    final AccessibleSelection selection = bar.getAccessibleContext().getAccessibleSelection();

    assertThat(selection.getAccessibleSelectionCount())
        .as("§8 — single, mandatory selection, the JTabbedPane shape")
        .isOne();
    assertThat(selection.getAccessibleSelection(0)).isSameAs(bar.getTabAt(2));
    assertThat(selection.isAccessibleChildSelected(2)).isTrue();
    assertThat(selection.isAccessibleChildSelected(0)).isFalse();
  }

  @Test
  void anEmptyBarReportsNoSelection() {
    assertThat(
            new ElwhaTabs()
                .getAccessibleContext()
                .getAccessibleSelection()
                .getAccessibleSelectionCount())
        .isZero();
  }

  @Test
  void selectingThroughAccessibilityActivatesTheTab() {
    final ElwhaTabs bar = barOf(3);

    bar.getAccessibleContext().getAccessibleSelection().addAccessibleSelection(2);

    assertThat(bar.getActiveTabIndex())
        .as("§8 — AT can drive the selection, not just observe it")
        .isEqualTo(2);
  }

  @Test
  void selectionCannotBeClearedThroughAccessibility() {
    final ElwhaTabs bar = barOf(3);
    bar.setActiveTabIndex(1);
    final AccessibleSelection selection = bar.getAccessibleContext().getAccessibleSelection();

    selection.removeAccessibleSelection(1);
    selection.clearAccessibleSelection();

    assertThat(bar.getActiveTabIndex())
        .as("§8 — the selection is mandatory; a tab can be replaced but never deselected")
        .isEqualTo(1);
  }

  @Test
  void selectingAllIsANoOpOnASingleSelectionBar() {
    final ElwhaTabs bar = barOf(3);

    bar.getAccessibleContext().getAccessibleSelection().selectAllAccessibleSelection();

    assertThat(bar.getAccessibleContext().getAccessibleSelection().getAccessibleSelectionCount())
        .as("§8 — there is no multi-select state to reach")
        .isOne();
  }

  @Test
  void aTabExposesAnActivateAction() {
    final ElwhaTabs bar = barOf(3);
    final AccessibleAction action = bar.getTabAt(2).getAccessibleContext().getAccessibleAction();

    assertThat(action.getAccessibleActionCount()).as("§8 — one action: select this tab").isOne();
    assertThat(action.getAccessibleActionDescription(0)).isNotBlank();

    assertThat(action.doAccessibleAction(0)).isTrue();
    assertThat(bar.getActiveTabIndex()).isEqualTo(2);
  }

  @Test
  void aTabsAccessibleNameIsItsLabel() {
    final ElwhaTabs bar = barOf(3);

    assertThat(bar.getTabAt(1).getAccessibleContext().getAccessibleName())
        .as("§8 — a labelled tab needs no extra naming from the consumer")
        .isEqualTo("Tab 1");
  }
}
