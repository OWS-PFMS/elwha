package com.owspfm.elwha.slider;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
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
 * Phase-5 / S2 playground (story #386) — exercises vertical <strong>interaction</strong>: drag
 * along the Y axis, click-to-jump, and Up/Down (Page/Home/End) keyboard stepping, all on bottom-up
 * vertical sliders. The {@link ElwhaSlider.Size#M} column carries the vertical <strong>inset
 * icon</strong> pinned at the top (max) end, recoloring as the bottom-up fill reaches it. A live
 * value label tracks each slider; the enable toggle dogfoods {@link ElwhaButton}. Tab between
 * sliders and use the arrows; drag with the mouse.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderVerticalInteractionDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 5 / S2 vertical interaction");

  private ElwhaSliderVerticalInteractionDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderVerticalInteractionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider standard = vertical(ElwhaSlider.Size.XS, 40, false);
    final ElwhaSlider sized = vertical(ElwhaSlider.Size.M, 80, false);
    final ElwhaSlider withIcon = vertical(ElwhaSlider.Size.M, 80, true);

    final JPanel rail = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 8));
    rail.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));
    rail.add(column("Standard XS", standard));
    rail.add(column("Standard M", sized));
    rail.add(column("M + inset (top)", withIcon));

    final ElwhaButton enableToggle = ElwhaButton.outlinedButton("Toggle enabled");
    enableToggle.addActionListener(
        e -> {
          standard.setEnabled(!standard.isEnabled());
          sized.setEnabled(!sized.isEnabled());
          withIcon.setEnabled(!withIcon.isEnabled());
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(new JLabel("Drag along Y · click-to-jump · Tab + Up/Down/Page/Home/End"));
    top.add(enableToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rail, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(560, 460));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaSlider vertical(
      final ElwhaSlider.Size size, final int value, final boolean insetIcon) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, value);
    slider.setSizeVariant(size);
    slider.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    slider.setValueIndicatorEnabled(true);
    slider.setLabel("Vertical " + size);
    if (insetIcon) {
      slider.setInsetIcon(MaterialIcons.brightnessAuto());
    }
    slider.setPreferredSize(new Dimension(slider.getPreferredSize().width, 300));
    return slider;
  }

  private static JPanel column(final String text, final ElwhaSlider slider) {
    final JPanel col = new JPanel(new BorderLayout(0, 8));
    final JLabel value = new JLabel("value: " + slider.getValue(), SwingConstants.CENTER);
    slider.addChangeListener(e -> value.setText("value: " + slider.getValue()));
    col.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.NORTH);
    col.add(slider, BorderLayout.CENTER);
    col.add(value, BorderLayout.SOUTH);
    return col;
  }
}
