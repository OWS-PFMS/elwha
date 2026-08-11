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
 * S5 demo (#337): the visual-only error contract. Toggle error to watch the chrome flip to the
 * error color (beating focus), the error text replace the supporting line with no layout shift, and
 * the non-color error icon auto-fill the trailing slot. A second field keeps its own trailing
 * affordance, so the auto error icon stays suppressed there.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS5ErrorDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS5ErrorDemo {

  private TextFieldS5ErrorDemo() {}

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

          final ElwhaTextField email = ElwhaTextField.outlined("Email");
          email.setText("not-an-email");
          email.setSupportingText("We'll only use this to contact you");

          gbc.gridx = 0;
          root.add(new JLabel("Email (auto error icon):"), gbc);
          gbc.gridx = 1;
          root.add(email, gbc);
          gbc.gridy++;

          final ElwhaButton toggle = ElwhaButton.filledTonalButton("Toggle error");
          toggle.addActionListener(
              e -> {
                final boolean next = !email.isError();
                email.setError(next);
                email.setErrorText(next ? "Enter a valid email address" : "");
              });
          gbc.gridx = 1;
          root.add(toggle, gbc);

          final JFrame frame = new JFrame("ElwhaTextField — S5 error API + auto error-icon (#337)");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
