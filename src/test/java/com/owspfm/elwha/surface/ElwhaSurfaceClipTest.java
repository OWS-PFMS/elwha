package com.owspfm.elwha.surface;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the rounded-corner child clip — the opt-in consumer flag, the separate
 * paint-time resolver a subclass with intrinsic corner-reaching content overrides, and the guard
 * that keeps a childless clip-enabled surface from silently losing its fill. Oracle: the {@link
 * ElwhaSurface#setClipChildrenToCorners(boolean)} and {@code paintChildren} contracts (#157, #272).
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSurfaceClipTest {

  private static final int WIDTH = 100;
  private static final int HEIGHT = 60;
  private static final Color GROUND = Color.MAGENTA;
  private static final Color CHILD = new Color(0x00, 0x80, 0x00);

  /** An opaque child that fills whatever cell it is given, right into the chassis corners. */
  private static JComponent edgeToEdgeChild() {
    final JPanel child =
        new JPanel() {
          @Override
          protected void paintComponent(final Graphics g) {
            g.setColor(CHILD);
            g.fillRect(0, 0, getWidth(), getHeight());
          }
        };
    child.setOpaque(true);
    return child;
  }

  private static ElwhaSurface surfaceWithChild() {
    final ElwhaSurface surface = new ElwhaSurface();
    surface.setLayout(new BorderLayout());
    surface.add(edgeToEdgeChild(), BorderLayout.CENTER);
    return surface;
  }

  private static BufferedImage render(final ElwhaSurface surface) {
    return Pixels.render(surface, WIDTH, HEIGHT, GROUND);
  }

  // ----------------------------------------------------------- the flag

  @Test
  void theClipIsOffByDefault() {
    assertThat(new ElwhaSurface().getClipChildrenToCorners())
        .as(
            "most surfaces inset their children, so the offscreen buffer would only cost paint"
                + " time")
        .isFalse();
  }

  @Test
  void theFlagRoundTripsBothWays() {
    final ElwhaSurface surface = new ElwhaSurface();

    assertThat(surface.setClipChildrenToCorners(true).getClipChildrenToCorners())
        .as("opting in is a plain fluent setter")
        .isTrue();
    assertThat(surface.setClipChildrenToCorners(false).getClipChildrenToCorners())
        .as("opting back out is supported too")
        .isFalse();
  }

  // ---------------------------------------------------------- the effect

  @Test
  void withoutTheClipAnEdgeToEdgeChildOverhangsTheCorner() {
    Pixels.assertPixelNear(
        render(surfaceWithChild()),
        0,
        0,
        CHILD,
        "a square opaque child paints straight over the chassis corner when nothing cuts it");
  }

  @Test
  void withTheClipTheChildConformsToTheChassisCurve() {
    final ElwhaSurface surface = surfaceWithChild().setClipChildrenToCorners(true);
    final BufferedImage image = render(surface);

    Pixels.assertPixelNear(image, 0, 0, GROUND, "the corner overflow is erased from the buffer");
    Pixels.assertPixelNear(
        image, WIDTH / 2, HEIGHT / 2, CHILD, "the child still covers the body it was given");
  }

  @Test
  void aSquareShapeLeavesTheClipNothingToCut() {
    final ElwhaSurface surface =
        surfaceWithChild().setShape(ShapeScale.NONE).setClipChildrenToCorners(true);

    Pixels.assertPixelNear(
        render(surface),
        0,
        0,
        CHILD,
        "with no corner radius the clip removes nothing, so the child reaches the corner");
  }

  @Test
  void aChildlessClipEnabledSurfaceStillPaintsItsFill() {
    final ElwhaSurface surface = new ElwhaSurface().setClipChildrenToCorners(true);

    Pixels.assertPixelNear(
        render(surface),
        WIDTH / 2,
        HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the fill is only folded into the clip buffer when there is a child to share the cut with");
  }

  @Test
  void theFoldedFillStillPaintsUnderTheChildrenWhenTheClipRuns() {
    final ElwhaSurface surface = new ElwhaSurface().setSurfaceRole(ColorRole.PRIMARY_CONTAINER);
    surface.setLayout(new BorderLayout());
    final JPanel inset = new JPanel();
    inset.setOpaque(false);
    surface.add(inset, BorderLayout.SOUTH);
    surface.setClipChildrenToCorners(true);

    Pixels.assertPixelNear(
        render(surface),
        WIDTH / 2,
        HEIGHT / 4,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "folding the fill into the buffer must not drop it where no child covers");
  }

  @Test
  void theBorderSurvivesTheClipPath() {
    final ElwhaSurface surface =
        surfaceWithChild()
            .setBorderRole(ColorRole.ERROR)
            .setBorderWidth(4)
            .setClipChildrenToCorners(true);

    Pixels.assertPixelNear(
        render(surface),
        1,
        HEIGHT / 2,
        ColorRole.ERROR.resolve(),
        "the border is stroked inside the kept region, after the corner cut");
  }

  // -------------------------------------------------- the paint-time resolver

  @Test
  void aSubclassCanForceTheClipWithoutDisturbingTheConsumerFlag() {
    final ElwhaSurface forced = new AlwaysClips();
    forced.setLayout(new BorderLayout());
    forced.add(edgeToEdgeChild(), BorderLayout.CENTER);

    assertThat(forced.getClipChildrenToCorners())
        .as("the public flag reports what a consumer set, not what the subclass resolved")
        .isFalse();
    Pixels.assertPixelNear(
        render(forced),
        0,
        0,
        GROUND,
        "the paint-time resolver is what actually gates the clip, so the corner is still cut");
  }

  /** Stands in for a subclass whose content intrinsically reaches the corners, e.g. card media. */
  private static final class AlwaysClips extends ElwhaSurface {
    @Override
    protected boolean clipsChildrenToCorners() {
      return true;
    }
  }
}
