package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S3 demo (#376): the combobox keyboard + expanded/collapsed a11y + arrow rotation. Focus the field
 * and press <b>Down</b>, <b>Up</b>, <b>Enter</b>, or <b>Space</b> to open the menu (focus moves
 * into it); type a letter to <b>type-ahead</b> to a matching option; press <b>Esc</b> to close and
 * return focus to the field. The trailing arrow <b>rotates</b> down→up as the menu opens — toggle
 * the dogfooded {@link ElwhaButton} to flip reduced-motion and watch the rotation become an instant
 * swap. The arrow's accessible name flips between "Open options" / "Close options" for screen
 * readers.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldS3KeyboardDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS3KeyboardDemo {

  private SelectFieldS3KeyboardDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — S3 keyboard / a11y");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
          select.setOptions(
              List.of(
                  "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"));

          final JLabel hint =
              new JLabel(
                  "<html>Focus the field — <b>Down/Up/Enter/Space</b> open, type a letter to jump,"
                      + " <b>Esc</b> closes.</html>");
          hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

          final boolean[] reduced = {MorphAnimator.isReducedMotion()};
          final ElwhaButton motion = ElwhaButton.outlinedButton(label(reduced[0]));
          motion.addActionListener(
              e -> {
                reduced[0] = !reduced[0];
                MorphAnimator.setReducedMotion(reduced[0]);
                motion.setText(label(reduced[0]));
              });

          final JPanel selectCell = new JPanel(new BorderLayout());
          selectCell.add(select, BorderLayout.NORTH);

          final JPanel root = new JPanel(new BorderLayout(0, 16));
          root.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));
          root.add(hint, BorderLayout.NORTH);
          root.add(selectCell, BorderLayout.CENTER);
          root.add(motion, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(460, 220));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static String label(final boolean reduced) {
    return reduced ? "Reduced motion: ON (instant)" : "Reduced motion: OFF (animate)";
  }
}
