package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaButton}'s #176 press and selection morphs, and of the width-borrow
 * surface a hosting {@code ElwhaButtonGroup} drives.
 *
 * <p>No frame of any animation is sampled from the wall clock. {@code ThemeExtension} pins reduced
 * motion, under which every {@code MorphAnimator} snaps to its endpoint the moment it is started —
 * so a synthetic press puts the morph at exactly progress 1 and the painted geometry is a pure
 * function of state. The probes are chrome-only (label-free bodies) at coordinates chosen with
 * several pixels of clearance from every anti-aliased boundary.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonMorphTest {

  /** The L tier's label-free body: 96x96, no halo, no touch-target inflation — clean arithmetic. */
  private static final int BODY = 96;

  /** Just inside the natural body's leading edge, at mid-height, on the pill's widest line. */
  private static final int EDGE_PROBE_X = 1;

  /**
   * Inside a 28 px square corner but outside a 48 px pill corner — the shape-flip discriminator.
   */
  private static final int CORNER_PROBE = 11;

  private static ElwhaButton bigButton() {
    final ElwhaButton button =
        new ElwhaButton()
            .setButtonSize(ButtonSize.L)
            .setShape(ButtonShape.ROUND)
            // Pinning the container role keeps the fill constant across the selection swap, so the
            // probes below isolate geometry from color.
            .setSurfaceRole(ColorRole.PRIMARY);
    button.setSize(BODY, BODY);
    return button;
  }

  private static BufferedImage render(final ElwhaButton button) {
    return Pixels.render(button, BODY, BODY);
  }

  @Test
  void reducedMotionIsPinnedSoEveryMorphEndpointIsExact() {
    assertThat(MorphAnimator.isReducedMotion())
        .as("the suite never races a Timer — the fixture pins reduced motion for every test")
        .isTrue();
  }

  // ------------------------------------------------------------ press morph

  @Test
  void pressingAPushButtonPinchesThePaintedBodyInward() {
    final ElwhaButton button = bigButton();

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        ColorRole.PRIMARY.resolve(),
        "at rest the body reaches its natural leading edge");

    Input.press(button, BODY / 2, BODY / 2);

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        ColorRole.SURFACE.resolve(),
        "a press narrows the painted body, so the old leading edge is now background");
  }

  @Test
  void releasingRestoresTheNaturalWidth() {
    final ElwhaButton button = bigButton();

    Input.press(button, BODY / 2, BODY / 2);
    Input.release(button, BODY / 2, BODY / 2);

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        ColorRole.PRIMARY.resolve(),
        "the release reverses the pinch back to the natural width");
  }

  @Test
  void layoutIsNeverDisturbedByThePressPinch() {
    final ElwhaButton button = bigButton();
    final int restingWidth = button.getPreferredSize().width;

    Input.press(button, BODY / 2, BODY / 2);

    assertThat(button.getPreferredSize().width)
        .as("the pinch is a paint-layer offset — preferred size does not move under it")
        .isEqualTo(restingWidth);
  }

  @Test
  void aSelectableButtonSuppressesThePressPinchSoTheFlipOwnsTheShapeSignal() {
    final ElwhaButton button = bigButton().setInteractionMode(ButtonInteractionMode.SELECTABLE);

    Input.press(button, BODY / 2, BODY / 2);

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        Pixels.mix(
            ColorRole.PRIMARY.resolve(),
            ColorRole.ON_PRIMARY.resolve(),
            StateLayer.PRESSED.opacity()),
        "a toggle keeps its full width on press and shows only the pressed layer");
  }

  @Test
  void workbenchHookDrivesThePressMorphEvenOnAToggleAndFiresNothing() {
    final ElwhaButton button = bigButton().setInteractionMode(ButtonInteractionMode.SELECTABLE);
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);
    button.addSelectionChangeListener(e -> fired[0]++);

    button.triggerPressAnimation();

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        ColorRole.SURFACE.resolve(),
        "triggerPressAnimation deliberately ignores the toggle suppression");
    assertThat(fired[0]).as("and is motion only — no activation and no selection change").isZero();
  }

  // ----------------------------------------------------------- select morph

  @Test
  void selectingFlipsTheRoundBodyToItsSquareCounterpart() {
    final ElwhaButton button = bigButton().setInteractionMode(ButtonInteractionMode.SELECTABLE);

    Pixels.assertPixelNear(
        render(button),
        CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.SURFACE.resolve(),
        "the resting pill leaves its corner empty");

    button.setSelected(true);

    Pixels.assertPixelNear(
        render(button),
        CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.PRIMARY.resolve(),
        "the selected body squares up to the tier's square corner and fills that corner");
  }

  @Test
  void deselectingFlipsTheShapeBack() {
    final ElwhaButton button =
        bigButton().setInteractionMode(ButtonInteractionMode.SELECTABLE).setSelected(true);

    button.setSelected(false);

    Pixels.assertPixelNear(
        render(button),
        CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.SURFACE.resolve(),
        "the flip is symmetric — deselecting restores the pill");
  }

  @Test
  void aSquareButtonFlipsTheOtherWayWhenSelected() {
    final ElwhaButton button =
        bigButton()
            .setShape(ButtonShape.SQUARE)
            .setInteractionMode(ButtonInteractionMode.SELECTABLE);

    Pixels.assertPixelNear(
        render(button),
        CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.PRIMARY.resolve(),
        "a square resting body already fills its corner");

    button.setSelected(true);

    Pixels.assertPixelNear(
        render(button),
        CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.SURFACE.resolve(),
        "selection takes the opposite shape, so a square group's selected segment rounds out");
  }

  @Test
  void aDisabledButtonLandsOnTheNewShapeWithoutAnimatingIntoIt() {
    final ElwhaButton button = bigButton().setInteractionMode(ButtonInteractionMode.SELECTABLE);
    button.setEnabled(false);

    button.setSelected(true);

    assertThat(button.isSelected())
        .as("a disabled toggle still accepts a programmatic selection")
        .isTrue();
    Pixels.assertPixelNear(
        render(button),
        CORNER_PROBE,
        CORNER_PROBE,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.PRIMARY.resolve(),
            StateLayer.disabledContainerOpacity()),
        "and paints the selected shape immediately, dimmed by the disabled treatment");
  }

  // ---------------------------------------------------------- width borrow

  @Test
  void aRestingButtonBorrowsNoWidth() {
    assertThat(bigButton().currentWidthBorrow())
        .as("a button outside a group's width ripple contributes nothing")
        .isEqualTo(0f);
  }

  @Test
  void borrowFactorIsReportedBackOnceTheMorphSettles() {
    final ElwhaButton button = bigButton();

    button.startWidthBorrow(0.3f);

    assertThat(button.currentWidthBorrow())
        .as("the readout is the morph progress scaled by the factor the group handed in")
        .isEqualTo(0.3f, within(1e-6f));
  }

  @Test
  void borrowFactorsClampToTheUnitInterval() {
    assertThat(borrowAfter(7f))
        .as("an over-large factor clamps to a full pinch")
        .isEqualTo(1f, within(1e-6f));
    assertThat(borrowAfter(-7f))
        .as("and a negative one clamps to a full lend")
        .isEqualTo(-1f, within(1e-6f));
  }

  @Test
  void aZeroFactorIsTheReleasePath() {
    final ElwhaButton button = bigButton();
    button.startWidthBorrow(1f);

    button.startWidthBorrow(0f);

    assertThat(button.currentWidthBorrow())
        .as("a group computing a borrow vector may pass zero instead of calling release")
        .isEqualTo(0f);
  }

  @Test
  void releasingReturnsTheBorrowToZero() {
    final ElwhaButton button = bigButton();
    button.startWidthBorrow(1f);

    button.releaseWidthBorrow();
    button.releaseWidthBorrow();

    assertThat(button.currentWidthBorrow()).as("release is idempotent").isEqualTo(0f);
  }

  @Test
  void aBorrowedWidthPinchesThePaintedBodyTheSameWayAPressDoes() {
    final ElwhaButton button = bigButton();

    button.startWidthBorrow(1f);

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        ColorRole.SURFACE.resolve(),
        "a neighbor's press reaches this button's painted width through the borrow");

    button.releaseWidthBorrow();

    Pixels.assertPixelNear(
        render(button),
        EDGE_PROBE_X,
        BODY / 2,
        ColorRole.PRIMARY.resolve(),
        "and releasing it restores the natural width");
  }

  private static float borrowAfter(final float factor) {
    final ElwhaButton button = bigButton();
    button.startWidthBorrow(factor);
    return button.currentWidthBorrow();
  }
}
