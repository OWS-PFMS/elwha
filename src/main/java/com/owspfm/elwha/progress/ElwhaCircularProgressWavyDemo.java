package com.owspfm.elwha.progress;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.slider.ElwhaSlider;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Throwaway S5 playground (story #473) — proves the {@link ElwhaCircularProgressIndicator}
 * Expressive wavy shape: the radial-sine scallop on the active arc (15px wavelength, 1.6px
 * amplitude, spinning phase), the flat track behind it, the seamless closed wavy ring at 100%,
 * the amplitude ramp flattening at ≥95%, the 48/52px wavy diameters, and the wavy indeterminate
 * spin. The scrub row shares one model with an {@code ElwhaSlider} — ride it through the ramp.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaCircularProgressWavyDemo {

  private final JFrame frame = new JFrame("ElwhaCircularProgressIndicator — S5 wavy");
  private Mode mode = Mode.LIGHT;

  private ElwhaCircularProgressWavyDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaCircularProgressWavyDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(2, 4, 24, 24));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(cell("wavy @ 25", wavyAt(25, 4)));
    grid.add(cell("wavy @ 60", wavyAt(60, 4)));
    grid.add(cell("wavy @ 97 (ramp → flat)", wavyAt(97, 4)));
    grid.add(cell("wavy @ 100 (closed ring)", wavyAt(100, 4)));
    grid.add(cell("wavy thick 8px (52px)", wavyAt(60, 8)));
    grid.add(cell("wavy indeterminate", ElwhaCircularProgressIndicator.wavyIndeterminate()));
    grid.add(cell("wavy indeterminate thick", thickWavyIndeterminate()));

    final ElwhaCircularProgressIndicator scrubbed = ElwhaCircularProgressIndicator.wavy();
    scrubbed.setValue(60);
    final ElwhaSlider scrubber = new ElwhaSlider(scrubbed.getModel());
    final JPanel scrubCell = new JPanel(new BorderLayout(0, 8));
    final JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    center.add(scrubbed);
    scrubCell.add(center, BorderLayout.CENTER);
    scrubCell.add(scrubber, BorderLayout.SOUTH);
    grid.add(cell("scrub (shared model)", scrubCell));

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(760, 400));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaCircularProgressIndicator wavyAt(final int value, final int thickness) {
    final ElwhaCircularProgressIndicator ring = ElwhaCircularProgressIndicator.wavy();
    ring.setValue(value);
    ring.setTrackThickness(thickness);
    return ring;
  }

  private static ElwhaCircularProgressIndicator thickWavyIndeterminate() {
    final ElwhaCircularProgressIndicator ring = ElwhaCircularProgressIndicator.wavyIndeterminate();
    ring.setTrackThickness(8);
    return ring;
  }

  private static JPanel cell(final String text, final java.awt.Component content) {
    final JPanel cell = new JPanel(new BorderLayout(0, 8));
    final JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    center.add(content);
    cell.add(center, BorderLayout.CENTER);
    cell.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.SOUTH);
    return cell;
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
