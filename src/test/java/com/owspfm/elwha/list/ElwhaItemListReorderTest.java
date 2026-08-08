package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of {@link ElwhaItemList}'s reorder surface beyond what the #69 spike classes
 * pinned — the four {@link ReorderAffordance} treatments (spec §7), the partition clamps a drag or
 * a keyboard move has to respect (§8), the full keyboard binding set including the Meta twins, and
 * the reorder event contract.
 *
 * <p>Real drags belong to the gui tier. What is reachable headless is the state each affordance
 * installs, the clamp arithmetic, and the InputMap-driven commands — all of which this covers.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaItemListReorderTest {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 400;

  /** A textless item view, so a handle probe cannot be satisfied by an item's own label ink. */
  private static final class Slab extends JComponent {
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(180, 40);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      g.setColor(new Color(180, 0, 180));
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  private DefaultElwhaListModel<String> model;
  private ElwhaItemList<String> list;

  private ElwhaItemList<String> chipListOf(final String... items) {
    model = new DefaultElwhaListModel<>(List.of(items));
    list = new ElwhaItemList<>(model, (item, index) -> ElwhaChip.filterChip(item));
    return layout();
  }

  private ElwhaItemList<String> slabListOf(final String... items) {
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

  private void focusOn(final String item) {
    Input.click(list.getComponentFor(item), 4, 4);
  }

  /** Dispatches a real crossing event, which the testkit's mouse helpers deliberately omit. */
  private static void crossing(final Component target, final int id) {
    target.dispatchEvent(
        new MouseEvent(
            target, id, System.nanoTime(), 0, 4, 4, 4, 4, 0, false, MouseEvent.NOBUTTON));
  }

  /** What {@code ReorderCursors.grab()} degrades to with no display to load a custom cursor on. */
  private static final int GRAB = Cursor.MOVE_CURSOR;

  private static int cursorOf(final JComponent view) {
    return view.getCursor().getType();
  }

  // -------------------------------------------------------- cursor swap

  @Test
  void cursorSwapMarksDraggableItemsAndNothingElse() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("§7 — CURSOR_SWAP advertises the drag by swapping the pointer over each item")
        .isEqualTo(GRAB);
  }

  @Test
  void aStaticListInstallsNoDragCursor() {
    slabListOf("a", "b");

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("STATIC means nothing drags, so nothing advertises a drag")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @ParameterizedTest
  @EnumSource(
      value = ReorderAffordance.class,
      names = {"HOVER_ICON", "NONE"})
  void affordancesWithoutTheCursorLeaveThePointerAlone(final ReorderAffordance affordance) {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE).setReorderAffordance(affordance);
    layout();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("%s does not touch the pointer", affordance)
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @Test
  void bothInstallsTheCursorAsWellAsTheHandle() {
    slabListOf("a", "b")
        .setMovementMode(MovementMode.MOVABLE)
        .setReorderAffordance(ReorderAffordance.BOTH);
    layout();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("BOTH is a superset, so the cursor half still applies")
        .isEqualTo(GRAB);
  }

  @Test
  void anAnchoredItemGetsNoDragCursorWhileItsNeighboursDo() {
    slabListOf("a", "b", "c").setAnchorPredicate("a"::equals);
    layout();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("the anchor is locked in place, so it must not advertise a drag")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
    assertThat(cursorOf(list.getComponentFor("b")))
        .as("while the items that can move still do")
        .isEqualTo(GRAB);
  }

  @Test
  void anActiveSortSuppressesTheDragCursorToo() {
    slabListOf("a", "b")
        .setMovementMode(MovementMode.MOVABLE)
        .setSortOrder(Comparator.naturalOrder());
    layout();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("a dropped item would sort straight back, so the list stops advertising the drag")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @Test
  void leavingReorderClearsTheCursorAgain() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();

    list.setMovementMode(MovementMode.STATIC);
    layout();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("the swap is installed per rebuild, so turning reorder off restores the pointer")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  // ----------------------------------------------------- cursor refresh #556

  /**
   * Stands in for what macOS does to a custom cursor across a Spaces transition — it drops the
   * OS-side association, and the pointer falls back to the default.
   */
  private void loseCursor(final String item) {
    list.getComponentFor(item).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  @Test
  void aRefreshReinstallsTheGrabCursorTheSystemDropped() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();
    loseCursor("a");

    list.reapplyReorderCursors();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("#556 — nothing else re-installs the cursor after the OS drops it")
        .isEqualTo(GRAB);
  }

  @Test
  void aRefreshCoversEveryDraggableItemNotJustTheOneUnderThePointer() {
    slabListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    layout();
    loseCursor("a");
    loseCursor("b");
    loseCursor("c");

    list.reapplyReorderCursors();

    assertThat(cursorOf(list.getComponentFor("b"))).isEqualTo(GRAB);
    assertThat(cursorOf(list.getComponentFor("c")))
        .as("the whole window comes back at once, so every item has to")
        .isEqualTo(GRAB);
  }

  @Test
  void aRefreshLeavesAStaticListAlone() {
    slabListOf("a", "b");
    layout();

    list.reapplyReorderCursors();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("a list that never advertised a drag must not acquire the cursor on reactivation")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @ParameterizedTest
  @EnumSource(
      value = ReorderAffordance.class,
      names = {"HOVER_ICON", "NONE"})
  void aRefreshLeavesTheCursorlessAffordancesAlone(final ReorderAffordance affordance) {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE).setReorderAffordance(affordance);
    layout();

    list.reapplyReorderCursors();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("%s never installs a cursor, so the refresh has nothing to restore", affordance)
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @Test
  void aRefreshSkipsAnchoredItemsTheSameWayTheInstallDid() {
    slabListOf("a", "b", "c").setAnchorPredicate("a"::equals);
    layout();
    loseCursor("b");

    list.reapplyReorderCursors();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("the anchor still cannot move, so it still must not advertise a drag")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
    assertThat(cursorOf(list.getComponentFor("b")))
        .as("while its movable neighbours come back")
        .isEqualTo(GRAB);
  }

  @Test
  void aRefreshStopsAtAnActiveSortJustLikeTheInstall() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();
    list.setSortOrder(Comparator.naturalOrder());
    layout();

    list.reapplyReorderCursors();

    assertThat(cursorOf(list.getComponentFor("a")))
        .as("a sort suppresses reorder, so reactivation must not resurrect the affordance")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  // --------------------------------------------- window binding (headless half)

  @Test
  void aListWithNoWindowBindsNothing() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);

    list.trackAncestorWindow();

    assertThat(list.getTrackedWindow())
        .as("no window ancestor is what makes the whole mechanism inert off-display")
        .isNull();
  }

  @Test
  void bindingIsIdempotentSoRepeatedAttachesCannotStack() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);

    list.trackAncestorWindow();
    list.trackAncestorWindow();

    assertThat(list.getTrackedWindow()).isNull();
  }

  @Test
  void unbindingIsSafeWhenNothingIsBound() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);

    list.untrackAncestorWindow();

    assertThat(list.getTrackedWindow())
        .as("removeNotify runs on lists that never reached a window, so it cannot throw")
        .isNull();
  }

  // -------------------------------------------------------- hover handle

  @Test
  void hoverHandleAppearsOverTheItemUnderThePointer() {
    slabListOf("a", "b")
        .setMovementMode(MovementMode.MOVABLE)
        .setReorderAffordance(ReorderAffordance.HOVER_ICON);
    layout();
    final JComponent view = list.getComponentFor("a");

    crossing(view, MouseEvent.MOUSE_ENTERED);

    assertThat(handleInkNear(view))
        .as("§7 — HOVER_ICON fades a drag handle in over the item's leading edge")
        .isTrue();
  }

  @Test
  void handleGoesAwayWhenThePointerLeaves() {
    slabListOf("a", "b")
        .setMovementMode(MovementMode.MOVABLE)
        .setReorderAffordance(ReorderAffordance.HOVER_ICON);
    layout();
    final JComponent view = list.getComponentFor("a");
    crossing(view, MouseEvent.MOUSE_ENTERED);

    crossing(view, MouseEvent.MOUSE_EXITED);

    assertThat(handleInkNear(view))
        .as("the handle is a hover affordance, not a permanent one")
        .isFalse();
  }

  @Test
  void noHandleAppearsUnderTheCursorSwapAffordance() {
    slabListOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    layout();
    final JComponent view = list.getComponentFor("a");

    crossing(view, MouseEvent.MOUSE_ENTERED);

    assertThat(handleInkNear(view)).as("CURSOR_SWAP is the cursor and only the cursor").isFalse();
  }

  @Test
  void noHandleAppearsOnAListThatCannotReorder() {
    slabListOf("a", "b").setReorderAffordance(ReorderAffordance.HOVER_ICON);
    layout();
    final JComponent view = list.getComponentFor("a");

    crossing(view, MouseEvent.MOUSE_ENTERED);

    assertThat(handleInkNear(view))
        .as("a STATIC list must not offer a handle for a drag that will never start")
        .isFalse();
  }

  @Test
  void switchingAffordanceMidFlightDropsAShowingHandle() {
    slabListOf("a", "b")
        .setMovementMode(MovementMode.MOVABLE)
        .setReorderAffordance(ReorderAffordance.HOVER_ICON);
    layout();
    crossing(list.getComponentFor("a"), MouseEvent.MOUSE_ENTERED);

    list.setReorderAffordance(ReorderAffordance.CURSOR_SWAP);
    layout();

    assertThat(handleInkNear(list.getComponentFor("a")))
        .as("the handle state is reset with the affordance rather than stranded on screen")
        .isFalse();
  }

  @Test
  void aNullAffordanceIsIgnoredRatherThanClearingTheCurrentOne() {
    chipListOf("a").setReorderAffordance(ReorderAffordance.NONE);

    list.setReorderAffordance(null);

    assertThat(list.getReorderAffordance())
        .as("null is a no-op, so a stray call cannot silently re-enable an affordance")
        .isEqualTo(ReorderAffordance.NONE);
  }

  /** True when the drag-handle glyph's ink shows over the view's leading edge. */
  private boolean handleInkNear(final JComponent view) {
    final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final java.awt.Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);
      list.paint(g);
    } finally {
      g.dispose();
    }
    final Color ink = UIManager.getColor("Label.foreground");
    final java.awt.Rectangle bounds =
        javax.swing.SwingUtilities.convertRectangle(view.getParent(), view.getBounds(), list);
    for (int y = bounds.y; y < bounds.y + bounds.height && y < HEIGHT; y++) {
      for (int x = bounds.x; x < bounds.x + 24 && x < WIDTH; x++) {
        final Color pixel = new Color(image.getRGB(x, y), true);
        if (pixel.getAlpha() == 255
            && Math.abs(pixel.getRed() - ink.getRed()) <= 10
            && Math.abs(pixel.getGreen() - ink.getGreen()) <= 10
            && Math.abs(pixel.getBlue() - ink.getBlue()) <= 10) {
          return true;
        }
      }
    }
    return false;
  }

  // ----------------------------------------------------------- keyboard

  @Test
  void bothPlatformModifiersDriveTheSameMoveCommands() {
    chipListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    focusOn("c");

    Input.pressBoundKey(list, "meta pressed UP", "elwhaList.moveUp");

    assertThat(model.getItems())
        .as("Cmd-Up must work on macOS exactly as Ctrl-Up does elsewhere")
        .containsExactly("a", "c", "b");
  }

  @Test
  void moveDownWalksTheItemTowardTheEnd() {
    chipListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    focusOn("a");

    Input.pressBoundKey(list, "ctrl pressed DOWN", "elwhaList.moveDown");

    assertThat(model.getItems())
        .as("Ctrl-Down moves the focused item one slot later")
        .containsExactly("b", "a", "c");
  }

  @Test
  void aMoveOffEitherEndIsANoOp() {
    chipListOf("a", "b").setMovementMode(MovementMode.MOVABLE);
    focusOn("a");
    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    focusOn("b");
    Input.pressBoundKey(list, "ctrl pressed DOWN", "elwhaList.moveDown");

    assertThat(model.getItems())
        .as("moving past either end changes nothing rather than wrapping")
        .containsExactly("a", "b");
  }

  @Test
  void aMoveWithNothingFocusedIsANoOp() {
    chipListOf("a", "b").setMovementMode(MovementMode.MOVABLE);

    Input.pressBoundKey(list, "ctrl pressed DOWN", "elwhaList.moveDown");

    assertThat(model.getItems())
        .as("the command needs a focused item to act on and degrades quietly without one")
        .containsExactly("a", "b");
  }

  @Test
  void bothDeleteBindingsAreInstalled() {
    chipListOf("a", "b", "c");
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed BACK_SPACE", "elwhaList.delete");
    focusOn("c");
    Input.pressBoundKey(list, "meta pressed BACK_SPACE", "elwhaList.delete");

    assertThat(model.getItems())
        .as("the platform Backspace twins delete alongside the Delete key")
        .containsExactly("a");
  }

  @Test
  void deleteWithNothingFocusedIsANoOp() {
    chipListOf("a", "b");

    Input.pressBoundKey(list, "pressed DELETE", "elwhaList.delete");

    assertThat(model.getItems())
        .as("delete needs a focused item and degrades quietly without one")
        .containsExactly("a", "b");
  }

  @Test
  void aModelRefusingRemovalSwallowsTheDelete() {
    model = new DefaultElwhaListModel<>(List.of("a", "b"));
    list = new ElwhaItemList<>(new UnremovableModel<>(List.of("a", "b")), (i, n) -> new Slab());
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "pressed DELETE", "elwhaList.delete");

    assertThat(list.getVisibleItems())
        .as("a model that refuses removal degrades quietly rather than throwing at the user")
        .containsExactly("a", "b");
  }

  @Test
  void navigationBindingsCoverBothInlineDirections() {
    chipListOf("a", "b");

    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed LEFT")))
        .as("Left steps back along the inline axis")
        .isEqualTo("elwhaList.previousInline");
    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed RIGHT")))
        .as("and Right steps forward")
        .isEqualTo("elwhaList.nextInline");
    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed END")))
        .as("End jumps to the last item")
        .isEqualTo("elwhaList.end");
    assertThat(
            list.getInputMap(JComponent.WHEN_FOCUSED)
                .get(javax.swing.KeyStroke.getKeyStroke("pressed ENTER")))
        .as("Enter activates the focused item alongside Space")
        .isEqualTo("elwhaList.activate");
  }

  // ---------------------------------------------------- partition clamps

  @Test
  void aPinnedItemCannotBeMovedOutOfThePinnedRun() {
    final Set<String> pinned = new HashSet<>(Set.of("a", "b"));
    chipListOf("a", "b", "c", "d").setPinPredicate(pinned::contains);
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed DOWN", "elwhaList.moveDown");

    assertThat(list.getVisibleItems())
        .as("§8 — a pinned item clamps to the end of its own partition")
        .containsExactly("a", "b", "c", "d");
  }

  @Test
  void anUnpinnedItemCannotBeMovedIntoThePinnedRun() {
    final Set<String> pinned = new HashSet<>(Set.of("a"));
    chipListOf("a", "b", "c").setPinPredicate(pinned::contains);
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(list.getVisibleItems())
        .as("and an unpinned one clamps to the slot right after the pins")
        .containsExactly("a", "b", "c");
  }

  @Test
  void anItemStillMovesFreelyInsideItsOwnPartition() {
    final Set<String> pinned = new HashSet<>(Set.of("a", "b"));
    chipListOf("a", "b", "c", "d").setPinPredicate(pinned::contains);
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(list.getVisibleItems())
        .as("the clamp bounds the partition, it does not freeze the items inside it")
        .containsExactly("b", "a", "c", "d");
  }

  @Test
  void nothingCanTakeTheAnchorsLeadingSlot() {
    chipListOf("a", "b", "c").setAnchorPredicate("a"::equals);
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(list.getVisibleItems())
        .as("§8 — with an anchor present slot zero is spoken for")
        .containsExactly("a", "b", "c");
  }

  @Test
  void anchoredModeStillReordersEverythingBelowTheAnchor() {
    chipListOf("a", "b", "c").setAnchorPredicate("a"::equals);
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed DOWN", "elwhaList.moveDown");

    assertThat(list.getVisibleItems())
        .as("only the leading slot is reserved; the rest of the list moves normally")
        .containsExactly("a", "c", "b");
  }

  @Test
  void clearingTheAnchorReleasesTheLeadingSlot() {
    final List<String> anchored = new ArrayList<>(List.of("a"));
    chipListOf("a", "b", "c").setAnchorPredicate(anchored::contains);
    list.setAnchorAction(
        item -> {
          anchored.clear();
          if (item != null) {
            anchored.add(item);
          }
          list.anchorStateChanged();
        });
    layout();
    list.clearAnchor();
    layout();
    focusOn("b");

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(list.getVisibleItems())
        .as("with no anchored item the clamp stops applying")
        .containsExactly("b", "a", "c");
  }

  // ---------------------------------------------------- reorder events

  @Test
  void aProgrammaticModelMoveFiresNoReorderEvent() {
    chipListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    final List<String> heard = new ArrayList<>();
    list.addReorderListener(event -> heard.add(event.getItem()));

    model.move(0, 2);

    assertThat(heard)
        .as("the event reports user-driven reorder, so consumers can tell the two apart")
        .isEmpty();
  }

  @Test
  void reorderListenersAreDeduplicatedAndRemovable() {
    chipListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    final List<String> heard = new ArrayList<>();
    final ElwhaReorderListener<String> listener = event -> heard.add(event.getItem());
    list.addReorderListener(listener);
    list.addReorderListener(listener);
    list.addReorderListener(null);
    focusOn("c");

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");
    final int afterFirst = heard.size();
    list.removeReorderListener(listener);
    focusOn("c");
    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(afterFirst).as("a listener registered twice is still notified once").isEqualTo(1);
    assertThat(heard).as("and removal actually detaches it").hasSize(1);
  }

  @Test
  void reorderEventCarriesModelIndicesNotVisibleOnes() {
    chipListOf("a", "hidden", "b", "c")
        .setFilter(item -> !"hidden".equals(item))
        .setMovementMode(MovementMode.MOVABLE);
    layout();
    final List<ElwhaReorderEvent<String>> heard = new ArrayList<>();
    list.addReorderListener(heard::add);
    focusOn("c");

    Input.pressBoundKey(list, "ctrl pressed UP", "elwhaList.moveUp");

    assertThat(heard).as("the move fires exactly once").hasSize(1);
    assertThat(heard.get(0).getSource())
        .as("the event names the list that produced it")
        .isSameAs(list);
    assertThat(heard.get(0).getFromIndex())
        .as("indices address the model, so a filtered view does not skew them")
        .isEqualTo(3);
    assertThat(heard.get(0).getToIndex())
        .as("and the destination is the model slot the item landed in")
        .isEqualTo(2);
  }

  // --------------------------------------------------- change animation

  @Test
  void changeAnimationIsOffByDefaultAndRoundTrips() {
    chipListOf("a");

    assertThat(list.isAnimateChanges()).as("a fresh list does not fade its changes").isFalse();
    assertThat(list.setAnimateChanges(true).isAnimateChanges())
        .as("the fade can be asked for")
        .isTrue();
    assertThat(list.setAnimateChanges(false).isAnimateChanges())
        .as("and turned back off")
        .isFalse();
  }

  @Test
  void animationDurationClampsToAFloor() {
    chipListOf("a");

    assertThat(list.setAnimationDuration(400).getAnimationDuration())
        .as("a sensible duration round-trips")
        .isEqualTo(400);
    assertThat(list.setAnimationDuration(1).getAnimationDuration())
        .as("an unusably short duration clamps rather than producing a one-frame flash")
        .isEqualTo(50);
  }

  @Test
  void reducedMotionSnapsChangesWithNoFadeAtAll() {
    chipListOf("a", "b").setAnimateChanges(true);

    model.add("c");

    assertThat(list.isPaintingOrigin())
        .as("with reduced motion pinned the rebuild is already settled — no fade is in flight")
        .isFalse();
    assertThat(list.getComponentFor("c"))
        .as("and the new item is rendered immediately rather than waiting on a tween")
        .isNotNull();
  }

  // ------------------------------------------------------------- teardown

  /** Presses on an item and drags past the threshold, leaving an active drag in flight. */
  private JComponent beginDragOn(final String item) {
    final JComponent view = list.getComponentFor(item);
    Input.press(view, 4, 4);
    Input.drag(view, 4, 4 + 3 * ElwhaItemList.DRAG_THRESHOLD);
    return view;
  }

  @Test
  void leavingTheHierarchyMidDragAbortsTheDrag() {
    slabListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    layout();
    beginDragOn("a");
    assertThat(list.isDragInFlight()).as("the drag is what the teardown has to clean up").isTrue();

    list.removeNotify();

    assertThat(list.isDragInFlight())
        .as("#588 — a removed list never sees the release, so removeNotify has to end the drag")
        .isFalse();
    assertThat(model.getItems())
        .as("a teardown is not a drop, so the interrupted move is not committed")
        .containsExactly("a", "b", "c");
  }

  @Test
  void aReAddedListCanStartAFreshDrag() {
    slabListOf("a", "b", "c").setMovementMode(MovementMode.MOVABLE);
    layout();
    beginDragOn("a");

    list.removeNotify();
    list.addNotify();
    beginDragOn("b");

    assertThat(list.isDragInFlight())
        .as("the teardown must leave the list armed, not dead — Showcase swaps re-add constantly")
        .isTrue();
  }

  @Test
  void leavingTheHierarchyStopsEveryAnimationTimer() {
    // The three timers this asserts on all no-op under the pinned reduced motion, so this is the
    // one case that has to run with motion live. Nothing here samples a tween — the test body and
    // every timer share the EDT, so no tick can interleave with the assertions.
    MorphAnimator.setReducedMotion(false);
    try {
      slabListOf("a", "b", "c")
          .setMovementMode(MovementMode.MOVABLE)
          .setReorderAffordance(ReorderAffordance.BOTH)
          .setAnimateChanges(true);
      layout();
      model.add("d");
      layout();
      beginDragOn("a");
      assertThat(list.hasRunningAnimationTimers())
          .as("the drag reflow and the content fade are both ticking")
          .isTrue();

      list.removeNotify();

      assertThat(list.hasRunningAnimationTimers())
          .as("#588 — a 16ms timer that outlives its list pins the whole model graph")
          .isFalse();
    } finally {
      MorphAnimator.setReducedMotion(true);
    }
  }

  /** A model that supports reads and moves but refuses removal. */
  private static final class UnremovableModel<T> extends DefaultElwhaListModel<T> {
    UnremovableModel(final List<T> items) {
      super(items);
    }

    @Override
    public boolean remove(final T item) {
      throw new UnsupportedOperationException("read-only");
    }
  }
}
