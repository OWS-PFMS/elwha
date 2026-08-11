package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase 3 S2 demo (#398): the multi-select <strong>summary display</strong>. A pre-seeded
 * multi-select over a long topping list with a live listener log and a summary-limit stepper.
 * Exercise: open the menu and toggle — the field summary updates <em>live on every toggle</em>
 * while the menu stays open, joining display strings in option order up to the limit and collapsing
 * to {@code N selected} past it; the log shows the per-toggle listener payload (the current values,
 * option-ordered). Step the limit down to 0 — any selection shows the count form, and the summary
 * re-renders immediately. Clear everything — the floating label rests.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldSummaryDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldSummaryDemo {

  private SelectFieldSummaryDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — Phase 3 S2 summary display");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final JTextArea log = new JTextArea(9, 44);
          log.setEditable(false);

          final ElwhaSelectField<String> combo = ElwhaSelectField.filled("Toppings");
          combo.setOptions(
              List.of(
                  "Mushroom",
                  "Pepperoni",
                  "Onion",
                  "Olive",
                  "Basil",
                  "Pineapple",
                  "Spinach",
                  "Garlic"));
          combo.setMultiSelect(true);
          combo.setSelectedValues(List.of("Pepperoni", "Basil"));
          combo.setSupportingText("Joined up to the limit, then 'N selected'");
          combo.addMultiSelectionChangeListener(values -> log.append("values -> " + values + "\n"));

          final JLabel limitLabel = new JLabel("Summary limit: " + combo.getSummaryLimit());
          final ElwhaIconButton minus = new ElwhaIconButton(MaterialIcons.remove());
          minus.addActionListener(
              e -> {
                combo.setSummaryLimit(combo.getSummaryLimit() - 1);
                limitLabel.setText("Summary limit: " + combo.getSummaryLimit());
              });
          final ElwhaIconButton plus = new ElwhaIconButton(MaterialIcons.add());
          plus.addActionListener(
              e -> {
                combo.setSummaryLimit(combo.getSummaryLimit() + 1);
                limitLabel.setText("Summary limit: " + combo.getSummaryLimit());
              });

          final ElwhaButton clear = ElwhaButton.outlinedButton("Clear selection");
          clear.addActionListener(e -> combo.setSelectedValues(List.of()));

          final JPanel controls = new JPanel();
          controls.add(minus);
          controls.add(limitLabel);
          controls.add(plus);
          controls.add(clear);

          final JPanel top = new JPanel(new BorderLayout(0, 12));
          top.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          top.add(combo, BorderLayout.NORTH);
          top.add(controls, BorderLayout.SOUTH);

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
