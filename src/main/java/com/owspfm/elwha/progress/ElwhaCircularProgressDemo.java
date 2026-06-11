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
 * Throwaway S4 playground (story #472) — proves the {@link ElwhaCircularProgressIndicator} flat
 * ring: the determinate active arc + double-gapped track across values (seamless full rings at the
 * 0%/100% edges), the 44px/8px thick reference, the 6000ms indeterminate grow/shrink + rotation
 * choreography, and a scrub row sharing one model with an {@code ElwhaSlider}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaCircularProgressDemo {

  private final JFrame frame = new JFrame("ElwhaCircularProgressIndicator — S4 ring");
  private Mode mode = Mode.LIGHT;

  private ElwhaCircularProgressDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaCircularProgressDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(2, 4, 24, 24));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(cell("0%", ring(0, 4)));
    grid.add(cell("25%", ring(25, 4)));
    grid.add(cell("60%", ring(60, 4)));
    grid.add(cell("100%", ring(100, 4)));
    grid.add(cell("thick 8px @ 60 (44px)", ring(60, 8)));
    grid.add(cell("indeterminate", ElwhaCircularProgressIndicator.indeterminate()));
    grid.add(cell("indeterminate thick", thickIndeterminate()));

    final ElwhaCircularProgressIndicator scrubbed = new ElwhaCircularProgressIndicator(0, 100, 60);
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
    frame.setMinimumSize(new Dimension(720, 380));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaCircularProgressIndicator ring(final int value, final int thickness) {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator(0, 100, value);
    ring.setTrackThickness(thickness);
    return ring;
  }

  private static ElwhaCircularProgressIndicator thickIndeterminate() {
    final ElwhaCircularProgressIndicator ring = ElwhaCircularProgressIndicator.indeterminate();
    ring.setTrackThickness(8);
    return ring;
  }

  private static JPanel cell(final String text, final java.awt.Component content) {
    final JPanel cell = new JPanel(new BorderLayout(0, 8));
    final JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    center.add(content);
    cell.add(center, BorderLayout.CENTER);
    final JLabel label = new JLabel(text, SwingConstants.CENTER);
    cell.add(label, BorderLayout.SOUTH);
    return cell;
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
