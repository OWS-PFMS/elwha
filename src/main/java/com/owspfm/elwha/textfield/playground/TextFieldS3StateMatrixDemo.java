package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S3 demo (#335): the full variant&#215;state matrix — Filled and Outlined columns across enabled,
 * focus-cue, error, disabled, and read-only rows — so the chrome color table (active indicator /
 * outline, label, error-beats-focus, the outlined label-notch) can be eye-confirmed against the M3
 * render, including the 3dp Expressive focus stroke.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS3StateMatrixDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS3StateMatrixDemo {

  private TextFieldS3StateMatrixDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());

          final JPanel grid = new JPanel(new GridBagLayout());
          grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
          final GridBagConstraints gbc = new GridBagConstraints();
          gbc.insets = new Insets(8, 12, 8, 12);
          gbc.anchor = GridBagConstraints.WEST;

          header(grid, gbc);
          int row = 1;
          row = stateRow(grid, gbc, row, "Enabled", false, false, false, false);
          row = stateRow(grid, gbc, row, "Populated", false, false, false, true);
          row = stateRow(grid, gbc, row, "Error", true, false, false, true);
          row = stateRow(grid, gbc, row, "Disabled", false, true, false, true);
          stateRow(grid, gbc, row, "Read-only", false, false, true, true);

          final JFrame frame = new JFrame("ElwhaTextField — S3 variant × state matrix (#335)");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          frame.setContentPane(grid);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static void header(final JPanel grid, final GridBagConstraints gbc) {
    gbc.gridy = 0;
    gbc.gridx = 0;
    grid.add(new JLabel("State"), gbc);
    gbc.gridx = 1;
    grid.add(new JLabel("Filled"), gbc);
    gbc.gridx = 2;
    grid.add(new JLabel("Outlined"), gbc);
  }

  private static int stateRow(
      final JPanel grid,
      final GridBagConstraints gbc,
      final int row,
      final String caption,
      final boolean error,
      final boolean disabled,
      final boolean readOnly,
      final boolean populated) {
    gbc.gridy = row;
    gbc.gridx = 0;
    grid.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    grid.add(
        field(ElwhaTextField.Variant.FILLED, caption, error, disabled, readOnly, populated), gbc);
    gbc.gridx = 2;
    grid.add(
        field(ElwhaTextField.Variant.OUTLINED, caption, error, disabled, readOnly, populated), gbc);
    return row + 1;
  }

  private static ElwhaTextField field(
      final ElwhaTextField.Variant variant,
      final String label,
      final boolean error,
      final boolean disabled,
      final boolean readOnly,
      final boolean populated) {
    final ElwhaTextField field = new ElwhaTextField(variant, label);
    if (populated) {
      field.setText("Value");
    }
    field.setError(error);
    field.setReadOnly(readOnly);
    field.setEnabled(!disabled);
    return field;
  }
}
