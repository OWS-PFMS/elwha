package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * V2 S4 visual smoke for the eyedropper (#500): a picker with {@code setEyedropperEnabled(true)} —
 * the colorize icon button sits at the headline row's trailing edge. Press it: every screen freezes
 * under an always-on-top capture with a crosshair cursor and a magnifier loupe (11×11 grid at 8×,
 * hex chip beneath, quadrant-flipping near edges). Click or Enter picks (watch the readout and the
 * recent row), Esc cancels, arrows nudge one pixel. The rainbow strip below gives in-app targets.
 * <strong>macOS:</strong> grant Screen Recording or captures show only the wallpaper + this window
 * — the documented, undetectable degradation.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerEyedropperDemo {

  private ElwhaColorPickerEyedropperDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerEyedropperDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — V2 S4 eyedropper (#500)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x6750A4));
    picker.setEyedropperEnabled(true);
    picker.setSupportingText("Pick from anywhere on screen");

    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(e -> readout.setText("Listener: " + picker.formatCurrentHex()));

    final JPanel targets = new JPanel(new GridLayout(1, 7, 4, 0));
    targets.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    for (int i = 0; i < 7; i++) {
      final JPanel cell = new JPanel();
      cell.setBackground(Color.getHSBColor(i / 7f, 0.85f, 0.95f));
      targets.add(cell);
    }

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new BorderLayout());
    controls.add(targets, BorderLayout.CENTER);
    final JPanel readoutRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    readoutRow.add(readout);
    controls.add(readoutRow, BorderLayout.SOUTH);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(560, 620);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
