package com.owspfm.elwha.theme.playground;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Visual smoke-test for {@link RipplePainter}. Click anywhere inside the central round-rect canvas
 * to seed a ripple at the click point; the painter animates a 250 ms expand + 150 ms fade tail (400
 * ms total) clipped to the canvas's rounded outline. Side-by-side comparable to the existing {@code
 * ElwhaCard} ripple by eye — the painter is a verbatim extraction.
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.theme.playground.RipplePainterDemo"}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class RipplePainterDemo {

  private static final int RIPPLE_TOTAL_MS = 400;

  private RipplePainterDemo() {}

  public static void main(String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(
        () -> {
          final JFrame frame = new JFrame("RipplePainter — visual smoke-test");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.setLayout(new BorderLayout());

          final RippleCanvas canvas = new RippleCanvas(360, 200, 24);

          final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
          controls.add(new JLabel("Mode:"));
          final JComboBox<Mode> modeBox = new JComboBox<>(new Mode[] {Mode.LIGHT, Mode.DARK});
          modeBox.addActionListener(
              e -> {
                ElwhaTheme.install(ElwhaTheme.current().withMode((Mode) modeBox.getSelectedItem()));
                canvas.repaint();
              });
          controls.add(modeBox);
          controls.add(new JLabel("Corner radius:"));
          final JComboBox<Integer> arcBox = new JComboBox<>(new Integer[] {0, 8, 16, 24, 999});
          arcBox.setSelectedItem(24);
          arcBox.addActionListener(
              e -> {
                canvas.setArc((Integer) arcBox.getSelectedItem());
                canvas.repaint();
              });
          controls.add(arcBox);

          final JLabel help =
              new JLabel("Click inside the canvas to seed a ripple at the click point.");
          help.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

          frame.add(controls, BorderLayout.NORTH);
          frame.add(canvas, BorderLayout.CENTER);
          frame.add(help, BorderLayout.SOUTH);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static final class RippleCanvas extends JPanel {

    private int arc;
    private Point rippleOrigin;
    private float rippleProgress = 1f;
    private Timer rippleTimer;

    RippleCanvas(int width, int height, int arc) {
      this.arc = arc;
      setOpaque(false);
      setPreferredSize(new Dimension(width + 80, height + 80));
      setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
              startRipple(e.getPoint());
            }
          });
    }

    void setArc(int arc) {
      this.arc = arc;
    }

    private void startRipple(Point origin) {
      this.rippleOrigin = origin;
      this.rippleProgress = 0f;
      if (rippleTimer != null && rippleTimer.isRunning()) {
        rippleTimer.stop();
      }
      final long startNanos = System.nanoTime();
      rippleTimer =
          new Timer(
              16,
              e -> {
                rippleProgress =
                    Math.min(1f, (System.nanoTime() - startNanos) / (RIPPLE_TOTAL_MS * 1_000_000f));
                repaint();
                if (rippleProgress >= 1f) {
                  ((Timer) e.getSource()).stop();
                }
              });
      rippleTimer.setRepeats(true);
      rippleTimer.setInitialDelay(0);
      rippleTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      final int insetX = 40;
      final int insetY = 40;
      final int bodyW = getWidth() - 2 * insetX;
      final int bodyH = getHeight() - 2 * insetY;
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(insetX, insetY);
        SurfacePainter.paint(
            g2,
            bodyW,
            bodyH,
            arc,
            ColorRole.SURFACE_CONTAINER,
            null,
            ColorRole.OUTLINE_VARIANT,
            1f);
        final Color tint = ColorRole.ON_SURFACE.resolve();
        if (rippleOrigin != null) {
          final Point local = new Point(rippleOrigin.x - insetX, rippleOrigin.y - insetY);
          RipplePainter.paint(g2, bodyW, bodyH, local, rippleProgress, arc, tint);
        }
      } finally {
        g2.dispose();
      }
    }
  }
}
