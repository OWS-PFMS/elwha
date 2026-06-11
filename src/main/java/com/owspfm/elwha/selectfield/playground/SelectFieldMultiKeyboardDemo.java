package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase 3 S3 demo (#399): the multi-select <strong>keyboard</strong>. A multi-select plus a plain
 * text field (the Tab/focus-return witness) and a live log of selection changes + focus owner.
 * Exercise — pointer-free: <strong>Tab</strong> to the select, <strong>Down / Enter /
 * Space</strong> opens the menu (the pure-select gestures, unchanged), <strong>Down/Up</strong>
 * walk the roving focus, <strong>Enter or Space</strong> toggles the focused item <em>without
 * closing</em> (watch the checkmark, the live summary, and the per-toggle log line),
 * <strong>Esc</strong> closes and returns focus to the field (the log shows the owner), and
 * <strong>type-ahead</strong> letters jump the focus. Screen-reader side: each item is
 * checkbox-like (CHECKED when on) and the arrow still announces expanded/collapsed.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldMultiKeyboardDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldMultiKeyboardDemo {

  private SelectFieldMultiKeyboardDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — Phase 3 S3 multi keyboard");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final JTextArea log = new JTextArea(10, 46);
          log.setEditable(false);

          final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Instruments");
          combo.setOptions(
              List.of("Violin", "Viola", "Cello", "Bass", "Flute", "Oboe", "Clarinet"));
          combo.setMultiSelect(true);
          combo.setSupportingText("Enter/Space toggles without closing · Esc closes");
          combo.addMultiSelectionChangeListener(values -> log.append("values -> " + values + "\n"));

          final ElwhaTextField other = ElwhaTextField.filled("Another field (Tab target)");

          KeyboardFocusManager.getCurrentKeyboardFocusManager()
              .addPropertyChangeListener(
                  "focusOwner",
                  evt -> {
                    if (evt.getNewValue() != null) {
                      log.append(
                          "  [focus] -> " + evt.getNewValue().getClass().getSimpleName() + "\n");
                    }
                  });

          final JPanel fields = new JPanel(new BorderLayout(0, 16));
          fields.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          fields.add(combo, BorderLayout.NORTH);
          fields.add(other, BorderLayout.SOUTH);

          final JScrollPane logScroll = new JScrollPane(log);
          logScroll.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

          final JPanel root = new JPanel(new BorderLayout());
          root.add(fields, BorderLayout.NORTH);
          root.add(logScroll, BorderLayout.CENTER);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(560, 520));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
