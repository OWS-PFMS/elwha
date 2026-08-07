package com.owspfm.elwha.selectfield;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the editable combo's <b>focus-loss commit guard</b> — which focus departures
 * are commit points and which are not. The Tier A suite drives the commit seams directly and so
 * proves what a commit <em>does</em>; only real focus arbitration can prove <em>when</em> one
 * happens, because the guard reads a real {@code FocusEvent}'s opposite component and temporary
 * flag, and synthesizing those would test the test's own event construction.
 *
 * <p><b>Not covered in either tier:</b> the {@code isTemporary} branch — a focus loss caused by the
 * whole window deactivating, where typing is expected to resume on return. Reproducing it needs a
 * second top-level window to steal activation, and under a window-manager-less display the
 * resulting activation sequence is not dependable enough for a merge gate. Recorded as backlog for
 * the gui wave rather than faked here.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaSelectFieldGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaSelectField<String> combo;
  private ElwhaTextField neighbor;
  private Robot robot;

  @BeforeEach
  void showAComboAndANeighbor() throws Exception {
    robot = new Robot();
    final int slot = FRAME_SLOT.getAndIncrement();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          combo = ElwhaSelectField.filled("Planet");
          combo.setOptions(List.of("Mercury", "Venus", "Earth", "Mars"));
          combo.setEditable(true);
          combo.setSelectedValue("Venus");
          neighbor = new ElwhaTextField(ElwhaTextField.Variant.FILLED, "Notes");
          frame = new JFrame("ElwhaSelectFieldGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout());
          frame.add(combo);
          frame.add(neighbor);
          frame.pack();
          frame.setLocation(100 + slot * 560, 100);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  @Test
  void focusLeavingTheControlCommitsTheTypedText() throws Exception {
    focusEditor();
    SwingUtilities.invokeAndWait(() -> editor().setText("Mars"));

    SwingUtilities.invokeAndWait(() -> neighbor.getEditor().requestFocusInWindow());
    waitFor("focus lands outside the combo", () -> neighbor.getEditor().isFocusOwner());

    assertThat(onEdt(() -> "Mars".equals(combo.getSelectedValue())))
        .as("leaving the control for good is a commit point")
        .isTrue();
  }

  @Test
  void focusLeavingTheControlRevertsUnrecognizedTextUnderTheConstrainedPolicy() throws Exception {
    focusEditor();
    SwingUtilities.invokeAndWait(() -> editor().setText("Pluto"));

    SwingUtilities.invokeAndWait(() -> neighbor.getEditor().requestFocusInWindow());
    waitFor("focus lands outside the combo", () -> neighbor.getEditor().isFocusOwner());

    assertThat(onEdt(() -> "Venus".equals(combo.getText())))
        .as("a constrained combo falls back to the committed value when focus departs")
        .isTrue();
  }

  @Test
  void hoppingFromTheEditorToTheArrowIsNotACommitPoint() throws Exception {
    focusEditor();
    SwingUtilities.invokeAndWait(() -> editor().setText("Pluto"));

    SwingUtilities.invokeAndWait(
        () ->
            ((ElwhaTextField) combo.getComponent(0))
                .getTrailingIconButton()
                .requestFocusInWindow());
    waitFor(
        "focus moves to the arrow, still inside the control",
        () -> ((ElwhaTextField) combo.getComponent(0)).getTrailingIconButton().isFocusOwner());

    assertThat(onEdt(() -> "Pluto".equals(combo.getText())))
        .as("reaching for the dropdown arrow must not throw away what the user was typing")
        .isTrue();
  }

  private void focusEditor() throws Exception {
    SwingUtilities.invokeAndWait(() -> editor().requestFocusInWindow());
    waitFor("the combo's editor owns focus", () -> editor().isFocusOwner());
  }

  private JTextComponent editor() {
    return ((ElwhaTextField) combo.getComponent(0)).getEditor();
  }
}
