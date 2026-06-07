package com.owspfm.elwha.slider;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
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
 * Phase-4 / S2 playground (story #370) — exercises the {@link ElwhaSlider#setInsetIcon inset icon}:
 * a brightness glyph inset at the leading end of the active track on standard {@code M}/{@code
 * L}/{@code XL} sliders. Drag toward the minimum to watch the icon <strong>swap into the inactive
 * track</strong> on the handle's trailing side (M3 swap-at-zero). The RTL row mirrors the leading
 * end; the {@code XS} row shows the documented no-op (icon absent). The mode toggle dogfoods {@link
 * ElwhaButton}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderInsetIconDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 4 / S2 inset icon");

  private ElwhaSliderInsetIconDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSliderInsetIconDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSlider medium = insetSlider(ElwhaSlider.Size.M, 70);
    final ElwhaSlider large = insetSlider(ElwhaSlider.Size.L, 70);
    final ElwhaSlider xl = insetSlider(ElwhaSlider.Size.XL, 70);
    final ElwhaSlider low = insetSlider(ElwhaSlider.Size.L, 4);
    final ElwhaSlider rtl = insetSlider(ElwhaSlider.Size.L, 70);
    rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    final ElwhaSlider tooSmall = insetSlider(ElwhaSlider.Size.XS, 70);

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 22));
    grid.setBorder(BorderFactory.createEmptyBorder(34, 32, 26, 32));
    grid.add(labeled("M · icon in active track", medium));
    grid.add(labeled("L · icon in active track", large));
    grid.add(labeled("XL · 32 dp icon", xl));
    grid.add(labeled("L · value 4 → icon swaps to inactive", low));
    grid.add(labeled("L · RTL (leading = right)", rtl));
    grid.add(labeled("XS · no-op (icon absent)", tooSmall));

    final ElwhaButton dim = ElwhaButton.outlinedButton("Disable all");
    dim.addActionListener(
        e -> {
          for (final ElwhaSlider s : new ElwhaSlider[] {medium, large, xl, low, rtl, tooSmall}) {
            s.setEnabled(!s.isEnabled());
          }
          dim.setText(medium.isEnabled() ? "Disable all" : "Enable all");
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(dim);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(640, 620));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static ElwhaSlider insetSlider(final ElwhaSlider.Size size, final int value) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, value);
    slider.setSizeVariant(size);
    slider.setInsetIcon(MaterialIcons.brightnessAuto());
    slider.setValueIndicatorEnabled(true);
    return slider;
  }

  private static JPanel labeled(final String text, final ElwhaSlider slider) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(220, 44));
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
  }
}
