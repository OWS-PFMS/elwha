package com.owspfm.elwha.slider;

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
 * S2 playground (story #343) — exercises {@link ElwhaSlider} drag, click-to-jump, hover/focus state
 * layers, the press ripple, the handle 4&rarr;2&nbsp;dp narrow morph, and the value-indicator
 * bubble. A live readout shows the value and {@link ElwhaSlider#getValueIsAdjusting()}; a
 * reduced-motion toggle proves the morph/bubble snap. Mode and reduced-motion controls dogfood
 * {@link ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderInteractionDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — S2 interaction & motion");
  private final JLabel readout = new JLabel("value = — · adjusting = —");
  private Mode mode = Mode.LIGHT;
  private boolean reducedMotion;

  private ElwhaSliderInteractionDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderInteractionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(40, 32, 24, 32));
    grid.add(labeled("drag / click-to-jump", tracked(slider(0, 100, 30, true))));
    grid.add(labeled("range 0–10", tracked(slider(0, 10, 4, true))));
    grid.add(labeled("disabled @ 70", slider(0, 100, 70, false)));

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final ElwhaButton motionToggle = ElwhaButton.outlinedButton("Reduced motion: off");
    motionToggle.addActionListener(
        e -> {
          reducedMotion = !reducedMotion;
          MorphAnimator.setReducedMotion(reducedMotion);
          motionToggle.setText("Reduced motion: " + (reducedMotion ? "on" : "off"));
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);
    top.add(motionToggle);
    top.add(readout);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(480, 420));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private ElwhaSlider tracked(final ElwhaSlider s) {
    s.addChangeListener(
        e ->
            readout.setText(
                "value = " + s.getValue() + " · adjusting = " + s.getValueIsAdjusting()));
    return s;
  }

  private static ElwhaSlider slider(
      final int min, final int max, final int value, final boolean enabled) {
    final ElwhaSlider s = new ElwhaSlider(min, max, value);
    s.setValueIndicatorEnabled(true);
    s.setEnabled(enabled);
    return s;
  }

  private static JPanel labeled(final String text, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(160, 44));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
