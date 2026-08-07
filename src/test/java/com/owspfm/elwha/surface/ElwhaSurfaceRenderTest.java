package com.owspfm.elwha.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of what an {@link ElwhaSurface} actually paints — the locked defaults, each of
 * the four typed setters, the corner geometry the shape step drives, and the paint-time
 * re-resolution that re-skins it with no listener wiring. Oracle: {@code
 * docs/research/elwha-surface-design.md} §2–§5.
 *
 * <p>Every probe renders onto a magenta ground, a color no bundled palette carries, so "the surface
 * did not paint here" is unambiguous.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSurfaceRenderTest {

  private static final int WIDTH = 100;
  private static final int HEIGHT = 60;
  private static final Color GROUND = Color.MAGENTA;

  private static BufferedImage render(final ElwhaSurface surface) {
    return Pixels.render(surface, WIDTH, HEIGHT, GROUND);
  }

  // --------------------------------------------------------------- defaults

  @Test
  void aFreshSurfaceCarriesTheLockedDefaults() {
    final ElwhaSurface surface = new ElwhaSurface();

    assertThat(surface.getSurfaceRole())
        .as("the workhorse background role is the default fill")
        .isEqualTo(ColorRole.SURFACE);
    assertThat(surface.getShape())
        .as("MD matches the locked ElwhaCard default so a card composing one needs no setup")
        .isEqualTo(ShapeScale.MD);
    assertThat(surface.getBorderRole()).as("no border by default").isNull();
    assertThat(surface.getBorderWidth())
        .as("the standard M3 outline width is ready even while the border is off")
        .isEqualTo(1);
    assertThat(surface.getElevation()).as("v1 surfaces ship flat").isZero();
    assertThat(surface.getClipChildrenToCorners())
        .as("the offscreen child clip is opt-in — it costs an allocation per paint")
        .isFalse();
    assertThat(surface.isOpaque())
        .as("an opaque panel would have Swing pre-fill square corners under the round-rect")
        .isFalse();
  }

  @Test
  void defaultLookIsASurfaceFilledRoundRectWithNoBorder() {
    final BufferedImage image = render(new ElwhaSurface());

    Pixels.assertPixelNear(
        image, WIDTH / 2, HEIGHT / 2, ColorRole.SURFACE.resolve(), "the body fills with SURFACE");
    Pixels.assertPixelNear(
        image, 0, HEIGHT / 2, ColorRole.SURFACE.resolve(), "with no border the edge is still fill");
    Pixels.assertPixelNear(image, 0, 0, GROUND, "the MD corner is cut away from the square bounds");
  }

  // ---------------------------------------------------------- typed setters

  @Test
  void settersAreFluentAndReturnTheSurface() {
    final ElwhaSurface surface = new ElwhaSurface();

    final ElwhaSurface chained =
        surface
            .setSurfaceRole(ColorRole.PRIMARY_CONTAINER)
            .setShape(ShapeScale.LG)
            .setBorderRole(ColorRole.OUTLINE)
            .setBorderWidth(2)
            .setElevation(1)
            .setClipChildrenToCorners(true);

    assertThat(chained)
        .as("every setter returns the surface itself for inline configuration")
        .isSameAs(surface);
  }

  @Test
  void surfaceRoleDrivesTheFill() {
    final BufferedImage image =
        render(new ElwhaSurface().setSurfaceRole(ColorRole.PRIMARY_CONTAINER));

    Pixels.assertPixelNear(
        image,
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "the fill follows the role, resolved at paint time");
  }

  @Test
  void borderRoleDrivesTheStrokeAndNullSuppressesIt() {
    final BufferedImage outlined =
        render(new ElwhaSurface().setBorderRole(ColorRole.ERROR).setBorderWidth(4));

    Pixels.assertPixelNear(
        outlined, 1, HEIGHT / 2, ColorRole.ERROR.resolve(), "the stroke paints in the border role");
    Pixels.assertPixelNear(
        outlined,
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the border is an edge treatment and leaves the fill alone");

    final ElwhaSurface cleared =
        new ElwhaSurface().setBorderRole(ColorRole.ERROR).setBorderWidth(4).setBorderRole(null);

    Pixels.assertPixelNear(
        render(cleared),
        1,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "setting the role back to null turns the border off again");
  }

  @Test
  void aZeroOrNegativeBorderWidthSuppressesTheStroke() {
    final ElwhaSurface surface =
        new ElwhaSurface().setBorderRole(ColorRole.ERROR).setBorderWidth(0);

    Pixels.assertPixelNear(
        render(surface),
        1,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "a zero width suppresses the border exactly as a null role does");
    assertThat(surface.setBorderWidth(-3).getBorderWidth())
        .as("the width is stored as given — suppression is a paint-time decision")
        .isEqualTo(-3);
  }

  @Test
  void shapeStepDrivesTheCornerCut() {
    Pixels.assertPixelNear(
        render(new ElwhaSurface().setShape(ShapeScale.NONE)),
        0,
        0,
        ColorRole.SURFACE.resolve(),
        "NONE is square, so the fill reaches the very corner");
    Pixels.assertPixelNear(
        render(new ElwhaSurface().setShape(ShapeScale.XL)),
        0,
        0,
        GROUND,
        "a larger step cuts more of the corner away");
  }

  @ParameterizedTest
  @EnumSource(ShapeScale.class)
  void everyShapeStepStillFillsTheBodyCentre(final ShapeScale shape) {
    Pixels.assertPixelNear(
        render(new ElwhaSurface().setShape(shape)),
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        shape + " rounds the corners without eating the body");
  }

  @Test
  void anOversizedShapeStepDegradesToACapsuleRatherThanBreaking() {
    final BufferedImage image = render(new ElwhaSurface().setShape(ShapeScale.FULL));

    Pixels.assertPixelNear(
        image,
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "FULL's 9999 px arc clamps to the body rather than painting nothing");
    Pixels.assertPixelNear(image, 0, 0, GROUND, "the clamped capsule still cuts the corner");
  }

  @Test
  void typedSettersRejectNullTokens() {
    final ElwhaSurface surface = new ElwhaSurface();

    assertThatThrownBy(() -> surface.setSurfaceRole(null))
        .as("a Surface always paints a fill — there is no transparent variant here")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> surface.setShape(null))
        .as("a null shape has no corner geometry to paint")
        .isInstanceOf(NullPointerException.class);
  }

  // --------------------------------------------------------------- re-skin

  @Test
  void aModeSwitchReskinsOnTheNextPaintWithNoListenerWiring() {
    final ElwhaSurface surface = new ElwhaSurface();
    Pixels.assertPixelNear(
        render(surface),
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the light scheme paints first");

    ThemeExtension.install(Mode.DARK);

    Pixels.assertPixelNear(
        render(surface),
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the same instance re-resolves its token on the next paint — no per-instance listener");
  }

  @Test
  void reskinActuallyChangesThePaintedColor() {
    final ElwhaSurface surface = new ElwhaSurface();
    final int lightCentre = render(surface).getRGB(WIDTH / 2, HEIGHT / 2);

    ThemeExtension.install(Mode.DARK);

    assertThat(render(surface).getRGB(WIDTH / 2, HEIGHT / 2))
        .as("a re-skin that resolved to the same color would prove nothing")
        .isNotEqualTo(lightCentre);
  }

  @Test
  void aThemeSwitchReskinsToo() {
    final ElwhaSurface surface = new ElwhaSurface().setSurfaceRole(ColorRole.PRIMARY);
    final int baselinePrimary = render(surface).getRGB(WIDTH / 2, HEIGHT / 2);

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.secondary().get(0)).mode(Mode.LIGHT).build());

    assertThat(render(surface).getRGB(WIDTH / 2, HEIGHT / 2))
        .as("swapping the palette moves the fill just as swapping the mode does")
        .isNotEqualTo(baselinePrimary);
  }
}
