package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #265 (S5) smoketest — accessibility + RTL. Opens dialogs that exercise the focus contract
 * (§10) and right-to-left mirroring (§11). The dialog reports {@code AccessibleRole.DIALOG} with
 * the headline as its accessible name; on open, focus moves to the confirming action; Tab /
 * Shift-Tab cycle only within the dialog (the background buttons are never reachable); on close,
 * focus returns to the trigger.
 *
 * <p>Validate: open the LTR dialog — focus lands on "Save"; Tab walks Save → Cancel → the text
 * field → back to Save and never escapes to the background; press Esc and the focus ring returns to
 * the trigger button. Open the RTL dialog — the headline/supporting text are right-aligned and the
 * action row mirrors (confirm leftmost). A screen reader announces "Save changes, dialog".
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogAccessibilityDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class DialogAccessibilityDemo {

  private final JFrame frame = new JFrame("ElwhaDialog — S5 a11y + RTL (#265)");

  private DialogAccessibilityDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new DialogAccessibilityDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final JButton ltr = new JButton("Open dialog (LTR) — focus trap + restore");
    ltr.addActionListener(e -> openDialog(ltr));

    final JButton rtl = new JButton("Open dialog (RTL) — mirrored layout");
    rtl.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    rtl.addActionListener(e -> openDialog(rtl));

    final JButton background = new JButton("Background button (must be unreachable while open)");

    final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 12));
    buttons.setBorder(BorderFactory.createEmptyBorder(40, 56, 40, 56));
    buttons.add(ltr);
    buttons.add(rtl);
    buttons.add(background);
    buttons.add(new JLabel("Trigger orientation drives the dialog's LTR/RTL."));

    frame.setContentPane(buttons);
    frame.setSize(680, 380);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // Shown from the trigger button so the dialog inherits that button's component orientation, and
  // focus restores to it on close.
  private void openDialog(final JComponent trigger) {
    final JTextField field = new JTextField("editable content field", 20);
    final JPanel content = new JPanel();
    content.setOpaque(false);
    content.add(field);

    final ElwhaButton save = ElwhaButton.filledButton("Save");
    final ElwhaButton cancel = ElwhaButton.textButton("Cancel");
    ElwhaDialog.builder()
        .headline("Save changes?")
        .supportingText("Name this revision before saving it to the shared workspace.")
        .content(content)
        .confirmAction(save)
        .cancelAction(cancel)
        .build()
        .show(trigger);
  }
}
