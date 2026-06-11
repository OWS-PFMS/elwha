package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ButtonInteractionMode;
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
 * Phase 2 S1 spike demo (#391): the {@link ElwhaSelectField} editable mode. Two live select fields
 * — an <strong>editable</strong> combo (typeable; the menu opens on typing, Down/Alt+Down, or the
 * trailing arrow, and keyboard focus stays in the editor the whole time the menu is open) beside a
 * <strong>pure select</strong> control (read-only, field body toggles — Phase 1 behavior,
 * unchanged). Type free text into the editable field and watch it stick; pick a menu item and watch
 * the write-back still land; press the arrow while open to close. The "Editable" toggle (dogfooded
 * {@code SELECTABLE} {@link ElwhaButton}) flips the first field between the two modes live.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldEditableSpikeDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldEditableSpikeDemo {

  private SelectFieldEditableSpikeDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — Phase 2 S1 editable spike");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final List<String> languages =
              List.of("Java", "Kotlin", "Scala", "Clojure", "Groovy", "Python", "Rust");

          final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Language (editable)");
          combo.setOptions(languages);
          combo.setEditable(true);
          combo.setSupportingText("Type freely, Down/arrow opens, pick writes back");

          final ElwhaSelectField<String> pure = ElwhaSelectField.filled("Language (pure select)");
          pure.setOptions(languages);
          pure.setSupportingText("Phase 1 behavior — unchanged");

          final JPanel fields = new JPanel(new GridLayout(2, 1, 0, 20));
          fields.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          fields.add(wrap(combo));
          fields.add(wrap(pure));

          final JLabel readout = new JLabel("Value: — · Text: —");
          readout.setBorder(BorderFactory.createEmptyBorder(0, 28, 8, 28));

          final ElwhaButton editableToggle =
              ElwhaButton.filledTonalButton("Editable")
                  .setInteractionMode(ButtonInteractionMode.SELECTABLE)
                  .setSelected(true);
          editableToggle.addActionListener(e -> combo.setEditable(editableToggle.isSelected()));

          final ElwhaButton read = ElwhaButton.filledButton("Read combo state");
          read.addActionListener(
              e ->
                  readout.setText(
                      "Value: "
                          + (combo.getSelectedValue() == null ? "—" : combo.getSelectedValue())
                          + " · Text: "
                          + (combo.getText().isEmpty() ? "—" : combo.getText())));

          final JPanel controls = new JPanel(new GridLayout(1, 2, 8, 0));
          controls.add(editableToggle);
          controls.add(read);

          final JPanel south = new JPanel(new BorderLayout(0, 8));
          south.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));
          south.add(readout, BorderLayout.CENTER);
          south.add(controls, BorderLayout.SOUTH);

          final JPanel root = new JPanel(new BorderLayout());
          root.add(fields, BorderLayout.CENTER);
          root.add(south, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(480, 360));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  /** Keeps the select field at its preferred height (top-aligned) inside a grid cell. */
  private static JPanel wrap(final ElwhaSelectField<String> select) {
    final JPanel cell = new JPanel(new BorderLayout());
    cell.add(select, BorderLayout.NORTH);
    return cell;
  }
}
