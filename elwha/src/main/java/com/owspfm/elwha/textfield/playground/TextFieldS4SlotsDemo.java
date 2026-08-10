package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
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
 * S4 demo (#336): the anatomy slots laid out to the M3 redlines — leading icon, an interactive
 * trailing clear button ({@link ElwhaIconButton}), prefix/suffix affixes, supporting text, the
 * required asterisk, and a right-to-left row to confirm leading/trailing mirror with {@link
 * ComponentOrientation}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS4SlotsDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS4SlotsDemo {

  private TextFieldS4SlotsDemo() {}

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

          final ElwhaTextField leading = ElwhaTextField.outlined("Leading icon");
          leading.setLeadingIcon(MaterialIcons.info());

          final ElwhaTextField trailing = ElwhaTextField.filled("Trailing clear");
          trailing.setText("Clearable");
          final ElwhaIconButton clear =
              new ElwhaIconButton(MaterialIcons.close())
                  .setVariant(IconButtonVariant.STANDARD)
                  .setButtonSize(IconButtonSize.M);
          clear.addActionListener(e -> trailing.setText(""));
          trailing.setTrailingIconButton(clear);

          final ElwhaTextField both = ElwhaTextField.outlined("Both icons");
          both.setLeadingIcon(MaterialIcons.favorite());
          both.setTrailingIcon(MaterialIcons.star());

          final ElwhaTextField prefixSuffix = ElwhaTextField.filled("Amount");
          prefixSuffix.setText("1.43");
          prefixSuffix.setPrefixText("$");
          prefixSuffix.setSuffixText("USD");

          final ElwhaTextField supporting = ElwhaTextField.outlined("With supporting");
          supporting.setSupportingText("Helper text below the field");

          final ElwhaTextField requiredField = ElwhaTextField.filled("Required");
          requiredField.setRequired(true);

          final ElwhaTextField rtl = ElwhaTextField.outlined("RTL");
          rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
          rtl.setLeadingIcon(MaterialIcons.info());
          rtl.setTrailingIcon(MaterialIcons.close());
          rtl.setSupportingText("leading on the right");

          addRow(root, gbc, "Leading icon:", leading);
          addRow(root, gbc, "Trailing clear button:", trailing);
          addRow(root, gbc, "Leading + trailing:", both);
          addRow(root, gbc, "Prefix + suffix:", prefixSuffix);
          addRow(root, gbc, "Supporting text:", supporting);
          addRow(root, gbc, "Required (asterisk):", requiredField);
          addRow(root, gbc, "RTL mirror:", rtl);

          final JFrame frame = new JFrame("ElwhaTextField — S4 slots + measurements (#336)");
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
