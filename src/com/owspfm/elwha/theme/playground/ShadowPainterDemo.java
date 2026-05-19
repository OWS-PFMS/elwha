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
 * Visual smoke-test for {@link ShadowPainter}. Renders 5 elevation levels side-by-side at a
 * button-like pill geometry (180×56) — wider-than-tall so the FULL arc setting produces a proper
 * capsule, not a degenerate circle. Body fill paints {@link ColorRole#SURFACE_CONTAINER} on a
 * {@link ColorRole#SURFACE} backdrop, so the elevation contrast (M3 tint model) reads in both light
 * and dark modes. The microbench panel exercises the cache-vs-cold paint timing claim from issue
 * #115 (cached 9-slice paint ≥10× faster than ConvolveOp recompute).
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
   * 5-cell row showing levels 1..5. Body geometry is a button-like pill (180×56) so the FULL arc
   * setting produces a proper capsule shape — matches the geometry consumers (ElwhaButton) will
   * use. Body fill is {@link ColorRole#SURFACE_CONTAINER} on a {@link ColorRole#SURFACE}-painted
   * backdrop so the elevation contrast reads in both light and dark modes (the M3 elevation tint
   * model — elevated surfaces step up one container role from the surrounding plane).
   */
  private static final class ElevationGallery extends JPanel {

    private int arc = 12;

    ElevationGallery() {
      super(new GridLayout(1, 5, 24, 0));
      setBorder(BorderFactory.createEmptyBorder(40, 24, 40, 24));
      setOpaque(true);
      for (int level = 1; level <= 5; level++) {
        add(new ShadowCell(level));
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      // In dark mode the spec'd SURFACE is near-black and black shadows disappear into it (which
      // is correct M3 behavior — dark elevation reads through surface tint, not shadow). The demo
      // intentionally overrides with a mid-gray backdrop in dark mode so the shadow is visible for
      // smoketest verification, even though that's not how a real M3 dark UI would look.
      final boolean dark = ElwhaTheme.current().mode().resolved() == Mode.DARK;
      g.setColor(dark ? new java.awt.Color(80, 80, 86) : ColorRole.SURFACE.resolve());
      g.fillRect(0, 0, getWidth(), getHeight());
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

      private static final int BODY_W = 180;
      private static final int BODY_H = 56;

      private final int elevation;
      private int arc = ElevationGallery.this.arc;

      ShadowCell(int elevation) {
        this.elevation = elevation;
        setOpaque(false);
        setPreferredSize(new Dimension(BODY_W + 40, BODY_H + 80));
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
          final int bodyX = (getWidth() - BODY_W) / 2;
          final int bodyY = (getHeight() - BODY_H - 24) / 2;
          g2.translate(bodyX, bodyY);
          ShadowPainter.paint(g2, BODY_W, BODY_H, arc, elevation);
          SurfacePainter.paint(
              g2, BODY_W, BODY_H, arc, surfaceRoleForElevation(elevation), null, null, 0f);
        } finally {
          g2.dispose();
        }
      }

      private static ColorRole surfaceRoleForElevation(int elevation) {
        // M3 elevation tint map — in dark mode the elevation signal reads primarily through
        // surface tint (each level steps one container role up the brightness scale), with the
        // shadow as a supporting layer. In light mode the tint stepping is subtle but the shadow
        // carries the contrast against the bright backdrop.
        return switch (elevation) {
          case 1 -> ColorRole.SURFACE_CONTAINER_LOW;
          case 2 -> ColorRole.SURFACE_CONTAINER;
          case 3 -> ColorRole.SURFACE_CONTAINER_HIGH;
          case 4 -> ColorRole.SURFACE_CONTAINER_HIGHEST;
          default -> ColorRole.SURFACE_CONTAINER_HIGHEST;
        };
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
