package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S4 visual smoke for the SLIDERS pane (#486): the connected RGB/HSV sub-toggle, three
 * context-gradient channel rows per model (each track sweeps its channel through the current
 * color), live value labels, and the validated hex field — Enter or focus-loss commits, bad text
 * raises the error supporting line ("Use #RRGGBB") and reverts on focus-loss. Try typing {@code
 * 0f0}, then garbage, then Tab away.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSlidersDemo {

  private ElwhaColorPickerSlidersDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerSlidersDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — S4 sliders + hex (#486)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x336699));
    picker.setModes(PickerMode.SLIDERS);
    picker.setSupportingText("Sliders only");

    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(
        e ->
            readout.setText(
                "Listener: "
                    + picker.formatCurrentHex()
                    + (picker.isAdjusting() ? " (adjusting)" : "")));

    final ElwhaButton amber = ElwhaButton.filledTonalButton("Set Amber 700 via setColor");
    amber.addActionListener(e -> picker.setColor(new Color(0xFFA000)));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(amber);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(520, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
