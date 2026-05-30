package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
 * Story #280 (S5) smoketest — accessibility + RTL. Proves: the surface reports {@code
 * AccessibleRole.DIALOG} with the headline as its accessible name; initial focus lands on the first
 * <em>content field</em> (not the close affordance — a full-screen dialog is an input flow), with a
 * blinking caret ready for typing; the focus trap keeps Tab / Shift-Tab inside the dialog; closing
 * restores focus to the trigger; and in RTL the app bar mirrors — close on the right, confirm on
 * the left, headline right-aligned.
 *
 * <p>Validate: open (LTR) — the caret is in "First name" immediately; type and it appears there
 * without clicking. Tab around: focus cycles within the dialog only (never lands on the
 * background). Press Esc → focus returns to the trigger button. Check "RTL" and reopen: the ✕ is
 * now on the right, "Save" on the left, and the headline is right-aligned; initial focus is still
 * the first field.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.dialog.playground.FullScreenDialogA11yDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class FullScreenDialogA11yDemo {

  private final JFrame frame = new JFrame("ElwhaFullScreenDialog — S5 a11y + RTL (#280)");
  private final JLabel status = new JLabel("last close: —");
  private final JCheckBox rtl = new JCheckBox("RTL (next dialog)", false);

  private FullScreenDialogA11yDemo() {}

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
    SwingUtilities.invokeLater(() -> new FullScreenDialogA11yDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JButton open = new JButton("Open full-screen dialog");
    open.addActionListener(e -> openDialog());

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    controls.add(new JLabel("Next dialog:"));
    controls.add(rtl);

    final JPanel center = new JPanel(new BorderLayout(0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    center.add(open, BorderLayout.NORTH);
    center.add(status, BorderLayout.SOUTH);

    frame.add(controls, BorderLayout.NORTH);
    frame.add(center, BorderLayout.CENTER);
    frame.setSize(820, 560);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openDialog() {
    // The dialog reads its orientation from the parent passed to show(...); flip the frame so the
    // dialog mirrors. (The background controls flip too, but the dialog covers them.)
    frame.applyComponentOrientation(
        rtl.isSelected() ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);

    ElwhaFullScreenDialog.builder()
        .headline("New event")
        .content(form())
        .confirmAction(ElwhaButton.textButton("Save"))
        .showDivider(true)
        .onClose(cause -> status.setText("last close: " + cause.name()))
        .build()
        .show(frame);
  }

  // First focusable descendant is the "First name" field — where initial focus should land.
  private JPanel form() {
    final JPanel f = new JPanel(new GridBagLayout());
    f.setOpaque(false);
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1;
    gc.insets = new Insets(8, 0, 8, 0);
    final String[] labels = {"First name", "Last name", "Email", "Notes"};
    for (int i = 0; i < labels.length; i++) {
      gc.gridy = i;
      final JPanel row = new JPanel(new BorderLayout(12, 0));
      row.setOpaque(false);
      // LINE_START so the label mirrors to the trailing edge in RTL alongside the dialog chrome.
      row.add(new JLabel(labels[i]), BorderLayout.LINE_START);
      final JTextField tf = new JTextField();
      tf.setPreferredSize(new Dimension(0, 28));
      row.add(tf, BorderLayout.CENTER);
      f.add(row, gc);
    }
    return f;
  }
}
