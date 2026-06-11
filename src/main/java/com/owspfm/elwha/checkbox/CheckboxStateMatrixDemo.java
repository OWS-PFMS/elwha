package com.owspfm.elwha.checkbox;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S1 demo (story #411) — the {@link ElwhaCheckbox} static visual contract: all three {@link
 * CheckState}s across enabled and disabled, light/dark toggle, zero new tokens. No interaction yet
 * (S2).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class CheckboxStateMatrixDemo {

  private final JFrame frame = new JFrame("ElwhaCheckbox — S1 state matrix");
  private Mode mode = Mode.LIGHT;

  private CheckboxStateMatrixDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(() -> new CheckboxStateMatrixDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(0, 4, 16, 8));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(new JLabel(""));
    grid.add(header("Unchecked"));
    grid.add(header("Checked"));
    grid.add(header("Indeterminate"));
    grid.add(header("Enabled"));
    grid.add(checkbox(CheckState.UNCHECKED, true));
    grid.add(checkbox(CheckState.CHECKED, true));
    grid.add(checkbox(CheckState.INDETERMINATE, true));
    grid.add(header("Disabled"));
    grid.add(checkbox(CheckState.UNCHECKED, false));
    grid.add(checkbox(CheckState.CHECKED, false));
    grid.add(checkbox(CheckState.INDETERMINATE, false));

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JLabel header(final String text) {
    return new JLabel(text, SwingConstants.CENTER);
  }

  private static JPanel checkbox(final CheckState state, final boolean enabled) {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(state);
    box.setEnabled(enabled);
    final JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    cell.add(box);
    return cell;
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
