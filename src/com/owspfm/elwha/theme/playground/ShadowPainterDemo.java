package com.owspfm.elwha.theme.playground;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Visual smoke-test for {@link ShadowPainter}. Renders 5 elevation levels side-by-side at the
 * canonical card geometry (matches {@code docs/research/shadow-spike-images/level-{1..5}-light.png}
 * to the eye) plus a microbench panel that exercises the cache-vs-cold paint timing claim from
 * issue #115 (cached 9-slice paint ≥10× faster than ConvolveOp recompute).
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.theme.playground.ShadowPainterDemo"}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ShadowPainterDemo {

  private ShadowPainterDemo() {}

  public static void main(String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(
        () -> {
          final JFrame frame = new JFrame("ShadowPainter — visual smoke-test");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.setLayout(new BorderLayout());

          final JPanel root = new JPanel(new BorderLayout());

          final ElevationGallery gallery = new ElevationGallery();
          root.add(gallery, BorderLayout.CENTER);

          final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
          controls.add(new JLabel("Mode:"));
          final JComboBox<Mode> modeBox = new JComboBox<>(new Mode[] {Mode.LIGHT, Mode.DARK});
          modeBox.addActionListener(
              e -> {
                ElwhaTheme.install(ElwhaTheme.current().withMode((Mode) modeBox.getSelectedItem()));
                gallery.repaint();
              });
          controls.add(modeBox);
          controls.add(new JLabel("Corner radius:"));
          final JComboBox<Integer> arcBox = new JComboBox<>(new Integer[] {8, 12, 16, 24, 32, 999});
          arcBox.setSelectedItem(12);
          arcBox.addActionListener(
              e -> {
                gallery.setArc((Integer) arcBox.getSelectedItem());
                gallery.repaint();
              });
          controls.add(arcBox);
          final JButton bench = new JButton("Run perf microbench");
          final JTextArea benchOut = new JTextArea(7, 70);
          benchOut.setEditable(false);
          benchOut.setLineWrap(false);
          benchOut.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
          bench.addActionListener(e -> benchOut.setText(runBenchmark()));
          controls.add(bench);

          final JPanel south = new JPanel(new BorderLayout());
          south.add(benchOut, BorderLayout.CENTER);

          root.add(controls, BorderLayout.NORTH);
          root.add(south, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  /**
   * 5-cell row showing levels 1..5 at canonical card geometry (280×160, arc 12 by default — same as
   * the spike reference images).
   */
  private static final class ElevationGallery extends JPanel {

    private int arc = 12;

    ElevationGallery() {
      super(new GridLayout(1, 5, 24, 0));
      setBorder(BorderFactory.createEmptyBorder(48, 24, 48, 24));
      for (int level = 1; level <= 5; level++) {
        add(new ShadowCell(level));
      }
    }

    void setArc(int arc) {
      this.arc = arc;
      for (java.awt.Component child : getComponents()) {
        if (child instanceof ShadowCell cell) {
          cell.setArc(arc);
        }
      }
    }

    private final class ShadowCell extends JPanel {

      private final int elevation;
      private int arc = ElevationGallery.this.arc;

      ShadowCell(int elevation) {
        this.elevation = elevation;
        setOpaque(false);
        setPreferredSize(new Dimension(160, 200));
        setLayout(new BorderLayout());
        final JLabel label = new JLabel("level " + elevation, SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(label, BorderLayout.SOUTH);
      }

      void setArc(int arc) {
        this.arc = arc;
      }

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final int bodyW = 120;
          final int bodyH = 120;
          final Insets reserve = ShadowPainter.shadowInsets(elevation);
          final int bodyX = (getWidth() - bodyW) / 2;
          final int bodyY = (getHeight() - bodyH - 24) / 2 - reserve.top / 2;
          g2.translate(bodyX, bodyY);
          ShadowPainter.paint(g2, bodyW, bodyH, arc, elevation);
          SurfacePainter.paint(g2, bodyW, bodyH, arc, ColorRole.SURFACE, null, null, 0f);
        } finally {
          g2.dispose();
        }
      }
    }
  }

  private static String runBenchmark() {
    final int arc = 12;
    final int elevation = 3;
    final int w = 280;
    final int h = 160;
    final int reps = 200;

    ShadowPainter.clearCache();

    // Cold path — direct ConvolveOp render at body size (mirrors the old ElwhaSurface cache pattern
    // that invalidated on every (bodyW, bodyH) change).
    final long coldStart = System.nanoTime();
    for (int i = 0; i < reps; i++) {
      SurfacePainter.renderShadowImage(w + i, h + i, arc, elevation);
    }
    final long coldNanos = System.nanoTime() - coldStart;

    // Warm path — 9-slice paint of the cached canonical image into a throwaway buffer.
    final BufferedImage target = new BufferedImage(w + 40, h + 40, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D tg = target.createGraphics();
    try {
      // Prime the cache with one paint
      tg.translate(20, 20);
      ShadowPainter.paint(tg, w, h, arc, elevation);
      final long warmStart = System.nanoTime();
      for (int i = 0; i < reps; i++) {
        // Use a slightly varying body size each iteration to demonstrate cache reuse across sizes.
        ShadowPainter.paint(tg, w + (i % 50), h + (i % 50), arc, elevation);
      }
      final long warmNanos = System.nanoTime() - warmStart;

      final double coldMs = coldNanos / 1_000_000.0;
      final double warmMs = warmNanos / 1_000_000.0;
      final double speedup = coldMs / Math.max(warmMs, 0.001);
      return String.format(
          "Microbench (arc=%d, elev=%d, %d×%d, %d reps):%n"
              + "  Cold (ConvolveOp per call):  %8.2f ms total  →  %6.3f ms/call%n"
              + "  Warm (9-slice cached draw):  %8.2f ms total  →  %6.3f ms/call%n"
              + "  Speedup: %.1f×%n"
              + "  Cache size: %d entries",
          arc,
          elevation,
          w,
          h,
          reps,
          coldMs,
          coldMs / reps,
          warmMs,
          warmMs / reps,
          speedup,
          ShadowPainter.cacheSize());
    } finally {
      tg.dispose();
    }
  }
}
