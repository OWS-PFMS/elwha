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
 * S3 visual smoke for the SPECTRUM pane (#485): the saturation/value square with its ring thumb,
 * the six-stop rainbow hue ColorTrackSlider, drag adjusting (watch the readout flag), keyboard
 * nudging (Tab to the box/slider, arrows and Page keys), and the hue-preservation invariant — press
 * "Set #808080", then nudge saturation right: the hue resumes where it was, never snapping to red.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSpectrumDemo {

  private ElwhaColorPickerSpectrumDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerSpectrumDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — S3 spectrum (#485)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x2E7D32));
    picker.setModes(PickerMode.SPECTRUM);
    picker.setSupportingText("Spectrum only");

    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(
        e ->
            readout.setText(
                "Listener: "
                    + picker.formatCurrentHex()
                    + (picker.isAdjusting() ? " (adjusting)" : "")));

    final ElwhaButton grey = ElwhaButton.filledTonalButton("Set #808080 (hue must survive)");
    grey.addActionListener(e -> picker.setColor(new Color(0x808080)));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(grey);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(520, 520);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
