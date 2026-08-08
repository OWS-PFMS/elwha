package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of what a chip paints: the per-variant surface and border, GHOST's
 * paint-nothing-at-rest treatment and its exemption from selection rendering, the border swap that
 * makes a selected chip read as "the picked one", and the disabled compositing pass.
 *
 * <p>Every chip here is built with empty text so no glyph pixels exist to probe by accident, and
 * onto a magenta ground so "nothing painted here" is unambiguous — the difference that matters for
 * GHOST.
 *
 * <p><strong>Coverage boundary.</strong> Hover and press chrome are deliberately absent. The chip
 * has no gallery-style {@code setHovered} hook; hover is entered by a real {@code MOUSE_ENTERED}
 * and then policed by a pointer-position poll timer that clears it as soon as it finds the chip is
 * not showing, and a press starts a wall-clock ripple that paints over the surface. Both would make
 * a headless pixel assertion time-dependent, so both belong to the {@code gui} tier. The press
 * <em>behaviour</em> is covered in full by {@code ElwhaChipInteractionTest}.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipRenderTest {

  private static final int WIDTH = 120;
  private static final int HEIGHT = 40;
  private static final int CENTER_X = 60;
  private static final int CENTER_Y = 20;

  /** A ground no bundled palette carries, so an unpainted chip is unmistakable. */
  private static final Color GROUND = Color.MAGENTA;

  /** Border probes use a thick stroke so the sample sits well inside it rather than on its edge. */
  private static final int THICK_BORDER = 4;

  private static final int BORDER_X = 1;

  private static BufferedImage render(final ElwhaChip chip) {
    return Pixels.render(chip, WIDTH, HEIGHT, GROUND);
  }

  private static ElwhaChip chip(final ChipVariant variant) {
    return new ElwhaChip("").setVariant(variant);
  }

  // ----------------------------------------------------------------- surface

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void filledPaintsThePrimaryContainerSurface(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED)),
        CENTER_X,
        CENTER_Y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "a FILLED chip fills with its variant's PRIMARY_CONTAINER in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void outlinedPaintsThePlainSurface(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(chip(ChipVariant.OUTLINED)),
        CENTER_X,
        CENTER_Y,
        ColorRole.SURFACE.resolve(),
        "an OUTLINED chip fills with SURFACE in " + mode);
  }

  @Test
  void ghostPaintsNothingAtRest() {
    final BufferedImage image = render(chip(ChipVariant.GHOST));

    Pixels.assertPixelNear(
        image, CENTER_X, CENTER_Y, GROUND, "an idle GHOST chip has no resting fill");
    Pixels.assertPixelNear(
        image, BORDER_X, CENTER_Y, GROUND, "and no resting border — it is text with padding");
  }

  @Test
  void anOverriddenSurfaceRoleDrivesTheFill() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED).setSurfaceRole(ColorRole.SECONDARY_CONTAINER)),
        CENTER_X,
        CENTER_Y,
        ColorRole.SECONDARY_CONTAINER.resolve(),
        "the per-instance override is what actually paints, not just what the accessor reports");
  }

  @Test
  void anOverrideGivesAGhostChipARestingFill() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.GHOST).setSurfaceRole(ColorRole.SECONDARY_CONTAINER)),
        CENTER_X,
        CENTER_Y,
        ColorRole.SECONDARY_CONTAINER.resolve(),
        "GHOST's missing fill is a default the override replaces");
  }

  // ------------------------------------------------------------------ border

  @Test
  void eachVariantStrokesItsOwnBorderRole() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED).setBorderWidth(THICK_BORDER)),
        BORDER_X,
        CENTER_Y,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "FILLED strokes the softer OUTLINE_VARIANT");
    Pixels.assertPixelNear(
        render(chip(ChipVariant.OUTLINED).setBorderWidth(THICK_BORDER)),
        BORDER_X,
        CENTER_Y,
        ColorRole.OUTLINE.resolve(),
        "OUTLINED strokes the full-strength OUTLINE");
  }

  @Test
  void aZeroBorderWidthSuppressesTheStroke() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.OUTLINED).setBorderWidth(0)),
        BORDER_X,
        CENTER_Y,
        ColorRole.SURFACE.resolve(),
        "with no width there is no stroke, and the fill reaches the edge");
  }

  // --------------------------------------------------------------- selection

  @Test
  void aSelectedChipTakesTheSelectedStateLayer() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.OUTLINED).setSelected(true)),
        CENTER_X,
        CENTER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.SELECTED.opacity()),
        "selection composites the 12% layer tinted by the surface's own on-pair");
  }

  @Test
  void aSelectedFilledChipTintsWithItsOwnOnPair() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED).setSelected(true)),
        CENTER_X,
        CENTER_Y,
        Pixels.mix(
            ColorRole.PRIMARY_CONTAINER.resolve(),
            ColorRole.ON_PRIMARY_CONTAINER.resolve(),
            StateLayer.SELECTED.opacity()),
        "the tint follows the effective surface role, so the layer stays legible on any variant");
  }

  @Test
  void aSelectedChipSwapsItsBorderToPrimary() {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.OUTLINED).setSelected(true).setBorderWidth(THICK_BORDER)),
        BORDER_X,
        CENTER_Y,
        ColorRole.PRIMARY.resolve(),
        "a selected chip reads as the picked one from its border, not the 12% fill alone");
  }

  @Test
  void ghostIgnoresSelectionEntirely() {
    final BufferedImage image =
        render(chip(ChipVariant.GHOST).setSelected(true).setBorderWidth(THICK_BORDER));

    Pixels.assertPixelNear(
        image,
        CENTER_X,
        CENTER_Y,
        GROUND,
        "GHOST is Elwha's text-button equivalent and M3 defines no selected state at that emphasis"
            + " level");
    Pixels.assertPixelNear(
        image, BORDER_X, CENTER_Y, GROUND, "so it gains no selected border either");
  }

  @Test
  void aGhostChipStillRemembersItsSelectedState() {
    final ElwhaChip chip = chip(ChipVariant.GHOST).setSelected(true);

    assertThat(chip.isSelected())
        .as("the state is held and reported normally — only the rendering opts out")
        .isTrue();

    chip.setVariant(ChipVariant.OUTLINED);

    Pixels.assertPixelNear(
        render(chip),
        CENTER_X,
        CENTER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.SELECTED.opacity()),
        "so switching to a variant that does render selection resumes it");
  }

  @Test
  void deselectingReturnsToTheRestingSurface() {
    final ElwhaChip chip = chip(ChipVariant.OUTLINED).setSelected(true);

    chip.setSelected(false);

    Pixels.assertPixelNear(
        render(chip),
        CENTER_X,
        CENTER_Y,
        ColorRole.SURFACE.resolve(),
        "the selected layer is a paint-time branch, not a latched fill");
  }

  // ------------------------------------------------------------------- shape

  @Test
  void shapeStepCutsTheCorner() {
    // Borderless, so the corner probe reads the fill rather than the 1px stroke over it.
    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED).setShape(ShapeScale.NONE).setBorderWidth(0)),
        0,
        0,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "NONE is square, so the fill reaches the very corner");
    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED).setShape(ShapeScale.SM).setBorderWidth(0)),
        0,
        0,
        GROUND,
        "and the SM chip default cuts it away");
  }

  @ParameterizedTest
  @EnumSource(ShapeScale.class)
  void everyShapeStepStillFillsTheBodyCentre(final ShapeScale shape) {
    Pixels.assertPixelNear(
        render(chip(ChipVariant.FILLED).setShape(shape)),
        CENTER_X,
        CENTER_Y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        shape + " rounds the corners without eating the body");
  }

  // ---------------------------------------------------------------- disabled

  @Test
  void aDisabledChipCompositesItsWholeSurface() {
    final ElwhaChip chip = chip(ChipVariant.FILLED);
    chip.setEnabled(false);

    Pixels.assertPixelNear(
        render(chip),
        CENTER_X,
        CENTER_Y,
        Pixels.mix(
            GROUND, ColorRole.PRIMARY_CONTAINER.resolve(), StateLayer.disabledContainerOpacity()),
        "M3 disabled is a compositing pass over the resolved surface, not a grey tint painted on"
            + " top");
  }

  @Test
  void aDisabledChipDimsItsBorderTheSameWay() {
    final ElwhaChip chip = chip(ChipVariant.OUTLINED).setBorderWidth(THICK_BORDER);
    chip.setEnabled(false);

    // The composite applies per drawing operation, so the border lands on the already-dimmed fill
    // rather than straight onto the ground.
    final Color dimmedFill =
        Pixels.mix(GROUND, ColorRole.SURFACE.resolve(), StateLayer.disabledContainerOpacity());

    Pixels.assertPixelNear(
        render(chip),
        BORDER_X,
        CENTER_Y,
        Pixels.mix(dimmedFill, ColorRole.OUTLINE.resolve(), StateLayer.disabledContainerOpacity()),
        "the border goes through the same pass, so the whole chip fades as one");
  }

  @Test
  void reEnablingRestoresTheOpaqueSurface() {
    final ElwhaChip chip = chip(ChipVariant.FILLED);
    chip.setEnabled(false);
    chip.setEnabled(true);

    Pixels.assertPixelNear(
        render(chip),
        CENTER_X,
        CENTER_Y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "the disabled treatment is a paint-time branch, not a latched state");
  }

  // ---------------------------------------------------------- disabled content

  @Test
  void aDisabledChipCarriesItsContentDownToTheDisabledOpacity() {
    final ElwhaChip chip = chip(ChipVariant.FILLED);
    final Color enabled = chip.resolveForegroundColor();

    assertThat(enabled.getAlpha()).as("an enabled chip's content is opaque").isEqualTo(255);

    chip.setEnabled(false);
    final Color disabled = chip.resolveForegroundColor();

    assertThat(disabled.getRGB() & 0xFFFFFF)
        .as("the role resolution is unchanged — only the alpha carries the dimming")
        .isEqualTo(enabled.getRGB() & 0xFFFFFF);
    assertThat(disabled.getAlpha())
        .as(
            "text and icons paint from paintChildren, after paintComponent's disabled composite has"
                + " been disposed, so the 38% has to ride on the color itself or the content stays"
                + " at full contrast on a ghosted container")
        .isEqualTo(Math.round(StateLayer.disabledContentOpacity() * 255f));

    chip.setEnabled(true);
    assertThat(chip.resolveForegroundColor().getAlpha())
        .as("and it is a resolution-time branch, not a latched color")
        .isEqualTo(255);
  }

  // ----------------------------------------------------------------- re-skin

  @Test
  void aModeSwitchReskinsOnTheNextPaint() {
    final ElwhaChip chip = chip(ChipVariant.FILLED);
    final int light = render(chip).getRGB(CENTER_X, CENTER_Y);

    ThemeExtension.install(Mode.DARK);

    Pixels.assertPixelNear(
        render(chip),
        CENTER_X,
        CENTER_Y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "the same instance re-resolves its role on the next paint with no listener wiring");
    assertThat(render(chip).getRGB(CENTER_X, CENTER_Y))
        .as("and the two schemes really do differ, so the probe above proves something")
        .isNotEqualTo(light);
  }
}
