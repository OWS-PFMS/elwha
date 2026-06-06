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
 * S4 playground (story #345) — exercises {@link ElwhaSlider} <strong>stops</strong> mode (snap to
 * step, stop-indicator dots active = {@code ON_PRIMARY} / inactive = {@code
 * ON_SECONDARY_CONTAINER}), the contrast end stop, and the value-indicator bubble with a {@code
 * valueFormatter}. The stops/end-stop toggles dogfood {@link ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderStopsDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — S4 stops + value indicator");

  private ElwhaSliderStopsDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderStopsDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider stops = new ElwhaSlider(0, 100, 40);
    stops.setStops(10);
    stops.setValueIndicatorEnabled(true);
    stops.setValueFormatter(v -> v + "%");

    final ElwhaSlider coarse = new ElwhaSlider(0, 5, 2);
    coarse.setStops(1);
    coarse.setValueIndicatorEnabled(true);

    final ElwhaSlider continuous = new ElwhaSlider(0, 100, 30);
    continuous.setValueIndicatorEnabled(true);

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(40, 32, 24, 32));
    grid.add(labeled("stops = 10 · '%'", stops));
    grid.add(labeled("stops = 1 (0–5)", coarse));
    grid.add(labeled("continuous + end stop", continuous));

    final ElwhaButton endStopToggle = ElwhaButton.outlinedButton("End stops: on");
    endStopToggle.addActionListener(
        e -> {
          final boolean next = !continuous.isEndStopsVisible();
          continuous.setEndStopsVisible(next);
          stops.setEndStopsVisible(next);
          endStopToggle.setText("End stops: " + (next ? "on" : "off"));
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(endStopToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(520, 420));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel labeled(final String text, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(170, 44));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }
}
