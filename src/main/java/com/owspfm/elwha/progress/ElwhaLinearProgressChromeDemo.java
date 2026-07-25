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
 * Throwaway S1 playground (story #469) — proves the {@link ElwhaLinearProgressIndicator}
 * determinate chrome renders the updated-M3 anatomy across values: active/track split with the 4px
 * gap, the trailing stop dot (and its hide-on-arrival), the 8px thick reference with 2px inner
 * corners, RTL mirroring, and light/dark theming with zero new tokens.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLinearProgressChromeDemo {

  private final JFrame frame = new JFrame("ElwhaLinearProgressIndicator — S1 chrome");
  private Mode mode = Mode.LIGHT;

  private ElwhaLinearProgressChromeDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaLinearProgressChromeDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 18));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(labeled("value = 0", bar(0, 4, false)));
    grid.add(labeled("value = 5", bar(5, 4, false)));
    grid.add(labeled("value = 25", bar(25, 4, false)));
    grid.add(labeled("value = 60", bar(60, 4, false)));
    grid.add(labeled("value = 95 (stop nearly reached)", bar(95, 4, false)));
    grid.add(labeled("value = 100 (track + stop gone)", bar(100, 4, false)));
    grid.add(labeled("thick 8px @ 60 (2px inner corners)", bar(60, 8, false)));
    grid.add(labeled("RTL @ 60 (mirrored fill + stop)", bar(60, 4, true)));
    grid.add(labeled("no stop dot @ 60", noStop()));

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(560, 420));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaLinearProgressIndicator bar(
      final int value, final int thickness, final boolean rtl) {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(0, 100, value);
    bar.setTrackThickness(thickness);
    if (rtl) {
      bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
    return bar;
  }

  private static ElwhaLinearProgressIndicator noStop() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(0, 100, 60);
    bar.setTrackStopIndicatorSize(0);
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
