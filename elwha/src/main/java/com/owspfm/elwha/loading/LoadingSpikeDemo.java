package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Interactive demo for the S1 (spike) shape-morph engine (story #514): the {@link
 * ElwhaLoadingIndicator} skeleton beside a gallery of the eight catalog shapes and a
 * SoftBurst→Cookie9 morph filmstrip — the visual proof that the radius-profile engine builds
 * faithful shapes and morphs them seamlessly. Not a test; run by hand.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingSpikeDemo {

  private LoadingSpikeDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(
        () -> {
          final JFrame frame = new JFrame("ElwhaLoadingIndicator — S1 spike");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

          final JPanel root = new JPanel(new GridLayout(3, 1, 0, 16));
          root.setBorder(new EmptyBorder(24, 24, 24, 24));

          final JPanel live = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
          final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
          indicator.setIndicatorSize(64);
          live.add(indicator);
          root.add(live);

          final RoundedPolygonShape[] all = {
            LoadingShapes.CIRCLE,
            LoadingShapes.SUNNY,
            LoadingShapes.COOKIE_9,
            LoadingShapes.COOKIE_4,
            LoadingShapes.PENTAGON,
            LoadingShapes.PILL,
            LoadingShapes.OVAL,
            LoadingShapes.SOFT_BURST
          };
          final JPanel shapes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
          for (final RoundedPolygonShape s : all) {
            shapes.add(swatch(s.radii(), 56));
          }
          root.add(shapes);

          final JPanel morph = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
          for (int i = 0; i <= 6; i++) {
            final float t = i / 6f;
            morph.add(
                swatch(
                    ShapeMorph.lerp(
                        LoadingShapes.SOFT_BURST.radii(), LoadingShapes.COOKIE_9.radii(), t),
                    56));
          }
          root.add(morph);

          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static JComponent swatch(final float[] profile, final int size) {
    return new JComponent() {
      @Override
      public java.awt.Dimension getPreferredSize() {
        return new java.awt.Dimension(size, size);
      }

      @Override
      protected void paintComponent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorRole.PRIMARY.resolve());
        final Path2D.Float p =
            ShapeMorph.toPath(profile, size / 2f, size / 2f, size / 2f - 4f, 0.0);
        g2.fill(p);
        g2.dispose();
      }
    };
  }
}
