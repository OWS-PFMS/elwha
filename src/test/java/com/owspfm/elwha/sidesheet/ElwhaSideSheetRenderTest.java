package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the chrome a side sheet derives from its type and posture: which container
 * role fills the body, which edge wears the standard divider (and which configurations drop it),
 * which corners round, and the gap a detached sheet floats in.
 *
 * <p>All three chrome axes are type/posture-derived by design — there are no raw chrome setters —
 * so these probes are how the derivation itself is pinned.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSideSheetRenderTest {

  private static final int WIDTH = 256;
  private static final int HEIGHT = 400;

  /**
   * Renders against a high-contrast ground.
   *
   * <p>Every probe here needs one. In the baseline palette {@code SURFACE} and {@code
   * SURFACE_CONTAINER_LOW} are 5/255 apart in light and 9/255 in dark — both inside the ±10 probe
   * tolerance — and the default render ground is {@code SURFACE}. So over the default ground a
   * probe expecting either role passes whether the sheet filled with that role, filled with the
   * other one, or never painted at all. Against {@code PRIMARY} the three outcomes are distinct.
   *
   * <p>This was found vacuous rather than predicted: the shape probes already used this ground and
   * said why, but the container-fill and divider probes did not, and the negative divider
   * assertions ("nothing painted on this edge") were passing against the ground colour they were
   * asserting (#707).
   */
  private static BufferedImage shotOf(final ElwhaSideSheet sheet) {
    return Pixels.render(sheet, sheet.modalFootprintWidth(), HEIGHT, GROUND);
  }

  private static final Color GROUND = ColorRole.PRIMARY.resolve();

  private static ElwhaSideSheet standard() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setSheetWidth(WIDTH);
    return sheet;
  }

  private static ElwhaSideSheet modal() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
    sheet.setSheetWidth(WIDTH);
    return sheet;
  }

  // ----------------------------------------------------------- container fill

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aStandardSheetFillsThePlainSurfaceRole(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaSideSheet sheet = standard();

    Pixels.assertPixelExact(
        shotOf(sheet),
        WIDTH / 2,
        HEIGHT - 4,
        ColorRole.SURFACE.resolve(),
        "an embedded panel is page furniture, so it takes the plain surface role");
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aModalSheetLiftsToTheContainerLowRole(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaSideSheet sheet = modal();

    Pixels.assertPixelExact(
        shotOf(sheet),
        WIDTH / 2,
        HEIGHT - 4,
        ColorRole.SURFACE_CONTAINER_LOW.resolve(),
        "a sheet presented over a scrim lifts off the page it covers");
  }

  // -------------------------------------------------------------- edge divider

  @Test
  void aDockedStandardSheetDrawsItsDividerOnTheContentFacingEdge() {
    final ElwhaSideSheet sheet = standard();

    final BufferedImage shot = shotOf(sheet);

    Pixels.assertPixelExact(
        shot,
        0,
        HEIGHT / 2,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "a trailing-docked sheet needs a boundary on its leading (content-facing) edge");
    Pixels.assertPixelExact(
        shot,
        WIDTH - 1,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "and nothing on the window edge it is flush against");
  }

  @Test
  void aLeadingDockedSheetMovesTheDividerToItsOtherEdge() {
    final ElwhaSideSheet sheet = standard();
    sheet.setSheetEdge(SheetEdge.LEADING);

    final BufferedImage shot = shotOf(sheet);

    Pixels.assertPixelExact(
        shot,
        WIDTH - 1,
        HEIGHT / 2,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "docking on the leading edge puts the content-facing boundary on the trailing side");
  }

  @Test
  void rightToLeftFlipsWhichEdgeIsContentFacing() {
    final ElwhaSideSheet sheet = standard();
    sheet.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    final BufferedImage shot = shotOf(sheet);

    Pixels.assertPixelExact(
        shot,
        WIDTH - 1,
        HEIGHT / 2,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "a trailing sheet docks on the left under RTL, so the divider moves to the right");
  }

  @Test
  void turningTheDividerOffLeavesABareEdge() {
    final ElwhaSideSheet sheet = standard();
    sheet.setEdgeDividerVisible(false);

    Pixels.assertPixelExact(
        shotOf(sheet),
        0,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the boundary is a toggle for hosts that draw their own");
  }

  @Test
  void aModalSheetDrawsNoEdgeDivider() {
    final ElwhaSideSheet sheet = modal();

    Pixels.assertPixelExact(
        shotOf(sheet),
        0,
        HEIGHT / 2,
        ColorRole.SURFACE_CONTAINER_LOW.resolve(),
        "a rounded, scrim-backed sheet reads as a panel without a line");
  }

  @Test
  void aDetachedSheetDrawsNoEdgeDivider() {
    final ElwhaSideSheet sheet = standard();
    sheet.setSheetPosture(SheetPosture.DETACHED);
    final int margin = ElwhaSideSheet.DETACHED_MARGIN_PX;

    Pixels.assertPixelExact(
        shotOf(sheet),
        margin,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "a floating card is bounded by its own rounded edge, not a divider");
  }

  // -------------------------------------------------------------------- shape

  @Test
  void aDockedStandardSheetHasSquareCorners() {
    final ElwhaSideSheet sheet = standard();

    Pixels.assertPixelNear(
        shotOf(sheet),
        WIDTH - 1,
        0,
        ColorRole.SURFACE.resolve(),
        "an embedded panel meets the window edge square, so its corner pixel is still container");
  }

  @Test
  void aDockedModalSheetRoundsOnlyItsContentFacingCorners() {
    final ElwhaSideSheet sheet = modal();
    final BufferedImage shot = shotOf(sheet);

    Pixels.assertPixelNear(
        shot,
        0,
        0,
        GROUND,
        "the content-facing top corner is cut away, so the ground shows through it");
    Pixels.assertPixelNear(
        shot,
        WIDTH - 1,
        0,
        ColorRole.SURFACE_CONTAINER_LOW.resolve(),
        "while the corner against the window edge stays square");
  }

  @Test
  void aDetachedSheetRoundsEveryCornerAndFloatsInItsMargin() {
    final ElwhaSideSheet sheet = modal();
    sheet.setSheetPosture(SheetPosture.DETACHED);
    final int margin = ElwhaSideSheet.DETACHED_MARGIN_PX;
    final BufferedImage shot = shotOf(sheet);
    final int trailingBodyEdge = sheet.modalFootprintWidth() - margin - 1;

    Pixels.assertPixelNear(
        shot,
        margin / 2,
        HEIGHT / 2,
        GROUND,
        "the margin band around a floating card shows whatever is behind it");
    Pixels.assertPixelNear(
        shot,
        trailingBodyEdge,
        margin,
        GROUND,
        "and the trailing top corner is rounded too, unlike a docked sheet's");
    Pixels.assertPixelNear(
        shot,
        trailingBodyEdge,
        HEIGHT / 2,
        ColorRole.SURFACE_CONTAINER_LOW.resolve(),
        "with the body itself filling the space inside the margin");
  }

  @Test
  void roundedCornersUseTheLargeShapeToken() {
    final ElwhaSideSheet sheet = modal();
    final int radius = ShapeScale.LG.px();
    final BufferedImage shot = shotOf(sheet);

    Pixels.assertPixelNear(
        shot,
        radius + 2,
        1,
        ColorRole.SURFACE_CONTAINER_LOW.resolve(),
        "past the corner radius the top edge is solid container again");
    Pixels.assertPixelNear(
        shot, 1, 1, GROUND, "while inside the radius the corner is still cut away");
  }
}
