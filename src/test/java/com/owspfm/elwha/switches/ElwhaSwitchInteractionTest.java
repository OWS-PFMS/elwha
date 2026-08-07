package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A spike for the #439 framework selection — the {@link ElwhaSwitchInteractionSmoke} §7
 * interaction contract re-expressed as JUnit tests, exercising the three assertion families the
 * regression suite (#438) needs headless: synthetic input event contracts, pixel probes against
 * resolved token colors, and accessibility shape. Two deliberate upgrades over the smoke idiom:
 * mouse events go through the real {@code Component.dispatchEvent} pipeline rather than direct
 * listener iteration, and keyboard coverage asserts the {@code WHEN_FOCUSED} InputMap bindings
 * before invoking the mapped actions (the smokes invoked actions by name, leaving the binding
 * itself untested).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@ExtendWith(EdtInterceptor.class)
class ElwhaSwitchInteractionTest {

  private static final int W = 60;
  private static final int H = 40;
  private static final int CY = 20;

  @BeforeEach
  void installBaselineLight() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
  }

  @Test
  void clickTogglesAndFiresEachListenerOnce() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final int[] actions = {0};
    final int[] changes = {0};
    final String[] lastCommand = {null};
    s.addActionListener(
        e -> {
          actions[0]++;
          lastCommand[0] = e.getActionCommand();
        });
    s.addChangeListener(e -> changes[0]++);

    press(s, 30, CY);
    release(s, 30, CY);

    assertThat(s.isSelected()).as("click toggles on").isTrue();
    assertThat(actions[0]).as("ActionListener fires once").isEqualTo(1);
    assertThat(changes[0]).as("ChangeListener fires once").isEqualTo(1);
    assertThat(lastCommand[0])
        .as("action command reflects the committed state")
        .isEqualTo("selected");
  }

  @Test
  void releaseOutsideBoundsCancelsTheClick() {
    final ElwhaSwitch s = sized(new ElwhaSwitch(true));
    final int[] events = {0};
    s.addActionListener(e -> events[0]++);
    s.addChangeListener(e -> events[0]++);

    press(s, 30, CY);
    release(s, 200, CY);

    assertThat(s.isSelected()).as("state unchanged after outside release").isTrue();
    assertThat(events[0]).as("cancelled click fires no events").isZero();
  }

  @Test
  void programmaticSetSelectedFiresChangeListenerOnly() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final int[] actions = {0};
    final int[] changes = {0};
    s.addActionListener(e -> actions[0]++);
    s.addChangeListener(e -> changes[0]++);

    s.setSelected(true);

    assertThat(changes[0]).as("setSelected fires ChangeListener").isEqualTo(1);
    assertThat(actions[0]).as("setSelected never fires ActionListener").isZero();
  }

  @Test
  void dragCommitsToTheNearestHalf() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final int[] actions = {0};
    s.addActionListener(e -> actions[0]++);

    press(s, 20, CY);
    drag(s, 40, CY);
    release(s, 40, CY);
    assertThat(s.isSelected()).as("drag to the far end commits selected").isTrue();
    assertThat(actions[0]).as("committed drag fires once").isEqualTo(1);

    press(s, 40, CY);
    drag(s, 24, CY);
    release(s, 24, CY);
    assertThat(s.isSelected()).as("drag past the midpoint back commits unselected").isFalse();
    assertThat(actions[0]).as("each committed drag fires once").isEqualTo(2);

    press(s, 20, CY);
    drag(s, 26, CY);
    release(s, 26, CY);
    assertThat(s.isSelected()).as("scrub landing on the starting half commits nothing").isFalse();
    assertThat(actions[0]).as("uncommitted drag fires no events").isEqualTo(2);
  }

  @Test
  void spaceIsBoundWhenFocusedAndTogglesOnRelease() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final var inputMap = s.getInputMap(JComponent.WHEN_FOCUSED);

    final Object pressKey = inputMap.get(KeyStroke.getKeyStroke("pressed SPACE"));
    final Object releaseKey = inputMap.get(KeyStroke.getKeyStroke("released SPACE"));
    assertThat(pressKey)
        .as("pressed SPACE is bound in WHEN_FOCUSED")
        .isEqualTo("elwhaSwitch.press");
    assertThat(releaseKey)
        .as("released SPACE is bound in WHEN_FOCUSED")
        .isEqualTo("elwhaSwitch.release");

    invokeAction(s, pressKey);
    assertThat(s.isSelected()).as("Space press alone does not toggle").isFalse();
    invokeAction(s, releaseKey);
    assertThat(s.isSelected()).as("Space release commits the toggle").isTrue();
  }

  @Test
  void disabledSwitchIgnoresMouseAndKeyboard() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    s.setEnabled(false);
    final int[] events = {0};
    s.addActionListener(e -> events[0]++);
    s.addChangeListener(e -> events[0]++);

    press(s, 30, CY);
    release(s, 30, CY);
    invokeAction(s, "elwhaSwitch.press");
    invokeAction(s, "elwhaSwitch.release");

    assertThat(s.isSelected()).as("disabled switch never toggles").isFalse();
    assertThat(events[0]).as("disabled switch fires no events").isZero();
  }

  @Test
  void accessibleShapeMatchesTheToggleContract() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    s.setLabel("Wi-Fi");
    final AccessibleContext ac = s.getAccessibleContext();

    assertThat(ac.getAccessibleRole()).as("role").isEqualTo(AccessibleRole.TOGGLE_BUTTON);
    assertThat(ac.getAccessibleName()).as("name comes from the label").isEqualTo("Wi-Fi");
    assertThat(ac.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("unselected carries no CHECKED")
        .isFalse();
    assertThat(ac.getAccessibleValue().getCurrentAccessibleValue())
        .as("unselected value")
        .isEqualTo(0);

    assertThat(ac.getAccessibleAction().doAccessibleAction(0)).as("action 0 performs").isTrue();

    assertThat(s.isSelected()).as("accessible action toggles").isTrue();
    assertThat(ac.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("selected carries CHECKED")
        .isTrue();
    assertThat(ac.getAccessibleValue().getCurrentAccessibleValue())
        .as("selected value")
        .isEqualTo(1);

    s.setEnabled(false);
    assertThat(ac.getAccessibleAction().doAccessibleAction(0))
        .as("accessible action refuses while disabled")
        .isFalse();
  }

  @Test
  void hoverPaintsTheStateLayerAndShiftsTheHandleRole() {
    final ElwhaSwitch hover = sized(new ElwhaSwitch());
    hover.setHovered(true);
    final BufferedImage img = Pixels.render(hover, W, H);

    Pixels.assertPixelNear(
        img,
        7,
        7,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.HOVER.opacity()),
        "hover paints the ON_SURFACE hover layer on the halo");
    Pixels.assertPixelNear(
        img,
        20,
        CY,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "hover shifts the unselected handle to ON_SURFACE_VARIANT");
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void selectedTrackResolvesPrimaryInBothModes(final Mode mode) {
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
    final ElwhaSwitch s = sized(new ElwhaSwitch(true));
    final BufferedImage img = Pixels.render(s, W, H);

    Pixels.assertPixelNear(
        img,
        9,
        CY,
        ColorRole.PRIMARY.resolve(),
        "selected track interior resolves PRIMARY in " + mode);
  }

  private static ElwhaSwitch sized(final ElwhaSwitch s) {
    s.setSize(W, H);
    return s;
  }

  private static void press(final ElwhaSwitch s, final int x, final int y) {
    mouse(s, MouseEvent.MOUSE_PRESSED, x, y);
  }

  private static void drag(final ElwhaSwitch s, final int x, final int y) {
    mouse(s, MouseEvent.MOUSE_DRAGGED, x, y);
  }

  private static void release(final ElwhaSwitch s, final int x, final int y) {
    mouse(s, MouseEvent.MOUSE_RELEASED, x, y);
  }

  private static void mouse(final ElwhaSwitch s, final int id, final int x, final int y) {
    final int modifiers = (id == MouseEvent.MOUSE_RELEASED) ? 0 : MouseEvent.BUTTON1_DOWN_MASK;
    // Explicit-abs-coords ctor: the (x,y)-only ctor calls getLocationOnScreen, which NPEs on an
    // unrealized (peerless) component in headless mode.
    s.dispatchEvent(
        new MouseEvent(
            s, id, System.nanoTime(), modifiers, x, y, x, y, 1, false, MouseEvent.BUTTON1));
  }

  private static void invokeAction(final ElwhaSwitch s, final Object actionKey) {
    s.getActionMap()
        .get(actionKey)
        .actionPerformed(new ActionEvent(s, ActionEvent.ACTION_PERFORMED, "space"));
  }
}
