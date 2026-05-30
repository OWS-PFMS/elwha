package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #278 (S3) smoketest — the inline top app bar. Proves the {@value
 * com.owspfm.elwha.dialog.ElwhaFullScreenDialog#APP_BAR_PX}px bar: a leading close affordance (the
 * new {@code MaterialIcons.close()} glyph), a start-aligned {@code TITLE_LARGE} headline, an
 * optional trailing confirm text button, and an optional 1px divider under the bar. The keyboard
 * wiring matches the Basic Dialog: Enter fires the confirm action, Esc fires the close affordance.
 *
 * <p>Validate: open with "confirm" checked — a "Save" text button sits at the trailing edge; press
 * Enter and the status shows "confirm listener ran → CONFIRM". Reopen and click the leading ✕ (or
 * press Esc) → "CANCEL". Toggle "divider" and confirm the 1px line under the bar
 * appears/disappears. Toggle "confirm" off and the bar is close + headline only.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.dialog.playground.FullScreenDialogAppBarDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class FullScreenDialogAppBarDemo {

  private final JFrame frame = new JFrame("ElwhaFullScreenDialog — S3 top app bar (#278)");
  private final JLabel status = new JLabel("last close: —");

  private FullScreenDialogAppBarDemo() {}

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
    SwingUtilities.invokeLater(() -> new FullScreenDialogAppBarDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JCheckBox withConfirm = new JCheckBox("confirm action", true);
    final JCheckBox withDivider = new JCheckBox("divider", true);

    final JButton open = new JButton("Open full-screen dialog");
    open.addActionListener(e -> openDialog(withConfirm.isSelected(), withDivider.isSelected()));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    controls.add(new JLabel("Next dialog:"));
    controls.add(withConfirm);
    controls.add(withDivider);

    final JPanel center = new JPanel(new GridLayout(0, 1, 0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    center.add(open);
    center.add(status);

    frame.add(controls, BorderLayout.NORTH);
    frame.add(center, BorderLayout.CENTER);
    frame.setSize(820, 560);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openDialog(final boolean withConfirm, final boolean withDivider) {
    final ElwhaFullScreenDialog.Builder b =
        ElwhaFullScreenDialog.builder()
            .headline("New event")
            .content(sampleContent())
            .showDivider(withDivider)
            .onClose(cause -> status.setText("last close: " + cause.name()));

    if (withConfirm) {
      final ElwhaButton save = ElwhaButton.textButton("Save");
      save.addActionListener(e -> status.setText("confirm listener ran → (closing)"));
      b.confirmAction(save);
    }

    b.build().show(frame);
  }

  private JPanel sampleContent() {
    final JPanel form = new JPanel(new GridLayout(0, 1, 0, 16));
    form.setOpaque(false);
    for (int i = 1; i <= 5; i++) {
      form.add(new JLabel("Sample field " + i));
    }
    return form;
  }
}
