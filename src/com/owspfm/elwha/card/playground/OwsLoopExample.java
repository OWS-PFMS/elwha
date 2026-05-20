package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.card.CollapseRule;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardChevron;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardLeadingIcon;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * OWS Loop card pattern: a real-world composition the playground showcases as a "what a non-trivial
 * consumer card looks like" reference.
 *
 * <p>Collapsed view (always visible): {@link ElwhaCardHeader} with a {@code start} leading icon,
 * the loop's reinforcement score as the title, a chevron in trailing; plus a horizontal {@link
 * ElwhaChip} strip in the spatial-media position (chips are not embedded in {@link
 * com.owspfm.elwha.card.ElwhaCardMedia} — that primitive is restricted to image / painter content
 * by spec §5.2). Expanded view (collapsible body): a painted node-and-arrow cycle diagram — not
 * chips, since the original OWS card paints its diagram directly onto a canvas.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class OwsLoopExample {

  private OwsLoopExample() {
    // builder-only
  }

  /**
   * Builds a fully-composed example card.
   *
   * @return the card
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaCard build() {
    final ElwhaCard card = ElwhaCard.elevatedCard().setCollapsible(true);

    final ElwhaCardLeadingIcon lead = new ElwhaCardLeadingIcon(MaterialIcons.start(24));
    lead.setColorRole(ColorRole.ERROR);
    final ElwhaCardHeader header = new ElwhaCardHeader().setLeading(lead).setTitle("1.3836");
    header.getTitle().setFont(TypeRole.TITLE_MEDIUM.resolve());
    header.addTrailing(new ElwhaCardChevron(card));
    card.add(header);
    card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);

    final JPanel chipStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    chipStrip.setOpaque(false);
    chipStrip.add(loopChip("CLI", new DotIcon(), new BarsIcon(3)));
    chipStrip.add(transitionGlyph(true));
    chipStrip.add(loopChip("LAD", new HexIcon(), new BarsIcon(2)));
    chipStrip.add(transitionGlyph(true));
    chipStrip.add(loopChip("SML", new DotIcon(), new BarsIcon(3)));
    chipStrip.add(transitionGlyph(false));
    chipStrip.add(loopChip("TER", new DotIcon(), new BarsIcon(2)));
    chipStrip.add(transitionGlyph(true));
    chipStrip.add(loopChip("GOV", new DotIcon(), new BarsIcon(3)));
    chipStrip.add(transitionGlyph(true));
    chipStrip.add(loopChip("COP", new HexIcon(), new BarsIcon(2)));
    chipStrip.add(transitionGlyph(true));
    chipStrip.add(loopChip("BIO", new DotIcon(), new BarsIcon(3)));
    chipStrip.add(transitionGlyph(true));
    chipStrip.add(new JLabel(MaterialIcons.rotateLeft(18)));
    card.add(chipStrip);
    card.setCollapseConstraint(chipStrip, CollapseRule.ALWAYS_VISIBLE);

    card.add(Box.createVerticalStrut(8));
    card.add(new LoopDiagram());

    return card;
  }

  private static ElwhaChip loopChip(final String label, final Icon leading, final Icon trailing) {
    final ElwhaChip chip = new ElwhaChip(label).setVariant(ChipVariant.OUTLINED);
    chip.setShape(ShapeScale.XL);
    chip.setLeadingIcon(leading);
    chip.setTrailingIcon(trailing, null, () -> {});
    return chip;
  }

  private static JLabel transitionGlyph(final boolean positive) {
    return new JLabel(positive ? MaterialIcons.keyboardTab(16) : MaterialIcons.start(16));
  }

  /** Small filled circle — node-type indicator in the loop chip. */
  static final class DotIcon implements Icon {
    private static final int SIZE = 10;

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
        g2.fillOval(x, y + 2, SIZE - 2, SIZE - 2);
      } finally {
        g2.dispose();
      }
    }

    @Override
    public int getIconWidth() {
      return SIZE;
    }

    @Override
    public int getIconHeight() {
      return SIZE;
    }
  }

  /** Small filled hexagon — alternate node-type indicator in the loop chip. */
  static final class HexIcon implements Icon {
    private static final int W = 11;
    private static final int H = 10;

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
        final int yy = y + 2;
        final int[] xp = {x + 2, x + W - 2, x + W, x + W - 2, x + 2, x};
        final int[] yp = {yy, yy, yy + H / 2, yy + H, yy + H, yy + H / 2};
        g2.fillPolygon(new Polygon(xp, yp, 6));
      } finally {
        g2.dispose();
      }
    }

    @Override
    public int getIconWidth() {
      return W;
    }

    @Override
    public int getIconHeight() {
      return H + 2;
    }
  }

  /** N stacked short horizontal bars — trailing affordance ({@code ≡} for 3, {@code =} for 2). */
  static final class BarsIcon implements Icon {
    private static final int W = 12;
    private final int bars;

    BarsIcon(final int bars) {
      this.bars = bars;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
        final int barH = 2;
        final int gap = 2;
        final int totalH = bars * barH + (bars - 1) * gap;
        int yy = y + Math.max(0, (getIconHeight() - totalH) / 2);
        for (int i = 0; i < bars; i++) {
          g2.fillRect(x, yy, W, barH);
          yy += barH + gap;
        }
      } finally {
        g2.dispose();
      }
    }

    @Override
    public int getIconWidth() {
      return W;
    }

    @Override
    public int getIconHeight() {
      return 12;
    }
  }

  /** Painted node-and-arrow cycle for the OWS Loop card's expanded body. */
  static final class LoopDiagram extends JComponent {

    private static final String[] NODES = {"CLI", "LAD", "SML", "TER", "GOV", "COP", "BIO"};
    private static final boolean[] IS_HEX = {false, true, false, false, false, true, false};
    private static final boolean[] POSITIVE_EDGE = {true, true, false, true, true, true, true};

    private static final int NODE_W = 64;
    private static final int NODE_H = 26;
    private static final int NODE_ARC = 8;

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(0, 320);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        final int w = getWidth();
        final int h = getHeight();
        final double cx = w / 2.0;
        final double cy = h / 2.0;
        final double rx = Math.max(80, w / 2.0 - NODE_W);
        final double ry = Math.max(60, h / 2.0 - NODE_H);

        final int n = NODES.length;
        final double[] nx = new double[n];
        final double[] ny = new double[n];
        for (int i = 0; i < n; i++) {
          final double a = -Math.PI / 2 + (2 * Math.PI * i) / n;
          nx[i] = cx + rx * Math.cos(a);
          ny[i] = cy + ry * Math.sin(a);
        }

        final Color green = new Color(0x2E7D32);
        final Color red = new Color(0xC62828);

        for (int i = 0; i < n; i++) {
          final int j = (i + 1) % n;
          final Color c = POSITIVE_EDGE[i] ? green : red;
          paintArc(g2, nx[i], ny[i], nx[j], ny[j], cx, cy, c);
        }

        final Font nodeFont = getFont().deriveFont(Font.PLAIN, 12f);
        g2.setFont(nodeFont);
        final FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
          final int x = (int) Math.round(nx[i]) - NODE_W / 2;
          final int y = (int) Math.round(ny[i]) - NODE_H / 2;
          g2.setColor(ColorRole.SURFACE.resolve());
          g2.fillRoundRect(x, y, NODE_W, NODE_H, NODE_ARC, NODE_ARC);
          g2.setColor(ColorRole.OUTLINE.resolve());
          g2.drawRoundRect(x, y, NODE_W, NODE_H, NODE_ARC, NODE_ARC);

          final Icon leading = IS_HEX[i] ? new HexIcon() : new DotIcon();
          final int iconY = y + (NODE_H - leading.getIconHeight()) / 2;
          leading.paintIcon(this, g2, x + 8, iconY);

          g2.setColor(ColorRole.ON_SURFACE.resolve());
          final int textX = x + 8 + leading.getIconWidth() + 6;
          final int textY = y + (NODE_H - fm.getHeight()) / 2 + fm.getAscent();
          g2.drawString(NODES[i], textX, textY);
        }
      } finally {
        g2.dispose();
      }
    }

    private void paintArc(
        final Graphics2D g2,
        final double x1,
        final double y1,
        final double x2,
        final double y2,
        final double cx,
        final double cy,
        final Color color) {
      final double mx = (x1 + x2) / 2.0;
      final double my = (y1 + y2) / 2.0;
      final double vx = mx - cx;
      final double vy = my - cy;
      final double len = Math.max(1, Math.hypot(vx, vy));
      final double bulge = 22.0;
      final double ctrlX = mx + (vx / len) * bulge;
      final double ctrlY = my + (vy / len) * bulge;

      final double inset = 18.0;
      final double dx1 = ctrlX - x1;
      final double dy1 = ctrlY - y1;
      final double l1 = Math.max(1, Math.hypot(dx1, dy1));
      final double sx = x1 + (dx1 / l1) * inset;
      final double sy = y1 + (dy1 / l1) * inset;
      final double dx2 = x2 - ctrlX;
      final double dy2 = y2 - ctrlY;
      final double l2 = Math.max(1, Math.hypot(dx2, dy2));
      final double ex = x2 - (dx2 / l2) * inset;
      final double ey = y2 - (dy2 / l2) * inset;

      g2.setColor(color);
      g2.setStroke(new java.awt.BasicStroke(2.5f));
      g2.draw(new QuadCurve2D.Double(sx, sy, ctrlX, ctrlY, ex, ey));

      final double tdx = ex - ctrlX;
      final double tdy = ey - ctrlY;
      final double tl = Math.max(1, Math.hypot(tdx, tdy));
      final double ux = tdx / tl;
      final double uy = tdy / tl;
      final double headLen = 8.0;
      final double headW = 5.0;
      final int[] hx = {
        (int) Math.round(ex),
        (int) Math.round(ex - ux * headLen - uy * headW),
        (int) Math.round(ex - ux * headLen + uy * headW)
      };
      final int[] hy = {
        (int) Math.round(ey),
        (int) Math.round(ey - uy * headLen + ux * headW),
        (int) Math.round(ey - uy * headLen - ux * headW)
      };
      g2.fillPolygon(hx, hy, 3);
    }
  }
}
