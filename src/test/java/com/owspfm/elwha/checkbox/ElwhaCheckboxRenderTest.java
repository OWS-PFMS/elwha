package com.owspfm.elwha.checkbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the painted checkbox chrome — the container's outline/fill swap, the two
 * hand-stroked marks, the state layers including M3's cross-color pressed rule, and the error and
 * disabled treatments. Oracle: {@code docs/research/elwha-checkbox-design.md} §5–§6.
 *
 * <p>The marks are stroked geometry, not glyphs, so they are probed as chrome: a small disc scan at
 * a point only one of the two marks can reach. The checkmark drops below the container's centre
 * line at its elbow and rises above it at the long arm's tip; the indeterminate dash is confined to
 * the centre row. Asserting presence at one and absence at the other tells the two apart without
 * pinning an eye-tuned stroke pixel.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCheckboxRenderTest {

  private static final int SIDE = 48;
  private static final int CENTER = 24;

  /** A ground no bundled palette carries, for the probes that assert a mark is absent. */
  private static final java.awt.Color GROUND = java.awt.Color.MAGENTA;

  /** On the left outline stroke, mid-edge — clear of both rounded corners. */
  private static final int OUTLINE_X = 16;

  /** Inside the container, clear of a rounded corner and of either mark. */
  private static final int FILL_X = 18;

  private static final int FILL_Y = 17;

  /** Inside the 40px state-layer circle but above the 18px container. */
  private static final int LAYER_Y = 9;

  /** The checkmark's elbow — below the centre row, where the dash never reaches. */
  private static final int ELBOW_X = 22;

  private static final int ELBOW_Y = 27;

  /** The checkmark's long-arm tip — above the centre row. */
  private static final int ARM_X = 29;

  private static final int ARM_Y = 20;

  /** The dash's right end, on the centre row. */
  private static final int DASH_X = 28;

  private static BufferedImage render(final ElwhaCheckbox box) {
    return Pixels.render(box, SIDE, SIDE);
  }

  private static ElwhaCheckbox box(final CheckState state) {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(state);
    return box;
  }

  // ------------------------------------------------------------- container

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void restingOutlineResolvesOnSurfaceVariant(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(new ElwhaCheckbox()),
        OUTLINE_X,
        CENTER,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "an idle unchecked container is outlined in ON_SURFACE_VARIANT in " + mode);
  }

  @Test
  void anUncheckedContainerIsNotFilled() {
    Pixels.assertPixelNear(
        render(new ElwhaCheckbox()),
        CENTER,
        CENTER,
        ColorRole.SURFACE.resolve(),
        "the unchecked container is an outline only — its interior stays the surface behind it");
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void checkedContainerFillsWithPrimary(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(box(CheckState.CHECKED)),
        FILL_X,
        FILL_Y,
        ColorRole.PRIMARY.resolve(),
        "a checked container fills with PRIMARY in " + mode);
  }

  @Test
  void indeterminateSharesTheSelectedFill() {
    Pixels.assertPixelNear(
        render(box(CheckState.INDETERMINATE)),
        FILL_X,
        FILL_Y,
        ColorRole.PRIMARY.resolve(),
        "indeterminate colors identically to checked — both are the selected family");
  }

  @Test
  void interactionBrightensTheUncheckedOutline() {
    final ElwhaCheckbox hovered = new ElwhaCheckbox();
    hovered.setHovered(true);

    Pixels.assertPixelNear(
        render(hovered),
        OUTLINE_X,
        CENTER,
        ColorRole.ON_SURFACE.resolve(),
        "hover lifts the outline from ON_SURFACE_VARIANT to the full-contrast ON_SURFACE");
  }

  // ------------------------------------------------------------------ marks

  @Test
  void checkedPaintsTheCheckmarkInOnPrimary() {
    final BufferedImage image = render(box(CheckState.CHECKED));

    Pixels.assertAnyPixelNear(
        image,
        ELBOW_X,
        ELBOW_Y,
        2,
        ColorRole.ON_PRIMARY.resolve(),
        "the checkmark's elbow strokes in ON_PRIMARY below the container's centre row");
    Pixels.assertAnyPixelNear(
        image,
        ARM_X,
        ARM_Y,
        2,
        ColorRole.ON_PRIMARY.resolve(),
        "and its long arm rises above the centre row");
  }

  @Test
  void indeterminatePaintsTheDashAndNoCheckmark() {
    final BufferedImage image = render(box(CheckState.INDETERMINATE));

    Pixels.assertAnyPixelNear(
        image,
        DASH_X,
        CENTER,
        1,
        ColorRole.ON_PRIMARY.resolve(),
        "the indeterminate dash strokes in ON_PRIMARY along the centre row");
    Pixels.assertNoPixelNear(
        image,
        ELBOW_X,
        ELBOW_Y,
        2,
        ColorRole.ON_PRIMARY.resolve(),
        "and nothing is drawn below the centre row — that region belongs to the checkmark alone");
    Pixels.assertNoPixelNear(
        image, ARM_X, ARM_Y, 2, ColorRole.ON_PRIMARY.resolve(), "nor above it");
  }

  @Test
  void anUncheckedContainerCarriesNoMark() {
    // On a magenta ground, not the usual SURFACE one: baseline-light SURFACE is #FDF7FF and
    // ON_PRIMARY is #FFFFFF, close enough that "no mark here" would pass against bare surface
    // whether or not a mark had been painted.
    final BufferedImage image = Pixels.render(new ElwhaCheckbox(), SIDE, SIDE, GROUND);

    Pixels.assertNoPixelNear(
        image,
        ELBOW_X,
        ELBOW_Y,
        3,
        ColorRole.ON_PRIMARY.resolve(),
        "an unchecked box paints no mark at all");
    Pixels.assertNoPixelNear(
        image, DASH_X, CENTER, 3, ColorRole.ON_PRIMARY.resolve(), "including no dash");
  }

  @Test
  void switchingBetweenTheTwoMarksRedrawsTheGlyph() {
    final ElwhaCheckbox box = box(CheckState.INDETERMINATE);
    box.setCheckState(CheckState.CHECKED);

    final BufferedImage image = render(box);

    Pixels.assertAnyPixelNear(
        image,
        ELBOW_X,
        ELBOW_Y,
        2,
        ColorRole.ON_PRIMARY.resolve(),
        "leaving indeterminate for checked swaps the dash out for the checkmark");
  }

  // ----------------------------------------------------------- state layers

  @Test
  void hoverTintsWithTheCurrentStatesColor() {
    final ElwhaCheckbox unchecked = new ElwhaCheckbox();
    unchecked.setHovered(true);
    Pixels.assertPixelNear(
        render(unchecked),
        CENTER,
        LAYER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.HOVER.opacity()),
        "an unchecked hover layer tints ON_SURFACE — the colour it currently is");

    final ElwhaCheckbox checked = box(CheckState.CHECKED);
    checked.setHovered(true);
    Pixels.assertPixelNear(
        render(checked),
        CENTER,
        LAYER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(), ColorRole.PRIMARY.resolve(), StateLayer.HOVER.opacity()),
        "a checked hover layer tints PRIMARY — again the colour it currently is");
  }

  @Test
  void pressTintsWithTheDestinationStatesColor() {
    final ElwhaCheckbox unchecked = new ElwhaCheckbox();
    unchecked.setPressed(true);
    Pixels.assertPixelNear(
        render(unchecked),
        CENTER,
        LAYER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(), ColorRole.PRIMARY.resolve(), StateLayer.PRESSED.opacity()),
        "pressing an unchecked box previews PRIMARY — the M3 cross-colour rule");

    final ElwhaCheckbox checked = box(CheckState.CHECKED);
    checked.setPressed(true);
    Pixels.assertPixelNear(
        render(checked),
        CENTER,
        LAYER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.PRESSED.opacity()),
        "and pressing a checked box previews ON_SURFACE — the same rule in the other direction");
  }

  @Test
  void pressOutranksHover() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setHovered(true);
    box.setPressed(true);

    Pixels.assertPixelNear(
        render(box),
        CENTER,
        LAYER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(), ColorRole.PRIMARY.resolve(), StateLayer.PRESSED.opacity()),
        "exactly one layer paints at a time and pressed wins over hovered");
  }

  @Test
  void anIdleCheckboxPaintsNoStateLayer() {
    Pixels.assertPixelNear(
        render(new ElwhaCheckbox()),
        CENTER,
        LAYER_Y,
        ColorRole.SURFACE.resolve(),
        "with no interaction the state-layer circle is not painted at all");
  }

  // ------------------------------------------------------------------ error

  @Test
  void errorSwapsTheChromaticRoleWholesale() {
    final ElwhaCheckbox unchecked = new ElwhaCheckbox();
    unchecked.setErrorShown(true);
    Pixels.assertPixelNear(
        render(unchecked),
        OUTLINE_X,
        CENTER,
        ColorRole.ERROR.resolve(),
        "the error outline is ERROR, not the resting ON_SURFACE_VARIANT");

    final ElwhaCheckbox checked = box(CheckState.CHECKED);
    checked.setErrorShown(true);
    final BufferedImage image = render(checked);
    Pixels.assertPixelNear(
        image,
        FILL_X,
        FILL_Y,
        ColorRole.ERROR.resolve(),
        "the error fill is ERROR in place of PRIMARY");
    Pixels.assertAnyPixelNear(
        image, ELBOW_X, ELBOW_Y, 2, ColorRole.ON_ERROR.resolve(), "and the mark pairs to ON_ERROR");
  }

  @Test
  void errorTintsEveryStateLayer() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setErrorShown(true);
    box.setHovered(true);

    Pixels.assertPixelNear(
        render(box),
        CENTER,
        LAYER_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(), ColorRole.ERROR.resolve(), StateLayer.HOVER.opacity()),
        "error overrides the state-layer tint too, in both the hover and press branches");
  }

  // --------------------------------------------------------------- disabled

  @Test
  void disabledDimsTheOutlineToTheContentOpacity() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setEnabled(false);

    Pixels.assertPixelNear(
        render(box),
        OUTLINE_X,
        CENTER,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContentOpacity()),
        "a disabled outline is ON_SURFACE at the 0.38 content opacity");
  }

  @Test
  void disabledSelectedPunchesTheMarkThroughInSurface() {
    final ElwhaCheckbox box = box(CheckState.CHECKED);
    box.setEnabled(false);
    final BufferedImage image = render(box);

    Pixels.assertPixelNear(
        image,
        FILL_X,
        FILL_Y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContentOpacity()),
        "the disabled selected container is ON_SURFACE at 0.38");
    Pixels.assertAnyPixelNear(
        image,
        ELBOW_X,
        ELBOW_Y,
        2,
        ColorRole.SURFACE.resolve(),
        "and the mark is opaque SURFACE punched through it — not ON_SURFACE, not alpha'd");
  }

  @Test
  void disabledSuppressesTheStateLayerEvenWhenForced() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setEnabled(false);
    box.setHovered(true);
    box.setPressed(true);

    Pixels.assertPixelNear(
        render(box),
        CENTER,
        LAYER_Y,
        ColorRole.SURFACE.resolve(),
        "a disabled checkbox paints no state layer whatever the interaction flags say");
  }

  @Test
  void disabledOutranksError() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setErrorShown(true);
    box.setEnabled(false);

    Pixels.assertPixelNear(
        render(box),
        OUTLINE_X,
        CENTER,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContentOpacity()),
        "disabled wins over error — an unavailable control reads unavailable first");
  }

  // ---------------------------------------------------------------- re-skin

  @Test
  void aModeSwitchReskinsOnTheNextPaint() {
    final ElwhaCheckbox box = box(CheckState.CHECKED);
    final int light = render(box).getRGB(FILL_X, FILL_Y);

    ThemeExtension.install(Mode.DARK);

    Pixels.assertPixelNear(
        render(box),
        FILL_X,
        FILL_Y,
        ColorRole.PRIMARY.resolve(),
        "the same instance re-resolves PRIMARY on the next paint with no listener wiring");
    assertThat(render(box).getRGB(FILL_X, FILL_Y))
        .as("and the two schemes really do differ, so the probe above proves something")
        .isNotEqualTo(light);
  }
}
