package com.owspfm.elwha.fab;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.Corners;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A chrome coverage for {@link ElwhaFab} — the six §5 color styles resolved in both theme
 * modes, the §6 state model (state layers plus the hover elevation bump), and the disabled
 * treatment.
 *
 * <p>Every FAB under test carries a glyph that paints nothing, so the probes land on container
 * chrome rather than icon pixels. The focus ring is deliberately out of scope here: {@code
 * focusVisible} is only armed by a real keyboard focus event, and dispatching synthetic {@code
 * FocusEvent}s is banned by the determinism rules.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaFabRenderTest {

  /** A glyph that occupies a slot but paints nothing, keeping the whole body probeable. */
  private static final class BlankIcon implements Icon {
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      // deliberately blank — the determinism rules keep probes off glyph pixels
    }

    @Override
    public int getIconWidth() {
      return 1;
    }

    @Override
    public int getIconHeight() {
      return 1;
    }
  }

  private static Stream<Arguments> colorsInBothModes() {
    return Stream.of(Mode.LIGHT, Mode.DARK)
        .flatMap(mode -> Stream.of(ElwhaFab.Color.values()).map(c -> Arguments.of(c, mode)));
  }

  private static ElwhaFab fab(final ElwhaFab.Color color) {
    return ElwhaFab.standard(new BlankIcon()).setColor(color);
  }

  private static BufferedImage render(final ElwhaFab fab) {
    final Dimension pref = fab.getPreferredSize();
    return Pixels.render(fab, pref.width, pref.height);
  }

  private static Point bodyCenter(final ElwhaFab fab) {
    final Insets reserve = fab.getShadowInsets();
    return new Point(
        reserve.left + fab.getFabSize().containerPx() / 2,
        reserve.top + fab.getFabSize().containerPx() / 2);
  }

  // ------------------------------------------------------------ color axis

  @ParameterizedTest
  @MethodSource("colorsInBothModes")
  void everyColorStylePaintsItsContainerRole(final ElwhaFab.Color color, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaFab fab = fab(color);

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        color.containerRole().resolve(),
        color + " fills with its container role in " + mode);
  }

  @ParameterizedTest
  @EnumSource(ElwhaFab.Color.class)
  void everyColorStylePairsItsOwnContentRole(final ElwhaFab.Color color) {
    assertThat(color.onContainerRole())
        .as("%s tints the icon, the label, and the state layer with one paired role", color)
        .isEqualTo(color.containerRole().on().orElse(ColorRole.ON_SURFACE));
  }

  @Test
  void defaultColorStyleIsPrimaryContainer() {
    assertThat(ElwhaFab.standard(new BlankIcon()).getColor())
        .as("§5 — the M3 color-styles legend defaults both forms to primaryContainer")
        .isEqualTo(ElwhaFab.Color.PRIMARY_CONTAINER);
  }

  // ------------------------------------------------------------ state model

  @ParameterizedTest
  @MethodSource("colorsInBothModes")
  void hoverCompositesTheEightPercentLayerInTheContentRole(
      final ElwhaFab.Color color, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaFab fab = fab(color).setHovered(true);

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        Pixels.mix(
            color.containerRole().resolve(),
            color.onContainerRole().resolve(),
            StateLayer.HOVER.opacity()),
        "§6.4 — " + color + " tints its hover layer with the on-container role in " + mode);
  }

  @ParameterizedTest
  @EnumSource(ElwhaFab.Color.class)
  void pressCompositesTheTenPercentLayerAndWinsOverHover(final ElwhaFab.Color color) {
    final ElwhaFab fab = fab(color).setHovered(true).setPressed(true);

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        Pixels.mix(
            color.containerRole().resolve(),
            color.onContainerRole().resolve(),
            StateLayer.PRESSED.opacity()),
        color + " paints only the pressed layer while both states are live");
  }

  @Test
  void aPressInTheShadowReserveLeavesTheChromeAtRest() {
    final ElwhaFab fab = fab(ElwhaFab.Color.PRIMARY_CONTAINER);
    final Dimension pref = fab.getPreferredSize();
    fab.setSize(pref.width, pref.height);
    final Insets reserve = fab.getShadowInsets();
    assertThat(reserve.left).as("the fixture depends on a non-empty reserve").isPositive();

    Input.press(fab, reserve.left / 2, reserve.top / 2);

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "the release was already gated, so a reserve press never activated — but it still flashed"
            + " the pressed layer from a click that missed the FAB entirely");
  }

  @Test
  void keyboardActivationFlashesThePressedLayerToo() {
    final ElwhaFab fab = fab(ElwhaFab.Color.PRIMARY_CONTAINER);

    Input.pressBoundKey(fab, "pressed SPACE", "elwhafab.activate");

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        Pixels.mix(
            ColorRole.PRIMARY_CONTAINER.resolve(),
            ColorRole.ON_PRIMARY_CONTAINER.resolve(),
            StateLayer.PRESSED.opacity()),
        "a keyboard user gets the same press confirmation a pointer user does");
  }

  @Test
  void hoverBumpsTheElevationSoTheHaloDeepens() {
    final ElwhaFab resting = fab(ElwhaFab.Color.PRIMARY_CONTAINER);
    final ElwhaFab hovered = fab(ElwhaFab.Color.PRIMARY_CONTAINER).setHovered(true);
    final Point probe = haloProbe(resting);

    final int restingHalo = brightness(render(resting), probe.x, probe.y);
    final int hoveredHalo = brightness(render(hovered), probe.x, probe.y);

    assertThat(hoveredHalo)
        .as(
            "§6.1 — hover is the one per-state elevation token M3 publishes, and level 4 casts a"
                + " deeper shadow than the resting level 3")
        .isLessThan(restingHalo);
  }

  @Test
  void haloIsPaintedBelowTheBodyNotOverIt() {
    final ElwhaFab fab = fab(ElwhaFab.Color.PRIMARY_CONTAINER);
    final BufferedImage image = render(fab);

    final int outside = brightness(image, haloProbe(fab).x, haloProbe(fab).y);
    final int ground = brightness(image, 0, 0);

    assertThat(outside).as("the halo darkens the ground just outside the body").isLessThan(ground);
    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "while the opaque container covers it inside the body");
  }

  // ------------------------------------------------------------- disabled

  @ParameterizedTest
  @MethodSource("colorsInBothModes")
  void disabledDropsTheContainerToTwelvePercent(final ElwhaFab.Color color, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaFab fab = fab(color).setHovered(true).setPressed(true);
    fab.setEnabled(false);

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            color.containerRole().resolve(),
            StateLayer.disabledContainerOpacity()),
        color + " freezes into the disabled treatment and drops every state layer in " + mode);
  }

  @Test
  void aDisabledFabCastsNoShadowAtAll() {
    final ElwhaFab fab = fab(ElwhaFab.Color.PRIMARY_CONTAINER);
    fab.setEnabled(false);
    final BufferedImage image = render(fab);

    assertThat(brightness(image, haloProbe(fab).x, haloProbe(fab).y))
        .as("a disabled FAB is not lifted, so the reserve stays clean ground")
        .isEqualTo(brightness(image, 0, 0));
  }

  @Test
  void disablingClearsAnyLiveHoverOrPressState() {
    final ElwhaFab fab = fab(ElwhaFab.Color.PRIMARY_CONTAINER).setHovered(true).setPressed(true);

    fab.setEnabled(false);
    fab.setEnabled(true);

    final Point center = bodyCenter(fab);
    Pixels.assertPixelNear(
        render(fab),
        center.x,
        center.y,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "re-enabling returns a clean resting FAB rather than a stuck hover or press");
  }

  /** A point in the reserve just below the body's bottom edge, where the halo falls. */
  private static Point haloProbe(final ElwhaFab fab) {
    final Insets reserve = fab.getShadowInsets();
    return new Point(
        reserve.left + fab.getFabSize().containerPx() / 2,
        reserve.top + fab.getFabSize().containerPx() + 2);
  }

  private static int brightness(final BufferedImage image, final int x, final int y) {
    final java.awt.Color color = new java.awt.Color(image.getRGB(x, y), true);
    return color.getRed() + color.getGreen() + color.getBlue();
  }

  // ------------------------------------------------------------ corner radius (#654)

  @ParameterizedTest
  @EnumSource(ElwhaFab.Size.class)
  void everySizeRendersAtItsDocumentedM3Radius(final ElwhaFab.Size size) {
    final ElwhaFab fab = fab(ElwhaFab.Color.PRIMARY_CONTAINER).setFabSize(size);
    final Dimension pref = fab.getPreferredSize();
    // A magenta ground, not the usual SURFACE: the corner probe classifies each pixel by which
    // reference color it sits nearer, and a shadowed body over a near-neutral ground leaves the
    // two references too close to separate the halo from the fill.
    final BufferedImage image = Pixels.render(fab, pref.width, pref.height, java.awt.Color.MAGENTA);
    final Insets reserve = fab.getShadowInsets();

    Corners.assertTopLeftRadius(
        image,
        reserve.left,
        reserve.top,
        size.cornerRadiusPx(),
        ElwhaFab.Color.PRIMARY_CONTAINER.containerRole().resolve(),
        // The FAB always carries a halo, so the outside reference is the local shadow rather than
        // the flat ground.
        new java.awt.Color(image.getRGB(reserve.left - 3, reserve.top - 3), true),
        size.cornerRadiusPx(),
        size
            + " paints the M3 dp radius its enum documents; it rendered at half until the arcWidth"
            + " conversion landed");
  }
}
