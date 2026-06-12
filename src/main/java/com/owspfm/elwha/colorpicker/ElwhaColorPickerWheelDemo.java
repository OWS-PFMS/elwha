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
 * V2 S1 visual smoke for the WHEEL pane (#497): a wheel-only picker beside a default four-mode
 * picker. Verify the disc tracks press/drag with the ring thumb (hue = angle, saturation = radius),
 * the value slider darkens the disc, four <em>stacked</em> tabs render un-truncated at the picker's
 * 328px width, keyboard polar nudging works (Tab to the disc; arrows, Page keys, Home/End), and the
 * hue-preservation invariant holds — press "Set #808080", then nudge saturation up: the hue resumes
 * where it was, never snapping to red.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerWheelDemo {

  private ElwhaColorPickerWheelDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerWheelDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — V2 S1 wheel (#497)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker wheelOnly = new ElwhaColorPicker(new Color(0x00897B));
    wheelOnly.setModes(PickerMode.WHEEL);
    wheelOnly.setSupportingText("Wheel only");

    final ElwhaColorPicker allModes = new ElwhaColorPicker(new Color(0xFF7043));
    allModes.setSupportingText("All four modes — tabs must not truncate");
    allModes.setMode(PickerMode.WHEEL);

    final JLabel readout = new JLabel("Listener: " + wheelOnly.formatCurrentHex());
    wheelOnly.addChangeListener(
        e ->
            readout.setText(
                "Listener: "
                    + wheelOnly.formatCurrentHex()
                    + (wheelOnly.isAdjusting() ? " (adjusting)" : "")));

    final ElwhaButton grey = ElwhaButton.filledTonalButton("Set #808080 (hue must survive)");
    grey.addActionListener(e -> wheelOnly.setColor(new Color(0x808080)));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(wheelOnly);
    center.add(allModes);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(grey);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(860, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
