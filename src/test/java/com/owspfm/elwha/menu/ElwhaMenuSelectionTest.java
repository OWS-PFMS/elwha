package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link SelectionMode} semantics — what an activation does in each mode, what
 * the menu reports back, and the fact that selection <em>lives on the items</em>, so it survives a
 * close and the surface rebuild that the next open performs. That persistence is the contract
 * behind the Showcase's "the menu remembers what you picked" behavior; the {@code gui} tier proves
 * the same thing across a real reopen.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaMenuSelectionTest {

  private HeadlessHost host;
  private final List<ElwhaMenu> opened = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(700, 500);
  }

  @AfterEach
  void closeEveryMenu() {
    for (final ElwhaMenu menu : opened) {
      menu.close();
    }
    opened.clear();
  }

  private void open(final ElwhaMenu menu) {
    opened.add(menu);
    menu.open(host.anchor());
  }

  private static ElwhaMenu menuIn(final SelectionMode mode) {
    return ElwhaMenu.builder()
        .selectionMode(mode)
        .addItem(ElwhaMenuItem.of("Grid"))
        .addItem(ElwhaMenuItem.of("List"))
        .addItem(ElwhaMenuItem.of("Gallery"))
        .build();
  }

  // ------------------------------------------------------------------- NONE

  @Test
  void anActionMenuHoldsNoSelectionAndClosesOnActivation() {
    final ElwhaMenu menu = menuIn(SelectionMode.NONE);
    open(menu);

    menu.getItems().get(1).activate(0);

    assertThat(menu.getSelectedItems())
        .as("an action menu keeps no persistent selection")
        .isEmpty();
    assertThat(host.mounted()).as("and a pick dismisses it").isEmpty();
  }

  @Test
  void anActionMenuNeverFiresTheSelectionCallback() {
    final List<ElwhaMenuItem> changes = new ArrayList<>();
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .onSelectionChange(changes::add)
            .addItem(ElwhaMenuItem.of("Grid"))
            .addItem(ElwhaMenuItem.of("List"))
            .build();
    open(menu);

    menu.getItems().get(0).activate(0);

    assertThat(changes).as("there is nothing to report when nothing can be selected").isEmpty();
  }

  @Test
  void defaultModeIsTheActionMenu() {
    assertThat(ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Grid")).build().getSelectionMode())
        .as("a menu is an action menu unless a selection mode is asked for")
        .isEqualTo(SelectionMode.NONE);
  }

  // ----------------------------------------------------------------- SINGLE

  @Test
  void singleSelectionMarksOneRowAndClosesTheMenu() {
    final ElwhaMenu menu = menuIn(SelectionMode.SINGLE);
    open(menu);

    menu.getItems().get(2).activate(0);

    assertThat(menu.getSelectedItems())
        .extracting(ElwhaMenuItem::getLabel)
        .as("exactly one row carries the mark")
        .containsExactly("Gallery");
    assertThat(host.mounted()).as("and choosing closes the menu").isEmpty();
  }

  @Test
  void singleSelectionReportsTheRowThatChanged() {
    final List<ElwhaMenuItem> changes = new ArrayList<>();
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.SINGLE)
            .onSelectionChange(changes::add)
            .addItem(ElwhaMenuItem.of("Grid"))
            .addItem(ElwhaMenuItem.of("List"))
            .build();
    open(menu);

    menu.getItems().get(0).activate(0);

    assertThat(changes)
        .extracting(ElwhaMenuItem::getLabel)
        .as("the selection callback names the row that just changed")
        .containsExactly("Grid");
  }

  @Test
  void aSecondSingleSelectionDeselectsTheFirst() {
    final ElwhaMenu menu = menuIn(SelectionMode.SINGLE);
    open(menu);
    menu.getItems().get(0).activate(0);

    open(menu);
    menu.getItems().get(1).activate(0);

    assertThat(menu.getSelectedItems())
        .extracting(ElwhaMenuItem::getLabel)
        .as("single selection is exclusive — the prior mark is cleared")
        .containsExactly("List");
  }

  @Test
  void singleSelectionSurvivesTheCloseAndTheNextOpensSurfaceRebuild() {
    final ElwhaMenu menu = menuIn(SelectionMode.SINGLE);
    open(menu);
    menu.getItems().get(1).activate(0);

    open(menu);

    assertThat(menu.getSelectedItems())
        .extracting(ElwhaMenuItem::getLabel)
        .as("selection lives on the items, so a reopened menu still shows the pick")
        .containsExactly("List");
  }

  // ------------------------------------------------------------------ MULTI

  @Test
  void multiSelectionTogglesAndLeavesTheMenuOpen() {
    final ElwhaMenu menu = menuIn(SelectionMode.MULTI);
    open(menu);

    menu.getItems().get(0).activate(0);
    menu.getItems().get(2).activate(0);

    assertThat(menu.getSelectedItems())
        .extracting(ElwhaMenuItem::getLabel)
        .as("multi-select accumulates, in display order")
        .containsExactly("Grid", "Gallery");
    assertThat(host.mounted()).as("and the menu stays up so more can be picked").hasSize(1);
  }

  @Test
  void reActivatingAMultiSelectRowClearsIt() {
    final ElwhaMenu menu = menuIn(SelectionMode.MULTI);
    open(menu);
    menu.getItems().get(0).activate(0);

    menu.getItems().get(0).activate(0);

    assertThat(menu.getSelectedItems())
        .as("a second activation toggles the row back off")
        .isEmpty();
  }

  @Test
  void everyMultiSelectToggleIsReported() {
    final List<String> changes = new ArrayList<>();
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.MULTI)
            .onSelectionChange(item -> changes.add(item.getLabel() + "=" + item.isSelected()))
            .addItem(ElwhaMenuItem.of("Grid"))
            .addItem(ElwhaMenuItem.of("List"))
            .build();
    open(menu);

    menu.getItems().get(0).activate(0);
    menu.getItems().get(1).activate(0);
    menu.getItems().get(0).activate(0);

    assertThat(changes)
        .as("the callback fires per toggle, carrying the row's new state")
        .containsExactly("Grid=true", "List=true", "Grid=false");
  }

  // ------------------------------------------------------- pre-set selection

  @Test
  void anInitialSelectionSetBeforeBuildIsHonored() {
    final ElwhaMenuItem list = ElwhaMenuItem.of("List");
    list.setSelected(true);
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.SINGLE)
            .addItem(ElwhaMenuItem.of("Grid"))
            .addItem(list)
            .build();

    assertThat(menu.getSelectedItems())
        .as("the documented way to seed a selection is to mark items before build")
        .containsExactly(list);
  }

  @Test
  void selectionSnapshotIsUnmodifiable() {
    final ElwhaMenu menu = menuIn(SelectionMode.MULTI);
    menu.getItems().get(0).setSelected(true);

    assertThat(menu.getSelectedItems())
        .as("callers read the selection, they do not edit it through the accessor")
        .isUnmodifiable();
  }

  // --------------------------------------------------------- a11y of the mark

  @Test
  void aMultiSelectRowIsCheckboxLikeToAssistiveTech() {
    final ElwhaMenu menu = menuIn(SelectionMode.MULTI);
    final ElwhaMenuItem row = menu.getItems().get(0);

    row.setSelected(true);

    assertThat(row.getAccessibleContext().getAccessibleStateSet().toArray())
        .as("a toggle row reports both selected and checked, so the tick is never the only cue")
        .contains(AccessibleState.SELECTED, AccessibleState.CHECKED);
  }

  @Test
  void aSingleSelectRowIsRadioLikeToAssistiveTech() {
    final ElwhaMenu menu = menuIn(SelectionMode.SINGLE);
    final ElwhaMenuItem row = menu.getItems().get(0);

    row.setSelected(true);

    assertThat(row.getAccessibleContext().getAccessibleStateSet().toArray())
        .as("an exclusive row reports selection only")
        .contains(AccessibleState.SELECTED)
        .doesNotContain(AccessibleState.CHECKED);
  }

  @Test
  void anActionRowInAnActionMenuReportsNeither() {
    final ElwhaMenu menu = menuIn(SelectionMode.NONE);
    final ElwhaMenuItem row = menu.getItems().get(0);

    assertThat(row.getAccessibleContext().getAccessibleStateSet().toArray())
        .as("nothing is selectable in an action menu, so nothing is announced as selected")
        .doesNotContain(AccessibleState.SELECTED, AccessibleState.CHECKED);
  }
}
