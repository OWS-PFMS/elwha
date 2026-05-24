package com.owspfm.elwha.theme.playground;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShapeMorphPainter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual smoke-test for the Phase 1 morph helpers — {@link ShapeMorphPainter}, {@link
 * MorphAnimator}, {@link Easing}, and {@link CornerRadii#interpolate(CornerRadii, CornerRadii,
 * float)}. Click the canvas (or hit <kbd>Space</kbd>) to morph a single rectangle between a fully
 * round pill and a small square, driven by one {@code MorphAnimator} and rendered each frame
 * through {@code ShapeMorphPainter}; the easing curve, duration multiplier, and reduced-motion flag
 * are controlled from the toolbar.
 *
 * <p>No consumer component (no {@code ElwhaButton}, no {@code ElwhaButtonGroup}) is involved —
 * Phase 1 ships helpers only. Per {@code docs/research/elwha-button-anim-design.md} §14, consumer
 * wiring is Phase 2 onward; this playground exists to validate the helper math in isolation.
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.theme.playground.ShapeMorphPlayground"}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ShapeMorphPlayground {

  private ShapeMorphPlayground() {}

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
    SwingUtilities.invokeLater(ShapeMorphPlayground::buildAndShow);
  }

  private static void buildAndShow() {
    final JFrame frame = new JFrame("ShapeMorphPainter / MorphAnimator / Easing — smoke-test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final MorphCanvas canvas = new MorphCanvas();

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));

    toolbar.add(new JLabel("Easing:"));
    final EasingPreset[] easings = {
      new EasingPreset("STANDARD", Easing.STANDARD),
      new EasingPreset("STANDARD_DECELERATE", Easing.STANDARD_DECELERATE),
      new EasingPreset("STANDARD_ACCELERATE", Easing.STANDARD_ACCELERATE),
      new EasingPreset("EMPHASIZED", Easing.EMPHASIZED),
      new EasingPreset("EMPHASIZED_DECELERATE", Easing.EMPHASIZED_DECELERATE),
      new EasingPreset("EMPHASIZED_ACCELERATE", Easing.EMPHASIZED_ACCELERATE),
      new EasingPreset("EASE_IN_OUT (symmetric — Elwha-pinned)", Easing.EASE_IN_OUT),
      new EasingPreset("SPRING_SPATIAL_DEFAULT (0.85)", Easing.SPRING_SPATIAL_DEFAULT),
      new EasingPreset("SPRING (0.70 — playful)", Easing.spring(0.70f)),
      new EasingPreset("LINEAR", Easing.LINEAR),
    };
    final JComboBox<EasingPreset> easingBox = new JComboBox<>(easings);
    easingBox.addActionListener(
        e -> canvas.setEasing(((EasingPreset) easingBox.getSelectedItem()).easing));
    toolbar.add(easingBox);

    toolbar.add(new JLabel("Duration:"));
    final DurationPreset[] durations = {
      new DurationPreset("SHORT3 (150 ms — press)", MorphAnimator.SHORT3_MS),
      new DurationPreset("MEDIUM2 (300 ms — select)", MorphAnimator.MEDIUM2_MS),
      new DurationPreset("600 ms — slow", 600),
      new DurationPreset("1500 ms — very slow", 1500),
    };
    final JComboBox<DurationPreset> durationBox = new JComboBox<>(durations);
    durationBox.setSelectedIndex(1);
    durationBox.addActionListener(
        e -> canvas.setDuration(((DurationPreset) durationBox.getSelectedItem()).ms));
    canvas.setDuration(durations[1].ms);

    toolbar.add(durationBox);

    toolbar.add(new JLabel("Multiplier:"));
    final Integer[] multipliers = {1, 2, 5, 10};
    final JComboBox<Integer> multiplierBox = new JComboBox<>(multipliers);
    multiplierBox.addActionListener(
        e -> canvas.setMultiplier((Integer) multiplierBox.getSelectedItem()));
    toolbar.add(multiplierBox);

    final JCheckBox reducedBox = new JCheckBox("Reduced motion");
    reducedBox.addActionListener(e -> MorphAnimator.setReducedMotion(reducedBox.isSelected()));
    toolbar.add(reducedBox);

    final JButton toggle = new JButton("Toggle morph");
    toggle.addActionListener(e -> canvas.toggle());
    toolbar.add(toggle);

    final JLabel help = new JLabel("Click the canvas (or hit Space) to morph round ↔ square.");
    help.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

    frame.add(toolbar, BorderLayout.NORTH);
    frame.add(canvas, BorderLayout.CENTER);
    frame.add(help, BorderLayout.SOUTH);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private record EasingPreset(String label, Easing easing) {
    @Override
    public String toString() {
      return label;
    }
  }

  private record DurationPreset(String label, int ms) {
    @Override
    public String toString() {
      return label;
    }
  }

  private static final class MorphCanvas extends JPanel {

    private static final int BODY_W = 320;
    private static final int BODY_H = 96;
    private static final CornerRadii ROUND = CornerRadii.uniform(BODY_H / 2);
    private static final CornerRadii SQUARE = CornerRadii.uniform(8);

    private final MorphAnimator animator = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);
    private Easing easing = Easing.EMPHASIZED;
    private int baseDurationMs = MorphAnimator.MEDIUM2_MS;
    private int multiplier = 1;
    private boolean towardSquare = true;

    MorphCanvas() {
      setPreferredSize(new Dimension(480, 240));
      setOpaque(true);
      setFocusable(true);
      addMouseListener(
          new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(final java.awt.event.MouseEvent e) {
              requestFocusInWindow();
              toggle();
            }
          });
      getInputMap()
          .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, 0), "toggle");
      getActionMap()
          .put(
              "toggle",
              new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(final java.awt.event.ActionEvent e) {
                  toggle();
                }
              });
    }

    void setEasing(final Easing easing) {
      this.easing = easing;
      repaint();
    }

    void setDuration(final int ms) {
      this.baseDurationMs = ms;
      animator.setDurationMs(ms * multiplier);
    }

    void setMultiplier(final int multiplier) {
      this.multiplier = multiplier;
      animator.setDurationMs(baseDurationMs * multiplier);
    }

    void toggle() {
      towardSquare = !towardSquare;
      if (towardSquare) {
        animator.start();
      } else {
        animator.reverse();
      }
    }

    @Override
    protected void paintComponent(final Graphics g) {
      super.paintComponent(g);
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorRole.SURFACE.resolve());
        g2.fillRect(0, 0, getWidth(), getHeight());
        final int bodyX = (getWidth() - BODY_W) / 2;
        final int bodyY = (getHeight() - BODY_H) / 2;
        g2.translate(bodyX, bodyY);
        ShapeMorphPainter.paint(
            g2,
            BODY_W,
            BODY_H,
            ROUND,
            SQUARE,
            animator.progress(),
            easing,
            ColorRole.PRIMARY,
            null,
            null,
            0f);
        g2.translate(-bodyX, -bodyY);

        g2.setColor(new Color(0x000000, false));
        g2.drawString(
            String.format(
                "progress = %.3f   eased = %.3f   running = %s   reducedMotion = %s",
                animator.progress(),
                easing.ease(animator.progress()),
                animator.isRunning(),
                MorphAnimator.isReducedMotion()),
            12,
            18);
      } finally {
        g2.dispose();
      }
    }
  }
}
