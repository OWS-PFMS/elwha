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
 * Phase-3 / S1 playground (story #359) — exercises the {@link ElwhaSlider.Variant#RANGE} chrome: two
 * pill handles selecting a {@code [lower, upper]} sub-span, the active {@code PRIMARY} track filling
 * <em>between</em> the handles with inactive track on both outer sides, and stop / both-end contrast
 * dots. Interaction (drag / no-cross clamp) lands in #360, so these sliders are shown statically with
 * preset spans; the disabled toggle dogfoods {@link ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderRangeChromeDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 3 / S1 range chrome");

  private ElwhaSliderRangeChromeDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderRangeChromeDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider span = ElwhaSlider.range(0, 100, 30, 70);

    final ElwhaSlider narrow = ElwhaSlider.range(0, 100, 45, 55);

    final ElwhaSlider stops = ElwhaSlider.range(0, 100, 20, 80);
    stops.setStops(10);

    final ElwhaSlider negative = ElwhaSlider.range(-50, 50, -20, 30);

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(40, 32, 24, 32));
    grid.add(labeled("range · [30, 70] over 0–100", span));
    grid.add(labeled("range · [45, 55] (narrow span)", narrow));
    grid.add(labeled("range · stops 10 · interior + end dots", stops));
    grid.add(labeled("range · [−20, 30] over −50–50", negative));

    final ElwhaButton disableToggle = ElwhaButton.outlinedButton("Disable all");
    disableToggle.addActionListener(
        e -> {
          final boolean enable = !span.isEnabled();
          span.setEnabled(enable);
          narrow.setEnabled(enable);
          stops.setEnabled(enable);
          negative.setEnabled(enable);
          disableToggle.setText(enable ? "Disable all" : "Enable all");
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(disableToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(560, 460));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel labeled(final String text, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(230, 44));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }
}
