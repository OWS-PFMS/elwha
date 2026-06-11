package com.owspfm.elwha.progress;

import com.owspfm.elwha.button.ElwhaButton;
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
 * Throwaway S2 playground (story #470) — proves the {@link ElwhaLinearProgressIndicator}
 * indeterminate motion: the 1750ms two-line cycle with track spans + gaps around each moving
 * line, no stop dot, thickness/RTL parity with determinate, and a clean
 * determinate↔indeterminate round-trip (value preserved; the clock stops with the mode).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLinearProgressIndeterminateDemo {

  private final JFrame frame = new JFrame("ElwhaLinearProgressIndicator — S2 indeterminate");
  private Mode mode = Mode.LIGHT;

  private ElwhaLinearProgressIndeterminateDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaLinearProgressIndeterminateDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(labeled("indeterminate (default 4px)", ElwhaLinearProgressIndicator.indeterminate()));
    grid.add(labeled("indeterminate thick 8px", thick()));
    grid.add(labeled("indeterminate RTL", rtl()));

    final ElwhaLinearProgressIndicator roundTrip = new ElwhaLinearProgressIndicator(0, 100, 60);
    roundTrip.setIndeterminate(true);
    grid.add(labeled("mode round-trip @ 60", roundTrip));

    final ElwhaButton toggle = ElwhaButton.filledButton("Toggle determinate ↔ indeterminate");
    toggle.addActionListener(e -> roundTrip.setIndeterminate(!roundTrip.isIndeterminate()));
    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(toggle);
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(620, 320));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaLinearProgressIndicator thick() {
    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.indeterminate();
    bar.setTrackThickness(8);
    return bar;
  }

  private static ElwhaLinearProgressIndicator rtl() {
    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.indeterminate();
    bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    return bar;
  }

  private static JPanel labeled(final String text, final ElwhaLinearProgressIndicator bar) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(230, 20));
    row.add(label, BorderLayout.WEST);
    row.add(bar, BorderLayout.CENTER);
    return row;
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
