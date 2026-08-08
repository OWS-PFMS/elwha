package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of the eyedropper's logical&rarr;raster coordinate mapping (#714).
 *
 * <p>Scope is deliberate. Opening a sampler needs a display and the macOS Screen Recording
 * permission, so the capture path stays under the eyedropper's manual-smoke carve-out (see {@code
 * ElwhaColorPickerOverlayGuiTest}) — and the defect itself only appears on a HiDPI screen, which CI
 * does not have. What is fully pinnable here is the arithmetic that was wrong: a synthetic raster
 * standing in for a device-resolution capture reproduces the exact failure without a screen, and it
 * is the part a future edit is most likely to break.
 *
 * <p>Still needs a real HiDPI display to confirm end to end: that {@code
 * createMultiResolutionScreenCapture} actually yields a 2&times; variant on macOS Retina, and that
 * the delivered colour matches the pixel under the crosshair.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/714">#714</a>
 */
class ScreenSamplerScaleTest {

  /** A logical screen 100×50; the capture arrives at {@code scale}× that. */
  private static final int LOGICAL_W = 100;

  private static final int LOGICAL_H = 50;

  private static final Color TARGET = new Color(0x2196F3);
  private static final Color FIELD = new Color(0x101010);

  /**
   * A capture at {@code scale}× logical resolution, filled with {@link #FIELD} except for the one
   * device pixel that a correct mapping must find at {@code (logicalX, logicalY)}.
   */
  private static BufferedImage captureWithTargetAt(
      final int scale, final int logicalX, final int logicalY) {
    final BufferedImage capture =
        new BufferedImage(LOGICAL_W * scale, LOGICAL_H * scale, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < capture.getHeight(); y++) {
      for (int x = 0; x < capture.getWidth(); x++) {
        capture.setRGB(x, y, FIELD.getRGB());
      }
    }
    // The centre of the logical pixel's device footprint — the pixel under the crosshair.
    capture.setRGB(
        (int) Math.floor((logicalX + 0.5) * scale),
        (int) Math.floor((logicalY + 0.5) * scale),
        TARGET.getRGB());
    return capture;
  }

  @ParameterizedTest(name = "a {0}x capture measures as scale {0}")
  @CsvSource({"1, 1.0", "2, 2.0", "3, 3.0"})
  void captureScaleIsMeasuredFromTheRasterNotAssumed(final int scale, final double expected) {
    final BufferedImage capture = captureWithTargetAt(scale, 0, 0);

    assertThat(ScreenSampler.captureScale(capture.getWidth(), LOGICAL_W)).isEqualTo(expected);
    assertThat(ScreenSampler.captureScale(capture.getHeight(), LOGICAL_H)).isEqualTo(expected);
  }

  /**
   * The defect itself. At 2× the old code read raster {@code (x, y)} with a logical {@code (x, y)},
   * so it answered with a pixel from the top-left quadrant of the screen — an error that grows with
   * distance from the origin, which is why it was a wrong colour rather than a soft one.
   */
  @ParameterizedTest(name = "logical ({1},{2}) samples correctly from a {0}x capture")
  @CsvSource({
    "1, 0, 0", "1, 73, 41", "1, 99, 49",
    "2, 0, 0", "2, 73, 41", "2, 99, 49",
    "3, 12, 7", "3, 99, 49"
  })
  void aLogicalPointSamplesItsOwnDevicePixel(final int scale, final int x, final int y) {
    final BufferedImage capture = captureWithTargetAt(scale, x, y);
    final double s = ScreenSampler.captureScale(capture.getWidth(), LOGICAL_W);

    assertThat(ScreenSampler.colorAt(capture, s, s, x, y))
        .as("logical (%d,%d) on a %dx capture", x, y, scale)
        .isEqualTo(TARGET);
  }

  @Test
  void indexingADeviceResolutionCaptureWithLogicalCoordinatesIsWhatWentWrong() {
    final BufferedImage capture = captureWithTargetAt(2, 73, 41);

    assertThat(ScreenSampler.colorAt(capture, 73, 41))
        .as("the unscaled read lands elsewhere on the screen — the #714 failure, kept explicit")
        .isEqualTo(FIELD);
    assertThat(ScreenSampler.colorAt(capture, 2.0, 2.0, 73, 41))
        .as("while the scaled read finds the pixel under the crosshair")
        .isEqualTo(TARGET);
  }

  /**
   * Far-edge behaviour, which the old code got wrong in a second way: a logical coordinate near the
   * right or bottom edge of a 2× capture indexes well inside the raster, so it never clamped and
   * never looked out of range — it just answered from the middle of the screen.
   */
  @Test
  void aFarCornerLogicalPointIsReachableRatherThanClamped() {
    final BufferedImage capture = captureWithTargetAt(2, LOGICAL_W - 1, LOGICAL_H - 1);

    assertThat(ScreenSampler.colorAt(capture, 2.0, 2.0, LOGICAL_W - 1, LOGICAL_H - 1))
        .as("the last logical pixel maps inside the raster, not past its end")
        .isEqualTo(TARGET);
  }

  @Test
  void anOutOfRangeLogicalPointStillClampsIntoTheRaster() {
    final BufferedImage capture = captureWithTargetAt(2, 0, 0);

    assertThat(ScreenSampler.colorAt(capture, 2.0, 2.0, -40, -40))
        .as("a negative logical point clamps rather than throwing")
        .isEqualTo(FIELD);
    assertThat(ScreenSampler.colorAt(capture, 2.0, 2.0, LOGICAL_W * 4, LOGICAL_H * 4))
        .as("and so does one past the far edge")
        .isEqualTo(FIELD);
  }

  @Test
  void aDegenerateLogicalExtentFallsBackToTheIdentity() {
    assertThat(ScreenSampler.captureScale(1920, 0))
        .as("a zero-width device cannot divide — the mapping degrades to 1:1 rather than NaN")
        .isEqualTo(1.0);
  }
}
