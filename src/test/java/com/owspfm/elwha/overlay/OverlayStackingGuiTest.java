package com.owspfm.elwha.overlay;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.sidesheet.ElwhaSideSheet;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of which stacked overlay owns a window-wide keystroke. Two overlays on different
 * z-bands must both be mounted, both showing, and both registered with the real {@code
 * KeyboardManager} for the assertion to mean anything — none of which a headless host can offer,
 * and the resolution being tested happens inside Swing's own key-binding search rather than in
 * Elwha code.
 *
 * <p>The arrangement that matters is the one where show order and z-order <em>disagree</em>: a
 * modal side sheet ({@code OVERLAY_LAYER}) opened while a dialog ({@code MODAL_LAYER}) is already
 * up registers its binding last but paints underneath. Swing resolves window-wide bindings
 * newest-registered-first, so before #599 that sheet took Escape out from under the dialog covering
 * it.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class OverlayStackingGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaButton sink;
  private Robot robot;
  private final List<ElwhaSideSheet> sheets = new ArrayList<>();
  private final List<ElwhaDialog> dialogs = new ArrayList<>();

  @BeforeEach
  void showAFrameWithASink() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          sink = ElwhaButton.textButton("Background");
          frame = new JFrame("OverlayStackingGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout());
          frame.add(sink);
          frame.setSize(900, 700);
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
    for (final ElwhaDialog dialog : dialogs) {
      SwingUtilities.invokeAndWait(dialog::dismiss);
    }
    for (final ElwhaSideSheet sheet : sheets) {
      SwingUtilities.invokeAndWait(sheet::dismiss);
    }
    dialogs.clear();
    sheets.clear();
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private ElwhaSideSheet showSheet() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    sheets.add(sheet);
    SwingUtilities.invokeAndWait(() -> sheet.showModal(sink));
    waitFor("the sheet is presented", sheet::isModalShowing);
    return sheet;
  }

  private ElwhaDialog showDialog(final List<DialogClose> closes) throws Exception {
    final ElwhaDialog dialog =
        ElwhaDialog.builder()
            .headline("Discard draft?")
            .confirmAction(ElwhaButton.textButton("Discard"))
            .onClose(cause -> closes.add(new DialogClose(cause.name())))
            .build();
    dialogs.add(dialog);
    SwingUtilities.invokeAndWait(() -> dialog.show(sink));
    waitFor("the dialog is mounted", () -> !mounted().isEmpty());
    return dialog;
  }

  /** Overlay components on the frame's layered pane. Call on the EDT. */
  private List<Component> mounted() {
    final List<Component> out = new ArrayList<>();
    for (final Component child : frame.getLayeredPane().getComponents()) {
      if (child != frame.getContentPane()) {
        out.add(child);
      }
    }
    return out;
  }

  /**
   * A recorded dialog close, named rather than typed to keep the cause enum out of the signature.
   */
  private record DialogClose(String cause) {}

  // ------------------------------------------------------------------- tests

  @Test
  void escapeDismissesTheDialogStackedOverAModalSheet() throws Exception {
    final ElwhaSideSheet sheet = showSheet();
    final List<DialogClose> closes = new ArrayList<>();
    showDialog(closes);
    waitFor("both overlays are mounted", () -> mounted().size() == 4);

    GuiSteps.keyUntil(
        robot, KeyEvent.VK_ESCAPE, "Escape dismisses the dialog", () -> closes.size() == 1);

    assertThat(onEdt(sheet::isModalShowing))
        .as("the sheet underneath keeps its presentation")
        .isTrue();
    assertThat(closes).extracting(DialogClose::cause).containsExactly("ESC");
  }

  @Test
  void escapeStillReachesTheDialogWhenTheSheetIsOpenedOnTopOfItInShowOrder() throws Exception {
    final List<DialogClose> closes = new ArrayList<>();
    showDialog(closes);
    final ElwhaSideSheet sheet = showSheet();
    waitFor("both overlays are mounted", () -> mounted().size() == 4);

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_ESCAPE,
        "Escape reaches the dialog on the higher z-band, not the more recently shown sheet",
        () -> closes.size() == 1);

    assertThat(onEdt(sheet::isModalShowing))
        .as("the sheet is not the one dismissed — it is painted beneath the dialog")
        .isTrue();
    assertThat(closes).extracting(DialogClose::cause).containsExactly("ESC");
  }
}
