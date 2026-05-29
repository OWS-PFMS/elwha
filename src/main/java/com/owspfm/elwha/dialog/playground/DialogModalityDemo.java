package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #261 (S1) smoketest — the modality overlay. Proves the in-window scrim overlay on the host
 * frame's layered pane: while a dialog is open the background buttons + text field are inert (their
 * click counter does not advance, the field can't be typed into), the scrim dims the app content,
 * and the dialog dismisses on scrim click (when enabled) or Esc — restoring focus to the trigger.
 *
 * <p>Validate: with the dialog closed, clicking "Background button" advances the counter and the
 * field is editable. Open the dialog; now neither responds — input is blocked. Toggle
 * "scrim-dismissible" off and confirm a scrim click no longer closes it (Esc still does unless that
 * is toggled off too). Every close logs its {@link ElwhaDialog.DismissCause}.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogModalityDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class DialogModalityDemo {

  private final JFrame frame = new JFrame("ElwhaDialog — S1 modality overlay (#261)");
  private final JLabel status = new JLabel("Background clicks: 0   |   last close: —");
  private int backgroundClicks;

  private DialogModalityDemo() {}

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
    SwingUtilities.invokeLater(() -> new DialogModalityDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JCheckBox scrimDismiss = new JCheckBox("scrim-dismissible", true);
    final JCheckBox escDismiss = new JCheckBox("esc-dismissible", true);

    final JButton bg = new JButton("Background button");
    bg.addActionListener(
        e -> {
          backgroundClicks++;
          refresh("—");
        });
    final JTextField field = new JTextField("type here when no dialog is open", 28);

    final JButton open = new JButton("Open dialog");
    open.addActionListener(e -> openDialog(scrimDismiss.isSelected(), escDismiss.isSelected()));

    final JPanel center = new JPanel(new GridLayout(0, 1, 0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    center.add(open);
    center.add(bg);
    center.add(field);
    center.add(status);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    controls.add(new JLabel("Next dialog:"));
    controls.add(scrimDismiss);
    controls.add(escDismiss);

    frame.add(controls, BorderLayout.NORTH);
    frame.add(center, BorderLayout.CENTER);
    frame.setSize(720, 460);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openDialog(final boolean scrimDismiss, final boolean escDismiss) {
    ElwhaDialog.builder()
        .headline("Reset to defaults?")
        .dismissibleByScrim(scrimDismiss)
        .dismissibleByEsc(escDismiss)
        .onClose(cause -> refresh(cause.name()))
        .build()
        .show(frame);
  }

  private void refresh(final String lastClose) {
    status.setText("Background clicks: " + backgroundClicks + "   |   last close: " + lastClose);
  }
}
