package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * V2 S6 visual sweep (#502): a fully-loaded picker (four modes, three swatch sources, alpha,
 * eyedropper, seeded favorites). Walk it with the keyboard only — Tab reaches the eyedropper button
 * (header reading order, before the tabs), the source toggle, every grid (arrows + Space, Delete on
 * Saved), the wheel disc (arrows/PgUp/PgDn/Home/End), and every track. Toggle RTL: grids, toggle
 * rows, and tracks mirror; the wheel's hue angle deliberately does not. Toggle Enabled: everything
 * goes inert (keys included) at 38% content opacity.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerV2SweepDemo {

  private ElwhaColorPickerV2SweepDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerV2SweepDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — V2 S6 keyboard/RTL/disabled sweep (#502)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x6750A4));
    picker.setAlphaEnabled(true);
    picker.setEyedropperEnabled(true);
    picker.setFavorites(List.of(new Color(0x6750A4), new Color(0x2E7D32), new Color(0xFFB300)));
    picker.setSupportingText("Keyboard-only walkthrough");

    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(e -> readout.setText("Listener: " + picker.formatCurrentHex()));

    final ElwhaButton rtl = ElwhaButton.outlinedButton("Toggle RTL");
    rtl.addActionListener(
        e ->
            frame.applyComponentOrientation(
                frame.getComponentOrientation().isLeftToRight()
                    ? ComponentOrientation.RIGHT_TO_LEFT
                    : ComponentOrientation.LEFT_TO_RIGHT));

    final ElwhaButton enabled = ElwhaButton.outlinedButton("Toggle enabled");
    enabled.addActionListener(e -> picker.setEnabled(!picker.isEnabled()));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(rtl);
    controls.add(enabled);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(560, 640);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
