package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #263 (S3) smoketest — the typed action row. Each button opens a dialog with a different
 * action-role combination so the validator can confirm M3 trailing-justified order (cancel leading
 * → alternate → confirm trailing) holds regardless of which roles are present, that the 8px gap and
 * 24px-above spacing render, and that the keyboard wiring works: Enter fires the confirming action,
 * Esc fires the cancel action (or closes with ESC when there is none). The status line logs the
 * {@link ElwhaDialog.DismissCause} and which consumer listeners ran.
 *
 * <p>Validate: open "Confirm + cancel" — confirm sits rightmost, cancel left of it; press Enter and
 * the status shows "confirm listener ran → CONFIRM"; reopen and press Esc → "cancel listener ran →
 * CANCEL". The three-action dialog shows cancel → alternate → confirm left-to-right. The confirm
 * button is a filled button to show role promotion is allowed.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogActionRowDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class DialogActionRowDemo {

  private final JFrame frame = new JFrame("ElwhaDialog — S3 typed action row (#263)");
  private final JLabel status = new JLabel("Open a dialog, then click / Enter / Esc.");

  private DialogActionRowDemo() {}

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
    SwingUtilities.invokeLater(() -> new DialogActionRowDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 12));
    buttons.setBorder(BorderFactory.createEmptyBorder(40, 64, 24, 64));
    buttons.add(button("Confirm only", this::openConfirmOnly));
    buttons.add(button("Confirm + cancel", this::openConfirmCancel));
    buttons.add(button("Confirm + alternate + cancel", this::openThreeActions));
    buttons.add(status);

    frame.setContentPane(buttons);
    frame.setSize(640, 380);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openConfirmOnly() {
    final ElwhaButton ok = ElwhaButton.filledButton("Got it");
    ok.addActionListener(e -> note("confirm listener ran"));
    ElwhaDialog.builder()
        .headline("Update installed")
        .supportingText("The app will use the new version next time it starts.")
        .confirmAction(ok)
        .onClose(cause -> log(cause))
        .build()
        .show(frame);
  }

  private void openConfirmCancel() {
    final ElwhaButton delete = ElwhaButton.filledButton("Delete");
    delete.addActionListener(e -> note("confirm listener ran"));
    final ElwhaButton cancel = ElwhaButton.textButton("Cancel");
    cancel.addActionListener(e -> note("cancel listener ran"));
    ElwhaDialog.builder()
        .headline("Delete this item?")
        .supportingText("This action can't be undone.")
        .confirmAction(delete)
        .cancelAction(cancel)
        .onClose(cause -> log(cause))
        .build()
        .show(frame);
  }

  private void openThreeActions() {
    final ElwhaButton save = ElwhaButton.filledButton("Save");
    save.addActionListener(e -> note("confirm listener ran"));
    final ElwhaButton dontSave = ElwhaButton.textButton("Don't save");
    dontSave.addActionListener(e -> note("alternate listener ran"));
    final ElwhaButton cancel = ElwhaButton.textButton("Cancel");
    cancel.addActionListener(e -> note("cancel listener ran"));
    ElwhaDialog.builder()
        .headline("Save changes before closing?")
        .confirmAction(save)
        .alternateAction(dontSave)
        .cancelAction(cancel)
        .onClose(cause -> log(cause))
        .build()
        .show(frame);
  }

  private String lastNote = "—";

  private void note(final String text) {
    lastNote = text;
  }

  private void log(final ElwhaDialog.DismissCause cause) {
    status.setText(lastNote + " → " + cause.name());
    lastNote = "—";
  }

  private static JButton button(final String text, final Runnable onClick) {
    final JButton b = new JButton(text);
    b.addActionListener(e -> onClick.run());
    return b;
  }
}
