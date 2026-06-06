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
 * Phase-3 / S3 playground (story #361) — exercises {@link ElwhaSlider.Variant#RANGE} keyboard,
 * two-thumb accessibility, and the one-at-a-time value bubble. <strong>Tab</strong> moves into the
 * lower handle, Tab again to the upper, Tab out (Shift+Tab reverses); arrows nudge the focused
 * handle, Home/End jump it to its no-cross bound, and only the focused/dragged handle shows a value
 * bubble (it follows focus). A screen reader sees two {@code AccessibleValue} children named
 * "Lower" / "Upper". The percent-format toggle dogfoods {@link ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderRangeKeyboardA11yDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 3 / S3 range keyboard + a11y");

  private ElwhaSliderRangeKeyboardA11yDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderRangeKeyboardA11yDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider price = ElwhaSlider.range(0, 100, 30, 70);
    price.setValueIndicatorEnabled(true);
    price.setLabel("Price range");

    final ElwhaSlider budget = ElwhaSlider.range(0, 100, 25, 75);
    budget.setValueIndicatorEnabled(true);
    budget.setStops(5);
    budget.setLabel("Budget percent");

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 28));
    grid.setBorder(BorderFactory.createEmptyBorder(48, 32, 24, 32));
    grid.add(labeled("Tab → lower, Tab → upper, arrows nudge", price));
    grid.add(labeled("Stops 5 · one bubble follows focus", budget));

    final JLabel hint =
        new JLabel("Tab between handles · ← → nudge · Home/End jump to the no-cross bound");

    final ElwhaButton percentToggle = ElwhaButton.outlinedButton("Show budget as %");
    percentToggle.addActionListener(
        e -> {
          final boolean on = percentToggle.getText().startsWith("Show");
          budget.setValueFormatter(on ? v -> v + "%" : null);
          percentToggle.setText(on ? "Show budget as number" : "Show budget as %");
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(percentToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.add(hint, BorderLayout.SOUTH);
    frame.setMinimumSize(new Dimension(640, 440));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel labeled(final String text, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(260, 44));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }
}
