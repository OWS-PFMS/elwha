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
 * Phase-3 / S2 playground (story #360) — exercises {@link ElwhaSlider.Variant#RANGE} interaction:
 * press near a handle to grab the nearest one, drag it (it stops at the other handle under the
 * no-cross clamp), click the track to jump the nearest handle, and watch the active handle narrow
 * (4&rarr;2&nbsp;dp) with a press ripple while the other handle stays at rest. Stops mode snaps the
 * moving handle. A live read-out reflects each slider's {@code [lower, upper]} span; the reset button
 * dogfoods {@link ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderRangeInteractionDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 3 / S2 range interaction");

  private ElwhaSliderRangeInteractionDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderRangeInteractionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider price = ElwhaSlider.range(0, 100, 30, 70);
    final ElwhaSlider stops = ElwhaSlider.range(0, 200, 40, 160);
    stops.setStops(20);
    final ElwhaSlider temps = ElwhaSlider.range(-20, 40, -5, 22);

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(40, 32, 24, 32));
    grid.add(span("Price 0–100", price));
    grid.add(span("Stops 20 · 0–200", stops));
    grid.add(span("Temps −20–40", temps));

    final ElwhaButton reset = ElwhaButton.outlinedButton("Reset spans");
    reset.addActionListener(
        e -> {
          price.setLowerValue(30);
          price.setUpperValue(70);
          stops.setLowerValue(40);
          stops.setUpperValue(160);
          temps.setLowerValue(-5);
          temps.setUpperValue(22);
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(reset);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(620, 420));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel span(final String name, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(name);
    label.setPreferredSize(new Dimension(150, 44));
    final JLabel readout = new JLabel();
    readout.setPreferredSize(new Dimension(120, 44));
    final Runnable update =
        () -> readout.setText("[" + slider.getLowerValue() + ", " + slider.getUpperValue() + "]");
    slider.addChangeListener(e -> update.run());
    update.run();
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    row.add(readout, BorderLayout.EAST);
    return row;
  }
}
