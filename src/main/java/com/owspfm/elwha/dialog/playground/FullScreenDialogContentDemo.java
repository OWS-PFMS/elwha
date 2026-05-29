package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #279 (S4) smoketest — edge-to-edge scrolling content + dismiss semantics. Proves that a
 * tall form scrolls inside the dialog while the top app bar stays pinned, that the scroll divider
 * under the bar appears once the content is scrolling, and that every dismiss path reports the
 * right {@code DismissCause}: the leading ✕ and Esc both report {@code CANCEL}, the trailing "Save"
 * reports {@code CONFIRM}.
 *
 * <p>Validate: open the dialog — the 16-row form scrolls (the divider appears under the pinned app
 * bar); scroll back to the top and the divider stays (content still overflows). Press the leading ✕
 * → status "CANCEL"; reopen, press Esc → "CANCEL"; reopen, press Enter or click "Save" → "CONFIRM".
 * The form fields are editable (input reaches the dialog) while the background button below stays
 * inert.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.dialog.playground.FullScreenDialogContentDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class FullScreenDialogContentDemo {

  private final JFrame frame = new JFrame("ElwhaFullScreenDialog — S4 content + dismiss (#279)");
  private final JLabel status = new JLabel("last close: —");

  private FullScreenDialogContentDemo() {}

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
    SwingUtilities.invokeLater(() -> new FullScreenDialogContentDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JButton open = new JButton("Open full-screen dialog");
    open.addActionListener(e -> openDialog());

    final JPanel center = new JPanel(new BorderLayout(0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    center.add(open, BorderLayout.NORTH);
    center.add(status, BorderLayout.SOUTH);

    frame.add(center, BorderLayout.CENTER);
    frame.setSize(820, 560);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openDialog() {
    final ElwhaButton save = ElwhaButton.textButton("Save");

    ElwhaFullScreenDialog.builder()
        .headline("New event")
        .content(tallForm())
        .confirmAction(save)
        .onClose(cause -> status.setText("last close: " + cause.name()))
        .build()
        .show(frame);
  }

  // A 16-row labeled-field form, tall enough to scroll in the demo frame.
  private JPanel tallForm() {
    final JPanel form = new JPanel(new GridBagLayout());
    form.setOpaque(false);
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1;
    gc.insets = new Insets(8, 0, 8, 0);
    for (int i = 1; i <= 16; i++) {
      gc.gridy = i;
      final JPanel row = new JPanel(new BorderLayout(12, 0));
      row.setOpaque(false);
      row.add(new JLabel("Field " + i), BorderLayout.WEST);
      final JTextField tf = new JTextField();
      tf.setPreferredSize(new Dimension(0, 28));
      row.add(tf, BorderLayout.CENTER);
      form.add(row, gc);
    }
    return form;
  }
}
