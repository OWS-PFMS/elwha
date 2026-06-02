package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #277 (S2) smoketest — the {@link ElwhaFullScreenDialog} skeleton. Proves the frame-filling
 * surface: opening the dialog covers the entire frame with a {@code SURFACE}-tinted,
 * square-cornered (0dp), shadowless container; the headline + content sit in a max-560dp column
 * centered horizontally; and the background is inert behind it (no scrim is needed — the surface
 * physically covers the app). Esc dismisses (cancel semantics), restoring focus to the trigger.
 *
 * <p>Validate: click "Open full-screen dialog" — the whole frame turns into the dialog surface, the
 * headline reads at the top of a centered column, the sample content below it. Resize the frame
 * narrower than 560px and the column spans the full width; wider and it stays 560 and centered.
 * Press Esc to close; focus returns to the trigger button. The background counter does not advance
 * while the dialog is open.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.dialog.playground.FullScreenDialogSkeletonDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.3.0
 */
public final class FullScreenDialogSkeletonDemo {

  private final JFrame frame = new JFrame("ElwhaFullScreenDialog — S2 skeleton (#277)");
  private final JLabel status = new JLabel("Background clicks: 0   |   last close: —");
  private int backgroundClicks;

  private FullScreenDialogSkeletonDemo() {}

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
    SwingUtilities.invokeLater(() -> new FullScreenDialogSkeletonDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaButton open = ElwhaButton.filledButton("Open full-screen dialog");
    open.addActionListener(e -> openDialog());

    final ElwhaButton bg = ElwhaButton.filledTonalButton("Background button");
    bg.addActionListener(
        e -> {
          backgroundClicks++;
          refresh("—");
        });

    final JPanel center = new JPanel(new GridLayout(0, 1, 0, 12));
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    center.add(open);
    center.add(bg);
    center.add(status);

    frame.add(center, BorderLayout.CENTER);
    frame.setSize(820, 560);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void openDialog() {
    ElwhaFullScreenDialog.builder()
        .headline("New event")
        .content(sampleContent())
        .onClose(cause -> refresh(cause.name()))
        .build()
        .show(frame);
  }

  // A tall-ish stack of placeholder rows standing in for a real form, so the centered-column width
  // and top alignment read. (Scroll-when-taller-than-frame is added in S4.)
  private JPanel sampleContent() {
    final JPanel form = new JPanel(new GridLayout(0, 1, 0, 16));
    form.setOpaque(false);
    for (int i = 1; i <= 6; i++) {
      form.add(new JLabel("Sample field " + i + " — the content column is capped at 560dp."));
    }
    return form;
  }

  private void refresh(final String lastClose) {
    status.setText("Background clicks: " + backgroundClicks + "   |   last close: " + lastClose);
  }
}
