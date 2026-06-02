package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #264 (S4) smoketest — scrim, dismiss semantics, and the scrollable content slot. The first
 * button opens a dialog whose content slot is taller than the frame leaves room for, so the content
 * scrolls inside the dialog while the headline and action row stay pinned, and the 1px scroll
 * divider appears above the actions. The second opens a required-decision dialog with {@code
 * dismissibleByScrim(false)}: clicking the scrim does nothing (the click is consumed but doesn't
 * close), and — with Esc also disabled — only an action can close it.
 *
 * <p>Validate: in the scrolling dialog, drag the scrollbar — the divider is visible only while the
 * content overflows; the headline and buttons never scroll away. In the required-decision dialog, a
 * scrim click is swallowed (status unchanged) and only "Delete" / "Keep" close it.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogDismissDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.3.0
 */
public final class DialogDismissDemo {

  private final JFrame frame = new JFrame("ElwhaDialog — S4 scrim + dismiss + content (#264)");
  private final JLabel status = new JLabel("Open a dialog.");

  private DialogDismissDemo() {}

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
    SwingUtilities.invokeLater(() -> new DialogDismissDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 12));
    buttons.setBorder(BorderFactory.createEmptyBorder(40, 64, 24, 64));
    buttons.add(button("Scrollable content (divider appears)", this::openScrollable));
    buttons.add(button("Required decision (scrim + Esc off)", this::openRequiredDecision));
    buttons.add(status);

    // Keep the frame short so the long content is forced to scroll.
    frame.setContentPane(buttons);
    frame.setSize(560, 320);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openScrollable() {
    final ElwhaButton agree = ElwhaButton.filledButton("Agree");
    final ElwhaButton cancel = ElwhaButton.textButton("Cancel");
    ElwhaDialog.builder()
        .headline("Terms of service")
        .content(buildTermsBody())
        .confirmAction(agree)
        .cancelAction(cancel)
        .onClose(cause -> status.setText("closed: " + cause.name()))
        .build()
        .show(frame);
  }

  private void openRequiredDecision() {
    final ElwhaButton delete = ElwhaButton.filledButton("Delete");
    final ElwhaButton keep = ElwhaButton.textButton("Keep");
    ElwhaDialog.builder()
        .headline("Delete account?")
        .supportingText("This permanently removes your account. You must choose an option.")
        .confirmAction(delete)
        .cancelAction(keep)
        .dismissibleByScrim(false)
        .dismissibleByEsc(false)
        .onClose(cause -> status.setText("closed: " + cause.name()))
        .build()
        .show(frame);
  }

  private static JComponent buildTermsBody() {
    final JPanel body = new JPanel();
    body.setOpaque(false);
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    for (int i = 1; i <= 18; i++) {
      final JLabel line =
          new JLabel("§ " + i + ". Clause text the reader scrolls through to reach the actions.");
      line.setFont(TypeRole.BODY_MEDIUM.resolve());
      line.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      line.setAlignmentX(Component.LEFT_ALIGNMENT);
      body.add(line);
      body.add(Box.createVerticalStrut(8));
    }
    return body;
  }

  private static ElwhaButton button(final String text, final Runnable onClick) {
    final ElwhaButton b = ElwhaButton.filledTonalButton(text);
    b.addActionListener(e -> onClick.run());
    return b;
  }
}
