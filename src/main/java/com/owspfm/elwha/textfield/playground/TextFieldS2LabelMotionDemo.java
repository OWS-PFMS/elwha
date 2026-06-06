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
 * S2 demo (#334): the floating label and placeholder. Focus an empty field to watch the label
 * scale-and-rise from centered to top; the placeholder fades in beneath it; a label-less field
 * keeps its input vertically centered.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS2LabelMotionDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS2LabelMotionDemo {

  private TextFieldS2LabelMotionDemo() {}

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
          gbc.gridy = 0;

          final ElwhaTextField empty = ElwhaTextField.filled("Full name");
          empty.setPlaceholder("e.g. Ada Lovelace");

          final ElwhaTextField populated = ElwhaTextField.outlined("Email");
          populated.setText("ada@analytical.engine");

          final ElwhaTextField placeholderOnly = ElwhaTextField.filled("");
          placeholderOnly.setPlaceholder("Search (label-less, centered)");

          addRow(root, gbc, "Empty + placeholder (focus me):", empty);
          addRow(root, gbc, "Populated (label pre-floated):", populated);
          addRow(root, gbc, "Label-less (adjacent-label):", placeholderOnly);

          final JFrame frame =
              new JFrame("ElwhaTextField — S2 floating label + placeholder (#334)");
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
    gbc.gridx = 0;
    root.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    root.add(field, gbc);
    gbc.gridy++;
  }
}
