package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #266 (S6) smoketest — entrance / exit motion. Opening a dialog fades the scrim 0 → 32%
 * while the container scales 0.80 → 1.0 about its center and fades in, eased emphasized-decelerate
 * over 300 ms (design doc §13); closing reverses it, then the overlay detaches. The "reduced
 * motion" toggle flips {@link MorphAnimator#setReducedMotion(boolean)} so the next open/close snaps
 * straight to the end state with no in-between frames; the "slow-mo" toggle stretches the duration
 * 5× so the curve is easy to watch.
 *
 * <p>Validate: with motion on, the dialog grows + fades in and the scrim darkens together; closing
 * (Esc / scrim / a button) shrinks + fades it back out before it disappears. With reduced motion
 * on, both open and close are instant. Slow-mo makes the scale-in plainly visible.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogMotionDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.3.0
 */
public final class DialogMotionDemo {

  private final JFrame frame = new JFrame("ElwhaDialog — S6 entrance/exit motion (#266)");

  private DialogMotionDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new DialogMotionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JCheckBox reduced = new JCheckBox("reduced motion");
    reduced.addActionListener(e -> MorphAnimator.setReducedMotion(reduced.isSelected()));
    final JCheckBox slow = new JCheckBox("slow-mo (5×)");
    slow.addActionListener(e -> MorphAnimator.setDurationMultiplier(slow.isSelected() ? 5f : 1f));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    controls.add(new JLabel("Motion:"));
    controls.add(reduced);
    controls.add(slow);

    final ElwhaButton open = ElwhaButton.filledButton("Open dialog (watch it scale + fade in)");
    open.addActionListener(e -> openDialog());

    final JPanel center = new JPanel(new GridLayout(0, 1, 0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(48, 64, 48, 64));
    center.add(open);

    frame.add(controls, BorderLayout.NORTH);
    frame.add(center, BorderLayout.CENTER);
    frame.setSize(720, 460);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openDialog() {
    final ElwhaButton ok = ElwhaButton.filledButton("OK");
    final ElwhaButton cancel = ElwhaButton.textButton("Cancel");
    ElwhaDialog.builder()
        .headline("Enable notifications?")
        .supportingText("We'll let you know when something needs your attention.")
        .confirmAction(ok)
        .cancelAction(cancel)
        .build()
        .show(frame);
  }
}
