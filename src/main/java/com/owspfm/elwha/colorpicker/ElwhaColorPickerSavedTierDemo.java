package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * V2 S3 visual smoke for the SAVED tier (#499): a swatches-only picker opened on the Saved card,
 * pre-seeded with three favorites. Verify "Save current" appends the headline color (dedup is a
 * no-op — press it twice), picking a saved cell commits it exactly, Delete/Backspace removes the
 * focused cell, right-click opens the "Remove from saved" menu, the empty-state hint appears after
 * removing everything, and the favorites-listener readout counts every mutation (the client-owned
 * persistence hook).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSavedTierDemo {

  private ElwhaColorPickerSavedTierDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerSavedTierDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — V2 S3 saved tier (#499)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF7043));
    picker.setModes(PickerMode.SWATCHES);
    picker.setSupportingText("Saved swatches");
    picker.setFavorites(List.of(new Color(0x6750A4), new Color(0x2E7D32), new Color(0xFFB300)));
    picker.setSwatchSource(SwatchSource.SAVED);

    final JLabel readout =
        new JLabel("Favorites listener: " + picker.getFavorites().size() + " saved");
    picker.addFavoritesListener(
        e -> readout.setText("Favorites listener: " + picker.getFavorites().size() + " saved"));

    final ElwhaButton reseed = ElwhaButton.filledTonalButton("Restore the three seeds");
    reseed.addActionListener(
        e ->
            picker.setFavorites(
                List.of(new Color(0x6750A4), new Color(0x2E7D32), new Color(0xFFB300))));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(reseed);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(560, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
