package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #290 (S2, epic #288) smoketest — both dialog types suppress the press ripple on their
 * dismiss action buttons, so clicking an action no longer leaves a frozen ripple on the exit-fade
 * snapshot.
 *
 * <p>Validate:
 *
 * <ul>
 *   <li><b>Basic dialog</b> — open it, then click <em>Confirm</em> or <em>Cancel</em> (a mouse
 *       click, not Esc). The dialog fades out cleanly; there is no ripple ghost frozen mid-stroke
 *       on the departing button.
 *   <li><b>Full-screen dialog</b> — open it, then click the trailing <em>Save</em> or the leading
 *       close ✕. Same clean exit, no frozen ripple.
 * </ul>
 *
 * <p>The dialogs suppress the ripple unconditionally (it isn't consumer-toggleable on a wired
 * action), so there's no in-demo control to reinstate the old frozen-ripple behavior — see {@code
 * RippleToggleDemo} (#289) for the bare on/off comparison on a standalone button.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogRippleSuppressionDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class DialogRippleSuppressionDemo {

  private final JFrame frame = new JFrame("Dialog ripple suppression — S2 (#290)");
  private final JLabel status = new JLabel("last close: —", SwingConstants.CENTER);

  private DialogRippleSuppressionDemo() {}

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
    SwingUtilities.invokeLater(() -> new DialogRippleSuppressionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JButton basic = new JButton("Open basic dialog");
    basic.addActionListener(e -> openBasic());

    final JButton fullScreen = new JButton("Open full-screen dialog");
    fullScreen.addActionListener(e -> openFullScreen());

    final JPanel center = new JPanel(new GridLayout(0, 1, 0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    center.add(
        new JLabel(
            "Click an action button (not Esc) and watch for a frozen ripple on exit.",
            SwingConstants.CENTER));
    center.add(basic);
    center.add(fullScreen);
    center.add(status);

    frame.add(center, BorderLayout.CENTER);
    frame.setSize(820, 520);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openBasic() {
    ElwhaDialog.builder()
        .headline("Discard draft?")
        .supportingText("Click Confirm or Cancel — the exit fade should leave no frozen ripple.")
        .confirmAction(ElwhaButton.filledButton("Discard"))
        .cancelAction(ElwhaButton.textButton("Cancel"))
        .onClose(cause -> status.setText("last close: " + cause.name()))
        .build()
        .show(frame);
  }

  private void openFullScreen() {
    ElwhaFullScreenDialog.builder()
        .headline("New event")
        .content(sampleForm())
        .confirmAction(ElwhaButton.textButton("Save"))
        .showDivider(true)
        .onClose(cause -> status.setText("last close: " + cause.name()))
        .build()
        .show(frame);
  }

  private JPanel sampleForm() {
    final JPanel form = new JPanel(new GridLayout(0, 1, 0, 16));
    form.setOpaque(false);
    for (int i = 1; i <= 5; i++) {
      form.add(new JLabel("Sample field " + i));
    }
    return form;
  }
}
