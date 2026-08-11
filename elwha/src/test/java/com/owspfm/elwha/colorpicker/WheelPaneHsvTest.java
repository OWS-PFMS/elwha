package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of the wheel's hue/saturation disc — the raster itself and the <b>black-composite
 * value trick</b> the design leans on: because {@code RGB(h,s,v) = v · RGB(h,s,1)}, the disc is
 * rasterized <em>once</em> at full brightness and the value axis is applied by compositing
 * translucent black over it. That identity is exact rather than approximate, which is the whole
 * reason the cache is legitimate — so it is worth asserting as arithmetic on sampled pixels rather
 * than trusting the comment.
 *
 * <p>Assertions are written as HSV invariants (the centre is white, saturation grows outward, hue
 * rotates counter-clockwise, value scales every channel) rather than by recomputing the pane's own
 * formula — a test that restates the implementation would agree with it however wrong it got.
 *
 * <p>The disc is sampled off a transparent ground so the anti-aliased edge is readable: the design
 * notes that Java2D {@code clip()} is never anti-aliased, so the circle's edge is per-pixel
 * coverage baked into the raster. A hard-clipped disc would pass every color assertion here and
 * still look wrong, so the partial-alpha rim gets its own test.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class WheelPaneHsvTest {

  private static final int D = WheelPane.DISC_DIAMETER;
  private static final int CENTER = D / 2;

  /** The wheel pane of a fresh picker, ready to be driven through its package-private seams. */
  private static WheelPane wheel() {
    return (WheelPane) new ElwhaColorPicker(Color.RED).paneFor(PickerMode.WHEEL);
  }

  /**
   * A wheel whose thumb is parked at the top of the rim, so raster probes taken elsewhere on the
   * disc cannot land on the thumb's ring — the ring is opaque chrome and would read as a colour.
   */
  private static WheelPane rasterPane() {
    final WheelPane pane = wheel();
    pane.hueSatTo(90f, 1f, false);
    return pane;
  }

  /** The hue/saturation disc inside a wheel pane — the only child sized to the disc diameter. */
  private static JComponent disc(final WheelPane pane) {
    return findDisc(pane);
  }

  private static JComponent findDisc(final Container container) {
    for (final Component child : container.getComponents()) {
      if (child instanceof JComponent candidate) {
        if (candidate.getClass().getSimpleName().equals("WheelDisc")) {
          return candidate;
        }
        final JComponent nested = findDisc(candidate);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  /** The disc painted on a transparent ground, so unpainted pixels stay unpainted. */
  private static BufferedImage discRaster(final WheelPane pane) {
    return Pixels.render(disc(pane), D, D, new Color(0, 0, 0, 0));
  }

  /**
   * The pixel at a polar position on the disc: {@code degrees} counter-clockwise, radius fraction.
   */
  private static Color at(final BufferedImage raster, final double degrees, final double radius) {
    final double r = radius * (D / 2.0);
    final int x = (int) Math.round(CENTER + Math.cos(Math.toRadians(degrees)) * r);
    final int y = (int) Math.round(CENTER - Math.sin(Math.toRadians(degrees)) * r);
    return new Color(raster.getRGB(x, y), true);
  }

  // ------------------------------------------------------------ the raster

  @Test
  void centreOfTheDiscIsWhite() {
    final Color center = at(discRaster(rasterPane()), 0, 0);

    assertThat(chroma(center))
        .as("zero saturation at full value has no colour in it at all")
        .isLessThan(6);
    assertThat(center.getRed())
        .as("and full value makes that colourless pixel white rather than grey")
        .isGreaterThan(250);
  }

  @ParameterizedTest
  @CsvSource({"0, red", "120, green", "240, blue"})
  void primaryHuesSitAtTheirPolarAngles(final int degrees, final String channel) {
    final Color sampled = at(discRaster(rasterPane()), degrees, 0.95);

    final int dominant =
        switch (channel) {
          case "red" -> sampled.getRed();
          case "green" -> sampled.getGreen();
          default -> sampled.getBlue();
        };
    final int others =
        switch (channel) {
          case "red" -> Math.max(sampled.getGreen(), sampled.getBlue());
          case "green" -> Math.max(sampled.getRed(), sampled.getBlue());
          default -> Math.max(sampled.getRed(), sampled.getGreen());
        };
    assertThat(dominant)
        .as("%d degrees on the wheel is %s at full brightness", degrees, channel)
        .isGreaterThan(240);
    assertThat(others).as("with the other channels desaturated away near the rim").isLessThan(40);
  }

  @Test
  void hueRotatesCounterClockwiseNotClockwise() {
    final BufferedImage raster = discRaster(rasterPane());

    final Color above = at(raster, 60, 0.95);
    final Color below = at(raster, 300, 0.95);

    assertThat(above.getGreen())
        .as("60 degrees is up and to the right, in the yellow-green arc")
        .isGreaterThan(above.getBlue());
    assertThat(below.getBlue())
        .as("300 degrees is down and to the right, in the magenta arc")
        .isGreaterThan(below.getGreen());
  }

  @Test
  void saturationGrowsFromTheCentreOutward() {
    final BufferedImage raster = discRaster(rasterPane());

    final int nearCentre = chroma(at(raster, 0, 0.2));
    final int midway = chroma(at(raster, 0, 0.5));
    final int nearRim = chroma(at(raster, 0, 0.9));

    assertThat(nearCentre).as("the centre is unsaturated").isLessThan(midway);
    assertThat(midway).as("and saturation climbs monotonically toward the rim").isLessThan(nearRim);
  }

  @Test
  void everythingOutsideTheDiscIsFullyTransparent() {
    final BufferedImage raster = discRaster(rasterPane());

    assertThat(new Color(raster.getRGB(0, 0), true).getAlpha())
        .as("the corner of a square raster is outside a circle")
        .isZero();
    assertThat(new Color(raster.getRGB(D - 1, D - 1), true).getAlpha())
        .as("as is the opposite corner")
        .isZero();
  }

  @Test
  void rimIsAntiAliasedInTheRasterRatherThanHardClipped() {
    final BufferedImage raster = discRaster(rasterPane());

    boolean partial = false;
    for (int y = 0; y < D && !partial; y++) {
      for (int x = 0; x < D && !partial; x++) {
        final int alpha = new Color(raster.getRGB(x, y), true).getAlpha();
        partial = alpha > 0 && alpha < 255;
      }
    }

    assertThat(partial)
        .as(
            "Java2D clip() is never anti-aliased, so the edge has to be per-pixel coverage inside"
                + " the raster — a hard-edged disc would pass every color assertion and still look"
                + " wrong")
        .isTrue();
  }

  // ------------------------------------------- the black-composite value

  @Test
  void loweringTheValueScalesEveryChannelByTheSameFactor() {
    final WheelPane pane = rasterPane();
    final Color full = at(discRaster(pane), 30, 0.7);

    pane.valueTo(50, false);
    final Color half = at(discRaster(pane), 30, 0.7);

    assertThat(half.getRed() / 255.0)
        .as("RGB(h,s,v) = v x RGB(h,s,1) — red")
        .isCloseTo(full.getRed() / 255.0 * 0.5, within(0.02));
    assertThat(half.getGreen() / 255.0)
        .as("green")
        .isCloseTo(full.getGreen() / 255.0 * 0.5, within(0.02));
    assertThat(half.getBlue() / 255.0)
        .as("blue")
        .isCloseTo(full.getBlue() / 255.0 * 0.5, within(0.02));
  }

  @Test
  void aZeroValueRendersTheWholeDiscBlack() {
    final WheelPane pane = rasterPane();

    pane.valueTo(0, false);
    final BufferedImage raster = discRaster(pane);

    for (final double degrees : new double[] {0, 90, 180, 270}) {
      final Color sampled = at(raster, degrees, 0.6);
      assertThat(chroma(sampled) + sampled.getRed())
          .as("at zero value every hue collapses to black, whatever the angle")
          .isLessThan(12);
    }
  }

  @Test
  void loweringTheValueDoesNotDisturbTheHue() {
    final WheelPane pane = wheel();
    final float hueAtFullValue = pane.hueDegrees();

    pane.valueTo(0, false);

    assertThat(pane.hueDegrees())
        .as("sliding value to black must not snap the hue — the pane owns float h/s while editing")
        .isEqualTo(hueAtFullValue);
  }

  @Test
  void draggingThroughTheDesaturatedCentrePreservesTheHue() {
    final WheelPane pane = wheel();
    pane.hueSatTo(200f, 0.8f, true);

    pane.hueSatTo(200f, 0f, true);

    assertThat(pane.hueDegrees())
        .as("a colorless centre has no hue to read back, so the pane keeps the one being edited")
        .isEqualTo(200f);
    assertThat(pane.saturation()).as("while the saturation does go to zero").isZero();
  }

  // ------------------------------------------------------------ round-trip

  @Test
  void discReportsTheHueSaturationAndValueItWasGiven() {
    final WheelPane pane = wheel();

    pane.hueSatTo(120f, 0.5f, false);
    pane.valueTo(75, false);

    assertThat(pane.hueDegrees()).as("hue").isEqualTo(120f);
    assertThat(pane.saturation()).as("saturation").isEqualTo(0.5f);
    assertThat(pane.value()).as("value").isCloseTo(0.75f, within(0.01f));
  }

  @Test
  void committingAHueSaturationChangeReachesThePicker() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final WheelPane pane = (WheelPane) picker.paneFor(PickerMode.WHEEL);

    pane.hueSatTo(240f, 1f, false);

    assertThat(picker.getColor().getBlue())
        .as("a blue hue at full saturation and value writes blue back to the picker")
        .isGreaterThan(200);
    assertThat(picker.getColor().getRed()).as("with no red left").isLessThan(40);
  }

  @Test
  void anAdjustingCommitMarksThePickerAdjusting() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final WheelPane pane = (WheelPane) picker.paneFor(PickerMode.WHEEL);

    pane.hueSatTo(240f, 1f, true);
    assertThat(picker.isAdjusting()).as("a live drag is marked adjusting").isTrue();

    pane.hueSatTo(240f, 1f, false);
    assertThat(picker.isAdjusting()).as("and the release commits").isFalse();
  }

  /** Distance between the brightest and darkest channel — zero for any grey. */
  private static int chroma(final Color color) {
    final int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
    final int min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
    return max - min;
  }
}
