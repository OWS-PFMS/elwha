package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of what M3's elevation tokens actually ramp (#667).
 *
 * <p>Across levels 1–5 the tokens raise each pass's blur radius and downward offset and hold both
 * opacities flat — key 0.30, ambient 0.15, at every level. That is why {@link ShadowPainter}'s two
 * source alphas are single constants where their blur and offset siblings are per-level tables: the
 * alpha accessors used to take an {@code elevation} argument and ignore it, which read as an
 * unwritten ramp rather than a deliberately flat one.
 *
 * <p>Asserted through the render rather than the constants, so the property survives any future
 * refactor of how the alphas are held.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShadowPainterElevationTest {

  private static final int BODY_W = 120;
  private static final int BODY_H = 40;
  private static final int ARC = 20;

  /** The darkest pixel anywhere in the composited key-over-ambient stack. */
  private static int peakAlpha(final int elevation) {
    final BufferedImage shadow =
        ShadowPainter.renderShadowAtBodySize(BODY_W, BODY_H, ARC, elevation);
    int peak = 0;
    for (int y = 0; y < shadow.getHeight(); y++) {
      for (int x = 0; x < shadow.getWidth(); x++) {
        peak = Math.max(peak, (shadow.getRGB(x, y) >>> 24) & 0xFF);
      }
    }
    return peak;
  }

  @Test
  void raisingElevationDoesNotDarkenTheShadow() {
    final int atLevelOne = peakAlpha(1);

    for (int elevation = 2; elevation <= 5; elevation++) {
      assertThat(peakAlpha(elevation))
          .as("level %d — M3 ramps blur and offset per level and holds opacity flat", elevation)
          .isCloseTo(atLevelOne, org.assertj.core.data.Offset.offset(2));
    }
  }

  @Test
  void raisingElevationSpreadsTheShadowFurther() {
    for (int elevation = 2; elevation <= 5; elevation++) {
      assertThat(ShadowPainter.shadowInsets(elevation).bottom)
          .as("level %d reserves more halo below than the level under it", elevation)
          .isGreaterThan(ShadowPainter.shadowInsets(elevation - 1).bottom);
    }
  }
}
