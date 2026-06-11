package com.owspfm.elwha.switches;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase-1 / S3 playground (story #404) — {@link ElwhaSwitch} motion: the 300&nbsp;ms overshoot
 * slide (watch the handle settle a couple of px past its rest point), the 250/100&nbsp;ms size
 * morphs, the color crossfade riding the slide, and the drag &rarr; release handoff (scrub the
 * handle partway and let go — it animates home from where you left it, no jump). The motion
 * controls dogfood {@code ElwhaSwitch} itself: one switch slows every morph 5&times; (the
 * Showcase's observation multiplier), one engages global reduced motion (toggles snap). The
 * dogfooded {@link ElwhaButton} flips the hero row programmatically — displayable switches animate
 * on programmatic writes too.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchMotionDemo {

  private final JFrame frame = new JFrame("ElwhaSwitch — Phase 1 / S3 motion");

  private ElwhaSwitchMotionDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(() -> new ElwhaSwitchMotionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSwitch first = new ElwhaSwitch();
    final ElwhaSwitch second = new ElwhaSwitch(true);
    final ElwhaSwitch dragMe = new ElwhaSwitch();

    final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 12));
    rows.setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
    rows.add(row("click me — overshoot slide + color crossfade", first));
    rows.add(row("click me too (starts selected)", second));
    rows.add(row("drag me partway and release — no-jump handoff", dragMe));

    final ElwhaSwitch slowMotion = new ElwhaSwitch();
    slowMotion.addChangeListener(
        e -> MorphAnimator.setDurationMultiplier(slowMotion.isSelected() ? 5f : 1f));
    final ElwhaSwitch reducedMotion = new ElwhaSwitch(MorphAnimator.isReducedMotion());
    reducedMotion.addChangeListener(
        e -> MorphAnimator.setReducedMotion(reducedMotion.isSelected()));
    rows.add(row("slow motion 5× (observation multiplier)", slowMotion));
    rows.add(row("reduced motion — toggles snap", reducedMotion));

    final ElwhaButton flip = ElwhaButton.filledTonalButton("Flip the top pair programmatically");
    flip.addActionListener(
        e -> {
          first.setSelected(!first.isSelected());
          second.setSelected(!second.isSelected());
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    top.add(flip);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rows, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(640, 360));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel row(final String text, final ElwhaSwitch elwhaSwitch) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 0));
    row.add(elwhaSwitch);
    row.add(new JLabel(text));
    return row;
  }
}
