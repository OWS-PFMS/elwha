package com.owspfm.elwha.textfield;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.SupportingTextVisibility;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the fields family's focus-driven behavior — the half of the text field's state
 * table headless cannot represent. The field's {@code focused} flag is set by a real {@code
 * FocusListener} on the embedded editor, and dispatching synthetic {@code FocusEvent}s is banned as
 * a testing idiom, so the focused label float, the Expressive focus-stroke bump, the {@code
 * ON_FOCUS} supporting-text reveal, and the decorator's click&#8594;editor focus handoff can only
 * be told the truth by a real {@code KeyboardFocusManager}.
 *
 * <p>Chrome is read from an offscreen repaint of the live component rather than a screen capture,
 * so nothing depends on window decorations or display scaling.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaTextFieldGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaTextField field;
  private ElwhaTextField other;
  private javax.swing.JButton focusSink;
  private Robot robot;

  @BeforeEach
  void showTwoFields() throws Exception {
    robot = new Robot();
    final int slot = FRAME_SLOT.getAndIncrement();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          field = new ElwhaTextField(Variant.FILLED, "Email");
          other = new ElwhaTextField(Variant.FILLED, "Name");
          frame = new JFrame("ElwhaTextFieldGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout());
          // First in traversal order: on the native toolkit a freshly-shown frame gives initial
          // focus to its first focusable child — without a sink that child is the field's editor,
          // and every resting-state precondition below would assert against a focused field.
          // (Cacio hands initial focus to nothing, which is how this passed under it locally.)
          focusSink = new javax.swing.JButton("sink");
          frame.add(focusSink);
          frame.add(field);
          frame.add(other);
          frame.pack();
          frame.setLocation(100 + slot * 560, 100);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    SwingUtilities.invokeAndWait(() -> focusSink.requestFocusInWindow());
    waitFor("initial focus parks on the sink", () -> focusSink.isFocusOwner());
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  @Test
  void realFocusFloatsTheLabelOfAnEmptyField() throws Exception {
    assertThat(labelSize())
        .as("an empty blurred field rests its label")
        .isEqualTo((float) TypeRole.BODY_LARGE.pt());

    focusTheEditor();

    assertThat(labelSize())
        .as("focus alone floats the label — the other half of 'focused or populated'")
        .isEqualTo((float) TypeRole.BODY_SMALL.pt());

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus on to the next field",
        () -> other.getEditor().isFocusOwner());

    assertThat(labelSize())
        .as("and blurring an empty field returns the label to rest")
        .isEqualTo((float) TypeRole.BODY_LARGE.pt());
  }

  @Test
  void realFocusBumpsTheActiveIndicatorToPrimaryAtThreeDeviceIndependentPixels() throws Exception {
    final PaintLog.Painted resting = indicator();
    assertThat(resting.color())
        .as("a blurred filled field rests its indicator at onSurfaceVariant")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
    assertThat(resting.bounds().getHeight())
        .as("1dp at rest")
        .isEqualTo(ElwhaTextField.RESTING_STROKE);

    focusTheEditor();

    final PaintLog.Painted focused = indicator();
    assertThat(focused.color())
        .as("focus turns the active indicator primary")
        .isEqualTo(ColorRole.PRIMARY.resolve());
    assertThat(focused.bounds().getHeight())
        .as("and bumps it to the Expressive 3dp")
        .isEqualTo(ElwhaTextField.FOCUS_STROKE);
  }

  @Test
  void onFocusRevealsTheSupportingTextOnlyWhileFocused() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          field.setSupportingText("We never share it");
          field.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
        });
    assertThat(paintedSupportingText())
        .as("ON_FOCUS leaves the reserved row blank while blurred")
        .isFalse();

    focusTheEditor();

    assertThat(paintedSupportingText()).as("focus reveals the advisory row").isTrue();

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus on to the next field",
        () -> other.getEditor().isFocusOwner());

    assertThat(paintedSupportingText()).as("and blur hides it again").isFalse();
  }

  @Test
  void pressingTheChromeHandsFocusToTheEmbeddedEditor() throws Exception {
    SwingUtilities.invokeAndWait(() -> other.getEditor().requestFocusInWindow());
    waitFor("the other field starts with focus", () -> other.getEditor().isFocusOwner());

    GuiSteps.clickUntil(
        robot,
        frame,
        this::chromeMargin,
        "clicking the field's own chrome moves focus into its editor",
        () -> field.getEditor().isFocusOwner());

    assertThat(onEdt(() -> other.getEditor().isFocusOwner()))
        .as("the previous editor released focus")
        .isFalse();
  }

  /** Moves real focus into the field's editor and waits for the KFM to agree. */
  /**
   * The chassis declines the tab stop (conventions §12), so {@code field.requestFocusInWindow()}
   * would land nowhere unless it forwards. Only a real {@code KeyboardFocusManager} can tell the
   * difference between "forwarded" and "returned false and did nothing" (#688).
   */
  @Test
  void theChassisForwardsAFocusRequestToItsEditor() throws Exception {
    assertThat(field.isFocusable()).as("the decorator is not itself a tab stop").isFalse();

    final AtomicReference<Boolean> accepted = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> accepted.set(field.requestFocusInWindow()));

    assertThat(accepted.get()).as("the forwarded request is accepted, not refused").isTrue();
    waitFor("focus lands on the embedded editor", () -> field.getEditor().isFocusOwner());
    assertThat(field.isFocusOwner()).as("and never on the chassis itself").isFalse();
  }

  private void focusTheEditor() throws Exception {
    SwingUtilities.invokeAndWait(() -> field.getEditor().requestFocusInWindow());
    waitFor("the field's editor owns focus", () -> field.getEditor().isFocusOwner());
  }

  /** A point on the field's left padding — chrome the decorator owns, not the editor. */
  private Point chromeMargin() {
    try {
      final AtomicReference<Point> point = new AtomicReference<>();
      SwingUtilities.invokeAndWait(
          () -> {
            final Point origin = field.getLocationOnScreen();
            point.set(new Point(origin.x + 4, origin.y + ElwhaTextField.CONTAINER_HEIGHT / 2));
          });
      return point.get();
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private float labelSize() throws Exception {
    final AtomicReference<Float> size = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () ->
            size.set(
                PaintLog.capture(field, field.getWidth(), field.getHeight())
                    .text("Email")
                    .orElseThrow()
                    .font()
                    .getSize2D()));
    return size.get();
  }

  private boolean paintedSupportingText() throws Exception {
    return onEdt(
        () ->
            PaintLog.capture(field, field.getWidth(), field.getHeight())
                .painted("We never share it"));
  }

  /** The filled variant's active indicator — the last wide, thin filled bar the field paints. */
  private PaintLog.Painted indicator() throws Exception {
    final AtomicReference<PaintLog.Painted> bar = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () ->
            bar.set(
                PaintLog.capture(field, field.getWidth(), field.getHeight()).shapes().stream()
                    .filter(s -> !s.stroked())
                    .filter(s -> s.bounds().getHeight() <= ElwhaTextField.FOCUS_STROKE)
                    .filter(s -> s.bounds().getWidth() > field.getWidth() / 2.0)
                    .reduce((first, second) -> second)
                    .orElseThrow()));
    return bar.get();
  }
}
