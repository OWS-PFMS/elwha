package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase 2 S3 demo (#393): the editable combo's value model, keyboard, and commit semantics. One
 * editable combo plus a plain {@link ElwhaTextField} (a focus-loss target) and a live event log.
 * Exercise: type a partial match and press <strong>Down/Up</strong> to walk the filtered menu's
 * highlight, <strong>Enter</strong> to commit it; type an exact option name (any case) and press
 * Enter with the menu closed to resolve it to the canonical option; type junk and press Enter —
 * constrained mode reverts to the last committed value, free-text mode (toggle below) keeps it with
 * a {@code null} selected value; <strong>Esc</strong> with the menu closed reverts; Tab into the
 * other field to see the focus-loss commit. Every selection-change event lands in the log.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldCommitDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldCommitDemo {

  private SelectFieldCommitDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — Phase 2 S3 commit semantics");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final JTextArea log = new JTextArea(8, 40);
          log.setEditable(false);

          final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Language");
          combo.setOptions(
              List.of("Java", "JavaScript", "Kotlin", "Scala", "Clojure", "Python", "Rust"));
          combo.setEditable(true);
          combo.setSupportingText("Down/Up walk the menu · Enter commits · Esc reverts");
          combo.addSelectionChangeListener(
              value ->
                  log.append(
                      "selection -> "
                          + (value == null ? "null (free text: " + combo.getText() + ")" : value)
                          + "\n"));

          final ElwhaTextField other = ElwhaTextField.filled("Another field (Tab target)");

          final ElwhaButton freeText =
              ElwhaButton.filledTonalButton("Allow free text")
                  .setInteractionMode(ButtonInteractionMode.SELECTABLE);
          freeText.addActionListener(
              e -> {
                combo.setFreeTextAllowed(freeText.isSelected());
                log.append("freeTextAllowed -> " + freeText.isSelected() + "\n");
              });

          final JPanel fields = new JPanel(new BorderLayout(0, 16));
          fields.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          fields.add(combo, BorderLayout.NORTH);
          fields.add(other, BorderLayout.CENTER);
          fields.add(freeText, BorderLayout.SOUTH);

          final JScrollPane logScroll = new JScrollPane(log);
          logScroll.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

          final JPanel root = new JPanel(new BorderLayout());
          root.add(fields, BorderLayout.NORTH);
          root.add(logScroll, BorderLayout.CENTER);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(520, 460));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
