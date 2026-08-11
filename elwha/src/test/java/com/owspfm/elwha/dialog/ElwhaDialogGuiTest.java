package com.owspfm.elwha.dialog;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of what makes a dialog <em>modal</em> — the half that is real focus arbitration
 * rather than composition. Initial focus placement, the trap that pulls an escaping focus back into
 * the surface, and the Escape / scrim-press dismiss routes all run through the {@code
 * KeyboardFocusManager} and the real input pipeline; none of them can be represented headless,
 * where nothing owns focus and no press is delivered.
 *
 * <p>This is also where the base host's {@code firstFocusable} fallback becomes reachable at all:
 * it requires displayable components, so a headless tree offers it no candidates. Until #578 that
 * fallback landed on the <em>headline label</em> — {@code isFocusable()} stays true on display-only
 * components, and Swing keeps labels out of traversal by policy rather than by the flag — so the
 * test below now pins the target it was previously only able to bound.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaDialogGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaButton sink;
  private Robot robot;
  private final List<ElwhaDialog> shown = new ArrayList<>();

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
          frame = new JFrame("ElwhaDialogGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout());
          frame.add(sink);
          frame.setSize(900, 700);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    // The native toolkit gives a fresh frame's focus to its first focusable child and Cacio gives
    // it
    // to nothing; parking it makes "focus moved into the dialog" mean the same thing on both.
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the background sink", () -> sink.isFocusOwner());
    robot.mouseMove(0, 0);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    for (final ElwhaDialog dialog : shown) {
      SwingUtilities.invokeAndWait(dialog::dismiss);
    }
    shown.clear();
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private ElwhaDialog show(final ElwhaDialog dialog) throws Exception {
    shown.add(dialog);
    SwingUtilities.invokeAndWait(() -> dialog.show(sink));
    waitFor("the dialog is mounted", () -> mounted().size() == 2);
    return dialog;
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

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  private Component surface() {
    return mounted().get(0);
  }

  /** The current focus owner. Call on the EDT. */
  private Component focusOwner() {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
  }

  private boolean focusIsInsideTheDialog() {
    final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return owner != null
        && !mounted().isEmpty()
        && SwingUtilities.isDescendingFrom(owner, (java.awt.Container) surface());
  }

  private Point scrimPointOffEdt() {
    try {
      // The top-left interior of the frame: covered by the scrim, well clear of the centered body.
      return read(
          () -> {
            final Point origin = frame.getLayeredPane().getLocationOnScreen();
            return new Point(origin.x + 12, origin.y + 12);
          });
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  // -------------------------------------------------------------- focus entry

  @Test
  void initialFocusLandsOnTheConfirmingAction() throws Exception {
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    show(
        ElwhaDialog.builder()
            .headline("Discard draft?")
            .cancelAction(ElwhaButton.textButton("Keep editing"))
            .confirmAction(confirm)
            .build());

    waitFor(
        "the confirming action really owns focus, so Enter and Space act on it immediately",
        () -> confirm.isFocusOwner());
    assertThat(onEdt(() -> sink.isFocusOwner())).as("and the background lost it").isFalse();
  }

  @Test
  void withNoConfirmingActionFocusFallsThroughTheHeadlineToTheFirstControl() throws Exception {
    final ElwhaTextField field = new ElwhaTextField(ElwhaTextField.Variant.FILLED, "Name");
    show(ElwhaDialog.builder().headline("Rename").content(field).build());

    waitFor(
        "the fallback search puts focus somewhere inside the dialog rather than leaving it outside",
        this::focusIsInsideTheDialog);
    assertThat(onEdt(() -> sink.isFocusOwner()))
        .as("never on the inert background behind the scrim")
        .isFalse();
    assertThat(read(this::focusOwner))
        .as(
            "the headline label is skipped and focus lands on the editor that actually takes"
                + " keystrokes — a label reports isFocusable() true but operates no keyboard"
                + " binding")
        .isInstanceOf(JTextComponent.class);
    assertThat(read(() -> SwingUtilities.isDescendingFrom(focusOwner(), field)))
        .as("and that editor is the field's own")
        .isTrue();
  }

  // ------------------------------------------------------------- focus trap

  @Test
  void tabbingNeverEscapesTheDialogIntoTheInertBackground() throws Exception {
    final ElwhaTextField field = new ElwhaTextField(ElwhaTextField.Variant.FILLED, "Name");
    final ElwhaButton confirm = ElwhaButton.textButton("Save");
    show(
        ElwhaDialog.builder()
            .headline("Rename")
            .content(field)
            .cancelAction(ElwhaButton.textButton("Cancel"))
            .confirmAction(confirm)
            .build());
    waitFor("initial focus lands inside the dialog", () -> confirm.isFocusOwner());

    for (int step = 0; step < 5; step++) {
      final int expected = step;
      GuiSteps.keyUntil(
          robot,
          KeyEvent.VK_TAB,
          "Tab step " + expected + " moves focus, and moves it inside the dialog",
          this::focusIsInsideTheDialog);
      assertThat(onEdt(() -> sink.isFocusOwner()))
          .as("the background control behind the scrim is never reachable by Tab")
          .isFalse();
    }
  }

  @Test
  void focusForcedOutOfTheDialogIsPulledBackIn() throws Exception {
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    show(ElwhaDialog.builder().headline("Discard draft?").confirmAction(confirm).build());
    waitFor("initial focus lands inside the dialog", () -> confirm.isFocusOwner());

    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());

    waitFor(
        "an escape to the inert background is trapped back into the surface",
        this::focusIsInsideTheDialog);
  }

  // ---------------------------------------------------------- dismiss routes

  @Test
  void escapeThroughTheRealPipelineCancelsTheDialog() throws Exception {
    final List<DismissCause> causes = new ArrayList<>();
    show(
        ElwhaDialog.builder()
            .headline("Discard draft?")
            .confirmAction(ElwhaButton.textButton("Discard"))
            .onClose(causes::add)
            .build());
    waitFor("the dialog owns focus", this::focusIsInsideTheDialog);

    GuiSteps.keyUntil(
        robot, KeyEvent.VK_ESCAPE, "Escape dismisses the dialog", () -> mounted().isEmpty());

    assertThat(causes)
        .as("with no cancel action, Escape reports itself")
        .containsExactly(DismissCause.ESC);
    waitFor("and focus returns to whatever opened it", () -> sink.isFocusOwner());
  }

  @Test
  void aDialogThatRefusesEscapeStaysUpWhenItIsPressed() throws Exception {
    show(
        ElwhaDialog.builder()
            .headline("Required decision")
            .dismissibleByEsc(false)
            .confirmAction(ElwhaButton.textButton("OK"))
            .build());
    waitFor("the dialog owns focus", this::focusIsInsideTheDialog);

    robot.keyPress(KeyEvent.VK_ESCAPE);
    robot.keyRelease(KeyEvent.VK_ESCAPE);
    robot.waitForIdle();

    assertThat(read(() -> mounted().size()))
        .as("a required-decision dialog has no Escape hatch, even from the real key pipeline")
        .isEqualTo(2);
  }

  @Test
  void aPressOnTheScrimDismissesADialogThatAllowsIt() throws Exception {
    final List<DismissCause> causes = new ArrayList<>();
    show(
        ElwhaDialog.builder()
            .headline("Discard draft?")
            .confirmAction(ElwhaButton.textButton("Discard"))
            .onClose(causes::add)
            .build());
    waitFor("the dialog owns focus", this::focusIsInsideTheDialog);

    GuiSteps.clickUntil(
        robot,
        frame,
        this::scrimPointOffEdt,
        "clicking the scrim dismisses the dialog",
        () -> mounted().isEmpty());

    assertThat(causes).containsExactly(DismissCause.SCRIM);
  }

  @Test
  void aScrimPressIsSwallowedRatherThanReachingTheContentBehindIt() throws Exception {
    final List<String> backgroundClicks = new ArrayList<>();
    SwingUtilities.invokeAndWait(() -> sink.addActionListener(e -> backgroundClicks.add("hit")));
    show(
        ElwhaDialog.builder()
            .headline("Required decision")
            .dismissibleByScrim(false)
            .confirmAction(ElwhaButton.textButton("OK"))
            .build());
    waitFor("the dialog owns focus", this::focusIsInsideTheDialog);
    final Point onSink =
        read(
            () -> {
              final Point origin = sink.getLocationOnScreen();
              return new Point(origin.x + sink.getWidth() / 2, origin.y + sink.getHeight() / 2);
            });

    robot.mouseMove(onSink.x, onSink.y);
    robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    assertThat(backgroundClicks)
        .as("the scrim is the input-blocking half of modality — the control behind it never fires")
        .isEmpty();
    assertThat(read(() -> mounted().size())).as("and the dialog is still up").isEqualTo(2);
  }

  @Test
  void firingTheConfirmingActionByKeyboardClosesWithConfirm() throws Exception {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    show(
        ElwhaDialog.builder()
            .headline("Discard draft?")
            .confirmAction(confirm)
            .onClose(causes::add)
            .build());
    waitFor("the confirming action owns focus", () -> confirm.isFocusOwner());

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_ENTER,
        "Enter fires the focused confirming action",
        () -> mounted().isEmpty());

    assertThat(causes).containsExactly(DismissCause.CONFIRM);
  }
}
