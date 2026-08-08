package com.owspfm.elwha.radio;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * S3 motion demo (story #419) — two radios driven programmatically so the M3 motion reads clearly:
 * the 300ms emphasized-decelerate dot grow on select, the 50ms fade (no shrink) on deselect, and
 * mid-flight retarget continuity (the "rapid flip" button reverses during the grow). A duration
 * multiplier slider slows everything 1&times;–10&times; for inspection and a reduced-motion
 * checkbox proves the global snap.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonMotionDemo {

  private final JFrame frame = new JFrame("ElwhaRadioButton — S3 motion");

  private ElwhaRadioButtonMotionDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaRadioButtonMotionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaRadioButton left = new ElwhaRadioButton(true);
    final ElwhaRadioButton right = new ElwhaRadioButton();
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 24));
    row.add(left);
    row.add(right);

    final ElwhaButton swap = ElwhaButton.filledTonalButton("Swap selection (animated)");
    swap.addActionListener(
        e -> {
          final boolean leftWas = left.isSelected();
          left.setSelected(!leftWas);
          right.setSelected(leftWas);
        });

    final ElwhaButton rapidFlip = ElwhaButton.outlinedButton("Rapid flip (retarget mid-grow)");
    rapidFlip.addActionListener(
        e -> {
          right.setSelected(true);
          new Timer(
                  90,
                  t -> {
                    right.setSelected(false);
                    ((Timer) t.getSource()).stop();
                  })
              .start();
        });

    final ElwhaCheckbox reduced = new ElwhaCheckbox("Reduced motion (global snap)");
    reduced.setChecked(MorphAnimator.isReducedMotion());
    reduced.addActionListener(e -> MorphAnimator.setReducedMotion(reduced.isChecked()));

    final ElwhaSlider multiplier = new ElwhaSlider(1, 10, 1);
    multiplier.setAccessibleLabel("Duration multiplier");
    multiplier.addChangeListener(e -> MorphAnimator.setDurationMultiplier(multiplier.getValue()));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    controls.add(swap);
    controls.add(rapidFlip);
    controls.add(reduced);
    controls.add(new JLabel("Duration ×"));
    controls.add(multiplier);

    frame.add(row, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setMinimumSize(new java.awt.Dimension(620, 240));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
