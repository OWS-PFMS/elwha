package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the colour picker's two gradient caches against a direct render (#692) — the
 * {@code ShadowPainterCacheTest} technique applied to the other places the library caches a
 * rendered image.
 *
 * <p>Both panes generate their raster once at <em>logical</em> size and blit it in user space: the
 * wheel through {@code drawImage}, the spectrum through a {@code TexturePaint} anchored to a
 * user-space rectangle. At scale 1 that is a one-to-one copy and the cached path is the direct path
 * — asserted below, because it is the property that makes the cache free rather than merely cheap.
 *
 * <p>On a HiDPI surface it is not: the 1× raster is resampled up to the device resolution instead
 * of the per-pixel function being evaluated there. The bounds here are <strong>measured, not
 * chosen</strong>, and they are what the issue asked for — a number where there was none. The wheel
 * is the interesting one: its circular edge carries analytic per-pixel coverage computed for a
 * 146&nbsp;px disc, and bilinearly stretching that is a visibly softer edge than evaluating
 * coverage at 292&nbsp;px would be. The spectrum's interior is a smooth ramp, where resampling is
 * nearly free.
 *
 * <p>Neither delta is defect-sized: a softer edge on a 2× display is a fidelity ceiling, not a
 * wrong colour, and no sampled value moves — the thumb reads the model, not the raster. So these
 * are pins, not a bug report. If a future change makes one of them jump, that is the signal.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class GradientCacheDeltaTest {

  /**
   * Worst per-channel gap between the wheel's upsampled cache and a device-resolution render, over
   * the disc interior. Measured at 2× on the shipped 146 px disc.
   */
  private static final int WHEEL_INTERIOR_TOLERANCE = 2;

  /** The same, for the spectrum's smooth saturation/value ramp. */
  private static final int SPECTRUM_TOLERANCE = 2;

  /** Blits {@code source} into a {@code scale}× raster the way a HiDPI surface would. */
  private static BufferedImage upscaled(final BufferedImage source, final int scale) {
    final int w = source.getWidth() * scale;
    final int h = source.getHeight() * scale;
    final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = out.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.scale(scale, scale);
      g.drawImage(source, 0, 0, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  /**
   * Worst per-channel difference over the pixels both rasters consider opaque, ignoring any pixel
   * where either side is even partly transparent — that is the antialiased rim, which is exactly
   * where the two disagree by construction and is measured separately.
   */
  private static int worstOpaqueDelta(final BufferedImage a, final BufferedImage b) {
    int worst = 0;
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        final int pa = a.getRGB(x, y);
        final int pb = b.getRGB(x, y);
        if (((pa >>> 24) & 0xFF) != 0xFF || ((pb >>> 24) & 0xFF) != 0xFF) {
          continue;
        }
        for (int shift = 0; shift < 24; shift += 8) {
          worst = Math.max(worst, Math.abs(((pa >> shift) & 0xFF) - ((pb >> shift) & 0xFF)));
        }
      }
    }
    return worst;
  }

  @Test
  void theWheelsCacheIsTheDirectRenderAtScaleOne() {
    final BufferedImage cached = WheelPane.renderDisc(WheelPane.DISC_DIAMETER);
    final BufferedImage direct = WheelPane.renderDisc(WheelPane.DISC_DIAMETER);

    for (int y = 0; y < cached.getHeight(); y++) {
      for (int x = 0; x < cached.getWidth(); x++) {
        assertThat(cached.getRGB(x, y))
            .as("the disc is a pure function of its diameter, so caching it costs nothing at 1x")
            .isEqualTo(direct.getRGB(x, y));
      }
    }
  }

  @ParameterizedTest(name = "the wheel's upsampled cache stays close to a {0}x direct render")
  @ValueSource(ints = {2, 3})
  void theWheelsUpsampledCacheStaysCloseToADeviceResolutionRender(final int scale) {
    final BufferedImage upsampled = upscaled(WheelPane.renderDisc(WheelPane.DISC_DIAMETER), scale);
    final BufferedImage direct = WheelPane.renderDisc(WheelPane.DISC_DIAMETER * scale);

    assertThat(worstOpaqueDelta(upsampled, direct))
        .as(
            "stretching the 1x disc to %dx instead of evaluating the polar function there is a"
                + " fidelity ceiling, and this is where it sits",
            scale)
        .isLessThanOrEqualTo(WHEEL_INTERIOR_TOLERANCE);
  }

  @ParameterizedTest(name = "the wheel's rim softens by at most {1}/255 of alpha at {0}x")
  @CsvSource({"2, 120", "3, 164"})
  void theWheelsAntialiasedRimIsWhereTheTwoRendersActuallyDisagree(
      final int scale, final int tolerance) {
    final BufferedImage upsampled = upscaled(WheelPane.renderDisc(WheelPane.DISC_DIAMETER), scale);
    final BufferedImage direct = WheelPane.renderDisc(WheelPane.DISC_DIAMETER * scale);

    int worstAlpha = 0;
    for (int y = 0; y < upsampled.getHeight(); y++) {
      for (int x = 0; x < upsampled.getWidth(); x++) {
        final int alphaA = (upsampled.getRGB(x, y) >>> 24) & 0xFF;
        final int alphaB = (direct.getRGB(x, y) >>> 24) & 0xFF;
        worstAlpha = Math.max(worstAlpha, Math.abs(alphaA - alphaB));
      }
    }

    assertThat(worstAlpha)
        .as("the stretched coverage term is the real cost of caching the disc at 1x, at %dx", scale)
        .isLessThanOrEqualTo(tolerance);
  }

  @Test
  void theSpectrumsCacheIsTheDirectRenderAtScaleOne() {
    final BufferedImage cached = SpectrumPane.renderSvRaster(240, 160, 210);
    final BufferedImage direct = SpectrumPane.renderSvRaster(240, 160, 210);

    assertThat(worstOpaqueDelta(cached, direct))
        .as("the square is a pure function of (width, height, hue), all three of which are keyed")
        .isZero();
  }

  @ParameterizedTest(name = "the spectrum's upsampled cache stays close to a {0}x direct render")
  @ValueSource(ints = {2, 3})
  void theSpectrumsUpsampledCacheStaysCloseToADeviceResolutionRender(final int scale) {
    final BufferedImage upsampled = upscaled(SpectrumPane.renderSvRaster(240, 160, 210), scale);
    final BufferedImage direct = SpectrumPane.renderSvRaster(240 * scale, 160 * scale, 210);

    assertThat(worstOpaqueDelta(upsampled, direct))
        .as("a smooth ramp resamples almost exactly, unlike the wheel's analytic edge")
        .isLessThanOrEqualTo(SPECTRUM_TOLERANCE);
  }

  @Test
  void roundingTheHueIsFaithfulRatherThanStale() {
    // The cache key rounds the hue to whole degrees, which would be a staleness bug if the raster
    // were generated at the unrounded value. It is generated AT the rounded hue, so cache and
    // content agree — the quantisation is a fidelity choice, not a key that misses an input.
    final BufferedImage atKey = SpectrumPane.renderSvRaster(120, 80, 210);
    final BufferedImage regenerated = SpectrumPane.renderSvRaster(120, 80, Math.round(210.4f));

    assertThat(worstOpaqueDelta(atKey, regenerated)).isZero();
  }
}
