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
 * Phase-4 / S1 playground (story #369) — exercises the {@link ElwhaSlider.Size} scale: one slider
 * per preset {@code XS}&ndash;{@code XL}, stacked so the track / handle / corner growth is visible
 * side by side. {@code XS} (the default) is the Phase-1&ndash;3 size unchanged; the larger sizes
 * give a bigger touch target and more visual emphasis. The stops toggle dogfoods {@link
 * ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderSizeScaleDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 4 / S1 size scale");

  private ElwhaSliderSizeScaleDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderSizeScaleDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 24));
    grid.setBorder(BorderFactory.createEmptyBorder(36, 32, 28, 32));

    final ElwhaSlider[] sliders = new ElwhaSlider[ElwhaSlider.Size.values().length];
    int i = 0;
    for (final ElwhaSlider.Size size : ElwhaSlider.Size.values()) {
      final ElwhaSlider slider = new ElwhaSlider(0, 100, 60);
      slider.setSizeVariant(size);
      slider.setValueIndicatorEnabled(true);
      sliders[i++] = slider;
      grid.add(labeled(size.name() + " · track " + trackHint(size), slider));
    }

    final ElwhaButton stopsToggle = ElwhaButton.outlinedButton("Toggle stops (25) on all");
    stopsToggle.addActionListener(
        e -> {
          for (final ElwhaSlider slider : sliders) {
            slider.setStops(slider.isStopsEnabled() ? 0 : 25);
          }
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(stopsToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(620, 640));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static String trackHint(final ElwhaSlider.Size size) {
    return switch (size) {
      case XS -> "16 (default)";
      case S -> "24";
      case M -> "40";
      case L -> "56";
      case XL -> "96";
    };
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
