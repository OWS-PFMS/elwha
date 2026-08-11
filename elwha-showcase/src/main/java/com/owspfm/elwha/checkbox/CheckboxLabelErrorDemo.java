package com.owspfm.elwha.checkbox;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
 * S3 demo (story #413) — {@link ElwhaCheckbox} labels, the M3 error treatment, and accessibility:
 * labeled checkboxes (clicking the label toggles), a long label truncating with an ellipsis at a
 * constrained width, the error matrix across all three states (plus error + disabled, where
 * disabled wins), and an accessible-name spot check printed to stdout.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class CheckboxLabelErrorDemo {

  private final JFrame frame = new JFrame("ElwhaCheckbox — S3 label + error + a11y");
  private Mode mode = Mode.LIGHT;

  private CheckboxLabelErrorDemo() {}

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
    SwingUtilities.invokeLater(() -> new CheckboxLabelErrorDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 4));
    rows.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

    rows.add(section("Labels — click the text, it toggles"));
    rows.add(new ElwhaCheckbox("Subscribe to the newsletter"));
    final ElwhaCheckbox checkedLabeled = new ElwhaCheckbox("Remember this device");
    checkedLabeled.setChecked(true);
    rows.add(checkedLabeled);
    final ElwhaCheckbox longLabel =
        new ElwhaCheckbox(
            "A deliberately long label that will not fit and must truncate with an ellipsis");
    longLabel.setPreferredSize(new Dimension(280, 48));
    rows.add(longLabel);
    final ElwhaCheckbox disabledLabeled = new ElwhaCheckbox("Disabled with label");
    disabledLabeled.setChecked(true);
    disabledLabeled.setEnabled(false);
    rows.add(disabledLabeled);

    rows.add(section("Error treatment (orthogonal flag; disabled wins)"));
    final JPanel errorRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 0));
    errorRow.add(errorBox(CheckState.UNCHECKED, true, "Unchecked"));
    errorRow.add(errorBox(CheckState.CHECKED, true, "Checked"));
    errorRow.add(errorBox(CheckState.INDETERMINATE, true, "Indeterminate"));
    errorRow.add(errorBox(CheckState.CHECKED, false, "Error + disabled"));
    rows.add(errorRow);

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rows, BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    printA11ySpotCheck();
  }

  private static JLabel section(final String text) {
    return new JLabel(text, SwingConstants.LEADING);
  }

  private static ElwhaCheckbox errorBox(
      final CheckState state, final boolean enabled, final String label) {
    final ElwhaCheckbox box = new ElwhaCheckbox(label);
    box.setCheckState(state);
    box.setErrorShown(true);
    box.setEnabled(enabled);
    return box;
  }

  private static void printA11ySpotCheck() {
    final ElwhaCheckbox labeled = new ElwhaCheckbox("Accept terms");
    labeled.setChecked(true);
    final ElwhaCheckbox bare = new ElwhaCheckbox();
    bare.setAccessibleLabel("Select all rows");
    bare.setErrorShown(true);
    bare.setIndeterminate(true);
    System.out.println("a11y spot check:");
    System.out.println(
        "  labeled  → role="
            + labeled.getAccessibleContext().getAccessibleRole()
            + " name=\""
            + labeled.getAccessibleContext().getAccessibleName()
            + "\" states="
            + labeled.getAccessibleContext().getAccessibleStateSet());
    System.out.println(
        "  bare     → name=\""
            + bare.getAccessibleContext().getAccessibleName()
            + "\" desc=\""
            + bare.getAccessibleContext().getAccessibleDescription()
            + "\" states="
            + bare.getAccessibleContext().getAccessibleStateSet());
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
