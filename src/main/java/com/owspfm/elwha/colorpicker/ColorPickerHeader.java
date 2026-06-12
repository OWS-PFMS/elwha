package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * The picker's M3 header (design doc {@code elwha-color-picker-design.md} §4): supporting text
 * naming the task, a headline row rendering the pending selection — a bordered preview swatch
 * beside the uppercase hex readout — and the divider that closes the header, mirroring the M3
 * date/time-picker header anatomy. Display-only; hex <em>entry</em> lives in the SLIDERS pane.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ColorPickerHeader extends JComponent {

  private static final int SWATCH_SIZE = 40;

  private final ElwhaColorPicker picker;

  ColorPickerHeader(final ElwhaColorPicker picker) {
    this.picker = picker;
    setOpaque(false);
  }

  @Override
  public Dimension getPreferredSize() {
    final int pad = SpaceScale.LG.px();
    int height = pad;
    if (picker.getSupportingText() != null) {
      height += fontHeight(TypeRole.LABEL_LARGE.resolve()) + SpaceScale.MD.px();
    }
    height += SWATCH_SIZE + pad + 1;
    return new Dimension(pad + SWATCH_SIZE + SpaceScale.MD.px() + 180 + pad, height);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      if (!picker.isEnabled()) {
        g2.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      }
      final int pad = SpaceScale.LG.px();
      int y = pad;
      final String supporting = picker.getSupportingText();
      if (supporting != null) {
        final Font supportingFont = TypeRole.LABEL_LARGE.resolve();
        final FontMetrics fm = g2.getFontMetrics(supportingFont);
        g2.setFont(supportingFont);
        g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
        g2.drawString(supporting, pad, y + fm.getAscent());
        y += fm.getHeight() + SpaceScale.MD.px();
      }
      paintSwatch(g2, pad, y);
      final Font headlineFont = picker.headlineTypeRole().resolve();
      final FontMetrics hm = g2.getFontMetrics(headlineFont);
      g2.setFont(headlineFont);
      g2.setColor(ColorRole.ON_SURFACE.resolve());
      final String hex = picker.formatCurrentHex();
      final int textX = pad + SWATCH_SIZE + SpaceScale.MD.px();
      final int textY = y + (SWATCH_SIZE - hm.getHeight()) / 2 + hm.getAscent();
      g2.drawString(hex, textX, textY);
      g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
      g2.fillRect(0, getHeight() - 1, getWidth(), 1);
    } finally {
      g2.dispose();
    }
  }

  private void paintSwatch(final Graphics2D g2, final int x, final int y) {
    final int arc = ShapeScale.SM.px();
    final java.awt.geom.RoundRectangle2D.Double shape =
        new java.awt.geom.RoundRectangle2D.Double(x, y, SWATCH_SIZE, SWATCH_SIZE, arc, arc);
    if (picker.isAlphaEnabled() && picker.getColor().getAlpha() < 255) {
      Checkerboard.fill(g2, shape);
    }
    g2.setColor(picker.getColor());
    g2.fill(shape);
    g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
    g2.drawRoundRect(x, y, SWATCH_SIZE - 1, SWATCH_SIZE - 1, arc, arc);
  }

  private static int fontHeight(final Font font) {
    return new javax.swing.JLabel().getFontMetrics(font).getHeight();
  }
}
