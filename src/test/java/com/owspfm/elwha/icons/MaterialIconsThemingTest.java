package com.owspfm.elwha.icons;

import static org.assertj.core.api.Assertions.assertThat;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the shared color filter — that a bundled monochrome symbol paints entirely in
 * the current {@code Label.foreground}, and that it follows a theme flip on the next paint with no
 * re-allocation and no listener.
 *
 * <p>Probing is deliberately geometry-free: rather than picking a pixel inside the glyph (the
 * determinism rules forbid leaning on rasterizer specifics), each assertion collects the <em>fully
 * opaque</em> pixels of the whole raster and asserts the set. Anti-aliased edge pixels carry
 * partial alpha and a blended color, so they are excluded by construction.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class MaterialIconsThemingTest {

  private static final int CANVAS = 32;

  /** A dense glyph, chosen so a solid interior exists at any reasonable render size. */
  private static FlatSVGIcon denseGlyph() {
    return MaterialIcons.pushPinFilled();
  }

  /** The distinct colors of every fully opaque pixel in the icon's raster. */
  private static Set<Color> opaqueColors(final FlatSVGIcon icon) {
    final BufferedImage image = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      icon.paintIcon(new JPanel(), g, 4, 4);
    } finally {
      g.dispose();
    }
    final Set<Color> colors = new LinkedHashSet<>();
    for (int y = 0; y < CANVAS; y++) {
      for (int x = 0; x < CANVAS; x++) {
        final Color pixel = new Color(image.getRGB(x, y), true);
        if (pixel.getAlpha() == 255) {
          colors.add(new Color(pixel.getRGB(), false));
        }
      }
    }
    return colors;
  }

  @Test
  void aBundledGlyphPaintsSomethingAtAll() {
    assertThat(opaqueColors(denseGlyph()))
        .as("a glyph that rasterized to nothing would make every color assertion vacuous")
        .isNotEmpty();
  }

  @Test
  void everyPaintedPixelTakesTheLabelForeground() {
    assertThat(opaqueColors(denseGlyph()))
        .as("Material Symbols are monochrome, so the blanket remap leaves exactly one color")
        .containsExactly(new Color(UIManager.getColor("Label.foreground").getRGB(), false));
  }

  @Test
  void labelForegroundIsItselfBridgedToTheOnSurfaceRole() {
    assertThat(UIManager.getColor("Label.foreground"))
        .as("icons follow Label.foreground, which the bridge maps onto the token vocabulary")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
  }

  @Test
  void sameIconInstanceRepaintsInTheNewThemeAfterAModeFlip() {
    final FlatSVGIcon icon = denseGlyph();
    final Set<Color> light = opaqueColors(icon);

    ThemeExtension.install(Mode.DARK);

    final Set<Color> dark = opaqueColors(icon);
    assertThat(dark)
        .as("the filter runs at paint time, so one instance re-themes with no re-allocation")
        .containsExactly(new Color(UIManager.getColor("Label.foreground").getRGB(), false));
    assertThat(dark)
        .as("a light and a dark foreground must not land on the same color")
        .isNotEqualTo(light);
  }

  @Test
  void aConsumerOwnedIconFollowsTheSameThemeOnceThemed() {
    final FlatSVGIcon consumerIcon =
        MaterialIcons.themed(
            new FlatSVGIcon("com/owspfm/icons/material/push_pin_fill.svg", 24, 24));

    ThemeExtension.install(Mode.DARK);

    assertThat(opaqueColors(consumerIcon))
        .as("themed() gives a client-path SVG the identical treatment the bundle gets")
        .containsExactly(new Color(UIManager.getColor("Label.foreground").getRGB(), false));
  }

  @Test
  void anUnthemedIconDoesNotFollowTheTheme() {
    final FlatSVGIcon raw = new FlatSVGIcon("com/owspfm/icons/material/push_pin_fill.svg", 24, 24);

    assertThat(opaqueColors(raw))
        .as("the theming is the filter's doing — a bare FlatSVGIcon keeps the source SVG's ink")
        .isNotEqualTo(opaqueColors(denseGlyph()));
  }

  @Test
  void sizedOverloadPaintsTheSameSingleColor() {
    assertThat(opaqueColors(MaterialIcons.pushPinFilled(16)))
        .as("the filter is size-independent")
        .containsExactly(new Color(UIManager.getColor("Label.foreground").getRGB(), false));
  }
}
