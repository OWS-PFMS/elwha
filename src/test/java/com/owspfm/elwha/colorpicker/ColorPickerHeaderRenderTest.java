package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.testkit.Corners;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the picker header's current-color swatch geometry — the corner it rounds, and
 * the fact that the swatch really is the picker's live color rather than a placeholder.
 *
 * <p>The swatch sits at the {@code SpaceScale.LG} pad in both axes when the header carries no
 * supporting text, which is what lets the corner probe anchor on a known origin.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ColorPickerHeaderRenderTest {

  private static final int WIDTH = 320;
  private static final int HEIGHT = 100;

  /**
   * The swatch color under test. Light on purpose: the swatch wears a 1 px {@code OUTLINE_VARIANT}
   * stroke riding its own corner arc, and the corner probe classifies every pixel as either body or
   * ground — so the stroke has to sit nearer the fill than the magenta ground, or the ring itself
   * reads as cut-away corner. Still far enough from magenta for the fill probe to prove something.
   */
  private static final Color SWATCH = new Color(0xEFE9F2);

  private static BufferedImage renderHeader() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setColor(SWATCH);
    // Clearing the supporting text drops the row above the swatch, which is what puts the swatch
    // at (pad, pad) and gives the corner probe a known origin.
    picker.setSupportingText(null);
    final ColorPickerHeader header = new ColorPickerHeader(picker);
    return Pixels.render(header, WIDTH, HEIGHT, Color.MAGENTA);
  }

  @Test
  void swatchFillsWithThePickersCurrentColor() {
    final int pad = SpaceScale.LG.px();

    Pixels.assertPixelNear(
        renderHeader(), pad + 20, pad + 20, SWATCH, "the swatch shows the color the picker holds");
  }

  @Test
  void swatchRoundsAtTheFullSmallTokenRadius() {
    final int pad = SpaceScale.LG.px();

    Corners.assertTopLeftRadius(
        renderHeader(),
        pad,
        pad,
        ShapeScale.SM.px(),
        SWATCH,
        Color.MAGENTA,
        ShapeScale.SM.px(),
        "the swatch handed its shape step to RoundRectangle2D undoubled and rendered at 4 before"
            + " the #663 normalization");
  }
}
