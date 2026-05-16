package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.ElwhaCard;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Builds a {@link ElwhaCard} that mimics the OWS cycle-viewer cycle card: chevron + reinforcing
 * glyph + score + factor-pill chain in the header, a ring-diagram + edge-weight summary in the
 * body. Used by {@link GalleryPanel} to demonstrate how a real-world OWS surface composes from the
 * ElwhaCard API.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
final class CycleCardExample {

  private static final Color REINFORCING = new Color(91, 168, 100);
  private static final Color BALANCING = new Color(200, 70, 70);

  private CycleCardExample() {
    // utility
  }

  /**
   * Builds the demo cycle card with the screenshot's BIO→COP→GOV→LAT→TER→AGP1→SML loop.
   *
   * @return a configured {@link ElwhaCard} ready to drop into a parent layout
   * @version v0.1.0
   * @since v0.1.0
   */
  static ElwhaCard build() {
    final String[] factors = {"BIO", "COP", "GOV", "LAT", "TER", "AGP1", "SML"};

    CycleRing ring = new CycleRing(factors);
    ring.setAlignmentX(Component.CENTER_ALIGNMENT);

    return new ElwhaCard("1.4897")
        .setVariant(CardVariant.OUTLINED)
        .setLeadingIcon(new ReinforcingIcon())
        .setSubhead("Reinforcing cycle  ·  7 factors")
        .setTrailingActions(buildChainPanel(factors))
        .setMedia(ring)
        .setCollapsible(true);
  }

  // ------------------------------------------------------------- chain row

  private static JComponent buildChainPanel(final String[] factors) {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
    row.setOpaque(false);
    for (int i = 0; i < factors.length; i++) {
      row.add(factorPill(factors[i]));
      row.add(arrow(true));
    }
    JLabel loop = new JLabel("↵");
    loop.setForeground(REINFORCING);
    row.add(loop);
    return row;
  }

  private static JComponent factorPill(final String abbr) {
    JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
    pill.setOpaque(false);
    Color border = UIManager.getColor("Component.borderColor");
    if (border == null) {
      border = new Color(180, 180, 180);
    }
    pill.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            BorderFactory.createEmptyBorder(1, 6, 1, 6)));
    JLabel grid = new JLabel("▦");
    grid.putClientProperty("FlatLaf.styleClass", "small");
    pill.add(grid);
    JLabel name = new JLabel(abbr);
    name.putClientProperty("FlatLaf.styleClass", "small");
    pill.add(name);
    JLabel menu = new JLabel("≡");
    menu.putClientProperty("FlatLaf.styleClass", "small");
    pill.add(menu);
    return pill;
  }

  private static JLabel arrow(final boolean reinforcing) {
    JLabel l = new JLabel(reinforcing ? "→⁺" : "→⁻");
    l.setForeground(reinforcing ? REINFORCING : BALANCING);
    return l;
  }

  // ------------------------------------------------------------- icons

  /** Green circular ↻ glyph used as the leading icon — reinforcing-cycle marker. */
  private static final class ReinforcingIcon implements Icon {
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(REINFORCING);
        g2.setStroke(new BasicStroke(1.7f));
        g2.drawArc(x + 1, y + 2, 14, 14, 60, 280);
        // arrow head at start of arc
        double headX = x + 1 + 7 + 7 * Math.cos(Math.toRadians(60));
        double headY = y + 2 + 7 - 7 * Math.sin(Math.toRadians(60));
        Path2D.Double tip = new Path2D.Double();
        tip.moveTo(headX, headY);
        tip.lineTo(headX - 3, headY - 4);
        tip.lineTo(headX + 4, headY - 2);
        tip.closePath();
        g2.fill(tip);
      } finally {
        g2.dispose();
      }
    }

    @Override
    public int getIconWidth() {
      return 18;
    }

    @Override
    public int getIconHeight() {
      return 18;
    }
  }

  // ------------------------------------------------------------- ring

  /**
   * Custom component that paints the factors arranged on a circle with reinforcing-green arrows
   * looping between consecutive members. Mirrors the cycle-viewer's ring diagram.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final class CycleRing extends JComponent {
    private final String[] factors;
    private static final int PILL_PAD_X = 7;
    private static final int PILL_PAD_Y = 4;
    private static final int PILL_ARC = 8;
    private static final int RING_MARGIN = 56;
    private static final int CURVE_BOW = 28;

    CycleRing(final String[] factors) {
      this.factors = factors;
      setPreferredSize(new Dimension(420, 320));
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.max(60, Math.min(w, h) / 2.0 - RING_MARGIN);
        int n = factors.length;

        Point2D.Double[] centers = new Point2D.Double[n];
        for (int i = 0; i < n; i++) {
          double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
          centers[i] =
              new Point2D.Double(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle));
        }

        g2.setColor(REINFORCING);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n; i++) {
          int j = (i + 1) % n;
          drawCurvedArrow(g2, centers[i], centers[j], cx, cy);
        }

        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
          drawPill(g2, factors[i], centers[i].x, centers[i].y, fm);
        }
      } finally {
        g2.dispose();
      }
    }

    private void drawPill(
        final Graphics2D g2,
        final String text,
        final double cx,
        final double cy,
        final FontMetrics fm) {
      Color surface = UIManager.getColor("Panel.background");
      if (surface == null) {
        surface = Color.WHITE;
      }
      Color border = UIManager.getColor("Component.borderColor");
      if (border == null) {
        border = new Color(180, 180, 180);
      }
      Color fg = UIManager.getColor("Label.foreground");
      if (fg == null) {
        fg = Color.DARK_GRAY;
      }

      int textWidth = fm.stringWidth(text);
      int pillWidth = textWidth + 2 * PILL_PAD_X;
      int pillHeight = fm.getHeight() + 2 * PILL_PAD_Y;
      int x = (int) Math.round(cx - pillWidth / 2.0);
      int y = (int) Math.round(cy - pillHeight / 2.0);
      g2.setColor(blendTowardWhite(surface));
      g2.fillRoundRect(x, y, pillWidth, pillHeight, PILL_ARC, PILL_ARC);
      g2.setColor(border);
      g2.drawRoundRect(x, y, pillWidth - 1, pillHeight - 1, PILL_ARC, PILL_ARC);
      g2.setColor(fg);
      g2.drawString(text, x + PILL_PAD_X, y + pillHeight - PILL_PAD_Y - fm.getDescent());
    }

    private static Color blendTowardWhite(final Color c) {
      int r = (int) (c.getRed() * 0.4 + 255 * 0.6);
      int g = (int) (c.getGreen() * 0.4 + 255 * 0.6);
      int b = (int) (c.getBlue() * 0.4 + 255 * 0.6);
      return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private void drawCurvedArrow(
        final Graphics2D g2,
        final Point2D.Double from,
        final Point2D.Double to,
        final double cx,
        final double cy) {
      Point2D.Double start = pullToward(from, to, 22);
      Point2D.Double end = pullToward(to, from, 22);

      double mx = (from.x + to.x) / 2.0;
      double my = (from.y + to.y) / 2.0;
      double dx = mx - cx;
      double dy = my - cy;
      double len = Math.hypot(dx, dy);
      if (len < 1e-6) {
        return;
      }
      double cpx = mx + dx / len * CURVE_BOW;
      double cpy = my + dy / len * CURVE_BOW;

      QuadCurve2D.Double curve = new QuadCurve2D.Double(start.x, start.y, cpx, cpy, end.x, end.y);
      g2.draw(curve);

      double angle = Math.atan2(end.y - cpy, end.x - cpx);
      drawArrowhead(g2, end.x, end.y, angle);
    }

    private static Point2D.Double pullToward(
        final Point2D.Double point, final Point2D.Double target, final double distance) {
      double dx = target.x - point.x;
      double dy = target.y - point.y;
      double len = Math.hypot(dx, dy);
      if (len < 1e-6) {
        return point;
      }
      return new Point2D.Double(point.x + dx / len * distance, point.y + dy / len * distance);
    }

    private static void drawArrowhead(
        final Graphics2D g2, final double x, final double y, final double angle) {
      double size = 9;
      Path2D.Double arrow = new Path2D.Double();
      arrow.moveTo(x, y);
      arrow.lineTo(
          x - size * Math.cos(angle - Math.PI / 7), y - size * Math.sin(angle - Math.PI / 7));
      arrow.lineTo(
          x - size * Math.cos(angle + Math.PI / 7), y - size * Math.sin(angle + Math.PI / 7));
      arrow.closePath();
      g2.fill(arrow);
    }
  }
}
