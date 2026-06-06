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
 * Phase-2 / S1 playground (story #351) — exercises the {@link ElwhaSlider.Variant#CENTERED}
 * variant: the active track fills out of the {@linkplain ElwhaSlider#getOrigin() origin} (range
 * midpoint or zero) toward the handle in either direction, with inactive track on both outer sides
 * and stop dots at both ends. Drag the centered sliders past and below the origin to watch the fill
 * flip direction; the mode toggle dogfoods {@link ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderCenteredDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 2 / S1 centered variant");

  private ElwhaSliderCenteredDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderCenteredDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider balance = new ElwhaSlider(-50, 50, 25);
    balance.setVariant(ElwhaSlider.Variant.CENTERED);
    balance.setValueIndicatorEnabled(true);

    final ElwhaSlider belowOrigin = new ElwhaSlider(-50, 50, -30);
    belowOrigin.setVariant(ElwhaSlider.Variant.CENTERED);
    belowOrigin.setValueIndicatorEnabled(true);

    final ElwhaSlider stops = new ElwhaSlider(-50, 50, 0);
    stops.setVariant(ElwhaSlider.Variant.CENTERED);
    stops.setStops(25);
    stops.setValueIndicatorEnabled(true);

    final ElwhaSlider customOrigin = new ElwhaSlider(0, 100, 80);
    customOrigin.setVariant(ElwhaSlider.Variant.CENTERED);
    customOrigin.setOrigin(60);
    customOrigin.setValueIndicatorEnabled(true);

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(40, 32, 24, 32));
    grid.add(labeled("centered · +25 (fills right)", balance));
    grid.add(labeled("centered · −30 (fills left)", belowOrigin));
    grid.add(labeled("centered · stops 25 · both-end dots", stops));
    grid.add(labeled("centered · origin = 60 (0–100)", customOrigin));

    final ElwhaButton variantToggle = ElwhaButton.outlinedButton("Make first slider: standard");
    variantToggle.addActionListener(
        e -> {
          final boolean centered = balance.getVariant() == ElwhaSlider.Variant.CENTERED;
          balance.setVariant(
              centered ? ElwhaSlider.Variant.STANDARD : ElwhaSlider.Variant.CENTERED);
          variantToggle.setText("Make first slider: " + (centered ? "centered" : "standard"));
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(variantToggle);

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
    label.setPreferredSize(new Dimension(210, 44));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }
}
