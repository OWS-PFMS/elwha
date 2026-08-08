package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of what {@link ElwhaItemList} renders and in what order — the {@link ElwhaList}
 * contract, the pin and anchor partitions, item-to-component lookup, right-to-left mirroring, and
 * the reorder paths that do not need a real pointer.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaItemListRenderTest {

  private static final int WIDTH = 300;
  private static final int HEIGHT = 400;

  private DefaultElwhaListModel<String> model;
  private ElwhaItemList<String> list;

  private ElwhaItemList<String> listOf(final String... items) {
    model = new DefaultElwhaListModel<>(List.of(items));
    list = new ElwhaItemList<>(model, (item, index) -> ElwhaChip.filterChip(item));
    layout();
    return list;
  }

  private void layout() {
    list.setSize(WIDTH, HEIGHT);
    list.doLayout();
    for (final Component child : list.getComponents()) {
      child.setSize(list.getWidth(), list.getHeight());
      ((JComponent) child).doLayout();
    }
  }

  /** The panel the layout managers actually run on, one level below the list's border. */
  private Component contentPanel() {
    for (final Component child : list.getComponents()) {
      if (child.getClass().getSimpleName().equals("ContentPanel")) {
        return child;
      }
    }
    throw new AssertionError("the list is not rendering items");
  }

  // ------------------------------------------------------------- construction

  @Test
  void bothConstructorArgumentsAreRequired() {
    assertThatThrownBy(() -> new ElwhaItemList<String>(null, (item, index) -> new JLabel()))
        .as("a null model is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ElwhaItemList<>(new DefaultElwhaListModel<String>(), null))
        .as("a null adapter is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void freshListDefaults() {
    listOf("a");

    assertThat(list.getOrientation())
        .as("a fresh list stacks vertically")
        .isEqualTo(ElwhaListOrientation.VERTICAL);
    assertThat(list.getSelectionMode())
        .as("selection is off by default")
        .isEqualTo(SelectionMode.NONE);
    assertThat(list.getMovementMode())
        .as("reorder is off by default")
        .isEqualTo(MovementMode.STATIC);
    assertThat(list.isReorderable()).as("STATIC means not reorderable").isFalse();
    assertThat(list.getReorderAffordance())
        .as("the cursor swap is the default affordance")
        .isEqualTo(ReorderAffordance.CURSOR_SWAP);
    assertThat(list.getPinAffordance())
        .as("pin defaults to the indicator glyph")
        .isEqualTo(IconAffordance.INDICATOR);
    assertThat(list.isAnimateChanges()).as("change animation is off by default").isFalse();
  }

  // ------------------------------------------------------- ElwhaList contract

  @Test
  void listImplementsTheCrossCuttingContract() {
    listOf("a");

    assertThat(list).as("the list honors the family contract").isInstanceOf(ElwhaList.class);
  }

  @Test
  void fluentSettersReturnTheConcreteType() {
    listOf("a");

    final ElwhaItemList<String> chained =
        list.setOrientation(ElwhaListOrientation.WRAP)
            .setItemGap(11)
            .setColumns(3)
            .setListPadding(new Insets(1, 2, 3, 4))
            .setLoading(false)
            .setFilter(null)
            .setSortOrder(null)
            .setSelectionMode(SelectionMode.SINGLE)
            .setMovementMode(MovementMode.MOVABLE)
            .setReorderAffordance(ReorderAffordance.BOTH);

    assertThat(chained).as("every mutator returns the list itself, covariantly").isSameAs(list);
    assertThat(list.getItemGap()).as("the gap round-trips").isEqualTo(11);
    assertThat(list.getColumns()).as("the column count round-trips").isEqualTo(3);
    assertThat(list.getListPadding()).as("padding round-trips").isEqualTo(new Insets(1, 2, 3, 4));
  }

  @Test
  void gapAndColumnsClampToTheirFloors() {
    listOf("a").setItemGap(-5).setColumns(0);

    assertThat(list.getItemGap()).as("a negative gap clamps to zero").isZero();
    assertThat(list.getColumns()).as("the column count clamps to one").isEqualTo(1);
  }

  @Test
  void nullPaddingIsTreatedAsZero() {
    listOf("a").setListPadding(null);

    assertThat(list.getListPadding())
        .as("null padding zeroes every edge")
        .isEqualTo(new Insets(0, 0, 0, 0));
  }

  @Test
  void listPaddingIsDefensivelyCopied() {
    final Insets supplied = new Insets(5, 5, 5, 5);
    listOf("a").setListPadding(supplied);

    supplied.top = 99;

    assertThat(list.getListPadding().top)
        .as("mutating the caller's insets does not reach the list")
        .isEqualTo(5);
  }

  @Test
  void emptyAndLoadingStatesFallBackToBuiltInDefaults() {
    listOf();

    assertThat(renderedText())
        .as("an empty list shows the built-in placeholder")
        .contains("No items to show");

    list.setLoading(true);
    assertThat(list.isLoading()).as("the loading flag round-trips").isTrue();
    assertThat(renderedText()).as("loading shows the built-in placeholder").contains("Loading");

    list.setLoadingComponent(new JLabel("Fetching"));
    assertThat(renderedText())
        .as("a supplied loading component replaces the default")
        .contains("Fetching");

    list.setLoadingComponent(null);
    assertThat(renderedText())
        .as("passing null restores the built-in default rather than rendering nothing")
        .contains("Loading");
  }

  @Test
  void loadingWinsOverItems() {
    listOf("a", "b").setLoading(true);

    assertThat(list.getComponentFor("a"))
        .as("no item is rendered while the list is loading")
        .isNull();
  }

  private String renderedText() {
    final StringBuilder text = new StringBuilder();
    collectText(list, text);
    return text.toString();
  }

  private static void collectText(final Component component, final StringBuilder into) {
    if (component instanceof JLabel label) {
      into.append(label.getText()).append(' ');
    }
    if (component instanceof java.awt.Container container) {
      for (final Component child : container.getComponents()) {
        collectText(child, into);
      }
    }
  }

  // ------------------------------------------------------ visible items view

  @Test
  void visibleItemsAppliesFilterThenComparator() {
    listOf("delta", "alpha", "charlie", "bravo")
        .setFilter(item -> !item.startsWith("c"))
        .setSortOrder(Comparator.naturalOrder());

    assertThat(list.getVisibleItems())
        .as("the filter removes and the comparator orders what remains")
        .containsExactly("alpha", "bravo", "delta");
  }

  @Test
  void visibleItemsIsAnUntrackingSnapshot() {
    listOf("a", "b");
    final List<String> snapshot = list.getVisibleItems();

    model.add("c");

    assertThat(snapshot).as("the snapshot does not follow later changes").containsExactly("a", "b");
    assertThatThrownBy(() -> snapshot.add("d"))
        .as("the snapshot is unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void componentLookupTracksWhatIsRendered() {
    listOf("a", "b");

    assertThat(list.getComponentFor("a"))
        .as("a visible item resolves to its component")
        .isNotNull();
    assertThat(list.getComponentFor("nope"))
        .as("an item outside the model resolves to nothing")
        .isNull();

    list.setFilter(item -> !"a".equals(item));
    assertThat(list.getComponentFor("a"))
        .as("a filtered-out item has no rendered component")
        .isNull();
    assertThat(list.getComponentFor("b")).as("the surviving item still resolves").isNotNull();
  }

  @Test
  void swappingTheAdapterRebuildsEveryComponent() {
    listOf("a", "b");
    final JComponent before = list.getComponentFor("a");

    list.setAdapter((item, index) -> new JLabel(item));

    assertThat(list.getComponentFor("a"))
        .as("the swap re-renders rather than reusing")
        .isNotSameAs(before);
    assertThat(list.getComponentFor("a"))
        .as("the new adapter's type is used")
        .isInstanceOf(JLabel.class);
  }

  @Test
  void adapterReceivesTheVisibleIndexNotTheModelIndex() {
    model = new DefaultElwhaListModel<>(List.of("a", "b", "c"));
    final List<Integer> indices = new ArrayList<>();
    list =
        new ElwhaItemList<>(
            model,
            (item, index) -> {
              indices.add(index);
              return ElwhaChip.filterChip(item);
            });
    indices.clear();

    list.setFilter(item -> !"a".equals(item));

    assertThat(indices)
        .as("indices are numbered across the visible items, starting at zero")
        .containsExactly(0, 1);
  }

  @Test
  void anAdapterReturningNullSkipsTheItem() {
    model = new DefaultElwhaListModel<>(List.of("a", "skip", "c"));
    list =
        new ElwhaItemList<>(
            model, (item, index) -> "skip".equals(item) ? null : ElwhaChip.filterChip(item));
    layout();

    assertThat(list.getComponentFor("skip")).as("a null component renders nothing").isNull();
    assertThat(list.getVisibleItems())
        .as("the item stays visible to the model even with no component")
        .containsExactly("a", "skip", "c");
  }

  // --------------------------------------------------------- pin partition

  @Test
  void pinnedItemsRenderBeforeUnpinnedOnes() {
    final Set<String> pinned = new HashSet<>(Set.of("c"));
    listOf("a", "b", "c", "d").setPinPredicate(pinned::contains);

    assertThat(list.getMovementMode())
        .as("installing a pin predicate enters PINNED")
        .isEqualTo(MovementMode.PINNED);
    assertThat(list.getVisibleItems())
        .as("the pinned item leads, the rest hold model order")
        .containsExactly("c", "a", "b", "d");
    assertThat(list.isPinned("c")).as("the predicate drives isPinned").isTrue();
    assertThat(list.isPinned("a")).as("unpinned items report unpinned").isFalse();
  }

  @Test
  void comparatorAppliesWithinEachPinPartition() {
    final Set<String> pinned = new HashSet<>(Set.of("d", "b"));
    listOf("d", "c", "b", "a")
        .setPinPredicate(pinned::contains)
        .setSortOrder(Comparator.naturalOrder());

    assertThat(list.getVisibleItems())
        .as("each partition sorts on its own, so pins keep their group identity")
        .containsExactly("b", "d", "a", "c");
  }

  @Test
  void pinStateChangedRebuildsWithoutAModelEvent() {
    final Set<String> pinned = new HashSet<>();
    listOf("a", "b", "c").setPinPredicate(pinned::contains);

    pinned.add("b");
    list.pinStateChanged();

    assertThat(list.getVisibleItems())
        .as("the list re-reads the predicate on demand rather than polling it")
        .containsExactly("b", "a", "c");
  }

  @Test
  void togglePinDelegatesToTheCallersAction() {
    final Set<String> pinned = new HashSet<>();
    listOf("a", "b")
        .setPinPredicate(pinned::contains)
        .setPinAction(
            (item, shouldPin) -> {
              if (shouldPin) {
                pinned.add(item);
              } else {
                pinned.remove(item);
              }
              list.pinStateChanged();
            });

    list.togglePin("b");
    assertThat(list.getVisibleItems())
        .as("the action pins and the list rebuilds")
        .containsExactly("b", "a");

    list.togglePin("b");
    assertThat(list.getVisibleItems()).as("toggling again unpins").containsExactly("a", "b");
  }

  @Test
  void pinApiIsInertOutsidePinnedMode() {
    final Set<String> pinned = new HashSet<>(Set.of("a"));
    listOf("a", "b").setPinPredicate(pinned::contains).setMovementMode(MovementMode.MOVABLE);

    assertThat(list.isPinned("a"))
        .as("the predicate is inert in a mode with no pin partition")
        .isFalse();
  }

  // ------------------------------------------------------ anchor partition

  @Test
  void anchoredItemLeadsAndIsReported() {
    listOf("a", "b", "c").setAnchorPredicate("b"::equals);

    assertThat(list.getMovementMode())
        .as("installing an anchor predicate enters ANCHORED")
        .isEqualTo(MovementMode.ANCHORED);
    assertThat(list.getVisibleItems())
        .as("the anchor takes the leading slot")
        .containsExactly("b", "a", "c");
    assertThat(list.getAnchoredItem()).as("the anchored item is queryable").isEqualTo("b");
    assertThat(list.isAnchored("b")).as("isAnchored follows the predicate").isTrue();
  }

  @Test
  void onlyTheFirstAnchorCandidateWins() {
    listOf("a", "b", "c").setAnchorPredicate(item -> !"a".equals(item));

    assertThat(list.getVisibleItems())
        .as("with several candidates the first in model order anchors and the rest follow")
        .containsExactly("b", "a", "c");
  }

  @Test
  void pinAndAnchorAreMutuallyExclusive() {
    listOf("a", "b").setPinPredicate("a"::equals);

    list.setAnchorPredicate("b"::equals);

    assertThat(list.getMovementMode())
        .as("the later predicate wins the mode")
        .isEqualTo(MovementMode.ANCHORED);
    assertThat(list.isPinned("a")).as("entering ANCHORED clears the pin predicate").isFalse();
    assertThat(list.getVisibleItems())
        .as("only the anchor partition applies")
        .containsExactly("b", "a");
  }

  @Test
  void setReorderablePreservesAnActivePartition() {
    listOf("a", "b").setPinPredicate("b"::equals);

    list.setReorderable(true);

    assertThat(list.getMovementMode())
        .as("enabling reorder on a partitioned list does not silently drop the partition")
        .isEqualTo(MovementMode.PINNED);

    list.setReorderable(false);
    assertThat(list.getMovementMode())
        .as("disabling reorder returns to STATIC")
        .isEqualTo(MovementMode.STATIC);
  }

  // --------------------------------------------------------------- reorder

  @Test
  void keyboardMoveReordersTheFocusedItemAndFiresTheEvent() {
    listOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    final List<String> heard = new ArrayList<>();
    list.addReorderListener(
        event ->
            heard.add(event.getItem() + ":" + event.getFromIndex() + "->" + event.getToIndex()));
    Input.click(list.getComponentFor("c"), 4, 4);

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(model.getItems())
        .as("Ctrl-Up moves the focused item one slot earlier")
        .containsExactly("a", "c", "b");
    assertThat(heard)
        .as("a keyboard move fires the reorder event with model indices")
        .containsExactly("c:2->1");
  }

  @Test
  void keyboardMoveIsSuppressedWhileSorting() {
    listOf("a", "b", "c")
        .setMovementMode(MovementMode.MOVABLE)
        .setSortOrder(Comparator.naturalOrder());
    Input.click(list.getComponentFor("c"), 4, 4);

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(model.getItems())
        .as("a dropped item would sort straight back, so reorder is suppressed")
        .containsExactly("a", "b", "c");
  }

  @Test
  void keyboardMoveIsSuppressedInStaticMode() {
    listOf("a", "b").setMovementMode(MovementMode.STATIC);
    Input.click(list.getComponentFor("b"), 4, 4);

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(model.getItems())
        .as("STATIC means no reorder from any affordance")
        .containsExactly("a", "b");
  }

  @Test
  void keyboardMoveTranslatesThroughVisibleOrderWhenFiltered() {
    listOf("a", "hidden", "b", "c")
        .setFilter(item -> !"hidden".equals(item))
        .setMovementMode(MovementMode.MOVABLE);
    Input.click(list.getComponentFor("c"), 4, 4);

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(list.getVisibleItems())
        .as("the move lands one visible slot earlier, not one model slot earlier")
        .containsExactly("a", "c", "b");
    assertThat(model.getItems())
        .as("the filtered-out item keeps its place in the model")
        .containsExactly("a", "hidden", "c", "b");
  }

  @Test
  void keyboardDeleteRemovesTheFocusedItemAndItsSelection() {
    listOf("a", "b", "c")
        .setSelectionMode(SelectionMode.MULTIPLE)
        .setMovementMode(MovementMode.MOVABLE);
    Input.click(list.getComponentFor("b"), 4, 4);

    Input.pressBoundKey(list, "pressed DELETE", "elwhaList.delete");

    assertThat(model.getItems()).as("Delete removes the focused item").containsExactly("a", "c");
    assertThat(list.getSelectionModel().getSelected())
        .as("a deleted item does not linger in the selection")
        .isEmpty();
  }

  @Test
  void keyboardDeleteIsSuppressedOnAStaticList() {
    listOf("a", "b", "c").setSelectionMode(SelectionMode.MULTIPLE);
    Input.click(list.getComponentFor("b"), 4, 4);

    Input.pressBoundKey(list, "pressed DELETE", "elwhaList.delete");

    assertThat(model.getItems())
        .as("a display-only list does not destroy its consumer's data on a stray Delete")
        .containsExactly("a", "b", "c");
  }

  @Test
  void keyboardDeleteIsSuppressedWhereTheContextMenuOffersNoDeleteEntry() {
    listOf("a", "b", "c")
        .setMovementMode(MovementMode.MOVABLE)
        .setSortOrder(Comparator.naturalOrder());
    Input.click(list.getComponentFor("b"), 4, 4);

    Input.pressBoundKey(list, "pressed DELETE", "elwhaList.delete");

    assertThat(list.createReorderMenuItems("b"))
        .as("an active sort withdraws the whole reorder section, Delete included")
        .isEmpty();
    assertThat(model.getItems())
        .as("so the keystroke must be withdrawn with it — the two paths agree")
        .containsExactly("a", "b", "c");
  }

  @Test
  void anchoredItemsRefuseToMove() {
    listOf("a", "b", "c").setAnchorPredicate("a"::equals);
    Input.click(list.getComponentFor("a"), 4, 4);

    Input.pressBoundKey(list, "ctrl pressed DOWN", "elwhaList.moveDown");

    assertThat(list.getVisibleItems())
        .as("the anchor is locked to the leading slot")
        .containsExactly("a", "b", "c");
  }

  @Test
  void contextMenuEntriesDisableAtTheEnds() {
    listOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);

    assertThat(list.createReorderMenuItems("a").get(0).isEnabled())
        .as("the first item cannot move up")
        .isFalse();
    assertThat(list.createReorderMenuItems("a").get(1).isEnabled())
        .as("the first item can move down")
        .isTrue();
    assertThat(list.createReorderMenuItems("c").get(1).isEnabled())
        .as("the last item cannot move down")
        .isFalse();
    assertThat(list.createReorderMenuItems("a"))
        .as("the section is Move up, Move down, Delete")
        .hasSize(3);
  }

  @Test
  void contextMenuEntriesRespectTheAnchorPartition() {
    listOf("a", "b", "c").setAnchorPredicate("a"::equals);

    assertThat(list.createReorderMenuItems("a").get(1).isEnabled())
        .as("the anchor is locked to the leading slot, so Move down would do nothing")
        .isFalse();
    assertThat(list.createReorderMenuItems("b").get(0).isEnabled())
        .as("its neighbour cannot move up into the slot the anchor holds either")
        .isFalse();
    assertThat(list.createReorderMenuItems("b").get(1).isEnabled())
        .as("but moving down is a real move")
        .isTrue();
  }

  @Test
  void contextMenuEntriesRespectThePinPartition() {
    listOf("a", "b", "c", "d").setPinPredicate(item -> "a".equals(item) || "b".equals(item));

    assertThat(list.createReorderMenuItems("b").get(1).isEnabled())
        .as("the last pinned item cannot move down out of the pinned partition")
        .isFalse();
    assertThat(list.createReorderMenuItems("c").get(0).isEnabled())
        .as("nor can the first unpinned item move up into it")
        .isFalse();
    assertThat(list.createReorderMenuItems("c").get(1).isEnabled())
        .as("moves inside a partition stay available")
        .isTrue();
  }

  @Test
  void noReorderMenuEntriesWhenReorderIsUnavailable() {
    listOf("a", "b");

    assertThat(list.createReorderMenuItems("a"))
        .as("a STATIC list offers no reorder entries")
        .isEmpty();
  }

  @Test
  void pinAndAnchorMenuItemsLabelForCurrentState() {
    final Set<String> pinned = new HashSet<>(Set.of("a"));
    listOf("a", "b").setPinPredicate(pinned::contains);

    assertThat(list.createPinMenuItem("a").getText())
        .as("a pinned item offers Unpin")
        .isEqualTo("Unpin");
    assertThat(list.createPinMenuItem("b").getText())
        .as("an unpinned item offers Pin")
        .isEqualTo("Pin");

    list.setAnchorPredicate("b"::equals);
    assertThat(list.createAnchorMenuItem("b").getText())
        .as("the anchored item offers to release the anchor")
        .isEqualTo("Remove anchor");
    assertThat(list.createAnchorMenuItem("a").getText())
        .as("another item offers to take the anchor")
        .isEqualTo("Set as anchor");
  }

  @Test
  void aModelRefusingMoveDisablesReorderWithoutFailing() {
    model = new DefaultElwhaListModel<>(List.of("a", "b"));
    list =
        new ElwhaItemList<>(
            new ReadOnlyModel<>(List.of("a", "b")), (item, i) -> ElwhaChip.filterChip(item));
    layout();
    list.setMovementMode(MovementMode.MOVABLE);
    Input.click(list.getComponentFor("b"), 4, 4);

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(list.getVisibleItems())
        .as("a model that refuses to move degrades quietly rather than throwing")
        .containsExactly("a", "b");
  }

  // --------------------------------------------------------------- keyboard

  @Test
  void navigationAndActivationBindingsAreInstalled() {
    listOf("a", "b");

    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed DOWN")))
        .as("Down moves focus forward")
        .isEqualTo("elwhaList.next");
    assertThat(
            list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed DOWN")))
        .as("the same binding applies while an item holds focus")
        .isEqualTo("elwhaList.next");
    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed HOME")))
        .as("Home jumps to the first item")
        .isEqualTo("elwhaList.home");
    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed SPACE")))
        .as("Space activates the focused item")
        .isEqualTo("elwhaList.activate");
  }

  /**
   * Focus position is private, so each case reads it back the way a user would: move focus, then
   * activate, and see which item the activation landed on.
   */
  private void focusThen(final String start, final String keystroke, final String actionKey) {
    Input.click(list.getComponentFor(start), 4, 4);
    Input.pressBoundKey(list, keystroke, actionKey);
    Input.pressBoundKey(list, "pressed SPACE", "elwhaList.activate");
  }

  @Test
  void gridBlockArrowsStepAWholeRow() {
    listOf("a", "b", "c", "d", "e", "f", "g", "h")
        .setOrientation(ElwhaListOrientation.GRID)
        .setColumns(4)
        .setSelectionMode(SelectionMode.SINGLE);
    layout();

    focusThen("a", "pressed DOWN", "elwhaList.next");

    assertThat(list.getSelectionModel().getSelected())
        .as("Down moves one row, not one item — otherwise it does exactly what Right does")
        .containsExactly("e");
  }

  @Test
  void gridBlockArrowsStepAWholeRowBackwards() {
    listOf("a", "b", "c", "d", "e", "f", "g", "h")
        .setOrientation(ElwhaListOrientation.GRID)
        .setColumns(4)
        .setSelectionMode(SelectionMode.SINGLE);
    layout();

    focusThen("f", "pressed UP", "elwhaList.previous");

    assertThat(list.getSelectionModel().getSelected())
        .as("Up lands in the same column of the row above")
        .containsExactly("b");
  }

  @Test
  void gridInlineArrowsStillStepOneItem() {
    listOf("a", "b", "c", "d", "e", "f", "g", "h")
        .setOrientation(ElwhaListOrientation.GRID)
        .setColumns(4)
        .setSelectionMode(SelectionMode.SINGLE);
    layout();

    focusThen("a", "pressed RIGHT", "elwhaList.nextInline");

    assertThat(list.getSelectionModel().getSelected())
        .as("the inline arrows walk within the row, which is what makes the two axes distinct")
        .containsExactly("b");
  }

  @Test
  void blockArrowsStillStepOneItemOutsideAGrid() {
    listOf("a", "b", "c", "d").setSelectionMode(SelectionMode.SINGLE);

    focusThen("a", "pressed DOWN", "elwhaList.next");

    assertThat(list.getSelectionModel().getSelected())
        .as("a flat orientation has one axis, so the row step must not leak into it")
        .containsExactly("b");
  }

  @Test
  void spaceActivatesTheFocusedItemAsAnUnmodifiedPress() {
    listOf("a", "b").setSelectionMode(SelectionMode.SINGLE);
    Input.click(list.getComponentFor("b"), 4, 4);

    Input.pressBoundKey(list, "pressed SPACE", "elwhaList.activate");

    assertThat(list.getSelectionModel().getSelected())
        .as("Space on the focused, already-selected item deselects it exactly as a click would")
        .isEmpty();
  }

  // -------------------------------------------------------------------- RTL

  @ParameterizedTest
  @EnumSource(ElwhaListOrientation.class)
  void everyOrientationMirrorsUnderRightToLeft(final ElwhaListOrientation orientation) {
    listOf("a", "b").setOrientation(orientation);
    layout();
    final int leftToRightX = list.getComponentFor("a").getX();

    list.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    layout();
    final int rightToLeftX = list.getComponentFor("a").getX();

    final Component panel = contentPanel();
    assertThat(rightToLeftX)
        .as("%s places the first item on the right under RTL", orientation)
        .isGreaterThan(leftToRightX);
    assertThat(rightToLeftX + list.getComponentFor("a").getWidth())
        .as("%s keeps the mirrored item inside the content bounds", orientation)
        .isLessThanOrEqualTo(panel.getWidth());
  }

  @Test
  void horizontalOrderReversesOnScreenUnderRightToLeft() {
    listOf("a", "b", "c").setOrientation(ElwhaListOrientation.HORIZONTAL);
    layout();

    list.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    layout();

    assertThat(list.getComponentFor("a").getX())
        .as("the first item sits furthest right when the list reads right to left")
        .isGreaterThan(list.getComponentFor("b").getX());
    assertThat(list.getComponentFor("b").getX())
        .as("the order continues leftward")
        .isGreaterThan(list.getComponentFor("c").getX());
    assertThat(list.getVisibleItems())
        .as("mirroring is a paint concern and leaves render order alone")
        .containsExactly("a", "b", "c");
  }

  /** A model that supports reads but refuses every mutation, the read-only contract in §5. */
  private static final class ReadOnlyModel<T> implements ElwhaListModel<T> {
    private final List<T> items;

    ReadOnlyModel(final List<T> items) {
      this.items = List.copyOf(items);
    }

    @Override
    public java.util.Iterator<T> iterator() {
      return items.iterator();
    }

    @Override
    public int getSize() {
      return items.size();
    }

    @Override
    public T getElementAt(final int index) {
      return items.get(index);
    }

    @Override
    public List<T> getItems() {
      return items;
    }

    @Override
    public boolean contains(final T item) {
      return items.contains(item);
    }

    @Override
    public int indexOf(final T item) {
      return items.indexOf(item);
    }

    @Override
    public void setItems(final List<T> items) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void add(final T item) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void add(final int index, final T item) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void addAll(final Collection<? extends T> items) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public boolean remove(final T item) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public T remove(final int index) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public T set(final int index, final T item) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void move(final int fromIndex, final int toIndex) {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void addListDataListener(final ElwhaListDataListener<T> listener) {
      // a model that never changes has nothing to report
    }

    @Override
    public void removeListDataListener(final ElwhaListDataListener<T> listener) {
      // a model that never changes has nothing to report
    }
  }
}
