package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * The painted plain-tooltip surface: an {@link ColorRole#INVERSE_SURFACE} round-rect at {@link
 * ShapeScale#XS} carrying a {@link TypeRole#BODY_SMALL} {@link ColorRole#INVERSE_ON_SURFACE} label,
 * word-wrapped by hand at the 200&nbsp;px plain max width (no HTML views — the #305 doctrine). The
 * wrapped block is horizontally centered as a unit; lines inside it stay leading-aligned (Compose
 * parity), which collapses to true centering for the single-line common case.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
class TooltipSurface extends JPanel {

  /** Plain horizontal content padding (M3 {@code PlainTooltipContentPadding}). */
  static final int PLAIN_H_PAD_PX = 8;

  /** Plain vertical content padding (M3 {@code PlainTooltipContentPadding}). */
  static final int PLAIN_V_PAD_PX = 4;

  /** Minimum tooltip width (M3 {@code TooltipMinWidth}). */
  static final int MIN_WIDTH_PX = 40;

  /** Minimum tooltip height (M3 {@code TooltipMinHeight}). */
  static final int MIN_HEIGHT_PX = 24;

  /** Plain max width — the label wraps beyond this (M3 {@code plainTooltipMaxWidth}). */
  static final int PLAIN_MAX_WIDTH_PX = 200;

  private String text;

  TooltipSurface(final String text) {
    setOpaque(false);
    setFocusable(false);
    this.text = text;
  }

  void setText(final String text) {
    this.text = text;
    revalidate();
    repaint();
  }

  String getText() {
    return text;
  }

  @Override
  public Dimension getPreferredSize() {
    final FontMetrics fm = getFontMetrics(TypeRole.BODY_SMALL.resolve());
    final List<String> lines = wrap(text, fm, PLAIN_MAX_WIDTH_PX - 2 * PLAIN_H_PAD_PX);
    int textWidth = 0;
    for (final String line : lines) {
      textWidth = Math.max(textWidth, fm.stringWidth(line));
    }
    final int width =
        Math.max(MIN_WIDTH_PX, Math.min(textWidth + 2 * PLAIN_H_PAD_PX, PLAIN_MAX_WIDTH_PX));
    final int height = Math.max(MIN_HEIGHT_PX, lines.size() * fm.getHeight() + 2 * PLAIN_V_PAD_PX);
    return new Dimension(width, height);
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      final int arc = ShapeScale.XS.px() * 2;
      g2.setColor(ColorRole.INVERSE_SURFACE.resolve());
      g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));

      g2.setFont(TypeRole.BODY_SMALL.resolve());
      g2.setColor(ColorRole.INVERSE_ON_SURFACE.resolve());
      final FontMetrics fm = g2.getFontMetrics();
      final List<String> lines = wrap(text, fm, PLAIN_MAX_WIDTH_PX - 2 * PLAIN_H_PAD_PX);
      int blockWidth = 0;
      for (final String line : lines) {
        blockWidth = Math.max(blockWidth, fm.stringWidth(line));
      }
      final int blockX = Math.max(PLAIN_H_PAD_PX, (getWidth() - blockWidth) / 2);
      final int blockHeight = lines.size() * fm.getHeight();
      int y = Math.max(PLAIN_V_PAD_PX, (getHeight() - blockHeight) / 2) + fm.getAscent();
      final boolean rtl = !getComponentOrientation().isLeftToRight();
      for (final String line : lines) {
        final int x = rtl ? blockX + blockWidth - fm.stringWidth(line) : blockX;
        g2.drawString(line, x, y);
        y += fm.getHeight();
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Greedy word wrap against {@code maxWidth}: explicit {@code \n} forces a break, runs of
   * whitespace collapse, and a single word wider than the wrap width hard-breaks by character so
   * pathological input can never blow past the M3 max width.
   *
   * @param text the label text
   * @param fm the metrics of the font the label paints with
   * @param maxWidth the wrap width in pixels
   * @return the wrapped lines, never empty
   */
  static List<String> wrap(final String text, final FontMetrics fm, final int maxWidth) {
    final List<String> lines = new ArrayList<>();
    for (final String paragraph : text.split("\n", -1)) {
      StringBuilder line = new StringBuilder();
      for (final String word : paragraph.trim().split("\\s+")) {
        if (word.isEmpty()) {
          continue;
        }
        final String candidate = line.isEmpty() ? word : line + " " + word;
        if (fm.stringWidth(candidate) <= maxWidth) {
          line = new StringBuilder(candidate);
          continue;
        }
        if (!line.isEmpty()) {
          lines.add(line.toString());
        }
        String rest = word;
        while (fm.stringWidth(rest) > maxWidth && rest.length() > 1) {
          int cut = rest.length() - 1;
          while (cut > 1 && fm.stringWidth(rest.substring(0, cut)) > maxWidth) {
            cut--;
          }
          lines.add(rest.substring(0, cut));
          rest = rest.substring(cut);
        }
        line = new StringBuilder(rest);
      }
      lines.add(line.toString());
    }
    return lines;
  }
}
