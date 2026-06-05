package com.owspfm.elwha.slider;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S3 playground (story #344) — exercises the {@link ElwhaSlider} keyboard map (Tab to focus, arrows
 * ±step, Space+arrows ±interval, Home/End to min/max), the {@code slider} accessible role + {@code
 * AccessibleValue}, and RTL mirroring. Each slider is paired with a {@link JLabel} via {@code
 * setLabelFor} so a screen reader reads label &rarr; role &rarr; value. The RTL toggle dogfoods
 * {@link ElwhaButton} and flips fill + arrow direction.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderKeyboardA11yDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — S3 keyboard + a11y + RTL");
  private final JPanel sliders = new JPanel(new GridBagLayout());
  private boolean rightToLeft;

  private ElwhaSliderKeyboardA11yDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderKeyboardA11yDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    sliders.setBorder(BorderFactory.createEmptyBorder(32, 32, 24, 32));

    addRow(0, "Brightness", new ElwhaSlider(0, 100, 40));
    addRow(1, "Volume", configured(new ElwhaSlider(0, 100, 65)));

    final ElwhaButton rtlToggle = ElwhaButton.outlinedButton("Orientation: LTR");
    rtlToggle.addActionListener(
        e -> {
          rightToLeft = !rightToLeft;
          applyOrientation();
          rtlToggle.setText("Orientation: " + (rightToLeft ? "RTL" : "LTR"));
        });
    final JLabel hint =
        new JLabel("Tab to focus · ← → arrows · Space+arrows = block · Home/End = min/max");
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(rtlToggle);
    top.add(hint);

    frame.add(top, BorderLayout.NORTH);
    frame.add(sliders, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(560, 320));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaSlider configured(final ElwhaSlider s) {
    s.setValueIndicatorEnabled(true);
    s.setBlockIncrement(20);
    return s;
  }

  private void addRow(final int row, final String name, final ElwhaSlider slider) {
    final JLabel label = new JLabel(name);
    label.setLabelFor(slider);
    final GridBagConstraints lc = new GridBagConstraints();
    lc.gridx = 0;
    lc.gridy = row;
    lc.anchor = GridBagConstraints.LINE_START;
    lc.insets = new Insets(8, 0, 8, 16);
    sliders.add(label, lc);

    final GridBagConstraints sc = new GridBagConstraints();
    sc.gridx = 1;
    sc.gridy = row;
    sc.weightx = 1;
    sc.fill = GridBagConstraints.HORIZONTAL;
    sc.insets = new Insets(8, 0, 8, 0);
    sliders.add(slider, sc);
  }

  private void applyOrientation() {
    final ComponentOrientation o =
        rightToLeft ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
    sliders.applyComponentOrientation(o);
    sliders.revalidate();
    sliders.repaint();
  }
}
