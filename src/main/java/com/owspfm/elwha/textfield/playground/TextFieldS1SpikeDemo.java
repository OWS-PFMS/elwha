package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.button.ElwhaButton;
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
 * S1 spike demo (#333): proves the {@link ElwhaTextField} decorator over an embedded {@code
 * JTextField} — both variant chrome skeletons, token theming, hover/focus/disabled/read-only
 * driving the chrome, the label-float mechanism, and the error&#8594;alert toggle.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS1SpikeDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS1SpikeDemo {

  private TextFieldS1SpikeDemo() {}

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

          final JPanel root = new JPanel(new GridBagLayout());
          root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
          final GridBagConstraints gbc = new GridBagConstraints();
          gbc.insets = new Insets(8, 8, 8, 8);
          gbc.anchor = GridBagConstraints.WEST;
          gbc.gridx = 0;
          gbc.gridy = 0;

          final ElwhaTextField filled = new ElwhaTextField(ElwhaTextField.Variant.FILLED, "Filled");
          final ElwhaTextField outlined =
              new ElwhaTextField(ElwhaTextField.Variant.OUTLINED, "Outlined");
          final ElwhaTextField populated = ElwhaTextField.filled("Populated");
          populated.setText("Hello Elwha");
          final ElwhaTextField disabled = ElwhaTextField.outlined("Disabled");
          disabled.setEnabled(false);
          final ElwhaTextField readOnly = ElwhaTextField.filled("Read only");
          readOnly.setText("Cannot edit");
          readOnly.setReadOnly(true);

          addRow(root, gbc, "Filled (empty):", filled);
          addRow(root, gbc, "Outlined (empty):", outlined);
          addRow(root, gbc, "Filled (populated):", populated);
          addRow(root, gbc, "Outlined (disabled):", disabled);
          addRow(root, gbc, "Filled (read-only):", readOnly);

          final ElwhaButton toggleError = ElwhaButton.filledTonalButton("Toggle error on Filled");
          toggleError.addActionListener(e -> filled.setError(!filled.isError()));
          gbc.gridx = 0;
          gbc.gridwidth = 2;
          root.add(toggleError, gbc);

          final JFrame frame = new JFrame("ElwhaTextField — S1 decorator spike (#333)");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static void addRow(
      final JPanel root,
      final GridBagConstraints gbc,
      final String caption,
      final ElwhaTextField field) {
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    root.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    root.add(field, gbc);
    gbc.gridy++;
  }
}
