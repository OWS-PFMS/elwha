package com.owspfm.elwha.progress;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
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
 * Throwaway S3 playground (story #471) — proves the {@link ElwhaLinearProgressIndicator}
 * Expressive wavy shape: the traveling sine on the active span (40px wavelength determinate, 20px
 * indeterminate, one wavelength/s), the amplitude ramp flattening outside (10%, 95%) progress,
 * the flat track + stop dot, the 14px-tall thick reference, and RTL travel. The scrub row shares
 * one {@code BoundedRangeModel} between an {@code ElwhaSlider} and the bar — drag the slider
 * through the ramp edges (≤10, ≥95) to watch the 500ms amplitude transition.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLinearProgressWavyDemo {

  private final JFrame frame = new JFrame("ElwhaLinearProgressIndicator — S3 wavy");
  private Mode mode = Mode.LIGHT;

  private ElwhaLinearProgressWavyDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaLinearProgressWavyDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 18));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(labeled("wavy @ 5 (ramp → flat)", wavyAt(5, 4, false)));
    grid.add(labeled("wavy @ 25", wavyAt(25, 4, false)));
    grid.add(labeled("wavy @ 60", wavyAt(60, 4, false)));
    grid.add(labeled("wavy @ 97 (ramp → flat)", wavyAt(97, 4, false)));
    grid.add(labeled("wavy thick 8px @ 60 (14px tall)", wavyAt(60, 8, false)));
    grid.add(labeled("wavy RTL @ 60", wavyAt(60, 4, true)));
    grid.add(
        labeled("wavy indeterminate (20px wavelength)", ElwhaLinearProgressIndicator.wavyIndeterminate()));

    final ElwhaLinearProgressIndicator scrubbed = ElwhaLinearProgressIndicator.wavy();
    scrubbed.setValue(60);
    final ElwhaSlider scrubber = new ElwhaSlider(scrubbed.getModel());
    final JPanel scrubRow = new JPanel(new GridLayout(2, 1, 0, 6));
    scrubRow.add(scrubbed);
    scrubRow.add(scrubber);
    grid.add(labeled("scrub (shared model w/ ElwhaSlider)", scrubRow));

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(680, 480));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaLinearProgressIndicator wavyAt(
      final int value, final int thickness, final boolean rtl) {
    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.wavy();
    bar.setValue(value);
    bar.setTrackThickness(thickness);
    if (rtl) {
      bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
    return bar;
  }

  private static JPanel labeled(final String text, final java.awt.Component content) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(250, 20));
    row.add(label, BorderLayout.WEST);
    row.add(content, BorderLayout.CENTER);
    return row;
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
