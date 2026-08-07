package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaItemList}'s selection semantics — the chip family's superset the
 * spec adopts wholesale, including the two card-family defects it exists to fix: selection lost on
 * filter, and chrome left stale after a single-selection change.
 *
 * <p>Modifier-bearing gestures are dispatched through the real event pipeline with explicit {@code
 * *_DOWN_MASK} values, since the testkit's {@code Input} helper deliberately sends none.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaItemListSelectionTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 400;

  private DefaultElwhaListModel<String> model;
  private ElwhaItemList<String> list;

  private ElwhaItemList<String> listOf(final String... items) {
    model = new DefaultElwhaListModel<>(List.of(items));
    list = new ElwhaItemList<>(model, (item, index) -> ElwhaChip.filterChip(item));
    list.setSize(WIDTH, HEIGHT);
    list.doLayout();
    return list;
  }

  /** Dispatches a press/release pair carrying the given extended modifier mask. */
  private static void clickWith(final Component target, final int modifiers) {
    dispatch(target, MouseEvent.MOUSE_PRESSED, modifiers | MouseEvent.BUTTON1_DOWN_MASK);
    dispatch(target, MouseEvent.MOUSE_RELEASED, modifiers);
  }

  private static void dispatch(final Component target, final int id, final int modifiers) {
    // The explicit-absolute-coordinate constructor: the shorter one calls getLocationOnScreen,
    // which throws on an unrealized component in the headless tier.
    target.dispatchEvent(
        new MouseEvent(
            target, id, System.nanoTime(), modifiers, 4, 4, 4, 4, 1, false, MouseEvent.BUTTON1));
  }

  private void click(final String item) {
    Input.click(list.getComponentFor(item), 4, 4);
  }

  // ------------------------------------------------------------------- NONE

  @Test
  void noneModeIgnoresClicks() {
    listOf("a", "b");

    click("a");

    assertThat(list.getSelectionModel().getSelected())
        .as("NONE never selects through list interaction")
        .isEmpty();
  }

  // ----------------------------------------------------------------- SINGLE

  @Test
  void singleModeSelectsThenDeselectsTheSameItem() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE);

    click("a");
    assertThat(list.getSelectionModel().getSelected()).as("a click selects").containsExactly("a");

    click("a");
    assertThat(list.getSelectionModel().getSelected())
        .as("SINGLE is toggleable — clicking the selected item deselects it")
        .isEmpty();
  }

  @Test
  void singleModeCollapsesToTheClickedItem() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.SINGLE);

    click("a");
    click("b");

    assertThat(list.getSelectionModel().getSelected())
        .as("SINGLE holds at most one item")
        .containsExactly("b");
  }

  @Test
  void singleModeSyncsChromeOnEveryRenderedView() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE);

    click("a");
    click("b");

    assertThat(((ElwhaChip) list.getComponentFor("a")).isSelected())
        .as("the previously selected view drops its selected chrome")
        .isFalse();
    assertThat(((ElwhaChip) list.getComponentFor("b")).isSelected())
        .as("the newly selected view paints as selected")
        .isTrue();
  }

  // ------------------------------------------------------- SINGLE_MANDATORY

  @Test
  void mandatoryModeSeedsTheFirstVisibleItemOnEntry() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.SINGLE_MANDATORY);

    assertThat(list.getSelectionModel().getSelected())
        .as("entering SINGLE_MANDATORY seeds the first visible item")
        .containsExactly("a");
  }

  @Test
  void mandatoryModeRefusesToDeselect() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE_MANDATORY);

    click("b");
    click("b");

    assertThat(list.getSelectionModel().getSelected())
        .as("clicking the selected item is a no-op — the selection is never empty")
        .containsExactly("b");
  }

  @Test
  void mandatoryModeReseedsAfterTheSelectedItemIsRemoved() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.SINGLE_MANDATORY);
    click("b");

    model.remove("b");

    assertThat(list.getSelectionModel().getSelected())
        .as("removing the selected item re-seeds the first remaining visible item")
        .containsExactly("a");
  }

  @Test
  void mandatoryModeSeedsAgainstVisibleOrderNotModelOrder() {
    listOf("a", "b", "c")
        .setFilter(item -> !"a".equals(item))
        .setSelectionMode(SelectionMode.SINGLE_MANDATORY);

    assertThat(list.getSelectionModel().getSelected())
        .as("the seed is the first item actually showing, not the first in the model")
        .containsExactly("b");
  }

  @Test
  void mandatoryModeSeedsNothingWhenNothingIsVisible() {
    listOf().setSelectionMode(SelectionMode.SINGLE_MANDATORY);

    assertThat(list.getSelectionModel().getSelected())
        .as("an empty list has nothing to seed")
        .isEmpty();
  }

  // --------------------------------------------------------------- MULTIPLE

  @Test
  void multipleModePlainClickCollapsesToTheClickedItem() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.MULTIPLE);

    clickWith(list.getComponentFor("a"), InputEvent.CTRL_DOWN_MASK);
    clickWith(list.getComponentFor("b"), InputEvent.CTRL_DOWN_MASK);
    click("c");

    assertThat(list.getSelectionModel().getSelected())
        .as("an unmodified click collapses a multi-selection to the clicked item")
        .containsExactly("c");
  }

  @Test
  void multipleModeCtrlClickTogglesOneItem() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.MULTIPLE);

    clickWith(list.getComponentFor("a"), InputEvent.CTRL_DOWN_MASK);
    clickWith(list.getComponentFor("c"), InputEvent.CTRL_DOWN_MASK);

    assertThat(list.getSelectionModel().getSelected())
        .as("Ctrl-click adds without collapsing")
        .containsExactly("a", "c");

    clickWith(list.getComponentFor("a"), InputEvent.CTRL_DOWN_MASK);
    assertThat(list.getSelectionModel().getSelected())
        .as("Ctrl-click on a selected item removes it")
        .containsExactly("c");
  }

  @Test
  void multipleModeMetaClickTogglesLikeCtrl() {
    listOf("a", "b").setSelectionMode(SelectionMode.MULTIPLE);

    clickWith(list.getComponentFor("a"), InputEvent.META_DOWN_MASK);
    clickWith(list.getComponentFor("b"), InputEvent.META_DOWN_MASK);

    assertThat(list.getSelectionModel().getSelected())
        .as("Cmd-click toggles the same way Ctrl-click does")
        .containsExactly("a", "b");
  }

  @Test
  void multipleModeShiftClickExtendsARangeOverVisibleOrder() {
    listOf("a", "b", "c", "d", "e").setSelectionMode(SelectionMode.MULTIPLE);

    click("b");
    clickWith(list.getComponentFor("d"), InputEvent.SHIFT_DOWN_MASK);

    assertThat(list.getSelectionModel().getSelected())
        .as("Shift-click selects the inclusive range from the anchor")
        .containsExactly("b", "c", "d");
  }

  @Test
  void multipleModeShiftClickExtendsBackwardToo() {
    listOf("a", "b", "c", "d").setSelectionMode(SelectionMode.MULTIPLE);

    click("c");
    clickWith(list.getComponentFor("a"), InputEvent.SHIFT_DOWN_MASK);

    assertThat(list.getSelectionModel().getSelected())
        .as("a range anchored above the click still spans both ends")
        .containsExactly("a", "b", "c");
  }

  @Test
  void multipleModeRangeFollowsSortedOrderNotModelOrder() {
    listOf("d", "a", "c", "b")
        .setSelectionMode(SelectionMode.MULTIPLE)
        .setSortOrder(String::compareTo);

    click("a");
    clickWith(list.getComponentFor("c"), InputEvent.SHIFT_DOWN_MASK);

    assertThat(list.getSelectionModel().getSelected())
        .as("the range walks visible order, which the comparator defines")
        .containsExactly("a", "b", "c");
  }

  @Test
  void selectAllBindingCoversTheVisibleItemsOnlyInMultipleMode() {
    listOf("a", "b", "c")
        .setSelectionMode(SelectionMode.MULTIPLE)
        .setFilter(item -> !"b".equals(item));

    Input.pressBoundKey(list, "ctrl pressed A", "elwhaList.selectAll");

    assertThat(list.getSelectionModel().getSelected())
        .as("Ctrl-A selects every visible item and no filtered-out one")
        .containsExactly("a", "c");
  }

  @Test
  void selectAllIsInertOutsideMultipleMode() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE);

    Input.pressBoundKey(list, "ctrl pressed A", "elwhaList.selectAll");

    assertThat(list.getSelectionModel().getSelected())
        .as("Ctrl-A does nothing when only one item may be selected")
        .isEmpty();
  }

  // ---------------------------------------------------- identity durability

  @Test
  void selectionSurvivesAFilterThatHidesTheSelectedItem() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.MULTIPLE);
    clickWith(list.getComponentFor("b"), InputEvent.CTRL_DOWN_MASK);

    list.setFilter(item -> !"b".equals(item));
    assertThat(list.getSelectionModel().getSelected())
        .as("filtering an item out of view does not deselect it")
        .containsExactly("b");

    list.setFilter(null);
    assertThat(list.getSelectionModel().getSelected())
        .as("the item comes back still selected")
        .containsExactly("b");
    assertThat(((ElwhaChip) list.getComponentFor("b")).isSelected())
        .as("the restored view paints as selected")
        .isTrue();
  }

  @Test
  void selectionSurvivesASortReorder() {
    listOf("c", "a", "b").setSelectionMode(SelectionMode.MULTIPLE);
    clickWith(list.getComponentFor("c"), InputEvent.CTRL_DOWN_MASK);

    list.setSortOrder(String::compareTo);

    assertThat(list.getSelectionModel().getSelected())
        .as("sorting is a view concern and leaves selection alone")
        .containsExactly("c");
    assertThat(((ElwhaChip) list.getComponentFor("c")).isSelected())
        .as("the re-rendered view still paints as selected")
        .isTrue();
  }

  @Test
  void switchingToNoneClearsTheSelection() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE);
    click("a");

    list.setSelectionMode(SelectionMode.NONE);

    assertThat(list.getSelectionModel().getSelected())
        .as("NONE means nothing may stay selected")
        .isEmpty();
  }

  // ----------------------------------------------------------- listener wiring

  @Test
  void listLevelListenersFireOnGesturesAndSurviveAModelSwap() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE);
    final List<List<String>> heard = new ArrayList<>();
    list.addSelectionListener(event -> heard.add(event.getSelected()));

    click("a");
    assertThat(heard).as("a gesture reaches list-level listeners").hasSize(1);
    assertThat(heard.get(heard.size() - 1))
        .as("the event carries the new selection")
        .containsExactly("a");

    list.setSelectionModel(new DefaultElwhaSelectionModel<>());
    click("b");
    assertThat(heard.get(heard.size() - 1))
        .as("a listener registered on the list follows it across a selection-model swap")
        .containsExactly("b");
  }

  @Test
  void redundantWritesDoNotFire() {
    listOf("a", "b").setSelectionMode(SelectionMode.MULTIPLE);
    final int[] fired = {0};
    list.addSelectionListener(event -> fired[0]++);

    list.getSelectionModel().setSelected(List.of("a"));
    final int afterFirst = fired[0];
    list.getSelectionModel().setSelected(List.of("a"));
    list.getSelectionModel().add("a");
    list.getSelectionModel().remove("zzz");

    assertThat(fired[0])
        .as("writing the selection it already holds fires nothing")
        .isEqualTo(afterFirst);
  }

  @Test
  void programmaticSelectionPushesChromeOntoRenderedViews() {
    listOf("a", "b").setSelectionMode(SelectionMode.MULTIPLE);

    list.getSelectionModel().setSelected(List.of("a", "b"));

    assertThat(((ElwhaChip) list.getComponentFor("a")).isSelected())
        .as("a programmatic write reaches the chrome, not only the model")
        .isTrue();
    assertThat(((ElwhaChip) list.getComponentFor("b")).isSelected())
        .as("every affected view is synced")
        .isTrue();
  }

  // ------------------------------------------------------------------ a11y

  @Test
  void accessibleSelectionReadsAndDrivesTheSelection() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.MULTIPLE);
    final javax.accessibility.AccessibleSelection selection =
        list.getAccessibleContext().getAccessibleSelection();

    selection.addAccessibleSelection(0);
    selection.addAccessibleSelection(2);

    assertThat(selection.getAccessibleSelectionCount())
        .as("the accessible count matches the selection")
        .isEqualTo(2);
    assertThat(selection.isAccessibleChildSelected(0))
        .as("a selected child reports selected")
        .isTrue();
    assertThat(selection.isAccessibleChildSelected(1))
        .as("an unselected child reports unselected")
        .isFalse();
    assertThat(selection.getAccessibleSelection(1))
        .as("selections are enumerated in visible order")
        .isSameAs(list.getComponentFor("c"));

    selection.removeAccessibleSelection(0);
    assertThat(selection.getAccessibleSelectionCount())
        .as("removing through the accessible API updates the model")
        .isEqualTo(1);

    selection.selectAllAccessibleSelection();
    assertThat(list.getSelectionModel().getSelected())
        .as("select-all covers every visible item")
        .containsExactly("a", "b", "c");

    selection.clearAccessibleSelection();
    assertThat(list.getSelectionModel().getSelected()).as("clear empties the selection").isEmpty();
  }

  @Test
  void accessibleSelectionRespectsMandatoryModesFloor() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE_MANDATORY);
    final javax.accessibility.AccessibleSelection selection =
        list.getAccessibleContext().getAccessibleSelection();

    selection.removeAccessibleSelection(0);
    selection.clearAccessibleSelection();

    assertThat(list.getSelectionModel().getSelected())
        .as("assistive technology cannot empty a mandatory selection either")
        .hasSize(1);
  }

  @Test
  void accessibleChildrenAreExactlyTheVisibleViews() {
    listOf("a", "b", "c").setFilter(item -> !"b".equals(item));

    final javax.accessibility.AccessibleContext context = list.getAccessibleContext();

    assertThat(context.getAccessibleRole())
        .as("the list reports the LIST role")
        .isEqualTo(javax.accessibility.AccessibleRole.LIST);
    assertThat(context.getAccessibleChildrenCount())
        .as("filtered-out items are not accessible children")
        .isEqualTo(2);
    assertThat((JComponent) context.getAccessibleChild(1))
        .as("children follow render order")
        .isSameAs(list.getComponentFor("c"));
    assertThat(context.getAccessibleChild(9))
        .as("an out-of-range index yields nothing rather than throwing")
        .isNull();
  }
}
