package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.switches.ElwhaSwitch;
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
 * S1 visual smoke for the {@link ElwhaColorPicker} shell (#483): the M3 picker header (supporting
 * text, preview swatch, hex headline, divider), the three-mode tab bar with the
 * palette/gradient/tune glyphs, placeholder panes, live ChangeListener readout, preset cycling, a
 * single-mode configuration with the tab bar hidden, and the enabled toggle.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerShellDemo {

  private static final Color[] PRESETS = {
    new Color(0xFF7043), new Color(0x42A5F5), new Color(0x66BB6A), new Color(0xAB47BC),
  };

  private ElwhaColorPickerShellDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerShellDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — S1 shell (#483)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(PRESETS[0]);
    final ElwhaColorPicker singleMode = new ElwhaColorPicker(PRESETS[1]);
    singleMode.setModes(PickerMode.SPECTRUM);
    singleMode.setSupportingText("Single mode — tab bar hidden");

    final JLabel readout = new JLabel(readoutText(picker));
    picker.addChangeListener(e -> readout.setText(readoutText(picker)));

    final ElwhaButton cycle = ElwhaButton.filledTonalButton("Next preset");
    cycle.addActionListener(
        e -> {
          int index = 0;
          while (index < PRESETS.length && !PRESETS[index].equals(picker.getColor())) {
            index++;
          }
          picker.setColor(PRESETS[(index + 1) % PRESETS.length]);
        });

    final ElwhaSwitch enabled = new ElwhaSwitch();
    enabled.setSelected(true);
    enabled.setLabel("Enabled");
    enabled.addActionListener(e -> picker.setEnabled(enabled.isSelected()));

    final JPanel pickers = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    pickers.add(picker);
    pickers.add(singleMode);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(cycle);
    controls.add(enabled);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(pickers, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(820, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static String readoutText(final ElwhaColorPicker picker) {
    return "Listener: " + picker.formatCurrentHex() + (picker.isAdjusting() ? " (adjusting)" : "");
  }
}
