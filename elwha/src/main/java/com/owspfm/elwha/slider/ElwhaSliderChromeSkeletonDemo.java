package com.owspfm.elwha.slider;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
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
 * Throwaway S1 playground (story #342) — proves the {@link ElwhaSlider} chrome skeleton renders
 * across values, light/dark mode, and the disabled treatment with zero new tokens. No interaction
 * yet (drag/keyboard land in later stories); this exercise is the spike that locks design §2.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderChromeSkeletonDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — S1 chrome skeleton");
  private Mode mode = Mode.LIGHT;

  private ElwhaSliderChromeSkeletonDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderChromeSkeletonDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 16));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(labeled("value = min (0)", slider(0, 100, 0, true)));
    grid.add(labeled("value = 30", slider(0, 100, 30, true)));
    grid.add(labeled("value = 60", slider(0, 100, 60, true)));
    grid.add(labeled("value = max (100)", slider(0, 100, 100, true)));
    grid.add(labeled("disabled @ 45", slider(0, 100, 45, false)));

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(420, 360));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaSlider slider(
      final int min, final int max, final int value, final boolean enabled) {
    final ElwhaSlider s = new ElwhaSlider(min, max, value);
    s.setEnabled(enabled);
    return s;
  }

  private static JPanel labeled(final String text, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(140, HANDLE_LABEL_HEIGHT));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }

  private static final int HANDLE_LABEL_HEIGHT = 44;

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
