package com.owspfm.elwha.surface;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShadowBearing;
import com.owspfm.elwha.theme.ShadowPainter;
import java.awt.Color;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.UIResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the elevation half of {@link ElwhaSurface} — the level clamp, the shadow-halo
 * reserve that {@code getInsets()} carries, and the {@link ShadowBearing} contract a placement
 * helper depends on (a fresh, un-aliased {@link Insets} every call). Oracle: {@code
 * docs/development/component-api-conventions.md} §8 and the {@code ShadowBearing} javadoc.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSurfaceElevationTest {

  private static final int WIDTH = 120;
  private static final int HEIGHT = 80;

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5})
  void everySupportedLevelRoundTrips(final int level) {
    assertThat(new ElwhaSurface().setElevation(level).getElevation())
        .as("level %d is inside the M3 range and is stored as given", level)
        .isEqualTo(level);
  }

  @Test
  void levelsClampToTheSupportedRange() {
    assertThat(new ElwhaSurface().setElevation(-4).getElevation())
        .as("a negative level clamps to flat rather than inverting the shadow")
        .isZero();
    assertThat(new ElwhaSurface().setElevation(99).getElevation())
        .as("levels above the M3 ladder clamp to the top of it")
        .isEqualTo(ElwhaSurface.MAX_ELEVATION);
  }

  @Test
  void ceilingMatchesTheSharedPainterCeiling() {
    assertThat(ElwhaSurface.MAX_ELEVATION)
        .as("the surface and the shared shadow painter must agree on where the ladder stops")
        .isEqualTo(ShadowPainter.MAX_ELEVATION);
  }

  // ----------------------------------------------------------- halo reserve

  @Test
  void aFlatSurfaceReservesNothing() {
    assertThat(new ElwhaSurface().getInsets())
        .as("no shadow means no halo to pad for")
        .isEqualTo(new Insets(0, 0, 0, 0));
    assertThat(new ElwhaSurface().getShadowInsets())
        .as("a flat primitive reports a zero-width halo to placement helpers")
        .isEqualTo(new Insets(0, 0, 0, 0));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void anElevatedSurfaceReservesTheSharedPaintersHalo(final int level) {
    final ElwhaSurface surface = new ElwhaSurface().setElevation(level);

    assertThat(surface.getInsets())
        .as("the chassis reserve at level %d is the painter's own extent, not a guess", level)
        .isEqualTo(ShadowPainter.shadowInsets(level));
    assertThat(surface.getShadowInsets())
        .as("ShadowBearing exposes the same reserve the chassis lays out inside")
        .isEqualTo(surface.getInsets());
  }

  @Test
  void haloGrowsWithTheLevel() {
    assertThat(new ElwhaSurface().setElevation(5).getShadowInsets().bottom)
        .as("a higher elevation drops a longer shadow and needs more room for it")
        .isGreaterThan(new ElwhaSurface().setElevation(1).getShadowInsets().bottom);
  }

  @Test
  void dropIsDirectionalSoTheBottomReserveLeadsTheTop() {
    final Insets reserve = new ElwhaSurface().setElevation(3).getShadowInsets();

    assertThat(reserve.bottom)
        .as("M3 shadows fall downward, so the bottom edge reserves more than the top")
        .isGreaterThan(reserve.top);
  }

  @Test
  void reportedHaloIsAFreshCopyCallersCannotMutate() {
    final ElwhaSurface surface = new ElwhaSurface().setElevation(3);
    final Insets first = surface.getShadowInsets();

    first.left = 999;

    assertThat(surface.getShadowInsets().left)
        .as("a placement consumer holding the reserve must not be able to move the geometry")
        .isNotEqualTo(999);
    assertThat(surface.getShadowInsets())
        .as("each call hands out its own instance")
        .isNotSameAs(surface.getShadowInsets());
  }

  @Test
  void aSurfaceIsShadowBearingSoOneAnchorServesEveryElevatedPrimitive() {
    assertThat(new ElwhaSurface())
        .as("placement helpers ask for the halo through the interface, not a concrete type")
        .isInstanceOf(ShadowBearing.class);
  }

  // ---------------------------------------------------------------- paint

  @Test
  void visibleBodyIsPaintedInsideTheReserve() {
    final ElwhaSurface surface = new ElwhaSurface().setElevation(3);
    final Insets reserve = surface.getShadowInsets();

    final java.awt.image.BufferedImage image = Pixels.render(surface, WIDTH, HEIGHT, Color.MAGENTA);

    Pixels.assertPixelNear(
        image,
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the body still fills the middle once the halo is reserved");
    Pixels.assertPixelNear(
        image,
        reserve.left + 4,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the body starts just inside the reserved left edge");
  }

  // ------------------------------------------------- both getInsets overloads

  @Test
  void theReuseBufferOverloadReportsTheSameReserve() {
    final ElwhaSurface surface = new ElwhaSurface().setElevation(4);
    final Insets buffer = new Insets(0, 0, 0, 0);

    final Insets filled = surface.getInsets(buffer);

    assertThat(filled)
        .as("a layout manager using the allocation-free overload must not see zero insets")
        .isSameAs(buffer)
        .isEqualTo(surface.getInsets());
  }

  @Test
  void theReuseBufferOverloadAllocatesWhenGivenNoBuffer() {
    final ElwhaSurface surface = new ElwhaSurface().setElevation(2);

    assertThat(surface.getInsets(null))
        .as("the null-buffer contract JComponent documents still holds")
        .isEqualTo(surface.getInsets());
  }

  @Test
  void aConsumerBorderIsRefusedRatherThanLeftToPaintWithNoSpace() {
    final ElwhaSurface surface = new ElwhaSurface().setElevation(1);
    final Insets reserve = surface.getInsets();

    surface.setBorder(BorderFactory.createLineBorder(Color.RED, 8));

    assertThat(surface.getBorder())
        .as("the chassis insets are the shadow reserve, so a Border has nowhere to live")
        .isNull();
    assertThat(surface.getInsets())
        .as("and the reserve every placement engine backs out is unchanged")
        .isEqualTo(reserve);
  }

  @Test
  void theLookAndFeelsOwnBorderPlumbingStillWorks() {
    final ElwhaSurface surface = new ElwhaSurface();
    final Border installed = new LineBorderUiResource();

    surface.setBorder(installed);

    assertThat(surface.getBorder())
        .as("LookAndFeel.installBorder passes a UIResource; refusing it would fight the LAF")
        .isSameAs(installed);
  }

  /** What {@code LookAndFeel.installBorder} hands a component — LAF-owned, not consumer-owned. */
  private static final class LineBorderUiResource extends LineBorder implements UIResource {
    LineBorderUiResource() {
      super(Color.BLUE, 1);
    }
  }

  @Test
  void shadowPaintsIntoTheReserveThatAFlatSurfaceLeavesClear() {
    final ElwhaSurface elevated = new ElwhaSurface().setElevation(5);
    final int probeY = HEIGHT / 2;

    final int haloPixel = Pixels.render(elevated, WIDTH, HEIGHT, Color.MAGENTA).getRGB(1, probeY);
    final int flatPixel =
        Pixels.render(new ElwhaSurface(), WIDTH, HEIGHT, Color.MAGENTA).getRGB(1, probeY);

    assertThat(haloPixel)
        .as("an elevated surface tints the reserved halo; a flat one leaves the ground alone")
        .isNotEqualTo(flatPixel);
  }
}
