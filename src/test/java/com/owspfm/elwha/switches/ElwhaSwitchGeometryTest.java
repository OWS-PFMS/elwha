package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the state-layer-inclusive box, the handle's rest positions, and the
 * right-to-left mirroring the class Javadoc promises — the travel, not just the paint, flips.
 *
 * <p>Mirroring is asserted in <em>value space</em>: {@code handleCenterX()} maps a selection
 * fraction onto pixels, and under a right-to-left orientation selected must land at the low-x end.
 * That is the property a drag's commit halves and the slide tween both depend on, so pinning it
 * here catches a half-mirrored implementation that still looks right at rest.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchGeometryTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final int CENTER_Y = 20;

  /** Handle centre at the unselected end of the travel — trackX + trackHeight/2. */
  private static final int TRAVEL_START_X = 20;

  /** Handle centre at the selected end — trackX + trackWidth - trackHeight/2. */
  private static final int TRAVEL_END_X = 40;

  private static ElwhaSwitch sized(final ElwhaSwitch toggle) {
    toggle.setSize(WIDTH, HEIGHT);
    return toggle;
  }

  // ------------------------------------------------------------------ sizing

  @Test
  void preferredSizeIsTheTrackPlusTheHaloOverhang() {
    assertThat(new ElwhaSwitch().getPreferredSize())
        .as(
            "the box reserves the interaction halo's overhang on every side, so the 40px circle"
                + " never clips")
        .isEqualTo(
            new Dimension(
                ElwhaSwitch.TRACK_WIDTH_PX + 2 * ElwhaSwitch.HALO_OVERHANG_PX,
                ElwhaSwitch.TRACK_HEIGHT_PX + 2 * ElwhaSwitch.HALO_OVERHANG_PX));
  }

  @Test
  void haloOverhangIsHalfTheDifferenceBetweenTheCircleAndTheTrack() {
    assertThat(ElwhaSwitch.HALO_OVERHANG_PX)
        .as("the overhang is derived from the two M3 tokens, not a separate magic number")
        .isEqualTo((ElwhaSwitch.STATE_LAYER_SIZE_PX - ElwhaSwitch.TRACK_HEIGHT_PX) / 2);
  }

  @Test
  void minimumSizeMatchesPreferredSize() {
    final ElwhaSwitch toggle = new ElwhaSwitch();

    assertThat(toggle.getMinimumSize())
        .as("a switch has fixed M3 geometry and does not compress")
        .isEqualTo(toggle.getPreferredSize());
  }

  @Test
  void anExplicitPreferredSizeWins() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    final Dimension fixed = toggle.getPreferredSize();
    final Dimension explicit = new Dimension(200, 90);

    toggle.setPreferredSize(explicit);

    assertThat(toggle.getPreferredSize())
        .as("a caller-set preferred size wins, matching ElwhaCheckbox and ElwhaRadioButton (#567)")
        .isEqualTo(explicit)
        .isNotEqualTo(fixed);
  }

  @Test
  void chromeCentresItselfInLargerBounds() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());

    assertThat(toggle.handleCenterY())
        .as("the track block centres vertically rather than pinning to the top")
        .isEqualTo(CENTER_Y);

    toggle.setSize(WIDTH + 40, HEIGHT + 40);
    assertThat(toggle.handleCenterY())
        .as("and follows the centre when the component is given more room")
        .isEqualTo((HEIGHT + 40) / 2);
  }

  // -------------------------------------------------------- handle at rest

  @Test
  void handleRestsAtEitherEndOfItsTravel() {
    assertThat(sized(new ElwhaSwitch()).handleCenterX())
        .as("an unselected handle rests at the start of the travel")
        .isEqualTo(TRAVEL_START_X);
    assertThat(sized(new ElwhaSwitch(true)).handleCenterX())
        .as("and a selected one at the end")
        .isEqualTo(TRAVEL_END_X);
  }

  @Test
  void togglingMovesTheHandleBetweenTheRestPoints() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());

    toggle.setSelected(true);
    assertThat(toggle.handleCenterX())
        .as("with motion pinned off the handle lands directly on its new rest point")
        .isEqualTo(TRAVEL_END_X);

    toggle.setSelected(false);
    assertThat(toggle.handleCenterX()).as("and comes back").isEqualTo(TRAVEL_START_X);
  }

  // --------------------------------------------------------------------- RTL

  @Test
  void rightToLeftFlipsTheTravelInValueSpace() {
    final ElwhaSwitch off = sized(new ElwhaSwitch());
    off.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    assertThat(off.handleCenterX())
        .as("mirrored, the unselected handle rests at the high-x end")
        .isEqualTo(TRAVEL_END_X);

    final ElwhaSwitch on = sized(new ElwhaSwitch(true));
    on.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    assertThat(on.handleCenterX())
        .as(
            "and the selected handle rests at the low-x end — the whole travel flips, not just the"
                + " painted chrome")
        .isEqualTo(TRAVEL_START_X);
  }

  @Test
  void changingOrientationMovesAnAlreadyPlacedHandle() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch(true));
    assertThat(toggle.handleCenterX()).as("it starts at the selected end").isEqualTo(TRAVEL_END_X);

    toggle.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    assertThat(toggle.handleCenterX())
        .as("the mirror is applied at read time, so an existing switch flips with the orientation")
        .isEqualTo(TRAVEL_START_X);
  }

  @Test
  void rightToLeftPaintsTheSelectedHandleAtTheLowEnd() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    toggle.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Pixels.assertPixelNear(
        Pixels.render(toggle, WIDTH, HEIGHT),
        TRAVEL_START_X,
        CENTER_Y,
        ColorRole.ON_PRIMARY.resolve(),
        "the mirrored rest position is what actually paints, not just what the accessor reports");
  }

  // ------------------------------------------------------- press-grown handle

  @Test
  void aPressedHandleGrowsPastItsRestingDiameter() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    Pixels.assertPixelNear(
        Pixels.render(toggle, WIDTH, HEIGHT),
        9,
        CENTER_Y,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "at rest the 16px handle leaves this column to the track");

    toggle.setPressed(true);

    Pixels.assertPixelNear(
        Pixels.render(toggle, WIDTH, HEIGHT),
        9,
        CENTER_Y,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "a press grows the handle to 28px and shifts it to the interactive role");
  }
}
