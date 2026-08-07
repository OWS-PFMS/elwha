package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the list's context menu (spec §6, §8) — the injection rule (the list offers
 * its own menu only for an item whose component carries none) and the behavior of the three public
 * builders whose output that injected menu is assembled from.
 *
 * <p>Showing a popup needs a component that is on screen, so the headless tier observes the
 * injection <em>decision</em> rather than the rendered menu: a list that declines returns quietly,
 * while a list that injects gets as far as needing a screen. What the menu contains is asserted
 * directly against the builders, which is the same code path the injection composes. Placement and
 * appearance are gui-tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaItemListContextMenuTest {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 400;

  /**
   * A menu that records being opened instead of opening — showing a real popup needs a component on
   * screen, which the headless tier does not have.
   */
  private static final class RecordingMenu extends JPopupMenu {
    private boolean shown;

    @Override
    public void show(final Component invoker, final int x, final int y) {
      shown = true;
    }
  }

  /** A plain item view that carries no menu of its own until a test gives it one. */
  private static final class Slab extends JComponent {
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(180, 40);
    }
  }

  private DefaultElwhaListModel<String> model;
  private ElwhaItemList<String> list;

  private ElwhaItemList<String> listOf(final String... items) {
    model = new DefaultElwhaListModel<>(List.of(items));
    list = new ElwhaItemList<>(model, (item, index) -> new Slab());
    return layout();
  }

  private ElwhaItemList<String> layout() {
    list.setSize(WIDTH, HEIGHT);
    list.doLayout();
    for (final Component child : list.getComponents()) {
      child.setSize(list.getWidth(), list.getHeight());
      ((JComponent) child).doLayout();
    }
    return list;
  }

  /** Dispatches the platform's popup gesture onto an item's component. */
  private void popupTrigger(final String item) {
    final Component view = list.getComponentFor(item);
    view.dispatchEvent(
        new MouseEvent(
            view,
            MouseEvent.MOUSE_PRESSED,
            System.nanoTime(),
            0,
            4,
            4,
            4,
            4,
            1,
            true,
            MouseEvent.BUTTON3));
  }

  // ------------------------------------------------------- injection rule

  @Test
  void aReorderableListOffersItsOwnMenu() {
    listOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();

    assertThatThrownBy(() -> popupTrigger("a"))
        .as("the list builds a menu for a bare item and gets as far as needing a screen for it")
        .isInstanceOf(IllegalComponentStateException.class);
  }

  @Test
  void aCallerInstalledMenuSuppressesTheInjectedOne() {
    listOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();
    final RecordingMenu callerMenu = new RecordingMenu();
    list.getComponentFor("a").setComponentPopupMenu(callerMenu);

    popupTrigger("a");

    assertThat(callerMenu.shown)
        .as("an item that already has a menu keeps it — the list does not compete for the gesture")
        .isTrue();
  }

  @Test
  void aListWithNothingToOfferDeclinesToo() {
    listOf("a", "b");

    assertThatCode(() -> popupTrigger("a"))
        .as("a STATIC list with no pin or anchor has no entries, so it shows no empty menu")
        .doesNotThrowAnyException();
  }

  @Test
  void aPinnedListOffersAMenuEvenWithReorderOtherwiseUnavailable() {
    final Set<String> pinned = new HashSet<>(Set.of("a"));
    listOf("a", "b").setPinPredicate(pinned::contains).setPinAction((item, shouldPin) -> {});
    list.setSortOrder(String::compareTo);
    layout();

    assertThatThrownBy(() -> popupTrigger("b"))
        .as("a sort blocks reorder but not pinning, so the Pin entry alone is worth a menu")
        .isInstanceOf(IllegalComponentStateException.class);
  }

  @Test
  void aPinPredicateWithNoActionOffersNoPinEntry() {
    final Set<String> pinned = new HashSet<>(Set.of("a"));
    listOf("a", "b").setPinPredicate(pinned::contains);
    list.setSortOrder(String::compareTo);
    layout();

    assertThatCode(() -> popupTrigger("b"))
        .as("an entry the list cannot carry out is not offered — the action is what applies it")
        .doesNotThrowAnyException();
  }

  // ------------------------------------------------------ reorder entries

  @Test
  void reorderSectionIsMoveUpMoveDownDelete() {
    listOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);

    final List<JMenuItem> entries = list.createReorderMenuItems("b");

    assertThat(entries).as("§6 — the section carries three entries").hasSize(3);
    assertThat(entries.get(0).getText()).as("in display order, move up first").isEqualTo("Move up");
    assertThat(entries.get(1).getText()).as("then move down").isEqualTo("Move down");
    assertThat(entries.get(2).getText()).as("then delete").isEqualTo("Delete");
  }

  @Test
  void moveEntriesActuallyMoveTheItem() {
    listOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);

    list.createReorderMenuItems("c").get(0).doClick();

    assertThat(model.getItems())
        .as("choosing Move up walks the item one slot earlier")
        .containsExactly("a", "c", "b");
  }

  @Test
  void deleteEntryActuallyRemovesTheItem() {
    listOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);

    list.createReorderMenuItems("b").get(2).doClick();

    assertThat(model.getItems()).as("choosing Delete removes that item").containsExactly("a", "c");
  }

  @Test
  void deleteEntryDropsTheItemsSelectionWithIt() {
    listOf("a", "b").setSelectionMode(SelectionMode.MULTIPLE).setMovementMode(MovementMode.MOVABLE);
    list.getSelectionModel().setSelected(List.of("a", "b"));

    list.createReorderMenuItems("b").get(2).doClick();

    assertThat(list.getSelectionModel().getSelected())
        .as("a removed item must not linger in the selection")
        .containsExactly("a");
  }

  @Test
  void everyBuilderCallHandsBackFreshEntries() {
    listOf("a", "b").setMovementMode(MovementMode.MOVABLE);

    assertThat(list.createReorderMenuItems("a").get(0))
        .as("entries are per-open, so a stale reference cannot end up in two menus")
        .isNotSameAs(list.createReorderMenuItems("a").get(0));
  }

  @Test
  void anAnchoredItemsMoveEntriesDoNothingWhenChosen() {
    listOf("a", "b", "c").setAnchorPredicate("a"::equals);
    layout();

    list.createReorderMenuItems("a").get(1).doClick();

    assertThat(list.getVisibleItems())
        .as("the anchor is locked to the leading slot, so the move is refused")
        .containsExactly("a", "b", "c");
  }

  @Test
  void anAnchoredItemCanStillBeDeletedFromTheMenu() {
    listOf("a", "b", "c").setAnchorPredicate("a"::equals);
    layout();

    final List<JMenuItem> entries = list.createReorderMenuItems("a");

    assertThat(entries.get(2).isEnabled())
        .as("locking an item in place does not make removing it illegitimate")
        .isTrue();
  }

  @Test
  void aPinnedItemsMoveOutOfItsRunDoesNothingWhenChosen() {
    final Set<String> pinned = new HashSet<>(Set.of("a", "b"));
    listOf("a", "b", "c").setPinPredicate(pinned::contains);
    layout();

    list.createReorderMenuItems("b").get(1).doClick();

    assertThat(list.getVisibleItems())
        .as("§8 — the partition clamp holds whichever affordance asked for the move")
        .containsExactly("a", "b", "c");
  }

  @Test
  void aMoveInsideThePartitionStillGoesThroughFromTheMenu() {
    final Set<String> pinned = new HashSet<>(Set.of("a", "b"));
    listOf("a", "b", "c").setPinPredicate(pinned::contains);
    layout();

    list.createReorderMenuItems("b").get(0).doClick();

    assertThat(list.getVisibleItems())
        .as("the clamp bounds the partition, it does not freeze the items inside it")
        .containsExactly("b", "a", "c");
  }

  // --------------------------------------------------- pin and anchor entries

  @Test
  void pinEntryTogglesThroughTheCallersAction() {
    final Set<String> pinned = new HashSet<>();
    final List<String> calls = new ArrayList<>();
    listOf("a", "b")
        .setPinPredicate(pinned::contains)
        .setPinAction(
            (item, shouldPin) -> {
              calls.add(item + ":" + shouldPin);
              if (shouldPin) {
                pinned.add(item);
              } else {
                pinned.remove(item);
              }
              list.pinStateChanged();
            });
    layout();

    list.createPinMenuItem("b").doClick();

    assertThat(calls)
        .as("the list asks the caller to apply the pin rather than owning the state")
        .containsExactly("b:true");
    assertThat(list.getVisibleItems())
        .as("and rebuilds against the caller's new answer")
        .containsExactly("b", "a");
  }

  @Test
  void anchorEntryTogglesThroughTheCallersAction() {
    final List<String> anchored = new ArrayList<>();
    listOf("a", "b").setAnchorPredicate(anchored::contains);
    list.setAnchorAction(
        item -> {
          anchored.clear();
          if (item != null) {
            anchored.add(item);
          }
          list.anchorStateChanged();
        });
    layout();

    list.createAnchorMenuItem("b").doClick();
    assertThat(list.getVisibleItems())
        .as("Set as anchor moves the item to the leading slot")
        .containsExactly("b", "a");

    list.createAnchorMenuItem("b").doClick();
    assertThat(list.getVisibleItems())
        .as("and the entry on an already-anchored item releases it")
        .containsExactly("a", "b");
  }

  @Test
  void aPinEntryWithNoActionInstalledIsInert() {
    final Set<String> pinned = new HashSet<>();
    listOf("a", "b").setPinPredicate(pinned::contains);
    layout();

    list.createPinMenuItem("b").doClick();

    assertThat(list.getVisibleItems())
        .as("with nothing to apply the pin, choosing the entry changes nothing rather than failing")
        .containsExactly("a", "b");
  }
}
