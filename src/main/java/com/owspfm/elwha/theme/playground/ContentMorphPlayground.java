package com.owspfm.elwha.theme.playground;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ContentMorphPainter;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeMorphPainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/**
 * Visual smoke-test for the {@link ContentMorphPainter} primitives — {@link
 * ContentMorphPainter#iconX(int, int, float) iconX}, {@link ContentMorphPainter#labelAlpha(float)
 * labelAlpha}, and {@link ContentMorphPainter#containerWidth(int, int, float) containerWidth}. A
 * slider drives raw progress; the canvas renders a mock FAB-style body whose width interpolates
 * between two endpoints, a circle "icon" whose X interpolates between the Standard centered
 * position and the Extended leading-inset position, and a label whose alpha follows the 0.5
 * cross-fade inflection — exactly the choreography {@code ElwhaFab} runs through these helpers.
 *
 * <p>No consumer component is involved — this playground validates the helper math in isolation and
 * serves as a reference for future consumers (Navigation Rail expand / collapse, future
 * scroll-collapse utility per FAB design doc §1).
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.theme.playground.ContentMorphPlayground"}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ContentMorphPlayground {

  private ContentMorphPlayground() {}

  /**
   * Entry point — installs a baseline LIGHT theme and shows the smoketest frame.
   *
   * @param args ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(ContentMorphPlayground::buildAndShow);
  }

  private static void buildAndShow() {
    final JFrame frame = new JFrame("ContentMorphPainter — smoke-test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final MorphCanvas canvas = new MorphCanvas();

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    toolbar.add(new JLabel("Progress:"));
    final JSlider progressSlider = new JSlider(0, 1000, 0);
    progressSlider.setPreferredSize(new Dimension(360, 32));
    progressSlider.addChangeListener(e -> canvas.setProgress(progressSlider.getValue() / 1000f));
    toolbar.add(progressSlider);

    final JLabel help =
        new JLabel(
            "Drag the slider to scrub Standard ↔ Extended. Eased through EMPHASIZED; "
                + "label fade inflection = 0.5.");
    help.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

    frame.add(toolbar, BorderLayout.NORTH);
    frame.add(canvas, BorderLayout.CENTER);
    frame.add(help, BorderLayout.SOUTH);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static final class MorphCanvas extends JPanel {

    private static final int BODY_H = 56;
    private static final int STANDARD_W = 56;
    private static final int EXTENDED_W = 200;
    private static final int LEADING_INSET = 16;
    private static final int GAP = 8;
    private static final int ICON_DIAMETER = 24;
    private static final int LABEL_SAMPLE_WIDTH_PX = 96;
    private static final String LABEL_TEXT = "Extended";

    private float rawProgress = 0f;

    MorphCanvas() {
      setPreferredSize(new Dimension(520, 240));
      setOpaque(true);
    }

    void setProgress(final float p) {
      this.rawProgress = p;
      repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
      super.paintComponent(g);
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorRole.SURFACE.resolve());
        g2.fillRect(0, 0, getWidth(), getHeight());

        final float eased = Easing.EMPHASIZED.ease(rawProgress);

        final int bodyW = ContentMorphPainter.containerWidth(STANDARD_W, EXTENDED_W, eased);
        final int bodyX = (getWidth() - EXTENDED_W) / 2;
        final int bodyY = (getHeight() - BODY_H) / 2;

        g2.translate(bodyX, bodyY);

        final CornerRadii radii = CornerRadii.uniform(BODY_H / 2);
        ShapeMorphPainter.paint(
            g2,
            bodyW,
            BODY_H,
            radii,
            radii,
            eased,
            Easing.LINEAR,
            ColorRole.PRIMARY_CONTAINER,
            null,
            null,
            0f);

        final int standardIconX = (bodyW - ICON_DIAMETER) / 2;
        final int extendedIconX = LEADING_INSET;
        final int iconX = ContentMorphPainter.iconX(standardIconX, extendedIconX, eased);
        final int iconY = (BODY_H - ICON_DIAMETER) / 2;

        g2.setColor(ColorRole.ON_PRIMARY_CONTAINER.resolve());
        g2.fillOval(iconX, iconY, ICON_DIAMETER, ICON_DIAMETER);

        final float labelAlpha = ContentMorphPainter.labelAlpha(eased);
        if (labelAlpha > 0f) {
          final Graphics2D gl = (Graphics2D) g2.create();
          try {
            gl.clipRect(0, 0, bodyW, BODY_H);
            gl.setComposite(AlphaComposite.SrcOver.derive(labelAlpha));
            gl.setColor(ColorRole.ON_PRIMARY_CONTAINER.resolve());
            final Font font = getFont().deriveFont(Font.BOLD, 14f);
            gl.setFont(font);
            final FontMetrics fm = gl.getFontMetrics();
            final int labelX = LEADING_INSET + ICON_DIAMETER + GAP;
            final int baseline = BODY_H / 2 + (fm.getAscent() - fm.getDescent()) / 2;
            gl.drawString(LABEL_TEXT, labelX, baseline);
          } finally {
            gl.dispose();
          }
        }

        g2.translate(-bodyX, -bodyY);

        g2.setColor(new Color(0x000000, false));
        g2.drawString(
            String.format("raw progress = %.3f   eased (EMPHASIZED) = %.3f", rawProgress, eased),
            12,
            18);
        g2.drawString(
            String.format(
                "containerWidth(%d, %d, eased) = %d   iconX = %d   labelAlpha = %.3f",
                STANDARD_W, EXTENDED_W, bodyW, iconX, labelAlpha),
            12,
            36);
        // Sample-width readout makes the helper's API surface obvious to a reader scanning the
        // playground — the static StateLayer reference keeps the import live so the import block
        // mirrors ShapeMorphPlayground's "everything theme-side is reachable" pattern.
        g2.drawString(
            String.format(
                "label sample width = %d px   (state-layer overlay = %s)",
                LABEL_SAMPLE_WIDTH_PX, StateLayer.HOVER),
            12,
            54);
      } finally {
        g2.dispose();
      }
    }
  }
}
