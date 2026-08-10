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
 * S2 visual smoke for the SWATCHES pane (#484): the twenty-hue grid (500 shades), the active hue's
 * connected shade strip, the recent row filling as colors are picked, the primary-ring +
 * luminance-check selection indicator riding the cell that equals the current color, external
 * {@code setColor} sync (the "Stage Teal 700" button), and keyboard navigation (Tab into a strip,
 * arrows, Space/Enter).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSwatchesDemo {

  private ElwhaColorPickerSwatchesDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerSwatchesDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — S2 swatches (#484)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xF44336));
    picker.setModes(PickerMode.SWATCHES);
    picker.setSupportingText("Swatches only — tab bar hidden");

    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(e -> readout.setText("Listener: " + picker.formatCurrentHex()));

    final ElwhaButton stageTeal = ElwhaButton.filledTonalButton("Stage Teal 700 via setColor");
    stageTeal.addActionListener(e -> picker.setColor(new Color(0x00796B)));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(stageTeal);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(520, 480);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
