package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
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
 * Phase 3 S1 demo (#397): the multi-select spike. One select field with a <strong>Multi-select
 * toggle</strong> and a live log. Exercise: turn multi on and open the menu — toggling options
 * checks/unchecks them <em>without closing</em> (Esc, a press outside, or the arrow close it), and
 * the field shows the provisional summary (display strings joined in option order, whatever order
 * you toggled in). The "Read values" button logs {@code getSelectedValues()} (option order) and
 * {@code getSelectedValue()} (the first). Flip multi off — the selection collapses to its first
 * value; flip it back on — the single value seeds the selection. "Seed Mars+Venus" drives {@code
 * setSelectedValues} programmatically (note the option-order result).
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldMultiSpikeDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldMultiSpikeDemo {

  private SelectFieldMultiSpikeDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — Phase 3 S1 multi-select spike");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final JTextArea log = new JTextArea(9, 44);
          log.setEditable(false);

          final ElwhaSelectField<String> combo = ElwhaSelectField.outlined("Planets");
          combo.setOptions(
              List.of("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Neptune"));
          combo.setSupportingText("Toggle options — the menu stays open");

          final ElwhaButton multi =
              ElwhaButton.filledTonalButton("Multi-select")
                  .setInteractionMode(ButtonInteractionMode.SELECTABLE);
          multi.addActionListener(
              e -> {
                combo.setMultiSelect(multi.isSelected());
                log.append("multiSelect -> " + combo.isMultiSelect() + "\n");
              });

          final ElwhaButton read = ElwhaButton.outlinedButton("Read values");
          read.addActionListener(
              e ->
                  log.append(
                      "values="
                          + combo.getSelectedValues()
                          + " first="
                          + combo.getSelectedValue()
                          + "\n"));

          final ElwhaButton seed = ElwhaButton.outlinedButton("Seed Mars+Venus");
          seed.addActionListener(
              e -> {
                combo.setSelectedValues(List.of("Mars", "Venus"));
                log.append("seeded -> " + combo.getSelectedValues() + "\n");
              });

          final JPanel buttons = new JPanel();
          buttons.add(multi);
          buttons.add(read);
          buttons.add(seed);

          final JPanel top = new JPanel(new BorderLayout(0, 12));
          top.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          top.add(combo, BorderLayout.NORTH);
          top.add(buttons, BorderLayout.SOUTH);

          final JScrollPane logScroll = new JScrollPane(log);
          logScroll.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

          final JPanel root = new JPanel(new BorderLayout());
          root.add(top, BorderLayout.NORTH);
          root.add(logScroll, BorderLayout.CENTER);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(560, 480));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
