package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The headless construction smoke, run once per catalog leaf: the surface exists, carries a real
 * view hierarchy, asks for a size, and paints something.
 *
 * <p>This is the wave's blunt instrument. Every leaf composes a workbench or gallery out of the
 * component library and its playground panel builders, so a leaf that throws, lays out to nothing,
 * or paints an empty rectangle is a defect somewhere underneath it — and the leaf is named in the
 * failure, which is the whole point of parameterising rather than looping.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseLeafConstructionTest {

  // A generous desktop-sized viewport: wide enough that a workbench's split pane, controls column,
  // and code view all get real estate, so a leaf is exercised at the geometry it actually ships at.
  private static final int WIDTH = 1240;
  private static final int HEIGHT = 780;

  static List<String> leaves() {
    return ShowcaseFixture.leafLabels();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("leaves")
  void leafConstructsASurface(final String label) {
    assertThat(ShowcaseFixture.surfaceOf(label))
        .as("populating the catalog realises every leaf; a null surface never reaches CardLayout")
        .isNotNull();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("leaves")
  void leafCarriesAViewHierarchy(final String label) {
    final List<Component> tree = ShowcaseFixture.descendants(ShowcaseFixture.surfaceOf(label));

    assertThat(tree)
        .as("a leaf that constructed to a bare empty panel would still pass a no-throw check")
        .hasSizeGreaterThan(3);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("leaves")
  void leafAsksForARealSize(final String label) {
    final Dimension preferred = ShowcaseFixture.surfaceOf(label).getPreferredSize();

    assertThat(preferred.width).as("%s preferred width", label).isPositive();
    assertThat(preferred.height).as("%s preferred height", label).isPositive();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("leaves")
  void leafLaysOutAndPaintsHeadlessly(final String label) {
    final JComponent surface = ShowcaseFixture.surfaceOf(label);
    ShowcaseFixture.layOut(surface, WIDTH, HEIGHT);

    final Color ground = new Color(0xFF00FF);
    final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(ground);
      g.fillRect(0, 0, WIDTH, HEIGHT);
      surface.paint(g);
    } finally {
      g.dispose();
    }

    assertThat(paintedPixels(image, ground))
        .as(
            "%s — a leaf that paints nothing over the ground is an empty preview, whether it threw "
                + "or simply laid out to nothing",
            label)
        .isGreaterThan(WIDTH * HEIGHT / 100);
  }

  // Pixels the leaf actually covered. The ground is a colour no M3 palette produces, so any
  // difference is paint rather than a coincidental role match.
  private static int paintedPixels(final BufferedImage image, final Color ground) {
    final int groundRgb = ground.getRGB();
    int painted = 0;
    for (int y = 0; y < image.getHeight(); y += 2) {
      for (int x = 0; x < image.getWidth(); x += 2) {
        if (image.getRGB(x, y) != groundRgb) {
          painted++;
        }
      }
    }
    return painted;
  }
}
