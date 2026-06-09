package com.owspfm.elwha.slider;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase-5 / S1 playground (story #385) — exercises {@link ElwhaSlider.Orientation#VERTICAL} chrome
 * and geometry: standard and centered sliders transposed onto a tall track that fills
 * <strong>bottom-up</strong> (minimum at the bottom, maximum at the top), shown across a couple of
 * {@link ElwhaSlider.Size} presets so the transposed track / handle / corner growth is visible. A
 * matching horizontal pair sits alongside for comparison. Stops toggle dogfoods {@link
 * ElwhaButton}; Tab into a slider to see the leading-side value bubble.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderVerticalChromeDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 5 / S1 vertical chrome");

  private ElwhaSliderVerticalChromeDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderVerticalChromeDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel rail = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 8));
    rail.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

    final ElwhaSlider[] sliders = {
      vertical(ElwhaSlider.Size.XS, ElwhaSlider.Variant.STANDARD, 70),
      vertical(ElwhaSlider.Size.M, ElwhaSlider.Variant.STANDARD, 70),
      verticalCentered(ElwhaSlider.Size.XS, 75),
      verticalCentered(ElwhaSlider.Size.M, 25),
    };
    final String[] labels = {
      "Standard XS", "Standard M", "Centered XS", "Centered M",
    };
    for (int i = 0; i < sliders.length; i++) {
      rail.add(column(labels[i], sliders[i]));
    }

    // A horizontal standard slider alongside so the transposition is obvious.
    final ElwhaSlider horizontal = new ElwhaSlider(0, 100, 70);
    horizontal.setValueIndicatorEnabled(true);
    horizontal.setLabel("Horizontal");
    final JPanel horizontalRow = new JPanel(new BorderLayout(12, 0));
    horizontalRow.setBorder(BorderFactory.createEmptyBorder(4, 28, 20, 28));
    horizontalRow.add(new JLabel("Horizontal (for comparison)"), BorderLayout.WEST);
    horizontalRow.add(horizontal, BorderLayout.CENTER);

    final ElwhaButton stopsToggle = ElwhaButton.outlinedButton("Toggle stops (25) on all");
    stopsToggle.addActionListener(
        e -> {
          for (final ElwhaSlider slider : sliders) {
            slider.setStops(slider.isStopsEnabled() ? 0 : 25);
          }
          horizontal.setStops(horizontal.isStopsEnabled() ? 0 : 25);
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(stopsToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rail, BorderLayout.CENTER);
    frame.add(horizontalRow, BorderLayout.SOUTH);
    frame.setMinimumSize(new Dimension(680, 540));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaSlider vertical(
      final ElwhaSlider.Size size, final ElwhaSlider.Variant variant, final int value) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, value);
    slider.setVariant(variant);
    slider.setSizeVariant(size);
    slider.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    slider.setValueIndicatorEnabled(true);
    slider.setLabel(size + " " + variant);
    slider.setPreferredSize(new Dimension(slider.getPreferredSize().width, 280));
    return slider;
  }

  private static ElwhaSlider verticalCentered(final ElwhaSlider.Size size, final int value) {
    final ElwhaSlider slider = new ElwhaSlider(-50, 50, value - 50);
    slider.setVariant(ElwhaSlider.Variant.CENTERED);
    slider.setOrigin(0);
    slider.setSizeVariant(size);
    slider.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    slider.setValueIndicatorEnabled(true);
    slider.setLabel("Centered " + size);
    slider.setPreferredSize(new Dimension(slider.getPreferredSize().width, 280));
    return slider;
  }

  private static JPanel column(final String text, final ElwhaSlider slider) {
    final JPanel col = new JPanel(new BorderLayout(0, 10));
    final JLabel label = new JLabel(text, SwingConstants.CENTER);
    col.add(slider, BorderLayout.CENTER);
    col.add(label, BorderLayout.SOUTH);
    return col;
  }
}
