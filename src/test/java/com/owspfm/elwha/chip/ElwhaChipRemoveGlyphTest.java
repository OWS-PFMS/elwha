package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the input chip's × glyph pairing with the chip's own foreground (#644).
 *
 * <p>The glyph is handed a {@code Component} at paint time, and it used to stroke in that
 * component's foreground. That component is the trailing {@link JLabel}, which has no foreground
 * override and no {@code FlatSVGIcon} for the chip's icon filter to re-point — so on a chip with a
 * custom surface role the × painted in the LAF label color while the text painted the role's {@code
 * on}-pair. The class documentation promises a derived, always-paired foreground, which made this a
 * contract violation rather than a nuance.
 *
 * <p>The glyph is painted directly rather than probed inside a rendered chip, and deliberately
 * against a host label whose foreground is set to something no palette carries. That is the whole
 * mechanism in one assertion: if the glyph consults its host it strokes the sabotage color, and if
 * it consults the chip it strokes the role.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipRemoveGlyphTest {

  /** A foreground no palette carries, planted on the host so a host read is unmistakable. */
  private static final Color SABOTAGE = Color.MAGENTA;

  /**
   * The raster ground. Deliberately not a token: {@code SURFACE} resolves to #FDF7FF in the light
   * baseline, which is inside the probe tolerance of the white that {@code ON_PRIMARY} resolves to
   * — so grounding on it would make "no pixel is ON_PRIMARY" fail against the ground itself.
   */
  private static final Color GROUND = new Color(0x00, 0x80, 0x00);

  private static final int BOX = 14;

  /** Renders the chip's trailing glyph over a host label whose foreground is {@link #SABOTAGE}. */
  private static BufferedImage paintGlyphOf(final ElwhaChip chip) {
    final JLabel host = (JLabel) chip.getTrailingButton();
    host.setForeground(SABOTAGE);
    final Icon glyph = host.getIcon();
    assertThat(glyph).as("the input-chip preset installs a trailing glyph").isNotNull();

    final BufferedImage image = new BufferedImage(BOX, BOX, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(GROUND);
      g.fillRect(0, 0, BOX, BOX);
      glyph.paintIcon(host, g, 0, 0);
    } finally {
      g.dispose();
    }
    return image;
  }

  @Test
  void removeGlyphStrokesInTheChipsResolvedForeground() {
    final ElwhaChip chip = ElwhaChip.inputChip("Tag", null).setSurfaceRole(ColorRole.PRIMARY);

    final BufferedImage shot = paintGlyphOf(chip);

    Pixels.assertAnyPixelNear(
        shot,
        BOX / 2,
        BOX / 2,
        BOX / 2,
        chip.resolveForegroundColor(),
        "the × pairs with the chip's surface role, not with the host label's foreground");
    Pixels.assertNoPixelNear(
        shot,
        BOX / 2,
        BOX / 2,
        BOX / 2,
        SABOTAGE,
        "and never reads the trailing label's own foreground");
  }

  @Test
  void glyphFollowsAChangeOfSurfaceRole() {
    final ElwhaChip chip = ElwhaChip.inputChip("Tag", null).setSurfaceRole(ColorRole.PRIMARY);
    final Color onPrimary = chip.resolveForegroundColor();

    // PRIMARY and TERTIARY both pair to white in the baseline palette, so a light/dark pair is
    // needed for the assertion to carry any information.
    chip.setSurfaceRole(ColorRole.SURFACE);
    final Color onSurface = chip.resolveForegroundColor();
    assertThat(onSurface)
        .as("the two roles must pair to different foregrounds or this proves nothing")
        .isNotEqualTo(onPrimary);

    final BufferedImage shot = paintGlyphOf(chip);

    Pixels.assertAnyPixelNear(
        shot, BOX / 2, BOX / 2, BOX / 2, onSurface, "the glyph re-resolves rather than caching");
    Pixels.assertNoPixelNear(
        shot, BOX / 2, BOX / 2, BOX / 2, onPrimary, "and drops the role it was built with");
  }

  @Test
  void aDisabledChipDimsItsRemoveGlyphWithTheRestOfItsContent() {
    final ElwhaChip chip = ElwhaChip.inputChip("Tag", null).setSurfaceRole(ColorRole.PRIMARY);
    chip.setEnabled(false);

    assertThat(chip.resolveForegroundColor().getAlpha())
        .as("disabled content resolves to the role at reduced alpha")
        .isLessThan(255);

    final BufferedImage shot = paintGlyphOf(chip);

    Pixels.assertNoPixelNear(
        shot,
        BOX / 2,
        BOX / 2,
        BOX / 2,
        SABOTAGE,
        "a disabled chip's × still comes from the chip, not the host label");
  }
}
