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
 * V2 S2 visual smoke for the SwatchSource toggle + THEME tier (#498): a swatches-only picker whose
 * Material/Theme toggle swaps the V1 stack for the live theme's 49-role grid. Verify the toggle
 * never moves the pane's height, picking a role commits its current resolved color, the selection
 * ring + check track the pick, and — the live-theming point — flipping dark mode or another palette
 * with the buttons re-colors every cell in place while the selection indicator follows the role's
 * new value.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerThemeTierDemo {

  private ElwhaColorPickerThemeTierDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerThemeTierDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — V2 S2 theme tier (#498)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x6750A4));
    picker.setModes(PickerMode.SWATCHES);
    picker.setSupportingText("Material / Theme sources");
    picker.setSwatchSource(SwatchSource.THEME);

    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(e -> readout.setText("Listener: " + picker.formatCurrentHex()));

    final ElwhaButton dark = ElwhaButton.filledTonalButton("Toggle dark mode");
    dark.addActionListener(
        e ->
            ElwhaTheme.install(
                ElwhaTheme.config()
                    .theme(ElwhaTheme.current().theme())
                    .mode(ElwhaTheme.current().mode() == Mode.DARK ? Mode.LIGHT : Mode.DARK)
                    .build()));

    final ElwhaButton palette = ElwhaButton.filledTonalButton("Cycle palette");
    palette.addActionListener(
        e -> {
          final var palettes = MaterialPalettes.primary();
          final int at = palettes.indexOf(ElwhaTheme.current().theme());
          ElwhaTheme.install(
              ElwhaTheme.config()
                  .theme(palettes.get((at + 1) % palettes.size()))
                  .mode(ElwhaTheme.current().mode())
                  .build());
        });

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(dark);
    controls.add(palette);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(560, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
