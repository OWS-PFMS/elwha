package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S4 demo (#377): variant delegation + state propagation + the owned trailing slot. A filled and an
 * outlined select sit side by side (variant comes from the {@code filled()}/{@code outlined()}
 * factories — the embedded field carries it). The dogfooded {@link ElwhaButton} controls toggle
 * <b>error</b>, <b>disabled</b>, and <b>read-only</b> on <i>both</i> fields at once, showing the
 * state propagating to the whole control (field chrome + arrow): error swaps the supporting row,
 * disabled dims everything and blocks opening, read-only keeps normal chrome but blocks opening.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldS4DelegationDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS4DelegationDemo {

  private SelectFieldS4DelegationDemo() {}

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

          final JFrame frame = new JFrame("ElwhaSelectField — S4 delegation / states");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final List<String> sizes =
              List.of("Extra small", "Small", "Medium", "Large", "Extra large");

          final ElwhaSelectField<String> filled = ElwhaSelectField.filled("Size (filled)");
          filled.setOptions(sizes);
          filled.setSupportingText("Choose a size");
          filled.setLeadingIcon(MaterialIcons.widgets(20));

          final ElwhaSelectField<String> outlined = ElwhaSelectField.outlined("Size (outlined)");
          outlined.setOptions(sizes);
          outlined.setSupportingText("Choose a size");

          final JPanel fields = new JPanel(new GridLayout(1, 2, 20, 0));
          fields.add(wrap(filled));
          fields.add(wrap(outlined));

          final boolean[] err = {false};
          final boolean[] off = {false};
          final boolean[] ro = {false};

          final ElwhaButton error = ElwhaButton.outlinedButton("Toggle error");
          error.addActionListener(
              e -> {
                err[0] = !err[0];
                for (final ElwhaSelectField<String> s : List.of(filled, outlined)) {
                  s.setError(err[0]);
                  s.setErrorText("Pick a size");
                }
              });
          final ElwhaButton disable = ElwhaButton.outlinedButton("Toggle disabled");
          disable.addActionListener(
              e -> {
                off[0] = !off[0];
                filled.setEnabled(!off[0]);
                outlined.setEnabled(!off[0]);
              });
          final ElwhaButton readonly = ElwhaButton.outlinedButton("Toggle read-only");
          readonly.addActionListener(
              e -> {
                ro[0] = !ro[0];
                filled.setReadOnly(ro[0]);
                outlined.setReadOnly(ro[0]);
              });

          final JPanel controls = new JPanel(new GridLayout(1, 3, 8, 0));
          controls.add(error);
          controls.add(disable);
          controls.add(readonly);

          final JPanel root = new JPanel(new BorderLayout(0, 20));
          root.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));
          root.add(fields, BorderLayout.CENTER);
          root.add(controls, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(620, 240));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static JPanel wrap(final ElwhaSelectField<String> select) {
    final JPanel cell = new JPanel(new BorderLayout());
    cell.add(select, BorderLayout.NORTH);
    return cell;
  }
}
