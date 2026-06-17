package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S7 visual smoke for keyboard + a11y + RTL (#489): Tab order (tabs → pane stops → hex field),
 * arrow-key focus cursors with primary focus visuals on every strip/slider/box, horizontal arrows
 * flipping under RTL, mirrored grids/strips/tracks (min at the leading edge), the disabled state
 * staying fully keyboard-inert, and accessible names readable by VoiceOver (the picker announces
 * "Select color", sliders their channel, cells their catalog names).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerA11yDemo {

  private ElwhaColorPickerA11yDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(ElwhaColorPickerA11yDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — S7 keyboard + a11y + RTL (#489)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x7E57C2));
    picker.setAlphaEnabled(true);

    final ElwhaSwitch rtl = new ElwhaSwitch();
    rtl.setLabel("Right-to-left");
    rtl.addActionListener(
        e -> {
          picker.applyComponentOrientation(
              rtl.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          picker.revalidate();
          picker.repaint();
        });

    final ElwhaSwitch enabled = new ElwhaSwitch();
    enabled.setSelected(true);
    enabled.setLabel("Enabled (try keys while off)");
    enabled.addActionListener(e -> picker.setEnabled(enabled.isSelected()));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(rtl);
    controls.add(enabled);
    controls.add(new JLabel("Tab through; arrows move cursors; Space/Enter picks."));

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(560, 640);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
