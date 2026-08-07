package com.owspfm.elwha.list;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the two list behaviours the headless suite could only reach halfway — the S7
 * backlog. The injected context menu is a real {@link JPopupMenu}, so <em>showing</em> it needs a
 * display; and a drag-reorder is a threshold crossing followed by a slot recomputation and a
 * commit, which only a real pointer path produces end to end.
 *
 * <p>The drag is asserted against the model order and the reorder event, never against pixels: the
 * gap animation is mid-flight while the pointer moves, so bounds mean nothing until it settles.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaItemListGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  /** A textless row, so a probe can never be satisfied by an item's own label ink. */
  private static final class Slab extends JComponent {
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(200, 48);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      g.setColor(new Color(180, 0, 180));
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  private JFrame frame;
  private DefaultElwhaListModel<String> model;
  private ElwhaItemList<String> list;
  private ElwhaButton sink;
  private Robot robot;

  @BeforeEach
  void showAList() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          model = new DefaultElwhaListModel<>(List.of("Alpha", "Bravo", "Charlie", "Delta"));
          list = new ElwhaItemList<>(model, (item, index) -> new Slab());
          list.setMovementMode(MovementMode.MOVABLE);
          sink = ElwhaButton.textButton("Elsewhere");
          frame = new JFrame("ElwhaItemListGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new BorderLayout());
          frame.add(list, BorderLayout.CENTER);
          frame.add(sink, BorderLayout.SOUTH);
          frame.setSize(420, 560);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the sink", () -> sink.isFocusOwner());
    robot.mouseMove(2, 2);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          MenuSelectionManager.defaultManager().clearSelectedPath();
          frame.dispose();
        });
  }

  // ------------------------------------------------------------------ helpers

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  private List<String> modelOrder() {
    final List<String> out = new ArrayList<>();
    for (int i = 0; i < model.getSize(); i++) {
      out.add(model.getElementAt(i));
    }
    return out;
  }

  private Rectangle rowOnScreen(final String item) throws Exception {
    return read(
        () -> {
          final JComponent view = list.getComponentFor(item);
          return new Rectangle(view.getLocationOnScreen(), view.getSize());
        });
  }

  /** The popup the list injected, or {@code null} while none is showing. Call on the EDT. */
  private JPopupMenu showingPopup() {
    final javax.swing.MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
    for (final javax.swing.MenuElement element : path) {
      if (element.getComponent() instanceof JPopupMenu popup) {
        return popup;
      }
    }
    return null;
  }

  /**
   * Delivers a popup-trigger press to the row through the component's real event pipeline.
   *
   * <p>Not {@code Robot.mousePress(BUTTON3)}: the virtual toolkit this tier runs on cannot dispatch
   * a third-button event at all — it reaches {@code LightweightDispatcher} carrying button 0 and
   * dies with {@code IllegalArgumentException: button doesn't exist 0}, so the press never reaches
   * the list. Only the <em>gesture</em> is substituted here; everything the test actually asserts —
   * the menu being built, shown on the real display, and placed against the row's real screen
   * bounds — is the component doing its own work.
   */
  private void popupTrigger(final String item) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final JComponent view = list.getComponentFor(item);
          final int x = view.getWidth() / 2;
          final int y = view.getHeight() / 2;
          final Point onScreen = view.getLocationOnScreen();
          view.dispatchEvent(
              new java.awt.event.MouseEvent(
                  view,
                  java.awt.event.MouseEvent.MOUSE_PRESSED,
                  System.currentTimeMillis(),
                  InputEvent.BUTTON3_DOWN_MASK,
                  x,
                  y,
                  onScreen.x + x,
                  onScreen.y + y,
                  1,
                  true,
                  java.awt.event.MouseEvent.BUTTON3));
        });
    robot.waitForIdle();
  }

  // ------------------------------------------------------------ context menu

  @Test
  void aRightClickOnAReorderableRowShowsTheInjectedMenu() throws Exception {
    popupTrigger("Bravo");

    waitFor("the list's own context menu is displayed", () -> showingPopup() != null);
    assertThat(read(() -> showingPopup().isShowing())).isTrue();
  }

  @Test
  void anInjectedMenuOpensBelowTheRowItActsOn() throws Exception {
    popupTrigger("Bravo");
    waitFor("the context menu is displayed", () -> showingPopup() != null);

    final Rectangle popup =
        read(() -> new Rectangle(showingPopup().getLocationOnScreen(), showingPopup().getSize()));
    final Rectangle row = rowOnScreen("Bravo");

    assertThat(popup.y)
        .as("the menu opens below the row so the row it acts on stays readable")
        .isGreaterThanOrEqualTo(row.y + row.height);
    assertThat(popup.x).as("aligned to the row's leading edge").isEqualTo(row.x);
  }

  @Test
  void aPinnedListSeparatesTheReorderSectionFromThePinEntry() throws Exception {
    final List<String> pinned = new ArrayList<>();
    SwingUtilities.invokeAndWait(
        () -> {
          list.setMovementMode(MovementMode.PINNED);
          list.setPinPredicate(pinned::contains);
          list.setPinAction((item, pin) -> pinned.add(item));
        });

    popupTrigger("Bravo");
    waitFor("the context menu is displayed", () -> showingPopup() != null);

    final List<String> entries = read(() -> entryNames(showingPopup()));
    assertThat(entries)
        .as("the reorder commands come first, then a separator, then the pin entry")
        .containsExactly("Move up", "Move down", "Delete", "---", "Pin");
  }

  private static List<String> entryNames(final JPopupMenu popup) {
    final List<String> names = new ArrayList<>();
    for (final Component child : popup.getComponents()) {
      if (child instanceof JMenuItem entry) {
        names.add(entry.getText());
      } else if (child instanceof JSeparator) {
        names.add("---");
      }
    }
    return names;
  }

  @Test
  void aPlainMovableListNeedsNoSeparator() throws Exception {
    popupTrigger("Bravo");
    waitFor("the context menu is displayed", () -> showingPopup() != null);

    assertThat(read(() -> entryNames(showingPopup())))
        .as("with nothing to separate the reorder section from, no rule is drawn")
        .containsExactly("Move up", "Move down", "Delete");
  }

  @Test
  void choosingMoveUpFromTheDisplayedMenuMovesTheRow() throws Exception {
    popupTrigger("Charlie");
    waitFor("the context menu is displayed", () -> showingPopup() != null);
    final JMenuItem moveUp = read(() -> (JMenuItem) showingPopup().getComponent(0));

    SwingUtilities.invokeAndWait(moveUp::doClick);

    waitFor(
        "the command the displayed menu carries really reorders the model",
        () -> modelOrder().equals(List.of("Alpha", "Charlie", "Bravo", "Delta")));
  }

  // ------------------------------------------------------------ drag reorder

  @Test
  void aDragPastTheThresholdReordersTheModelAndFiresTheEvent() throws Exception {
    final List<String> events = new ArrayList<>();
    SwingUtilities.invokeAndWait(
        () ->
            list.addReorderListener(
                event ->
                    events.add(
                        event.getItem() + ":" + event.getFromIndex() + "->" + event.getToIndex())));
    final Rectangle from = rowOnScreen("Alpha");
    final Rectangle to = rowOnScreen("Charlie");

    robot.mouseMove(from.x + from.width / 2, from.y + from.height / 2);
    robot.waitForIdle();
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    // Step the pointer down in small increments: the first few cross the drag threshold, the rest
    // walk the drop slot past the intervening rows. A single jump would arrive as one coalesced
    // move and could skip the threshold transition the gesture is built on.
    final int startY = from.y + from.height / 2;
    final int endY = to.y + to.height / 2;
    final int steps = 12;
    for (int step = 1; step <= steps; step++) {
      robot.mouseMove(from.x + from.width / 2, startY + (endY - startY) * step / steps);
      robot.waitForIdle();
    }
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    waitFor(
        "the drop commits the new order to the model",
        () -> modelOrder().equals(List.of("Bravo", "Charlie", "Alpha", "Delta")));
    assertThat(events)
        .as("and the reorder event carries the item with its model indices")
        .containsExactly("Alpha:0->2");
  }

  @Test
  void aPressAndReleaseWithoutCrossingTheThresholdIsAClickNotADrag() throws Exception {
    final List<String> events = new ArrayList<>();
    SwingUtilities.invokeAndWait(() -> list.addReorderListener(event -> events.add("reorder")));
    final Rectangle row = rowOnScreen("Alpha");
    final List<String> before = read(this::modelOrder);

    robot.mouseMove(row.x + row.width / 2, row.y + row.height / 2);
    robot.waitForIdle();
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    // Two pixels — inside the threshold, so this stays a click.
    robot.mouseMove(row.x + row.width / 2 + 2, row.y + row.height / 2);
    robot.waitForIdle();
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    assertThat(read(this::modelOrder)).as("a click leaves the order alone").isEqualTo(before);
    assertThat(events).as("and fires no reorder").isEmpty();
  }

  @Test
  void aStaticListIgnoresTheSameDragEntirely() throws Exception {
    SwingUtilities.invokeAndWait(() -> list.setMovementMode(MovementMode.STATIC));
    final List<String> before = read(this::modelOrder);
    final Rectangle from = rowOnScreen("Alpha");
    final Rectangle to = rowOnScreen("Charlie");

    robot.mouseMove(from.x + from.width / 2, from.y + from.height / 2);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    for (int step = 1; step <= 8; step++) {
      robot.mouseMove(
          from.x + from.width / 2, from.y + from.height / 2 + (to.y - from.y) * step / 8);
      robot.waitForIdle();
    }
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    assertThat(read(this::modelOrder))
        .as("a static list is not draggable, however far the pointer travels")
        .isEqualTo(before);
  }

  @Test
  void aDragBackToWhereItStartedCommitsNothing() throws Exception {
    final List<String> events = new ArrayList<>();
    SwingUtilities.invokeAndWait(() -> list.addReorderListener(event -> events.add("reorder")));
    final Rectangle from = rowOnScreen("Bravo");
    final List<String> before = read(this::modelOrder);
    final Point center = new Point(from.x + from.width / 2, from.y + from.height / 2);

    robot.mouseMove(center.x, center.y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    for (int step = 1; step <= 6; step++) {
      robot.mouseMove(center.x, center.y + 4 * step);
      robot.waitForIdle();
    }
    for (int step = 6; step >= 0; step--) {
      robot.mouseMove(center.x, center.y + 4 * step);
      robot.waitForIdle();
    }
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    assertThat(read(this::modelOrder))
        .as("dropping a row back in its own slot is not a reorder")
        .isEqualTo(before);
    assertThat(events).isEmpty();
    assertThat(onEdt(() -> true)).isTrue();
  }

  // -------------------------------------------------------- cursor refresh #556

  @Test
  void aShownListBindsItsCursorRefreshToTheHostWindow() throws Exception {
    assertThat(read(list::getTrackedWindow))
        .as("#556 — the refresh rides the host window's focus, so it has to find that window")
        .isSameAs(frame);
  }

  @Test
  void aListTakenOutOfItsWindowReleasesTheBinding() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          frame.remove(list);
          frame.validate();
        });

    assertThat(read(list::getTrackedWindow))
        .as("a list that outlives its window must not leave a listener on it")
        .isNull();
  }

  /**
   * Notifies the window's focus listeners the way a Spaces return does.
   *
   * <p>Not {@code frame.dispatchEvent}: the focus manager intercepts {@code WINDOW_GAINED_FOCUS}
   * and drops it as redundant when the window is already focused — which it is here, and which is
   * the only state this fixture can be in. Delivering to the registered listeners keeps what the
   * test is actually about, that the list put a listener on its window and that the listener does
   * the re-install. AWT delivering the event on a real Spaces return is platform contract.
   */
  private void simulateWindowRegain() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final WindowEvent regain = new WindowEvent(frame, WindowEvent.WINDOW_GAINED_FOCUS);
          for (final WindowFocusListener listener : frame.getWindowFocusListeners()) {
            listener.windowGainedFocus(regain);
          }
        });
  }

  @Test
  void aWindowFocusRegainReinstallsTheDragCursor() throws Exception {
    // A sentinel rather than the default pointer: the virtual toolkit this tier can run on hands
    // back a default-typed cursor from createCustomCursor, so "is it the default" proves nothing.
    // "Is it still the thing we stomped it with" holds whatever grab() degrades to.
    SwingUtilities.invokeAndWait(
        () ->
            list.getComponentFor("Bravo")
                .setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)));

    simulateWindowRegain();

    assertThat(read(() -> list.getComponentFor("Bravo").getCursor().getType()))
        .as("#556 — the window coming back is the signal that re-installs the cursor")
        .isNotEqualTo(Cursor.TEXT_CURSOR);
  }

  @Test
  void aRegainLeavesAStaticListsPointerAlone() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          list.setMovementMode(MovementMode.STATIC);
          list.getComponentFor("Bravo").setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        });

    simulateWindowRegain();

    assertThat(read(() -> list.getComponentFor("Bravo").getCursor().getType()))
        .as("a list with no drag to advertise must not acquire a cursor on reactivation")
        .isEqualTo(Cursor.TEXT_CURSOR);
  }

  @Test
  void aRegainRebuildsTheCursorRatherThanReapplyingTheDeadOne() throws Exception {
    final Cursor installed = read(() -> list.getComponentFor("Bravo").getCursor());
    // Toolkits that refuse createCustomCursor hand back a shared predefined instance, where
    // identity says nothing. The bug being guarded is macOS-only, and macOS builds a real one.
    assumeTrue(installed.getType() == Cursor.CUSTOM_CURSOR, "no custom cursor on this toolkit");

    simulateWindowRegain();

    assertThat(read(() -> list.getComponentFor("Bravo").getCursor()))
        .as(
            "macOS keeps the Cursor object and drops only its OS-side association, so re-applying"
                + " the same instance would restore nothing")
        .isNotSameAs(installed);
  }
}
