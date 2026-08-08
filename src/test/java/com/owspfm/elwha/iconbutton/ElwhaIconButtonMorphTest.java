package com.owspfm.elwha.iconbutton;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaIconButton}'s #295 press shape-morph — the bidirectional corner
 * treatment that has to carry the press cue on its own, because a fixed-size square has no
 * companion width pinch and the ripple is suppressible (#288).
 *
 * <p>Reduced motion is pinned by the fixture, so a synthetic press puts the morph at exactly its
 * endpoint and the corner geometry is a pure function of state — no animation frame is sampled from
 * the wall clock. The probes sit five or more pixels clear of both the resting and pressed corner
 * arcs on an oversized body, so anti-aliasing never decides the outcome.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaIconButtonMorphTest {

  /** An oversized square body: a 60 px max radius makes the 40 % morph delta 24 px wide. */
  private static final int SIDE = 120;

  /** Outside a 60 px circular corner, inside the 36 px corner a press morphs a round body to. */
  private static final int ROUND_PROBE = 14;

  /** Inside a square corner, outside the 24 px corner a press morphs a square body to. */
  private static final int SQUARE_PROBE = 3;

  private static ElwhaIconButton bigButton(final ShapeScale shape) {
    final ElwhaIconButton button =
        new ElwhaIconButton().setVariant(IconButtonVariant.FILLED).setShape(shape);
    button.setContainerSize(SIDE);
    button.setSize(SIDE, SIDE);
    return button;
  }

  private static Color pressedFill() {
    return Pixels.mix(
        ColorRole.PRIMARY.resolve(), ColorRole.ON_PRIMARY.resolve(), StateLayer.PRESSED.opacity());
  }

  @Test
  void pressingARoundButtonSquaresItsCornersUp() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        ColorRole.SURFACE.resolve(),
        "a capsule leaves its corner empty at rest");

    Input.press(button, SIDE / 2, SIDE / 2);

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        pressedFill(),
        "the press sheds radius, so the body reaches into the corner");
  }

  @Test
  void pressingASquareButtonRoundsItsCornersOut() {
    final ElwhaIconButton button = bigButton(ShapeScale.NONE);

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        SQUARE_PROBE,
        SQUARE_PROBE,
        ColorRole.PRIMARY.resolve(),
        "a square body fills its corner at rest");

    Input.press(button, SIDE / 2, SIDE / 2);

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        SQUARE_PROBE,
        SQUARE_PROBE,
        ColorRole.SURFACE.resolve(),
        "the morph is bidirectional, so a body with no roundness to shed gains some instead");
  }

  @Test
  void releasingRestoresTheRestingCorners() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);

    Input.press(button, SIDE / 2, SIDE / 2);
    Input.release(button, SIDE / 2, SIDE / 2);

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        ColorRole.SURFACE.resolve(),
        "the release reverses the morph back to the resting capsule");
  }

  @Test
  void aPollDetectedExitRestoresTheRestingCornersToo() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);
    Input.enter(button, SIDE / 2, SIDE / 2);
    Input.press(button, SIDE / 2, SIDE / 2);

    // The case the poll exists to catch: the pointer left the window (or the button was hidden)
    // with the press still down, so no mouseExited will arrive. An unrealized button is never
    // showing, which is exactly that branch.
    button.pollHoverState();

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        ColorRole.SURFACE.resolve(),
        "clearing the flag is not enough — morphedRadii reads the animator, so the poll has to"
            + " reverse the morph or the button rests in the pressed geometry");
  }

  @Test
  void disablingMidPressRestoresTheRestingCorners() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);
    Input.press(button, SIDE / 2, SIDE / 2);

    button.setEnabled(false);
    button.setEnabled(true);

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        ColorRole.SURFACE.resolve(),
        "a button disabled mid-press comes back at rest, not still wearing the press");
  }

  @Test
  void keyboardActivationGivesTheSameConfirmationAPointerPressDoes() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);

    Input.pressBoundKey(button, "pressed SPACE", "elwhaiconbutton.activate");

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        pressedFill(),
        "#562 — a keyboard activation flashes every press affordance this button has, the shape"
            + " morph and the pressed layer together, exactly as a pointer press does; the three"
            + " members of the actions family used to give three different answers here");
  }

  @Test
  void aDisabledButtonNeverEntersThePressedShape() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);
    button.setEnabled(false);

    Input.press(button, SIDE / 2, SIDE / 2);
    Input.pressBoundKey(button, "pressed SPACE", "elwhaiconbutton.activate");

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        ROUND_PROBE,
        ROUND_PROBE,
        ColorRole.SURFACE.resolve(),
        "a disabled button is visually frozen through both input paths");
  }

  @Test
  void aConnectedOverrideReplacesTheShapeDerivedCorners() {
    final ElwhaIconButton button = bigButton(ShapeScale.FULL);

    button.setCornerRadii(CornerRadii.of(0, 60, 60, 0));

    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        SQUARE_PROBE,
        SQUARE_PROBE,
        ColorRole.PRIMARY.resolve(),
        "the group-supplied override squares the leading corners of a butted segment");
    Pixels.assertPixelNear(
        Pixels.render(button, SIDE, SIDE),
        SIDE - 1 - ROUND_PROBE,
        ROUND_PROBE,
        ColorRole.SURFACE.resolve(),
        "while its outward corners keep the group's pill end-cap");
  }
}
