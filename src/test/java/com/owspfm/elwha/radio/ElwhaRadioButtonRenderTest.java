package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the ring-and-dot chrome — the per-state ring roles, the dot that only exists
 * while selected, the state layers including M3's swapped pressed tint, and the disabled treatment
 * that (unlike the switch) is symmetric across both sides. Oracle: research §T/§C′ as quoted in the
 * class Javadoc.
 *
 * <p>Probes sit on the ring band and at the icon centre — both plain filled geometry, so a probe is
 * reading chrome rather than an anti-aliased stroke edge.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioButtonRenderTest {

  private static final int SIDE = 48;
  private static final int CENTER = 24;

  /** A ground no bundled palette carries, so "nothing painted here" is unambiguous. */
  private static final Color GROUND = Color.MAGENTA;

  /** On the ring band: 9px left of centre, between the 8px hole and the 10px outer edge. */
  private static final int RING_X = CENTER - 9;

  /** Inside the 40px state-layer circle, clear of the 20px icon. */
  private static final int LAYER_Y = CENTER - 15;

  private static BufferedImage render(final ElwhaRadioButton radio) {
    return Pixels.render(radio, SIDE, SIDE, GROUND);
  }

  // ------------------------------------------------------------------- ring

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void anIdleUnselectedRingResolvesOnSurfaceVariant(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(new ElwhaRadioButton()),
        RING_X,
        CENTER,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "an idle unselected ring is ON_SURFACE_VARIANT in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aSelectedRingResolvesPrimary(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(new ElwhaRadioButton(true)),
        RING_X,
        CENTER,
        ColorRole.PRIMARY.resolve(),
        "a selected ring is PRIMARY in " + mode);
  }

  @Test
  void interactionShiftsTheUnselectedRingToOnSurface() {
    final ElwhaRadioButton hovered = new ElwhaRadioButton();
    hovered.setHovered(true);
    Pixels.assertPixelNear(
        render(hovered),
        RING_X,
        CENTER,
        ColorRole.ON_SURFACE.resolve(),
        "hover lifts the unselected ring to the full-contrast ON_SURFACE");

    final ElwhaRadioButton pressed = new ElwhaRadioButton();
    pressed.setPressed(true);
    Pixels.assertPixelNear(
        render(pressed), RING_X, CENTER, ColorRole.ON_SURFACE.resolve(), "and so does a press");
  }

  @Test
  void aSelectedRingStaysPrimaryUnderInteraction() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    radio.setHovered(true);

    Pixels.assertPixelNear(
        render(radio),
        RING_X,
        CENTER,
        ColorRole.PRIMARY.resolve(),
        "the selected side has no interaction shift — PRIMARY in every interactive state");
  }

  // -------------------------------------------------------------------- dot

  @Test
  void onlyASelectedRadioPaintsTheDot() {
    Pixels.assertPixelNear(
        render(new ElwhaRadioButton(true)),
        CENTER,
        CENTER,
        ColorRole.PRIMARY.resolve(),
        "a selected radio fills its inner dot in PRIMARY");
    Pixels.assertPixelNear(
        render(new ElwhaRadioButton()),
        CENTER,
        CENTER,
        GROUND,
        "an unselected radio leaves the ring's hole empty");
  }

  @Test
  void selectingAndDeselectingAddsAndRemovesTheDot() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();

    radio.setSelected(true);
    Pixels.assertPixelNear(
        render(radio), CENTER, CENTER, ColorRole.PRIMARY.resolve(), "selecting grows the dot in");

    radio.setSelected(false);
    Pixels.assertPixelNear(
        render(radio), CENTER, CENTER, GROUND, "and deselecting fades it back out to nothing");
  }

  // ------------------------------------------------------------ state layers

  @Test
  void hoverAndFocusTintWithTheCurrentStatesColor() {
    final ElwhaRadioButton unselected = new ElwhaRadioButton();
    unselected.setHovered(true);
    Pixels.assertPixelNear(
        render(unselected),
        CENTER,
        LAYER_Y,
        Pixels.mix(GROUND, ColorRole.ON_SURFACE.resolve(), StateLayer.HOVER.opacity()),
        "an unselected hover layer tints ON_SURFACE — the colour the radio currently is");

    final ElwhaRadioButton selected = new ElwhaRadioButton(true);
    selected.setHovered(true);
    Pixels.assertPixelNear(
        render(selected),
        CENTER,
        LAYER_Y,
        Pixels.mix(GROUND, ColorRole.PRIMARY.resolve(), StateLayer.HOVER.opacity()),
        "and a selected one tints PRIMARY");
  }

  @Test
  void pressTintsWithTheSwappedColor() {
    final ElwhaRadioButton unselected = new ElwhaRadioButton();
    unselected.setPressed(true);
    Pixels.assertPixelNear(
        render(unselected),
        CENTER,
        LAYER_Y,
        Pixels.mix(GROUND, ColorRole.PRIMARY.resolve(), StateLayer.PRESSED.opacity()),
        "pressing an unselected radio shows PRIMARY — the press anticipates selection");

    final ElwhaRadioButton selected = new ElwhaRadioButton(true);
    selected.setPressed(true);
    Pixels.assertPixelNear(
        render(selected),
        CENTER,
        LAYER_Y,
        Pixels.mix(GROUND, ColorRole.ON_SURFACE.resolve(), StateLayer.PRESSED.opacity()),
        "and pressing a selected one shows ON_SURFACE — the same swap in reverse");
  }

  @Test
  void pressOutranksHover() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setHovered(true);
    radio.setPressed(true);

    Pixels.assertPixelNear(
        render(radio),
        CENTER,
        LAYER_Y,
        Pixels.mix(GROUND, ColorRole.PRIMARY.resolve(), StateLayer.PRESSED.opacity()),
        "one layer paints at a time and pressed wins — the ElwhaCheckbox priority");
  }

  @Test
  void anIdleRadioPaintsNoStateLayer() {
    Pixels.assertPixelNear(
        render(new ElwhaRadioButton()),
        CENTER,
        LAYER_Y,
        GROUND,
        "with no interaction the state-layer circle is not painted at all");
  }

  // ---------------------------------------------------------------- disabled

  @Test
  void disabledDimsBothSidesSymmetrically() {
    final Color dimmed =
        Pixels.mix(GROUND, ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());

    final ElwhaRadioButton unselected = new ElwhaRadioButton();
    unselected.setEnabled(false);
    Pixels.assertPixelNear(
        render(unselected),
        RING_X,
        CENTER,
        dimmed,
        "a disabled unselected ring is ON_SURFACE at the 0.38 content opacity");

    final ElwhaRadioButton selected = new ElwhaRadioButton(true);
    selected.setEnabled(false);
    final BufferedImage image = render(selected);
    Pixels.assertPixelNear(
        image,
        RING_X,
        CENTER,
        dimmed,
        "the disabled selected ring uses the same treatment — unlike the switch, this side is"
            + " symmetric");
    Pixels.assertPixelNear(image, CENTER, CENTER, dimmed, "and so does its dot");
  }

  @Test
  void disabledSuppressesTheStateLayerEvenWhenForced() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setEnabled(false);
    radio.setHovered(true);
    radio.setPressed(true);

    Pixels.assertPixelNear(
        render(radio),
        CENTER,
        LAYER_Y,
        GROUND,
        "a disabled radio paints no state layer whatever the interaction flags say");
  }

  // ----------------------------------------------------------------- re-skin

  @Test
  void aModeSwitchReskinsOnTheNextPaint() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    final int light = render(radio).getRGB(CENTER, CENTER);

    ThemeExtension.install(Mode.DARK);

    Pixels.assertPixelNear(
        render(radio),
        CENTER,
        CENTER,
        ColorRole.PRIMARY.resolve(),
        "the same instance re-resolves PRIMARY on the next paint with no listener wiring");
    assertThat(render(radio).getRGB(CENTER, CENTER))
        .as("and the two schemes really do differ, so the probe above proves something")
        .isNotEqualTo(light);
  }
}
