package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the recolored-glyph cache (#715). {@code paintGlyph} used to allocate a fresh
 * {@code BufferedImage} on every call — twice per paint, at ~60fps for the whole ~300ms handle
 * tween — so the buffer is now held per icon slot and keyed on the icon instance, the resolved
 * color and the size.
 *
 * <p>These tests exist for the risk the cache <em>introduces</em> rather than the allocation it
 * removes, which is #692's stale-cache concern seen from the other side: an under-keyed cache keeps
 * painting the glyph it rendered first. Both probes therefore reuse <strong>one switch
 * instance</strong> across the change — a fresh instance per case would start with an empty cache
 * and pass against any keying at all, which is why {@code ElwhaSwitchIconsTest}'s per-mode
 * parameterization does not already cover this.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/715">#715</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchGlyphCacheTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final int CENTER_Y = 20;

  /** The selected handle's centre — travelEnd. */
  private static final int ON_HANDLE_X = 40;

  /** Radius of the glyph scan; stays inside even the 24px handle. */
  private static final int GLYPH_RADIUS = 6;

  private static BufferedImage render(final ElwhaSwitch toggle) {
    return Pixels.render(toggle, WIDTH, HEIGHT);
  }

  private static ElwhaSwitch selectedWithIcons() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    toggle.setIconsVisible(true);
    return toggle;
  }

  @Test
  void theCachedGlyphIsRecoloredWhenTheThemeFlipsUnderTheSameSwitch() {
    ThemeExtension.install(Mode.LIGHT);
    final ElwhaSwitch toggle = selectedWithIcons();
    final Color light = ColorRole.ON_PRIMARY_CONTAINER.resolve();
    Pixels.assertAnyPixelNear(
        render(toggle),
        ON_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        light,
        "the light glyph is on the handle before the flip — this render fills the cache");

    ThemeExtension.install(Mode.DARK);
    final Color dark = ColorRole.ON_PRIMARY_CONTAINER.resolve();
    final BufferedImage after = render(toggle);

    assertThat(dark)
        .as("the two modes must resolve the role differently to test anything")
        .isNotEqualTo(light);
    Pixels.assertAnyPixelNear(
        after,
        ON_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        dark,
        "the glyph re-renders in the dark role rather than serving the cached light raster");
  }

  @Test
  void theCachedGlyphIsReplacedWhenTheIconChangesUnderTheSameSwitch() {
    final ElwhaSwitch toggle = selectedWithIcons();
    final BufferedImage before = render(toggle);

    toggle.setSelectedIcon(MaterialIcons.starFilled(ElwhaSwitch.ICON_SIZE_PX));
    final BufferedImage after = render(toggle);

    assertThat(handleDiffers(before, after))
        .as("a replaced glyph must reach the handle, not be masked by the cached check mark")
        .isTrue();
  }

  /**
   * The cache must be invisible when nothing changed. Without this the two probes above would still
   * pass against a "cache" that never caches, which would fix the staleness risk by reintroducing
   * the per-paint allocation the issue is about.
   */
  @Test
  void anUnchangedSwitchRendersIdenticallyTwice() {
    final ElwhaSwitch toggle = selectedWithIcons();

    final BufferedImage first = render(toggle);
    final BufferedImage second = render(toggle);

    assertThat(handleDiffers(first, second))
        .as("re-serving the cached raster is pixel-identical to rendering it")
        .isFalse();
  }

  /** Whether the two rasters disagree anywhere in the selected handle's glyph disc. */
  private static boolean handleDiffers(final BufferedImage a, final BufferedImage b) {
    for (int y = CENTER_Y - GLYPH_RADIUS; y <= CENTER_Y + GLYPH_RADIUS; y++) {
      for (int x = ON_HANDLE_X - GLYPH_RADIUS; x <= ON_HANDLE_X + GLYPH_RADIUS; x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          return true;
        }
      }
    }
    return false;
  }
}
