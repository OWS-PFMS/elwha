package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #262 (S2) smoketest — the {@link ElwhaDialog} container + anatomy slots. Each button opens
 * a dialog exercising a different slot combination so the validator can read the container surface
 * (SURFACE_CONTAINER_HIGH / 28px / Level-3 shadow / 24px padding / 280–560px width) and the
 * icon-present centering rule (§7): with an icon, the icon + headline + supporting text are
 * center-aligned; without one, they are start-aligned.
 *
 * <p>Validate: the no-icon dialog is start-aligned and sizes to its content within the 280–560px
 * band; the with-icon dialog centers the whole header column; long supporting text word-wraps at
 * the max content width rather than stretching the dialog past 560px. (No action row yet — Esc or a
 * scrim click closes them; that's S1/S3/S4.)
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.dialog.playground.DialogSkeletonDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class DialogSkeletonDemo {

  private final JFrame frame = new JFrame("ElwhaDialog — S2 container + anatomy (#262)");

  private DialogSkeletonDemo() {}

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
    SwingUtilities.invokeLater(() -> new DialogSkeletonDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 12));
    buttons.setBorder(BorderFactory.createEmptyBorder(48, 64, 48, 64));
    buttons.add(button("Headline only (start-aligned)", this::openHeadlineOnly));
    buttons.add(button("Headline + supporting (start-aligned)", this::openWithSupporting));
    buttons.add(button("Icon + headline + supporting (centered)", this::openWithIcon));
    buttons.add(button("Long supporting text (wraps at max width)", this::openLongText));

    frame.setContentPane(buttons);
    frame.setSize(640, 420);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openHeadlineOnly() {
    ElwhaDialog.builder().headline("Discard draft?").build().show(frame);
  }

  private void openWithSupporting() {
    ElwhaDialog.builder()
        .headline("Discard draft?")
        .supportingText("Your changes haven't been saved and will be lost.")
        .build()
        .show(frame);
  }

  private void openWithIcon() {
    ElwhaDialog.builder()
        .icon(MaterialIcons.symbol("delete").unselected(28))
        .headline("Delete this item?")
        .supportingText("This action can't be undone.")
        .build()
        .show(frame);
  }

  private void openLongText() {
    ElwhaDialog.builder()
        .headline("Permissions changed")
        .supportingText(
            "The administrator updated this workspace's sharing policy. External collaborators "
                + "can no longer view documents in shared folders unless they are explicitly added "
                + "to each folder. Existing links remain valid for internal members only. Review "
                + "your shared folders to confirm the new access levels meet your needs.")
        .build()
        .show(frame);
  }

  private static JButton button(final String text, final Runnable onClick) {
    final JButton b = new JButton(text);
    b.addActionListener(e -> onClick.run());
    return b;
  }
}
