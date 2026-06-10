package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase 2 S2 demo (#392): {@link ElwhaSelectField} filter-as-you-type. Two editable combos — a
 * short fruit list (watch the open menu narrow live as you type, show the disabled "No matches" row
 * when nothing matches, and restore the full list when you clear or pick) and a long generated list
 * that opens scrollable (the filter shrinks the scroll viewport in place). Typing opens the menu;
 * the match is case-insensitive against the display string (prefix and substring both qualify);
 * picking an option clears the filter so the next open shows everything again.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldFilterDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldFilterDemo {

  private SelectFieldFilterDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — Phase 2 S2 filter-as-you-type");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final ElwhaSelectField<String> fruits = ElwhaSelectField.outlined("Fruit");
          fruits.setOptions(
              List.of(
                  "Apple",
                  "Apricot",
                  "Banana",
                  "Blueberry",
                  "Cherry",
                  "Cranberry",
                  "Date",
                  "Elderberry",
                  "Fig",
                  "Grapefruit",
                  "Mango",
                  "Papaya"));
          fruits.setEditable(true);
          fruits.setSupportingText("Type to filter — try \"berry\", then something junk");

          final ElwhaSelectField<String> wide = ElwhaSelectField.filled("Long list (scrolls)");
          wide.setOptions(
              IntStream.rangeClosed(1, 40).mapToObj(i -> "Option " + i + " of many").toList());
          wide.setEditable(true);
          wide.setSupportingText("Opens scrollable; filtering shrinks it in place");

          final JPanel fields = new JPanel(new GridLayout(2, 1, 0, 20));
          fields.setBorder(BorderFactory.createEmptyBorder(28, 28, 12, 28));
          fields.add(wrap(fruits));
          fields.add(wrap(wide));

          final JLabel hint =
              new JLabel("Esc closes · picking writes back + clears the filter · arrow reopens");
          hint.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

          final JPanel root = new JPanel(new BorderLayout());
          root.add(fields, BorderLayout.CENTER);
          root.add(hint, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(520, 360));
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
