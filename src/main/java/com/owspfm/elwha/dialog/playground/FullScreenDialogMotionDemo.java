package com.owspfm.elwha.dialog.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #281 (S6) smoketest — slide-up + fade entrance/exit motion. Proves the container translates
 * up from a small downward offset and fades 0 → 1 on open (emphasized-decelerate, 300ms), and
 * reverses (slide-down + fade-out) on close, via the shared {@code MorphAnimator}. The "reduced
 * motion" toggle proves it snaps to the end state with no in-between frames; the slow-mo selector
 * stretches the tween so the slide reads clearly.
 *
 * <p>Validate: with slow-mo at 5× and reduced-motion off, open — the surface slides up from below
 * while fading in; close (✕ / Esc / Save) — it slides back down while fading out, then detaches.
 * Toggle "reduced motion" on and reopen — it appears and disappears instantly, no slide. Restore 1×
 * to confirm the real 300ms feel.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.dialog.playground.FullScreenDialogMotionDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.3.0
 */
public final class FullScreenDialogMotionDemo {

  private final JFrame frame = new JFrame("ElwhaFullScreenDialog — S6 motion (#281)");
  private final JLabel status = new JLabel("last close: —");

  private FullScreenDialogMotionDemo() {}

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
    SwingUtilities.invokeLater(() -> new FullScreenDialogMotionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JCheckBox reduced = new JCheckBox("reduced motion", false);
    reduced.addActionListener(e -> MorphAnimator.setReducedMotion(reduced.isSelected()));

    final JComboBox<String> slowmo = new JComboBox<>(new String[] {"1×", "2×", "5×"});
    slowmo.addActionListener(
        e -> {
          final float mult =
              switch (slowmo.getSelectedIndex()) {
                case 1 -> 2f;
                case 2 -> 5f;
                default -> 1f;
              };
          MorphAnimator.setDurationMultiplier(mult);
        });

    final ElwhaButton open = ElwhaButton.filledButton("Open full-screen dialog");
    open.addActionListener(e -> openDialog());

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    controls.add(reduced);
    controls.add(new JLabel("slow-mo:"));
    controls.add(slowmo);

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

  private void openDialog() {
    ElwhaFullScreenDialog.builder()
        .headline("New event")
        .content(sampleContent())
        .confirmAction(ElwhaButton.textButton("Save"))
        .showDivider(true)
        .onClose(cause -> status.setText("last close: " + cause.name()))
        .build()
        .show(frame);
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
