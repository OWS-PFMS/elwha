package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S1 spike demo (#374): the {@link ElwhaSelectField} composition skeleton. Two live select fields
 * (filled + outlined) over a typed option list — click the field or its trailing dropdown arrow to
 * open the anchored menu, pick an item to write its text back into the field, and reopen to see the
 * choice marked {@code selected}. The arrow flips down→up while the menu is open and the menu
 * light-dismisses (outside press / Escape), restoring focus to the field. The "Read selection"
 * button (dogfooded {@link ElwhaButton}) prints the current {@link
 * ElwhaSelectField#getSelectedValue() typed value} of each field, proving the write-back
 * round-trip.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldS1SpikeDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS1SpikeDemo {

  private SelectFieldS1SpikeDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — S1 spike");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final List<String> fruits =
              List.of("Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grapefruit");

          final ElwhaSelectField<String> filled = ElwhaSelectField.filled("Fruit (filled)");
          filled.setOptions(fruits);

          final ElwhaSelectField<String> outlined = ElwhaSelectField.outlined("Fruit (outlined)");
          outlined.setOptions(fruits);

          final JPanel fields = new JPanel(new GridLayout(2, 1, 0, 20));
          fields.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          fields.add(wrap(filled));
          fields.add(wrap(outlined));

          final JLabel readout = new JLabel("Selected: (filled) — , (outlined) — ");
          readout.setBorder(BorderFactory.createEmptyBorder(0, 28, 8, 28));

          final ElwhaButton read = ElwhaButton.filledButton("Read selection");
          read.addActionListener(
              e ->
                  readout.setText(
                      "Selected: (filled) "
                          + valueOf(filled.getSelectedValue())
                          + " , (outlined) "
                          + valueOf(outlined.getSelectedValue())));

          final JPanel south = new JPanel(new BorderLayout(0, 8));
          south.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));
          south.add(readout, BorderLayout.CENTER);
          south.add(read, BorderLayout.SOUTH);

          final JPanel root = new JPanel(new BorderLayout());
          root.add(fields, BorderLayout.CENTER);
          root.add(south, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(460, 320));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static String valueOf(final String value) {
    return value == null ? "—" : value;
  }

  /** Keeps the select field at its preferred height (top-aligned) inside a grid cell. */
  private static JPanel wrap(final ElwhaSelectField<String> select) {
    final JPanel cell = new JPanel(new BorderLayout());
    cell.add(select, BorderLayout.NORTH);
    return cell;
  }
}
