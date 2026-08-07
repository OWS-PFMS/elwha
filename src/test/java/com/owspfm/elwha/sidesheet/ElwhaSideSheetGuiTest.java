package com.owspfm.elwha.sidesheet;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the side sheet's two presentation paths where they meet a real layout and a
 * real display: the standard sheet's <em>coplanar squash</em> (its sibling content actually
 * reflows), and the modal presentation's docking, focus entry, and Escape route.
 *
 * <p>The squash in particular is only meaningful against a live layout manager — headless the sheet
 * reports a preferred width, but nothing consumes it.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaSideSheetGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();
  private static final int FRAME_W = 900;
  private static final int FRAME_H = 600;

  private JFrame frame;
  private JPanel content;
  private ElwhaButton sink;
  private Robot robot;
  private final List<ElwhaSideSheet> presented = new ArrayList<>();

  @BeforeEach
  void showAFrameWithContent() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          sink = ElwhaButton.textButton("Background");
          content = new JPanel(new BorderLayout());
          content.add(sink, BorderLayout.NORTH);
          frame = new JFrame("ElwhaSideSheetGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new BorderLayout());
          frame.add(content, BorderLayout.CENTER);
          frame.setSize(FRAME_W, FRAME_H);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the background sink", () -> sink.isFocusOwner());
    robot.mouseMove(2, 2);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    for (final ElwhaSideSheet sheet : presented) {
      SwingUtilities.invokeAndWait(sheet::dismiss);
    }
    presented.clear();
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  /** Overlay components on the frame's layered pane, front-most first. Call on the EDT. */
  private List<Component> mounted() {
    final List<Component> out = new ArrayList<>();
    for (final Component child : frame.getLayeredPane().getComponents()) {
      if (child != frame.getContentPane()) {
        out.add(child);
      }
    }
    return out;
  }

  private Component surfaceOf(final ElwhaSideSheet sheet) {
    for (final Component child : mounted()) {
      if (child instanceof java.awt.Container container
          && SwingUtilities.isDescendingFrom(sheet, container)) {
        return child;
      }
    }
    throw new IllegalStateException("no mounted surface for the sheet");
  }

  private ElwhaSideSheet present(final ElwhaSideSheet sheet) throws Exception {
    presented.add(sheet);
    SwingUtilities.invokeAndWait(() -> sheet.showModal(sink));
    waitFor("the sheet is mounted over its scrim", () -> mounted().size() == 2);
    return sheet;
  }

  private void relayoutFrame() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          frame.revalidate();
          frame.getContentPane().doLayout();
          frame.validate();
        });
    robot.waitForIdle();
  }

  // --------------------------------------------------------- coplanar squash

  @Test
  void anEmbeddedSheetTakesItsWidthOutOfTheSiblingContent() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    SwingUtilities.invokeAndWait(
        () -> {
          frame.add(sheet, BorderLayout.LINE_END);
          frame.revalidate();
        });
    relayoutFrame();

    final int contentWidthWithSheet = read(() -> content.getWidth());
    final int sheetWidth = read(() -> sheet.getWidth());

    assertThat(sheetWidth)
        .as("the sheet is laid out at its own width by the host, not merely asked for it")
        .isEqualTo(ElwhaSideSheet.SHEET_WIDTH_PX);
    assertThat(contentWidthWithSheet + sheetWidth)
        .as("and the main content gives up exactly that much room — the coplanar squash")
        .isEqualTo(read(() -> frame.getContentPane().getWidth()));
  }

  @Test
  void closingAnEmbeddedSheetGivesTheRoomBackToTheContent() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    SwingUtilities.invokeAndWait(
        () -> {
          frame.add(sheet, BorderLayout.LINE_END);
          frame.revalidate();
        });
    relayoutFrame();
    final int squashed = read(() -> content.getWidth());

    SwingUtilities.invokeAndWait(sheet::close);
    relayoutFrame();

    assertThat(read(() -> content.getWidth()))
        .as("a closed sheet leaves no sliver, so the content reclaims the whole pane")
        .isEqualTo(read(() -> frame.getContentPane().getWidth()));
    assertThat(read(() -> content.getWidth()))
        .as("which is strictly more than it had while the sheet was open")
        .isGreaterThan(squashed);
  }

  // ------------------------------------------------------------ modal docking

  @Test
  void aModalSheetDocksFlushAgainstTheTrailingEdgeFullHeight() throws Exception {
    final ElwhaSideSheet sheet = present(ElwhaSideSheet.modalSheet("Filters"));

    final Rectangle surface = read(() -> surfaceOf(sheet).getBounds());
    final int paneW = read(() -> frame.getLayeredPane().getWidth());
    final int paneH = read(() -> frame.getLayeredPane().getHeight());

    assertThat(surface.width).isEqualTo(read(sheet::modalFootprintWidth));
    assertThat(surface.x + surface.width).as("flush with the trailing edge").isEqualTo(paneW);
    assertThat(surface.height).as("and full height").isEqualTo(paneH);
  }

  @Test
  void aDetachedSheetFloatsItsBodyOffEveryWindowEdge() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    SwingUtilities.invokeAndWait(() -> sheet.setSheetPosture(SheetPosture.DETACHED));
    present(sheet);

    final Rectangle body = read(() -> boundsOnScreen(sheet.bodyComponent()));
    final Rectangle pane = read(() -> boundsOnScreen(frame.getLayeredPane()));
    final int margin = ElwhaSideSheet.DETACHED_MARGIN_PX;

    assertThat(pane.x + pane.width - (body.x + body.width))
        .as("the floating card is inset from the trailing window edge by its margin")
        .isEqualTo(margin);
    assertThat(body.y - pane.y).as("and from the top").isEqualTo(margin);
    assertThat(pane.y + pane.height - (body.y + body.height))
        .as("and from the bottom")
        .isEqualTo(margin);
  }

  private static Rectangle boundsOnScreen(final Component component) {
    return new Rectangle(component.getLocationOnScreen(), component.getSize());
  }

  // ------------------------------------------------------------- modal focus

  @Test
  void presentingModallyMovesFocusIntoTheSheet() throws Exception {
    final ElwhaSideSheet sheet = present(ElwhaSideSheet.modalSheet("Filters"));

    waitFor(
        "focus really moves into the sheet, so its Escape route and controls are live",
        () -> {
          final Component owner =
              java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          return owner != null && SwingUtilities.isDescendingFrom(owner, sheet);
        });
    assertThat(onEdt(() -> sink.isFocusOwner()))
        .as("and off the content behind the scrim")
        .isFalse();
  }

  @Test
  void escapeThroughTheRealPipelineDismissesTheSheet() throws Exception {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    SwingUtilities.invokeAndWait(() -> sheet.setOnClose(causes::add));
    present(sheet);
    waitFor("the sheet has focus", () -> !mounted().isEmpty());

    GuiSteps.keyUntil(
        robot, KeyEvent.VK_ESCAPE, "Escape dismisses the sheet", () -> mounted().isEmpty());

    assertThat(causes).containsExactly(SheetDismissCause.ESC);
    waitFor("and focus returns to the content that opened it", () -> sink.isFocusOwner());
  }

  @Test
  void aSheetThatRefusesEscapeStaysUpWhenItIsPressed() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    SwingUtilities.invokeAndWait(() -> sheet.setDismissibleByEsc(false));
    present(sheet);

    robot.keyPress(KeyEvent.VK_ESCAPE);
    robot.keyRelease(KeyEvent.VK_ESCAPE);
    robot.waitForIdle();

    assertThat(read(() -> mounted().size()))
        .as("the toggle is honored live, straight from the real key pipeline")
        .isEqualTo(2);
  }

  @Test
  void aPressOnTheScrimDismissesTheSheet() throws Exception {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    SwingUtilities.invokeAndWait(() -> sheet.setOnClose(causes::add));
    present(sheet);

    GuiSteps.clickUntil(
        robot,
        frame,
        this::scrimPointOffEdt,
        "a press on the scrim dismisses the sheet",
        () -> mounted().isEmpty());

    assertThat(causes).containsExactly(SheetDismissCause.SCRIM);
  }

  private Point scrimPointOffEdt() {
    try {
      // The leading edge of the pane: covered by the scrim, well clear of the trailing-docked
      // sheet.
      return read(
          () -> {
            final Point origin = frame.getLayeredPane().getLocationOnScreen();
            return new Point(origin.x + 20, origin.y + frame.getLayeredPane().getHeight() / 2);
          });
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void aCloseAffordanceDismissesThroughARealClick() throws Exception {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    SwingUtilities.invokeAndWait(() -> sheet.setOnClose(causes::add));
    present(sheet);

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOfOffEdt(sheetCloseButton(sheet)),
        "clicking the header's close affordance dismisses the sheet",
        () -> mounted().isEmpty());

    assertThat(causes).containsExactly(SheetDismissCause.CLOSE_AFFORDANCE);
  }

  private Component sheetCloseButton(final ElwhaSideSheet sheet) {
    return sheet.closeAffordanceButton();
  }

  private Point centerOfOffEdt(final Component component) {
    try {
      return read(
          () -> {
            final Point origin = component.getLocationOnScreen();
            return new Point(
                origin.x + component.getWidth() / 2, origin.y + component.getHeight() / 2);
          });
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void aModalSheetSitsBelowTheDialogBandSoADialogCanOpenOverIt() throws Exception {
    final ElwhaSideSheet sheet = present(ElwhaSideSheet.modalSheet("Filters"));

    final int sheetLayer = read(() -> frame.getLayeredPane().getLayer(surfaceOf(sheet)));

    assertThat(sheetLayer)
        .as("the sheet band is below the dialog band")
        .isLessThan(javax.swing.JLayeredPane.MODAL_LAYER);
    assertThat(sheetLayer)
        .as("and below the popup band a menu opens on")
        .isLessThan(javax.swing.JLayeredPane.POPUP_LAYER);
  }
}
